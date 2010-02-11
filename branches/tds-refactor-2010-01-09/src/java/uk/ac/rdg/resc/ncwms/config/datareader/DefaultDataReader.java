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

package uk.ac.rdg.resc.ncwms.config.datareader;

import uk.ac.rdg.resc.ncwms.coords.PointList;
import uk.ac.rdg.resc.ncwms.coords.PixelMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joda.time.DateTime;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.TypedDatasetFactory;
import uk.ac.rdg.resc.ncwms.cdm.AbstractScalarLayerBuilder;
import uk.ac.rdg.resc.ncwms.cdm.CdmUtils;
import uk.ac.rdg.resc.ncwms.config.LayerImpl;
import uk.ac.rdg.resc.ncwms.coords.CrsHelper;
import uk.ac.rdg.resc.ncwms.coords.HorizontalPosition;
import uk.ac.rdg.resc.ncwms.coords.LonLatPosition;
import uk.ac.rdg.resc.ncwms.cdm.DataReadingStrategy;
import uk.ac.rdg.resc.ncwms.coords.HorizontalGrid;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.Layer;

/**
 * Default data reading class for CF-compliant NetCDF datasets.
 *
 * @author Jon Blower
 */
public class DefaultDataReader extends DataReader
{
    private static final Logger logger = LoggerFactory.getLogger(DefaultDataReader.class);
    // We'll use this logger to output performance information
    private static final Logger benchmarkLogger = LoggerFactory.getLogger("ncwms.benchmark");

    /**
     * Enumeration of enhancements we want to perform when opening NetcdfDatasets
     * Read the coordinate systems but don't automatically process
     * scale/missing/offset when reading data, for efficiency reasons.
     */
    private static final Set<Enhance> DATASET_ENHANCEMENTS =
        EnumSet.of(Enhance.ScaleMissingDefer, Enhance.CoordSystems);

