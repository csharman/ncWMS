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

package uk.ac.rdg.resc.ncwms.metadata;

import com.sleepycat.persist.model.Persistent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.log4j.Logger;
import ucar.unidata.geoloc.LatLonPoint;

/**
 * Coordinate axis defined by a look-up table, as generated by Greg Smith's code
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Persistent
public class LUTCoordAxis extends EnhancedCoordAxis
{
    private static final Logger logger = Logger.getLogger(LUTCoordAxis.class);
    
    /**
     * Maps filenames to axes
     */
    private static final Hashtable<String, LUTCoordAxis> lutAxes =
        new Hashtable<String, LUTCoordAxis>();
    
    private short[] indices;
    private Regular1DCoordAxis xAxis, yAxis; // Used to convert from lat/lon to LUT index
    
    /**
     * Creates a LUTCoordAxis from the LUT in the given filename.  If an
     * LUTCoordAxis has already been generated by this filename, this returns it
     * @throws IOException if there was an error reading from the file
     */
    public static LUTCoordAxis createAxis(String filename) throws IOException
    {
        if (!lutAxes.containsKey(filename))
        {
            InputStream in = null;
            File file = new File(filename);
            File parentFile = file.getParentFile();
            if (parentFile == null)
            {
                throw new FileNotFoundException(filename);
            }
            else if (parentFile.getPath().endsWith(".zip"))
            {
                InputStream zipIn = in = Thread.currentThread()
                    .getContextClassLoader().getResourceAsStream(parentFile.getPath());
                in = new ZipInputStream(zipIn);
                // Skip to the required entry
                while (true)
                {
                    ZipEntry entry = ((ZipInputStream)in).getNextEntry();
                    if (entry == null)
                    {
                        // We have gone past the last entry
                        throw new FileNotFoundException(file.getName() +
                            " not found in zip file " + parentFile.getPath());
                    }
                    else if (entry.getName().equals(file.getName()))
                    {
                        break; // exit the loop
                    }
                }
            }
            else
            {
                // This is a regular, uncompressed file
                in = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(filename);
            }
            lutAxes.put(filename, createAxis(in, filename));
        }
        return lutAxes.get(filename);
    }
    
    /**
     * Creates an LUTCoordAxis
     * @param in InputStream from which to read the look-up table
     * @param filename Name of file from which we are reading (for error
     * reporting purposes)
     * @return a newly-created LUTCoordAxis
     */
    private static final LUTCoordAxis createAxis(InputStream in, String filename) throws IOException
    {        
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new InputStreamReader(in));
            String line = null;
            
            // Read the header information
            int headerLinesRead = 0;
            double minLon = 0.0;
            double maxLon = 0.0;
            double minLat = 0.0;
            double maxLat = 0.0;
            int nlon = 0;
            int nlat = 0;
            do
            {
                line = reader.readLine();
                // Ignore comment lines
                if (line != null && !line.trim().startsWith("#"))
                {
                    StringTokenizer tok = new StringTokenizer(line);
                    if (tok.countTokens() != 2)
                    {
                        throw new IOException("Header format error in " + filename);
                    }
                    if (headerLinesRead == 0)
                    {
                        // We are reading the longitude range
                        minLon = Double.parseDouble(tok.nextToken());
                        maxLon = Double.parseDouble(tok.nextToken());
                    }
                    else if (headerLinesRead == 1)
                    {
                        // We are reading the latitude range
                        minLat = Double.parseDouble(tok.nextToken());
                        maxLat = Double.parseDouble(tok.nextToken());
                    }
                    else
                    {
                        // We are reading the number of points in each direction
                        nlon = Integer.parseInt(tok.nextToken());
                        nlat = Integer.parseInt(tok.nextToken());
                    }
                    headerLinesRead++;
                }
            } while (line != null && headerLinesRead < 3);
            if (headerLinesRead < 3)
            {
                throw new IOException("Header information incomplete in " + filename);
            }
            logger.debug("minLon = {}, maxLon = {}, minLat = {}, maxLat {}, nlon = {}, nlat = {}",
                new Object[]{minLon, maxLon, minLat, maxLat, nlon, nlat});
            
            // Now read the look-up table itself
            int i = 0;
            short[] indices = new short[nlon * nlat];
            do
            {
                line = reader.readLine();
                // Ignore comment lines that start with #
                if (line != null && !line.trim().startsWith("#"))
                {
                    StringTokenizer tok = new StringTokenizer(line);
                    while(tok.hasMoreTokens())
                    {
                        indices[i] = Short.parseShort(tok.nextToken());
                        // Files were produced using FORTRAN, hence indices are 1-based
                        indices[i] -= 1;
                        i++;
                    }
                }
            } while (line != null);
            logger.debug("Read {} items of lookup data from {}", i, filename);
            // Garbage-collect to try to free some memory
            System.gc();
            return new LUTCoordAxis(indices, minLon, maxLon, minLat, maxLat,
                nlon, nlat);
        }
        catch(RuntimeException rte)
        {
            logger.error("Runtime error reading from " + filename + ": ", rte);
            throw rte;
        }
        finally
        {
            if (reader != null)
            {
                // Close the reader, ignoring error messages
                try { reader.close(); } catch (IOException ioe) {}
            }
        }
    }
    
    private LUTCoordAxis(short[] indices, double minLon, double maxLon,
        double minLat, double maxLat, int nlon, int nlat)
    {
        this.indices = indices;
        double lonStride = (maxLon - minLon) / (nlon - 1);
        double latStride = (maxLat - minLat) / (nlat - 1);
        this.xAxis = new Regular1DCoordAxis(minLon, lonStride, nlon, true);
        this.yAxis = new Regular1DCoordAxis(minLat, latStride, nlat, false);
    }
    
    public int getIndex(LatLonPoint point)
    {
        int xi = this.xAxis.getIndex(point);
        int yi = this.yAxis.getIndex(point);
        if (xi >= 0 && yi >= 0)
        {
            return this.indices[yi * this.xAxis.getCount() + xi];
        }
        else
        {
            return -1;
        }
    }
    
}
