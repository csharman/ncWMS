/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.rdg.resc.ncwms.datareader.sciamachy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.rdg.resc.ncwms.datareader.sciamachy.SciamachySwath.Retrieval;

/**
 * A {@link SciamachyCatalogue} that provides access to local files
 * @author Jon
 */
public class LocalSciamachyCatalogue extends AbstractSciamachyCatalogue {

    private static final Logger logger = LoggerFactory.getLogger(LocalSciamachyCatalogue.class);

    // These properties will be set by Spring
    private String dataLocation;
    private String fileExtension;

    /** Scans the files on disk and populates the catalogue */
    public void populateCatalogue() throws IOException {

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

        for (File f : dataDir.listFiles(filter)) {
            SciamachySwath swath = SciamachySwath.fromFile(f.getPath());
            List<Retrieval> retrievals = swath.getRetrievals();
            Interval timeRange = new Interval(
                retrievals.get(0).getDateTime(),
                retrievals.get(retrievals.size() - 1).getDateTime().plusMillis(1)
            );
            this.addDataFile(f.getPath(), timeRange);
        }
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
