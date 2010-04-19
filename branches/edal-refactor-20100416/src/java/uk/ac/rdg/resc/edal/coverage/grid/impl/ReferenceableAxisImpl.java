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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import uk.ac.rdg.resc.edal.coverage.grid.ReferenceableAxis;

/**
 * Immutable implementation of a {@link ReferenceableAxis}.
 * @author Jon
 */
public final class ReferenceableAxisImpl implements ReferenceableAxis
{
    private final double[] axisValues;

    private final List<Double> coordValues = new AbstractList<Double>() {
        @Override public Double get(int index) {
            return ReferenceableAxisImpl.this.axisValues[index];
        }

        @Override public int size() {
            return ReferenceableAxisImpl.this.axisValues.length;
        }

        @Override public int indexOf(Object o) {
            // Uses binary search to find the index of the target object more efficiently
            if (o == null) return -1;
            if (!(o instanceof Double)) return -1;
            double target = ((Double)o).doubleValue();
            int index = Arrays.binarySearch(ReferenceableAxisImpl.this.axisValues, target);
            return index >= 0 ? index  : -1;
        }
    };
    
    /**
     * Creates a ReferenceableAxis from the given array of axis values.  The
     * axis values are copied to internal data structures, therefore subsequent
     * modifications to the array of axis values have no effect on this object.
     * @param axisValues Array of axis values; must be in strictly ascending
     * numerical order
     * @throws NullPointerException if {@code axisValues} is null
     * @throws IllegalArgumentException if the axis values are not in strictly
     * ascending numerical order
     */
    public ReferenceableAxisImpl(double[] axisValues)
    {
        if (axisValues == null) throw new NullPointerException();
        // Defensive copy taken to preserve immutability
        this.axisValues = axisValues.clone();
        this.checkOrder();
    }

    /**
     * Creates a ReferenceableAxis from the given collection of axis values.
     * The axis values are copied to internal data structures, therefore subsequent
     * modifications to the collection of axis values have no effect on this object.
     * @param axisValues Collection of axis values; must be in strictly ascending
     * numerical order
     * @throws NullPointerException if {@code axisValues} is null, or if any
     * of the values in the collection is null.
     * @throws IllegalArgumentException if the axis values are not in strictly
     * ascending numerical order
     */
    public ReferenceableAxisImpl(Collection<? extends Number> axisValues)
    {
        if (axisValues == null) throw new NullPointerException();
        this.axisValues = new double[axisValues.size()];
        int i = 0;
        for (Number d : axisValues)
        {
            if (d == null) throw new NullPointerException("Coordinate value cannot be null");
            this.axisValues[i] = d.doubleValue();
            i++;
        }
        this.checkOrder();
    }

    /**
     * Checks that the axis values are in ascending order, throwing an
     * IllegalArgumentException if not
     */
    private void checkOrder()
    {
        if (this.axisValues.length == 0) return;
        double prevVal = this.axisValues[0];
        for (int i = 1; i < this.axisValues.length; i++)
        {
            if (this.axisValues[i] <= prevVal)
            {
                throw new IllegalArgumentException("Coordinate values must increase monotonically");
            }
            prevVal = this.axisValues[i];
        }
    }

    /** Returns an unmodifiable List view of the coordinate values */
    @Override
    public List<Double> getCoordinateValues() {
        return this.coordValues;
    }

    @Override
    public int getNearestCoordinateIndex(double value) {
        return 0;
    }

    @Override
    public CoordinateSystemAxis getCoordinateSystemAxis() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
