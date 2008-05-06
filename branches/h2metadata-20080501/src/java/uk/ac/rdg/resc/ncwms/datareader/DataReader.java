/*
 * Copyright (c) 2007 The University of Reading
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uk.ac.rdg.resc.ncwms.config.Dataset;
import uk.ac.rdg.resc.ncwms.metadata.Layer;
import uk.ac.rdg.resc.ncwms.metadata.LayerImpl;
import uk.ac.rdg.resc.ncwms.metadata.TimestepInfo;
import uk.ac.rdg.resc.ncwms.utils.WmsUtils;

/**
 * Abstract superclass for classes that read data and metadata from datasets.
 * Only one instance of each DataReader class will be created, hence subclasses
 * must be thread-safe (no instance variables etc).
 * @see DefaultDataReader
 * @author jdb
 */
public abstract class DataReader
{
    /**
     * Maps class names to DataReader objects.  Only one DataReader object of
     * each class will ever be created.
     */
    private static Map<String, DataReader> readers = new HashMap<String, DataReader>();
    
    /**
     * This class can only be instantiated through getDataReader()
     */
    protected DataReader() {}
    
    /**
     * Gets a DataReader object.  <b>Only one</b> object of each class will be
     * created (hence methods have to be thread-safe).
     * 
     * @param className Name of the class to generate
     * @param location the location of the dataset: used to detect OPeNDAP URLs
     * @return If className is empty, this returns a {@link DefaultDataReader}, unless
     * the location is an OPeNDAP location, in which case this returns a
     * {@link BoundingBoxDataReader}.  If className is not empty, this will
     * return an instance of the corresponding class.
     * @throws an Exception if the DataReader could not be created
     */
    public static DataReader getDataReader(String className, String location)
        throws Exception
    {
        String clazz = DefaultDataReader.class.getName();
        if (WmsUtils.isOpendapLocation(location))
        {
            clazz = BoundingBoxDataReader.class.getName();
        }
        if (className != null && !className.trim().equals(""))
        {
            clazz = className;
        }
        if (!readers.containsKey(clazz))
        {
            // Create the DataReader object
            Object drObj = Class.forName(clazz).newInstance();
            // this will throw a ClassCastException if drObj is not a DataReader
            readers.put(clazz, (DataReader)drObj);
        }
        return readers.get(clazz);
    }
    
    /**
     * Reads an array of data from a NetCDF file and projects onto the given
     * {@link HorizontalGrid}.  Reads data for a single timestep only.  This method knows
     * nothing about aggregation: it simply reads data from the given file.
     * Missing values (e.g. land pixels in oceanography data) will be represented
     * by Float.NaN.
     *
     * @param filename Location of the file, NcML aggregation or OPeNDAP URL
     * @param layer {@link Layer} object representing the variable
     * @param tIndex The index along the time axis (or -1 if there is no time axis)
     * @param zIndex The index along the vertical axis (or -1 if there is no vertical axis)
     * @param grid The grid onto which the data are to be read
     * @throws Exception if an error occurs
     */
    public abstract float[] read(String filename, Layer layer,
        int tIndex, int zIndex, HorizontalGrid grid)
        throws Exception;
    
    /**
     * Reads and returns the metadata for all the layers (i.e. variables) in the
     * given {@link Dataset}.
     * @param ds Object describing the Dataset to which the layer belongs
     * @return Map of layer IDs mapped to {@link LayerImpl} objects
     * @throws IOException if there was an error reading from the data source
     */
    public Map<String, LayerImpl> getAllLayers(Dataset ds)
        throws IOException
    {
        List<String> filenames = ds.getFilenames();
        // Now extract the data for each individual file
        Map<String, LayerImpl> layers = new HashMap<String, LayerImpl>();
        for (String filename : filenames)
        {
            // Read the metadata from the file and update the Map.
            // TODO: only do this if the file's last modified date has changed?
            // This would require us to keep the previous metadata...
            this.findAndUpdateLayers(filename, layers);
        }
        return layers;
    }
    
    /**
     * Looks through all the data files in the provided dataset, finding the
     * relationship between variables, timesteps and filenames.
     * @return A Map of internal variable IDs to TimestepInfo objects that
     * define which file contains which timesteps for that variable.
     */
    public abstract Map<String, List<TimestepInfo>> getTimestepInfoForAllLayers(Dataset ds);
    
    /**
     * Gets the metadata for a particular layer/variable, not including time
     * axis information.
     * @param location The file/OPeNDAP url/NcML aggregation that contains the 
     * layer
     * @param layerId The unique ID of the layer within the file
     * @return a LayerImpl object.  The TimestepInfo part of this object does
     * not need to be completed.
     * @todo should not return a LayerImpl object!
     */
    public abstract LayerImpl getLayerMetadata(String location, String layerId);
    
    /**
     * Reads the metadata for all the variables in the dataset
     * at the given location, which is the location of a NetCDF file, NcML
     * aggregation, or OPeNDAP location (i.e. one element resulting from the
     * expansion of a glob aggregation).
     * @param location Full path to the dataset
     * @param layers Map of Layer Ids to Layer objects to populate or update
     * @throws IOException if there was an error reading from the data source
     */
    @Deprecated
    protected abstract void findAndUpdateLayers(String location, Map<String, LayerImpl> layers)
        throws IOException;
    
}
