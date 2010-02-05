/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.rdg.resc.ncwms.thredds;

import java.util.Map;
import java.util.Set;
import org.joda.time.DateTime;
import ucar.nc2.dt.GridDataset;
import uk.ac.rdg.resc.ncwms.wms.AbstractServerConfig;
import uk.ac.rdg.resc.ncwms.wms.Dataset;

/**
 * @author Jon
 */
public class ThreddsServerConfig extends AbstractServerConfig {

    /**
     * Returns false: THREDDS servers can't produce a capabilities document
     * containing all datasets.
     */
    @Override
    public boolean getAllowsGlobalCapabilities() {
        return false;
    }

    /**
     * Returns null: THREDDS servers can't produce a collection of all the
     * datasets they hold
     */
    @Override
    public Map<String, ? extends Dataset> getAllDatasets() {
        return null;
    }

    /**
     * Returns the current time.  THREDDS servers don't cache their metadata
     * so the datasets could change at any time.  This effectively means that
     * clients should not cache Capabilities documents from THREDDS servers for
     * any "significant" period of time, to prevent inconsistencies between
     * client and server.
     */
    @Override
    public DateTime getLastUpdateTime() {
        return new DateTime();
    }

    @Override
    public Dataset getDatasetById(String datasetId) {
       GridDataset gd = this.getGridDataset(datasetId);
       return new ThreddsDataset(datasetId, gd);
    }

    /**
     * Return the GridDataset with the given id, or null if there is no dataset
     * with the given id.
     */
    private GridDataset getGridDataset(String datasetId) {
        throw new UnsupportedOperationException("Implement me!");
    }
    
    
    //// The methods below should be easily populated from existing THREDDS
    //// metadata or the OGCMeta.xml file

    @Override
    public String getTitle() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getMaxImageWidth() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getMaxImageHeight() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getAbstract() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<String> getKeywords() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getServiceProviderUrl() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getContactName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getContactOrganization() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getContactTelephone() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getContactEmail() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
