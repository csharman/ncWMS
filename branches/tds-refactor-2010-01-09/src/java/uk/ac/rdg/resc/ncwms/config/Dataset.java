/*
 * Copyright (c) 2007 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.ncwms.config;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.oro.io.GlobFilenameFilter;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.load.Commit;
import org.simpleframework.xml.load.PersistenceException;
import org.simpleframework.xml.load.Validate;
import uk.ac.rdg.resc.ncwms.datareader.DataReader;
import uk.ac.rdg.resc.ncwms.utils.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.Layer;

/**
 * A dataset object in the ncWMS configuration system: contains a number of
 * Layer objects, which are held in memory and loaded periodically, triggered
 * by the {@link MetadataLoader}.
 *
 * @author Jon Blower
 * @todo A lot of these methods can be made package-private
 */
@Root(name="dataset")
public class Dataset implements uk.ac.rdg.resc.ncwms.wms.Dataset
{
    private static final Logger logger = LoggerFactory.getLogger(Dataset.class);
    
    @Attribute(name="id")
    private String id; // Unique ID for this dataset
    
    @Attribute(name="location")
    private String location; // Location of this dataset (NcML file, OPeNDAP location etc)
    
    @Attribute(name="queryable", required=false)
    private boolean queryable = true; // True if we want GetFeatureInfo enabled for this dataset
    
    @Attribute(name="dataReaderClass", required=false)
    private String dataReaderClass = ""; // We'll use a default data reader
                                         // unless this is overridden in the config file
    
    @Attribute(name="copyrightStatement", required=false)
    private String copyrightStatement = "";

    @Attribute(name="moreInfo", required=false)
    private String moreInfo = "";
    
    @Attribute(name="disabled", required=false)
    private boolean disabled = false; // Set true to disable the dataset without removing it completely
    
    @Attribute(name="title")
    private String title;
    
    @Attribute(name="updateInterval", required=false)
    private int updateInterval = -1; // The update interval in minutes. -1 means "never update automatically"

    // We don't do "private List<Variable> variable..." here because if we do,
    // the config file will contain "<variable class="java.util.ArrayList>",
    // presumably because the definition doesn't clarify what sort of List should
    // be used.
    // This allows the admin to override certain auto-detected parameters of
    // the variables within the dataset (e.g. title, min and max values)
    // This is a temporary store of variables that are read from the config file.
    // The real set of all variables is in the variables Map.
    @ElementList(name="variables", type=Variable.class, required=false)
    private ArrayList<Variable> variableList = new ArrayList<Variable>();
    
    private State state = State.NEEDS_REFRESH;     // State of this dataset.
    
    private Exception err;   // Set if there is an error loading the dataset
    private StringBuilder loadingProgress = new StringBuilder(); // Used to express progress with loading
                                         // the metadata for this dataset

    /**
     * This contains the map of variable IDs to Variable objects.  We use a
     * LinkedHashMap so that the order of datasets in the Map is preserved.
     */
    private Map<String, Variable> variables = new LinkedHashMap<String, Variable>();

    /** The object that will be used for reading metadata (Layers) and data
     * from the source files */
    private DataReader dataReader;

    /** The time at which this dataset's stored Layers were last updated, or
     * null if the Layers have not yet been loaded */
    private DateTime lastUpdateTime = null;

    /** The Layers that belong to this dataset.  This will be loaded through the
     * {@link #loadLayers()} method, which is called periodically by the
     * {@link MetadataLoader}. */
    private Map<String, Layer> layers;

    /**
     * Checks that the data we have read are valid.  Checks that there are no
     * duplicate variable IDs.
     */
    @Validate
    public void validate() throws PersistenceException
    {
        List<String> varIds = new ArrayList<String>();
        for (Variable var : this.variableList)
        {
            String varId = var.getId();
            if (varIds.contains(varId))
            {
                throw new PersistenceException("Duplicate variable id %s", varId);
            }
            varIds.add(varId);
        }
    }

