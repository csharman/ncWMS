/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.rdg.resc.ncwms.datareader;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import uk.ac.rdg.resc.ncwms.metadata.Layer;
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
    public List<String> getDatesForDataset(Layer layer) {

        List<String> calendarList = new ArrayList<String>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd+HH:mm");
        System.out.println("Layer "+layer.getLayerName());
        List<TimestepInfo> timesteps = layer.getTimesteps();
        for (TimestepInfo timestep : timesteps) {
            calendarList.add(sdf.format(timestep.getDate()));
        }
        Collections.sort(calendarList);
        return calendarList;
    }
}
