/*
 * Copyright (c) 2010 The University of Reading
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
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR  CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.edal.coverage.grid.impl;

import java.util.List;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import uk.ac.rdg.resc.edal.coverage.grid.GridCoordinates;
import uk.ac.rdg.resc.edal.coverage.grid.RectilinearGrid;
import uk.ac.rdg.resc.edal.coverage.grid.ReferenceableAxis;
import uk.ac.rdg.resc.edal.position.BoundingBox;
import uk.ac.rdg.resc.edal.position.HorizontalPosition;
import uk.ac.rdg.resc.edal.position.impl.BoundingBoxImpl;
import uk.ac.rdg.resc.edal.position.impl.HorizontalPositionImpl;

/**
 * Abstract superclass that partially implements a two-dimensional
 * {@link RectilinearGrid}.
 * @author Jon
 */
public abstract class AbstractRectilinearGrid implements RectilinearGrid
{
    private final ReferenceableAxis xAxis;
    private final ReferenceableAxis yAxis;
    private final CoordinateReferenceSystem crs;
    private final BoundingBox extent;
    private final GridEnvelopeImpl gridExtent;

    public AbstractRectilinearGrid(ReferenceableAxis xAxis, ReferenceableAxis yAxis,
            CoordinateReferenceSystem crs)
    {
        if (xAxis == null || yAxis == null) {
            throw new NullPointerException("Axes cannot be null");
        }
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        this.crs = crs;

        this.extent = new BoundingBoxImpl(
            this.xAxis.getExtent(),
            this.yAxis.getExtent(),
            this.getCoordinateReferenceSystem()
        );

        this.gridExtent = new GridEnvelopeImpl(xAxis.getSize() - 1, yAxis.getSize() - 1);
    }

    @Override
    public ReferenceableAxis getAxis(int index) {
        if (index == 0) return this.xAxis;
        if (index == 1) return this.yAxis;
        throw new IndexOutOfBoundsException();
    }

    /** Returns 2 */
    @Override
    public final int getDimension() { return 2; }

    @Override
    public int getSize() {
        return this.xAxis.getSize() * this.yAxis.getSize();
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return this.crs;
    }

    @Override
    public BoundingBox getExtent() { return this.extent; }

    @Override
    public GridEnvelope getGridExtent() { return this.gridExtent; }

    @Override
    public HorizontalPosition transformCoordinates(GridCoordinates coords) {
        if (coords.getDimension() != 2) {
            throw new IllegalArgumentException("GridCoordinates must be 2D");
        }
        if (!this.gridExtent.contains(coords)) {
            return null;
        }
        double x = this.xAxis.getCoordinateValue(coords.getCoordinateValue(0));
        double y = this.yAxis.getCoordinateValue(coords.getCoordinateValue(1));
        return new HorizontalPositionImpl(x, y, this.getCoordinateReferenceSystem());
    }

    @Override
    public GridCoordinates inverseTransformCoordinates(HorizontalPosition pos) {
        int i = this.xAxis.getCoordinateIndex(pos.getX());
        int j = this.yAxis.getCoordinateIndex(pos.getY());
        if (i < 0 || j < 0) return null;
        // [i,j] order corresponds with [x,y] as specified in the contract of
        // RectilinearGrid
        return new GridCoordinatesImpl(i, j);
    }

    @Override
    public GridCoordinates findNearestGridPoint(HorizontalPosition pos) {
        int i = this.xAxis.getNearestCoordinateIndex(pos.getX());
        int j = this.yAxis.getNearestCoordinateIndex(pos.getY());
        if (i < 0 || j < 0) return null;
        // [i,j] order corresponds with [x,y] as specified in the contract of
        // RectilinearGrid
        return new GridCoordinatesImpl(i, j);
    }

    @Override
    public List<String> getAxisNames() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<HorizontalPosition> getDomainObjects() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
