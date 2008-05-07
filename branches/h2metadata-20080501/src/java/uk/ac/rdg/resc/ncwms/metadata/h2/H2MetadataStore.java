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

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.h2.tools.RunScript;
import ucar.nc2.dataset.AxisType;
import uk.ac.rdg.resc.ncwms.config.Dataset;
import uk.ac.rdg.resc.ncwms.datareader.DataReader;
import uk.ac.rdg.resc.ncwms.metadata.CoordAxis;
import uk.ac.rdg.resc.ncwms.metadata.Irregular1DCoordAxis;
import uk.ac.rdg.resc.ncwms.metadata.LUTCoordAxis;
import uk.ac.rdg.resc.ncwms.metadata.Layer;
import uk.ac.rdg.resc.ncwms.metadata.LayerImpl;
import uk.ac.rdg.resc.ncwms.metadata.MetadataStore;
import uk.ac.rdg.resc.ncwms.metadata.PositiveDirection;
import uk.ac.rdg.resc.ncwms.metadata.Regular1DCoordAxis;
import uk.ac.rdg.resc.ncwms.metadata.TimestepInfo;
import uk.ac.rdg.resc.ncwms.metadata.VectorLayer;
import uk.ac.rdg.resc.ncwms.metadata.projection.HorizontalProjection;
import uk.ac.rdg.resc.ncwms.utils.WmsUtils;

/**
 * Stores metadata in an H2 database
 * @todo make sure all PreparedStatements are closed
 * @author Jon
 */
public class H2MetadataStore extends MetadataStore
{
    private static final Logger logger = Logger.getLogger(H2MetadataStore.class);
    
    private Connection conn;
    
    /**
     * Called by Spring to initialize the database
     * @throws Exception if the database could not be initialized
     */
    @Override
    public void init() throws Exception
    {
        File metadataDir = new File(this.ncwmsContext.getWorkingDirectory(), "metadataDB");
        // This will create the directory if it doesn't exist, throwing an
        // Exception if there was an error
        WmsUtils.createDirectory(metadataDir);
        String databasePath = new File(metadataDir, "metadataDB").getCanonicalPath();
        
        // Load the SQL script file that initializes the database.
        // This script file does nothing if the database is already populated
        InputStream scriptIn =
            Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("/uk/ac/rdg/resc/ncwms/metadata/h2/init.sql");
        if (scriptIn == null)
        {
            throw new Exception("Can't find initialization script init.sql");
        }
        Reader scriptReader = new InputStreamReader(scriptIn);
        
        try
        {
            // Load the database driver
            Class.forName("org.h2.Driver");
            // Get a connection to the database.  Appending "LOCK_MODE=3" means
            // "read committed", i.e. only committed data will be read
            this.conn = DriverManager.getConnection("jdbc:h2:" + databasePath +
                ";LOCK_MODE=3");
            // Set auto-commit to false to allow transactions.
            // See http://java.sun.com/docs/books/tutorial/jdbc/basics/transactions.html
            this.conn.setAutoCommit(false);

            // Now run the script to initialize the database
            RunScript.execute(this.conn, scriptReader);
        }
        catch(Exception e)
        {
            // Make sure we clean up before closing
            this.close();
            throw e;
        }
        
        logger.info("H2 metadata store initialized");
    }
    
    /**
     * Gets a Layer object from a dataset
     * @param datasetId The ID of the dataset to which the layer belongs
     * @param layerId The unique ID of the layer within the dataset
     * @return The corresponding Layer, or null if there is no corresponding
     * layer in the store.
     * @throws Exception if an error occurs reading from the persistent store
     */
    @Override
    public Layer getLayer(String datasetId, String layerId) throws Exception
    {
        logger.debug("Getting layer {} from dataset {}",
            layerId, datasetId);
        final String FIND_VARIABLE = "select * from variables " +
            "where internal_name = ? and dataset_id = ?";
        try
        {
            PreparedStatement ps = this.conn.prepareStatement(FIND_VARIABLE);
            ps.setString(1, layerId);
            ps.setString(2, datasetId);
            ResultSet rs = ps.executeQuery();
            if (rs.first())
            {
                H2Layer layer = this.createLayer(rs);
                layer.dataset = this.config.getDatasets().get(datasetId);
                logger.debug("Found layer {} in dataset {}", layerId, datasetId);
                return layer;
            }
            else
            {
                return null; // Variable not found in the database
            }
        }
        catch (SQLException sqle)
        {
            logger.error("Error getting layer " + layerId + " from dataset "
                + datasetId, sqle);
            throw sqle;
        }
    }

