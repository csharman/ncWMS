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

import java.util.AbstractList;
import java.util.List;
import org.geotoolkit.referencing.CRS;
import uk.ac.rdg.resc.edal.position.impl.HorizontalPositionImpl;
import uk.ac.rdg.resc.edal.position.HorizontalPosition;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.rdg.resc.edal.coverage.domain.Domain;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidCrsException;

/**
 * A Grid of points onto which data are to be projected.  This is the grid that
 * is defined by the request CRS, the width, height and bounding box.
 *
 * @author Jon Blower
 */
public final class HorizontalGrid implements Domain<HorizontalPosition>
{
    private static final Logger logger = LoggerFactory.getLogger(HorizontalGrid.class);

    private final int width;      // Width of the grid in pixels
    private final int height;     // Height of the grid in pixels
    private final double[] bbox;  // Array of four doubles representing the bounding box
    private final String crsCode; // String representing the CRS
    private final CrsHelper crsHelper;

    private final double[] xAxisValues;
    private final double[] yAxisValues;

    private final List<HorizontalPosition> domainObjects = new AbstractList<HorizontalPosition>()
    {
        @Override
        public HorizontalPosition get(int index) {
            return HorizontalGrid.this.getPoint(index);
        }

        @Override
        public int size() {
            return HorizontalGrid.this.size();
        }
    };

    /**
     * Creates a HorizontalGrid.
     *
     * @param crsCode Code for the CRS of the grid
     * @param width Width of the grid in pixels
     * @param height Height of the grid in pixels
     * @param bbox Bounding box of the grid in the units of the given CRS
     * @throws InvalidCrsException if the given CRS code is not recognized
     * @todo check validity of the bounding box?
     */
    public HorizontalGrid(String crsCode, int width, int height, double[] bbox)
        throws InvalidCrsException
    {
        this(crsCode, CrsHelper.fromCrsCode(crsCode), width, height, bbox);
    }

    /**
     * Creates a HorizontalGrid in WGS84 latitude-longitude coordinates
     *
     * @param width Width of the grid in pixels
     * @param height Height of the grid in pixels
     * @param bbox Bounding box of the grid
     */
    public HorizontalGrid(int width, int height, GeographicBoundingBox bbox)
    {
        this(CrsHelper.PLATE_CARREE_CRS_CODE, CrsHelper.CRS_84, width, height,
                new double[] {
                    bbox.getWestBoundLongitude(),
                    bbox.getSouthBoundLatitude(),
                    bbox.getEastBoundLongitude(),
                    bbox.getNorthBoundLatitude()
        });
    }

    private HorizontalGrid(String crsCode, CrsHelper crsHelper, int width,
            int height, double[] bbox)
    {
        this.crsHelper = crsHelper;
        this.crsCode = crsCode;
        this.width = width;
        this.height = height;
        this.bbox = bbox;

        // Now calculate the values along the x and y axes of this grid
        double dx = (this.bbox[2] - this.bbox[0]) / this.width;
        this.xAxisValues = new double[this.width];
        for (int i = 0; i < this.xAxisValues.length; i++)
        {
            this.xAxisValues[i] = this.bbox[0] + (i + 0.5) * dx;
        }

        double dy = (this.bbox[3] - this.bbox[1]) / this.height;
        this.yAxisValues = new double[this.height];
        for (int i = 0; i < this.yAxisValues.length; i++)
        {
            // The y axis is flipped
            this.yAxisValues[i] = this.bbox[1] + (this.height - i - 0.5) * dy;
        }

        logger.debug("Created HorizontalGrid object for CRS {}", crsCode);
    }

    public int getWidth()
    {
        return this.width;
    }

    public int getHeight()
    {
        return this.height;
    }

    public double[] getBbox()
    {
        return this.bbox;
    }

    /**
     * @return array of points along the x axis in this coordinate
     * reference system
     */
    public double[] getXAxisValues()
    {
        return this.xAxisValues;
    }

    /**
     * @return array of points along the y axis in this coordinate
     * reference system
     */
    public double[] getYAxisValues()
    {
        return this.yAxisValues;
    }

    public int size()
    {
        return this.width * this.height;
    }

    /**
     * @return true if this is a lat-lon grid, i.e. x axis is longitude,
     * y axis is latitude
     */
    public boolean isLatLon()
    {
        return this.crsHelper.isLatLon();
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem()
    {
        return this.crsHelper.getCoordinateReferenceSystem();
    }

    public String getCrsCode()
    {
        return this.crsCode;
    }

    /**
     * Returns the HorizontalPosition at the given index in this PointList.  The
     * index is such that the x axis in this grid varies fastest, i.e. index=0
     * corresponds with x=0,y=0, and index=1 corresponds with x=1,y=0.
     */
    public HorizontalPosition getPoint(int index) {
        int yi = index / this.xAxisValues.length;
        int xi = index % this.xAxisValues.length;
        return new HorizontalPositionImpl(this.xAxisValues[xi], this.yAxisValues[yi]);
    }

    @Override
    public List<HorizontalPosition> getDomainObjects() {
        return this.domainObjects;
    }
}
