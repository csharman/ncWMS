/*
 * Copyright (c) 2007 The University of Reading
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

package uk.ac.rdg.resc.ncwms.coords;

import uk.ac.rdg.resc.edal.position.HorizontalPosition;
import uk.ac.rdg.resc.edal.position.LonLatPosition;
import java.util.Arrays;
import java.util.List;
import org.geotoolkit.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import uk.ac.rdg.resc.edal.coverage.domain.Domain;

/**
 * <p>A list of {@link HorizontalPosition}s in a certain coordinate reference system.
 * Instances of this class usually represent requests for data from the
 * GetTransect operation: the points in this list are the coordinates
 * of real-world points for which we need data.</p>
 * <p>The order of points in this list is important, hence this class supports
 * index-based to the data.  A typical use of this class would be as follows:</p>
 * <pre>
 *    // Process the client's request and get the list of points for which we need data
 *    PointList pointList = request...;
 *    // Use this list of points to extract data
 *    float[] data = dataReader.read(pointList, otherParams...);
 *    // data.length equals pointList.size()
 *    // Furthermore data[i] corresponds to pointList.get(i)
 * </pre>
 * @author Jon
 */
public final class PointList implements Domain<HorizontalPosition>
{

    private final CoordinateReferenceSystem crs;
    private final List<HorizontalPosition> posList;

    /**
     * Creates a PointList from the given List of HorizontalPositions with their
     * coordinate reference system
     * @param list The x-y points to wrap as a PointList
     * @param crs CoordinateReferenceSystem of the points, cannot be null
     * @return a new PointList that wraps the given list of projection points
     */
    public PointList(List<HorizontalPosition> posList, CoordinateReferenceSystem crs) {
        if (crs == null) throw new NullPointerException("Must specify a CRS");
        this.posList = posList;
        this.crs = crs;
    }

    /**
     * Creates a PointList containing a single point.  Useful for getFeatureInfo
     * requests
     * @param point The HorizontalPosition to wrap
     * @param crs the coordinate reference system of the point
     * @return a new PointList that wraps the given point
     */
    public PointList(HorizontalPosition point, CoordinateReferenceSystem crs)
    {
        this(Arrays.asList(point), crs);
    }

    /**
     * Creates a PointList containing a single point.  Useful for getFeatureInfo
     * requests
     * @param point The HorizontalPosition to wrap
     * @return a new PointList that wraps the given point
     */
    public PointList(HorizontalPosition point)
    {
        this(point, point.getCoordinateReferenceSystem());
    }

    /**
     * Creates a PointList containing a single lon-lat point.  Useful for getFeatureInfo
     * requests.
     * @param point The LonLatPosition to wrap
     * @return a new PointList that wraps the given lon-lat point
     */
    public PointList(LonLatPosition lonLat)
    {
        this(lonLat, DefaultGeographicCRS.WGS84);
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return this.crs;
    }

    @Override
    public List<HorizontalPosition> getDomainObjects() {
        return this.posList;
    }

}