    /**
     * Called when we have checked that the configuration is valid.  Populates
     * the variables hashmap.
     */
    @Commit
    public void build()
    {
        // We already know from validate() that there are no duplicate variable
        // IDs
        for (Variable var : this.variableList)
        {
            var.setDataset(this);
            this.variables.put(var.getId(), var);
        }
    }

    @Override
    public String getId()
    {
        return this.id;
    }
    
    public void setId(String id)
    {
        this.id = id.trim();
    }
    
    public String getLocation()
    {
        return location;
    }

    public void setLocation(String location)
    {
        this.location = location.trim();
    }
    
    /**
     * @return true if this dataset is ready for use
     */
    @Override
    public synchronized boolean isReady()
    {
        return !this.isDisabled() &&
               (this.state == State.READY ||
                this.state == State.UPDATING);
    }

    /**
     * @return true if this dataset is not ready because it is being loaded
     */
    @Override
    public synchronized boolean isLoading()
    {
        return !this.isDisabled() &&
               (this.state == State.NEEDS_REFRESH ||
                this.state == State.LOADING);
    }

    @Override
    public boolean isError()
    {
        return this.state == State.ERROR;
    }

    /**
     * If this Dataset has not been loaded correctly, this returns the Exception
     * that was thrown.  If the dataset has no errors, this returns null.
     */
    @Override
    public Exception getException()
    {
        return this.state == State.ERROR ? this.err : null;
    }

    public State getState()
    {
        return this.state;
    }
    
    /**
     * @return true if this dataset can be queried using GetFeatureInfo
     */
    public boolean isQueryable()
    {
        return this.queryable;
    }
    
    public void setQueryable(boolean queryable)
    {
        this.queryable = queryable;
    }
    
    /**
     * @return the human-readable Title of this dataset
     */
    @Override
    public String getTitle()
    {
        return this.title;
    }
    
    public void setTitle(String title)
    {
        this.title = title;
    }
    
    @Override
    public String toString()
    {
        return "id: " + this.id + ", location: " + this.location;
    }

    public String getDataReaderClass()
    {
        return dataReaderClass;
    }

    void setDataReaderClass(String dataReaderClass)
    {
        this.dataReaderClass = dataReaderClass;
    }

    /**
     * @return the update interval for this dataset in minutes
     */
    public int getUpdateInterval()
    {
        return updateInterval;
    }

    /**
     * Sets the update interval for this dataset in minutes
     */
    void setUpdateInterval(int updateInterval)
    {
        this.updateInterval = updateInterval;
    }
    
    /**
     * @return a DateTime object representing the time at which this dataset was
     * last updated, or the present time if this is unknown.  This is only used
     * in the generation of Capabilities documents.
     */
    @Override
    public DateTime getLastUpdateTime()
    {
        return this.lastUpdateTime == null ? new DateTime() : this.lastUpdateTime;
    }

    /**
     * Returns the layer in this dataset with the given id, or null if there is
     * no layer in this dataset with the given id.
     * @param layerId The layer identifier, unique within this dataset.  Note that
     * this is distinct from the layer name, which is unique on the server.
     * @return the layer in this dataset with the given id, or null if there is
     * no layer in this dataset with the given id.
     */
    Layer getLayer(String layerId)
    {
        return this.layers.get(layerId);
    }
    
    /**
     * @return a Set of all the layers in this dataset.
     */
    @Override
    public Set<Layer> getLayers()
    {
        Collection<Layer> layersInDataset = this.layers.values();
        Set<Layer> layerSet = new LinkedHashSet<Layer>(layersInDataset.size());
        for (Layer layerInDataset : layersInDataset) {
            layerSet.add(layerInDataset);
        }
        return layerSet;
    }

    /**
     * Returns true if this dataset has been disabled, which will make it
     * invisible to the outside world.
     * @return true if this dataset has been disabled
     */
    @Override
    public boolean isDisabled()
    {
        return disabled;
    }

    /**
     * Called by the admin application to hide a dataset completely from public
     * view
     * @param disabled
     */
    public void setDisabled(boolean disabled)
    {
        this.disabled = disabled;
    }

    @Override
    public String getCopyrightStatement()
    {
        return copyrightStatement;
    }

