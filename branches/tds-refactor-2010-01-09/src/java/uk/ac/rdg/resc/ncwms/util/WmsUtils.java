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

package uk.ac.rdg.resc.ncwms.util;

import java.io.File;
import java.io.FilenameFilter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.oro.io.GlobFilenameFilter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;

/**
 * <p>Collection of static utility methods that are useful in the WMS application.</p>
 *
 * <p>Through the taglib definition /WEB-INF/taglib/wmsUtils.tld, these functions
 * are also available as JSP2.0 functions. For example:</p>
 * <code>
 * <%@taglib uri="/WEB-INF/taglib/wmsUtils" prefix="utils"%>
 * The epoch: ${utils:secondsToISO8601(0)}
 * </code>
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class WmsUtils
{
    /**
     * The versions of the WMS standard that this server supports
     */
    public static final Set<String> SUPPORTED_VERSIONS = new HashSet<String>();

    private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER =
        ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC);

    private static final DateTimeFormatter ISO_DATE_TIME_PARSER =
        ISODateTimeFormat.dateTimeParser().withZone(DateTimeZone.UTC);

    private static final DateTimeFormatter ISO_TIME_FORMATTER =
        ISODateTimeFormat.time().withZone(DateTimeZone.UTC);

    /**
     * <p>A {@link Comparator} that compares {@link DateTime} objects based only
     * on their millisecond instant values.  This can be used for
     * {@link Collections#sort(java.util.List, java.util.Comparator) sorting} or
     * {@link Collections#binarySearch(java.util.List, java.lang.Object,
     * java.util.Comparator) searching} {@link List}s of {@link DateTime} objects.</p>
     * <p>The ordering defined by this Comparator is <i>inconsistent with equals</i>
     * because it ignores the Chronology of the DateTime instants.</p>
     * <p><i>(Note: The DateTime object inherits from Comparable, not
     * Comparable&lt;DateTime&gt;, so we can't use the methods in Collections
     * directly.  However we can reuse the {@link DateTime#compareTo(java.lang.Object)}
     * method.)</i></p>
     */
    public static final Comparator<DateTime> DATE_TIME_COMPARATOR =
        new Comparator<DateTime>()
    {
        @Override
        public int compare(DateTime dt1, DateTime dt2) {
            return dt1.compareTo(dt2);
        }
    };
    
    static
    {
        SUPPORTED_VERSIONS.add("1.1.1");
        SUPPORTED_VERSIONS.add("1.3.0");
    }

    /** Private constructor to prevent direct instantiation */
    private WmsUtils() { throw new AssertionError(); }

    /**
     * Converts a {@link DateTime} object into an ISO8601-formatted String.
     */
    public static String dateTimeToISO8601(DateTime dateTime)
    {
        return ISO_DATE_TIME_FORMATTER.print(dateTime);
    }

    /**
     * Converts an ISO8601-formatted String into a {@link DateTime} object
     * @throws IllegalArgumentException if the string is not a valid ISO date-time
     */
    public static DateTime iso8601ToDateTime(String isoDateTime)
    {
        return ISO_DATE_TIME_PARSER.parseDateTime(isoDateTime);
    }
    
    /**
     * Formats a DateTime as the time only
     * in the format "HH:mm:ss", e.g. "14:53:03".  Time zone offset is zero (UTC).
     */
    public static String formatUTCTimeOnly(DateTime dateTime)
    {
        return ISO_TIME_FORMATTER.print(dateTime);
    }

    /**
     * Searches the given list of timesteps for the specified date-time using the binary
     * search algorithm.  Matches are found based only upon the millisecond
     * instant of the target DateTime, not its Chronology.
     * @param  target The timestep to search for.
     * @return the index of the search key, if it is contained in the list;
     *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
     *	       <i>insertion point</i> is defined as the point at which the
     *	       key would be inserted into the list: the index of the first
     *	       element greater than the key, or <tt>list.size()</tt> if all
     *	       elements in the list are less than the specified key.  Note
     *	       that this guarantees that the return value will be &gt;= 0 if
     *	       and only if the key is found.  If this Layer does not have a time
     *         axis this method will return -1.
     */
    public static int findTimeIndex(List<DateTime> dtList, DateTime target)
    {
        return Collections.binarySearch(dtList, target, DATE_TIME_COMPARATOR);
    }
    
    /**
     * Creates a directory, throwing an Exception if it could not be created and
     * it does not already exist.
     */
    public static void createDirectory(File dir) throws Exception
    {
        if (dir.exists())
        {
            if (dir.isDirectory())
            {
                return;
            }
            else
            {
                throw new Exception(dir.getPath() + 
                    " already exists but it is a regular file");
            }
        }
        else
        {
            boolean created = dir.mkdirs();
            if (!created)
            {
                throw new Exception("Could not create directory "
                    + dir.getPath());
            }
        }
    }
    
    /**
     * Creates a unique name for a Layer (for display in the Capabilities
     * document) based on a dataset ID and a Layer ID that is unique within a
     * dataset.  Matches up with {@link #parseUniqueLayerName(java.lang.String)}.
     */
    public static String createUniqueLayerName(String datasetId, String layerId)
    {
        return datasetId + "/" + layerId;
    }
    
    /**
     * Parses a unique layer name and returns a two-element String array containing
     * the dataset id (first element) and the layer id (second element).  Matches
     * up with {@link #createUniqueLayerName(java.lang.String, java.lang.String)}.
     * This method does not check for the existence or otherwise of the dataset
     * or layer.
     * @throws ParseException if the provided layer name is not in the correct
     * format.
     */
    public static String[] parseUniqueLayerName(String uniqueLayerName)
        throws ParseException
    {
        String[] els = new String[2];
        
        int slashIndex = uniqueLayerName.lastIndexOf("/");
        if(slashIndex > 0)
        {
            els[0] = uniqueLayerName.substring(0, slashIndex);
            els[1] = uniqueLayerName.substring(slashIndex + 1);
            return els;
        }
        else
        {
            // We don't bother looking for the position in the string where the
            // parse error occurs
            throw new ParseException(uniqueLayerName + " is not in the correct format", -1);
        }
    }
    
    /**
     * Converts a string of the form "x1,y1,x2,y2" into a bounding box of four
     * doubles.
     * @throws WmsException if the format of the bounding box is invalid
     */
    public static double[] parseBbox(String bboxStr) throws WmsException
    {
        String[] bboxEls = bboxStr.split(",");
        // Check the validity of the bounding box
        if (bboxEls.length != 4)
        {
            throw new WmsException("Invalid bounding box format: need four elements");
        }
        double[] bbox = new double[4];
        try
        {
            for (int i = 0; i < bbox.length; i++)
            {
                bbox[i] = Double.parseDouble(bboxEls[i]);
            }
        }
        catch(NumberFormatException nfe)
        {
            throw new WmsException("Invalid bounding box format: all elements must be numeric");
        }
        if (bbox[0] >= bbox[2] || bbox[1] >= bbox[3])
        {
            throw new WmsException("Invalid bounding box format");
        }
        return bbox;
    }

    /**
     * Calculates the magnitude of the vector components given in the provided
     * Lists.  The two lists must be of the same length.  For any element in the
     * component lists, if either east or north is null, the magnitude will also
     * be null.
     * @return a List of the magnitudes calculated from the components.
     */
    public static List<Float> getMagnitudes(List<Float> eastData, List<Float> northData)
    {
        if (eastData == null || northData == null) throw new NullPointerException();
        if (eastData.size() != northData.size())
        {
            throw new IllegalArgumentException("east and north data components must be the same length");
        }
        List<Float> mag = new ArrayList<Float>(eastData.size());
        for (int i = 0; i < eastData.size(); i++)
        {
            Float east = eastData.get(i);
            Float north = northData.get(i);
            Float val = null;
            if (east != null && north != null)
            {
                val = (float)Math.sqrt(east * east + north * north);
            }
            mag.add(val);
        }
        if (mag.size() != eastData.size()) throw new AssertionError();
        return mag;
    }
    
    /**
     * @return true if the given location represents an OPeNDAP dataset.
     * This method simply checks to see if the location string starts with "http://",
     * "https://" or "dods://".
     */
    public static boolean isOpendapLocation(String location)
    {
        return location.startsWith("http://") || location.startsWith("dods://")
            || location.startsWith("https://");
    }
    
    /**
     * @return true if the given location represents an NcML aggregation. dataset.
     * This method simply checks to see if the location string ends with ".xml"
     * or ".ncml", following the same procedure as the Java NetCDF library.
     */
    public static boolean isNcmlAggregation(String location)
    {
        return location.endsWith(".xml") || location.endsWith(".ncml");
    }

    /**
     * Expands a glob expression to give a List of absolute paths to files.  This
     * method recursively searches directories, allowing for glob expressions like
     * {@code "c:\\data\\200[6-7]\\*\\1*\\A*.nc"}.
     * @return a a List of absolute paths to files matching the given glob
     * expression
     * @throws IllegalArgumentException if the glob expression does not represent
     * an absolute path (according to {@code new File(globExpression).isAbsolute()}).
     * @author Mike Grant, Plymouth Marine Labs; Jon Blower
     */
    public static List<File> expandGlobExpression(String globExpression)
    {
        // Check that the glob expression represents an absolute path.  Relative
        // paths would cause unpredictable and platform-dependent behaviour so
        // we disallow them.
        File globFile = new File(globExpression);
        if (!globFile.isAbsolute())
        {
            throw new IllegalArgumentException("Dataset location must be an absolute path");
        }

        // Break glob pattern into path components.  To do this in a reliable
        // and platform-independent way we use methods of the File class, rather
        // than String.split().
        List<String> pathComponents = new ArrayList<String>();
        while (globFile != null)
        {
            // We "pop off" the last component of the glob pattern and place
            // it in the first component of the pathComponents List.  We therefore
            // ensure that the pathComponents end up in the right order.
            File parent = globFile.getParentFile();
            // For a top-level directory, getName() returns an empty string,
            // hence we use getPath() in this case
            String pathComponent = parent == null ? globFile.getPath() : globFile.getName();
            pathComponents.add(0, pathComponent);
            globFile = parent;
        }

        // We must have at least two path components: one directory and one
        // filename or glob expression
        List<File> searchPaths = new ArrayList<File>();
        searchPaths.add(new File(pathComponents.get(0)));
        int i = 1; // Index of the glob path component

        while(i < pathComponents.size())
        {
            FilenameFilter globFilter = new GlobFilenameFilter(pathComponents.get(i));
            List<File> newSearchPaths = new ArrayList<File>();
            // Look for matches in all the current search paths
            for (File dir : searchPaths)
            {
                if (dir.isDirectory())
                {
                    // Workaround for automounters that don't make filesystems
                    // appear unless they're poked
                    // do a listing on searchpath/pathcomponent whether or not
                    // it exists, then discard the results
                    new File(dir, pathComponents.get(i)).list();

                    for (File match : dir.listFiles(globFilter))
                    {
                        newSearchPaths.add(match);
                    }
                }
            }
            // Next time we'll search based on these new matches and will use
            // the next globComponent
            searchPaths = newSearchPaths;
            i++;
        }

        // Now we've done all our searching, we'll only retain the files from
        // the list of search paths
        List<File> files = new ArrayList<File>();
        for (File path : searchPaths)
        {
            if (path.isFile()) files.add(path);
        }
        return files;
    }
    
}
