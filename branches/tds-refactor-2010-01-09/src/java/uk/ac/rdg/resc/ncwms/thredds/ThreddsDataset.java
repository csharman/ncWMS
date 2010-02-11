/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.rdg.resc.ncwms.thredds;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joda.time.DateTime;
import ucar.nc2.dt.GridDataset;
import uk.ac.rdg.resc.ncwms.cdm.AbstractScalarLayerBuilder;
import uk.ac.rdg.resc.ncwms.cdm.CdmUtils;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.Dataset;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import uk.ac.rdg.resc.ncwms.wms.VectorLayer;

/**
 *
 * @author Jon
 */
class ThreddsDataset implements Dataset
{

    private static final ThreddsLayerBuilder THREDDS_LAYER_BUILDER = new ThreddsLayerBuilder();

    private final String id;
    private final GridDataset gd;
    private final Map<String, ThreddsLayer> scalarLayers = new LinkedHashMap<String, ThreddsLayer>();
    private final Map<String, VectorLayer> vectorLayers = new LinkedHashMap<String, VectorLayer>();

    public ThreddsDataset(String id, GridDataset gd)
    {
        this.id = id;
        this.gd = gd;
        
        // Now load the scalar layers
        CdmUtils.findAndUpdateLayers(this.gd, THREDDS_LAYER_BUILDER, this.scalarLayers);
        // Add the necessary properties
        for (ThreddsLayer layer : this.scalarLayers.values())
        {
            layer.setGridDatatype(this.gd.findGridDatatype(layer.getId()));
            layer.setDataset(this);
        }

        // Find the vector quantities
        Collection<VectorLayer> vectorLayersColl = WmsUtils.findVectorLayers(this.scalarLayers.values());
        // Add the vector quantities to the map of layers
        for (VectorLayer vecLayer : vectorLayersColl)
        {
            this.vectorLayers.put(vecLayer.getId(), vecLayer);
        }
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

    /**
     * Gets the {@link Layer} with the given {@link Layer#getId() id}.  The id
     * is unique within the dataset, not necessarily on the whole server.
     * @return The layer with the given id, or null if there is no layer with
     * the given id.
     * @todo repetitive of code in ncwms.config.Dataset: any way to refactor?
     */
    @Override
    public Layer getLayerById(String layerId)
    {
        Layer layer = this.scalarLayers.get(layerId);
        if (layer == null) layer = this.vectorLayers.get(layerId);
        return layer;
    }

    /**
     * @todo repetitive of code in ncwms.config.Dataset: any way to refactor?
     */
    @Override
    public Set<Layer> getLayers()
    {
        Set<Layer> layerSet = new LinkedHashSet<Layer>();
        layerSet.addAll(this.scalarLayers.values());
        layerSet.addAll(this.vectorLayers.values());
        return layerSet;
    }

    private static final class ThreddsLayerBuilder extends AbstractScalarLayerBuilder<ThreddsLayer>
    {

        @Override
        public ThreddsLayer newLayer(String id) {
            return new ThreddsLayer(id);
        }

        @Override
        public void setTimeValues(ThreddsLayer layer, List<DateTime> times) {
            layer.setTimeValues(times);
        }

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
