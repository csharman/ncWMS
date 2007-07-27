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
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.apache.log4j.Logger;
import simple.xml.Element;
import simple.xml.ElementList;
import simple.xml.Root;
import simple.xml.load.Commit;
import simple.xml.load.PersistenceException;
import simple.xml.load.Persister;
import simple.xml.load.Validate;
import uk.ac.rdg.resc.ncwms.datareader.VariableMetadata;
import uk.ac.rdg.resc.ncwms.exceptions.LayerNotDefinedException;

/**
 * Configuration of the server.  We use Simple XML Serialization
 * (http://simple.sourceforge.net/) to convert to and from XML.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Root(name="config")
public class Config
{
    private static final Logger logger = Logger.getLogger(Config.class);
    
    @Element(name="contact", required=false)
    private Contact contact;
    @Element(name="server")
    private Server server;
    @ElementList(name="datasets", type=Dataset.class)
    private Vector<Dataset> datasetList;
    
    private Date lastUpdateTime; // Time of the last update to this configuration
                                 // or any of the contained metadata
    private File configFile;     // Location to which the config information was
                                 // last saved
    
    /**
     * This contains the map of dataset IDs to Dataset objects
     */
    private Map<String, Dataset> datasets;
    
    /** Creates a new instance of Config */
    public Config()
    {
        this.datasets = new Hashtable<String, Dataset>();
        this.datasetList = new Vector<Dataset>();
        this.lastUpdateTime = new Date();
        this.server = new Server();
        this.contact = new Contact();
    }
    
    /**
     * Reads the config information from the default location
     * ($HOME/.ncWMS-Spring/config.xml).  If the configuration file
     * does not exist in the given location it will be created.
     */
    public static Config readConfig() throws Exception
    {
        String homeDir = System.getProperty("user.home");
        File ncWmsDir = new File(homeDir, ".ncWMS-Spring");
        File configFile = new File(ncWmsDir, "config.xml");
        return readConfig(configFile);
    }
    
    /**
     * Reads configuration information from disk.  If the configuration file
     * does not exist in the given location it will be created.
     * @param configFile The configuration file
     * @throws Exception if there was an error reading the configuration
     */
    public static Config readConfig(File configFile) throws Exception
    {
        Config config;
        if (configFile.exists())
        {
            config = new Persister().read(Config.class, configFile);
            logger.debug("Loaded configuration from {}", configFile.getPath());
        }
        else
        {
            // We must make a new config file and save it
            config = new Config();
            config.saveConfig();
            logger.debug("Created new configuration object and saved to {}",
                configFile.getPath());
        }
        config.setConfigFile(configFile);
        return config;
    }
    
    /**
     * Saves configuration information to the disk to the place it was last
     * saved
     * @throws Exception if there was an error reading the configuration
     * @throws IllegalStateException if the config file has not previously been
     * saved.
     */
    public void saveConfig() throws Exception
    {
        if (this.configFile == null)
        {
            throw new IllegalStateException("No location set for config file");
        }
        new Persister().write(this, this.configFile);
    }
    
    /**
     * Checks that the data we have read are valid.  Checks that there are no
     * duplicate dataset IDs or usernames.
     */
    @Validate
    public void validate() throws PersistenceException
    {
        List<String> dsIds = new ArrayList<String>();
        for (Dataset ds : datasetList)
        {
            String dsId = ds.getId();
            if (dsIds.contains(dsId))
            {
                throw new PersistenceException("Duplicate dataset id %s", dsId);
            }
            dsIds.add(dsId);
        }
    }
    
    /**
     * Called when we have checked that the configuration is valid.  Populates
     * the datasets and users hashtables
     */
    @Commit
    public void build()
    {
        for (Dataset ds : this.datasetList)
        {
            ds.setConfig(this);
            this.datasets.put(ds.getId(), ds);
        }
    }
    
    public synchronized void setLastUpdateTime(Date date)
    {
        if (date.after(this.lastUpdateTime))
        {
            this.lastUpdateTime = date;
        }
    }
    
    public Date getLastUpdateTime()
    {
        return this.lastUpdateTime;
    }
    
    /**
     * @return the time of the last change to the configuration or metadata,
     * in seconds since the epoch
     */
    public double getLastUpdateTimeSeconds()
    {
        return this.lastUpdateTime.getTime() / 1000.0;
    }

    public Server getServer()
    {
        return server;
    }

    public void setServer(Server server)
    {
        this.server = server;
    }

    public Contact getContact()
    {
        return contact;
    }

    public void setContact(Contact contact)
    {
        this.contact = contact;
    }

    public File getConfigFile()
    {
        return configFile;
    }

    public void setConfigFile(File configFile)
    {
        this.configFile = configFile;
    }
    
    public Map<String, Dataset> getDatasets()
    {
        return this.datasets;
    }
    
    public synchronized void addDataset(Dataset ds)
    {
        ds.setConfig(this);
        this.datasets.put(ds.getId(), ds);
        this.datasetList.add(ds);
    }
    
    public synchronized void removeDataset(Dataset ds)
    {
        this.datasets.remove(ds.getId());
        this.datasetList.remove(ds);
    }
    
    /**
     * @return the VariableMetadata object that corresponds with the supplied
     * layer name (e.g. "FOAM_ONE/TMP").
     * @throws LayerNotDefinedException if the given name does not match an
     * available layer.
     */
    public VariableMetadata getVariable(String layerName)
        throws LayerNotDefinedException
    {
        // NOTE!! The logic of this method must match up with
        // VariableMetadata.getLayerName()!
        String[] dsAndVarIds = layerName.split("/");
        if (dsAndVarIds.length != 2)
        {
            throw new LayerNotDefinedException(layerName);
        }
        Dataset ds = this.datasets.get(dsAndVarIds[0]);
        if (ds == null)
        {
            throw new LayerNotDefinedException(layerName);
        }
        VariableMetadata var = ds.getVariables().get(dsAndVarIds[1]);
        if (var == null)
        {
            throw new LayerNotDefinedException(layerName);
        }
        return var;
    }
    
}