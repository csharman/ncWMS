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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.load.Commit;
import org.simpleframework.xml.load.PersistenceException;
import org.simpleframework.xml.load.Persister;
import org.simpleframework.xml.load.Validate;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import uk.ac.rdg.resc.ncwms.exceptions.LayerNotDefinedException;
import uk.ac.rdg.resc.ncwms.security.Users;
import uk.ac.rdg.resc.ncwms.utils.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import uk.ac.rdg.resc.ncwms.wms.ServerConfig;

/**
 * <p>Configuration of the ncWMS server.  We use Simple XML Serialization
 * (http://simple.sourceforge.net/) to convert to and from XML.</p>
 * <p>This implements {@link ServerConfig}, which is the general interface
 * for providing access to server metadata and data.  (ServerConfig can be
 * implemented by other configuration systems and catalogs, e.g. THREDDS.)</p>
 *
 * @author Jon Blower
 */
@Root(name="config")
public class Config implements ServerConfig, ApplicationContextAware
{
    private static final Logger logger = LoggerFactory.getLogger(Config.class);
    
    // We don't do "private List<Dataset> datasetList..." here because if we do,
    // the config file will contain "<datasets class="java.util.ArrayList>",
    // presumably because the definition doesn't clarify what sort of List should
    // be used.
    // This is a temporary store of datasets that are read from the config file.
    // The real set of all datasets is in the datasets Map.
    @ElementList(name="datasets", type=Dataset.class)
    private ArrayList<Dataset> datasetList = new ArrayList<Dataset>();
    
    // Nothing happens to this at the moment... TODO for the future
    @Element(name="threddsCatalog", required=false)
    private String threddsCatalogLocation = " ";    //location of the Thredds Catalog.xml (if there is one...)
    
    @Element(name="contact", required=false)
    private Contact contact = new Contact();
    
    @Element(name="server")
    private Server server = new Server();
    
    @Element(name="cache", required=false)
    private Cache cache = new Cache();
    
    // Time of the last update to this configuration or any of the contained
    // metadata
    private DateTime lastUpdateTime = new DateTime();
    
    private File configFile; // Location of the file from which this information has been read
    
    /**
     * This contains the map of dataset IDs to Dataset objects.  We use a 
     * LinkedHashMap so that the order of datasets in the Map is preserved.
     */
    private Map<String, Dataset> datasets = new LinkedHashMap<String, Dataset>();

    /** The scheduler that will handle the background (re)loading of datasets */
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    /** Contains handles to background threads that can be used to cancel reloading of datasets.
      * Maps dataset Ids to Future objects*/
    private Map<String, ScheduledFuture<?>> futures = new HashMap<String, ScheduledFuture<?>>();
    
    /**
     * Private constructor.  This prevents other classes from creating
     * new Config objects directly.
     */
    private Config() {}
    
    /**
     * Reads configuration information from the file location given by the
     * provided Context object
     * @param ncwmsContext object containing the context of this ncWMS application
     * (including the location of the config file)
     */
    public static Config readConfig(NcwmsContext ncwmsContext) throws Exception
    {
        Config config;
        File configFile = ncwmsContext.getConfigFile();
        if (configFile.exists())
        {
            config = new Persister().read(Config.class, configFile);
            config.configFile = configFile;
            logger.debug("Loaded configuration from {}", configFile.getPath());
        }
        else
        {
            // We must make a new config file and save it
            config = new Config();
            config.configFile = configFile;
            config.save();
            logger.debug("Created new configuration object and saved to {}",
                configFile.getPath());
        }

        // Set up background threads to reload dataset metadata
        for (Dataset ds : config.datasets.values())
        {
            config.scheduleReloading(ds);
        }
        
        return config;
    }
    
    /**
     * Saves configuration information to the disk.  Other classes can call this
     * method when they have altered the contents of this object.
     * @throws Exception if there was an error saving the configuration
     * @throws IllegalStateException if the config file has not previously been
     * saved.
     */
    public synchronized void save() throws Exception
    {
        if (this.configFile == null)
        {
            throw new IllegalStateException("No location set for config file");
        }
        new Persister().write(this, this.configFile);
        logger.debug("Config information saved to {}", this.configFile.getPath());
    }
    
