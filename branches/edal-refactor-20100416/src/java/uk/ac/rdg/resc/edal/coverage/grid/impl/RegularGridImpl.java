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
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import uk.ac.rdg.resc.edal.coverage.grid.GridCoordinates;
import uk.ac.rdg.resc.edal.coverage.grid.RegularAxis;
import uk.ac.rdg.resc.edal.coverage.grid.RegularGrid;

/**
 * Immutable implementation of a {@link RegularGrid}.
 * @author Jon
 */
public final class RegularGridImpl extends AbstractRectilinearGrid
{

    

    @Override
    public RegularAxis getAxis(int index) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getDimension() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DirectPosition transformCoordinates(GridCoordinates coords) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public GridCoordinates inverseTransformCoordinates(DirectPosition pos) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<String> getAxisNames() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public GridEnvelope getExtent() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getSize() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List getDomainObjects() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
