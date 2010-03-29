/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.rdg.resc.ncwms.datareader;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.apache.log4j.Logger;
import org.geotoolkit.referencing.datum.DefaultEllipsoid;
import ucar.ma2.Array;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dt.GridCoordSystem;

/**
 *
 * @author Jon
 */
class OMICoordinateSystem {
    private static final Logger logger = Logger.getLogger(OMICoordinateSystem.class);
    /**
     * @todo should be WGS84?
     */
    private static DefaultEllipsoid ELLIPSOID = DefaultEllipsoid.SPHERE;

        /**
     * Time zone representing Greenwich Mean Time
     */
    public static final TimeZone GMT = TimeZone.getTimeZone("GMT+0");


    private final float[][] lon; // Missing values represent NaNs.
    private final float[][] lat; // [1644][60]
    private final int ni, nj;  // ni = 1644, nj = 60.  j varies fastest!
    private final double spacing; // Estimate of the average spacing between grid points
    private final Date date;

    public OMICoordinateSystem(GridCoordSystem coordSys) throws IOException {
        this.lon = getArray(coordSys.getXHorizAxis());
        this.lat = getArray(coordSys.getYHorizAxis());

        this.ni = this.lon.length;
        this.nj = this.lon[0].length;

        // Find the time in the middle of this swath
        // It is expressed in seconds since UTC midnight Jan. 1, 1993
        double tai93 = coordSys.getTimeAxis1D().getCoordValue(this.ni / 2);
        Calendar cal = Calendar.getInstance(GMT);
        cal.set(1993, 0, 1, 0, 0, 0);
        cal.add(Calendar.SECOND, (int)Math.rint(tai93));
        this.date = cal.getTime();
        logger.info("Date of swath centre: {}", this.date);

        // Estimate a representative spacing for this grid, so that we can
        // see when a "nearest neighbour" is actually out of the grid's domain
        this.spacing = this.estimateGridSpacing();
    }

    /**
     * Note: makes defensive copy
     * @return
     */
    public Date getDate() {
        return new Date(this.date.getTime());
    }

    private static float[][] getArray(CoordinateAxis axis) throws IOException {
        Array arr = axis.convertScaleOffsetMissing(axis.read());
        return (float[][])arr.copyToNDJavaArray();
    }

    /**
     * Estimates a representative spacing between grid points.
     * @return
     */
    private double estimateGridSpacing() {
        // We find the maximum of the grid spacings between grid points
        // in the middle of the grid
        int i = this.ni / 2;
        double maxDist = 0.0;
        for (int j = 0; j < this.nj - 1; j++) {
            // Get the distance between the jth and (j+1)th grid point
            double dist = this.getDistance(this.lon[i][j], this.lat[i][j], i, j + 1);
            if (!Double.isNaN(dist)) {
                logger.debug("dist = {}", dist);
                if (dist > maxDist) maxDist = dist;
            }
        }
        logger.debug("Estimated distance = {}", maxDist);
        return maxDist;
    }

    /**
     * Gets an array of 2 integers representing the closest point to the given
     * longitude and latitude by exhaustive search (slow!), or null if the
     * given lon/lat pair is not represented here.
     * @return
     */
    public int[] getIndicesExhaustive(double lon, double lat) {
        double minDist = 0.0;
        int[] min = null;
        for (int i = 0; i < this.ni; i++) { // 1644
            for (int j = 0; j < this.nj; j++) { // 60
                if (!isMissing(i, j)) {
                    double testDist = this.getDistance(lon, lat, i, j);
                    if (min == null || testDist < minDist) {
                        minDist = testDist;
                        min = new int[]{i,j};
                    }
                }
            }
        }
        return min;
    }

    /**
     * Gets the orthodromic distance between the given lon and lat pair and the
     * position in the coordinate system given by the i,j pair.
     */
    private double getDistance(double lon, double lat, int i, int j) {
        return ELLIPSOID.orthodromicDistance(lon, lat, this.lon[i][j], this.lat[i][j]);
    }

    /**
     * Calls {@link #getIndicesDownhill(double, double, int, int)}, starting from
     * (0,0)
     */
    public int[] getIndicesDownhill(double lon, double lat) {
        return this.getIndicesDownhill(lon, lat, 0, 0);
    }

    /**
     * Gets an array of 2 integers representing the closest point to the given
     * longitude and latitude by downhill search, or null if the
     * given lon/lat pair is not represented here.  Starts looking at the given
     * point.
     * @return
     */
    public int[] getIndicesDownhill(double lon, double lat, int i, int j) {
        double dist = this.getDistance(lon, lat, i, j);
        boolean finished = false;
        while(!finished) {
            // Test in all four directions, wrapping around in both i and j
            // TODO no need to test the previous direction!
            int newi = i, newj = j;
            {
                // Test moving in i - 1 direction
                int testi = i == 0 ? this.ni - 1 : i - 1;
                if (!isMissing(testi, j)) {
                    double test = this.getDistance(lon, lat, testi, j);
                    if (test < dist) { newi = testi; newj = j; dist = test; }
                }
            }
            {
                // Test moving in i + 1 direction
                int testi = i == this.ni - 1 ? 0 : i + 1;
                if (!isMissing(testi, j)) {
                    double test = this.getDistance(lon, lat, testi, j);
                    if (test < dist) { newi = testi; newj = j; dist = test; }
                }
            }
            {
                // Test moving in j - 1 direction
                int testj = j == 0 ? this.nj - 1 : j - 1;
                if (!isMissing(i, testj)) {
                    double test = this.getDistance(lon, lat, i, testj);
                    if (test < dist) { newi = i; newj = testj; dist = test; }
                }
            }
            {
                // Test moving in j + 1 direction
                int testj = j == this.nj - 1 ? 0 : j + 1;
                if (!isMissing(i, testj)) {
                    double test = this.getDistance(lon, lat, i, testj);
                    if (test < dist) { newi = i; newj = testj; dist = test; }
                }
            }
            // Test to see if we have found a better match than the previous spot,
            // else finish
            if (newi == i && newj == j) finished = true;
            else { i = newi; j = newj; }
        }
        // Check to see if we're further away than a representative grid spacing
        // We only apply this filter if we're on the edge of the grid
        logger.debug("Dist = {}, spacing = {}", dist, spacing);
        if (onEdgeOfGrid(i, j) && dist > this.spacing) {
            return new int[]{-i,-j};
        } else {
            return new int[]{i,j};
        }
    }

    /**
     * We are on the edge of a grid if we are on the actual edge or if we are next
     * to a missing value
     * @param i
     * @param j
     * @return
     */
    private boolean onEdgeOfGrid(int i, int j) {
        return (i == 0) ||
               (i == this.ni - 1) ||
               (j == 0) ||
               (j == this.nj - 1) ||
               (isMissing(i-1, j)) ||
               (isMissing(i+1, j)) ||
               (isMissing(i, j-1)) ||
               (isMissing(i, j+1));
    }

    public float getLongitude(int i, int j) {
        return this.lon[i][j];
    }

    public float getLatitude(int i, int j) {
        return this.lat[i][j];
    }

    /**
     * Returns true if there is no lat or lon data for the given coordinate pair
     */
    private boolean isMissing(int i, int j) {
        try {
            return Float.isNaN(this.lon[i][j]) || Float.isNaN(this.lat[i][j]);
        } catch(ArrayIndexOutOfBoundsException e) {
            System.out.printf("Out of bounds: %d, %d%n", i, j);
            throw e;
        }
    }
}
