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

import java.util.ArrayList;
import java.util.List;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionPointImpl;

/**
 * This class wraps the GeoTools/GeoAPI coordinate reference system methods,
 * providing a set of convenience methods such as transformations and validity
 * checks.
 * @todo this object is immutable and could be re-used.
 * @author Jon
 */
public final class CrsHelper {

    public static final String PLATE_CARREE_CRS_CODE = "CRS:84";
    public static final CoordinateReferenceSystem PLATE_CARREE_CRS;
    public static final List<String> SUPPORTED_CRS_CODES = new ArrayList<String>();

    private final CoordinateReferenceSystem crs;
    private final MathTransform crsToLatLon;
    private final MathTransform latLonToCrs;
    private final boolean isLatLon;

    static
    {
        // Find the supported CRS codes
        // I think this is the appropriate method to get all the CRS codes
        // that we can support
        for (Object codeObj : CRS.getSupportedCodes("urn:ogc:def"))
        {
            SUPPORTED_CRS_CODES.add((String)codeObj);
        }
        try
        {
            PLATE_CARREE_CRS = CRS.decode(PLATE_CARREE_CRS_CODE, true); // force longitude-first
        }
        catch (Exception ex)
        {
            throw new ExceptionInInitializerError("Can't find CRS:84");
        }
    }

    public CrsHelper(CoordinateReferenceSystem crs) throws FactoryException {
        this.crs = crs;
        // Get transformations to and from lat-lon.
        // The "true" means "lenient", i.e. ignore datum shifts.  This
        // is necessary to prevent "Bursa wolf parameters required"
        // errors (Some CRSs, including British National Grid, fail if
        // we are not "lenient".)
        this.crsToLatLon = CRS.findMathTransform(this.crs, PLATE_CARREE_CRS, true);
        this.latLonToCrs = CRS.findMathTransform(PLATE_CARREE_CRS, this.crs, true);
        this.isLatLon = this.crsToLatLon.isIdentity();
    }

    public CrsHelper(String crsCode) throws NoSuchAuthorityCodeException, FactoryException {
        this(CRS.decode(crsCode, true));
    }

    public CoordinateReferenceSystem getCoordinateReferenceSystem()
    {
        return this.crs;
    }

    /**
     * @return true if the given coordinate pair is within the valid range of
     * both the x and y axis of this coordinate reference system.
     */
    public boolean isPointValidForCrs(ProjectionPoint point)
    {
        return this.isPointValidForCrs(point.getX(), point.getY());
    }

    /**
     * @return true if the given coordinate pair is within the valid range of
     * both the x and y axis of this coordinate reference system.
     */
    public boolean isPointValidForCrs(double x, double y)
    {
        CoordinateSystemAxis xAxis = this.crs.getCoordinateSystem().getAxis(0);
        CoordinateSystemAxis yAxis = this.crs.getCoordinateSystem().getAxis(1);
        return x >= xAxis.getMinimumValue() && x <= xAxis.getMaximumValue() &&
               y >= yAxis.getMinimumValue() && y <= yAxis.getMaximumValue();
    }

    /**
     * Transforms the given x-y point in this {@link #getCoordinateReferenceSystem() CRS}
     * to a LatLonPoint.
     * @throws TransformException if the required transformation could not be performed
     */
    public LatLonPoint crsToLatLon(double x, double y) throws TransformException
    {
        if (this.isLatLon) {
            // We don't need to do the transformation
            return new LatLonPointImpl(y, x);
        }
        // We know x must go first in this array because we selected
        // "force longitude-first" when creating the CRS for this grid
        double[] point = new double[]{x, y};
        // Transform to lat-lon in-place
        this.crsToLatLon.transform(point, 0, point, 0, 1);
        return new LatLonPointImpl(point[1], point[0]);
    }

    /**
     * Transforms the given x-y point in this {@link #getCoordinateReferenceSystem() CRS}
     * to a LatLonPoint.
     * @throws TransformException if the required transformation could not be performed
     */
    public LatLonPoint crsToLatLon(ProjectionPoint point) throws TransformException
    {
        return this.crsToLatLon(point.getX(), point.getY());
    }

    /**
     * Transforms the given LatLonPoint to an x-y point in this
     * {@link #getCoordinateReferenceSystem() CRS}.
     * @throws TransformException if the required transformation could not be performed
     */
    public ProjectionPoint latLonToCrs(LatLonPoint latLonPoint) throws TransformException
    {
        return this.latLonToCrs(latLonPoint.getLongitude(), latLonPoint.getLatitude());
    }

    /**
     * Transforms the given longitude-latitude point to an x-y point in this
     * {@link #getCoordinateReferenceSystem() CRS}.
     * @throws TransformException if the required transformation could not be performed
     */
    public ProjectionPoint latLonToCrs(double longitude, double latitude) throws TransformException
    {
        if (this.isLatLon) {
            // We don't need to do the transformation
            return new ProjectionPointImpl(longitude, latitude);
        }
        // We know x must go first in this array because we selected
        // "force longitude-first" when creating the CRS for this grid
        double[] point = new double[]{longitude, latitude};
        // Transform to lat-lon in-place
        this.latLonToCrs.transform(point, 0, point, 0, 1);
        return new ProjectionPointImpl(point[0], point[1]);
    }

    /**
     * @return true if this crs is lat-lon
     */
    public boolean isLatLon()
    {
        return this.isLatLon;
    }

}