    @Override
    public Collection<? extends Layer> getLayersInDataset(String datasetId) throws Exception
    {
        final String FIND_VARIABLES = "select * from variables where dataset_id = ?";
        Dataset ds = this.config.getDatasets().get(datasetId);
        try
        {
            PreparedStatement ps = this.conn.prepareStatement(FIND_VARIABLES);
            ps.setString(1, datasetId);
            ResultSet rs = ps.executeQuery();
            List<H2Layer> layers = new ArrayList<H2Layer>();
            while (rs.next())
            {
                H2Layer layer = this.createLayer(rs);
                layer.dataset = ds;
                layers.add(layer);
            }
            return layers;
        }
        catch (SQLException sqle)
        {
            logger.error("Error getting layers from dataset " + datasetId, sqle);
            throw sqle;
        }
    }
    
    /**
     * Creates and returns a new H2Layer object based on the data at the current
     * cursor position of the given ResultSet.  Does not set the dataset property
     * of the layer.
     */
    private H2Layer createLayer(ResultSet rs) throws SQLException
    {
        H2Layer layer = new H2Layer();
        layer.metadataStore = this;
        layer.id = rs.getLong("id");
        layer.internalName = rs.getString("internal_name");
        layer.title = rs.getString("display_name");
        layer.abstr = rs.getString("description");
        layer.units = rs.getString("units");
        layer.bbox = new double[]{
            rs.getDouble("bbox_west"),
            rs.getDouble("bbox_south"),
            rs.getDouble("bbox_east"),
            rs.getDouble("bbox_north")
        };
        layer.scaleMin = (float)rs.getDouble("color_scale_min");
        layer.scaleMax = (float)rs.getDouble("color_scale_max");
        layer.xAxisId = rs.getLong("x_axis_id");
        layer.yAxisId = rs.getLong("y_axis_id");
        layer.zAxisId = (Long)rs.getObject("z_axis_id"); // can be null
        Long projectionId = (Long)rs.getObject("projection_id");
        if (projectionId != null)
        {
            // We find the projection object in the database
            final String FIND_PROJECTION = "select projection_object " +
                "from projections where id = ?";
            PreparedStatement ps = this.conn.prepareStatement(FIND_PROJECTION);
            ps.setLong(1, projectionId);
            ResultSet proj = ps.executeQuery();
            if (proj.first())
            {
                Object projObj = rs.getObject("projection_object");
                layer.horizProj = (HorizontalProjection)projObj;
            }
        }
        return layer;
    }
    
