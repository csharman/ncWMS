/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.rdg.resc.ncwms.datareader.sciamachy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Partially implements a {@link SciamachyCatalogue}.  Subclasses must implement
 * {@link #populateCatalogue()}, calling {@link #addDataFile(java.lang.String, org.joda.time.Interval)}
 * for each data file found.
 * @todo implement caching of swaths
 * @author Jon
 */
public abstract class AbstractSciamachyCatalogue implements SciamachyCatalogue {

    private static final Logger logger = LoggerFactory.getLogger(AbstractSciamachyCatalogue.class);

    private static class DataFile {
        private Interval timeRange;
        private String location; // File path or URL to data file
    }

    private List<DataFile> dataFiles = new ArrayList<DataFile>();
    private Interval timeBounds = null;

    /**
     * Implements an LRU in-memory cache
     */
    private static final class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private int maxNumEntries;
        public LRUCache(int maxNumEntries) {
            this.maxNumEntries = maxNumEntries;
        }
        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> entry) {
            return this.size() > this.maxNumEntries;
        }
    }

    /**
     * Holds in memory the most recently-used Swaths
     */
    private Map<String, SciamachySwath> swathCache =
        new LRUCache<String, SciamachySwath>(20);

    /**
     * Initializes the catalogue: simply calls {@link #populateCatalogue()}
     * and logs the result.
     * @throws java.lang.Exception
     */
    public final void init() throws Exception {
        this.populateCatalogue();
        logger.debug("Found {} Sciamachy data files.  Time bounds = {}",
            this.getNumDataFiles(), this.getTimeBounds());
    }

    /**
     * Reads data from the underlying catalogue, calling
     * {@link #addDataFile(java.lang.String, org.joda.time.Interval)} for each
     * data file found.
     */
    protected abstract void populateCatalogue() throws Exception;

    /**
     * Adds a data file to this catalogue
     * @param location Full file path or URL to this file
     * @param timeRange Temporal range of data within this file
     */
    protected final void addDataFile(String location, Interval timeRange) {
        if (this.timeBounds == null) {
            this.timeBounds = timeRange;
        } else {
            if (timeRange.getStart().isBefore(this.timeBounds.getStart())) {
                this.timeBounds = this.timeBounds.withStart(timeRange.getStart());
            }
            if (timeRange.getEnd().isAfter(this.timeBounds.getEnd())) {
                this.timeBounds = this.timeBounds.withEnd(timeRange.getEnd());
            }
        }
        DataFile df = new DataFile();
        df.location = location;
        df.timeRange = timeRange;
        this.dataFiles.add(df);
    }

    public final int getNumDataFiles() {
        return this.dataFiles.size();
    }

    public final Interval getTimeBounds() {
        return this.timeBounds;
    }

    /**
     * Searches the list of available data files for files that contain data
     * within the provided time interval.
     * @param timeInterval
     * @return List of full paths to matching data files
     */
    public final List<String> findDataFiles(Interval timeInterval) {
        logger.debug("Searching for Sciamachy files in time interval {}", timeInterval);
        List<String> dataFilePaths = new ArrayList<String>();
        for (DataFile dataFile : this.dataFiles) {
            if (dataFile.timeRange.overlaps(timeInterval)) {
                logger.debug("File found: {}", dataFile.location);
                dataFilePaths.add(dataFile.location);
            }
        }
        return dataFilePaths;
    }

    /**
     * Gets the swath from the file with the given id.  Uses an in-memory cache
     * of the most recent swaths: therefore this will only read from the
     * file/URL if the data aren't already in the cache.
     * @param dataFileId
     * @return
     * @throws IOException
     */
    public final SciamachySwath getSwath(String dataFileId) throws IOException {
        SciamachySwath swath = this.swathCache.get(dataFileId);
        if (swath == null) {
            swath = this.readSwath(dataFileId);
            logger.debug("Putting swath {} into cache", dataFileId);
            this.swathCache.put(dataFileId, swath);
        } else {
            logger.debug("Getting swath {} from cache", dataFileId);
        }
        return swath;
    }

    protected abstract SciamachySwath readSwath(String dataFileId) throws IOException;

}