    /**
     * Checks that the data we have read are valid.  Checks that there are no
     * duplicate dataset IDs or duplicate URLs for third-party layer providers.
     */
    @Validate
    public void validate() throws PersistenceException
    {
        List<String> dsIds = new ArrayList<String>();
        for (Dataset ds : this.datasetList)
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
     * the datasets hashmap.
     * @todo load the datasets from the THREDDS catalog and
     * populate the hashmap of third-party layer providers.)
     */
    @Commit
    public void build()
    {
        for (Dataset ds : this.datasetList)
        {
            this.datasets.put(ds.getId(), ds);
        }
    }
    
    public void setLastUpdateTime(DateTime date)
    {
        if (date.isAfter(this.lastUpdateTime))
        {
            this.lastUpdateTime = date;
        }
    }

    /**
     * Schedules the regular reloading of the given dataset
     */
    private void scheduleReloading(final Dataset ds)
    {
        Runnable reloader = new Runnable() {
            @Override public void run() { ds.loadLayers(); }
        };
        ScheduledFuture<?> future = this.scheduler.scheduleWithFixedDelay(
            reloader,            // The reloading task to run
            0,                   // Schedule the first run immediately
            1, TimeUnit.SECONDS  // Schedule each subsequent run 1 second after
                                 // the previous one finished.
        );
        // We need to keep a handle to the Future object so we can cancel it
        this.futures.put(ds.getId(), future);
        logger.debug("Scheduled auto-reloading of dataset {}", ds.getId());
    }
    
    /**
     * @return the time at which this configuration was last updated
     */
    @Override
    public DateTime getLastUpdateTime()
    {
        ensure revisit: how does this get updated?  Should we search all the
        datasets each time, or allow datasets to update this value?
        return this.lastUpdateTime;
    }

    public Server getServer()
    {
        return server;
    }

    public void setServer(Server server)
    {
        this.server = server;
    }
    
    public Cache getCache()
    {
        return this.cache;
    }

    public void setCache(Cache cache)
    {
        this.cache = cache;
    }
    
    public Contact getContact()
    {
        return contact;
    }

    public void setContact(Contact contact)
    {
        this.contact = contact;
    }

    /**
     * Gets the Layer object with the given unique name.
     * @throws LayerNotDefinedException if the given name does not match a layer
     * on this server
     */
    @Override
    public Layer getLayerByUniqueName(String uniqueLayerName)
        throws LayerNotDefinedException
    {
        try
        {
            String[] els = WmsUtils.parseUniqueLayerName(uniqueLayerName);
            Dataset ds = this.datasets.get(els[0]);
            if (ds == null) throw new NullPointerException();
            Layer layer = ds.getLayer(els[1]);
            if (layer == null) throw new NullPointerException();
            return layer;
        }
        catch(Exception e)
        {
            throw new LayerNotDefinedException(uniqueLayerName);
        }
    }

    /**
     * Gets a Map of dataset IDs to Dataset objects for all datasets on this
     * server, whether or not they are ready for use.  This operation is used
     * only by the ncWMS configuration system, and hence does not appear in the
     * {@link ServerConfig} interface.
     */
    Map<String, Dataset> getAllDatasets()
    {
        return this.datasets;
    }

    /**
     * Returns an unmodifiable {@link Set} of all the {@link Dataset}s on this server that are
     * ready for use.  (Note that implementations may have other datasets
     * in the system that are not ready for use, perhaps because a lengthy
     * metadata-loading operation is in progress.  Such datasets would not
     * appear in this Set.)
     * @return a {@link Set} of all the {@link Dataset}s on this server that are
     * ready for use.
     * @todo changing to a collection would be more convenient!
     */
    @Override
    public synchronized Set<uk.ac.rdg.resc.ncwms.wms.Dataset> getDatasets()
    {
        // preserve iteration order in the set with a LinkedHashSet
        Set<uk.ac.rdg.resc.ncwms.wms.Dataset> dss =
            new LinkedHashSet<uk.ac.rdg.resc.ncwms.wms.Dataset>();
        for (Dataset ds : this.datasets.values()) dss.add(ds);
        return Collections.unmodifiableSet(dss);
    }

