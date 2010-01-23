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
import org.joda.time.DateTime;
import uk.ac.rdg.resc.ncwms.coordsys.CrsHelper;
import uk.ac.rdg.resc.ncwms.coordsys.HorizontalCoordSys;
import uk.ac.rdg.resc.ncwms.coordsys.HorizontalPosition;
import uk.ac.rdg.resc.ncwms.datareader.DataReader;
import uk.ac.rdg.resc.ncwms.datareader.PointList;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.styles.ColorPalette;
import uk.ac.rdg.resc.ncwms.util.Range;
import uk.ac.rdg.resc.ncwms.wms.AbstractTimeAggregatedLayer;

/**
 * A concrete Layer implementation that supports  time aggregation through the
 * {@link AbstractTimeAggregatedLayer} superclass.
 *
 * @author Jon Blower
 */
public final class LayerImpl extends AbstractTimeAggregatedLayer
{
    private Dataset dataset;
    private HorizontalCoordSys horizCoordSys;
    private DataReader dataReader;
    
    /**
     * Creates a new Layer using a default bounding box (covering the whole 
     * earth), with the given id and with a default boxfill style
     */
    public LayerImpl(String id)
    {
        super(id);
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
    // Called by Dataset.loadLayers()
    public void setDataset(Dataset dataset) { this.dataset = dataset; }

    // Called by Dataset.loadLayers()
    void setDataReader(DataReader dataReader)
    {
        this.dataReader = dataReader;
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
     * @return true if this variable can be queried through the GetFeatureInfo
     * function.  Delegates to Dataset.isQueryable().
     */
    @Override
    public boolean isQueryable()
    {
        return this.dataset.isQueryable();
    }

    /**
     * Return true if we are to use logarithmic colour scaling by default for
     * this layer.
     * @return true if we are to use logarithmic colour scaling by default for
     * this layer.
     */
    @Override
    public boolean isLogScaling()
    {
        return this.getVariable().isLogScaling();
    }

    /**
     * Returns the default colour palette to be used if the client does not
     * specify one in a GetMap request
     * @return the default colour palette to be used if the client does not
     * specify one
     */
    @Override
    public ColorPalette getDefaultColorPalette()
    {
        return ColorPalette.get(this.getVariable().getPaletteName());
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
        return this.dataReader.read(filename, this, tIndexInFile, zIndex, singlePoint)[0];
    }
}
