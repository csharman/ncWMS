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

package uk.ac.rdg.resc.ncwms.coordsys;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.rdg.resc.ncwms.coordsys.CurvilinearGrid.Cell;

/**
 * Generates look-up tables by "painting" cells from a {@link CurvilinearGrid}
 * onto {@link BufferedImage}s.
 * @author Jon
 */
final class BufferedImageLutGenerator
{
    private static final Logger logger = LoggerFactory.getLogger(BufferedImageLutGenerator.class);

    /**
     * Populates the given look-up table, using the given curvilinear grid.
     * @param lut
     * @param grid
     */
    public static synchronized void populateLut(LookUpTable lut, CurvilinearGrid grid)
    {
        // Create BufferedImages for temporarily holding the LUT information
        BufferedImage im = createBufferedImage(lut);
        
        // Get the graphics contexts
        Graphics2D g2d = im.createGraphics();

        // Apply a transform so that we can paint in lat-lon coordinates
        g2d.setTransform(lut.getTransform());

        // Populate the BufferedImages using the information from the curvilinear grid
        // Iterate over all the cells in the grid, painting the i indices of the
        // cell onto the BufferedImage
        for (Cell cell : grid)
        {
            paintCell(g2d, cell, cell.getI());
        }

        // Copy the information from the BufferedImages to the LookUpTable.
        for (int y = 0; y < im.getHeight(); y++) {
            for (int x = 0; x < im.getWidth(); x++) {
                int iIndex = im.getRGB(x, y);
                lut.setIIndex(x, y, iIndex);
            }
        }

        // Now reset the image and do the same for the j indices
        resetImage(im);
        for (Cell cell : grid)
        {
            paintCell(g2d, cell, cell.getJ());
        }
        for (int y = 0; y < im.getHeight(); y++) {
            for (int x = 0; x < im.getWidth(); x++) {
                int jIndex = im.getRGB(x, y);
                lut.setJIndex(x, y, jIndex);
            }
        }

        // Free the resources from the graphics contexts
        g2d.dispose();
    }

    /**
     * Creates a {@link BufferedImage} whose "colors" will be set to the i or
     * j indices of the look-up table.  Initializes all pixels in the image
     * to -1.
     */
    private static final BufferedImage createBufferedImage(LookUpTable lut) {
        int width = lut.getNumLonPoints();
        int height = lut.getNumLatPoints();
        // This color model matches the one assumed by BufferedImage.get/setRGB()
        BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        resetImage(im);
        logger.debug("Created BufferedImage of size {},{}", im.getWidth(), im.getHeight());
        return im;
    }
    
    /**
     * Resets all the pixels in the given image to -1.
     * @param im
     */
    private static void resetImage(BufferedImage im)
    {
        for (int y = 0; y < im.getHeight(); y++) {
            for (int x = 0; x < im.getWidth(); x++) {
                im.setRGB(x, y, -1);
            }
        }
    }

    private static void paintCell(Graphics2D g2d, Cell cell, int color)
    {
        // Get a Path representing the boundary of the cell
        Path2D path = cell.getBoundaryPath();
        // Paint the path onto the BufferedImages as polygons
        // Use the i and j indices of the cell as the colours
        g2d.setPaint(new Color(color));
        g2d.fill(path);

        // We paint a second copy of the cell, shifted by 360 degrees, to handle
        // the anti-meridian
        double shiftLon = cell.getCentre().getLongitude() > 0.0
            ? -360.0
            : 360.0;
        path.transform(AffineTransform.getTranslateInstance(shiftLon, 0.0));
        g2d.fill(path);
    }

}
