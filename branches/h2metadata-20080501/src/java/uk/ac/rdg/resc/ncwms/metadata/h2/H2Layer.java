/*
 * Copyright (c) 2008 The University of Reading
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

package uk.ac.rdg.resc.ncwms.metadata.h2;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import uk.ac.rdg.resc.ncwms.config.Dataset;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.metadata.CoordAxis;
import uk.ac.rdg.resc.ncwms.metadata.Irregular1DCoordAxis;
import uk.ac.rdg.resc.ncwms.metadata.Layer;
import uk.ac.rdg.resc.ncwms.metadata.PositiveDirection;
import uk.ac.rdg.resc.ncwms.metadata.TimestepInfo;
import uk.ac.rdg.resc.ncwms.metadata.projection.HorizontalProjection;
import uk.ac.rdg.resc.ncwms.styles.Style;
import uk.ac.rdg.resc.ncwms.utils.WmsUtils;

/**
 * A Layer that is linked to the H2 database.  Simple properties (id, title,
 * description etc) are loaded immediately, but other properties (e.g. axes)
 * are loaded lazily (i.e. on-demand) from the database.  This helps avoid
 * loading large amounts of data (e.g. long time axes) unnecessarily.
 * @todo Some of this repeats code in LayerImpl: refactor?
 * @author Jon
 */
class H2Layer implements Layer
{
    H2MetadataStore metadataStore;
    
    long id; /* Unique ID as stored in the database */
    String internalName; /* This is unique only within a dataset */
    String title;
    String abstr;
    String units;
    double[] bbox;
    float scaleMin;
    float scaleMax;
    Dataset dataset;
    
    // Stores the styles that this variable supports
    private List<Style> supportedStyles = new ArrayList<Style>();
    
    long xAxisId;
    long yAxisId;
    Long zAxisId; // can be null
    private CoordAxis xAxis;
    private CoordAxis yAxis;
    private Irregular1DCoordAxis zAxis;
    HorizontalProjection horizProj = HorizontalProjection.LON_LAT_PROJECTION;
    
    private List<TimestepInfo> timesteps;
    private long[] tValues;
    
    public H2Layer()
    {
        this.supportedStyles.add(Style.BOXFILL);
    }
    
    
    /// SIMPLE PROPERTIES ///

    public String getId()
    {
        return this.internalName;
    }

    public String getTitle()
    {
        return this.title;
    }

    public String getAbstract()
    {
        return this.abstr;
    }

    public String getUnits()
    {
        return this.units;
    }
    
    public double[] getBbox()
    {
        return this.bbox;
    }

    public float getScaleMax()
    {
        return this.scaleMax;
    }

    public float getScaleMin()
    {
        return this.scaleMin;
    }

    public float[] getScaleRange()
    {
        return new float[]{this.scaleMin, this.scaleMax};
    }

    public String getLayerName()
    {
        return WmsUtils.createUniqueLayerName(this.dataset.getId(), this.internalName);
    }

    public boolean isQueryable()
    {
        return this.dataset.isQueryable();
    }
    
    public String getCopyrightStatement()
    {
        return this.dataset.getCopyrightStatement();
    }

    public Dataset getDataset()
    {
        return this.dataset;
    }
    
    
    /// STYLES ///

    /**
     * @return List of styles that this layer can be rendered in.
     * @todo Repeats code from LayerImpl
     */
    public List<Style> getSupportedStyles()
    {
        return this.supportedStyles;
    }
    
    /**
     * @return the key of the default style for this Variable.  Exactly 
     * equivalent to getSupportedStyles().get(0)
     * @todo Repeats code from LayerImpl
     */
    public Style getDefaultStyle()
    {
        // Could be an IndexOutOfBoundsException here, but would be a programming
        // error if so
        return this.supportedStyles.get(0);
    }
    
    /**
     * @return true if this Variable can be rendered in the style with the 
     * given name, false otherwise.
     * @todo Repeats code from LayerImpl
     */
    public boolean supportsStyle(String styleName)
    {
        return this.supportedStyles.contains(styleName.trim());
    }
    
    
    /// HORIZONTAL AXES ///

    /**
     * Gets the X axis for this layer, loading it from the H2 database if
     * necessary.
     */
    public CoordAxis getXaxis()
    {
        if (this.xAxis == null)
        {
            this.xAxis = this.metadataStore.getCoordAxis(this.xAxisId);
        }
        return this.xAxis;
    }
    
    /**
     * Gets the X axis for this layer, loading it from the H2 database if
     * necessary.
     */
    public CoordAxis getYaxis()
    {
        if (this.yAxis == null)
        {
            this.yAxis = this.metadataStore.getCoordAxis(this.yAxisId);
        }
        return this.yAxis;
    }

    public HorizontalProjection getHorizontalProjection()
    {
        return this.horizProj;
    }
    
    
    /// VERTICAL AXIS ///
    /// TODO: much of this should be refactored into Irregular1DCoordAxis or similar
    /// Too many methods!

    public boolean isZaxisPresent()
    {
        return this.zAxisId != null;
    }

    /**
     * @todo copied from LayerImpl: refactor!
     */
    public int findZIndex(String targetVal) throws InvalidDimensionValueException
    {
        this.loadZAxis();
        double[] zValues = this.zAxis.getCoordValues();
        try
        {
            float zVal = Float.parseFloat(targetVal);
            for (int i = 0; i < zValues.length; i++)
            {
                // The fuzzy comparison fails for zVal == 0.0 so we do a direct
                // comparison too
                if (zValues[i] == zVal || Math.abs((zValues[i] - zVal) / zVal) < 1e-5)
                {
                    return i;
                }
            }
            throw new InvalidDimensionValueException("elevation", targetVal);
        }
        catch(NumberFormatException nfe)
        {
            throw new InvalidDimensionValueException("elevation", targetVal);
        }
    }

