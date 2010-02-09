/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.rdg.resc.ncwms.thredds;

import java.util.LinkedHashSet;
import java.util.Set;
import org.joda.time.DateTime;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDataset.Gridset;
import uk.ac.rdg.resc.ncwms.wms.Dataset;
import uk.ac.rdg.resc.ncwms.wms.Layer;

/**
 *
 * @author Jon
 */
class ThreddsDataset implements Dataset {

    private String id;
    private GridDataset gd;

    public ThreddsDataset(String id, GridDataset gd)
    {
        this.id = id;
        this.gd = gd;
    }

    /** Returns the ID of this dataset, unique on the server. */
    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getTitle() {
        return this.gd.getTitle();
    }

    /**
     * Returns the current time, since datasets could change at any time without
     * our knowledge.
     * @see ThreddsServerConfig#getLastUpdateTime()
     */
    @Override
    public DateTime getLastUpdateTime() {
        return new DateTime();
    }

    @Override
    public Layer getLayerById(String layerId) {
        return null; // TODO
    }

    @Override
    public Set<Layer> getLayers() {
        Set<Layer> layers = new LinkedHashSet<Layer>();

        for (Gridset gridset : this.gd.getGridsets())
        {

        }
        //Collection<VectorLayer> vecLayers = WmsUtils.findVectorLayers(layers.values());
        
        return layers;
    }

    /** Returns an empty string */
    @Override
    public String getCopyrightStatement() {
        return "";
    }

    /** Returns an empty string */
    @Override
    public String getMoreInfoUrl() {
        return "";
    }

    @Override
    public boolean isReady() { return true; }

    @Override
    public boolean isLoading() { return false; }

    @Override
    public boolean isError() { return false; }

    @Override
    public Exception getException() { return null; }

    @Override
    public boolean isDisabled() { return false; }

}
