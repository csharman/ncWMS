/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.rdg.resc.ncwms.cdm;

import java.util.List;
import org.joda.time.DateTime;
import org.opengis.metadata.extent.GeographicBoundingBox;
import uk.ac.rdg.resc.ncwms.coords.HorizontalCoordSys;
import uk.ac.rdg.resc.ncwms.wms.Layer;

/**
 *
 * @author Jon
 */
public interface LayerBuilder<L extends Layer> {

    public L newLayer(String id);

    public void setTitle(L layer, String title);

    public void setAbstract(L layer, String abstr);

    public void setGeographicBoundingBox(L layer, GeographicBoundingBox bbox);

    public void setUnits(L layer, String units);

    public void setHorizontalCoordSys(L layer, HorizontalCoordSys coordSys);

    public void setElevationAxis(L layer, List<Double> zValues, boolean zPositive, String zUnits);

    public void setTimeValues(L layer, List<DateTime> times);

}
