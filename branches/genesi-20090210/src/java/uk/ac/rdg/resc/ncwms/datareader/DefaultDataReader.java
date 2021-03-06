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

package uk.ac.rdg.resc.ncwms.datareader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDataset.Gridset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.TypedDatasetFactory;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import uk.ac.rdg.resc.ncwms.metadata.CoordAxis;
import uk.ac.rdg.resc.ncwms.metadata.Layer;
import uk.ac.rdg.resc.ncwms.metadata.LayerImpl;
import uk.ac.rdg.resc.ncwms.metadata.TimestepInfo;
import uk.ac.rdg.resc.ncwms.metadata.projection.HorizontalProjection;

/**
 * Default data reading class for CF-compliant NetCDF datasets.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class DefaultDataReader extends DataReader
{
    private static final Logger logger = LoggerFactory.getLogger(DefaultDataReader.class);
    // We'll use this logger to output performance information
    private static final Logger benchmarkLogger = LoggerFactory.getLogger("ncwms.benchmark");
    
    /**
     * <p>Reads an array of data from a NetCDF file and projects onto the given
     * {@link HorizontalGrid}.  Reads data for a single timestep and elevation only.
     * This method knows
     * nothing about aggregation: it simply reads data from the given file. 
     * Missing values (e.g. land pixels in marine data) will be represented
     * by Float.NaN.</p>
     *
     * <p>The actual reading of data is performed in {@link #populatePixelArray
     * populatePixelArray()}</p>
     * 
     * @param filename Location of the file, NcML aggregation or OPeNDAP URL
     * @param layer {@link Layer} object representing the variable to read
     * @param tIndex The index along the time axis (or -1 if there is no time axis)
     * @param zIndex The index along the vertical axis (or -1 if there is no vertical axis)
     * @param grid The grid onto which the data are to be read
     * @throws Exception if an error occurs
     */
    public float[] read(String filename, Layer layer, int tIndex, int zIndex,
        HorizontalGrid grid) throws Exception
    {
        NetcdfDataset nc = null;
        try
        {
            long start = System.currentTimeMillis();
            
            logger.debug("filename = {}, tIndex = {}, zIndex = {}",
                new Object[]{filename, tIndex, zIndex});
            // Prevent InvalidRangeExceptions for ranges we're not going to use anyway
            if (tIndex < 0) tIndex = 0;
            if (zIndex < 0) zIndex = 0;
            Range tRange = new Range(tIndex, tIndex);
            Range zRange = new Range(zIndex, zIndex);
            
            // Create an array to hold the data
            float[] picData = new float[grid.getSize()];
            // Use NaNs to represent missing data
            Arrays.fill(picData, Float.NaN);
            
            PixelMap pixelMap = new PixelMap(layer, grid);
            if (pixelMap.isEmpty()) return picData;
            
            long readMetadata = System.currentTimeMillis();
            logger.debug("Read metadata in {} milliseconds", (readMetadata - start));
            
            // Get the dataset, from the cache if possible
            nc = NetcdfDataset.acquireDataset(
                null, // Use the default factory
                filename,
                // Read the coordinate systems but don't automatically process
                // scale/missing.offset when reading data, for efficiency reasons
                EnumSet.of(Enhance.ScaleMissingDefer, Enhance.CoordSystems),
                -1, // use default buffer size
                null, // no CancelTask
                null // no iospMessage
            );
            long openedDS = System.currentTimeMillis();
            logger.debug("Opened NetcdfDataset in {} milliseconds", (openedDS - readMetadata));
            // Get a GridDataset object, since we know this is a grid
            GridDataset gd = (GridDataset)TypedDatasetFactory.open(FeatureType.GRID, nc, null, null);
            
            logger.debug("Getting GeoGrid with id {}", layer.getId());
            GridDatatype gridData = gd.findGridDatatype(layer.getId());
            logger.debug("filename = {}, gg = {}", filename, gridData.toString());
            
            // Read the data from the dataset
            long before = System.currentTimeMillis();
            // Get an enhanced variable for doing the conversion of missing
            // values
            VariableDS var = (VariableDS)nc.findVariable(layer.getId());
            // The actual reading of data from the variable is done here
            this.populatePixelArray(picData, tRange, zRange, pixelMap, gridData, var);
            long after = System.currentTimeMillis();
            // Write to the benchmark logger (if enabled in log4j.properties)
            // Headings are written in NcwmsContext.init()
            if (pixelMap.getNumUniqueIJPairs() > 1)
            {
                // Don't log single-pixel (GetFeatureInfo) requests
                benchmarkLogger.info
                (
                    layer.getDataset().getId() + "," +
                    layer.getId() + "," +
                    this.getClass().getSimpleName() + "," +
                    grid.getSize() + "," +
                    pixelMap.getNumUniqueIJPairs() + "," +
                    pixelMap.getSumRowLengths() + "," +
                    pixelMap.getBoundingBoxSize() + "," +
                    (after - before)
                );
            }
            
            long builtPic = System.currentTimeMillis();
            logger.debug("Built picture array in {} milliseconds", (builtPic - readMetadata));
            logger.debug("Whole read() operation took {} milliseconds", (builtPic - start));
            
            return picData;
        }
        finally
        {
            if (nc != null)
            {
                try
                {
                    nc.close();
                }
                catch (IOException ex)
                {
                    logger.error("IOException closing " + nc.getLocation(), ex);
                }
            }
        }
    }
    
    /**
     * Reads data from the given GridDatatype and populates the given pixel array.
     * This uses a scanline-based algorithm: subclasses can override this to
     * use alternative strategies, e.g. point-by-point or bounding box.
     * @see PixelMap
     */
    protected void populatePixelArray(float[] picData, Range tRange, Range zRange,
        PixelMap pixelMap, GridDatatype grid, VariableDS var) throws Exception
    {
        // Cycle through the y indices, extracting a scanline of
        // data each time from minX to maxX
        logger.debug("Shape of grid: {}", Arrays.toString(grid.getShape()));
        for (int j : pixelMap.getJIndices())
        {
            Range yRange = new Range(j, j);
            // Read a row of data from the source
            int imin = pixelMap.getMinIIndexInRow(j);
            int imax = pixelMap.getMaxIIndexInRow(j);
            Range xRange = new Range(imin, imax);
            // Read a chunk of data - values will not be unpacked or
            // checked for missing values yet
            logger.debug("tRange: {}, zRange: {}, yRange: {}, xRange: {}", new
                Object[] {tRange, zRange, yRange, xRange});
            GridDatatype subset = grid.makeSubset(null, null, tRange, zRange, yRange, xRange);
            logger.debug("Subset shape = {}", Arrays.toString(subset.getShape()));
            // Read all of the x-y data in this subset
            Array xySlice = subset.readDataSlice(0, 0, -1, -1);
            logger.debug("Slice shape = {}", Arrays.toString(xySlice.getShape()));
            // We now have a 2D array in y,x order.  We don't reduce this array
            // because it will go to zero size if there is only one point in
            // each direction.
            Index index = xySlice.getIndex();
            
            // Now copy the scanline's data to the picture array
            for (int i : pixelMap.getIIndices(j))
            {
                float val = xySlice.getFloat(index.set(0, i - imin));
                // We unpack and check for missing values just for
                // the points we need to display.
                val = (float)var.convertScaleOffsetMissing(val);
                // Now we set the value of all the image pixels associated with
                // this data point.
                for (int p : pixelMap.getPixelIndices(i, j))
                {
                    picData[p] = val;
                }
            }
        }
    }
    
    /**
     * Reads the metadata for all the variables in the dataset
     * at the given location, which is the location of a NetCDF file, NcML
     * aggregation, or OPeNDAP location (i.e. one element resulting from the
     * expansion of a glob aggregation).
     * @param location Full path to the dataset. This will be passed to 
     * {@link NetcdfDataset#openDataset}.
     * @param layers Map of Layer Ids to LayerImpl objects to populate or update
     * @throws IOException if there was an error reading from the data source
     */
    protected void findAndUpdateLayers(String location, Map<String, LayerImpl> layers)
        throws IOException
    {
        logger.debug("Finding layers in {}", location);
        
        NetcdfDataset nc = null;
        try
        {
            // Open the dataset.  We use the dataset cache here for the following
            // reason: if the cache has swallowed all of our available file handles,
            // we can't bypass the cache as there will be no file handles left.
            // The metadata might be a little out of date, depending on how long
            // the cache clear-out time is set to in MetadataLoader.init()
            // (should be around 5 minutes).
            nc = NetcdfDataset.acquireDataset(location, null);
            GridDataset gd = (GridDataset)TypedDatasetFactory.open(FeatureType.GRID,
                nc, null, null);
            
            // Search through all coordinate systems, creating appropriate metadata
            // for each.  This allows metadata objects to be shared among Layer objects,
            // saving memory.
            for (Gridset gridset : gd.getGridsets())
            {
                GridCoordSystem coordSys = gridset.getGeoCoordSystem();
                
                // Compute TimestepInfo objects for this file
                List<TimestepInfo> timesteps = getTimesteps(location, coordSys);
                
                // Look for new variables in this coordinate system.
                List<GridDatatype> grids = gridset.getGrids();
                List<GridDatatype> newGrids = new ArrayList<GridDatatype>();
                for (GridDatatype grid : grids)
                {
                    if (!layers.containsKey(grid.getName()) &&
                        this.includeGrid(grid))
                    {
                        // We haven't seen this variable before so we must create
                        // a Layer object later
                        newGrids.add(grid);
                    }
                }
                
                // We only create all the coordsys-related objects if we have
                // new Layers to create
                if (newGrids.size() > 0)
                {
                    CoordAxis xAxis = this.getXAxis(coordSys);
                    CoordAxis yAxis = this.getYAxis(coordSys);
                    HorizontalProjection proj = HorizontalProjection.create(coordSys.getProjection());
                    
                    boolean zPositive = this.isZPositive(coordSys);
                    CoordinateAxis1D zAxis = coordSys.getVerticalAxis();
                    double[] zValues = this.getZValues(zAxis, zPositive);

                    // Set the bounding box
                    // TODO: should take into account the cell bounds
                    LatLonRect latLonRect = coordSys.getLatLonBoundingBox();
                    LatLonPoint lowerLeft = latLonRect.getLowerLeftPoint();
                    LatLonPoint upperRight = latLonRect.getUpperRightPoint();
                    double minLon = lowerLeft.getLongitude();
                    double maxLon = upperRight.getLongitude();
                    double minLat = lowerLeft.getLatitude();
                    double maxLat = upperRight.getLatitude();
                    // Correct the bounding box in case of mistakes or in case it 
                    // crosses the date line
                    if (latLonRect.crossDateline() || minLon >= maxLon)
                    {
                        minLon = -180.0;
                        maxLon = 180.0;
                    }
                    if (minLat >= maxLat)
                    {
                        minLat = -90.0;
                        maxLat = 90.0;
                    }
                    double[] bbox = new double[]{minLon, minLat, maxLon, maxLat};

                    // Now add every variable that has this coordinate system
                    for (GridDatatype grid : newGrids)
                    {
                        logger.debug("Creating new Layer object for {}", grid.getName());
                        LayerImpl layer = new LayerImpl();
                        layer.setId(grid.getName());
                        layer.setTitle(getLayerTitle(grid.getVariable()));
                        layer.setAbstract(grid.getDescription());
                        layer.setUnits(grid.getUnitsString());
                        layer.setXaxis(xAxis);
                        layer.setYaxis(yAxis);
                        layer.setHorizontalProjection(proj);
                        layer.setBbox(bbox);

                        if (zAxis != null)
                        {
                            layer.setZunits(zAxis.getUnitsString());
                            layer.setZpositive(zPositive);
                            layer.setZvalues(zValues);
                        }

                        // Add this layer to the Map
                        layers.put(layer.getId(), layer);
                    }
                }
                // Now we add the new timestep information for all grids
                // in this Gridset
                for (GridDatatype grid : grids)
                {
                    if (this.includeGrid(grid))
                    {
                        LayerImpl layer = layers.get(grid.getName());
                        for (TimestepInfo timestep : timesteps)
                        {
                            layer.addTimestepInfo(timestep);
                        }
                    }
                }
            }
        }
        finally
        {
            logger.debug("In finally clause");
            if (nc != null)
            {
                try
                {
                    logger.debug("Closing NetCDF file");
                    nc.close();
                    logger.debug("NetCDF file closed");
                }
                catch (IOException ex)
                {
                    logger.error("IOException closing " + nc.getLocation(), ex);
                }
            }
        }
    }
    
    /**
     * @return true if the given variable is to be exposed through the WMS.
     * This default implementation always returns true, but allows subclasses
     * to filter out variables if required.
     */
    protected boolean includeGrid(GridDatatype grid)
    {
        return true;
    }
    
    /**
     * Gets the X axis from the given coordinate system
     */
    protected CoordAxis getXAxis(GridCoordSystem coordSys) throws IOException
    {
        return CoordAxis.create(coordSys.getXHorizAxis());
    }
    
    /**
     * Gets the Y axis from the given coordinate system
     */
    protected CoordAxis getYAxis(GridCoordSystem coordSys) throws IOException
    {
        return CoordAxis.create(coordSys.getYHorizAxis());
    }
    
    /**
     * @return the values on the z axis, with sign reversed if zPositive == false.
     * Returns null if zAxis is null.
     */
    protected double[] getZValues(CoordinateAxis1D zAxis, boolean zPositive)
    {
        double[] zValues = null;
        if (zAxis != null)
        {
            zValues = zAxis.getCoordValues();
            if (!zPositive)
            {
                double[] zVals = new double[zValues.length];
                for (int i = 0; i < zVals.length; i++)
                {
                    zVals[i] = 0.0 - zValues[i];
                }
                zValues = zVals;
            }
        }
        return zValues;
    }
    
    /**
     * @return true if the zAxis is positive
     */
    protected boolean isZPositive(GridCoordSystem coordSys)
    {
        return coordSys.isZPositive();
    }
    
    /**
     * Gets array of Dates representing the timesteps of the given coordinate system.
     * @param filename The name of the file/dataset to which the coord sys belongs
     * @param coordSys The coordinate system containing the time information
     * @return List of TimestepInfo objects
     * @throws IOException if there was an error reading the timesteps data
     */
    protected static List<TimestepInfo> getTimesteps(String filename, GridCoordSystem coordSys)
        throws IOException
    {
        List<TimestepInfo> timesteps = new ArrayList<TimestepInfo>();
        if (coordSys.hasTimeAxis1D())
        {
            Date[] dates = coordSys.getTimeAxis1D().getTimeDates();
            for (int i = 0; i < dates.length; i++)
            {
                TimestepInfo tInfo = new TimestepInfo(dates[i], filename, i);
                timesteps.add(tInfo);
            }
        }
        return timesteps;
    }
    
    /**
     * @return the value of the standard_name attribute of the variable,
     * or the long_name if it does not exist, or the unique id if neither of
     * these attributes exist.
     */
    protected static String getLayerTitle(VariableEnhanced var)
    {
        Attribute stdNameAtt = var.findAttributeIgnoreCase("standard_name");
        if (stdNameAtt == null || stdNameAtt.getStringValue().trim().equals(""))
        {
            Attribute longNameAtt = var.findAttributeIgnoreCase("long_name");
            if (longNameAtt == null || longNameAtt.getStringValue().trim().equals(""))
            {
                return var.getName();
            }
            else
            {
                return longNameAtt.getStringValue();
            }
        }
        else
        {
            return stdNameAtt.getStringValue();
        }
    }
    
}
