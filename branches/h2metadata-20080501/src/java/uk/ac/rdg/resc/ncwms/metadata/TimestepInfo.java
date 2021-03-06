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

package uk.ac.rdg.resc.ncwms.metadata;

import java.util.Date;

/**
 * Simple class that holds information about which files in an aggregation
 * hold which timesteps for a variable.  Implements Comparable to allow
 * collections of this class to be sorted in order of their timestep.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class TimestepInfo implements Comparable<TimestepInfo>
{
    private Date timestep;
    private String filename;
    private int indexInFile;

    /**
     * Creates a new TimestepInfo object
     * @param timestep The real date/time of this timestep
     * @param filename The filename containing this timestep
     * @param indexInFile The index of this timestep in the file
     */
    public TimestepInfo(Date timestep, String filename, int indexInFile)
    {
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
    public Date getDate()
    {
        return this.timestep;
    }

    /**
     * Sorts based on the timestep only
     */
    public int compareTo(TimestepInfo otherInfo)
    {
        return this.timestep.compareTo(otherInfo.timestep);
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 29 * hash + (this.timestep != null ? this.timestep.hashCode() : 0);
        hash = 29 * hash + (this.filename != null ? this.filename.hashCode() : 0);
        hash = 29 * hash + this.indexInFile;
        return hash;
    }
    
    /**
     * Compares all fields for equality
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (!(obj instanceof TimestepInfo)) return false;
        TimestepInfo otherTstep = (TimestepInfo)obj;
        return this.timestep.equals(otherTstep.timestep) &&
               this.indexInFile == otherTstep.indexInFile &&
               this.filename.equals(otherTstep.filename);
    }
}
