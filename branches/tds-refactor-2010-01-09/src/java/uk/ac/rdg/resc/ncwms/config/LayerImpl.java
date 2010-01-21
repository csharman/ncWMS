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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;
import uk.ac.rdg.resc.ncwms.coordsys.CrsHelper;
import uk.ac.rdg.resc.ncwms.coordsys.HorizontalCoordSys;
import uk.ac.rdg.resc.ncwms.coordsys.HorizontalPosition;
import uk.ac.rdg.resc.ncwms.datareader.DataReader;
import uk.ac.rdg.resc.ncwms.datareader.PointList;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.styles.Style;
import uk.ac.rdg.resc.ncwms.util.Range;
import uk.ac.rdg.resc.ncwms.wms.AbstractTimeAggregatedLayer;

/**
 * A concrete Layer implementation that supports  time aggregation through the
 * {@link AbstractTimeAggregatedLayer} superclass.  Data values are represented
 * as floating-point numbers.
 *
 * @author Jon Blower
 */
public class LayerImpl extends AbstractTimeAggregatedLayer<Float>
{
    protected Dataset dataset;
    protected boolean zPositive;
    protected HorizontalCoordSys horizCoordSys;
    // Stores the keys of the styles that this variable supports
    protected List<Style> supportedStyles = new ArrayList<Style>();
    
    /**
     * Creates a new Layer using a default bounding box (covering the whole 
     * earth), with the given id and with a default boxfill style
     */
    public LayerImpl(String id)
    {
        super(id);
        this.supportedStyles.add(Style.BOXFILL);
    }

    /**
     * Gets the human-readable Title of this Layer.  If the sysadmin has set a
     * title for this layer in the config file, this title will be returned.
     * If not, the title that was read in the relevant {@link DataReader} will
     * be used.
     */
    @Override
    public String getTitle()
    {
        Variable var = this.getVariable();
        if (var != null && var.getTitle() != null) return var.getTitle();
        else return this.title;
    }

    @Override public Dataset getDataset() { return this.dataset; }
    public void setDataset(Dataset dataset) { this.dataset = dataset; }

    @Override
    public Class<Float> getDataType()
    {
        return Float.class;
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
     * Returns an approximate range of values that this layer can take.  This
     * is merely a hint, for example to suggest to clients sensible default
     * values for choosing a colour scale.
     * @return an approximate range of values that this layer can take.
     */
    @Override
    public Range<Float> getApproxValueRange()
    {
        return this.getVariable().getColorScaleRange();
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
        return this.supportedStyles.contains(styleName.trim());
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

    @Override
    public Float readSinglePoint(DateTime time, double elevation, HorizontalPosition xy)
        throws InvalidDimensionValueException, IOException
    {
        // Find and check the time and elevation values. Indices of -1 will be
        // returned if this layer does not have a time/elevation axis
        int tIndex = this.findAndCheckTimeIndex(time);
        int zIndex = this.findAndCheckElevationIndex(elevation);

        // Find which file we're reading from and the time index in the file
        String filename = this.dataset.getLocation();
        int tIndexInFile = tIndex;
        if (tIndex >= 0) {
            TimestepInfo tInfo = this.timesteps.get(tIndex);
            filename = tInfo.getFilename();
            tIndexInFile = tInfo.getIndexInFile();
        }

        PointList singlePoint = PointList.fromPoint(xy, CrsHelper.CRS_84);

        DataReader dr = DataReader.forName("todo");
        return dr.read(filename, this, tIndexInFile, zIndex, singlePoint)[0];
    }
}