    /**
     * Synchronizes the metadata of all variables in the given dataset with
     * the database.
     * @param ds The Dataset from which to read metadata
     * @param forceReloadAllMetadata If this is false then only the time axes will be
     * updated for each variable: the rest of the variable metadata will be
     * assumed to be unchanged.  If this is true then all metadata for each
     * variable will be reloaded.
     * @throws Exception if the metadata could not be read
     */
    public void synchronizeMetadata(Dataset ds, boolean forceReloadAllMetadata) throws Exception
    {
        try
        {
            // Create an entry in the database for this dataset, if not already
            // present
            this.insertDataset(ds.getId());

            // Get the DataReader that we'll use to read metadata
            DataReader dr = ds.getDataReader();
            // Get the timestep details for all the data files in the dataset.
            // This also discovers all the variable IDs in the dataset (the keys
            // in the Map).
            Map<String, List<TimestepInfo>> varTimesteps = dr.getTimestepInfoForAllLayers(ds);
            
            // Cycle through each variable
            for (String varId : varTimesteps.keySet())
            {
                List<TimestepInfo> timesteps = varTimesteps.get(varId);
                
                // See if we already have this variable in the database
                boolean varExists = this.containsVariable(ds.getId(), varId);
                if (!varExists || forceReloadAllMetadata)
                {
                    // We need to reload all the metadata for this variable.
                    // We load metadata from the most recent file in the
                    // aggregation
                    String lastFile = timesteps.get(timesteps.size() - 1).getFilename();
                    LayerImpl layer = dr.getLayerMetadata(lastFile, varId);
                    // Now we insert this into the database
                    this.insertVariable(ds.getId(), layer, varExists);
                }
                
                // Now we can update the time axis information
                // TODO: deal with orphans
                
            }
            
            // TODO: what about variables that are in the database that are not
            // present in the data files?  Need to clean them up.

            // Set the last update time of the dataset
        
            // If we've got this far everything is OK and we can commit the changes
            this.conn.commit();
        }
        catch(SQLException sqle)
        {
            this.conn.rollback();
            logger.error("Error synchronizing metadata for dataset " + ds.getId(), sqle);
            throw sqle;
        }
    }
    
    /**
     * Checks to see if a dataset with the given ID exists already in the 
     * database, creating a new entry if not
     * @param datasetId The unique ID of the dataset
     */
    private void insertDataset(String datasetId) throws SQLException
    {
        // First we see if the 
        final String FIND_DATASET = "select from datasets where id = ?";
        PreparedStatement findDataset = this.conn.prepareStatement(FIND_DATASET);
        findDataset.setString(1, datasetId);
        ResultSet rs = findDataset.executeQuery();
        if (!rs.first())
        {
            // This dataset is not present in the database so we must insert it
            final String INSERT_DATASET = "insert into datasets(id) values (?)";
            PreparedStatement insertDataset = this.conn.prepareStatement(INSERT_DATASET);
            insertDataset.setString(1, datasetId);
            insertDataset.executeUpdate();
        }
    }
    
    /**
     * Checks to see if a given variable exists in the database.  This does not
     * alter the database contents.
     * @param datasetId The ID of the dataset to which the variable belongs
     * @param variableId The unique ID of the variable within the dataset
     * @return true if the variable exists in the database, false otherwise.
     */
    private boolean containsVariable(String datasetId, String variableId)
    {
        return true; // TODO
    }
    
    /**
     * Inserts a variable into the database.  Does not insert time axis
     * information
     * @param datasetId The ID of the dataset to which the variable belongs
     * @param layer The object containing the variable's metadaat
     * @param exists True if the variable already exists in the database
     * (meaning that the variable must be deleted 
     */
    private void insertVariable(String datasetId, LayerImpl layer, boolean exists)
    {
        // TODO
    }

