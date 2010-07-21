/*
 * Copyright (c) 2009 The University of Reading
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.Variable;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dt.GridDatatype;
import uk.ac.rdg.resc.edal.coverage.domain.Domain;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.position.HorizontalPosition;
import uk.ac.rdg.resc.ncwms.config.datareader.DataReader;

/**
 * <p>Defines different strategies for reading data from files. The grid below represents the source
 * data.  Black grid squares represent data points that must be read from the source
 * data and will be used to generate the final output (e.g. image):</p>
 * <img src="doc-files/pixelmap_pbp.png">
 * <p>A variety of strategies are possible for reading these data points:</p>
 *
 * <h3>Strategy 1: read data points one at a time</h3>
 * <p>Read each data point individually by iterating through {@link PixelMap#getJIndices}
 *    and {@link PixelMap#getIIndices}.  This minimizes the memory footprint as the minimum
 *    amount of data is read from disk.  However, in general this method is inefficient
 *    as it maximizes the overhead of the low-level data extraction code by making
 *    a large number of small data extractions.  This is the {@link #PIXEL_BY_PIXEL
 *    pixel-by-pixel} strategy and is not recommended for general use.</p>
 *
 * <h3>Strategy 2: read all data points in one operation</h3>
 * <p>Read all data in one operation (potentially including lots of data points
 *       that are not needed) by finding the overall i-j bounding box with
 *       {@link PixelMap#getMinIIndex}, {@link PixelMap#getMaxIIndex}, {@link PixelMap#getMinJIndex}
 *       and {@link PixelMap#getMaxJIndex}.  This minimizes the number
 *       of calls to low-level data extraction code, but may result in a large memory
 *       footprint.  The {@link DataReader} would then subset this data array in-memory.
 *       This is the {@link #BOUNDING_BOX bounding-box} strategy.  This approach is
 *       recommended for remote datasets (e.g. on an OPeNDAP server) and compressed
 *       dataasets as it minimizes the overhead associated with the individual
 *       data-reading operations.</p>
 * <p>This approach is illustrated in the diagram below.  Grey squares represent
 * data points that are read into memory but are discarded because they do not
 * form part of the final image:</p>
 * <img src="doc-files/pixelmap_bbox.png">
 *
 * <h3>Strategy 3: Read "scanlines" of data</h3>
 * <p>A compromise strategy, which balances memory considerations against the overhead
 *       of the low-level data extraction code, works as follows:
 *       <ol>
 *          <li>Iterate through each row (i.e. each j index) that is represented in
 *              the PixelMap using {@link PixelMap#getJIndices}.</li>
 *          <li>For each j index, extract data from the minimum to the maximum i index
 *              in this row (a "scanline") using {@link PixelMap#getMinIIndexInRow} and
 *              {@link PixelMap#getMaxIIndexInRow}.  (This assumes that the data are stored with the i
 *              dimension varying fastest, meaning that the scanline represents
 *              contiguous data in the source files.)</li>
 *       </ol>
 *       Therefore if there are 25 distinct j indices in the PixelMap there will be 25
 *       individual calls to the low-level data extraction code.  This algorithm has
 *       been found to work well in a variety of situations although it may not always
 *       be the most efficient.  This is the {@link #SCANLINE scanline} strategy.</p>
 * <p>This approach is illustrated in the diagram below.  There is now a much smaller
 * amount of "wasted data" (i.e. grey squares) than in Strategy 2, and there are
 * much fewer individual read operations than in Strategy 1.</p>
 * <img src="doc-files/pixelmap_scanline.png">
 * @author Jon
 */
public enum DataReadingStrategy {

