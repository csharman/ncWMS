/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.rdg.resc.ncwms.datareader;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import ucar.unidata.geoloc.LatLonPointImpl;
import uk.ac.rdg.resc.ncwms.config.Config;
import uk.ac.rdg.resc.ncwms.config.NcwmsContext;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidCrsException;
import uk.ac.rdg.resc.ncwms.metadata.InMemoryMetadataStore;
import uk.ac.rdg.resc.ncwms.metadata.Layer;
import uk.ac.rdg.resc.ncwms.metadata.MetadataStore;
import uk.ac.rdg.resc.ncwms.metadata.Regular1DCoordAxis;
import uk.ac.rdg.resc.ncwms.metadata.TimestepInfo;
import uk.ac.rdg.resc.ncwms.metadata.projection.HorizontalProjection;

/**
 *
 * @author dcrossma
 */
public class TransectDataReader extends DefaultDataReader {

    /**
     * Retrieves the data for the specified transect from the data repository
     *
     * @param pointA of the transect
     * @param pointB of the transect
     * @param date   for the transect
     * @param layer  you want from file
     * @return Collection GriddedDataElement
     * @throws java.io.IOException for the data
     */
    public Collection<uk.ac.rdg.resc.ncwms.datareader.GriddedDataElement> getTransectData(Point2D.Double pointA, Point2D.Double pointB, java.util.Date date, Layer layer) throws IOException {

        Double resolutionX = calcXResolution(pointA, pointB, layer);
        Double resolutionY = calcYResolution(pointA, pointB, layer);
        Double pixelsWidth = (Math.abs(pointB.getX() - pointA.getX())) / Math.abs(resolutionX);
        Double pixelsHeight = (Math.abs(pointB.getY() - pointA.getY())) / Math.abs(resolutionY);
        System.out.println("res " + resolutionX + " resY " + resolutionY + " width " + pixelsWidth + " height " + pixelsHeight);

        if(pixelsWidth >300){
        double[] bbox = getTransectBBox(pointA, pointB);
        double xDifference =bbox[2] - bbox[0];
        double yDifference = bbox[3] - bbox[1];

        Point2D.Double pointGridQuater  = new Point2D.Double();
        pointGridQuater.setLocation(bbox[0]+(xDifference/4), bbox[3]-(yDifference/4));

        Point2D.Double pointGridHalf  = new Point2D.Double();
        pointGridHalf.setLocation(bbox[0]+(xDifference/2), bbox[3]-(yDifference/2));

        Point2D.Double pointGrid2ndQuater  = new Point2D.Double();
        pointGrid2ndQuater.setLocation(bbox[2]-(xDifference/4), bbox[1]+(yDifference/4));

        Collection<GriddedDataElement> grid1  = this.convert(date, pointA, pointGridQuater, layer);
        Collection<GriddedDataElement> grid2  = this.convert(date, pointGridQuater, pointGridHalf, layer);
        Collection<GriddedDataElement> grid3  = this.convert(date, pointGridHalf, pointGrid2ndQuater, layer);
        Collection<GriddedDataElement> grid4  = this.convert(date, pointGrid2ndQuater, pointB, layer);

        grid1.addAll(grid2);
        grid1.addAll(grid3);
        grid1.addAll(grid4);
        return grid1;
        }
        return this.convert(date, pointA, pointB, layer);

    }

