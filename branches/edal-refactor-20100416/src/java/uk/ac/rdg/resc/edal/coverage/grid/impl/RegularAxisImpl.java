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

import java.util.AbstractList;
import java.util.List;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import uk.ac.rdg.resc.edal.coverage.grid.RegularAxis;

/**
 * Immutable implementation of a {@link RegularAxis}.
 * @author Jon
 */
public final class RegularAxisImpl implements RegularAxis
{

    private final double firstValue; // The first value on the axis
    private final double spacing; // The axis spacing
    private final int size; // The number of points on the axis
    private final boolean longitude = false; // True if this is a longitude axis in degrees

    private final List<Double> coordValues = new AbstractList<Double>() {
        @Override public Double get(int index) {
            return RegularAxisImpl.this.getCoordinateValue(index);
        }

        @Override public int size() {
            return RegularAxisImpl.this.size;
        }

        @Override public int indexOf(Object o) {
            if (o == null) return -1;
            if (!(o instanceof Double)) return -1;
            return RegularAxisImpl.this.getCoordinateIndex((Double)o);
        }
    };

    public RegularAxisImpl(double firstValue, double spacing, int size)
    {
        if (spacing <= 0.0) {
            throw new IllegalArgumentException("Axis spacing must be positive");
        }
        if (size < 0) {
            throw new IllegalArgumentException("Axis length must not be negative");
        }
        this.firstValue = firstValue;
        this.spacing = spacing;
        this.size = size;
    }

    @Override
    public double getCoordinateSpacing() { return this.spacing; }

    /** Returns an unmodifiable List view of the coordinate values */
    @Override
    public List<Double> getCoordinateValues() {
        return this.coordValues;
    }

    private double getCoordinateValue(int index) {
        if (index < 0 || index >= this.size) {
            throw new IndexOutOfBoundsException(index + " must be between 0 and "
                + (this.size - 1));
        }
        return this.firstValue + index * this.spacing;
    }

    private int getCoordinateIndex(double value) {
        // This method will generally be faster than an exhaustive search, or
        // even a binary search
        
        // We find the (non-integer) index of the given value
        double indexDbl = (value - this.firstValue) / this.spacing;

        // We find the nearest integer indices on either side of this and compare
        // the corresponding values with the target value.  We do this so that we
        // are not sensitive to rounding errors
        {
            int indexAbove = (int)Math.ceil(indexDbl);
            if (indexMatchesValue(indexAbove, value)) return indexAbove;
        }

        {
            int indexBelow = (int)Math.floor(indexDbl);
            if (indexMatchesValue(indexBelow, value)) return indexBelow;
        }

        // Neither of the indices matched the target value
        return -1;
    }

    private boolean indexMatchesValue(int index, double value) {
        if (index < 0 || index >= this.size) return false;
        return Double.compare(value, this.getCoordinateValue(index)) == 0;
    }

    @Override
    public int getNearestCoordinateIndex(double value) {
        return 0;
    }

    @Override
    public CoordinateSystemAxis getCoordinateSystemAxis() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static void main(String[] args)
    {
        System.out.println(-361.0 % 360.0);
    }

}
