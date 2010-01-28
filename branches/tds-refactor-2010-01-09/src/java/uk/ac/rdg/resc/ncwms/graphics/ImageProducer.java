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

package uk.ac.rdg.resc.ncwms.graphics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.rdg.resc.ncwms.controller.GetMapDataRequest;
import uk.ac.rdg.resc.ncwms.controller.GetMapRequest;
import uk.ac.rdg.resc.ncwms.controller.GetMapStyleRequest;
import uk.ac.rdg.resc.ncwms.exceptions.StyleNotDefinedException;
import uk.ac.rdg.resc.ncwms.util.Range;
import uk.ac.rdg.resc.ncwms.util.Ranges;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import uk.ac.rdg.resc.ncwms.wms.VectorLayer;

/**
 * An object that is used to render data into images.
 *
 * @author Jon Blower
 */
public final class ImageProducer
{
    private static final Logger logger = LoggerFactory.getLogger(ImageProducer.class);

    private enum Style {BOXFILL, VECTOR};
    
    private Layer layer;
    private Style style;
    // Width and height of the resulting picture
    private int picWidth;
    private int picHeight;
    private boolean transparent;
    private int opacity;
    private int numColourBands;
    private boolean logarithmic;  // True if the colour scale is to be logarithmic,
                                  // false if linear
    private Color bgColor;
    private ColorPalette colorPalette;
    
    /**
     * Colour scale range of the picture.  An {@link Range#isEmpty() empty Range}
     * means that the picture will be auto-scaled.
     */
    private Range<Float> scaleRange;
    
    /**
     * The length of arrows in pixels, only used for vector plots
     */
    private float arrowLength = 10.0f;
    
    // set of rendered images, ready to be turned into a picture
    private List<BufferedImage> renderedFrames = new ArrayList<BufferedImage>();
    // If we need to cache the frame data and associated labels (we do this if
    // we have to auto-scale the image) this is where we put them.
    // The inner List<Float> is the data for a single vector component
    // The middle List contains the two vector components
    // The outer List contains data for each animation frame
    private List<List<List<Float>>> frameData; // YUCK!!!
    private List<String> labels;
    
    /**
     * Creates a new ImageProducer object from the given request parameters
     * @param getMapRequest Object containing the request parameters
     * @param layer The Layer object which is to be rendered as an image
     * @throws uk.ac.rdg.resc.ncwms.exceptions.StyleNotDefinedException if
     * the requested Style is not supported by the given layer
     */
    public ImageProducer(GetMapRequest getMapRequest, Layer layer)
        throws StyleNotDefinedException
    {
        GetMapStyleRequest styleRequest = getMapRequest.getStyleRequest();
        GetMapDataRequest dataRequest = getMapRequest.getDataRequest();
        this.layer = layer;
        if (styleRequest.getStyles().length == 0)
        {
            // Use the default style and colour palette for this layer
            this.style = layer instanceof VectorLayer ? Style.VECTOR : Style.BOXFILL;
            this.colorPalette = layer.getDefaultColorPalette();
        }
        else
        {
            // The style specification consists of a style type and a colour palette,
            // separated by a forward slash
            String styleSpec = styleRequest.getStyles()[0].trim();
            String[] styleStrEls = styleSpec.split("/");
            String styleType = styleStrEls[0];
            String paletteName = null;
            if (styleStrEls.length > 1)
            {
                paletteName = styleStrEls[1];
            }
            if (styleType.equalsIgnoreCase("boxfill")) this.style = Style.BOXFILL;
            else if (styleType.equalsIgnoreCase("vector")) this.style = Style.VECTOR;
            else throw new StyleNotDefinedException("The style " + styleSpec +
                " is not supported by this server");
            if (this.style == Style.VECTOR && !(layer instanceof VectorLayer))
            {
                throw new StyleNotDefinedException("The style " + styleSpec +
                    " is not supported by this layer");
            }
            // Now get the colour palette
            this.colorPalette = ColorPalette.get(paletteName);
            if (this.colorPalette == null)
            {
                throw new StyleNotDefinedException("There is no palette with the name "
                    + paletteName);
            }
        }
        this.transparent = styleRequest.isTransparent();
        this.bgColor = styleRequest.getBackgroundColour();
        this.opacity = styleRequest.getOpacity();
        this.scaleRange = styleRequest.getColorScaleRange();
        if (this.scaleRange == null)
        {
            // Use the layer's default range
            this.scaleRange = layer.getApproxValueRange();
        }

        // If the client does not specify a scaling, we use the layer's default
        Boolean logRequest = styleRequest.isScaleLogarithmic();
        this.logarithmic = logRequest == null ? layer.isLogScaling() : logRequest.booleanValue();

        this.picWidth = dataRequest.getWidth();
        this.picHeight = dataRequest.getHeight();
        this.numColourBands = styleRequest.getNumColourBands();
    }

