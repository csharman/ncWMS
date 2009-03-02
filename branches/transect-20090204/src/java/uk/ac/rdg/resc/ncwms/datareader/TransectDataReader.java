/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.rdg.resc.ncwms.datareader;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.ProjectionPoint;
import uk.ac.rdg.resc.ncwms.config.Config;
import uk.ac.rdg.resc.ncwms.config.Dataset;
import uk.ac.rdg.resc.ncwms.config.NcwmsContext;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidCrsException;
import uk.ac.rdg.resc.ncwms.metadata.InMemoryMetadataStore;
import uk.ac.rdg.resc.ncwms.metadata.Layer;
import uk.ac.rdg.resc.ncwms.metadata.LayerImpl;
import uk.ac.rdg.resc.ncwms.metadata.MetadataStore;
import uk.ac.rdg.resc.ncwms.metadata.Regular1DCoordAxis;
import uk.ac.rdg.resc.ncwms.metadata.TimestepInfo;
import uk.ac.rdg.resc.ncwms.metadata.TwoDCoordAxis;
import uk.ac.rdg.resc.ncwms.metadata.projection.HorizontalProjection;

/**
 *
 * @author dcrossma
 */
public class TransectDataReader extends DefaultDataReader{

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
    public Collection getTransectData(Point2D.Double pointA, Point2D.Double pointB, java.util.Date date, String layer) throws IOException {
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
    private Collection convert(java.util.Date date, Point2D.Double pointA, Point2D.Double pointB, String givenLayer) {

        Double resolutionX = null;
        Double resolutionY = null;
        //create the dataset
        Layer actualLayer = null;

        Config config = readConfig();

        Map<String, Dataset> datasets = config.getDatasets();

        Calendar transectDate = Calendar.getInstance();
        transectDate.setTime(date);

        Map<String, LayerImpl> layers = new HashMap<String, LayerImpl>();
        for (Map.Entry<String, Dataset> entry : datasets.entrySet()) {
            Dataset set = entry.getValue();

            DefaultDataReader reader = new DefaultDataReader();
            
                try {
                    layers = reader.getAllLayers(set);
                } catch (Exception ex) {
                    Logger.getLogger(TransectDataReader.class.getName()).log(Level.SEVERE, null, ex);
                }
           
            for (Map.Entry<String, LayerImpl> entryLayers : layers.entrySet()) {
                LayerImpl currentLayer = (LayerImpl) entryLayers.getValue();
                if (givenLayer.equals(set.getId() + "/" + currentLayer.getId())) {

                    currentLayer.setDataset(set);
                    actualLayer = currentLayer;

                    resolutionX = calcXResolution(pointA, pointB, actualLayer);
                    resolutionY = calcYResolution(pointA, pointB, actualLayer);
                }
            }
        }

        Date startDate = new java.util.Date();
        Double pixelsWidth = (Math.abs(pointB.getX() - pointA.getX())) / Math.abs(resolutionX);
        Double pixelsHeight = (Math.abs(pointB.getY() - pointA.getY())) / Math.abs(resolutionY);
        Double slope = (pointA.getY() - pointB.getY()) / (pointA.getX() - pointB.getX());

        double deltaLat = deltaLat(pixelsHeight.intValue(), pointA, pointB);
        double deltaLon = deltaLon(pixelsWidth.intValue(), pointA, pointB);
        List<GriddedDataElement> gridded = new ArrayList<GriddedDataElement>();

        double[] lats = new double[pixelsHeight.intValue()];
        double[] lons = new double[pixelsWidth.intValue()];

        for (int i = 0; i < pixelsHeight.intValue(); i++) {
            lats[i] = truncate((pointA.getY() + (i * deltaLat)));
        }

        for (int i = 0; i < pixelsWidth.intValue(); i++) {
            lons[i] = truncate((pointA.getX() + (i * deltaLon)));
        }


        DataReader dataReader = new DefaultDataReader();

        float[] realData = new float[0];
        try {
            HorizontalGrid hGrid = new HorizontalGrid("EPSG:4326", pixelsWidth.intValue(), pixelsHeight.intValue(), new double[]{pointA.getX(),pointA.getY(),pointB.getX(),pointB.getY()});
            realData = readActualLayerData(date, actualLayer, hGrid, dataReader, realData);
        } catch (InvalidCrsException ex) {
            Logger.getLogger(TransectDataReader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(TransectDataReader.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("slopoe is "+Math.abs(slope));
        boolean isRow = (Math.abs(slope) >= 1) ? true : false;
        System.out.println("isNeg "+Math.signum(slope));
        boolean isNegative  = (Math.signum(slope)==-1.0)?true : false;

        int axisNum = (isRow) ? pixelsHeight.intValue() : pixelsWidth.intValue();

        for (int rownum = 0; rownum < axisNum; rownum++) {
            GriddedDataElement e = new GriddedDataElement();

            e.setDate(date);

            int p = this.calcPixel(rownum, slope, pixelsWidth.intValue(), pixelsHeight.intValue(), isRow,isNegative);
            Point2D.Double latlon = pixelToLatLon(lats, lons, p, pixelsWidth.intValue());
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

        //time taken to convert data
        Date endDate = new java.util.Date();
        System.out.println("took to convert data " + ((endDate.getTime() - startDate.getTime()) / 1000.0));
        return gridded;

    }

    /**
     * Method readActualLayerData ...
     *
     * @param date        of type Date
     * @param actualLayer of type Layer
     * @param lats        of type double[]
     * @param lons        of type double[]
     * @param dataReader  of type DataReader
     * @param realData    of type float[]
     * @return float[]
     */
    private float[] readActualLayerData(Date date, Layer actualLayer, HorizontalGrid hGrid, DataReader dataReader, float[] realData) {
        try {

            for (TimestepInfo timestep : actualLayer.getTimesteps()) {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                df.format(timestep.getDate());
                df2.format(date);

                if (df.getCalendar().getTime().equals(df2.getCalendar().getTime())) {
                    realData = dataReader.read(timestep.getFilename(), actualLayer, timestep.getIndexInFile(), 0, hGrid);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
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
        Regular1DCoordAxis axis  =(Regular1DCoordAxis)actualLayer.getYaxis();

//        int y1 = axis.getIndex(dataProj.latLonToProj(new LatLonPointImpl(pointA.getY(), pointA.getX())));
//        int y2 = axis.getIndex(dataProj.latLonToProj(new LatLonPointImpl(pointB.getY(), pointB.getX())));
        int y1 = axis.getIndex(4);
        int y2 = axis.getIndex(5);
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
        Regular1DCoordAxis axis  =(Regular1DCoordAxis)actualLayer.getYaxis();
       // int x1 = axis.getIndex(dataProj.latLonToProj(new LatLonPointImpl(pointA.getY(), pointA.getX())));
       // int x2 = axis.getIndex(dataProj.latLonToProj(new LatLonPointImpl(pointB.getY(), pointB.getX())));
        int x1 = axis.getIndex(5);
        int x2 = axis.getIndex(4);
        resolutionX = (pointA.getX() - pointB.getX()) / (x2 - x1);
        return resolutionX;
    }

    /**
     * Method readConfig
     *
     * @return Config
     */
    private Config readConfig() {
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
        return config;
    }


    /**
     * Method calcPixel ...
     *
     * @param axisnum of type int
     * @param slope   of type double
     * @param width   of type int
     * @param height  of type int
     * @param isRow   of type boolean
     * @return int
     */
    private int calcPixel(int axisnum, double slope, int width, int height, boolean isRow,boolean isNegative) {

        int c;
        int widthOrHeight;

        if (isRow) {
            widthOrHeight = width;
        } else {
            widthOrHeight = height;
        }

        if(isNegative){
        slope = slope * -1.0;
        }


        if (slope > 0) {
            c = 0;
        } else {
            c = widthOrHeight - 1;

        }

        if (isRow) {
            int xcoord = (int) ((axisnum / slope) + (c));
            return (widthOrHeight * axisnum) + xcoord;
        } else {    //y=mx + c

            int ycoord = (int) ((slope * axisnum) + c);
            //System.out.println("slope is "+slope+ " axisNum is "+axisnum +" c is "+c);
            return ((width * ycoord) + axisnum);
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
     * calculates the deltaLat
     *
     * @param numPixels for the lat
     * @param pointA    of the transect
     * @param pointB    of the transect
     * @return double deltaLat
     */
    private double deltaLat(int numPixels, Point2D.Double pointA, Point2D.Double pointB) {

        return (pointB.getY() - pointA.getY()) / (numPixels - 1);

    }

    /**
     * calculates the deltaLon
     *
     * @param numPixels for the lon
     * @param pointA    of the transect
     * @param pointB    of the transect
     * @return double deltaLat
     */
    private double deltaLon(int numPixels, Point2D.Double pointA, Point2D.Double pointB) {

        return (pointB.getX() - pointA.getX()) / (numPixels - 1);

    }

    /**
     * formats the data into a series to be displayed on a chart
     *
     * @param data    for the series
     * @param strDate for the data
     * @return XYSeries of the data for a given date
     */
    public XYSeries formatChartData(Collection data, String strDate) {

        XYSeries s = new XYSeries("transect for " + strDate, true);

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

    /**
     * truncates a value two decimal places
     *
     * @param val to be truncated
     * @return double truncated for the given value
     */
    private double truncate(double val) {

        if (val > 0) {
            return Math.floor(val * 100.0) / 100.0;
        } else {
            return Math.ceil(val * 100.0) / 100.0;
        }
    }

}