    public void setCopyrightStatement(String copyrightStatement)
    {
        this.copyrightStatement = copyrightStatement;
    }

    @Override
    public String getMoreInfoUrl()
    {
        return moreInfo;
    }

    public void setMoreInfo(String moreInfo)
    {
        this.moreInfo = moreInfo;
    }

    /**
     * Gets an explanation of the current progress with loading this dataset.
     * Will be displayed in the admin application when isLoading() == true.
     */
    public String getLoadingProgress()
    {
        return this.loadingProgress.toString();
    }

    /**
     * Gets the configuration information for all the {@link Variable}s in this
     * dataset.  This information allows the system administrator to manually
     * set certain properties that would otherwise be auto-detected.
     * @return A {@link Map} of variable IDs to {@link Variable} objects.  The
     * variable ID is unique within a dataset and corresponds with the {@link Layer#getId()}.
     * @see Variable
     */
    public Map<String, Variable> getVariables()
    {
        return variables;
    }

    public void addVariable(Variable var)
    {
        var.setDataset(this);
        this.variableList.add(var);
        this.variables.put(var.getId(), var);
    }

    /**
     * Forces this dataset to be refreshed the next time it has an opportunity
     */
    void forceRefresh()
    {
        this.state = State.NEEDS_REFRESH;
    }

    /**
     * Called by the scheduled reloader in the {@link Config} object to load
     * the Layers from the data files and store them in memory.  This method
     * is called periodially by the config object and is not called by any
     * other client.  This is also the only method that can update the
     * {@link #getState()} of the dataset.  Therefore we know that multiple
     * threads will not be calling this method simultaneously and we don't
     * have to synchronize anything.
     */
    void loadLayers()
    {
        // Include the id of the dataset in the thread for debugging purposes
        // Comment this out to use the default thread names (e.g. "pool-2-thread-1")
        Thread.currentThread().setName("load-metadata-" + this.id);

        // Check to see if this dataset needs to have its metadata refreshed
        if (!this.needsRefresh()) return;

        // Now load the layers and manage the state of the dataset
        try
        {
            // if lastUpdateTime == null, this dataset has never previously been loaded.
            this.state = this.lastUpdateTime == null ? State.LOADING : State.UPDATING;
            this.doLoadLayers();
            this.state = State.READY;
            this.lastUpdateTime = new DateTime();
        }
        catch (Exception e)
        {
            this.state = State.ERROR;
            // Reduce logging volume by only logging the error if it's a new
            // type of exception.
            if (this.err == null || this.err.getClass() != e.getClass())
            {
                logger.error(e.getClass().getName() + " loading metadata for dataset "
                    + this.id, e);
            }
            this.err = e;
        }
    }
    
    /**
     * @return true if the metadata from this dataset needs to be reloaded.
     */
    private boolean needsRefresh()
    {
        // We don't use getLastUpdateTime(), because the dataset might not ever
        // have been loaded, and so lastUpdate might be null. (getLastUpdateTime()
        // is defined never to return null.)
        logger.debug("Last update time for dataset {} is {}", this.id, this.lastUpdateTime);
        logger.debug("State of dataset {} is {}", this.id, this.state);
        logger.debug("Disabled = {}", this.disabled);
        if (this.disabled || this.state == State.LOADING || this.state == State.UPDATING)
        {
            return false;
        }
        else if (this.state == State.NEEDS_REFRESH || this.state == State.ERROR)
        {
            return true;
        }
        else if (this.updateInterval < 0)
        {
            return false; // We never update this dataset
        }
        else
        {
            // State = READY.  Check the age of the metadata
            // Return true if we are after the next scheduled update
            return new DateTime().isAfter(this.lastUpdateTime.plusMinutes(this.updateInterval));
        }
    }

