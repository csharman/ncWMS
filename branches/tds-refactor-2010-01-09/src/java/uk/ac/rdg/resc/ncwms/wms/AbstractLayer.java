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
import java.util.Collections;
import java.util.List;
import org.geotoolkit.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.joda.time.DateTime;
import org.opengis.metadata.extent.GeographicBoundingBox;
import uk.ac.rdg.resc.ncwms.coordsys.HorizontalPosition;
import uk.ac.rdg.resc.ncwms.datareader.PointList;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.utils.WmsUtils;

/**
 * Partial implementation of the {@link Layer} interface, providing convenience
 * methods and default implementations of some methods.  Most properties are
 * set through the provided setter methods.
 * @param <T> The type of the data values contained in this layer
 * @todo implement a makeImmutable() method, which prevents futher changes?
 * This could be called by the metadata-reading operation to ensure that
 * all future operations are read-only.
 * @author Jon
 */
public abstract class AbstractLayer<T> implements Layer<T>
{
    protected String id;
    protected String title = null;
    protected String abstr = null; // "abstract" is a reserved word in Java
    protected String units;
    protected String zUnits;
    protected List<Double> zValues;
    protected GeographicBoundingBox bbox = DefaultGeographicBoundingBox.WORLD;

    /**
     * Creates an AbstractLayer with a bounding box that covers the whole world
     * and the given identifier.
     * @param id An identifier that is unique within this layer's
     * {@link #getDataset() dataset}.
     * @throws NullPointerException if {@code id == null}
     */
    public AbstractLayer(String id)
    {
        if (id == null) throw new NullPointerException("id cannot be null");
        this.id = id;
    }

    @Override public String getId() { return this.id; }

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

    @Override public String getTitle() { return this.title; }
    public void setTitle(String title) { this.title = title; }

    @Override public String getAbstract() { return this.abstr; }
    public void setAbstract(String abstr) { this.abstr = abstr; }

    @Override public String getUnits() { return this.units; }
    public void setUnits(String units) { this.units = units; }

    @Override public String getElevationUnits() { return this.zUnits; }
    public void setElevationUnits(String zUnits) { this.zUnits = zUnits; }

    @Override public List<Double> getElevationValues() { return zValues; }
    public void setElevationValues(List<Double> zValues) { this.zValues = zValues; }

    @Override
    public GeographicBoundingBox getGeographicBoundingBox() { return this.bbox; }
    public void setGeographicBoundingBox(GeographicBoundingBox bbox) { this.bbox = bbox; }
    /** bbox = [minx, miny, maxx, maxy]. */
    public void setGeographicBoundingBox(double[] bbox)
    {
        if (bbox == null) throw new NullPointerException();
        if (bbox.length != 4) throw new IllegalArgumentException("Bounding box must have four elements");
        // Note that these arguments are different from OGC BBOX order!
        this.bbox = new DefaultGeographicBoundingBox(bbox[0], bbox[2], bbox[1], bbox[3]);
    }

    /**
     * Returns true if this layer has a time axis.  This is a convenience method
     * that simply checks if the length of the list of timesteps is non-zero.
     */
    protected boolean hasTimeAxis() { return this.getTimeValues().size() > 0; }

    /**
     * Returns true if this layer has an elevation axis.  This is a convenience
     * method that simply checks if the length of the list of elevation values is
     * non-zero.
     */
    protected boolean hasElevationAxis() { return this.getElevationValues().size() > 0; }
    
    /**
     * Gets the time value that will be used by default if a client does not
     * explicitly provide a time parameter in a request ({@literal e.g. GetMap}),
     * or null if this layer does not have a time axis.  This returns the time
     * value that is closest to the current time.
     */
    @Override
    public DateTime getDefaultTimeValue()
    {
        int currentTimeIndex = this.getCurrentTimeIndex();
        if (currentTimeIndex < 0) return null; // this layer doesn't have a time axis
        return this.getTimeValues().get(currentTimeIndex);
    }

    /**
     * Gets the index in the {@link #getTimeValues() list of valid timesteps}
     * of the past or present timestep that is closest to the current time, or
     * -1 if this layer does not have a time axis.  If all timesteps in this Layer
     * are in the future then this will return 0.
     * @return the index in the {@link #getTimeValues() list of valid timesteps}
     * of the timestep that is closest to the current time, or -1 if this layer
     * does not have a time axis.
     * @todo should this always be a time in the past or present (not the future)
     * unless all the values are in the future?
     */
    protected int getCurrentTimeIndex()
    {
        if (this.getTimeValues().size() == 0) return -1; // no time axis
        int index = findTimeIndex(new DateTime());
        if (index >= 0) {
            // Exact match.  Very unlikely!
            return index;
        } else {
            // We can calculate the insertion point
            int insertionPoint = -(index + 1); // see docs for Collections.binarySearch()
            // We return the index of the most recent past time
            if (insertionPoint > 0) return insertionPoint - 1; // The most recent past time
            else return 0; // All DateTimes on the axis are in the future, so we take the earliest
        }
    }