    /**
     * This is the only method that can update data in the database
     * @param datasetId
     * @param layers
     * @throws java.lang.Exception
     */
    @Override
    public void setLayersInDataset(String datasetId, Map<String, ? extends Layer> layers) throws Exception
    {
        final String DELETE_DATASET = "delete from datasets where id = ?";
        final String INSERT_DATASET = "insert into datasets(id) values (?)";
        final String INSERT_VARIABLE = "insert into variables" +
            "(internal_name, dataset_id, display_name, description, units, " +
            "bbox_west, bbox_south, bbox_east, bbox_north, color_scale_min, " +
            "color_scale_max, x_axis_id, y_axis_id, z_axis_id, projection_id) " +
            "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        final String SET_LAST_UPDATE = "update datasets set last_updated = ? where id = ?";
        
        try
        {
            // First we delete all the metadata for this dataset.  Foreign key
            // relationships ensure that all metadata is deleted with a single
            // command (see init.sql).
            PreparedStatement deleteDataset = this.conn.prepareStatement(DELETE_DATASET);
            deleteDataset.setString(1, datasetId);
            deleteDataset.executeUpdate();
            
            // Now we create a new dataset
            PreparedStatement insertDataset = this.conn.prepareStatement(INSERT_DATASET);
            insertDataset.setString(1, datasetId);
            insertDataset.executeUpdate();
            
            // Now we create variables, axes and projections
            PreparedStatement insertVariable = this.conn.prepareStatement(INSERT_VARIABLE,
                Statement.RETURN_GENERATED_KEYS);
            insertVariable.setString(2, datasetId);
            for (Layer layer : layers.values())
            {
                // We skip vector layers for the moment
                // TODO: how best to handle these?  We don't want to store
                // redundant metadata but we need to make sure these layers
                // are re-created when a request for layers comes in.  Perhaps
                // we need a separate table for vector layers?
                if (layer instanceof VectorLayer) continue;
                
                long xAxisId = this.findOrInsertXYAxis(datasetId, layer.getXaxis());
                long yAxisId = this.findOrInsertXYAxis(datasetId, layer.getYaxis());
                // This will be null if there is no z axis present in the layer
                Long zAxisId = this.findOrInsertZAxis(datasetId, layer);
                 // Will be null if the projection is lat-lon
                Long projectionId = this.findOrInsertProjection(datasetId,
                    layer.getHorizontalProjection());
                
                insertVariable.setString(1, layer.getId());
                insertVariable.setString(3, layer.getTitle());
                insertVariable.setString(4, layer.getAbstract());
                insertVariable.setString(5, layer.getUnits());
                
                double[] bbox = layer.getBbox();
                insertVariable.setDouble(6, bbox[0]);
                insertVariable.setDouble(7, bbox[1]);
                insertVariable.setDouble(8, bbox[2]);
                insertVariable.setDouble(9, bbox[3]);
                
                insertVariable.setDouble(10, layer.getScaleMin());
                insertVariable.setDouble(11, layer.getScaleMax());
                
                insertVariable.setLong(12, xAxisId);
                insertVariable.setLong(13, yAxisId);
                insertVariable.setObject(14, zAxisId); // can be null
                
                insertVariable.setObject(15, projectionId);
                
                // Insert the new variable and find its primary key
                insertVariable.executeUpdate();
                logger.debug("Inserted variable with id {} in dataset {}",
                    layer.getId(), datasetId);
                ResultSet generatedKeys = insertVariable.getGeneratedKeys();
                if (!generatedKeys.first())
                {
                    // Shouldn't happen
                    throw new IllegalStateException("Generated key not found");
                }
                long varId = generatedKeys.getLong(1);
                logger.debug("Created new variable with primary key {}", varId);
                
                // Now add the timesteps for this variable
                if (layer.isTaxisPresent())
                {
                    this.addTimesteps(varId, datasetId, layer.getTimesteps());
                }
            }
            
            // Finally we set the last-update time for this dataset
            PreparedStatement setLastUpdate = this.conn.prepareStatement(SET_LAST_UPDATE);
            setLastUpdate.setTimestamp(1, new Timestamp(new Date().getTime()));
            setLastUpdate.setString(2, datasetId);
            setLastUpdate.executeUpdate();
            
            // If we've got this far all is well: we can commit the changes
            this.conn.commit();
        }
        catch(SQLException sqle)
        {
            this.conn.rollback();
            logger.error("Error setting layers in dataset " + datasetId, sqle);
            throw sqle;
        }
    }
    
