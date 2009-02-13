/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.rdg.resc.ncwms.datareader.sciamachy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides access to Sciamachy files on the GENESI-DR system.
 * @author Jon
 */
public class GenesiSciamachyCatalogue extends AbstractSciamachyCatalogue {

    private static final Logger logger = LoggerFactory.getLogger(GenesiSciamachyCatalogue.class);

    /**
     * This URL returns an XML document containing the list of all the
     * Sciamachy files.
     * @todo need to discover this URL from the GENESI-DR central site, but
     * can't figure out how for now.
     */
    private static final String CATALOGUE_URL =
        "http://dr-site.esrin.esa.int/genesi/envisat_sciamachy/sci_ol__2pt/xml/";
    
    /**
     * This parses date/times in the GENESI catalogue XML.
     */
    private static final DateTimeFormatter DATE_TIME_PARSER = DateTimeFormat
        .forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
        .withZone(DateTimeZone.UTC);

    /**
     * Connects to GENESI-DR and discovers the list of data files.
     */
    public void populateCatalogue() throws DocumentException {
        // Read the catalogue XML using dom4j
        // TODO: replace with CATALOGUE_URL
        //File f = new File("C:\\documents and settings\\jon\\desktop\\scia.xml");
        logger.debug("Searching for SCIAMACHY datasets in {}", CATALOGUE_URL);
        Document doc = new SAXReader().read(CATALOGUE_URL);

        // We know this is a List<Node>. Dom4j doesn't yet use generics.
        List<Node> datasets = (List<Node>)doc.selectNodes("//dclite4g:DataSet");
        logger.debug("Found {} matching datasets in GENESI-DR catalogue", datasets.size());

        for (Node node : datasets) {
            // Find the HTTP URL to this dataset
            String urlStr = node.selectSingleNode("dclite4g:onlineResource/ws:HTTP/@rdf:about").getText();
            // The catalogue incorrectly returns https:// urls instead of http:// ones
            // TODO
            urlStr = urlStr.replace("https://", "http://");

            // Find the start and end times of this dataset
            String startTimeStr = node.selectSingleNode("ical:dtstart").getText();
            String endTimeStr = node.selectSingleNode("ical:dtend").getText();
            DateTime startDateTime = DATE_TIME_PARSER.parseDateTime(startTimeStr);
            DateTime endDateTime = DATE_TIME_PARSER.parseDateTime(endTimeStr);

            this.addDataFile(urlStr, new Interval(startDateTime, endDateTime));
        }
    }

    /**
     * Reads a {@link SciamachySwath} from the given URL to the data file.
     * @param dataFileUrl
     * @return
     * @throws java.io.IOException
     */
    public SciamachySwath getSwath(String dataFileUrl) throws IOException {
        logger.debug("Reading Sciamachy data from {}", dataFileUrl);
        try {
            InputStream in = new URL(dataFileUrl).openStream();
            SciamachySwath swath = SciamachySwath.fromInputStream(in);
            logger.debug("Successfully read Sciamachy data from {}", dataFileUrl);
            return swath;
        } catch (IOException ioe) {
            throw ioe;
        }
    }

    public static final void main(String[] args) throws Exception {
        new GenesiSciamachyCatalogue().populateCatalogue();
    }

}
