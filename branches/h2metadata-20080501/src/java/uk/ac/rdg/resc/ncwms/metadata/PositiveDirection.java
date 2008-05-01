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

import ucar.nc2.dataset.CoordinateAxis1D;

/**
 * Type-safe enumeration of the possible values of the positive direction of
 * a vertical coordinate axis
 * @author Jon
 */
public enum PositiveDirection
{
    POSITIVE_UP, POSITIVE_DOWN;
    
    /**
     * Static convenience method for finding the positive direction of a
     * CoordinateAxis1D that is obtained from the Java NetCDF libraries.
     */
    public static PositiveDirection getPositiveDirection(CoordinateAxis1D axis1D)
    {
        String pos = axis1D.getPositive();
        if (pos == null) return null;
        else if (pos.equals(CoordinateAxis1D.POSITIVE_UP)) return POSITIVE_UP;
        else return POSITIVE_DOWN;
    }
    
    /**
     * Static convenience method for finding the positive direction from
     * a Boolean object that is true when positive is up and null when
     * the direction is unknown.
     */
    public static PositiveDirection getPositiveDirection(Boolean isPositiveUp)
    {
        if (isPositiveUp == null) return null;
        else if (isPositiveUp.booleanValue()) return POSITIVE_UP;
        else return POSITIVE_DOWN;
    }
}
