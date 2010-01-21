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

import java.util.List;
import org.joda.time.DateTime;
import org.opengis.metadata.extent.GeographicBoundingBox;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;

/**
 * Implementation of a {@link VectorLayer} that wraps two Layer objects,
 * one for the eastward and one for the northward component.  Most of the
 * properties are derived directly from the eastward component.  The components
 * must share the same domain, although this class does not verify this.
 * @author Jon
 */
public class VectorLayerImpl implements VectorLayer
{
    private final String id;
    private final ScalarLayer east;
    private final ScalarLayer north;

    public VectorLayerImpl(String id, ScalarLayer east, ScalarLayer north)
    {
        this.id = id;
        this.east = east;
        this.north = north;
        // TODO add vector as supported Style
    }

    @Override
    public ScalarLayer getEastwardComponent() { return this.east; }

    @Override
    public ScalarLayer getNorthwardComponent() { return this.north; }

    @Override
    public String getId() { return this.id; }

    @Override
    public String getTitle() { return this.id; }

    @Override
    public String getAbstract() {
        return "Automatically-generated vector field, composed of the fields "
            + this.east.getTitle() + " and " + this.north.getTitle(); 
    }

    /**
     * Returns a layer name that is unique on this server, created from the
     * {@link #getDataset() dataset} id and the {@link #getId() layer id} by the
     * {@link WmsUtils#createUniqueLayerName(java.lang.String, java.lang.String)}
     * method.
     */
    @Override
    public String getName()
    {
        return WmsUtils.createUniqueLayerName(this.getDataset().getId(), this.getId());
    }

    @Override
    public Dataset getDataset() { return this.east.getDataset(); }

    @Override
    public String getUnits() { return this.east.getUnits(); }

    @Override
    public boolean isQueryable() { return this.east.isQueryable(); }

    @Override
    public GeographicBoundingBox getGeographicBoundingBox() { return this.east.getGeographicBoundingBox(); }

    @Override
    public List<DateTime> getTimeValues() { return this.east.getTimeValues(); }

    @Override
    public DateTime getCurrentTimeValue() { return this.east.getCurrentTimeValue(); }

    @Override
    public DateTime getDefaultTimeValue() { return this.east.getDefaultTimeValue(); }

    @Override
    public List<Double> getElevationValues() { return this.east.getElevationValues(); }

    @Override
    public double getDefaultElevationValue() { return this.east.getDefaultElevationValue(); }

    @Override
    public String getElevationUnits() { return this.east.getElevationUnits(); }
}
