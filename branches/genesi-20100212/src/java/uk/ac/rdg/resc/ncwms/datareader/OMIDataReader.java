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
package uk.ac.rdg.resc.ncwms.datareader;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDataset.Gridset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.TypedDatasetFactory;
import uk.ac.rdg.resc.ncwms.coordsys.HorizontalPosition;
import uk.ac.rdg.resc.ncwms.coordsys.LonLatPosition;
import uk.ac.rdg.resc.ncwms.metadata.Layer;
import uk.ac.rdg.resc.ncwms.metadata.LayerImpl;
import uk.ac.rdg.resc.ncwms.metadata.TimestepInfo;

/**
 * DataReader for reading data from OMI satellite imagery in HDF5-EOS format.
 * This could be much better integrated with DefaultDataReader.
 * @author Jon
 */
public class OMIDataReader extends DataReader {

    private static final Logger logger = Logger.getLogger(OMIDataReader.class);
    private static final String GROUP_PREFIX = "HDFEOS/SWATHS/ColumnAmountO3/";

    /**
     * Reads and returns the metadata for all the variables in the dataset
     * at the given location, which is the location of a NetCDF file, NcML
     * aggregation, or OPeNDAP location (i.e. one element resulting from the
     * expansion of a glob aggregation).
     * @param location Full path to the individual file
     * @throws IOException if there was an error reading from the data source
     */
    @Override
    protected void findAndUpdateLayers(String location, Map<String, LayerImpl> layers)
            throws IOException {
        NetcdfDataset nc = null;
        try {
            // Don't use the cache when reading metadata
            nc = NetcdfDataset.openDataset(location);
            GridDataset gd = (GridDataset) TypedDatasetFactory.open(FeatureType.GRID, nc, null, null);
            for (Gridset gs : gd.getGridsets()) {
                GridCoordSystem coordSys = gs.getGeoCoordSystem();
                logger.debug("Reading coordinate system from {}", location);
                OMICoordinateSystem cs = new OMICoordinateSystem(coordSys);
                // Now look through for the layers
                for (GridDatatype gdt : gs.getGrids()) {
                    // Only read the ozone data for now.
                    if (!gdt.getName().endsWith("/ColumnAmountO3")) {
                        continue;
                    }
                    String name = gdt.getName().substring(GROUP_PREFIX.length()).replace("/", ":").replace(" ", "_");
                    logger.debug("Found layer {}", name);
                    LayerImpl layer = layers.get(name);
                    if (layer == null) {
                        layer = new LayerImpl();
                        layer.setId(name);
                        layer.setTitle(name); // TODO
                        layer.setUnits("kg m-2"); // Dobson units in data, we convert here
                        layer.setBbox(new double[]{-180.0, -90.0, 180.0, 90.0});
                        // Attachment maps file locations to coordinate systems
                        // because each file uses a different CS
                        layer.setAttachment(new HashMap<String, OMICoordinateSystem>());
                        layers.put(layer.getId(), layer);
                    }
                    // Add the coordinate system for this file
                    Map<String, OMICoordinateSystem> map =
                            (Map<String, OMICoordinateSystem>) layer.getAttachment();
                    map.put(location, cs);
                    layer.addTimestepInfo(new TimestepInfo(new DateTime(cs.getDate()), location, 0));
                }
            }

        } finally {
            if (nc != null) {
                nc.close(); // TODO swallow exception?
            }
        }
    }

    /**
     * Reads an array of data from a NetCDF file and projects onto a rectangular
     * lat-lon grid.  Reads data for a single timestep only.  This method knows
     * nothing about aggregation: it simply reads data from the given file.
     * Missing values (e.g. land pixels in oceanography data) will be represented
     * by Float.NaN.
     *
     * @param filename Full path to the individual file containing the data
     * @param layer {@link Layer} object representing the variable
     * @param tIndex The index along the time axis (or -1 if there is no time axis).
     * This is ignored in this class as there is only one timestep per file.
     * @param zIndex The index along the vertical axis (or -1 if there is no vertical axis)
     * @param grid The grid onto which the data are to be read
     * @throws Exception if an error occurs
     */
    @Override
    public float[] read(String filename, Layer layer, int tIndex, int zIndex, PointList pointList)
            throws Exception {
        logger.debug("Reading data from " + filename);

        // Find the coordinate system for this file
        Map<String, OMICoordinateSystem> map =
                (Map<String, OMICoordinateSystem>) layer.getAttachment();
        OMICoordinateSystem cs = map.get(filename);

        String varName = GROUP_PREFIX + layer.getId().replace("_", " ").replace(":", "/");

        NetcdfDataset nc = null;
        try {
            // Open the dataset without adding coordinate systems and delaying
            // enhancement
            nc = NetcdfDataset.acquireDataset(null, filename, EnumSet.of(Enhance.ScaleMissingDefer),
                    -1, null, null);
            // Find the variable
            VariableDS var = (VariableDS) nc.findVariable(varName);
            if (var == null) {
                logger.error("{} not found", varName); // TODO do more here
            }
            return readData(var, cs, pointList);
        } finally {
            if (nc != null) {
                nc.close();
            }
        }
    }

    static float[] readData(VariableDS var, OMICoordinateSystem cs, PointList pointList) throws Exception {
        logger.debug("Reading {}", var.getName());
        // Read the whole variable into memory
        Array arr = var.read();
        logger.debug("Read {} into memory", var.getName());
        Index idx = arr.getIndex();

        // Create an array to hold the data
        float[] picData = new float[pointList.size()];
        Arrays.fill(picData, Float.NaN);

        logger.debug("Populating pixel array");
        int picIndex = 0;
        int i = 0, j = 0;

        for (HorizontalPosition point : pointList.asList()) {

            LonLatPosition lonLat = pointList.getCrsHelper().crsToLonLat(point);

            if (lonLat.getLatitude() >= -90.0 && lonLat.getLatitude() <= 90.0) {
                // Find the index in the source data using the
                // coordinate system, starting at the last point found
                // TODO when we wrap around to the next y we're not
                // starting looking from the best place...
                int[] indices = cs.getIndicesDownhill(lonLat.getLongitude(), lonLat.getLatitude(), i, j);
                if (indices[0] >= 0 && indices[1] >= 0) {
                    // The closest point is close enough to a data point
                    i = indices[0];
                    j = indices[1];
                    idx.set(i, j);
                    // Get the data value from the array, applying the
                    // scale-offset-missing parameters
                    double val = var.convertScaleOffsetMissing(arr.getDouble(idx));
                    if (!var.isMissing(val)) {
                        picData[picIndex] = (float) convertToKgM2(val);
                    }
                } else {
                    // The closest point is too far from a data point
                    // but we still want to search from this place next time
                    i = -indices[0];
                    j = -indices[1];
                }
            }
            picIndex++;
        }
        logger.debug("Populated pixel array");
        return picData;
    }

    /**
     * Converts a value in Dobson units to kg/m2, needed for comparison with
     * GlobModel data.
     * @param du
     * @return
     */
    private static double convertToKgM2(double du) {
        return du * 2.1414e-5; // TODO check conversion
    }
}