    public BufferedImage getLegend()
    {
        return this.colorPalette.createLegend(this.numColourBands, this.layer,
            this.logarithmic, this.scaleRange);
    }
    
    public int getPicWidth()
    {
        return picWidth;
    }
    
    public int getPicHeight()
    {
        return picHeight;
    }
    
    public boolean isTransparent()
    {
        return transparent;
    }
    
    /**
     * Adds a frame of data to this ImageProducer.  If the data cannot yet be rendered
     * into a BufferedImage, the data and label are stored.
     */
    public void addFrame(List<List<Float>> data, String label)
    {
        logger.debug("Adding frame with label {}", label);
        if (this.scaleRange.isEmpty())
        {
            logger.debug("Auto-scaling, so caching frame");
            if (this.frameData == null)
            {
                this.frameData = new ArrayList<List<List<Float>>>();
                this.labels = new ArrayList<String>();
            }
            this.frameData.add(data);
            this.labels.add(label);
        }
        else
        {
            logger.debug("Scale is set, so rendering image");
            this.renderedFrames.add(this.createImage(data, label));
        }
    }
    
    /**
     * Creates and returns a single frame as an Image, based on the given data.
     * Adds the label if one has been set.  The scale must be set before
     * calling this method.
     */
    private BufferedImage createImage(List<List<Float>> data, String label)
    {
        // Create the pixel array for the frame
        byte[] pixels = new byte[this.picWidth * this.picHeight];
        // We get the magnitude of the input data (takes care of the case
        // in which the data are two components of a vector)
        List<Float> magnitudes = getMagnitude(data);
        for (int i = 0; i < pixels.length; i++)
        {
            pixels[i] = (byte)this.getColourIndex(magnitudes.get(i));
        }
        
        // Create a ColorModel for the image
        ColorModel colorModel = this.colorPalette.getColorModel(this.numColourBands,
            this.opacity, this.bgColor, this.transparent);
        
        // Create the Image
        DataBuffer buf = new DataBufferByte(pixels, pixels.length);
        SampleModel sampleModel = colorModel.createCompatibleSampleModel(this.picWidth, this.picHeight);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, buf, null);
        BufferedImage image = new BufferedImage(colorModel, raster, false, null);
        
        // Add the label to the image
        // TODO: colour needs to change with different palettes!
        if (label != null && !label.equals(""))
        {
            Graphics2D gfx = (Graphics2D)image.getGraphics();
            gfx.setPaint(new Color(0, 0, 143));
            gfx.fillRect(1, image.getHeight() - 19, image.getWidth() - 1, 18);
            gfx.setPaint(new Color(255, 151, 0));
            gfx.drawString(label, 10, image.getHeight() - 5);
        }
        
        if (this.style == Style.VECTOR)
        {
            // We superimpose direction arrows on top of the background
            // TODO: only do this for lat-lon projections!
            Graphics2D g = image.createGraphics();
            // TODO: control the colour of the arrows with an attribute
            // Must be part of the colour palette (here we use the colour
            // for out-of-range values)
            g.setColor(Color.BLACK);

            logger.debug("Drawing vectors, length = {} pixels", this.arrowLength);
            List<Float> east = data.get(0);
            List<Float> north = data.get(1);
            for (int i = 0; i < this.picWidth; i += Math.ceil(this.arrowLength * 1.2))
            {
                for (int j = 0; j < this.picHeight; j += Math.ceil(this.arrowLength * 1.2))
                {
                    int dataIndex = j * this.picWidth + i;
                    Float eastVal = east.get(dataIndex);
                    Float northVal = north.get(dataIndex);
                    if (eastVal != null && northVal != null)
                    {
                        double angle = Math.atan2(northVal.doubleValue(), eastVal.doubleValue());
                        // Calculate the end point of the arrow
                        double iEnd = i + this.arrowLength * Math.cos(angle);
                        // Screen coordinates go down, but north is up, hence the minus sign
                        double jEnd = j - this.arrowLength * Math.sin(angle);
                        //logger.debug("i={}, j={}, dataIndex={}, east={}, north={}",
                        //    new Object[]{i, j, dataIndex, data[0][dataIndex], data[1][dataIndex]});
                        // Draw a dot representing the data location
                        g.fillOval(i - 2, j - 2, 4, 4);
                        // Draw a line representing the vector direction and magnitude
                        g.setStroke(new BasicStroke(1));
                        g.drawLine(i, j, (int)Math.round(iEnd), (int)Math.round(jEnd));
                        // Draw the arrow on the canvas
                        //drawArrow(g, i, j, (int)Math.round(iEnd), (int)Math.round(jEnd), 2);
                    }
                }
            }
        }
        
