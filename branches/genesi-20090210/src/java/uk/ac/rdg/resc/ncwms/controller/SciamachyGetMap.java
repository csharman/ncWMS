/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.rdg.resc.ncwms.controller;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.awt.image.ImageProducer;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.rdg.resc.ncwms.datareader.sciamachy.SciamachySwath;
import uk.ac.rdg.resc.ncwms.datareader.sciamachy.SciamachySwath.GroundPixel;
import uk.ac.rdg.resc.ncwms.datareader.sciamachy.SciamachySwath.LonLat;
import uk.ac.rdg.resc.ncwms.datareader.sciamachy.SciamachySwath.Retrieval;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidFormatException;
import uk.ac.rdg.resc.ncwms.exceptions.StyleNotDefinedException;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;
import uk.ac.rdg.resc.ncwms.styles.ColorPalette;
import uk.ac.rdg.resc.ncwms.utils.WmsUtils;

/**
 * Hacked-together class to produce an image of Sciamachy data in response
 * to a GetMap query
 * @author Jon
 */
class SciamachyGetMap {
    
    private static final Logger logger = LoggerFactory.getLogger(SciamachyGetMap.class);

    private static File DATA_DIR = new File("C:\\Documents and Settings\\Jon\\Desktop\\ESA");

    private static class DataFile {
        private Interval timeRange;
        private File file;
    }

    private static List<DataFile> DATA_FILES = new ArrayList<DataFile>();

    /**
     * Gets an Interval representing the overall time bounds of data.
     * @return
     */
    private static Interval getTimeBounds() {
        return new Interval (
            DATA_FILES.get(0).timeRange.getStart(),
            DATA_FILES.get(DATA_FILES.size() - 1).timeRange.getEnd().plusMillis(1)
        );
    }

