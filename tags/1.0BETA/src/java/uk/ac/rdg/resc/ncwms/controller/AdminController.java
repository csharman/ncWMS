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

package uk.ac.rdg.resc.ncwms.controller;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;
import uk.ac.rdg.resc.ncwms.config.Config;
import uk.ac.rdg.resc.ncwms.config.Contact;
import uk.ac.rdg.resc.ncwms.config.Dataset;
import uk.ac.rdg.resc.ncwms.config.NcwmsContext;
import uk.ac.rdg.resc.ncwms.config.Server;
import uk.ac.rdg.resc.ncwms.metadata.MetadataLoader;

/**
 * Displays the administrative pages of the ncWMS application (i.e. /admin/*)
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class AdminController extends MultiActionController
{
    // These will be injected by Spring
    private MetadataLoader metadataLoader;
    private Config config;
    
    /**
     * Displays the administrative web page
     */
    public ModelAndView displayAdminPage(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        return new ModelAndView("admin", "config", this.config);
    }
    
    /**
     * Displays the errors associated with a particular dataset
     */
    public ModelAndView displayErrorPage(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        // Get the dataset id
        String datasetId = request.getParameter("dataset");
        if (datasetId == null)
        {
            throw new Exception("Must provide a dataset id");
        }
        Dataset dataset = this.config.getDatasets().get(datasetId);
        if (dataset == null)
        {
            throw new Exception("There is no dataset with id " + datasetId);
        }
        return new ModelAndView("admin_error", "dataset", dataset);
    }
    
    /**
     * Handles the submission of new configuration information from admin_index.jsp
     */
    public ModelAndView updateConfig(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        Contact contact = this.config.getContact();
        Server server = this.config.getServer();

        if (request.getParameter("contact.name") != null)
        {
            contact.setName(request.getParameter("contact.name"));
            contact.setOrg(request.getParameter("contact.org"));
            contact.setTel(request.getParameter("contact.tel"));
            contact.setEmail(request.getParameter("contact.email"));

            // Process the server details
            server.setTitle(request.getParameter("server.title"));
            server.setAbstract(request.getParameter("server.abstract"));
            server.setKeywords(request.getParameter("server.keywords"));
            server.setUrl(request.getParameter("server.url"));
            server.setMaxImageWidth(Integer.parseInt(request.getParameter("server.maximagewidth")));
            server.setMaxImageHeight(Integer.parseInt(request.getParameter("server.maximageheight")));

            // Save the dataset information, checking for removals
            // First look through the existing datasets for edits.
            List<Dataset> datasetsToRemove = new ArrayList<Dataset>();
            for (Dataset ds : this.config.getDatasets().values())
            {
                boolean refreshDataset = false;
                if (request.getParameter("dataset." + ds.getId() + ".remove") != null)
                {
                    // We don't do the actual removal here because we get a
                    // ConcurrentModificationException for the hashmap
                    datasetsToRemove.add(ds);
                }
                else
                {
                    ds.setTitle(request.getParameter("dataset." + ds.getId() + ".title"));
                    String newLocation = request.getParameter("dataset." + ds.getId() + ".location");
                    if (!newLocation.trim().equals(ds.getLocation().trim()))
                    {
                        refreshDataset = true;
                    }
                    ds.setLocation(newLocation);
                    String newDataReaderClass = request.getParameter("dataset." + ds.getId() + ".reader");
                    if (!newDataReaderClass.trim().equals(ds.getDataReaderClass().trim()))
                    {
                        refreshDataset = true;
                    }
                    ds.setDataReaderClass(newDataReaderClass);
                    ds.setQueryable(request.getParameter("dataset." + ds.getId() + ".queryable") != null);
                    ds.setUpdateInterval(Integer.parseInt(request.getParameter("dataset." + ds.getId() + ".updateinterval")));
                    ds.setId(request.getParameter("dataset." + ds.getId() + ".id"));
                    if (request.getParameter("dataset." + ds.getId() + ".refresh") != null)
                    {
                        refreshDataset = true;
                    }
                }
                if (refreshDataset)
                {
                    this.metadataLoader.forceReloadMetadata(ds);
                }
            }
            // Now we can remove the datasets
            for (Dataset ds : datasetsToRemove)
            {
                config.removeDataset(ds);
            }
            // Now look for the new datasets. This logic means that we don't have
            // to know in advance how many new datasets the user has created (or
            // how many spaces were available in admin_index.jsp)
            int i = 0;
            while (request.getParameter("dataset.new" + i + ".id") != null)
            {
                // Look for non-blank ID fields
                if (!request.getParameter("dataset.new" + i + ".id").trim().equals(""))
                {
                    Dataset ds = new Dataset();
                    ds.setId(request.getParameter("dataset.new" + i + ".id"));
                    ds.setTitle(request.getParameter("dataset.new" + i + ".title"));
                    ds.setLocation(request.getParameter("dataset.new" + i + ".location"));
                    ds.setDataReaderClass(request.getParameter("dataset.new" + i + ".reader"));
                    ds.setQueryable(request.getParameter("dataset.new" + i + ".queryable") != null);
                    ds.setUpdateInterval(Integer.parseInt(request.getParameter("dataset.new" + i + ".updateinterval")));
                    config.addDataset(ds);
                    this.metadataLoader.forceReloadMetadata(ds);
                }
                i++;
            }
            
            // Set the location of the THREDDS catalog if it has changed
            String newThreddsCatalogLocation = request.getParameter("thredds.catalog.location");
            if (!config.getThreddsCatalogLocation().trim().equals(newThreddsCatalogLocation))
            {
                config.setThreddsCatalogLocation(newThreddsCatalogLocation);
                // Reload Thredds datasets in a new thread
                new Thread()
                {
                    public void run()
                    {
                        config.loadThreddsCatalog();
                    }
                }.start();
            }

            // Save the updated config information to disk
            this.config.save();
        }
        
        // This causes a client-side redirect, meaning that the user can safely
        // press refresh in their browser without resubmitting the new config information.
        return new ModelAndView("postConfigUpdate");
    }
    
    /**
     * Called by Spring to inject the metadata loading object
     */
    public void setMetadataLoader(MetadataLoader metadataLoader)
    {
        this.metadataLoader = metadataLoader;
    }
    
    /**
     * Called by Spring to inject the context containing method to save the
     * configuration information
     */
    public void setConfig(Config config)
    {
        this.config = config;
    }
    
}
