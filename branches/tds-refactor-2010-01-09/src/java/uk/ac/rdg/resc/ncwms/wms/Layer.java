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

package uk.ac.rdg.resc.ncwms.wms;

import java.util.List;
import org.joda.time.DateTime;
import org.opengis.metadata.extent.GeographicBoundingBox;
import uk.ac.rdg.resc.ncwms.coordsys.HorizontalPosition;
import uk.ac.rdg.resc.ncwms.datareader.HorizontalGrid;
import uk.ac.rdg.resc.ncwms.datareader.PointList;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;

/**
 * A displayable layer, contained within a {@link Dataset}.
 * @param <T> The type of the data values contained in this layer
 * @todo allow for "stepless" time and elevation axes, plus regularly-spaced
 * ones that could save space in Capabilities docs.
 * @todo factor out a Dimension class, which could hold the values of a dimension,
 * the default value, any operations to find indices along the axis, plus how
 * the values can be rendered in a Capabilities doc (e.g. if regularly-spaced).
 * @author Jon
 */
public interface Layer<T>
{
    /** Returns the {@link Dataset} to which this layer belongs. */
    public Dataset getDataset();

    /** Returns an ID that is unique <b>within the {@link #getDataset() dataset}</b>. */
    public String getId();

    /** Returns a human-readable title */
    public String getTitle();

    /** Returns a (perhaps-lengthy) description of this layer */
    public String getAbstract();

    /**
     * Returns an identifier for this layer that is unique <b>on this server</b>.
     * This is used in the display of Layers in a Capabilities document and in
     * GetMap requests).
     * @return an identifier for this layer that is unique on this server.
     */
    public String getName();

    /**
     * Returns the layer's units.
     * @todo What if the layer has no units?  Empty string or null?
     * @todo Replace with strongly-typed JSR-275 Unit?
     */
    public String getUnits();

    /**
     * Returns true if this Layer can be queried through GetFeatureInfo
     */
    public boolean isQueryable();

    /**
     * Returns the geographic extent of this layer in latitude-longitude
     * coordinates.  Note that this extent is not necessarily precise so
     * specifying the coordinate system is unnecessary.
     * @return the geographic extent of this layer in WGS84 latitude-longitude.
     */
    public GeographicBoundingBox getGeographicBoundingBox();

    /**
     * Returns the list of time instants that are valid for this layer, in
     * chronological order, or an empty list if this Layer does not have a time axis.
     * @return the list of time instants that are valid for this layer, in
     * chronological order, or an empty list if this Layer does not have a time axis.
     */
    public List<DateTime> getTimeValues();

    /**
     * Get the time value that will be used by default if a client does not
     * explicitly provide a time parameter in a request ({@literal e.g.} GetMap),
     * or null if this layer does not support a default time value (or does not
     * have a time axis).
     * @return the default time value or null
     */
    public DateTime getDefaultTimeValue();

    /**
     * Returns the list of elevation values that are valid for this layer, or
     * an empty list if this Layer does not have a vertical axis.  Note that the
     * values in this list do not have to be ordered (although they usually will
     * be).  Clients must make no assumptions about ordering.
     * @return the list of elevation values that are valid for this layer, or
     * an empty list if this Layer does not have a vertical axis.
     */
    public List<Double> getElevationValues();

    /**
     * Get the elevation value that will be used by default if a client does not
     * explicitly provide an elevation parameter in a request ({@literal e.g.} GetMap),
     * or {@link Double#NaN} if this layer does not support a default elevation
     * value (or does not have an elevation axis).
     * @return the default elevation value or {@link Double#NaN}
     */
    public double getDefaultElevationValue();

    /**
     * Returns the units of the vertical axis
     * @todo What if the axis has no units?  Empty string or null?
     * @todo Replace with strongly-typed JSR-275 Unit?
     */
    public String getElevationUnits();

    /**
     * Returns the runtime type of the data values in this Layer
     * @return the runtime type of the data values in this Layer
     */
    public Class<T> getDataType();

