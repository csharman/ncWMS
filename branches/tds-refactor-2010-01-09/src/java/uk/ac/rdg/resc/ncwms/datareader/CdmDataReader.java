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

package uk.ac.rdg.resc.ncwms.datareader;

import java.util.Set;
import org.joda.time.DateTime;
import ucar.nc2.dt.GridDataset;
import uk.ac.rdg.resc.ncwms.wms.Dataset;
import uk.ac.rdg.resc.ncwms.wms.Layer;

/**
 * Contains static methods for creating {@link Dataset} and {@link Layer} objects
 * from a CDM {@link GridDataset} object.  This method is useful in both a ncWMS
 * and THREDDS context.
 * @author Jon
 */
public class CdmDataReader
{
    /**
     * Creates a {@link Dataset} object from the given {@link GridDataset}.  This
     * method does not close the passed-in GridDataset.  The returned Dataset
     * will contain a reference to the passed-in GridDataset so the GridDataset
     * cannot be garbage-collected until the Dataset is itself ready for
     * garbage collection.
     * @param gd
     * @return a {@link Dataset} object that encapsulates the GridDataset.
     */
    public static Dataset createDataset(GridDataset gd)
    {
        return new CdmDataset(gd);
    }

    public static Map<String, Layer> readAllLayers(GridDataset gd)
    {

    }

    public static Layer getLayerById(GridDataset gd, String variableId)
    {

    }

    private static class CdmDataset implements Dataset
    {
        private GridDataset gd;

        public CdmDataset(GridDataset gd)
        {
            this.gd = gd;
        }

        @Override
        public String getId() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getTitle() {
            return this.gd.getTitle();
        }

        @Override
        public String getCopyrightStatement() {
            return "";
        }

        @Override
        public String getMoreInfoUrl() {
            return "";
        }

        @Override
        public DateTime getLastUpdateTime() {
            return new DateTime();
        }

        @Override
        public Set<Layer> getLayers() {
            return
        }
    }
}
