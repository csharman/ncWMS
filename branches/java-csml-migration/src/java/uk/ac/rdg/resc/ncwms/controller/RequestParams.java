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

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.jcsml.ncutils.config.ColorScaleRange;
import org.jcsml.ncutils.config.MapData;
import org.jcsml.ncutils.config.MapLegendData;
import org.jcsml.ncutils.config.MapStyleData;
import org.jcsml.ncutils.utils.NcUtils;

import uk.ac.rdg.resc.ncwms.exceptions.WmsException;

/**
 * Class that contains the parameters of the user's request.  Parameter names
 * are not case sensitive.
 *
 * @author Jon Blower
 */
public class RequestParams
{
    private Map<String, String> paramMap = new HashMap<String, String>();
    
    /**
     * Creates a new RequestParams object from the given Map of parameter names
     * and values (normally gained from HttpServletRequest.getParameterMap()).
     * The Map matches parameter names (Strings) to parameter values (String
     * arrays).
     */
    public RequestParams(Map<String, String[]> httpRequestParamMap)
    {
        for (String name : httpRequestParamMap.keySet())
        {
            String[] values = (String[])httpRequestParamMap.get(name);
            assert values.length >= 1;
            this.paramMap.put(unquotePlus(name.toString().trim()).toLowerCase(),
                unquotePlus(values[0].trim()));
        }
    }
    
    /**
     * Replaces URL escape sequences with their correct characters, and replaces
     * plus signs with spaces.  Nearly a direct port of urllib.unquote_plus() in
     * Python 2.3.
     * @todo doesn't handle "%%" correctly as an escape sequence for "%"
     */
    private static final String unquotePlus(String s)
    {
        s = s.replaceAll("\\+", " ");
        String[] items = s.split("%");
        StringBuffer buf = new StringBuffer(items[0]);
        for (int i = 1; i < items.length; i++)
        {
            System.out.println(items[i]);
            // The first two characters of each item will be a hex representation
            // of the ASCII code of the escaped character
            if (items[i].length() >= 2)
            {
                try
                {
                    int charNum = Integer.parseInt(items[i].substring(0, 2), 16);
                    buf.append((char)charNum);
                    buf.append(items[i].substring(2));
                }
                catch(NumberFormatException nfe)
                {
                    // It wasn't a valid hex code
                    buf.append("%" + items[i]);
                }
            }
            else
            {
                buf.append("%" + items[i]);
            }
        }
        return buf.toString();
    }
    
    /**
     * Returns the value of the parameter with the given name as a String, or null if the
     * parameter does not have a value.  This method is not sensitive to the case
     * of the parameter name.  Use getWmsVersion() to get the requested WMS version.
     */
    public String getString(String paramName)
    {
        return this.paramMap.get(paramName.toLowerCase());
    }
    
    /**
     * Returns the value of the parameter with the given name, throwing a
     * WmsException if the parameter does not exist.  Use getMandatoryWmsVersion()
     * to get the requested WMS version.
     */
    public String getMandatoryString(String paramName) throws WmsException
    {
        String value = this.getString(paramName);
        if (value == null)
        {
            throw new WmsException("Must provide a value for parameter "
                + paramName.toUpperCase());
        }
        return value;
    }
    
    /**
     * Finds the WMS version that the user has requested.  This looks for both
     * WMTVER and VERSION, the latter taking precedence.  WMTVER is used by
     * older versions of WMS and older clients may use this in version negotiation.
     * @return The request WMS version as a string, or null if not set
     */
    public String getWmsVersion()
    {
        String version = this.getString("version");
        if (version == null)
        {
            version = this.getString("wmtver");
        }
        return version; // might be null
    }
    
    /**
     * Finds the WMS version that the user has requested, throwing a WmsException
     * if a version has not been set.
     * @return The request WMS version as a string
     * @throws WmsException if neither VERSION nor WMTVER have been requested
     */
    public String getMandatoryWmsVersion() throws WmsException
    {
        String version = this.getWmsVersion();
        if (version == null)
        {
            throw new WmsException("Must provide a value for VERSION");
        }
        return version;
    }
    
    /**
     * Returns the value of the parameter with the given name as a positive integer,
     * or the provided default if no parameter with the given name has been supplied.
     * Throws a WmsException if the parameter does not exist or if the value
     * is not a valid positive integer.  Zero is counted as a positive integer.
     */
    public int getPositiveInt(String paramName, int defaultValue) throws WmsException
    {
        String value = this.getString(paramName);
        if (value == null) return defaultValue;
        return parsePositiveInt(paramName, value);
    }
    
    /**
     * Returns the value of the parameter with the given name as a positive integer,
     * throwing a WmsException if the parameter does not exist or if the value
     * is not a valid positive integer.  Zero is counted as a positive integer.
     */
    public int getMandatoryPositiveInt(String paramName) throws WmsException
    {
        String value = this.getString(paramName);
        if (value == null)
        {
            throw new WmsException("Must provide a value for parameter "
                + paramName.toUpperCase());
        }
        return parsePositiveInt(paramName, value);
    }
    
    private static int parsePositiveInt(String paramName, String value) throws WmsException
    {
        try
        {
            int i = Integer.parseInt(value);
            if (i < 0)
            {
                throw new WmsException("Parameter " + paramName.toUpperCase() +
                    " must be a valid positive integer");
            }
            return i;
        }
        catch(NumberFormatException nfe)
        {
            throw new WmsException("Parameter " + paramName.toUpperCase() +
                " must be a valid positive integer");
        }
    }
    
