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

package uk.ac.rdg.resc.ncwms.wms;

import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;
import uk.ac.rdg.resc.ncwms.coordsys.HorizontalPosition;
import uk.ac.rdg.resc.ncwms.datareader.PointList;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;

/**
 * Partial implementation of the {@link Layer} interface, providing convenience
 * methods and default implementations of some methods.
 * @author Jon
 */
public abstract class AbstractLayer implements Layer{

    /**
     * Returns a layer name created by joining the {@link Dataset#getId() dataset id}
     * with the {@link #getId() id of this layer}, separated by a forward slash.
     * @return an id for this layer that is unique on the server.
     */
    @Override
    public String getName()
    {
        return this.getDataset().getId() + "/" + this.getId();
    }

    /**
     * <p>Simple but naive implementation of
     * {@link Layer#readPointList(org.joda.time.DateTime, double,
     * uk.ac.rdg.resc.ncwms.datareader.PointList)} that makes repeated calls to
     * {@link Layer#readSinglePoint(org.joda.time.DateTime, double,
     * uk.ac.rdg.resc.ncwms.coordsys.HorizontalPosition)}.
     * This implementation is not expected to be maximally efficient and subclasses
     * are encouraged to override this.</p>
     * @return an unmodifiable List of data values
     */
    @Override
    public List<Float> readPointList(DateTime time, double elevation, PointList pointList)
            throws InvalidDimensionValueException
    {
        List<Float> vals = new ArrayList<Float>(pointList.size());
        for (HorizontalPosition xy : pointList.asList()) {
            vals.add(this.readSinglePoint(time, elevation, xy));
        }
        return vals;
    }

    /**
     * <p>Simple but naive implementation of
     * {@link Layer#readTimeseries(java.util.List, double,
     * uk.ac.rdg.resc.ncwms.coordsys.HorizontalPosition)} that makes repeated calls to
     * {@link Layer#readSinglePoint(org.joda.time.DateTime, double,
     * uk.ac.rdg.resc.ncwms.coordsys.HorizontalPosition)}. This implementation
     * is not expected to be maximally efficient and subclasses are encouraged
     * to override this.</p>
     */
    @Override
    //Behaviour with invalid time values doesn't match contract!
    public List<Float> readTimeseries(List<DateTime> times, double elevation,
        HorizontalPosition xy) throws InvalidDimensionValueException
    {
        List<Float> vals = new ArrayList<Float>(times.size());
        for (DateTime time : times) {
            vals.add(this.readSinglePoint(time, elevation, xy));
        }
        return vals;
    }



}
