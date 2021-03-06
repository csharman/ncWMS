/*
 * Copyright (c) 2009 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.ncwms.datareader;

import uk.ac.rdg.resc.ncwms.coordsys.CrsHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionPointImpl;

/**
 * Represents a path through a coordinate system.  The path consists of a set
 * of linear path elements, drawn between <i>control points</i>.
 * @todo This shares things in common with GeoAPI's {@link LineString} and
 * GeoTools' LineStringImpl - can we reuse these classes?  The problem
 * is that the Geo* classes are based upon different components, e.g. DirectPositions,
 * and seem to lack a method for finding coordinates along the path.
 * @author Jon
 */
public final class LineString {

    private final List<ProjectionPoint> controlPoints;
    private final transient double[] controlPointDistances;
    private double pathLength;
    private CrsHelper crsHelper;

    /**
     * Constructs a {@link LineString} from a line string in the form.
     * @param lineStringSpec the line string as specified in the form
     * "x1 y1, x2 y2, x3 y3".
     * @param crsHelper The coordinate reference system for the line string's
     * coordinates (wrapped in a helper object)
     * @throws InvalidLineStringException if the line string is not correctly
     * specified.
     * @throws NullPointerException if crsHelper == null
     */
    public LineString(String lineStringSpec, CrsHelper crsHelper) throws InvalidLineStringException {
        String[] pointsStr = lineStringSpec.split(",");
        if (pointsStr.length < 2) {
            throw new InvalidLineStringException("At least two points are required to generate a transect");
        }
        if (crsHelper == null) {
            throw new NullPointerException("CrsHelper cannot be null");
        }
        this.crsHelper = crsHelper;

        // The control points along the transect as specified by the line string
        final List<ProjectionPoint> ctlPoints = new ArrayList<ProjectionPoint>();
        for (String s : pointsStr) {
            String[] coords = s.trim().split(" +"); // allows one or more spaces to be used as a delimiter
            if (coords.length != 2) {
                throw new InvalidLineStringException("Coordinates format error");
            }
            try {
                ctlPoints.add(new ProjectionPointImpl(
                    Double.parseDouble(coords[0].trim()),
                    Double.parseDouble(coords[1].trim())
                ));
            } catch (NumberFormatException nfe) {
                throw new InvalidLineStringException("Coordinates format error");
            }
        }
        this.controlPoints = Collections.unmodifiableList(ctlPoints);

        // Calculate the total length of the path in units of the CRS.
        // While we're doing this we'll calculate the total length of the path
        // up to each waypoint
        this.controlPointDistances = new double[this.controlPoints.size()];
        this.pathLength = 0.0;
        this.controlPointDistances[0] = this.pathLength;
        for (int i = 1; i < this.controlPoints.size(); i++) {
            ProjectionPoint p1 = this.controlPoints.get(i - 1);
            ProjectionPoint p2 = this.controlPoints.get(i);
            double dx = p2.getX() - p1.getX();
            double dy = p2.getY() - p1.getY();
            this.pathLength += Math.sqrt(dx * dx + dy * dy);
            this.controlPointDistances[i] = this.pathLength;
        }
    }

    /**
     * Returns the list of control points along this line string.
     * @return an unmodifiable list of control points.
     */
    public List<ProjectionPoint> getControlPoints() {
        return this.controlPoints;
    }

    /**
     * Returns the fractional distance along the line string to the control
     * point with the given index.
     * @index The index of the control point.  An index of zero represents the
     * start of the line string.
     * @return the fractional distance along the whole line string to the
     * control point
     * @throws IndexOutOfBoundsException if {@code index < 0 || index >= number of control points}
     */
    public double getFractionalControlPointDistance(int index) {
        if (index < 0 || index >= this.controlPointDistances.length) {
            throw new IndexOutOfBoundsException();
        }
        return this.controlPointDistances[index] / this.pathLength;
    }

    /**
     * Returns a list of <i>n</i> points along the path defined by this line
     * string.  The first point will be the first control point; the last point
     * will be the last control point.  Intermediate points are linearly
     * interpolated between the control points.  Note that it is not guaranteed
     * that the intermediate control points will be contained in this list.
     * @param numPoints The number of points to return
     * @return an unmodifiable list of {@code n} points that lie on this path.
     * @throws IllegalArgumentException if {@code numPoints < 2}
     * @todo Add the control points to this list, in the correct location.
     */
    public List<ProjectionPoint> getPointsOnPath(int n) {
        if (n < 2) {
            throw new IllegalArgumentException("Must request at least 2 points");
        }
        final List<ProjectionPoint> points = new ArrayList<ProjectionPoint>(n);
        // The first point is the first control point
        points.add(this.controlPoints.get(0));
        // Now for the points in the middle
        for (int i = 1; i < n - 1; i++) {
            // Calculate the distance along the path
            double s = this.pathLength * i / (n - 1);
            // Calculate an interpolated point along the path
            points.add(this.interpolatePoint(s));
        }
        // The last point is the last waypoint
        points.add(this.controlPoints.get(this.controlPoints.size() - 1));

        return Collections.unmodifiableList(points);
    }

    /**
     * Given a length <i>s</i> along the path defined by this line string, this
     * method returns a {@link ProjectionPoint} that represents this point on the
     * path.
     * @param s the distance along the path
     * @return a ProjectionPoint representing this point on the path.
     * @throws IllegalArgumentException if s < 0 or s > pathLength
     */
    private ProjectionPoint interpolatePoint(double s) {
        if (s < 0.0 || s > this.pathLength) {
            throw new IllegalArgumentException("s does not lie on the path");
        }
        // Find the index of the last control point we passed
        int i = this.getPreviousControlPointIndex(s);
        // Find the distance from the last control point
        double dlast = s - this.controlPointDistances[i];
        // Find the distance to the next control point
        double dnext = this.controlPointDistances[i + 1] - s;
        // Find the fraction of the distance between the last and next control points
        double dfrac = dlast / (dlast + dnext);

        // Find the x and y coordinates of the interpolated point
        ProjectionPoint cplast = this.controlPoints.get(i);
        ProjectionPoint cpnext = this.controlPoints.get(i + 1);
        double x = (1.0 - dfrac) * cplast.getX() + dfrac * cpnext.getX();
        double y = (1.0 - dfrac) * cplast.getY() + dfrac * cpnext.getY();

        return new ProjectionPointImpl(x, y);
    }

    /**
     * If we walk a distance <i>s</i> along the path defined by this line string,
     * this method returns the index <i>i</i> of the last control point we passed.
     * I.e. the point at distance <i>s</i> lies between control point <i>i</i>
     * and control point <i>i</i> + 1.
     * @param s The distance along the path, which must lie between 0 and
     * pathLength (checked before this method is called).
     * @return the index of the previous control point, where 0 <= i < numControlPoints - 1.
     */
    private int getPreviousControlPointIndex(double s) {
        for (int i = 1; i < this.controlPointDistances.length; i++) {
            if (this.controlPointDistances[i] > s) return i - 1;
        }
        throw new AssertionError(); // Shouldn't get here.
    }

    public CrsHelper getCrsHelper()
    {
        return this.crsHelper;
    }

}

