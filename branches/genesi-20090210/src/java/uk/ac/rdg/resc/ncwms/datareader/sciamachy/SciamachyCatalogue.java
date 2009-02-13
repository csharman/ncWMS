/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.rdg.resc.ncwms.datareader.sciamachy;

import java.io.IOException;
import java.util.List;
import org.joda.time.Interval;

/**
 * A catalogue of Sciamachy data files
 * @author Jon
 */
public interface SciamachyCatalogue {

    /** Gets the overall time interval spanning the data */
    public Interval getTimeBounds();

    /** Gets the number of data files in the catalogue */
    public int getNumDataFiles();

    /**
     * Gets the list of data file identifiers (file paths or URLs) that contain
     * data that overlap the given time interval
     */
    public List<String> findDataFiles(Interval timeInterval);

    /**
     * Returns an {@link SciamachySwath} from the file with the given id,
     * which has been returned by {@link #getDataFiles(org.joda.time.Interval)}.
     * @throws IOException if there was a problem opening the stream
     */
    public SciamachySwath getSwath(String dataFileId) throws IOException;
}