    /**
     * Reads data from a NetCDF file.  Reads data for a single timestep only.
     * This method knows
     * nothing about aggregation: it simply reads data from the given file.
     * Missing values (e.g. land pixels in oceanography data) will be represented
     * by null.
     *
     * <p>The actual reading of data is performed in {@link #populatePixelArray
     * populatePixelArray()}</p>
     *
     * @param filename Location of the file, NcML aggregation or OPeNDAP URL
     * @param layer {@link Layer} object representing the variable
     * @param tIndex The index along the time axis (or -1 if there is no time axis)
     * @param zIndex The index along the vertical axis (or -1 if there is no vertical axis)
     * @param pointList The list of real-world x-y points for which we need data.
     * In the case of a GetMap operation this will usually be a {@link HorizontalGrid}.
     * @return an array of floating-point data values, one for each point in
     * the {@code pointList}, in the same order.
     * @throws IOException if an input/output exception occurred when reading data
     */
    @Override
    public List<Float> read(String filename, Layer layer, int tIndex, int zIndex,
        PointList pointList) throws IOException
    {
        NetcdfDataset nc = null;
        try
        {
            long start = System.currentTimeMillis();
            
            // Open the dataset, using the cache for NcML aggregations
            nc = openDataset(filename);
            long openedDS = System.currentTimeMillis();
            logger.debug("Opened NetcdfDataset in {} milliseconds", (openedDS - start));

            // Get a GridDataset object, since we know this is a grid
            GridDataset gd = (GridDataset)TypedDatasetFactory.open(FeatureType.GRID, nc, null, null);
            
            logger.debug("Getting GridDatatype with id {}", layer.getId());
            GridDatatype gridData = gd.findGridDatatype(layer.getId());
            logger.debug("filename = {}, gg = {}", filename, gridData.toString());

            // Decide on which strategy to use for reading data from the source
            // If data are local and uncompressed then it's relatively cheap to
            // make many small reads of data to save memory.  If data are remote
            // or compressed, it's generally more efficient to read data in a
            // single operation, even if the memory footprint is larger.
            String fileType = nc.getFileTypeId();
            DataReadingStrategy drStrategy = fileType.equals("netCDF") || fileType.equals("HDF4")
                ? DataReadingStrategy.SCANLINE
                : DataReadingStrategy.BOUNDING_BOX;

            return CdmUtils.readPointList(
                gridData,           // The grid of data to read from
                layer.getHorizontalCoordSys(),
                tIndex,
                zIndex,
                pointList,
                drStrategy
            );
            
            // Read the data from the dataset
            long before = System.currentTimeMillis();
            // Decide on which strategy to use for reading data from the source
            String fileType = nc.getFileTypeId();
            // If data are local and uncompressed then it's relatively cheap to
            // make many small reads of data to save memory.  If data are remote
            // or compressed, it's generally more efficient to read data in a
            // single operation, even if the memory footprint is larger.
            DataReadingStrategy drStrategy = fileType.equals("netCDF") || fileType.equals("HDF4")
                ? DataReadingStrategy.SCANLINE
                : DataReadingStrategy.BOUNDING_BOX;
            drStrategy.populatePixelArray(picData, tRange, zRange, pixelMap, gridData);
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
                    pointList.size() + "," +
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
     * <p>Reads a timeseries of data from a file from a single xyz point.  This
     * method knows nothing about aggregation: it simply reads data from the
     * given file.  Missing values (e.g. land pixels in oceanography data) will
     * be represented by null.</p>
     * <p>If the provided Layer doesn't have a time axis then {@code tIndices}
     * must be a single-element list with value -1.  In this case the returned
     * "timeseries" of data will be a single data value. (TODO: make this more
     * sensible.)</p>
     * <p>This implementation reads all data with a single I/O operation
     * (as opposed to the {@link DataReader#readTimeseries(java.lang.String,
     * uk.ac.rdg.resc.ncwms.metadata.Layer, java.util.List, int,
     * uk.ac.rdg.resc.ncwms.coordsys.LonLatPosition) superclass implementation},
     * which uses an I/O operation for each individual point).  This method is
     * therefore expected to be more efficient, particularly when reading from
     * OPeNDAP servers.</p>
     * @param filename Location of the file, NcML aggregation or OPeNDAP URL
     * @param layer {@link Layer} object representing the variable
     * @param tIndices the indices along the time axis within this file
     * @param zIndex The index along the vertical axis (or -1 if there is no vertical axis)
     * @param xy the horizontal position of the point
     * @return an array of floating-point data values, one for each point in
     * {@code tIndices}, in the same order.
     * @throws IOException if an input/output exception occurred when reading data
     * @todo Validity checking on tIndices and layer.hasTAxis()?
     */
    @Override
    public List<Float> readTimeseries(String filename, Layer layer,
        List<Integer> tIndices, int zIndex, HorizontalPosition xy)
        throws IOException
    {
        LonLatPosition lonLat;
        if (xy instanceof LonLatPosition)
        {
            lonLat = (LonLatPosition)xy;
        }
        else if (xy.getCoordinateReferenceSystem() == null)
        {
            throw new IllegalArgumentException("Horizontal position must have a"
                + " coordinate reference system");
        }
        else
        {
            CrsHelper crsHelper = CrsHelper.fromCrs(xy.getCoordinateReferenceSystem());
            try
            {
                lonLat = crsHelper.crsToLonLat(xy);
            }
            catch(TransformException te)
            {
                // This would only happen if there were an internal error transforming
                // between coordinate systems in making the PixelMap.  There is
                // nothing a client could do to recover from this so we turn it into
                // a runtime exception
                // TODO: think of a better exception type
                throw new RuntimeException(te);
            }
        }
        int[] gridCoords = layer.getHorizontalCoordSys().lonLatToGrid(lonLat);

        int firstTIndex = tIndices.get(0);
        int lastTIndex = tIndices.get(tIndices.size() - 1);
        // Prevent InvalidRangeExceptions if z or t axes are missing
        if (firstTIndex < 0 || lastTIndex < 0)
        {
            firstTIndex = 0;
            lastTIndex = 0;
        }
        if (zIndex < 0) zIndex = 0;

        NetcdfDataset nc = null;

        try
        {
            Range tRange = new Range(firstTIndex, lastTIndex);
            Range zRange = new Range(zIndex, zIndex);
            Range yRange = new Range(gridCoords[1], gridCoords[1]);
            Range xRange = new Range(gridCoords[0], gridCoords[0]);

            // Open the dataset, using the cache for NcML aggregations
            nc = openDataset(filename);
            GridDataset gd = (GridDataset)TypedDatasetFactory.open(FeatureType.GRID,
                nc, null, null);
            GridDatatype grid = gd.findGridDatatype(layer.getId());

            // Now read the data
            GridDatatype subset = grid.makeSubset(null, null, tRange, zRange, yRange, xRange);
            Array arr = subset.readDataSlice(-1, 0, 0, 0);

            // Check for consistency
            if (arr.getSize() != lastTIndex - firstTIndex + 1)
            {
                // This is an internal error
                throw new IllegalStateException("Unexpected array size (got " + arr.getSize()
                    + ", expected " + (lastTIndex - firstTIndex + 1) + ")");
            }

            // Copy the data (which may include many points we don't need) to
            // the required array
            VariableDS var = grid.getVariable();
            List<Float> tsData = new ArrayList<Float>();
            for (int tIndex : tIndices)
            {
                int tIndexOffset = tIndex - firstTIndex;
                if (tIndexOffset < 0) tIndexOffset = 0; // This will happen if the layer has no t axis
                float val = arr.getFloat(tIndexOffset);
                // Convert scale-offset-missing
                val = (float)var.convertScaleOffsetMissing(val);
                // Replace missing values with nulls
                tsData.add(Float.isNaN(val) ? null : val);
            }
            return tsData;
        }
        catch(InvalidRangeException ire)
        {
            // This is a programming error, and one from which we can't recover
            throw new IllegalStateException(ire);
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
     * Reads the metadata for all the variables in the dataset
     * at the given location, which is the location of a NetCDF file, NcML
     * aggregation, or OPeNDAP location (i.e. one element resulting from the
     * expansion of a glob aggregation).
     * @param location Full path to the dataset. This will be passed to 
     * {@link NetcdfDataset#openDataset}.
     * @param layers Map of Layer Ids to LayerImpl objects to populate or update
     * @throws Exception if there was an error reading from the data source
     */
    @Override
    protected void findAndUpdateLayers(String location,
        Map<String, LayerImpl> layers) throws Exception
    {
        logger.debug("Finding layers in {}", location);
        
        NetcdfDataset nc = null;
        try
        {
            // Open the dataset, using the cache for NcML aggregations
            nc = openDataset(location);
            GridDataset gd = (GridDataset)TypedDatasetFactory.open(FeatureType.GRID,
                nc, null, null);

            LayerImplBuilder layerBuilder = new LayerImplBuilder(location);
            CdmUtils.findAndUpdateLayers(gd, layerBuilder, layers);
        }
        finally
        {
            logger.debug("In finally clause");
            if (nc != null)
            {
                try
                {
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

    private static final class LayerImplBuilder extends AbstractScalarLayerBuilder<LayerImpl>
    {
        private final String location;

        public LayerImplBuilder(String location) {
            this.location = location;
        }

        @Override
        public LayerImpl newLayer(String id) {
            return new LayerImpl(id);
        }

        @Override
        public void setTimeValues(LayerImpl layer, List<DateTime> times) {
            for (int i = 0; i < times.size(); i++) {
                layer.addTimestepInfo(times.get(i), this.location, i);
            }
        }
    }

    /**
     * Opens the NetCDF dataset at the given location, using the dataset
     * cache if {@code location} represents an NcML aggregation.  We cannot
     * use the cache for OPeNDAP or single NetCDF files because the underlying
     * data may have changed and the NetcdfDataset cache may cache a dataset
     * forever.  In the case of NcML we rely on the fact that server administrators
     * ought to have set a "recheckEvery" parameter for NcML aggregations that
     * may change with time.  It is desirable to use the dataset cache for NcML
     * aggregations because they can be time-consuming to assemble and we don't
     * want to do this every time a map is drawn.
     * @param location The location of the data: a local NetCDF file, an NcML
     * aggregation file or an OPeNDAP location, {@literal i.e.} anything that can be
     * passed to NetcdfDataset.openDataset(location).
     * @return a {@link NetcdfDataset} object for accessing the data at the
     * given location.  The coordinate systems will have been read, but
     * the application of scale-offset-missing is deferred.
     * @throws IOException if there was an error reading from the data source.
     */
    private static NetcdfDataset openDataset(String location) throws IOException
    {
        if (WmsUtils.isNcmlAggregation(location))
        {
            // We use the cache of NetcdfDatasets to read NcML aggregations
            // as they can be time-consuming to put together.  If the underlying
            // data can change we rely on the server admin setting the
            // "recheckEvery" parameter in the aggregation file.
            return NetcdfDataset.acquireDataset(
                null, // Use the default factory
                location,
                DATASET_ENHANCEMENTS,
                -1, // use default buffer size
                null, // no CancelTask
                null // no iospMessage
            );
        }
        else
        {
            // For local single files and OPeNDAP datasets we don't use the
            // cache, to ensure that we are always reading the most up-to-date
            // data.  There is a small possibility that the dataset cache will
            // have swallowed up all available file handles, in which case
            // the server admin will need to increase the number of available
            // handles on the server.
            return NetcdfDataset.openDataset(
                location,
                DATASET_ENHANCEMENTS,
                -1, // use default buffer size
                null, // no CancelTask
                null // no iospMessage
            );
        }
    }
    
}