    /**
     * @todo copied from LayerImpl: refactor!
     */
    public int getDefaultZIndex()
    {
        return 0;
    }
    
    /**
     * @todo copied from LayerImpl: refactor!
     */
    public double getDefaultZValue()
    {
        this.loadZAxis();
        double[] zValues = this.zAxis.getCoordValues();
        return zValues[this.getDefaultZIndex()];
    }

    public String getZunits()
    {
        this.loadZAxis();
        return this.zAxis.getUnits();
    }

    public double[] getZvalues()
    {
        this.loadZAxis();
        return this.zAxis.getCoordValues();
    }

    public boolean isZpositive()
    {
        this.loadZAxis();
        return this.zAxis.getPositiveDirection() == PositiveDirection.POSITIVE_UP;
    }
    
    /**
     * Loads the z axis from the metadata store, or does nothing if already
     * loaded
     */
    private void loadZAxis()
    {
        if (this.zAxis == null)
        {
            this.zAxis = (Irregular1DCoordAxis)this.metadataStore.getCoordAxis(this.zAxisId);
        }
    }
    
    
    /// TIME AXIS   MAJOR REFACTORING NEEDED! ///
    
    public boolean isTaxisPresent()
    {
        // TODO: would be better if the variable had a tAxisPresent boolean
        // property
        loadTimesteps();
        return this.timesteps.size() > 0;
    }

    /**
     * @todo Copied from LayerImpl: refactor!
     * @todo Should query the database directly: we don't need to have the entire
     * time axis in memory
     */
    public int findTIndex(String isoDateTime) throws InvalidDimensionValueException
    {
        if (isoDateTime.equals("current"))
        {
            // Return the last timestep
            // TODO: should be the index of the timestep closest to now
            return this.getLastTIndex();
        }
        Date target = WmsUtils.iso8601ToDate(isoDateTime);
        if (target == null)
        {
            throw new InvalidDimensionValueException("time", isoDateTime);
        }
        int index = findTIndex(target);
        if (index < 0)
        {
            throw new InvalidDimensionValueException("time", isoDateTime);
        }
        return index;
    }
    
    /**
     * @todo copied from LayerImpl: refactor!
     * @todo We shouldn't be finding the index within the array of timesteps:
     * we should be finding the file and the index in the file from the database
     * directly
     */
    private int findTIndex(Date target)
    {
        loadTimesteps();
        if (this.timesteps.size() == 0) return -1;
        // Check that the point is within range
        if (target.before(this.timesteps.get(0).getDate()) ||
            target.after(this.timesteps.get(this.timesteps.size()  - 1).getDate()))
        {
            return -1;
        }
        
        // do a binary search to find the nearest index
        int low = 0;
        int high = this.timesteps.size() - 1;
        while (low <= high)
        {
            int mid = (low + high) >> 1;
            Date midVal = this.timesteps.get(mid).getDate();
            if (midVal.equals(target)) return mid;
            else if (midVal.before(target)) low = mid + 1;
            else high = mid - 1;
        }
        
        // If we've got this far we have to decide between values[low]
        // and values[high]
        if (this.timesteps.get(low).getDate().equals(target)) return low;
        else if (this.timesteps.get(high).getDate().equals(target)) return high;
        // The given time doesn't match any axis value
        return -1;
    }

    /**
     * @todo copied from LayerImpl: refactor!
     */
    public List<Integer> findTIndices(String isoDateTimeStart, String isoDateTimeEnd)
        throws InvalidDimensionValueException
    {
        int startIndex = this.findTIndex(isoDateTimeStart);
        int endIndex = this.findTIndex(isoDateTimeEnd);
        if (startIndex > endIndex)
        {
            throw new InvalidDimensionValueException("time",
                isoDateTimeStart + "/" + isoDateTimeEnd);
        }
        List<Integer> tIndices = new ArrayList<Integer>();
        for (int i = startIndex; i <= endIndex; i++)
        {
            tIndices.add(i);
        }
        return tIndices;
    }

    /**
     * @todo copied from LayerImpl: refactor!
     */
    public int getDefaultTIndex()
    {
        return this.getLastTIndex();
    }
    
    /**
     * @todo copied from LayerImpl: refactor!
     */
    public long getDefaultTValue()
    {
        return this.getTvalues()[this.getDefaultTIndex()];
    }

    /**
     * @todo copied from LayerImpl: refactor!
     */
    public int getLastTIndex()
    {
        return this.timesteps.size() - 1;
    }

    public List<TimestepInfo> getTimesteps()
    {
        loadTimesteps();
        return this.timesteps;
    }

    public long[] getTvalues()
    {
        loadTimesteps();
        return this.tValues;
    }
    
    private void loadTimesteps()
    {
        if (this.timesteps == null)
        {
            this.timesteps = this.metadataStore.getTimesteps(this.id);
            // TODO: copied from LayerImpl: refactor!
            this.tValues = new long[this.timesteps.size()];
            int i = 0;
            for (TimestepInfo tInfo : this.timesteps)
            {
                this.tValues[i] = tInfo.getDate().getTime();
                i++;
            }
        }
    }

}