    /**
     * Returns the dataset with the given ID, or null if there is no available
     * dataset with the given id.
     */
    @Override
    public Dataset getDatasetById(String datasetId)
    {
        return this.datasets.get(datasetId);
    }
    
    public synchronized void addDataset(Dataset ds)
    {
        this.datasetList.add(ds);
        this.datasets.put(ds.getId(), ds);
        this.scheduleReloading(ds);
    }
    
    public synchronized void removeDataset(Dataset ds)
    {
        this.datasetList.remove(ds);
        this.datasets.remove(ds.getId());
        // Cancel the auto-reloading of this dataset
        ScheduledFuture<?> future = this.futures.remove(ds.getId());
        // We allow the reloading task to be interrupted
        if (future != null) future.cancel(true);
    }
    
    public synchronized void changeDatasetId(Dataset ds, String newId)
    {
        String oldId = ds.getId();
        this.datasets.remove(oldId);
        ScheduledFuture<?> future = this.futures.remove(oldId);
        ds.setId(newId);
        this.datasets.put(newId, ds);
        this.futures.put(newId, future);
        logger.debug("Changed dataset with ID {} to {}", oldId, newId);
    }
    
    /**
     * If s is whitespace-only or empty, returns a space, otherwise returns s.
     * This is to work around problems with the Simple XML software, which throws
     * an Exception if it tries to read an empty field from an XML file.
     */
    static String checkEmpty(String s)
    {
        if (s == null) return " ";
        s = s.trim();
        return s.equals("") ? " " : s;
    }

    @Override
    public String getTitle() {
        return this.server.getTitle();
    }

    @Override
    public String getAbstract() {
        return this.server.getAbstract();
    }

    @Override
    public int getMaxImageWidth() {
        return this.server.getMaxImageWidth();
    }

    @Override
    public int getMaxImageHeight() {
        return this.server.getMaxImageHeight();
    }

    @Override
    public Set<String> getKeywords() {
        String[] keysArray = this.server.getKeywords().split(",");
        // preserves iteration order
        Set<String> keywords = new LinkedHashSet<String>(keysArray.length);
        for (String keyword : keysArray) {
            keywords.add(keyword);
        }
        return keywords;
    }

    @Override
    public boolean getAllowsGlobalCapabilities() {
        return this.server.isAllowGlobalCapabilities();
    }

    @Override
    public String getServiceProviderUrl() {
        return this.server.getUrl();
    }

    @Override
    public String getContactName() {
        return this.contact.getName();
    }

    @Override
    public String getContactEmail() {
        return this.contact.getEmail();
    }

    @Override
    public String getContactOrganization() {
        return this.contact.getOrg();
    }

    @Override
    public String getContactTelephone() {
        return this.contact.getTel();
    }

    /** Not used. */
    public String getThreddsCatalogLocation()
    {
        return this.threddsCatalogLocation;
    }

    /** Not used. */
    public void setThreddsCatalogLocation(String threddsCatalogLocation)
    {
        this.threddsCatalogLocation = checkEmpty(threddsCatalogLocation);
    }

    /**
     * Called automatically by Spring.  When we have the application context
     * we can set the admin password in the Users object that is used by Acegi.
     * This is called after the Config object has been created.
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        // Set the admin password in the Users bean, which we'll need to
        // get from the app context
        Users users = (Users)applicationContext.getBean("users");
        if (users == null)
        {
            logger.error("Could not retrieve Users object from application context");
        }
        else
        {
            logger.debug("Setting admin password in Users object");
            users.setAdminPassword(this.server.getAdminPassword());
        }
    }
    
}