    /**
     * Checks to see if the given axis already exists in the database, returning
     * the id of the axis if so, or inserting a new axis if not.
     * @param datasetId ID of the dataset to which the axis belongs
     * @param axis the axis object
     * @return the id of the existing or inserted axis
     */
    private long findOrInsertXYAxis(String datasetId, CoordAxis axis)
        throws SQLException
    {
        PreparedStatement findAxisId;
        PreparedStatement insertAxis;
        
        if (axis instanceof Regular1DCoordAxis)
        {
            Regular1DCoordAxis regAxis = (Regular1DCoordAxis)axis;
            
            final String FIND_AXIS_ID = "select id from axes where " +
                "dataset_id=? and axis_type=? and units=? and " +
                "axis_start=? and axis_stride=? and axis_count=?";
            findAxisId = this.conn.prepareStatement(FIND_AXIS_ID);
            findAxisId.setDouble(4, regAxis.getStart());
            findAxisId.setDouble(5, regAxis.getStride());
            findAxisId.setInt(6, regAxis.getSize());
            
            final String INSERT_AXIS = "insert into axes" +
                "(dataset_id, axis_type, units, axis_start, axis_stride, axis_count) " +
                "values(?,?,?,?,?,?)";
            // We will need to be able to get the generated axis id from the
            // insert statement
            insertAxis = this.conn.prepareStatement(INSERT_AXIS,
                Statement.RETURN_GENERATED_KEYS);
            insertAxis.setDouble(4, regAxis.getStart());
            insertAxis.setDouble(5, regAxis.getStride());
            insertAxis.setInt(6, regAxis.getSize());
        }
        else if (axis instanceof Irregular1DCoordAxis)
        {
            Irregular1DCoordAxis irregAxis = (Irregular1DCoordAxis)axis;
            
            final String FIND_AXIS_ID = "select id from axes where " +
                "dataset_id=? and axis_type=? and units=? and axis_values=?";
            findAxisId = this.conn.prepareStatement(FIND_AXIS_ID);
            findAxisId.setObject(4, irregAxis.getCoordValues());
            
            final String INSERT_AXIS = "insert into axes" +
                "(dataset_id, axis_type, units, axis_values) values(?,?,?,?)";
            // We will need to be able to get the generated axis id from the
            // insert statement
            insertAxis = this.conn.prepareStatement(INSERT_AXIS,
                Statement.RETURN_GENERATED_KEYS);
            insertAxis.setObject(4, irregAxis.getCoordValues());
        }
        else if (axis instanceof LUTCoordAxis)
        {
            LUTCoordAxis lutAxis = (LUTCoordAxis)axis;
            
            final String FIND_AXIS_ID = "select id from axes where " +
                "dataset_id=? and axis_type=? and units=? and lut_filename=?";
            findAxisId = this.conn.prepareStatement(FIND_AXIS_ID);
            findAxisId.setObject(4, lutAxis.getFilename());
            
            final String INSERT_AXIS = "insert into axes" +
                "(dataset_id, axis_type, units, lut_filename) values(?,?,?,?)";
            // We will need to be able to get the generated axis id from the
            // insert statement
            insertAxis = this.conn.prepareStatement(INSERT_AXIS,
                Statement.RETURN_GENERATED_KEYS);
            insertAxis.setObject(4, lutAxis.getFilename());
        }
        else
        {
            throw new IllegalArgumentException("Axis type " +
                axis.getClass().getName() + " not supported");
        }
        
        // Look to see if we already have this axis
        findAxisId.setString(1, datasetId);
        findAxisId.setString(2, axis.getAxisType().toString());
        findAxisId.setString(3, axis.getUnits());
        ResultSet rs = findAxisId.executeQuery();
        if (rs.first())
        {
            long axisId = rs.getLong(1);
            logger.debug("Found axis with id {}", axisId);
            // We already have this axis so return its id
            return axisId;
        }
        else
        {
            // We need to insert a new axis
            insertAxis.setString(1, datasetId);
            insertAxis.setString(2, axis.getAxisType().toString());
            insertAxis.setString(3, axis.getUnits());
            insertAxis.executeUpdate();
            // return the newly-generated key
            ResultSet generatedKeys = insertAxis.getGeneratedKeys();
            if (generatedKeys.first())
            {
                long axisId = generatedKeys.getLong(1);
                logger.debug("Created new axis with id {}", axisId);
                return axisId;
            }
            else
            {
                // Shouldn't happen
                throw new IllegalStateException("Generated key not found");
            }
        }
    }
    
