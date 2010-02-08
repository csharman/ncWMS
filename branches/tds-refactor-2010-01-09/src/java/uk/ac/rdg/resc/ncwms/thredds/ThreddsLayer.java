/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.rdg.resc.ncwms.thredds;

import java.io.IOException;
import java.util.List;
import org.joda.time.DateTime;
import org.opengis.metadata.extent.GeographicBoundingBox;
import ucar.nc2.dt.GridDatatype;
import uk.ac.rdg.resc.ncwms.coords.HorizontalCoordSys;
import uk.ac.rdg.resc.ncwms.coords.HorizontalPosition;
import uk.ac.rdg.resc.ncwms.coords.PointList;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.graphics.ColorPalette;
import uk.ac.rdg.resc.ncwms.util.Range;
import uk.ac.rdg.resc.ncwms.wms.Dataset;
import uk.ac.rdg.resc.ncwms.wms.ScalarLayer;

/**
 * Wraps a GridDatatype as a ScalarLayer object
 * @author Jon
 */
class ThreddsLayer implements ScalarLayer {

    private String id;
    private GridDatatype grid;
    private ThreddsDataset dataset;

    public ThreddsLayer(String id, GridDatatype grid, ThreddsDataset dataset)
    {
        this.id = id;
        this.grid = grid;
        this.dataset = dataset;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public Dataset getDataset() {
        return this.dataset;
    }

    @Override
    public Float readSinglePoint(DateTime time, double elevation, HorizontalPosition xy) throws InvalidDimensionValueException, IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Float> readPointList(DateTime time, double elevation, PointList pointList) throws InvalidDimensionValueException, IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Float> readTimeseries(List<DateTime> times, double elevation, HorizontalPosition xy) throws InvalidDimensionValueException, IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getTitle() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getAbstract() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getUnits() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isQueryable() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public GeographicBoundingBox getGeographicBoundingBox() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public HorizontalCoordSys getHorizontalCoordSys() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<DateTime> getTimeValues() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DateTime getCurrentTimeValue() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DateTime getDefaultTimeValue() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Double> getElevationValues() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getDefaultElevationValue() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getElevationUnits() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isElevationPositive() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Range<Float> getApproxValueRange() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isLogScaling() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ColorPalette getDefaultColorPalette() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