    /**
     * <p>Reads a single item of data from a point in space and time.  Returns
     * null for points outside the domain of the layer, or for
     * missing values (e.g. land pixels in oceanography data).</p>
     * <p>This method will perform no interpolation in time or elevation, but
     * will perform nearest-neighbour interpolation in the horizontal.</p>
     * @param time The time instant for which we require data.  If this does not
     * match a time instant in {@link #getTimeValues()} an {@link InvalidDimensionValueException}
     * will be thrown.  (If this Layer has no time axis, this parameter will be ignored.)
     * @param elevation The elevation for which we require data (in the
     * {@link #getElevationUnits() units of this Layer's elevation axis}).  If
     * this does not match a valid {@link #getElevationValues() elevation value}
     * in this Layer, this method will throw an {@link InvalidDimensionValueException}.
     * (If this Layer has no elevation axis, this parameter will be ignored.)
     * @param xy The horizontal location from which this method will extract
     * data.  Data will be extracted from the nearest grid point to this location,
     * unless the point is outside the domain of the layer, in which case
     * null will be returned.
     * @return a single item of data from the given point in space and time
     * @throws NullPointerException if {@code xy} is null or if this
     * layer has a time axis and {@code time} is null.
     * @throws InvalidDimensionValueException if {@code elevation} is not a valid
     * elevation in this Layer, or if {@code time} is not a valid time in this
     * Layer.
     */
    public T readSinglePoint(DateTime time, double elevation, HorizontalPosition xy)
        throws InvalidDimensionValueException;
    
    /**
     * <p>Reads data at a number of horizontal locations at a single time and
     * elevation.  This is the method to use for reading a {@link HorizontalGrid}
     * of data.  Missing values (e.g. land pixels in oceanography data) will
     * be represented by null.</p>
     * <p>This method will perform no interpolation in time or elevation, but
     * will perform nearest-neighbour interpolation in the horizontal.</p>
     * @param time The time instant for which we require data.  If this does not
     * match a time instant in {@link #getTimeValues()} an {@link InvalidDimensionValueException}
     * will be thrown.  (If this Layer has no time axis, this parameter will be ignored.)
     * @param elevation The elevation for which we require data (in the
     * {@link #getElevationUnits() units of this Layer's elevation axis}).  If
     * this does not match a valid {@link #getElevationValues() elevation value}
     * in this Layer, this method will throw an {@link InvalidDimensionValueException}.
     * (If this Layer has no elevation axis, this parameter will be ignored.)
     * @param pointList The list of horizontal locations from which we are to
     * read data.  The returned List of data values will contain one value for
     * each item in this list in the same order.  This method will extract data
     * from the nearest grid points to each item in this list, returning
     * null for any points outside the domain of this Layer.
     * @return a List of data values, one for each point in
     * the {@code pointList}, in the same order.
     * @throws NullPointerException if {@code pointList} is null or if this 
     * layer has a time axis and {@code time} is null.
     * @throws InvalidDimensionValueException if {@code elevation} is not a valid
     * elevation in this Layer, or if {@code time} is not a valid time in this
     * Layer.
     */
    public List<T> readPointList(DateTime time, double elevation, PointList pointList)
        throws InvalidDimensionValueException;

    /**
     * <p>Reads a timeseries of data at a single xyz point from this Layer.
     * Missing values (e.g. land pixels in oceanography data will be represented
     * by null.</p>
     * <p>This method will perform no interpolation in time or elevation, but
     * will perform nearest-neighbour interpolation in the horizontal, i.e. it
     * will extract data from the nearest grid point to {@code xy}.  If {@code xy}
     * is outside the domain of this Layer, this method will return a List of
     * nulls, to retain consistency with other read...() methods
     * in this interface.</p>
     * @param times The list of time instants for which we require data.  If a
     * value in this list is not found in {@link #getTimeValues()}, this method
     * will throw an {@link InvalidDimensionValueException}.
     * @param elevation The elevation for which we require data (in the
     * {@link #getElevationUnits() units of this Layer's elevation axis}).  If
     * this does not match a valid {@link #getElevationValues() elevation value}
     * in this Layer, this method will throw an {@link InvalidDimensionValueException}.
     * (If this Layer has no elevation axis, this parameter will be ignored.)
     * @param xy The horizontal location from which this method will extract
     * data.  Data will be extracted from the nearest grid point to this location,
     * unless the point is outside the domain of the layer.
     * @return a List of data values, one for each point in
     * {@code times}, in the same order.
     * @throws NullPointerException if {@code times} or {@code xy} is null
     * @throws InvalidDimensionValueException if {@code elevation} is not a valid
     * elevation in this Layer, or if any of the {@code times} are not valid
     * times for this layer.
     * @todo what if this method is called on a Layer that has no time axis?
     */
    public List<T> readTimeseries(List<DateTime> times, double elevation,
        HorizontalPosition xy) throws InvalidDimensionValueException;

}