    /**
     * Checks to see if the a matching z axis already exists in the database, returning
     * the id of the axis if so, or inserting a new axis if not.
     * @param datasetId ID of the dataset to which the axis belongs
     * @param layer the layer to which the z axis belongs
     * @return the id of the existing or inserted axis or null if there is no
     * z axis in the layer
     * @todo Will be obsolete when we model a z axis as an Irregular1DCoordAxis
     */
    private Long findOrInsertZAxis(String datasetId, Layer layer)
        throws SQLException
    {
        if (!layer.isZaxisPresent()) return null;
        String axisType = "z"; // TODO: need to replace with proper AxisType
        
        // Try to find this axis in the database
        final String FIND_AXIS_ID = "select id from axes where " +
            "dataset_id=? and axis_type=? and units=? and positive_up=? and axis_values=?";
        PreparedStatement findAxisId = this.conn.prepareStatement(FIND_AXIS_ID);
        findAxisId.setString(1, datasetId);
        findAxisId.setString(2, axisType);
        findAxisId.setString(3, layer.getZunits());
        findAxisId.setBoolean(4, layer.isZpositive());
        findAxisId.setObject(5, layer.getZvalues());
        ResultSet rs = findAxisId.executeQuery();
        if (rs.first())
        {
            // We've found a matching axis, so return its id
            return rs.getLong(1);
        }
        else
        {
            // We need to insert a new axis
            final String INSERT_AXIS = "insert into axes" +
                "(dataset_id, axis_type, units, positive_up, axis_values) " +
                "values(?,?,?,?,?)";
            PreparedStatement insertAxis = this.conn.prepareStatement(INSERT_AXIS,
                Statement.RETURN_GENERATED_KEYS);
            insertAxis.setString(1, datasetId);
            insertAxis.setString(2, axisType);
            insertAxis.setString(3, layer.getZunits());
            insertAxis.setBoolean(4, layer.isZpositive());
            insertAxis.setObject(5, layer.getZvalues());
            insertAxis.executeUpdate();
            // return the newly-generated key
            ResultSet generatedKeys = insertAxis.getGeneratedKeys();
            if (generatedKeys.first())
            {
                return generatedKeys.getLong(1);
            }
            else
            {
                // Shouldn't happen
                throw new IllegalStateException("Generated key not found");
            }
        }
    }

    /**
     * Find the id of the given projection in the database, or inserts a new
     * projection and returns its id
     * @param datasetId The id of the dataset to which the projection belongs
     * @param horizProj The projection object itself
     * @return the id (primary key) of the projection in the database, or null
     * if the projection is a lat-lon projection
     */
    private Long findOrInsertProjection(String datasetId,
        HorizontalProjection horizProj) throws SQLException
    {
        if (horizProj.isLatLon()) return null;
        final String FIND_PROJECTION = "select id from projections " +
            "where dataset_id=? and projection_object=?";
        PreparedStatement findProj = this.conn.prepareStatement(FIND_PROJECTION);
        findProj.setString(1, datasetId);
        findProj.setObject(2, horizProj);
        ResultSet rs = findProj.executeQuery();
        if (rs.first())
        {
            // We've found a matching projection
            return rs.getLong("id");
        }
        else
        {
            // We need to create a new entry in the table
            final String INSERT_PROJECTION = "insert into projections " +
                "(dataset_id, projection_object) values (?,?)";
            PreparedStatement insertProj = this.conn.prepareStatement(INSERT_PROJECTION,
                Statement.RETURN_GENERATED_KEYS);
            insertProj.setString(1, datasetId);
            insertProj.setObject(2, horizProj);
            insertProj.executeUpdate();
            // Find the automatically-generated id for this projection
            ResultSet keys = insertProj.getGeneratedKeys();
            keys.first();
            return keys.getLong(1);
        }
    }

