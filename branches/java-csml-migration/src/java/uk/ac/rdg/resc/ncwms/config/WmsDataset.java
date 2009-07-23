/*
 * Copyright (c) 2009 The University of Reading
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

import java.util.Collection;

import org.jcsml.ncutils.config.Dataset;
import org.jcsml.ncutils.metadata.Layer;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extend Dataset to add WMS specific functionality - to deal with the data cache
 * and refreshing of this.
 * 
 * @author C Byrom, Tessella
 *
 */
public class WmsDataset extends Dataset
{
    private static final Logger logger = LoggerFactory.getLogger(Dataset.class);
    
    private Config config;   // The Config object to which this belongs

    /**
     * @return true if the metadata from this dataset needs to be reloaded
     * automatically via the periodic reloader in MetadataLoader.  Note that this
     * does something more sophisticated than simply checking that
     * this.state == NEEDS_REFRESH!
     */
    public boolean needsRefresh()
    {
        DateTime lastUpdate = this.getLastUpdate();
        logger.debug("Last update time for dataset {} is {}", this.getId(), lastUpdate);
        logger.debug("State of dataset {} is {}", this.getId(), this.getState());
        logger.debug("Disabled = {}", this.isDisabled());
        if (this.isDisabled() || this.getState() == State.SCHEDULED ||
            this.getState() == State.LOADING || this.getState() == State.UPDATING)
        {
            return false;
        }
        else if (this.getState() == State.ERROR || 
        		this.getState() == State.NEEDS_REFRESH || 
        		lastUpdate == null)
        {
            return true;
        }
        else if (this.getUpdateInterval() < 0)
        {
            return false; // We never update this dataset
        }
        else
        {
            // State = READY.  Check the age of the metadata
            // Return true if we are after the next scheduled update
            return new DateTime().isAfter(lastUpdate.plusMinutes(this.getUpdateInterval()));
        }
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
    @Override
	public DateTime getLastUpdate()
    {
        return this.config.getMetadataStore().getLastUpdateTime(this.getId());
    }
    
    /**
     * @return a Collection of all the layers in this dataset.  A convenience
     * method that reads from the metadata store.
     * @throws Exception if there was an error reading from the store.
     */
    public Collection<? extends Layer> getLayers() throws Exception
    {
        return this.config.getMetadataStore().getLayersInDataset(this.getId());
    }



}
