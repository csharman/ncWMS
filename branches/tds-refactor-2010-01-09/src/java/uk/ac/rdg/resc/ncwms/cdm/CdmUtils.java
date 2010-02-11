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


package uk.ac.rdg.resc.ncwms.cdm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.geotoolkit.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.joda.time.DateTime;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.Attribute;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDataset.Gridset;
import ucar.nc2.dt.GridDatatype;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import uk.ac.rdg.resc.ncwms.coords.HorizontalCoordSys;
import uk.ac.rdg.resc.ncwms.coords.HorizontalPosition;
import uk.ac.rdg.resc.ncwms.coords.PointList;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import uk.ac.rdg.resc.ncwms.wms.ScalarLayer;

/**
 * Contains static helper methods for reading data and metadata from NetCDF files,
 * OPeNDAP servers and other data sources using the Unidata Common Data Model.
 * @author Jon
 */
public final class CdmUtils
{
    private static final Logger logger = LoggerFactory.getLogger(CdmUtils.class);

    /** Enforce non-instantiability */
    private CdmUtils() { throw new AssertionError(); }

    /**
     * Searches through the given GridDatasets for GridDatatypes, which are
     * returned as {@link ScalarLayer}s in the passed-in Map.  If this method
     * encounters a GridDatatype that is already represented in the Map of layers,
     * this method only updates the list of the layer's timesteps (through
     * {@link LayerBuilder#setTimeValues(uk.ac.rdg.resc.ncwms.wms.Layer, java.util.List)}).
     * (In this way, time-aggregated layers can be created without creating
     * multiple unnecessary objects.)
     * If the GridDatatype is not represented in the Map of layers, this method
     * creates a new Layer using {@link LayerBuilder#newLayer(java.lang.String)}
     * and populates all its fields using LayerBuilder's various setter methods.
     * @param <L> The type of {@link ScalarLayer} that can be handled by the
     * {@code layerBuilder}, and that will be returned in the Map.
     * @param gd The GridDataset to search
     * @param layerBuilder The {@link LayerBuilder} that creates ScalarLayers
     * of the given type and updates their properties.
     * @param layers Map of {@link Layer#getId() layer id}s to ScalarLayer objects,
     * which may be empty but cannot be null.
     * @throws NullPointerException if {@code layers == null}
     */
    public static <L extends ScalarLayer> void findAndUpdateLayers(GridDataset gd,
            LayerBuilder<L> layerBuilder, Map<String, L> layers)
    {
        if (layers == null) throw new NullPointerException();
        // Search through all coordinate systems, creating appropriate metadata
        // for each.  This allows metadata objects to be shared among Layer objects,
        // saving memory.
        for (Gridset gridset : gd.getGridsets())
        {
            GridCoordSystem coordSys = gridset.getGeoCoordSystem();

            // Look for new variables in this coordinate system.
            List<GridDatatype> grids = gridset.getGrids();
            List<GridDatatype> newGrids = new ArrayList<GridDatatype>();
            for (GridDatatype grid : grids)
            {
                if (layers.containsKey(grid.getName()))
                {
                    logger.debug("We already have data for {}", grid.getName());
                }
                else
                {
                    // We haven't seen this variable before so we must create
                    // a Layer object later
                    logger.debug("{} is a new grid", grid.getName());
                    newGrids.add(grid);
                }
            }

            // We only create all the coordsys-related objects if we have
            // new Layers to create
            if (!newGrids.isEmpty())
            {
                logger.debug("Creating coordinate system objects");
                // Create an object that will map lat-lon points to nearest grid points
                HorizontalCoordSys horizCoordSys = HorizontalCoordSys.fromCoordSys(coordSys);

                boolean zPositive = coordSys.isZPositive();
                CoordinateAxis1D zAxis = coordSys.getVerticalAxis();
                List<Double> zValues = getZValues(zAxis, zPositive);

                // Get the bounding box
                GeographicBoundingBox bbox = getBbox(coordSys);

                // Now add every variable that has this coordinate system
                for (GridDatatype grid : newGrids)
                {
                    logger.debug("Creating new Layer object for {}", grid.getName());
                    L layer = layerBuilder.newLayer(grid.getName());
                    layerBuilder.setTitle(layer, getLayerTitle(grid.getVariable()));
                    layerBuilder.setAbstract(layer, grid.getDescription());
                    layerBuilder.setUnits(layer, grid.getUnitsString());
                    layerBuilder.setHorizontalCoordSys(layer, horizCoordSys);
                    layerBuilder.setGeographicBoundingBox(layer, bbox);

                    if (zAxis != null)
                    {
                        layerBuilder.setElevationAxis(layer, zValues, zPositive, zAxis.getUnitsString());
                    }

                    // Add this layer to the Map
                    layers.put(layer.getId(), layer);
                }
            }

            // Now we add the new timestep information for *all* grids
            // in this Gridset
            List<DateTime> timesteps = getTimesteps(coordSys);
            for (GridDatatype grid : grids)
            {
                L layer = layers.get(grid.getName());
                layerBuilder.setTimeValues(layer, timesteps);
            }
        }
    }

    /**
     * Gets the latitude-longitude bounding box of the given coordinate system
     * in the form [minLon, minLat, maxLon, maxLat]
     */
    private static GeographicBoundingBox getBbox(GridCoordSystem coordSys)
    {
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
        // Sometimes the bounding boxes can be NaN, e.g. for a VerticalPerspectiveView
        // that encompasses more than the Earth's disc
        minLon = Double.isNaN(minLon) ? -180.0 : minLon;
        minLat = Double.isNaN(minLat) ? -90.0  : minLat;
        maxLon = Double.isNaN(maxLon) ?  180.0 : maxLon;
        maxLat = Double.isNaN(maxLat) ?   90.0 : maxLat;
        return new DefaultGeographicBoundingBox(minLon, maxLon, minLat, maxLat);
    }

    /**
     * @return the value of the standard_name attribute of the variable,
     * or the long_name if it does not exist, or the unique id if neither of
     * these attributes exist.
     */
    private static String getLayerTitle(VariableEnhanced var)
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

    /**
     * @return the values on the z axis, with sign reversed if zPositive == false.
     * Returns an empty list if zAxis is null.
     */
    private static List<Double> getZValues(CoordinateAxis1D zAxis, boolean zPositive)
    {
        List<Double> zValues = new ArrayList<Double>();
        if (zAxis != null)
        {
            for (double zVal : zAxis.getCoordValues())
            {
                zValues.add(zPositive ? zVal : 0.0 - zVal);
            }
        }
        return zValues;
    }

    /**
     * Gets List of DateTimes representing the timesteps of the given coordinate system.
     * @param coordSys The coordinate system containing the time information
     * @return List of TimestepInfo objects, or an empty list if the coordinate
     * system has no time axis
     */
    private static List<DateTime> getTimesteps(GridCoordSystem coordSys)
    {
        List<DateTime> timesteps = new ArrayList<DateTime>();
        if (coordSys.hasTimeAxis1D())
        {
            for (Date date : coordSys.getTimeAxis1D().getTimeDates())
            {
                timesteps.add(new DateTime(date));
            }
        }
        return timesteps;
    }

    public static List<Float> readPointList(GridDatatype grid, int tIndex, int zIndex,
            PointList pointList) throws IOException
    {
        return null;
    }

    public static List<Float> readTimeseries(GridDatatype grid,
            List<Integer> tIndices, int zIndex, HorizontalPosition xy)
            throws IOException
    {
        return null;
    }

}