    /**
     * Adds the timesteps for a particular variable to the database
     * @param varId The primary key of the variable in question
     * @param timesteps List of TimestepInfo object representing the time
     * axis for the variable
     * @todo The whole time axis thing needs more thought and refactoring...
     * The List of TimestepInfo objects may not contain all the timesteps for
     * some files (if there are duplicate Dates due to different forecast
     * periods)
     */
    private void addTimesteps(long varId, String datasetId, List<TimestepInfo> timesteps)
        throws SQLException
    {
        final String FIND_DATA_FILE = "select id from data_files " +
            "where filepath = ?";
        final String INSERT_DATA_FILE = "insert into data_files" +
            "(dataset_id, filepath) values (?,?)";
        final String FIND_TIMESTEP = "select id from timesteps " +
            "where data_file_id=? and index_in_file=? and timestep=?";
        final String INSERT_TIMESTEP = "insert into timesteps" +
            "(data_file_id, index_in_file, timestep) values(?,?,?)";
        final String INSERT_VARIABLE_TIMESTEP = "insert into variables_timesteps" +
            "(variable_id, timestep_id) values (?,?)";
        
        PreparedStatement findDataFile = this.conn.prepareStatement(FIND_DATA_FILE);
        PreparedStatement insertDataFile = this.conn.prepareStatement(INSERT_DATA_FILE,
            Statement.RETURN_GENERATED_KEYS);
        insertDataFile.setString(1, datasetId);
        PreparedStatement findTimestep = this.conn.prepareStatement(FIND_TIMESTEP);
        PreparedStatement insertTimestep = this.conn.prepareStatement(INSERT_TIMESTEP,
            Statement.RETURN_GENERATED_KEYS);
        PreparedStatement insertVariableTimestep =
            this.conn.prepareStatement(INSERT_VARIABLE_TIMESTEP);
        
        for (TimestepInfo tInfo : timesteps)
        {
            // Look to see if we already have an entry for this data file
            long dataFileId;
            findDataFile.setString(1, tInfo.getFilename());
            ResultSet dataFiles = findDataFile.executeQuery();
            if (dataFiles.first())
            {
                // We've found a matching data file
                dataFileId = dataFiles.getLong("id");
            }
            else
            {
                // We need to insert a new data file
                insertDataFile.setString(2, tInfo.getFilename());
                insertDataFile.executeUpdate();
                ResultSet keys = insertDataFile.getGeneratedKeys();
                keys.first(); // Should always succeed
                dataFileId = keys.getLong(1);
            }
            
            // Now look for a matching timestep
            long timestepId;
            findTimestep.setLong(1, dataFileId);
            findTimestep.setInt(2, tInfo.getIndexInFile());
            findTimestep.setTimestamp(3, new Timestamp(tInfo.getDate().getTime()));
            ResultSet tsteps = findTimestep.executeQuery();
            if (tsteps.first())
            {
                // We've found a matching timestep
                timestepId = tsteps.getLong("id");
            }
            else
            {
                // We need to insert a new timestep into the table
                insertTimestep.setLong(1, dataFileId);
                insertTimestep.setInt(2, tInfo.getIndexInFile());
                insertTimestep.setTimestamp(3, new Timestamp(tInfo.getDate().getTime()));
                insertTimestep.executeUpdate();
                ResultSet keys = insertTimestep.getGeneratedKeys();
                keys.first();
                timestepId = keys.getLong(1);
            }
            
            // Now we can make an entry in the variables_timesteps table to
            // map this variable to its corresponding timesteps
            insertVariableTimestep.setLong(1, varId);
            insertVariableTimestep.setLong(2, timestepId);
            insertVariableTimestep.executeUpdate();
        }
        
        // Free resources: TODO: do this in a finally clause?
        findDataFile.close();
        insertDataFile.close();
        findTimestep.close();
        insertTimestep.close();
        insertVariableTimestep.close();
    }
    
    /**
     * @return the time of the last update of the dataset with the given id,
     * or null if the dataset has not yet been loaded into this store.  If an
     * error occurs loading the last update time (which should be unlikely)
     * this will log the error and return null.
     */
    @Override
    public Date getLastUpdateTime(String datasetId)
    {
        final String SQL_COMMAND = "select last_updated from datasets where id = ?";
        try
        {
            PreparedStatement ps = this.conn.prepareStatement(SQL_COMMAND);
            ps.setString(1, datasetId);
            ResultSet rs = ps.executeQuery();
            if (rs.first()) return rs.getTimestamp(1);
            else return null;
        }
        catch(SQLException sqle)
        {
            logger.error("Error getting last update time for dataset " +
                datasetId, sqle);
            return null;
        }
    }
    