    /**
     * Reads "scanlines" of data, leading to a smaller memory footprint than
     * the {@link #BOUNDING_BOX bounding-box} strategy, but a larger number of individual
     * data-reading operations.  Recommended for use when the overhead of
     * a data-reading operation is low, e.g. for local, uncompressed files.
     */
    SCANLINE {
        @Override
        protected void populatePixelArray(List<Float> picData,
            PixelMap pixelMap, VariableDS var, RangesList ranges)
            throws IOException, InvalidRangeException
        {
            logger.debug("Reading data using a scanline algorithm");
            // Cycle through the y indices, extracting a scanline of
            // data each time from minX to maxX
            logger.debug("Shape of grid: {}", Arrays.toString(var.getShape()));
            // Get a VariableDS for unpacking and checking for missing data
            Variable origVar = var.getOriginalVariable();

            for (int j : pixelMap.getJIndices())
            {
                Range yRange = new Range(j, j);
                ranges.setYRange(yRange);
                // Read a row of data from the source
                int imin = pixelMap.getMinIIndexInRow(j);
                int imax = pixelMap.getMaxIIndexInRow(j);
                Range xRange = new Range(imin, imax);
                ranges.setXRange(xRange);

                logger.debug(ranges.toString());

                // Read a chunk of data - values will not be unpacked or
                // checked for missing values yet
                long start = System.nanoTime();
                Array arr = origVar.read(ranges.getRanges());
                logger.debug("Array shape = {}", Arrays.toString(arr.getShape()));
                long end = System.nanoTime();
                double timeToReadDataMs = (end - start) / 1.e6;

                // Get an index for the array and set it to zero
                Index index = arr.getIndex();
                index.set(new int[index.getRank()]);

                // Now copy the scanline's data to the picture array
                Set<Integer> iIndices = pixelMap.getIIndices(j);
                for (int i : iIndices) {
                    index.setDim(ranges.xi, i - imin);
                    float val = arr.getFloat(index);
                    // The value we've read won't have had scale-offset-missing applied
                    val = (float) var.convertScaleOffsetMissing(val);

                    // Now we set the value of all the image pixels associated with
                    // this data point.
                    if (!Float.isNaN(val)) {
                        for (int p : pixelMap.getPixelIndices(i, j)) {
                            picData.set(p, val);
                        }
                    }
                }
//                System.out.printf("Row: %d, imin: %d, imax: %d, pointsRead: %d, usefulPointsRead: %d, timeMs: %f, msPerPoint: %f, msPerUsefulPoint: %f%n",
//                     j, imin, imax, (imax - imin + 1), iIndices.size(), timeToReadDataMs, (timeToReadDataMs / (imax - imin + 1)), (timeToReadDataMs / iIndices.size()));
            }
        }

        @Override
        protected boolean isPixelMapSorted() { return false; }
    },

    /**
     * Reads all data in a single operation, then subsets in memory.  Recommended
     * in situations in which individual data reads have a high overhead, e.g. when
     * reading from OPeNDAP datasets or compressed files.
     */
    BOUNDING_BOX {
        @Override
        protected void populatePixelArray(List<Float> picData,
            PixelMap pixelMap, VariableDS var, RangesList ranges)
            throws IOException, InvalidRangeException
        {
            logger.debug("Reading data using a bounding-box algorithm");
            // Read the whole chunk of x-y data
            Range xRange = new Range(pixelMap.getMinIIndex(), pixelMap.getMaxIIndex());
            Range yRange = new Range(pixelMap.getMinJIndex(), pixelMap.getMaxJIndex());
            ranges.setXRange(xRange);
            ranges.setYRange(yRange);
            logger.debug("Shape of grid: {}", Arrays.toString(var.getShape()));
            logger.debug(ranges.toString());

            Variable origVar = var.getOriginalVariable();

            long start = System.currentTimeMillis();
            Array arr = origVar.read(ranges.getRanges());
            long readData = System.currentTimeMillis();
            logger.debug("Read data using bounding box algorithm in {} milliseconds", (readData - start));

            // Now extract the information we need from the data array
            Index index = arr.getIndex();
            index.set(new int[index.getRank()]);
            for (int j : pixelMap.getJIndices())
            {
                index.setDim(ranges.yi, j - yRange.first());
                for (int i : pixelMap.getIIndices(j))
                {
                    index.setDim(ranges.xi, i - xRange.first());
                    float val = arr.getFloat(index);
                    // The value we've read won't have had scale-offset-missing applied
                    val = (float)var.convertScaleOffsetMissing(val);
                    if (!Float.isNaN(val))
                    {
                        for (int pixelIndex : pixelMap.getPixelIndices(i, j))
                        {
                            picData.set(pixelIndex, val);
                        }
                    }
                }
            }
        }

        @Override
        protected boolean isPixelMapSorted() { return false; }
    },