    /**
     * //todo refactor more
     * Converts data points into a collection
     *
     * @param date       for the data
     * @param pointA     of the transect
     * @param pointB     of the transect
     * @param givenLayer of the file eg 1/SST
     * @return Collection of GriddedDataElements
     */
    private Collection<uk.ac.rdg.resc.ncwms.datareader.GriddedDataElement> convert(java.util.Date date, Point2D.Double pointA, Point2D.Double pointB, Layer actualLayer) {

        Double resolutionX = null;
        Double resolutionY = null;
        //create the dataset        

        resolutionX = calcXResolution(pointA, pointB, actualLayer);
        resolutionY = calcYResolution(pointA, pointB, actualLayer);

        Date startDate = new java.util.Date();
        Double pixelsWidth = (Math.abs(pointB.getX() - pointA.getX())) / Math.abs(resolutionX);

        Double pixelsHeight = (Math.abs(pointB.getY() - pointA.getY())) / Math.abs(resolutionY);
        Double slope = (pointA.getY() - pointB.getY()) / (pointA.getX() - pointB.getX());

        List<GriddedDataElement> gridded = new ArrayList<GriddedDataElement>();

        DataReader dataReader = new DefaultDataReader();

        float[] realData = new float[0];

        try {
            boolean isRow = (Math.abs(slope) >= 1) ? true : false;
            boolean isReverse = isReverseTraversal(pointA, pointB, isRow);

            HorizontalGrid hGrid = new HorizontalGrid("EPSG:4326", pixelsWidth.intValue(), pixelsHeight.intValue(), getTransectBBox(pointA, pointB));
            realData = readActualLayerData(date, actualLayer, hGrid, dataReader, realData);

            int axisNum = (isRow) ? pixelsHeight.intValue() : pixelsWidth.intValue();
            Date forStartDate = new java.util.Date();
            for (int rownum = 0; rownum < axisNum; rownum++) {
                GriddedDataElement e = new GriddedDataElement();

                e.setDate(date);
                int p = this.calcPixel(rownum, slope, hGrid, isRow, isReverse);
                Point2D.Double latlon = pixelToLatLon(hGrid.getYAxisValues(), hGrid.getXAxisValues(), p, pixelsWidth.intValue());
                e.setLat(latlon.getY());
                e.setLon(latlon.getX());

                try {
                    e.setValue(round(realData[p], 2));
                } catch (ArrayIndexOutOfBoundsException aiob) {
                    e.setValue(Double.NaN);
                }
                e.setPixelIndex(p);
                gridded.add(e);
            }
            Date forEndDate = new java.util.Date();
            Logger.getLogger(TransectDataReader.class.getName()).log(Level.INFO, "took to iterate through rows  " + ((forEndDate.getTime() - forStartDate.getTime()) / 1000.0));

        } catch (InvalidCrsException ex) {
            Logger.getLogger(TransectDataReader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(TransectDataReader.class.getName()).log(Level.SEVERE, null, ex);
        }

        //time taken to convert data
        Date endDate = new java.util.Date();
        Logger.getLogger(TransectDataReader.class.getName()).log(Level.INFO, "took to convert data " + ((endDate.getTime() - startDate.getTime()) / 1000.0));
        return gridded;

    }

    private double[] getTransectBBox(Point2D.Double pointA, Point2D.Double pointB) {
        Date startDate = new java.util.Date();
        double maxLat = Double.NaN;
        double minLat = Double.NaN;
        double maxLon = Double.NaN;
        double minLon = Double.NaN;

        if (pointA.getX() >= pointB.getX()) {
            maxLon = pointA.getX();
            minLon = pointB.getX();
        } else {
            maxLon = pointB.getX();
            minLon = pointA.getX();
        }

        if (pointA.getY() >= pointB.getY()) {
            maxLat = pointA.getY();
            minLat = pointB.getY();
        } else {
            maxLat = pointB.getY();
            minLat = pointA.getY();
        }
        Date endDate = new java.util.Date();
        Logger.getLogger(TransectDataReader.class.getName()).log(Level.INFO, "took to calculate transect bbox " + ((endDate.getTime() - startDate.getTime()) / 1000.0));
        return new double[]{minLon, minLat, maxLon, maxLat};
    }

    private boolean isReverseTraversal(Point2D pointA, Point2D pointB, boolean isRow) {
        if (isRow) {
            return (pointA.getY() < pointB.getY()) ? true : false;
        } else {
            return (pointA.getX() > pointB.getX()) ? true : false;
        }
    }

    /**
     * Method readActualLayerData ...
     *
     * @param date        of type Date
     * @param actualLayer of type Layer
     * @param hGrid        of type HorizontalGrid
     * @param dataReader  of type DataReader
     * @param realData    of type float[]
     * @return float[]
     */
    private float[] readActualLayerData(Date date, Layer actualLayer, HorizontalGrid hGrid, DataReader dataReader, float[] realData) {
        Date startDate = new java.util.Date();
        try {

            for (TimestepInfo timestep : actualLayer.getTimesteps()) {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                df.format(timestep.getDate());
                df2.format(date);

                if (df.getCalendar().getTime().equals(df2.getCalendar().getTime())) {
                    realData = dataReader.read(timestep.getFilename(), actualLayer, timestep.getIndexInFile(), -1, hGrid);
                    Logger.getLogger(TransectDataReader.class.getName()).log(Level.INFO, "Reading data class readActualLayerData");
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        Date endDate = new java.util.Date();
        Logger.getLogger(TransectDataReader.class.getName()).log(Level.INFO, "took to read Actual Layer " + ((endDate.getTime() - startDate.getTime()) / 1000.0));
        return realData;
    }

    /**
     * Method calcYResolution
     *
     * @param pointA      of type Double
     * @param pointB      of type Double
     * @param actualLayer of type Layer
     * @return Double
     */
    private Double calcYResolution(Point2D.Double pointA, Point2D.Double pointB, Layer actualLayer) {
        Double resolutionY;

        HorizontalProjection dataProj = actualLayer.getHorizontalProjection();
        Regular1DCoordAxis axis = (Regular1DCoordAxis) actualLayer.getYaxis();
        int y1 = axis.getIndex(dataProj.latLonToProj(new LatLonPointImpl(pointA.getY(), pointA.getX())).getY());
        int y2 = axis.getIndex(dataProj.latLonToProj(new LatLonPointImpl(pointB.getY(), pointB.getX())).getY());
        resolutionY = (pointA.getY() - pointB.getY()) / (y2 - y1);
        return resolutionY;
    }

    /**
     * Method calcXResolution
     *
     * @param pointA      of type Double
     * @param pointB      of type Double
     * @param actualLayer of type Layer
     * @return Double
     */
    private Double calcXResolution(Point2D.Double pointA, Point2D.Double pointB, Layer actualLayer) {
        Double resolutionX;

        HorizontalProjection dataProj = actualLayer.getHorizontalProjection();
        Regular1DCoordAxis axis = (Regular1DCoordAxis) actualLayer.getXaxis();
        int x1 = axis.getIndex(dataProj.latLonToProj(new LatLonPointImpl(pointA.getY(), pointA.getX())).getX());
        int x2 = axis.getIndex(dataProj.latLonToProj(new LatLonPointImpl(pointB.getY(), pointB.getX())).getX());


        resolutionX = (pointA.getX() - pointB.getX()) / (x2 - x1);
        return resolutionX;
    }

    /**
     * Method readConfig
     *
     * @return Config
     */
    private Config readConfig() {
        Date startDate = new java.util.Date();
        NcwmsContext context = new NcwmsContext();
        MetadataStore store = new InMemoryMetadataStore();
        Config config = null;
        store.setNcwmsContext(context);
        try {
            store.init();
            config = Config.readConfig(context, store);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Date endDate = new java.util.Date();
        Logger.getLogger(TransectDataReader.class.getName()).log(Level.INFO, "took to read Cnfig " + ((endDate.getTime() - startDate.getTime()) / 1000.0));
        return config;
    }

    private int calcPixel(int axisnum, double slope, HorizontalGrid hGrid, boolean isRow, boolean isReverse) {
        int c;
        int widthOrHeight;

        if (isRow) {
            widthOrHeight = hGrid.getWidth();
        } else {
            widthOrHeight = hGrid.getHeight();
        }

        if (slope < 0) {
            c = 0;
        } else {
            c = widthOrHeight - 1;

        }

        if (isRow) {
            int xcoord = (int) ((axisnum / slope * -1)) + c;

            if (isReverse) {
                return (hGrid.getWidth() * hGrid.getHeight() - 1) - (hGrid.getWidth() * axisnum) - xcoord;
            }
            return ((hGrid.getWidth() * axisnum)) + xcoord;
        } else {    //y=mx + c

            int ycoord = (int) ((slope * axisnum * -1) + c);
            if (isReverse) {
                return (hGrid.getWidth() * hGrid.getHeight() - 1) - (hGrid.getWidth() * ycoord) - axisnum;
            }
            return ((hGrid.getWidth() * ycoord) + axisnum);
        }


    }

    /**
     * Method pixelToLatLon ...
     *
     * @param lats        of type double[]
     * @param lons        of type double[]
     * @param pixelIndex  of type int
     * @param pixelsWidth of type int
     * @return Double
     */
    public java.awt.geom.Point2D.Double pixelToLatLon(double[] lats, double[] lons, int pixelIndex, int pixelsWidth) {

        int r = pixelIndex / pixelsWidth;
        int c = pixelIndex % pixelsWidth;
        Point2D.Double latLon = new Point2D.Double();

        if (r >= lats.length) {
            r = lats.length - 1;
        }

        double lat = lats[r];
        double lon = lons[c];

        latLon.setLocation(lon, lat);

        return latLon;


    }

    /**
     * formats the data into a series to be displayed on a chart
     *
     * @param data    for the series
     * @param strDate for the data
     * @return XYSeries of the data for a given date
     */
    public XYSeries formatChartData(Collection data, Layer layer) {

        XYSeries s = new XYSeries("Transect for " + layer.getLayerName(), true);

        boolean hasData = false;

        int index = 0;
        for (Object aData : data) {
            GriddedDataElement element = (GriddedDataElement) aData;

             if (element.getValue() > 0) {
                hasData = true;
                s.add(new XYDataItem(index, element.getValue()));

            } else {
                s.add(new XYDataItem(index, null));
            }

            index++;
        }
        return (hasData) ? s : null;

    }

    /**
     * rounds a value to the given decimal place
     *
     * @param val    to be rounded
     * @param places to be rounded to
     * @return float rounded for the given value
     */
    private double round(float val, int places) {
        long factor = (long) Math.pow(10, places);

        val = val * factor;
        long tmp = Math.round(val);
        return (double) tmp / factor;
    }
}
