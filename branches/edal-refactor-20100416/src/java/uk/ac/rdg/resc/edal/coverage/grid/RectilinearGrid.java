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
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.edal.coverage.grid;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import uk.ac.rdg.resc.edal.position.HorizontalPosition;

/**
 * A {@link HorizontalGrid} whose axes in real space are aligned with the axes
 * in grid space.  Therefore the axes of the grid are separable.
 * @param <HP> The type of HorizontalPosition that comprises this grid.
 * @author Jon
 */
public interface RectilinearGrid<HP extends HorizontalPosition> extends HorizontalGrid<HP> {

    /**
     * Returns the {@link ReferenceableAxis} for the given axis index. This object
     * maps from integer indices along the axis to real-world coordinates.
     * The index matches the index of the corresponding {@link CoordinateSystemAxis}
     * within the {@link #getCoordinateReferenceSystem() coordinate reference system}.
     * @param index The index of the required axis with the grid's
     * {@link #getCoordinateReferenceSystem() coordinate reference system}.
     * @return The ReferenceableAxis at the required index
     * @throws IndexOutOfBoundsException if {@code index} is neither 0 nor 1.
     */
    public ReferenceableAxis getAxis(int index);

    /**
     * {@inheritDoc}
     *
     * <p>The number of dimensions in this coordinate reference system must
     * match the {@link #getDimension() number of dimensions in the grid}.</p>
     */
    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem();

}
