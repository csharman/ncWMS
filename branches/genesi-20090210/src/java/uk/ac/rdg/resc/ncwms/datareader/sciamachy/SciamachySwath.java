/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.rdg.resc.ncwms.datareader.sciamachy;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A single swath of data from the SCIAMACHY instrument.  Each line in the data
 * file is one retrieval (i.e. one ground pixel, plus data values).
 * @author Jon
 */
public class SciamachySwath {

    private static final Logger logger = LoggerFactory.getLogger(SciamachySwath.class);

    private List<Retrieval> retrievals = new ArrayList<Retrieval>();
    
    /**
     * Object for parsing dates and times from SCIAMACHY files.  This object
     * is immutable and so can be shared between threads.  This parser will always
     * use UTC as the time zone.
     */
    private static final DateTimeFormatter DATE_TIME_PARSER = DateTimeFormat
        .forPattern("yyyyMMdd HHmmss.SSS")
        .withZone(DateTimeZone.UTC);

    private SciamachySwath() {}

    /**
     * Reads data from the given file, line by line.
     * @param filename
     * @throws FileNotFoundException if the file could not be found or opened
     * @throws IOException
     * @throws ParseException if the file could not be parsed.
     */
    public static SciamachySwath fromFile(String filename) throws FileNotFoundException, IOException {
        logger.debug("Loading swath data from {}", filename);
        SciamachySwath swath = fromInputStream(new FileInputStream(filename));
        logger.debug("Successfully loaded swath data from {}", filename);
        return swath;
    }

    public static SciamachySwath fromInputStream(InputStream in) throws FileNotFoundException, IOException {
        BufferedReader br = null;
        try {
            SciamachySwath swath = new SciamachySwath();
            br = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = br.readLine()) != null) {
                Retrieval retrieval = new Retrieval(line);
                swath.retrievals.add(retrieval);
            }
            swath.retrievals = Collections.unmodifiableList(swath.retrievals);
            return swath;
        } finally {
            if (br != null) br.close();
        }
    }

    public List<Retrieval> getRetrievals() {
        return this.retrievals;
    }

    /// Helper methods
    private static final double readValue(String s, double factor) {
        return Integer.parseInt(s.trim()) / factor;
    }
    private static final double readOzone(String s) {
        return readValue(s, 10.0);
    }
    private static final double readLonOrLat(String s) {
        return readValue(s, 100.0);
    }

    /**
     * A single retrieval (ground pixel + time + data value).
     */
    public static class Retrieval {
        private DateTime dateTime;
        private GroundPixel groundPixel;
        private double totalOzone;
        private double totalOzoneError;

        /** Private constructor to prevent instantiation by clients */
        private Retrieval(String line) {
            this.dateTime = DATE_TIME_PARSER.parseDateTime(line.substring(0, 19));
            this.groundPixel = new GroundPixel(line);
            this.totalOzone = readOzone(line.substring(92, 97).trim());
            this.totalOzoneError = readOzone(line.substring(97, 102));
        }

        @Override
        public String toString() {
            return String.format("%s (%s): %f+/-%f DU", this.dateTime,
                this.groundPixel.centre, this.totalOzone, this.totalOzoneError);
        }

        public DateTime getDateTime() {
            return dateTime;
        }

        public GroundPixel getGroundPixel() {
            return groundPixel;
        }

        public double getTotalOzone() {
            return totalOzone;
        }

        public double getTotalOzoneError() {
            return totalOzoneError;
        }
    }

    public static class GroundPixel {
        private LonLat centre;
        private List<LonLat> corners = new ArrayList<LonLat>(4); // List of 4 corners

        /** Private constructor to prevent instantiation by clients */
        private GroundPixel(String line) {
            this.corners.add(new LonLat(line.substring(19, 33)));
            this.corners.add(new LonLat(line.substring(33, 47)));
            this.corners.add(new LonLat(line.substring(61, 75)));
            this.corners.add(new LonLat(line.substring(47, 61)));
            this.corners = Collections.unmodifiableList(this.corners);
            this.centre  = new LonLat(line.substring(75, 89));
        }

        public LonLat getCentre() {
            return centre;
        }

        public List<LonLat> getCorners() {
            return corners;
        }
    }

    /**
     * 2D position in lon-lat coordinates
     */
    public static class LonLat {
        private double longitude;
        private double latitude;
        /** Private constructor to prevent instantiation by clients */
        private LonLat(String pair) {
            // The plus sign is necessary because there may be more than one
            // space between lon and lat values (it's a regular expression).
            String[] lonLatStr = pair.trim().split(" +");
            if (lonLatStr.length != 2) {
                throw new AssertionError("lonLatStr.length = " + lonLatStr.length);
            }
            this.longitude = readLonOrLat(lonLatStr[0]);
            this.latitude = readLonOrLat(lonLatStr[1]);
        }

        @Override
        public String toString() {
            return String.format("%f,%f", this.longitude, this.latitude);
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public double getLongitude360() {
            return longitude < 0.0 ? longitude + 360.0 : longitude;
        }

        public double getLongitude180() {
            return longitude > 180.0 ? longitude - 360.0 : longitude;
        }
    }

}
