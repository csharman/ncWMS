/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.rdg.resc.ncwms.datareader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.rdg.resc.ncwms.config.Config;
import uk.ac.rdg.resc.ncwms.config.Dataset;
import uk.ac.rdg.resc.ncwms.config.NcwmsContext;
import uk.ac.rdg.resc.ncwms.metadata.InMemoryMetadataStore;
import uk.ac.rdg.resc.ncwms.metadata.Layer;
import uk.ac.rdg.resc.ncwms.metadata.LayerImpl;
import uk.ac.rdg.resc.ncwms.metadata.MetadataStore;
import uk.ac.rdg.resc.ncwms.metadata.TimestepInfo;

/**
 *
 * @author dcrossma
 */
public class CalendarDataReader {

    /**
     * Method getDatesForDataset ...
     *
     * @param givenLayer of type String
     * @return List<Date>
     */
    public List<Date> getDatesForDataset(String givenLayer) {

        List<Date> calendarList = new ArrayList<Date>();

        Config config = null;
        NcwmsContext context = new NcwmsContext();
        MetadataStore store = new InMemoryMetadataStore();

        store.setNcwmsContext(context);
        try {
            store.init();
            config = Config.readConfig(context, store);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Map<String, Dataset> datasets = config.getDatasets();

        Map<String, LayerImpl> layers = new HashMap<String, LayerImpl>();
        for (Map.Entry<String, Dataset> entry : datasets.entrySet()) {
            Dataset set = entry.getValue();

            DefaultDataReader reader = new DefaultDataReader();


            try {
                layers = reader.getAllLayers(set);
                System.out.println("number of layers is "+layers.size());
            } catch (Exception ex) {
                Logger.getLogger(CalendarDataReader.class.getName()).log(Level.SEVERE, null, ex);
            }


            for (Map.Entry<String, LayerImpl> entryLayers : layers.entrySet()) {
                Layer currentLayer = entryLayers.getValue();
                System.out.println("given layer *"+givenLayer+"*");
                System.out.println("*"+set.getId() + "/" + currentLayer.getId()+"*");
                if (givenLayer.equals(set.getId() + "/" + currentLayer.getId())) {
                    List<TimestepInfo> timesteps = currentLayer.getTimesteps();

                    for (TimestepInfo timestep : timesteps) {
                        calendarList.add(timestep.getDate());
                    }
                    Collections.sort(calendarList);
                }
            }
        }
        return calendarList;
    }
}
