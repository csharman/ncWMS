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

package uk.ac.rdg.resc.ncwms.datareader;

import org.apache.log4j.Logger;
import ucar.ma2.Array;
import ucar.ma2.Range;
import ucar.nc2.dataset.EnhanceScaleMissingImpl;
import ucar.nc2.dataset.grid.GeoGrid;

/**
 * DataReader for OPeNDAP datasets.  This is very similar to the DefaultDataReader
 * except that it's designed to make as few requests to the OPeNDAP server as
 * possible: it downloads a large chunk of data in a single request, then does
 * the subsetting locally.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class OpendapDataReader extends DefaultDataReader
{
    private static final Logger logger = Logger.getLogger(OpendapDataReader.class);
    
    /**
     * Reads data from the given GeoGrid and populates the given pixel array.
     * This uses a bounding-box algorithm: all data from the surrounding bounding
     * box are extracted in one go, then the necessary points are picked out.
     * This often leads to a lot of data being read from disk, but only one
     * read operation is required.
     */
    protected void populatePixelArray(float[] picData, Range tRange, Range zRange,
        PixelMap pixelMap, GeoGrid gg) throws Exception
    {
        // Get an enhanced version of the variable for dealing with missing values etc
        EnhanceScaleMissingImpl enhanced = getEnhanced(gg);

        // Read the whole chunk of x-y data
        Range xRange = new Range(pixelMap.getMinXIndex(), pixelMap.getMaxXIndex());
        Range yRange = new Range(pixelMap.getMinYIndex(), pixelMap.getMaxYIndex());
        long start = System.currentTimeMillis();
        GeoGrid subset = gg.subset(tRange, zRange, yRange, xRange);
        Array arr = subset.readYXData(0,0);
        logger.debug("Rank of arr = {}", arr.getRank());
        for (int i : arr.getShape())
        {
            logger.debug("Dimension size = {}", i);
        }
        DataChunk dataChunk = new DataChunk(arr);
        long readData = System.currentTimeMillis();
        logger.debug("Read data over OPeNDAP in {} milliseconds", (readData - start));

        // Now create the picture from the data array
        for (int yIndex : pixelMap.getYIndices())
        {
            for (int xIndex : pixelMap.getXIndices(yIndex))
            {
                try
                {
                    float val = dataChunk.getValue(yIndex - pixelMap.getMinYIndex(),
                        xIndex - pixelMap.getMinXIndex());
                    // We unpack and check for missing values just for
                    // the points we need to display.
                    val = (float)enhanced.convertScaleOffsetMissing(val);
                    for (int pixelIndex : pixelMap.getPixelIndices(xIndex, yIndex))
                    {
                        picData[pixelIndex] = val;
                    }
                }
                catch(ArrayIndexOutOfBoundsException aioobe)
                {
                    logger.error("Array index ({},{}) out of bounds",
                        yIndex - pixelMap.getMinYIndex(), xIndex - pixelMap.getMinXIndex());
                    throw aioobe;
                }
            }
        }
    }
    
}