    /**
     * Loads the temporal extents of all the data files
     */
    static {
        // Look for all the .txt files
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".txt");
            }
        };
        for (File f : DATA_DIR.listFiles(filter)) {
            SciamachySwath swath = null;
            try {
                swath = SciamachySwath.fromFile(f.getPath());
            } catch (IOException ioe) {
                throw new ExceptionInInitializerError(ioe);
            }
            List<Retrieval> retrievals = swath.getRetrievals();
            DataFile df = new DataFile();
            df.file = f;
            // The interval is exclusive of the end time so we add 1ms to the end
            // time so that it includes all retrievals.
            df.timeRange = new Interval(
                retrievals.get(0).getDateTime(),
                retrievals.get(retrievals.size() - 1).getDateTime().plusMillis(1)
            );
            DATA_FILES.add(df);
        }
        logger.debug("Found {} Sciamachy data files", DATA_FILES.size());
    }

    public SciamachyGetMap() { throw new AssertionError(); }

    public static void renderMap(GetMapRequest getMap, HttpServletResponse response)
        throws WmsException, IOException {

        // Get the time range requested
        String timeString = getMap.getDataRequest().getTimeString();
        Interval timeInterval = getTimeInterval(timeString);

        if (!getTimeBounds().overlaps(timeInterval)) {
            throw new InvalidDimensionValueException("time", timeString);
        }

        // Find the file(s) that match this time interval and bounding box
        // TODO: in the final system this will search the GENESI-DR system
        List<File> dataFiles = findDataFiles(timeInterval);

        // Get the ColorPalette requested.  This is simply the value of
        // the STYLES parameter
        String[] styles = getMap.getStyleRequest().getStyles();
        String paletteName = styles.length == 0
            ? ColorPalette.DEFAULT_PALETTE_NAME
            : styles[0];
        ColorPalette pal = ColorPalette.get(paletteName);
        if (pal == null) {
            throw new StyleNotDefinedException(paletteName);
        }
        // Convert this palette to an indexed colour model, using the client's
        // request parameters.
        IndexColorModel indexColorModel = pal.getColorModel(
            getMap.getStyleRequest().getNumColourBands(),
            getMap.getStyleRequest().getOpacity(),
            getMap.getStyleRequest().getBackgroundColour(),
            getMap.getStyleRequest().isTransparent()
        );
        
        // Create a BufferedImage to hold the image pixels
        BufferedImage im = new BufferedImage(
            getMap.getDataRequest().getWidth(),
            getMap.getDataRequest().getHeight(),
            BufferedImage.TYPE_INT_ARGB
        );
        
        // Add data from each of the matching files to the image
        Graphics2D g2d = (Graphics2D)im.getGraphics();
        for (File f : dataFiles) {
            addData(f, g2d, timeInterval, getMap, indexColorModel);
        }

        // Send the image to the client
        String mimeType = getMap.getStyleRequest().getImageFormat();
        writeImage(im, mimeType, response);
    }

    /**
     * Gets the joda-time Interval corresponding with the given time specification
     * string.  The Interval is inclusive of the beginning and exclusive of the
     * end, therefore we add 1ms to the end time that the client specifies so
     * that effectively the interval is inclusive of the specified end time.
     * @param timeString
     * @return
     * @throws WmsException if the time string is invalid
     */
    private static Interval getTimeInterval(String timeString) throws WmsException {
        if (timeString == null) {
            throw new InvalidDimensionValueException("time", "null");
        }
        // We expect a time interval, separated by a slash (start/stop)
        String[] startStop = timeString.split("/");
        if (startStop.length > 2) {
            throw new InvalidDimensionValueException("time", timeString);
        }
        try {
            long start = WmsUtils.ISO_DATE_TIME_FORMATTER.parseMillis(startStop[0]);
            long end = startStop.length == 2
                ? WmsUtils.ISO_DATE_TIME_FORMATTER.parseMillis(startStop[1])
                : start;
            return new Interval(start, end + 1); // add 1ms to make this an inclusive interval
        } catch (IllegalArgumentException iae) {
            // Date/time string is invalid
            throw new InvalidDimensionValueException("time", timeString);
        }
    }

    /**
     * Searches the list of available data files for files that contain data
     * within the provided time interval.
     * @param timeInterval
     * @return
     */
    private static List<File> findDataFiles(Interval timeInterval) {
        logger.debug("Searching for Sciamachy files in time interval {}", timeInterval);
        List<File> dataFiles = new ArrayList<File>();
        for (DataFile dataFile : DATA_FILES) {
            if (dataFile.timeRange.overlaps(timeInterval)) {
                logger.debug("File found: {}", dataFile.file);
                dataFiles.add(dataFile.file);
            }
        }
        return dataFiles;
    }
    
    /**
     * Adds data from the given file to the given graphics context
     * @param file
     * @param im
     * @param getMap
     */
    private static void addData(File file, Graphics2D g2d, Interval interval, GetMapRequest getMap,
        IndexColorModel indexColorModel) throws WmsException, IOException {
        // Read the data from the file
        SciamachySwath swath = SciamachySwath.fromFile(file.getPath());
        // Draw a polygon for each retrieval that is within the time interval
        for (Retrieval retrieval : swath.getRetrievals()) {
            if (interval.contains(retrieval.getDateTime())) {
                Color color = getColor((float)retrieval.getTotalOzone(), indexColorModel, getMap.getStyleRequest());
                g2d.setPaint(color);
                for (Polygon polygon : getPolygons(retrieval.getGroundPixel(), getMap)) {
                    g2d.fillPolygon(polygon);
                }
            }
        }
    }

    private static Color getColor(float dataValue, IndexColorModel indexColorModel,
        GetMapStyleRequest styleRequest) throws WmsException {
        ColorScaleRange scaleRange = styleRequest.getColorScaleRange();
        float scaleMin, scaleMax;
        if (scaleRange.isAuto()) {
            throw new WmsException("Can't use an automatic scale range for Sciamachy data");
        } else if (scaleRange.isDefault()) {
            scaleMin = 180.0f;
            scaleMax = 320.0f;
        } else {
            scaleMin = scaleRange.getScaleMin();
            scaleMax = scaleRange.getScaleMax();
        }
        int index = getColourIndex(dataValue, scaleMin, scaleMax, styleRequest);
        return new Color(
            indexColorModel.getRed(index),
            indexColorModel.getGreen(index),
            indexColorModel.getBlue(index),
            indexColorModel.getAlpha(index)
        );
    }

    /**
     * @return the colour index that corresponds to the given value
     * @see ImageProducer
     */
    private static int getColourIndex(float value, float scaleMin, float scaleMax,
        GetMapStyleRequest styleRequest)
    {
        int numColourBands = styleRequest.getNumColourBands();
        boolean logarithmic = styleRequest.isScaleLogarithmic() == null ?
            false : styleRequest.isScaleLogarithmic().booleanValue();
        if (Float.isNaN(value))
        {
            return numColourBands; // represents a background pixel
        }
        else if (value < scaleMin || value > scaleMax)
        {
            return numColourBands + 1; // represents an out-of-range pixel
        }
        else
        {
            double min = logarithmic ? Math.log(scaleMin) : scaleMin;
            double max = logarithmic ? Math.log(scaleMax) : scaleMax;
            double val = logarithmic ? Math.log(value) : value;
            double frac = (val - min) / (max - min);
            // Compute and return the index of the corresponding colour
            return (int)(frac * numColourBands);
        }
    }

    private static List<Polygon> getPolygons(GroundPixel groundPixel, GetMapRequest getMap) {
        List<Polygon> polygons = new ArrayList<Polygon>();

        Polygon poly1 = new Polygon();

        // TODO: explain this, and add another polygon when this one crosses
        // the anti-meridian
        double centreLon = groundPixel.getCentre().getLongitude180();
        for (LonLat corner: groundPixel.getCorners()) {
            double cornerLon1 = corner.getLongitude180();
            double cornerLon2 = cornerLon1 < 0.0 ? cornerLon1 + 360.0 : cornerLon1 - 360.0;
            double d1 = Math.abs(centreLon - cornerLon1);
            double d2 = Math.abs(centreLon - cornerLon2);
            double cornerLon = d1 < d2 ? cornerLon1 : cornerLon2;
            addPoint(poly1, getPoint(cornerLon, corner.getLatitude(), getMap));
        }

        polygons.add(poly1);

        return polygons;
    }
    
    private static void addPoint(Polygon polygon, Point point) {
        polygon.addPoint(point.x, point.y);
    }

    private static Point getPoint(double longitude, double latitude, GetMapRequest getMap) {
        int width = getMap.getDataRequest().getWidth();
        int height = getMap.getDataRequest().getHeight();
        double[] bbox = getMap.getDataRequest().getBbox();
        double minLon = bbox[0];
        double minLat = bbox[1];
        double dLon = bbox[2] - minLon;
        double dLat = bbox[3] - minLat;
        // Calculate fractional distance of point along image in each direction
        double fracLon = (longitude - minLon) / dLon;
        double fracLat = (latitude  - minLat) / dLat;
        return new Point(
            (int)Math.round(fracLon * width),
            height - 1 - (int)Math.round(fracLat * height) // y-axis is flipped
        );
    }

    /**
     * Writes the image to the servlet's output stream
     * @param im
     * @param mimeType
     * @param response
     * @throws InvalidFormatException if the image cannot be rendered using
     * the given mime type
     * @throws IOException if there was an error writing data to the output stream
     */
    private static void writeImage(BufferedImage im, String mimeType, HttpServletResponse response)
        throws InvalidFormatException, IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(mimeType);
        if (!writers.hasNext()) {
            // Should check this earlier really
            throw new InvalidFormatException(mimeType);
        }
        // Use the first writer in the iterator
        ImageWriter writer = writers.next();
        ImageOutputStream ios = ImageIO.createImageOutputStream(response.getOutputStream());
        writer.setOutput(ios);
        writer.write(im);
        // TODO: how can we make sure these are always called correctly
        // even in event of failure?
        writer.dispose();
        ios.close();
    }

}