        return image;
    }
    
    /**
     * If the input data are the two components of a vector, this
     * calculates the magnitude of these components and returns
     * the array of magnitudes as a new array.  If the input data
     * contains one array only, this array is simply returned.
     */
    private static List<Float> getMagnitude(List<List<Float>> data)
    {
        logger.debug("Calculating the magnitude of {} components", data.size());
        List<Float> firstComponent = data.get(0);
        if (data.size() == 1)
        {
            return firstComponent;
        }
        List<Float> magnitudes = new ArrayList<Float>(firstComponent.size());
        for (int i = 0; i < firstComponent.size(); i++)
        {
            if (firstComponent.get(i) == null)
            {
                magnitudes.add((Float)null);
            }
            else
            {
                double sumsq = firstComponent.get(i) * firstComponent.get(i);
                for (int j = 1; j < data.size(); j++)
                {
                    sumsq += data.get(j).get(i) * data.get(j).get(i);
                }
                magnitudes.add((float)Math.sqrt(sumsq));
            }
        }
        return magnitudes;
    }
    
    /**
     * @return the colour index that corresponds to the given value
     */
    private int getColourIndex(Float value)
    {
        if (value == null)
        {
            return this.numColourBands; // represents a background pixel
        }
        else if (!this.scaleRange.contains(value))
        {
            return this.numColourBands + 1; // represents an out-of-range pixel
        }
        else
        {
            float scaleMin = this.scaleRange.getMinimum().floatValue();
            float scaleMax = this.scaleRange.getMaximum().floatValue();
            double min = this.logarithmic ? Math.log(scaleMin) : scaleMin;
            double max = this.logarithmic ? Math.log(scaleMax) : scaleMax;
            double val = this.logarithmic ? Math.log(value) : value;
            double frac = (val - min) / (max - min);
            // Compute and return the index of the corresponding colour
            return (int)(frac * this.numColourBands);
        }
    }
    
    /**
     * Gets the frames as BufferedImages, ready to be turned into a picture or
     * animation.  This is called just before the picture is due to be created,
     * so subclasses can delay creating the BufferedImages until all the data
     * has been extracted (for example, if we are auto-scaling an animation,
     * we can't create each individual frame until we have data for all the frames)
     * @return List of BufferedImages
     */
    public List<BufferedImage> getRenderedFrames()
    {
        this.setScale(); // Make sure the colour scale is set before proceeding
        // We render the frames if we have not done so already
        if (this.frameData != null)
        {
            logger.debug("Rendering image frames...");
            for (int i = 0; i < this.frameData.size(); i++)
            {
                logger.debug("    ... rendering frame {}", i);
                this.renderedFrames.add(this.createImage(this.frameData.get(i), this.labels.get(i)));
            }
        }
        return this.renderedFrames;
    }
    
    /**
     * Makes sure that the scale is set: if we are auto-scaling, this reads all
     * of the data we have stored to find the extremes.  If the scale has
     * already been set, this does nothing.
     */
    private void setScale()
    {
        if (this.scaleRange.isEmpty())
        {
            Float scaleMin = null;
            Float scaleMax = null;
            logger.debug("Setting the scale automatically");
            // We have a cache of image data, which we use to generate the colour scale
            for (List<List<Float>> data : this.frameData)
            {
                // We only use the first component if this is a vector quantity
                Range<Float> range = Ranges.findMinMax(data.get(0));
                // TODO: could move this logic to the Range/Ranges class
                if (!range.isEmpty())
                {
                    if (scaleMin == null || range.getMinimum().compareTo(scaleMin) < 0)
                    {
                        scaleMin = range.getMinimum();
                    }
                    if (scaleMax == null || range.getMaximum().compareTo(scaleMax) > 0)
                    {
                        scaleMax = range.getMaximum();
                    }
                }
            }
            this.scaleRange = Ranges.newRange(scaleMin, scaleMax);
        }
    }

    public int getOpacity()
    {
        return opacity;
    }
}
