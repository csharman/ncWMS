/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.rdg.resc.ncwms.security;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import javax.net.ssl.HttpsURLConnection;
import uk.ac.rdg.resc.ncwms.config.NcwmsContext;

import java.security.KeyStore;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import java.io.FileInputStream;
import java.net.URL;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 *
 * @author ads
 */
public class GetProxyCert {

    private NcwmsContext ncwmsContext;
    private String keyStorePath = "C:\\Tomcat6.0\\webapps\\userscerts.p12";
    private String keyStorePassword = "test123";
    private String trustStore = "C:\\Tomcat6.0\\webapps\\cacerts";
    private String trustStorePass = "changeit";
    private String webservice_url = "https://user-man.genesi-dr.eu:8443/axis/services/genesidrsecurity";
    private HttpsURLConnection httpsConn = null;
    private String VO = "vostr";
    private String Fquan = "/GENESIDR_GBM";

    public GetProxyCert() {

        try {
            // Load the keystore
            FileInputStream is = new FileInputStream(keyStorePath);
            KeyStore ksKeys = KeyStore.getInstance("pkcs12");
            ksKeys.load(is, keyStorePassword.toCharArray());

            KeyStore ksTrust = KeyStore.getInstance("jks");
            ksTrust.load(new FileInputStream(trustStore), trustStorePass.toCharArray());

            // KeyManager's decide which key material to use.
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ksKeys, keyStorePassword.toCharArray());

            // TrustManager's decide whether to allow connections.
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ksTrust);

            SSLContext sc = SSLContext.getInstance("SSLv3");
            sc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            SSLSocketFactory ssf = sc.getSocketFactory();

            setHttpsConn(ssf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setHttpsConn(SSLSocketFactory ssf) throws Exception {
        URL url = new URL(this.webservice_url);

        this.httpsConn = (HttpsURLConnection) url.openConnection();

        //set the host name verifier
        this.httpsConn.setHostnameVerifier(new HostnameVerifier() {

            //verify the ssl peer
            public boolean verify(String urlHost, SSLSession ssls) {
                if (!urlHost.equals(ssls.getPeerHost())) {
                    System.out.println("Alert: SSL Host" + ssls.getPeerHost() + " does not match URL Host" + urlHost);
                }
                return true;
            }
        });
        this.httpsConn.setSSLSocketFactory(ssf);
    }

    public boolean ContactWebService(String proxyname, String proxypwd) {

        try {
            //get httpsconn
            HttpsURLConnection httpConn = this.httpsConn;

            //build soap request for getVOMSProxy using proxy username and password
            String xVOMSProxy = new String();
            xVOMSProxy = xVOMSProxy.concat("<p86:getVOMSProxy xmlns:p86=\"urn:genesidrsecurity\">");
            xVOMSProxy = xVOMSProxy.concat("<p86:in0>vostr</p86:in0>");
            xVOMSProxy = xVOMSProxy.concat("<p86:in1>/GENESIDR_GBM</p86:in1>");
            xVOMSProxy = xVOMSProxy.concat("<p86:in2>");
            xVOMSProxy = xVOMSProxy.concat(proxyname + "</p86:in2>");
            xVOMSProxy = xVOMSProxy.concat("<p86:in3>");
            xVOMSProxy = xVOMSProxy.concat(proxypwd + "</p86:in3>");
            xVOMSProxy = xVOMSProxy.concat("</p86:getVOMSProxy>");

            String SOAPRequestXML = constuctSOAPRequest(xVOMSProxy);
            byte[] byteAry = SOAPRequestXML.getBytes();

            // Set the appropriate HTTP parameters.
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Host", "user-man.genesi-dr.eu:8443");

            httpConn.setRequestProperty(
                    "Content-Type",
                    "text/xml; charset=utf-8");

            httpConn.setRequestProperty(
                    "Content-Length",
                    String.valueOf(byteAry.length));

            httpConn.setRequestProperty("SOAPAction", "https://user-man.genesi-dr.eu:8443/axis/services/genesidrsecurity");

            httpConn.setDoOutput(true);
            httpConn.setDoInput(true);

            // Send the SOAP XML to the webservice.
            OutputStream out = httpConn.getOutputStream();
            out.write(byteAry);

            //get Response code
            int resCode = httpConn.getResponseCode();

            if (resCode == 200) {
                //getSoapResponse(httpConn.getInputStream());
                saveDataStream(httpConn.getInputStream());
                return true;
            } else {
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void getSoapResponse(InputStream in) throws Exception {

        String line = null;
        BufferedReader reader = null;

        reader = new BufferedReader(
                new InputStreamReader(in));
        OutputStreamWriter rwr = new OutputStreamWriter(System.out);
        //String line;
        StringBuffer htmlBuf = new StringBuffer();
        System.out.println("THE CONTENT");
        System.out.println("-----------");
        while ((line = reader.readLine()) != null) {
            htmlBuf.append(line);
            rwr.write(line + "\n");
            //System.out.println(line);
        }
        reader.close();
        rwr.flush();
        rwr.close();
    }


    /*
     * Construct Soap request XML String.
     */
    private String constuctSOAPRequest(
            String xmldata) {

        String SOAPRequestXML = new String();

        SOAPRequestXML =
                SOAPRequestXML.concat(
                "<?xml version=\"1.0\"?>");
        SOAPRequestXML = SOAPRequestXML.concat("<soap:Envelope ");
        SOAPRequestXML =
                SOAPRequestXML.concat(
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
        SOAPRequestXML =
                SOAPRequestXML.concat(
                "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" ");
        SOAPRequestXML =
                SOAPRequestXML.concat(
                "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">");
        SOAPRequestXML = SOAPRequestXML.concat("<soap:Body>");

        SOAPRequestXML =
                SOAPRequestXML.concat(xmldata);

        SOAPRequestXML = SOAPRequestXML.concat("</soap:Body>");
        SOAPRequestXML = SOAPRequestXML.concat("</soap:Envelope>");

        return SOAPRequestXML;
    }

    public void saveDataStream(InputStream in) {

        try {
            OutputStream out = new BufferedOutputStream(new FileOutputStream("C:\\clean\\response.xml"));
            try {
                byte[] buf = new byte[8 * 1024];  // 8K buffer
                int bytesRead;
                while ((bytesRead = in.read(buf)) != -1) {
                    out.write(buf, 0, bytesRead);
                }
                // Successfully saved the data

            } finally {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.flush();
                    out.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("saveData() done");
    }
}
