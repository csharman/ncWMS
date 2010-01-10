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

package uk.ac.rdg.resc.ncwms.wms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Brings time aggregation capabilities to the {@link AbstractLayer} class.
 * This class allows for the fact that different timesteps might be contained
 * within different files within the {@link #getDataset() dataset}.  If two files
 * contain information for the same time, the file with the shortest forecast time
 * is chosen, which is more likely to be the "more accurate" data.  This logic
 * implements the "best estimate" timeseries of a forecast model run collection.
 * @author Jon
 */
public abstract class AbstractTimeAggregatedLayer extends AbstractLayer
{
    private static final Logger logger = LoggerFactory.getLogger(AbstractTimeAggregatedLayer.class);

    /** These are sorted into ascending order of time */
    private final List<TimestepInfo> timesteps = new ArrayList<TimestepInfo>();
    
    /**
     * Creates an AbstractTimeAggregatedLayer with a bounding box that covers
     * the whole world and the given identifier.
     * @param id An identifier that is unique within this layer's
     * {@link #getDataset() dataset}.
     * @throws NullPointerException if {@code id == null}
     */
    public AbstractTimeAggregatedLayer(String id)
    {
        super(id);
    }

    /**
     * Returns the list of time instants that are valid for this layer, in
     * chronological order, or an empty list if this Layer does not have a time axis.
     * @return the list of time instants that are valid for this layer, in
     * chronological order, or an empty list if this Layer does not have a time axis.
     */
    @Override
    public List<DateTime> getTimeValues()
    {
        List<DateTime> tVals = new ArrayList<DateTime>(this.timesteps.size());
        synchronized(this.timesteps) {
            for (TimestepInfo tInfo : this.timesteps) {
                tVals.add(tInfo.getDateTime());
            }
        }
        return tVals;
    }

    /**
     * Adds a new TimestepInfo to this layer.  If a TimestepInfo object
     * already exists for this timestep, the TimestepInfo object with the lower
     * indexInFile value is chosen (this is most likely to be the result of a
     * shorter forecast lead time and therefore more accurate).
     * @param timestep The real date/time of this timestep
     * @param filename The filename containing this timestep
     * @param indexInFile The index of this timestep in the file
     * @throws NullPointerException if {@code timestep} or {@code filename}
     * is null.
     * @throws IllegalArgumentException if {@code indexInFile} is less than zero
     */
    public void addTimestepInfo(DateTime dt, String filename, int indexInFile)
    {
        TimestepInfo tInfo = new TimestepInfo(dt, filename, indexInFile);
        synchronized(this.timesteps) {
            // Find the insertion point in the List of timesteps
            int index = Collections.binarySearch(this.timesteps, tInfo);
            if (index >= 0)
            {
                // We already have a timestep for this time
                TimestepInfo existingTStep = this.timesteps.get(index);
                if (tInfo.getIndexInFile() < existingTStep.getIndexInFile())
                {
                    // The new info probably has a shorter forecast time and so we
                    // replace the existing version with this one
                    existingTStep = tInfo;
                }
            }
            else
            {
                // We need to insert the TimestepInfo object into the list at the
                // correct location to ensure that the list is sorted in ascending
                // order of time.
                int insertionPoint = -(index + 1); // see docs for Collections.binarySearch()
                this.timesteps.add(insertionPoint, tInfo);
            }
        }
    }

    /**
     * Finds and returns the {@link TimestepInfo} object with the given
     * timestep.
     */
    protected TimestepInfo findTimestepInfo(DateTime target)
    {
        int index = this.findTimestepInfoIndex(target);
        
    }

    /**
     * Finds and returns the index of the {@link TimestepInfo} object with the
     * given timestep, or -1 if the given timestep does not exist in this layer.
     */
    private int findTimestepInfoIndex(DateTime target)
    {
        logger.debug("Looking for {} in layer {}", target, this.id);
        // Adapted from Collections.binarySearch()
        int low = 0;
        int high = this.timesteps.size() - 1;

        while (low <= high)
        {
            int mid = (low + high) >>> 1;
            DateTime midVal = this.timesteps.get(mid).getDateTime();
            if (midVal.isBefore(target)) low = mid + 1;
            else if (midVal.isAfter(target)) high = mid - 1;
            else return mid; // key found
        }

        // The given time doesn't match any axis value
        logger.debug("{} not found", target);
        return -1;
    }

    /**
     * Simple class that holds information about which files in an aggregation
     * hold which timesteps for a variable.  Implements Comparable to allow
     * collections of this class to be sorted in order of their timestep.
     * Instances of this class are immutable.
     */
    protected static class TimestepInfo implements Comparable<TimestepInfo>
    {
        private DateTime timestep;
        private String filename;
        private int indexInFile;

        /**
         * Creates a new TimestepInfo object
         * @param timestep The real date/time of this timestep
         * @param filename The filename containing this timestep
         * @param indexInFile The index of this timestep in the file
         * @throws NullPointerException if {@code timestep} or {@code filename}
         * is null.
         * @throws IllegalArgumentException if {@code indexInFile} is less than zero
         */
        public TimestepInfo(DateTime timestep, String filename, int indexInFile)
        {
            if (timestep == null || filename == null)
            {
                throw new NullPointerException();
            }
            if (indexInFile < 0)
            {
                throw new IllegalArgumentException("indexInFile must be >= 0");
            }
            this.timestep = timestep;
            this.filename = filename;
            this.indexInFile = indexInFile;
        }

        public String getFilename()
        {
            return this.filename;
        }

        public int getIndexInFile()
        {
            return this.indexInFile;
        }

        /**
         * @return the date-time that this timestep represents
         */
        public DateTime getDateTime()
        {
            return this.timestep;
        }

        /**
         * Sorts based on the timestep only
         */
        @Override
        public int compareTo(TimestepInfo otherInfo)
        {
            return this.timestep.compareTo(otherInfo.timestep);
        }

        /**
         * Compares all fields for equality, using only the millisecond value
         * to compare the timesteps.
         */
        @Override
        public boolean equals(Object obj)
        {
            if (this == obj) return true;
            if (!(obj instanceof TimestepInfo)) return false;
            TimestepInfo otherTstep = (TimestepInfo)obj;
            return this.timestep.isEqual(otherTstep.timestep) && // Compares based on millisecond value only
                   this.indexInFile == otherTstep.indexInFile &&
                   this.filename.equals(otherTstep.filename);
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 41 * hash + this.timestep.hashCode();
            hash = 41 * hash + this.filename.hashCode();
            hash = 41 * hash + this.indexInFile;
            return hash;
        }
    }

}
