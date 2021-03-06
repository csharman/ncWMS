
/* Copyright (c) 2006 The University of Reading
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

/**
 * The same as NemoDataReaderTwoDegree, but uses different look-up tables.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class NemoDataReaderTwoDegreeDiad extends NemoDataReaderTwoDegree
{
    /**
     * Gets the location of the x axis' LUT
     */
    @Override
    protected String getXAxisLUTLocation()
    {
        return "/uk/ac/rdg/resc/ncwms/metadata/NEMO_2DEG_DIAD.zip/ORCA2_LUT_i_3601_1801.dat";
    }
    
    /**
     * Gets the location of the y axis' LUT
     */
    @Override
    protected String getYAxisLUTLocation()
    {
        return "/uk/ac/rdg/resc/ncwms/metadata/NEMO_2DEG_DIAD.zip/ORCA2_LUT_j_3601_1801.dat";
    }
}