    /**
     * Searches the list of timesteps for the specified date-time using the binary
     * search algorithm.  Matches are found based only upon the millisecond
     * instant of the passed DateTime, not its Chronology.
     * @param  target The timestep to search for.
     * @return the index of the search key, if it is contained in the list;
     *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
     *	       <i>insertion point</i> is defined as the point at which the
     *	       key would be inserted into the list: the index of the first
     *	       element greater than the key, or <tt>list.size()</tt> if all
     *	       elements in the list are less than the specified key.  Note
     *	       that this guarantees that the return value will be &gt;= 0 if
     *	       and only if the key is found.  If this Layer does not have a time
     *         axis this method will return -1.
     */
    protected int findTimeIndex(DateTime target)
    {
        return Collections.binarySearch(
            this.getTimeValues(),         // The list of timesteps
            target,                       // The target value
            WmsUtils.DATE_TIME_COMPARATOR // Comparator based upon ms values only
        );
    }

    /**
     * Searches the list of timesteps for the specified date-time, returning
     * the index of the date-time, or throwing an {@link InvalidDimensionValueException}
     * if the specified date-time is not a valid timestep for this layer.  If
     * this layer does not have an elevation axis, this will return -1.
     */
    protected int findAndCheckTimeIndex(DateTime target) throws InvalidDimensionValueException
    {
        if (!this.hasTimeAxis()) return -1;
        int index = this.findTimeIndex(target);
        if (index >= 0) return index;
        throw new InvalidDimensionValueException("time", WmsUtils.dateTimeToISO8601(target));
    }

    /**
     * <p>Gets the elevation value that will be used by default if a client does not
     * explicitly provide an elevation parameter in a request ({@literal e.g. GetMap}),
     * or {@link Double#NaN} if this layer does not support a default elevation
     * value (or does not have an elevation axis).</p>
     * <p>This implementation simply returns the value that is closest to zero
     * (zero will usually be the surface), or NaN if this layer doesn't have
     * an elevation axis.</p>
     * @return the default elevation value or {@link Double#NaN}
     */
    @Override
    public double getDefaultElevationValue()
    {
        // We must access this via the accessor method in case subclasses override it.
        List<Double> zVals = this.getElevationValues();
        if (zVals.size() == 0) return Double.NaN;
        double minAbsValue = Double.MAX_VALUE;
        for (double zVal : zVals)
        {
            minAbsValue = Math.min(minAbsValue, Math.abs(zVal));
        }
        return minAbsValue;
    }

    /**
     * Finds the index of a certain z value (within the {@link #getElevationValues()
     * list of elevation values}) by brute-force search.  We can afford
     * to be inefficient here because z axes are not likely to be large.
     * @param targetVal Value to search for
     * @return the z index corresponding with the given targetVal, or -1 if
     * targetVal is not found in the list of elevation values, or if this
     * layer does not have an elevation axis.
     */
    protected int findElevationIndex(double targetVal)
    {
        // We must access this via the accessor method in case subclasses override it.
        List<Double> zVals = this.getElevationValues();
        for (int i = 0; i < zVals.size(); i++)
        {
            // The fuzzy comparison fails for zVal == 0.0 so we do a direct
            // comparison too
            if (zVals.get(i) == targetVal ||
                Math.abs((zVals.get(i) - targetVal) / targetVal) < 1e-5)
            {
                return i;
            }
        }
        return -1;
    }

    /**
     * Searches the list of elevation values for the specified value, returning
     * the index of the elevation value, or throwing an {@link InvalidDimensionValueException}
     * if the specified elevation is not valid for this layer.  If this layer
     * does not have an elevation axis, this will return -1.
     */
    protected int findAndCheckElevationIndex(double targetVal) throws InvalidDimensionValueException
    {
        if (!this.hasElevationAxis()) return -1;
        int index = this.findElevationIndex(targetVal);
        if (index >= 0) return index;
        throw new InvalidDimensionValueException("elevation", "" + targetVal);
    }

    /**
     * <p>Simple but naive implementation of
     * {@link Layer#readPointList(org.joda.time.DateTime, double,
     * uk.ac.rdg.resc.ncwms.datareader.PointList) Layer.readPointList()} that
     * makes repeated calls to
     * {@link Layer#readSinglePoint(org.joda.time.DateTime, double,
     * uk.ac.rdg.resc.ncwms.coordsys.HorizontalPosition) Layer.readSinglePoint()}.
     * This implementation is not expected to be maximally efficient and subclasses
     * are encouraged to override this.</p>
     * @return a List of data values
     */
    @Override
    public List<T> readPointList(DateTime time, double elevation, PointList pointList)
            throws InvalidDimensionValueException
    {
        List<T> vals = new ArrayList<T>(pointList.size());
        for (HorizontalPosition xy : pointList.asList()) {
            vals.add(this.readSinglePoint(time, elevation, xy));
        }
        return vals;
    }

    /**
     * <p>Simple but naive implementation of
     * {@link Layer#readTimeseries(java.util.List, double,
     * uk.ac.rdg.resc.ncwms.coordsys.HorizontalPosition) Layer.readTimeseries()}
     * that makes repeated
     * calls to {@link Layer#readSinglePoint(org.joda.time.DateTime, double,
     * uk.ac.rdg.resc.ncwms.coordsys.HorizontalPosition) Layer.readSinglePoint()}.
     * This implementation
     * is not expected to be maximally efficient and subclasses are encouraged
     * to override this.</p>
     */
    @Override
    public List<T> readTimeseries(List<DateTime> times, double elevation,
        HorizontalPosition xy) throws InvalidDimensionValueException
    {
        // TODO: could check validity of all the times before we start
        // potentially-lengthy data-reading operations
        List<T> vals = new ArrayList<T>(times.size());
        for (DateTime time : times) {
            vals.add(this.readSinglePoint(time, elevation, xy));
        }
        return vals;
    }
    
}
