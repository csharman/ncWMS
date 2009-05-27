/*
 * Copyright (c) 2009 The University of Reading
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

import java.util.Collections;
import java.util.Set;

/**
 * Class that associates a data value with all the indices in some coordinate
 * system that contain this value.  For example, the indices might refer to
 * points within a {@link HorizontalGrid}, and objects of this class might be
 * used to populate the grid with data values, in order to generate a map image.
 * Alternatively, the indices might refer to points along a transect.
 * @author Jon
 */
public final class DataValues {
    
    private final float dataValue;
    private final Set<Integer> indices;
    
    public DataValues(float dataValue, Set<Integer> indices) {
        this.dataValue = dataValue;
        this.indices = Collections.unmodifiableSet(indices);
    }

    /**
     * Returns the data value
     */
    public float getDataValue() {
        return this.dataValue;
    }

    /**
     * Returns the indices that are associated with the data value
     */
    public Set<Integer> getIndices() {
        return this.indices;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + Float.floatToIntBits(dataValue);
        result = 31 * result + this.indices.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof DataValues)) return false;
        DataValues other = (DataValues)obj;
        return Float.compare(this.dataValue, other.dataValue) == 0
            && this.indices.equals(other.indices);
    }

    @Override
    public String toString() {
        return this.dataValue + ": " + this.indices.toString();
    }

}
