/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.rdg.resc.ncwms.thredds;

import java.io.IOException;
import java.util.List;
import org.joda.time.DateTime;
import ucar.nc2.dt.GridDatatype;
import uk.ac.rdg.resc.ncwms.coords.HorizontalPosition;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.util.Range;
import uk.ac.rdg.resc.ncwms.wms.AbstractScalarLayer;
import uk.ac.rdg.resc.ncwms.wms.Dataset;

/**
 * Wraps a GridDatatype as a ScalarLayer object
 * @author Jon
 */
class ThreddsLayer extends AbstractScalarLayer
{
    private GridDatatype grid;
    private ThreddsDataset dataset;
    private List<DateTime> times;

    public ThreddsLayer(String id)
    {
        super(id);
    }

    @Override
    public Dataset getDataset() { return this.dataset; }
    public void setDataset(ThreddsDataset dataset) { this.dataset = dataset; }

    public void setGridDatatype(GridDatatype grid) { this.grid = grid; }

    /** Returns true: THREDDS layers are always queryable through GetFeatureInfo */
    @Override
    public boolean isQueryable() { return true; }

    @Override
    public List<DateTime> getTimeValues() {
        return this.times;
    }

    public void setTimeValues(List<DateTime> timeValues) {
        this.times = timeValues;
    }

    @Override
    public Float readSinglePoint(DateTime time, double elevation, HorizontalPosition xy) throws InvalidDimensionValueException, IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Range<Float> getApproxValueRange() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
