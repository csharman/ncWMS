/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.rdg.resc.ncwms.security;

import java.io.Serializable;


/**
 *
 * @author ads
 */
public class CertManager implements Serializable {

    private String certLocation;
    
    public CertManager(){
    }

    public String getLocation(){
        return certLocation;
    }

    public void setLocation(String loc){
        certLocation = loc;
    }
}
