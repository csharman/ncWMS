/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.rdg.resc.ncwms.datareader;

import java.util.Date;

/**
 *
 * @author dcrossma
 */
public class GriddedDataElement {

    private Double value;
    private java.util.Date date;
    private double lat;
    private double lon;
    private int pixelIndex;

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public int getPixelIndex() {
        return pixelIndex;
    }

    public void setPixelIndex(int pixelIndex) {
        this.pixelIndex = pixelIndex;
    }
}