    /**
     * Returns the value of the parameter with the given name, or the supplied
     * default value if the parameter does not exist.
     */
    public String getString(String paramName, String defaultValue)
    {
        String value = this.getString(paramName);
        if (value == null)
        {
            return defaultValue;
        }
        return value;
    }
    
    /**
     * Returns the value of the parameter with the given name, or the supplied
     * default value if the parameter does not exist.
     * @throws WmsException if the value of the parameter is not a valid
     * floating-point number
     */
    public float getFloat(String paramName, float defaultValue) throws WmsException
    {
        String value = this.getString(paramName);
        if (value == null)
        {
            return defaultValue;
        }
        try
        {
            return Float.parseFloat(value);
        }
        catch(NumberFormatException nfe)
        {
            throw new WmsException("Parameter " + paramName.toUpperCase() +
                " must be a valid floating-point number");
        }
    }
    
    /**
     * Return a MapStyleData object wrapping the data required for the map style
     * taken from the request params
     * 
     * @return MapStyleData object with the style data from request params
     * 
     * @throws IllegalArgumentException if invalid params have been specified
     * @throws WmsException if problem occurs whilst processing the params
     */
    public MapStyleData getMapStyleData() throws IllegalArgumentException, WmsException
    {
        // RequestParser replaces pluses with spaces: we must change back
        // to parse the format correctly
        String stylesStr = this.getMandatoryString("styles");
        String[] styles;
        String imageFormat;
        boolean transparent;
        Color backgroundColour;
        int opacity; // Opacity of the image in the range [0,100]
        int numColourBands; // Number of colour bands to use in the image
        Boolean logarithmic; // True if we're using a log scale, false if linear and null if not specified
        // These are the data values that correspond with the extremes of the
        // colour scale
        ColorScaleRange colorScaleRange;

        if (stylesStr.trim().equals(""))
    	{
        	styles = new String[0]; 
    	}
        else 
    	{
        	styles = stylesStr.split(",");
    	}
        
        imageFormat = this.getMandatoryString("format").replaceAll(" ", "+");
        
        String trans = this.getString("transparent", "false").toLowerCase();
        if (trans.equals("false")) 
    	{
        	transparent = false;
    	}
        else if (trans.equals("true")) 
    	{
        	transparent = true;
    	}
        else 
    	{
        	throw new IllegalArgumentException(
        			"The value of TRANSPARENT must be \"TRUE\" or \"FALSE\"");
    	}
        
        String bgc = this.getString("bgcolor", "0xFFFFFF");
        if (bgc.length() != 8 || !bgc.startsWith("0x"))
        {
        	throw new IllegalArgumentException(
        			"Background colour is incorrectly specified - must be in Hex string format");
        }
        // Parse the hexadecimal string, ignoring the "0x" prefix
        backgroundColour = new Color(Integer.parseInt(bgc.substring(2), 16));
        
        opacity = this.getPositiveInt("opacity", 100);
        if (opacity > 100) opacity = 100;
        
        colorScaleRange = new ColorScaleRange(this.getString("colorscalerange"));
        numColourBands = this.getPositiveInt("numcolorbands", 254);
        // 254 is the maximum number of colours we can support in a palette.
        // One would be hard pushed to distinguish more colours than this in a
        // typical scenario anyway.
        if (numColourBands > 254) numColourBands = 254;

        String logScaleStr = this.getString("logscale");
        if (logScaleStr == null)
        {
        	logarithmic = null;
        }
        else if (logScaleStr.equalsIgnoreCase("true"))
    	{
        	logarithmic = Boolean.TRUE;
    	}
        else if (logScaleStr.equalsIgnoreCase("false"))
    	{
        	logarithmic = Boolean.FALSE;
    	}
        else
    	{
        	throw new IllegalArgumentException("The value of LOGSCALE must be TRUE or FALSE (or can be omitted");
    	}
        
        return new MapStyleData.Builder().styles(styles).
        	imageFormat(imageFormat).isTransparent(transparent).
        	backgroundColour(backgroundColour).opacity(opacity).
        	numColourBands(numColourBands).isLogarithmic(logarithmic).
        	colorScaleRange(colorScaleRange).build();
    }

	/**
	 * Return a MapLegendData object populated with the relevant data from the
	 * request parameters
	 * 
	 * @return MapLegendData with the legend data from the params
	 * @throws WmsException if either the height or width is a negative number
	 */
	public MapLegendData getMapLegendData() throws WmsException
	{
        String paletteName = this.getString("palette");
        // Find out if we just want the colour bar with no supporting text
        String colorBarOnly = this.getString("colorbaronly", "false");
        int width = this.getPositiveInt("width", 50);
        int height = this.getPositiveInt("height", 200);

		return new MapLegendData(paletteName, Boolean.getBoolean(colorBarOnly),
				height, width);
	}
	
	/**
	 * Return a MapData object populated with the relevant data from the
	 * request parameters
	 * 
	 * @return MapData with the map data from the params
	 * @throws WmsException 
	 */
	public MapData getMapData() throws WmsException
	{
		String[] layerNames = this.getMandatoryString("layers").split(",");
        String crsCode = this.getMandatoryString(this.getMandatoryWmsVersion().equals("1.3.0") ? "crs" : "srs");
        double[] bbox = NcUtils.parseBbox(this.getMandatoryString("bbox"));
        int width = this.getMandatoryPositiveInt("width");
        int height = this.getMandatoryPositiveInt("height");
        String timeString = this.getString("time");
        String elevationString = this.getString("elevation");
        
        return new MapData(layerNames, crsCode, bbox, width, height, timeString, elevationString);
	}
}