    /**
     * Does the job of loading the metadata from this dataset.
     */
    private void doLoadLayers() throws Exception
    {
        // Get the filenames that comprise this dataset, expanding any glob expressions
        List<String> filenames = this.getFiles();
        if (filenames.size() == 0)
        {
            throw new Exception(this.location + " does not match any files");
        }

        // Now extract the data for each individual file
        // LinkedHashMaps preserve the order of insertion
        Map<String, LayerImpl> layers = new LinkedHashMap<String, LayerImpl>();
        for (String filename : filenames)
        {
            // Read the metadata from the file and update the Map.
            // TODO: only do this if the file's last modified date has changed?
            // This would require us to keep the previous metadata...
            this.dataReader.findAndUpdateLayers(filename, layers);

            // Follow the steps in MetadataLoader, updating the progress information
        }
    }

    /**
     * Gets a List of the files that comprise this dataset; if this dataset's
     * location is a glob expression, this will be expanded.  This method
     * recursively searches directories, allowing for glob expressions like
     * {@code "c:\\data\\200[6-7]\\*\\1*\\A*.nc"}.  If this dataset's location
     * is not a glob expression, this method will return a single-element list
     * containing the dataset's {@link #getLocation() location}.
     * @return a list of the full paths to files that comprise this dataset,
     * or a single-element list containing the dataset's location.
     * @throws Exception if the glob expression does not represent an absolute
     * path
     * @author Mike Grant, Plymouth Marine Labs; Jon Blower
     */
    private List<String> getFiles() throws Exception
    {
        if (WmsUtils.isOpendapLocation(this.location)) return Arrays.asList(this.location);
        // Check that the glob expression is an absolute path.  Relative paths
        // would cause unpredictable and platform-dependent behaviour so
        // we disallow them.
        // If ds.getLocation() is a glob expression this test will still work
        // because we are not attempting to resolve the string to a real path.
        File globFile = new File(this.location);
        if (!globFile.isAbsolute())
        {
            throw new Exception("Dataset location must be an absolute path");
        }

        // Break glob pattern into path components.  To do this in a reliable
        // and platform-independent way we use methods of the File class, rather
        // than String.split().
        List<String> pathComponents = new ArrayList<String>();
        while (globFile != null)
        {
            // We "pop off" the last component of the glob pattern and place
            // it in the first component of the pathComponents List.  We therefore
            // ensure that the pathComponents end up in the right order.
            File parent = globFile.getParentFile();
            // For a top-level directory, getName() returns an empty string,
            // hence we use getPath() in this case
            String pathComponent = parent == null ? globFile.getPath() : globFile.getName();
            pathComponents.add(0, pathComponent);
            globFile = parent;
        }

        // We must have at least two path components: one directory and one
        // filename or glob expression
        List<File> searchPaths = new ArrayList<File>();
        searchPaths.add(new File(pathComponents.get(0)));
        int i = 1; // Index of the glob path component

        while(i < pathComponents.size())
        {
            FilenameFilter globFilter = new GlobFilenameFilter(pathComponents.get(i));
            List<File> newSearchPaths = new ArrayList<File>();
            // Look for matches in all the current search paths
            for (File dir : searchPaths)
            {
                if (dir.isDirectory())
                {
                    // Workaround for automounters that don't make filesystems
                    // appear unless they're poked
                    // do a listing on searchpath/pathcomponent whether or not
                    // it exists, then discard the results
                    new File(dir, pathComponents.get(i)).list();

                    for (File match : dir.listFiles(globFilter))
                    {
                        newSearchPaths.add(match);
                    }
                }
            }
            // Next time we'll search based on these new matches and will use
            // the next globComponent
            searchPaths = newSearchPaths;
            i++;
        }

        // Now we've done all our searching, we'll only retain the files from
        // the list of search paths
        List<String> filePaths = new ArrayList<String>();
        for (File path : searchPaths)
        {
            if (path.isFile()) filePaths.add(path.getPath());
        }
        return filePaths;
    }

    /**
     * The state of a Dataset.
     */
    public static enum State {

        /** Dataset is new or has changed and needs to be loaded */
        NEEDS_REFRESH,

        /** In the process of loading */
        LOADING,

        /** Ready for use */
        READY,

        /** Dataset is ready but is internally sychronizing its metadata */
        UPDATING,

        /** An error occurred when loading the dataset. */
        ERROR;

    };
}