    /**
     * Retrieves the coordinate axis with the given ID from the database.
     * This is called by H2Layer.getXaxis() and .getYaxis()
     * @return The coordinate axis, or null if not found
     * @throws RuntimeException if there was an error reading the axis
     */
    CoordAxis getCoordAxis(long id)
    {
        logger.debug("Finding coordinate axis {}", id);
        final String FIND_AXIS = "select * from axes where id=?";
        try
        {
            PreparedStatement ps = this.conn.prepareStatement(FIND_AXIS);
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.first())
            {
                AxisType axisType = AxisType.getType(rs.getString("axis_type"));
                String units = rs.getString("units");
                Boolean posUp = (Boolean)rs.getObject("positive_up");
                PositiveDirection positiveDirection = PositiveDirection.getPositiveDirection(posUp);
                if (rs.getObject("axis_start") != null)
                {
                    logger.debug("Found regular coordinate axis");
                    return new Regular1DCoordAxis(
                        rs.getDouble("axis_start"),
                        rs.getDouble("axis_stride"),
                        rs.getInt("axis_count"),
                        axisType,
                        units
                    );
                }
                else if (rs.getObject("axis_values") != null)
                {
                    // This is an irregular axis: we need to get the axis values
                    // Note that we store the axis values as a serialized object,
                    // not a SQL Array, because H2 would require the double[]
                    // array to be converted to Double[]
                    double[] vals = (double[])rs.getObject("axis_values");
                    logger.debug("Found irregular coordinate axis");
                    return new Irregular1DCoordAxis(vals, axisType, units, positiveDirection);
                }
                else if (rs.getString("lut_filename") != null)
                {
                    // TODO: assuming isLatLon = true
                    logger.debug("Found LUT coordinate axis");
                    return LUTCoordAxis.createAxis(rs.getString("lut_filename"), axisType);
                }
                else
                {
                    // Shouldn't happen
                    throw new IllegalStateException("Unrecognized axis type");
                }
            }
            else
            {
                // No results found: shouldn't happen
                return null;
            }
        }
        catch(Exception e)
        {
            String message = "Error loading axis " + id + " from database";
            logger.error(message, e);
            throw new RuntimeException(message, e);
        }
    }
    
    /**
     * Finds all the timesteps for the variable with the given id.
     * @param variableId The unique id for the variable
     * @return a newly-created List of TimestepInfo objects.  If there is
     * no time axis for the variable this returns an empty list.
     * @todo Not the best structure, the whole time axis thing needs refactoring
     */
    List<TimestepInfo> getTimesteps(long variableId)
    {
        logger.debug("Getting timesteps for variable {}", variableId);
        List<TimestepInfo> tInfo = new ArrayList<TimestepInfo>();
        // TODO: any way to optimize this query?
        final String FIND_TIMESTEPS =
            "select timesteps.timestep, data_files.filepath, timesteps.index_in_file " +
            "from data_files, timesteps, variables_timesteps " +
            "where variables_timesteps.variable_id = ? " + 
            "and variables_timesteps.timestep_id = timesteps.id " +
            "and timesteps.data_file_id = data_files.id";
        try
        {
            PreparedStatement ps = this.conn.prepareStatement(FIND_TIMESTEPS);
            ps.setLong(1, variableId);
            ResultSet rs = ps.executeQuery();
            while(rs.next())
            {
                TimestepInfo t = new TimestepInfo(
                    new Date(rs.getTimestamp("timestep").getTime()),
                    rs.getString("filepath"),
                    rs.getInt("index_in_file")
                );
                tInfo.add(t);
            }
        }
        catch(SQLException sqle)
        {
            String message = "Error loading timesteps for variable " +
                variableId + " from database";
            logger.error(message, sqle);
            throw new RuntimeException(message, sqle);
        }
        logger.debug("Found timesteps for variable {}", variableId);
        return tInfo;
    }
    
    /**
     * Called by Spring to close the database
     */
    @Override
    public void close()
    {
        if (this.conn != null)
        {
            try { this.conn.close(); }
            catch(SQLException sqle)
            {
                logger.error("Error closing H2 metadata database", sqle);
            }
        }
        logger.info("H2 metadata database closed");
    }

}
