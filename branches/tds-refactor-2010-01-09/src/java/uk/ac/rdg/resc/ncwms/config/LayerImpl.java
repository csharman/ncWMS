/*
 * Copyright (c) 2006 The University of Reading
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

package uk.ac.rdg.resc.ncwms.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.rdg.resc.ncwms.coordsys.HorizontalCoordSys;
import uk.ac.rdg.resc.ncwms.datareader.DataReader;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.styles.Style;
import uk.ac.rdg.resc.ncwms.utils.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.Layer;

/**
 * Concrete implementation of the Layer interface.  Stores the metadata for
 * a layer in the WMS
 *
 * @author Jon Blower
 */
public class LayerImpl implements Layer
{
    private static final Logger logger = LoggerFactory.getLogger(LayerImpl.class);
    
    protected boolean zPositive;
    protected HorizontalCoordSys horizCoordSys;
    // Stores the keys of the styles that this variable supports
    protected List<Style> supportedStyles = new ArrayList<Style>();
    
    /**
     * Creates a new Layer using a default bounding box (covering the whole 
     * earth) and with a default boxfill style
     */
    public LayerImpl()
    {
        this.supportedStyles.add(Style.BOXFILL);
    }

    /**
     * Gets the human-readable Title of this Layer.  If the sysadmin has set a
     * title for this layer in the config file, this title will be returned.
     * If not, the title that was read in the relevant {@link DataReader} will
     * be used.
     * @return
     */
    @Override
    public String getTitle()
    {
        Variable var = this.getVariable();
        if (var != null && var.getTitle() != null) return var.getTitle();
        else return this.title;
    }

    public boolean isZpositive()
    {
        return zPositive;
    }

    public void setZpositive(boolean zPositive)
    {
        this.zPositive = zPositive;
    }
    /**
     * @return array of two doubles, representing the min and max of the scale range
     * Note that this is not the same as a "valid_max" for the dataset.  This is
     * simply a hint to visualization tools.  This implementation reads from
     * the Config information (via the Dataset object).
     */
    public float[] getColorScaleRange()
    {
        return this.getVariable().getColorScaleRange();
    }
    
