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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import uk.ac.rdg.resc.ncwms.datareader.HorizontalGrid;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.util.Range;
import uk.ac.rdg.resc.ncwms.util.Ranges;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import uk.ac.rdg.resc.ncwms.wms.ScalarLayer;
import uk.ac.rdg.resc.ncwms.wms.VectorLayer;

/**
 * A dataset object in the ncWMS configuration system: contains a number of
 * Layer objects, which are held in memory and loaded periodically, triggered
 * by the {@link Config} object.
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

    private Config config;
    
    private State state = State.NEEDS_REFRESH;     // State of this dataset.
    
    private Exception err;   // Set if there is an error loading the dataset
    private StringBuilder loadingProgress = new StringBuilder(); // Used to express progress with loading
                                         // the metadata for this dataset

    /**
     * This contains the map of variable IDs to Variable objects.  We use a
     * LinkedHashMap so that the order of datasets in the Map is preserved.
     */
    private Map<String, Variable> variables = new LinkedHashMap<String, Variable>();

    /** The time at which this dataset's stored Layers were last updated, or
     * null if the Layers have not yet been loaded */
    private DateTime lastUpdateTime = null;

    /** The Layers that belong to this dataset.  This will be loaded through the
     * {@link #loadLayers()} method, which is called periodically by the
     * {@link Config} object. */
    private Map<String, LayerImpl> scalarLayers;

    /** The VectorLayers generated from the scalarLayers */
    private Map<String, VectorLayerImpl> vectorLayers;

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
     * Called when a new dataset is created to set up a back-link to the
     * {@link Config} object.  This is only called by {@link Config}.
     */
    void setConfig(Config config)
    {
        this.config = config;
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

    void setDataReaderClass(String dataReaderClass) throws Exception
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
        return this.scalarLayers.get(layerId);
    }
    
    /**
     * @return a Set of all the layers in this dataset.
     */
    @Override
    public Set<Layer> getLayers()
    {
        Set<Layer> layerSet = new LinkedHashSet<Layer>(this.scalarLayers.size());
        for (Layer scalarLayer : this.scalarLayers.values()) {
            layerSet.add(scalarLayer);
        }
        for (VectorLayer vectorLayer : this.vectorLayers.values()) {
            layerSet.add(vectorLayer);
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


    /**
     * Gets the copyright statement for this dataset, replacing "${year}" as
     * appropriate with the current year.
     * @return The copyright statement, or the empty string if no copyright
     * statement has been set.
     */
    @Override
    public String getCopyrightStatement()
    {
        if (this.copyrightStatement == null || this.copyrightStatement.trim().equals(""))
        {
            return "";
        }
        int currentYear = new DateTime().getYear();
        // Don't forget to escape dollar signs and backslashes in the regexp
        return this.copyrightStatement.replaceAll("\\$\\{year\\}", "" + currentYear);
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

            // Update the state of this dataset
            this.state = State.READY;
            this.lastUpdateTime = new DateTime();

            logger.debug("Loaded metadata for {}", this.id);
            
            // Update the state of the config object
            this.config.setLastUpdateTime(this.lastUpdateTime);
            this.config.save();
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
        logger.debug("Getting data reader of type {}", this.dataReaderClass);
        DataReader dr = DataReader.forName(this.dataReaderClass);
        // Look for OPeNDAP datasets and update the credentials provider accordingly
        this.config.updateCredentialsProvider(this);
        // Read the metadata
        this.scalarLayers = dr.getAllLayers(this);
        for (LayerImpl layer : this.scalarLayers.values())
        {
            layer.setDataset(this);
            layer.setDataReader(dr);
        }
        this.loadingProgress.append("loaded layers");
        // Search for vector quantities (e.g. northward/eastward_sea_water_velocity)
        this.findVectorQuantities();
        this.loadingProgress.append("found vector quantities");
        // Look for overriding attributes in the configuration
        this.readLayerConfig();
        this.loadingProgress.append("attributes overridden");
        this.loadingProgress.append("Finished loading metadata");
    }

    /**
     * Searches through the collection of Layer objects, looking for
     * pairs of quantities that represent the components of a vector, e.g.
     * northward/eastward_sea_water_velocity.  Modifies the given Map
     * in-place.
     * @todo Only works for northward/eastward so far
     */
    private void findVectorQuantities()
    {
        // This hashtable will store pairs of components in eastward-northward
        // order, keyed by the standard name for the vector quantity
        Map<String, LayerImpl[]> components = new HashMap<String, LayerImpl[]>();
        this.vectorLayers = new LinkedHashMap<String, VectorLayerImpl>();
        for (LayerImpl layer : this.scalarLayers.values())
        {
            if (layer.getTitle().contains("eastward"))
            {
                String vectorKey = layer.getTitle().replaceFirst("eastward_", "");
                // Look to see if we've already found the northward component
                if (!components.containsKey(vectorKey))
                {
                    // We haven't found the northward component yet
                    components.put(vectorKey, new LayerImpl[2]);
                }
                components.get(vectorKey)[0] = layer;
            }
            else if (layer.getTitle().contains("northward"))
            {
                String vectorKey = layer.getTitle().replaceFirst("northward_", "");
                // Look to see if we've already found the eastward component
                if (!components.containsKey(vectorKey))
                {
                    // We haven't found the eastward component yet
                    components.put(vectorKey, new LayerImpl[2]);
                }
                components.get(vectorKey)[1] = layer;
            }
        }

        // Now add the vector quantities to the collection of Layer objects
        for (String key : components.keySet())
        {
            LayerImpl[] comps = components.get(key);
            if (comps[0] != null && comps[1] != null)
            {
                // We've found both components.  Create a new Layer object
                VectorLayerImpl vec = new VectorLayerImpl(key, comps[0], comps[1]);
                this.vectorLayers.put(key, vec);
            }
        }
    }

    /**
     * Read the configuration information from individual layers from the
     * config file.
     */
    private void readLayerConfig()
    {
        for (Layer layer : this.getLayers()) // all the layers, scalars and vectors
        {
            // Load the Variable object from the config file or create a new
            // one if it doesn't exist.
            Variable var = this.getVariables().get(layer.getId());
            if (var == null)
            {
                var = new Variable();
                var.setId(layer.getId());
                this.addVariable(var);
            }

            // If there is no title set for this layer in the config file, we
            // use the title that was read by the DataReader.
            if (var.getTitle() == null) var.setTitle(layer.getTitle());

            // Set the colour scale range.  If this isn't specified in the
            // config information, load an "educated guess" at the scale range
            // from the source data.
            if (var.getColorScaleRange() == null)
            {
                this.loadingProgress.append("Reading min-max data for layer " + layer.getName());
                Range<Float> valueRange;
                try
                {
                    valueRange = estimateValueRange(layer);
                    if (valueRange.isEmpty())
                    {
                        // We failed to get a valid range.  Just guess at a scale
                        valueRange = Ranges.newRange(-50.0f, 50.0f);
                    }
                    else if (valueRange.getMinimum().equals(valueRange.getMaximum()))
                    {
                        // This happens occasionally if the above algorithm happens
                        // to hit an area of uniform data.  We make sure that
                        // the max is greater than the min.
                        valueRange = Ranges.newRange(valueRange.getMinimum(),
                            valueRange.getMaximum() + 1.0f);
                    }
                    else
                    {
                        // Set the scale range of the layer, factoring in a 10% expansion
                        // to deal with the fact that the sample data we read might
                        // not be representative
                        float diff = valueRange.getMaximum() - valueRange.getMinimum();
                        valueRange = Ranges.newRange(
                            valueRange.getMinimum() - 0.05f * diff,
                            valueRange.getMaximum() + 0.05f * diff
                        );
                    }
                }
                catch(Exception e)
                {
                    logger.error("Error reading min-max from layer " + layer.getId()
                        + " in dataset " + this.id, e);
                    valueRange = Ranges.newRange(-50.0f, 50.0f);
                }
                var.setColorScaleRange(valueRange);
            }
        }
    }

    /**
     * Estimate the range of values in this layer by reading a sample of data
     * from the default time and elevation.
     * @return
     * @throws IOException if there was an error reading from the source data
     */
    private static Range<Float> estimateValueRange(Layer layer) throws IOException
    {
        if (layer instanceof ScalarLayer)
        {
            List<Float> dataSample = readDataSample((ScalarLayer)layer);
            return Ranges.findMinMax(dataSample);
        }
        else if (layer instanceof VectorLayer)
        {
            VectorLayer vecLayer = (VectorLayer)layer;
            List<Float> eastDataSample = readDataSample(vecLayer.getEastwardComponent());
            List<Float> northDataSample = readDataSample(vecLayer.getEastwardComponent());
            List<Float> magnitudes = getMagnitudes(eastDataSample, northDataSample);
            return Ranges.findMinMax(magnitudes);
        }
        else
        {
            throw new IllegalStateException("Unrecognized layer type");
        }
    }

    private static List<Float> readDataSample(ScalarLayer layer) throws IOException
    {
        try {
            // Read a low-resolution grid of data covering the entire spatial extent
            return layer.readPointList(
                layer.getDefaultTimeValue(),
                layer.getDefaultElevationValue(),
                new HorizontalGrid(100, 100, layer.getGeographicBoundingBox())
            );
        } catch (InvalidDimensionValueException idve) {
            // This would only happen due to a programming error in getDefaultXValue()
            throw new IllegalStateException(idve);
        }
    }

    private static List<Float> getMagnitudes(List<Float> eastData, List<Float> northData)
    {
        if (eastData == null || northData == null) throw new NullPointerException();
        if (eastData.size() != northData.size())
        {
            throw new IllegalArgumentException("east and north data components must be the same length");
        }
        List<Float> mag = new ArrayList<Float>(eastData.size());
        for (int i = 0; i < eastData.size(); i++)
        {
            Float east = eastData.get(i);
            Float north = northData.get(i);
            Float val = null;
            if (east != null && north != null)
            {
                val = (float)Math.sqrt(east * east + north * north);
            }
            mag.add(val);
        }
        if (mag.size() != eastData.size()) throw new AssertionError();
        return mag;
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