    /**
     * Reads each data point individually.  Generally very inefficient and
     * recommended only for debugging and testing purposes.
     */
    PIXEL_BY_PIXEL {
        @Override
        protected void populatePixelArray(List<Float> picData,
            PixelMap pixelMap, VariableDS var, RangesList ranges)
            throws IOException, InvalidRangeException
        {
            logger.debug("Reading data using a pixel-by-pixel algorithm");
            long start = System.currentTimeMillis();

            Variable origVar = var.getOriginalVariable();

            // Now create the picture from the data array
            for (int j : pixelMap.getJIndices())
            {
                ranges.setYRange(new Range(j, j));
                for (int i : pixelMap.getIIndices(j))
                {
                    ranges.setXRange(new Range(i, i));
                    Array arr = origVar.read(ranges.getRanges());
                    // Get an index and set all elements to zero
                    Index index = arr.getIndex();
                    index.set(new int[index.getRank()]);
                    float val = arr.getFloat(index); // TODO: can we just use "0" instead of the index?
                    // The value we've read won't have had scale-offset-missing applied
                    val = (float)var.convertScaleOffsetMissing(val);
                    if (!Float.isNaN(val))
                    {
                        for (int pixelIndex : pixelMap.getPixelIndices(i, j))
                        {
                            picData.set(pixelIndex, val);
                        }
                    }
                }
            }
            logger.debug("Read data pixel-by-pixel in {} ms",
                (System.currentTimeMillis() - start));
        }

        /** Data reading is faster if the pixel map is sorted */
        @Override
        protected boolean isPixelMapSorted() { return true; }
    };
    
    /** Wraps a List of Ranges, providing methods to safely set ranges for
     * x, y, z and t */
    protected static final class RangesList
    {
        private static final Range ZERO_RANGE;

        private final List<Range> ranges;
        private final int xi;
        private final int yi;
        private final int zi;
        private final int ti;

        static
        {
            try { ZERO_RANGE = new Range(0, 0); }
            catch (InvalidRangeException ire) { throw new ExceptionInInitializerError(ire); }
        }
        
        public RangesList(GridDatatype grid, int tIndex, int zIndex)
                throws InvalidRangeException
        {
            int rank = grid.getRank();
            this.ranges = new ArrayList<Range>(rank);
            for (int i = 0; i < rank; i++) { ranges.add(ZERO_RANGE); }

            this.xi = grid.getXDimensionIndex();
            this.yi = grid.getYDimensionIndex();
            this.zi = grid.getZDimensionIndex();
            this.ti = grid.getTimeDimensionIndex();

            // Set the time and z ranges, avoiding InvalidRangeExceptions for
            // ranges we won't use
            if (tIndex < 0) tIndex = 0;
            if (zIndex < 0) zIndex = 0;
            this.setRange(this.ti, new Range(tIndex, tIndex));
            this.setRange(this.zi, new Range(zIndex, zIndex));
        }

        public void setXRange(Range xRange)
        {
            this.setRange(this.xi, xRange);
        }

        public void setYRange(Range yRange)
        {
            this.setRange(this.yi, yRange);
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
        
        public List<Range> getRanges() { return this.ranges; }

        @Override
        public String toString()
        {
            Range tRange = this.getRange(this.ti);
            Range zRange = this.getRange(this.zi);
            Range yRange = this.getRange(this.yi);
            Range xRange = this.getRange(this.xi);
            return String.format("tRange: %s, zRange: %s, yRange: %s, xRange: %s",
                tRange, zRange, yRange, xRange);
        }
    }

    /**
     * Reads data from the given GridDatatype and populates the given pixel array.
     * @param picData A List of the correct size, full of nulls.
     * @see PixelMap
     */
    public final List<Float> readData(int tIndex, int zIndex,
        HorizontalGrid sourceGrid, Domain<HorizontalPosition> targetDomain,
        GridDatatype grid) throws IOException
    {
        List<Float> picData = nullArrayList(targetDomain.getDomainObjects().size());
        PixelMap pixelMap;
        try
        {
            pixelMap = new PixelMap(sourceGrid, targetDomain, this.isPixelMapSorted());
        }
        catch (TransformException te)
        {
            throw new RuntimeException(te);
        }
        if (pixelMap.isEmpty()) return picData;
        try
        {
            RangesList rangesList = new RangesList(grid, tIndex, zIndex);
            this.populatePixelArray(picData, pixelMap, grid.getVariable(), rangesList);
        }
        catch (InvalidRangeException ire)
        {
            // This is a programming error from which we can't recover
            throw new IllegalStateException(ire);
        }
        return picData;
    }

    /**
     * Returns an ArrayList of null values of the given length
     */
    private static ArrayList<Float> nullArrayList(int n)
    {
        ArrayList<Float> list = new ArrayList<Float>(n);
        for (int i = 0; i < n; i++)
        {
            list.add((Float)null);
        }
        return list;
    }

    protected abstract boolean isPixelMapSorted();

    protected abstract void populatePixelArray(List<Float> picData,
            PixelMap pixelMap, VariableDS var, RangesList ranges)
        throws IOException, InvalidRangeException;

    private static final Logger logger = LoggerFactory.getLogger(DataReadingStrategy.class);
}