    /**
     * @return the index of the TimestepInfo object corresponding with the given
     * ISO8601 time string. Uses binary search for efficiency.
     * @throws InvalidDimensionValueException if there is no corresponding
     * TimestepInfo object, or if the given ISO8601 string is not valid.  
     */
    public int findTIndex(String isoDateTime) throws InvalidDimensionValueException
    {
        if (isoDateTime.equals("current"))
        {
            // Return the last timestep
            // TODO: should be the index of the timestep closest to now
            return this.getLastTIndex();
        }
        DateTime target = WmsUtils.iso8601ToDateTime(isoDateTime);
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
     * Gets a List of integers representing indices along the time axis
     * starting from isoDateTimeStart and ending at isoDateTimeEnd, inclusive.
     * @param isoDateTimeStart ISO8601-formatted String representing the start time
     * @param isoDateTimeEnd ISO8601-formatted String representing the start time
     * @return List of Integer indices
     * @throws InvalidDimensionValueException if either of the start or end
     * values were not found in the axis, or if they are not valid ISO8601 times.
     */
    public List<Integer> findTIndices(String isoDateTimeStart,
        String isoDateTimeEnd) throws InvalidDimensionValueException
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
     * Finds the index of a certain z value by brute-force search.  We can afford
     * to be inefficient here because z axes are not likely to be large.
     * @param targetVal Value to search for
     * @return the z index corresponding with the given targetVal
     * @throws InvalidDimensionValueException if targetVal could not be found
     * within zValues
     */
    public int findZIndex(String targetVal) throws InvalidDimensionValueException
    {
        try
        {
            double zVal = Double.parseDouble(targetVal);
            for (int i = 0; i < this.zValues.size(); i++)
            {
                // The fuzzy comparison fails for zVal == 0.0 so we do a direct
                // comparison too
                if (this.zValues.get(i) == zVal || Math.abs((this.zValues.get(i) - zVal) / zVal) < 1e-5)
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
     * @return List of styles that this layer can be rendered in.
     */
    public List<Style> getSupportedStyles()
    {
        return this.supportedStyles;
    }
    
    /**
     * @return the key of the default style for this Variable.  Exactly 
     * equivalent to getSupportedStyles().get(0)
     */
    public Style getDefaultStyle()
    {
        return this.supportedStyles.get(0);
    }
    
    /**
     * @return true if this Layer can be rendered in the style with the
     * given name, false otherwise.
     */
    public boolean supportsStyle(String styleName)
    {
        text to ensure revisit
        return this.supportedStyles.contains(styleName.trim());
    }
    
    /**
     * @return true if this variable has a depth/elevation axis
     */
    public boolean isZaxisPresent()
    {
        return this.zValues != null && this.zValues.size() > 0;
    }
    
    /**
     * @return true if this variable has a time axis
     */
    public boolean isTaxisPresent()
    {
        return this.getTimesteps() != null && this.getTimesteps().size() > 0;
    }
    
    /**
     * @return the index of the default value on the z axis (i.e. the index of
     * the z value that will be used if the user does not specify an explicit
     * z value in a GetMap request).
     */
    public int getDefaultZIndex()
    {
        return 0;
    }
    
    /**
     * @return the default value of the z axis (i.e. the z value that will be
     * used if the user does not specify an explicit z value in a GetMap request).
     */
    public final double getDefaultZValue()
    {
        return this.zValues.get(this.getDefaultZIndex());
    }
    
    /**
     * @return the last index on the t axis
     */
    public int getLastTIndex()
    {
        return this.timesteps.size() - 1;
    }
    
    /**
     * @return the index of the default value of the t axis (i.e. the t value that will be
     * used if the user does not specify an explicit t value in a GetMap request),
     * as a TimestepInfo object.  This currently returns the last value along
     * the time axis, but should probably return the value closest to now.
     */
    public final int getDefaultTIndex()
    {
        return this.getLastTIndex();
    }
    
    /**
     * @return the default value of the t axis (i.e. the t value that will be
     * used if the user does not specify an explicit t value in a GetMap request),
     * in milliseconds since the epoch.  This currently returns the last value along
     * the time axis, but should probably return the value closest to now.
     */
    public final DateTime getDefaultTValue()
    {
        return this.getTimeValues().get(this.getLastTIndex());
    }
    
    /**
     * @return a unique identifier string for this LayerImpl object (used
     * in the display of Layers in a Capabilities document).
     */
    @Override
    public String getName()
    {
        return WmsUtils.createUniqueLayerName(this.dataset.getId(), this.id);
    }
    
    /**
     * @return true if this variable can be queried through the GetFeatureInfo
     * function.  Delegates to Dataset.isQueryable().
     */
    @Override
    public boolean isQueryable()
    {
        return this.dataset.isQueryable();
    }

    /**
     * Gets the copyright statement for this layer, replacing ${year} as 
     * appropriate with the year range that this layer covers.
     * @return The copyright statement, or the empty string if no copyright
     * statement has been set.
     */
    public String getCopyrightStatement()
    {
        String copyright = this.dataset.getCopyrightStatement();
        if (copyright == null || copyright.trim().equals(""))
        {
            return "";
        }
        else if (!this.isTaxisPresent())
        {
            return copyright;
        }
        // We (might) need to use the year range of the layer to generate
        // the final copyright statement.
        int startYear = this.timesteps.get(0).getDateTime().getYear();
        int endYear = this.timesteps.get(this.timesteps.size() - 1).getDateTime().getYear();
        String yearStr = startYear == endYear
            ? "" + startYear
            : startYear + "-" + endYear;
        // Don't forget to escape dollar signs and backslashes in the regexp
        return copyright.replaceAll("\\$\\{year\\}", yearStr);
    }

    public String getMoreInfo() {
        return this.dataset.getMoreInfoUrl();
    }

    /**
     * Get the name of the default palette that should be used to render this
     * layer.
     * @return
     */
    public String getDefaultPaletteName()
    {
        return this.getVariable().getPaletteName();
    }

    /**
     * Return true if we are to use logarithmic colour scaling by default for
     * this layer.
     * @return
     */
    public boolean isLogScaling()
    {
        return this.getVariable().isLogScaling();
    }

    /**
     * Gets the {@link Variable} object that is associated with this Layer.
     * The Variable object allows the sysadmin to override certain properties.
     */
    private Variable getVariable()
    {
        return this.dataset.getVariables().get(this.id);
    }

    public HorizontalCoordSys getHorizontalCoordSys()
    {
        return this.horizCoordSys;
    }

    public void setHorizontalCoordSys(HorizontalCoordSys horizCoordSys)
    {
        this.horizCoordSys = horizCoordSys;
    }
}
