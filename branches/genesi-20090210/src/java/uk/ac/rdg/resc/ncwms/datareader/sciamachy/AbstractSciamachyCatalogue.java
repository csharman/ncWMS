/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.rdg.resc.ncwms.datareader.sciamachy;

import java.util.ArrayList;
import java.util.List;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Partially implements a {@link SciamachyCatalogue}.  Subclasses must implement
 * {@link #populateCatalogue()}, calling {@link #addDataFile(java.lang.String, org.joda.time.Interval)}
 * for each data file found.
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

}
