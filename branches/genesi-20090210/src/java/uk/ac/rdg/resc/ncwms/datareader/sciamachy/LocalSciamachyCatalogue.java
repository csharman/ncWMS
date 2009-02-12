/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.rdg.resc.ncwms.datareader.sciamachy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.rdg.resc.ncwms.datareader.sciamachy.SciamachySwath.Retrieval;

/**
 * A {@link SciamachyCatalogue} that provides access to local files
 * @author Jon
 */
public class LocalSciamachyCatalogue implements SciamachyCatalogue {

    private static final Logger logger = LoggerFactory.getLogger(LocalSciamachyCatalogue.class);

    // These properties will be set by Spring
    private String dataLocation;
    private String fileExtension;

    private static class DataFile {
        private Interval timeRange;
        private File file;
    }

    private List<DataFile> dataFiles;
    private Interval timeBounds;

    /** Scans the files on disk and populates the catalogue */
    public void init() throws IOException {
        File dataDir = new File(dataLocation);
        if (!dataDir.exists() || !dataDir.isDirectory()) {
            throw new FileNotFoundException(dataLocation + " is not a valid directory");
        }
        logger.debug("Scanning files in {}", dataDir);

        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith("." + fileExtension);
            }
        };

        this.dataFiles = new ArrayList<DataFile>();
        DateTime first = null;
        DateTime last = null;
        for (File f : dataDir.listFiles(filter)) {
            SciamachySwath swath = SciamachySwath.fromFile(f.getPath());
            List<Retrieval> retrievals = swath.getRetrievals();
            DataFile df = new DataFile();
            df.file = f;
            // The interval is exclusive of the end time so we add 1ms to the end
            // time so that it includes all retrievals.
            df.timeRange = new Interval(
                retrievals.get(0).getDateTime(),
                retrievals.get(retrievals.size() - 1).getDateTime().plusMillis(1)
            );
            if (first == null || df.timeRange.getStart().isBefore(first)) {
                first = df.timeRange.getStart();
            }
            if (last == null || df.timeRange.getEnd().isAfter(last)) {
                last = df.timeRange.getEnd();
            }
            dataFiles.add(df);
        }
        this.timeBounds = new Interval(first, last);
        logger.debug("Found {} Sciamachy data files.  Time bounds = {}", dataFiles.size(), this.timeBounds);
    }

    public Interval getTimeBounds() {
        return this.timeBounds;
    }

    /**
     * Searches the list of available data files for files that contain data
     * within the provided time interval.
     * @param timeInterval
     * @return List of full paths to matching data files
     */
    public List<String> findDataFiles(Interval timeInterval) {
        logger.debug("Searching for Sciamachy files in time interval {}", timeInterval);
        List<String> dataFilePaths = new ArrayList<String>();
        for (DataFile dataFile : this.dataFiles) {
            if (dataFile.timeRange.overlaps(timeInterval)) {
                logger.debug("File found: {}", dataFile.file);
                dataFilePaths.add(dataFile.file.getPath());
            }
        }
        return dataFilePaths;
    }

    /**
     * Returns an {@link SciamachySwath} from the file with the given id,
     * which has been returned by {@link #getDataFiles(org.joda.time.Interval)}.
     * @throws IOException if there was a problem opening the stream
     */
    public SciamachySwath getSwath(String dataFileId) throws IOException {
        return SciamachySwath.fromFile(dataFileId);
    }

    public void setDataLocation(String dataLocation) {
        this.dataLocation = dataLocation;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

}
