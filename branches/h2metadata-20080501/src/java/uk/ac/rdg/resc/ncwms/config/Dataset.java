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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;
import org.apache.oro.io.GlobFilenameFilter;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;
import uk.ac.rdg.resc.ncwms.datareader.DataReader;
import uk.ac.rdg.resc.ncwms.metadata.Layer;
import uk.ac.rdg.resc.ncwms.utils.WmsUtils;

/**
 * A dataset Java bean: contains a number of Layer objects.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Root(name="dataset")
public class Dataset
{
    private static final Logger logger = Logger.getLogger(Dataset.class);
    
    /**
     * The state of a Dataset.
     * TO_BE_LOADED: Dataset is new or has changed and needs to be loaded
     * LOADING: In the process of loading
     * READY: Ready for use
     * UPDATING: A previously-ready dataset is synchronizing with the disk
     * ERROR: An error occurred when loading the dataset.
     */
    public static enum State { TO_BE_LOADED, LOADING, READY, UPDATING, ERROR };
    
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
    
    @Attribute(name="disabled", required=false)
    private boolean disabled = false; // Set true to disable the dataset without removing it completely
    
    @Attribute(name="title")
    private String title;
    
    @Attribute(name="updateInterval", required=false)
    private int updateInterval = -1; // The update interval in minutes. -1 means "never update automatically"
    
    private State state;     // State of this dataset.  Will be set in Config.build()
    
    private Exception err;   // Set if there is an error loading the dataset
    private Config config;   // The Config object to which this belongs

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
    public synchronized boolean isReady()
    {
        return this.disabled == false &&
            (this.state == State.READY || this.state == State.UPDATING);
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
    public String getTitle()
    {
        return this.title;
    }
    
    public void setTitle(String title)
    {
        this.title = title;
    }
    
    /**
     * @return true if the metadata from this dataset needs to be reloaded
     * automatically via the periodic reloader in MetadataLoader
     */
    public boolean needsRefresh()
    {
        Date lastUpdate = this.getLastUpdate();
        logger.debug("Last update time for dataset {} is {}", this.id, lastUpdate);
        logger.debug("State of dataset {} is {}", this.id, this.state);
        logger.debug("Disabled = {}", this.disabled);
        if (this.disabled || this.state == State.LOADING || this.state == State.UPDATING)
        {
            return false;
        }
        else if (this.state == State.ERROR || this.state == State.TO_BE_LOADED
            || lastUpdate == null)
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
            Calendar cal = Calendar.getInstance();
            cal.setTime(lastUpdate);
            cal.add(Calendar.MINUTE, this.updateInterval);
            // Return true if we are after the next scheduled update
            return new Date().after(cal.getTime());
        }
    }
    
    /**
     * @return true if there is an error with this dataset
     */
    public boolean isError()
    {
        return this.state == State.ERROR;
    }
    
    /**
     * If this Dataset has not been loaded correctly, this returns the Exception
     * that was thrown.  If the dataset has no errors, this returns null.
     */
    public Exception getException()
    {
        return this.state == State.ERROR ? this.err : null;
    }
    
    /**
     * Called by the MetadataReloader to set the error associated with this
     * dataset
     */
    public void setException(Exception e)
    {
        this.err = e;
    }
    
    public State getState()
    {
        return this.state;
    }
    
    public void setState(State state)
    {
        this.state = state;
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

    public void setDataReaderClass(String dataReaderClass)
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
    public void setUpdateInterval(int updateInterval)
    {
        this.updateInterval = updateInterval;
    }

    public void setConfig(Config config)
    {
        this.config = config;
    }
    
    /**
     * @return a Date object representing the time at which this dataset was
     * last updated, or null if this dataset has never been updated.  Delegates
     * to {@link uk.ac.rdg.resc.ncwms.metadata.MetadataStore#getLastUpdateTime}
     * (because the last update time is 
     * stored with the metadata - which may or may not be persistent across
     * server reboots, depending on the type of MetadataStore).
     */
    public Date getLastUpdate()
    {
        return this.config.getMetadataStore().getLastUpdateTime(this.id);
    }
    
    /**
     * @return a Collection of all the layers in this dataset.  A convenience
     * method that reads from the metadata store.
     * @throws Exception if there was an error reading from the store.
     */
    public Collection<? extends Layer> getLayers() throws Exception
    {
        return this.config.getMetadataStore().getLayersInDataset(this.id);
    }
    
    /**
     * Gets the names of all the files that make up this dataset.  In the case
     * of an OPeNDAP endpoint, this simply returns a
     * single-element list with the location of the dataset.  In the case of a
     * glob aggregation this finds all files in the local filesystem that match
     * the glob expression.
     * @throws IOException if there was an error reading from the local
     * filesystem, or if the location of this dataset is not valid.
     */
    public List<String> getFilenames() throws IOException
    {
        // A list of names of files resulting from glob expansion
        List<String> filenames = new ArrayList<String>();
        if (WmsUtils.isOpendapLocation(this.location))
        {
            // We don't do the glob expansion
            filenames.add(this.location);
        }
        else
        {
            // The location might be a glob expression, in which case the last part
            // of the location path will be the filter expression
            File locFile = new File(this.location);
            FilenameFilter filter = new GlobFilenameFilter(locFile.getName());
            File parentDir = locFile.getParentFile();
            if (parentDir == null)
            {
                throw new IOException(locFile.getPath() + " is not a valid path");
            }
            if (!parentDir.isDirectory())
            {
                throw new IOException(parentDir.getPath() + " is not a valid directory");
            }
            // Find the files that match the glob pattern
            File[] files = parentDir.listFiles(filter);
            if (files == null || files.length == 0)
            {
                throw new IOException(this.location + " does not match any files");
            }
            // Add all the matching filenamse
            for (File f : files)
            {
                filenames.add(f.getPath());
            }
        }
        return filenames;
    }
    
    /**
     * Returns a DataReader object for reading data and metadata from this
     * Dataset.
     * @throws Exception if the DataReader object could not be retrieved
     */
    public DataReader getDataReader() throws Exception
    {
        return DataReader.getDataReader(this.dataReaderClass, this.location);
    }

    public boolean isDisabled()
    {
        return disabled;
    }

    public void setDisabled(boolean disabled)
    {
        this.disabled = disabled;
    }

    public String getCopyrightStatement()
    {
        return copyrightStatement;
    }

    public void setCopyrightStatement(String copyrightStatement)
    {
        this.copyrightStatement = copyrightStatement;
    }
}
