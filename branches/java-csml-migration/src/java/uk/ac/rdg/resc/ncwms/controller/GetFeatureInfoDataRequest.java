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

package uk.ac.rdg.resc.ncwms.controller;

import org.jcsml.ncutils.utils.NcUtils;

import uk.ac.rdg.resc.ncwms.exceptions.InvalidPointException;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;

/**
 * The portion of a GetFeatureInfoRequest that pertains to extraction of 
 * data (rather than presentation).  Shares most of its parameters with
 * GetMapDataRequest and hence inherits from it (TODO: probably not the most
 * logical structure since it's not a real "is-a" relationship - we're only doing
 * this to save code repetition).
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class GetFeatureInfoDataRequest
{
    private final int pixelColumn; // I and J indices of the feature in question on the map
    private final int pixelRow;
    private final int featureCount;
    private final String[] layerNames;
	private final String crsCode;
	private final double[] bbox;
	private final int height;
	private final int width;
	private final String timeString;
	private final String elevationString;


	/**
     * Creates a new instance of GetFeatureInfoDataRequest
     */
    public GetFeatureInfoDataRequest(RequestParams params, String version) throws WmsException
    {
    	
        this.layerNames = params.getMandatoryString("query_layers").split(",");
        this.crsCode = params.getMandatoryString(params.getMandatoryWmsVersion().equals("1.3.0") ? "crs" : "srs");
        this.bbox = NcUtils.parseBbox(params.getMandatoryString("bbox"));
        this.width = params.getMandatoryPositiveInt("width");
        this.height = params.getMandatoryPositiveInt("height");
        this.timeString = params.getString("time");
        this.elevationString = params.getString("elevation");
        this.featureCount = params.getPositiveInt("feature_count", 1);
        this.pixelColumn = params.getMandatoryPositiveInt("i");
        if (this.pixelColumn > this.getWidth() - 1)
        {
            throw new InvalidPointException("i");
        }
        this.pixelRow = params.getMandatoryPositiveInt("j");
        if (this.pixelRow > this.getHeight() - 1)
        {
            throw new InvalidPointException("j");
        }
    }

    /**
	 * @return the layerNames
	 */
	public String[] getLayerNames()
	{
		return layerNames;
	}

	/**
	 * @return the crsCode
	 */
	public String getCrsCode()
	{
		return crsCode;
	}

	/**
	 * @return the bbox
	 */
	public double[] getBbox()
	{
		return bbox;
	}

	/**
	 * @return the height
	 */
	public int getHeight()
	{
		return height;
	}

	/**
	 * @return the width
	 */
	public int getWidth()
	{
		return width;
	}

	/**
	 * @return the timeString
	 */
	public String getTimeString()
	{
		return timeString;
	}

	/**
	 * @return the elevationString
	 */
	public String getElevationString()
	{
		return elevationString;
	}

	public int getPixelColumn()
    {
        return pixelColumn;
    }

    public int getPixelRow()
    {
        return pixelRow;
    }

    public int getFeatureCount()
    {
        return featureCount;
    }
    
}
