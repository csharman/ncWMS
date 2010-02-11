/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.rdg.resc.ncwms.thredds;

import java.io.IOException;
import java.util.List;
import org.joda.time.DateTime;
import ucar.nc2.dt.GridDatatype;
import uk.ac.rdg.resc.ncwms.cdm.CdmUtils;
import uk.ac.rdg.resc.ncwms.cdm.DataReadingStrategy;
import uk.ac.rdg.resc.ncwms.coords.HorizontalPosition;
import uk.ac.rdg.resc.ncwms.coords.PointList;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.util.Range;
import uk.ac.rdg.resc.ncwms.wms.AbstractScalarLayer;
import uk.ac.rdg.resc.ncwms.wms.Dataset;

/**
 * Wraps a GridDatatype as a ScalarLayer object
 * @todo Implement more efficient getTimeseries()
 * @todo Decide upon data reading strategy more intelligently (requires access
 * to the type of the underlying data)
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
    public Float readSinglePoint(DateTime time, double elevation, HorizontalPosition xy)
            throws InvalidDimensionValueException, IOException
    {
        PointList singlePoint = PointList.fromPoint(xy);
        return this.readPointList(time, elevation, singlePoint).get(0);
    }

    @Override
    public List<Float> readPointList(DateTime time, double elevation, PointList pointList)
            throws InvalidDimensionValueException, IOException
    {
        int tIndex = this.findAndCheckTimeIndex(time);
        int zIndex = this.findAndCheckElevationIndex(elevation);
        // TODO: take into account case in which data are/are not enhanced!
        return CdmUtils.readPointList(
            this.grid,
            this.getHorizontalCoordSys(),
            tIndex,
            zIndex,
            pointList,
            DataReadingStrategy.BOUNDING_BOX  // TODO: decide more intelligently
        );
    }

    @Override
    public Range<Float> getApproxValueRange() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
