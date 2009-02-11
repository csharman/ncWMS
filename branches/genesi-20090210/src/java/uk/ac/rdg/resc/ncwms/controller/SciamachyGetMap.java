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
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;
import uk.ac.rdg.resc.ncwms.utils.WmsUtils;

/**
 * Hacked-together class to produce an image of Sciamachy data in response
 * to a GetMap query
 * @author Jon
 */
class SciamachyGetMap {
    
    private static final Logger logger = LoggerFactory.getLogger(SciamachyGetMap.class);

    private static File DATA_DIR = new File("C:\\Documents and Settings\\Jon\\Desktop\\ASCII");

    private static class DataFile {
        private Interval timeRange;
        private File file;
    }

    private static List<DataFile> DATA_FILES = new ArrayList<DataFile>();

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

        // Find the file(s) that match this time interval and bounding box
        // TODO: in the final system this will search the GENESI-DR system
        List<File> dataFiles = findDataFiles(timeInterval);
        
        if (dataFiles.size() == 0) {
            throw new InvalidDimensionValueException("time", timeString);
        }
        
        // Create a BufferedImage to hold the image pixels
        BufferedImage im = new BufferedImage(
            getMap.getDataRequest().getWidth(),
            getMap.getDataRequest().getHeight(),
            BufferedImage.TYPE_INT_ARGB
        );
        
        // Add data from each of the matching files to the image
        Graphics2D g2d = (Graphics2D)im.getGraphics();
        for (File f : dataFiles) {
            addData(f, g2d, timeInterval, getMap);
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
            return new Interval(start, end + 1);
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
    private static void addData(File file, Graphics2D g2d, Interval interval, GetMapRequest getMap) throws IOException {
        // Read the data from the file
        SciamachySwath swath = SciamachySwath.fromFile(file.getPath());
        // Draw a polygon for each retrieval that is within the time interval
        for (Retrieval retrieval : swath.getRetrievals()) {
            if (interval.contains(retrieval.getDateTime())) {
                Color color = getColor(retrieval.getTotalOzone());
                g2d.setPaint(color);
                for (Polygon polygon : getPolygons(retrieval.getGroundPixel(), getMap)) {
                    g2d.fillPolygon(polygon);
                }
            }
        }
    }
    
    private static Color getColor(double dataValue) {
        if (dataValue < 200.0) {
            return Color.BLUE;
        } else if (dataValue < 225.0) {
            return Color.GREEN;
        } else if (dataValue < 250.0) {
            return Color.YELLOW;
        } else if (dataValue < 275.0) {
            return Color.ORANGE;
        } else if (dataValue < 300.0) {
            return Color.RED;
        } else {
            return Color.MAGENTA;
        }
    }

    private static List<Polygon> getPolygons(GroundPixel groundPixel, GetMapRequest getMap) {
        List<Polygon> polygons = new ArrayList<Polygon>();
        Polygon polygon1 = new Polygon();
        Polygon polygon2 = new Polygon();
        boolean allLessThan180 = true;
        boolean allGreaterThan180 = true;
        for (LonLat corner : groundPixel.getCorners()) {
            if (corner.getLongitude() >= 180.0) {
                allLessThan180 = false;
            } else {
                allGreaterThan180 = false;
            }
            addPoint(polygon1, getPoint(corner.getLongitude(), corner.getLatitude(), getMap));
            addPoint(polygon2, getPoint(corner.getLongitude() - 360.0, corner.getLatitude(), getMap));
        }
        if (allLessThan180) {
            polygons.add(polygon1);
        } else if (allGreaterThan180) {
            polygons.add(polygon2);
        } else {
            // this spans the anti-meridian so we need to add both polygons
            polygons.add(polygon1);
            polygons.add(polygon2);
        }
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
            (int)Math.round(fracLat * height)
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
        if (writers.hasNext()) {
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
        // Should check this earlier really
        throw new InvalidFormatException(mimeType);
    }

}
