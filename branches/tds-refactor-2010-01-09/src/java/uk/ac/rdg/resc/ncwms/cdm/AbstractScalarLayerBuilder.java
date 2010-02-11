/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.rdg.resc.ncwms.cdm;

import java.util.List;
import org.opengis.metadata.extent.GeographicBoundingBox;
import uk.ac.rdg.resc.ncwms.coords.HorizontalCoordSys;
import uk.ac.rdg.resc.ncwms.wms.AbstractScalarLayer;

/**
 *
 * @author Jon
 */
public abstract class AbstractScalarLayerBuilder<L extends AbstractScalarLayer> implements LayerBuilder<L> {

    @Override
    public void setTitle(L layer, String title) {
        layer.setTitle(title);
    }

    @Override
    public void setAbstract(L layer, String abstr) {
        layer.setAbstract(abstr);
    }

    @Override
    public void setGeographicBoundingBox(L layer, GeographicBoundingBox bbox) {
        layer.setGeographicBoundingBox(bbox);
    }

    @Override
    public void setUnits(L layer, String units) {
        layer.setUnits(units);
    }

    @Override
    public void setHorizontalCoordSys(L layer, HorizontalCoordSys coordSys) {
        layer.setHorizontalCoordSys(coordSys);
    }

    @Override
    public void setElevationAxis(L layer, List<Double> zValues, boolean zPositive, String zUnits) {
        layer.setElevationValues(zValues);
        layer.setElevationPositive(zPositive);
        layer.setElevationUnits(zUnits);
    }

}
