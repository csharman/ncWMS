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
package uk.ac.rdg.resc.ncwms.cdm;

import java.util.ArrayList;
import java.util.List;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.Variable;
import ucar.nc2.dt.GridDatatype;

/**
 * Wraps a List of Ranges, providing methods to safely set ranges for
 * x, y, z and t.  Call {@link #getRanges()} to get a List of Range objects
 * that can be passed directly to {@link Variable#read(java.util.List)}.
 * @author Jon Blower
 */
final class RangesList
{
    private static final Range ZERO_RANGE;

    private final List<Range> ranges;

    private final int xAxisIndex;
    private final int yAxisIndex;
    private final int zAxisIndex;
    private final int tAxisIndex;

    static
    {
        try { ZERO_RANGE = new Range(0, 0); }
        catch (InvalidRangeException ire) { throw new ExceptionInInitializerError(ire); }
    }

    public RangesList(GridDatatype grid)
    {
        int rank = grid.getRank();
        this.ranges = new ArrayList<Range>(rank);
        for (int i = 0; i < rank; i++) { this.ranges.add(ZERO_RANGE); }

        this.xAxisIndex = grid.getXDimensionIndex();
        this.yAxisIndex = grid.getYDimensionIndex();
        this.zAxisIndex = grid.getZDimensionIndex();
        this.tAxisIndex = grid.getTimeDimensionIndex();
    }

    public void setXRange(Range xRange)
    {
        this.setRange(this.xAxisIndex, xRange);
    }

    public void setYRange(Range yRange)
    {
        this.setRange(this.yAxisIndex, yRange);
    }

    public void setZRange(Range zRange)
    {
        this.setRange(this.zAxisIndex, zRange);
    }

    public void setTRange(Range tRange)
    {
        this.setRange(this.tAxisIndex, tRange);
    }

    private void setRange(int index, Range range)
    {
        if (index >= 0) this.ranges.set(index, range);
    }

    private Range getRange(int index)
    {
        if (index >= 0) return this.ranges.get(index);
        return null;
    }

    /** Gets the index of the x axis within the {@link #getRanges() list of ranges}.*/
    public int getXAxisIndex() { return this.xAxisIndex; }

    /** Gets the index of the y axis within the {@link #getRanges() list of ranges}.*/
    public int getYAxisIndex() { return this.yAxisIndex; }

    /** Gets the index of the z axis within the {@link #getRanges() list of ranges}.*/
    public int getZAxisIndex() { return this.zAxisIndex; }

    /** Gets the index of the t axis within the {@link #getRanges() list of ranges}.*/
    public int getTAxisIndex() { return this.tAxisIndex; }

    public List<Range> getRanges() { return this.ranges; }

    @Override
    public String toString()
    {
        Range tRange = this.getRange(this.tAxisIndex);
        Range zRange = this.getRange(this.zAxisIndex);
        Range yRange = this.getRange(this.yAxisIndex);
        Range xRange = this.getRange(this.xAxisIndex);
        return String.format("tRange: %s, zRange: %s, yRange: %s, xRange: %s",
            tRange, zRange, yRange, xRange);
    }
}
