/*
 * Copyright (c) 2006 The University of Reading
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

package uk.ac.rdg.resc.ncwms.metadata;

import ucar.nc2.dataset.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;

/**
 * An abstract superclass for all coordinate axes.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public abstract class CoordAxis
{
    private AxisType axisType;
    private String units;
    
    /**
     * Static factory method for creating a CoordAxis from a CoordinateAxis
     * object that is obtained from the Java NetCDF libraries.
     * 
     * @param axis The {@link ucar.nc2.dataset.CoordinateAxis} to wrap, which
     * must be a one-dimensional axis (in the current implementation).
     * @return a CoordAxis
     * @throws IllegalArgumentException if the provided axis cannot be turned into
     * an CoordAxis
     */
    public static CoordAxis create(CoordinateAxis axis)
    {
        if (axis instanceof CoordinateAxis1D)
        {
            return OneDCoordAxis.create((CoordinateAxis1D)axis);
        }
        else
        {
            throw new IllegalArgumentException("Cannot yet deal with coordinate" +
                " axes of >1 dimension");
        }
    }
    
    protected CoordAxis(AxisType type, String units)
    {
        this.axisType = type;
        this.units = units;
    }    

    public AxisType getAxisType()
    {
        return axisType;
    }

    public String getUnits()
    {
        return units;
    }
    
}
