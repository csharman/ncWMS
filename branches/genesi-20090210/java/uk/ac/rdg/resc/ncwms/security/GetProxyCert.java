/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.rdg.resc.ncwms.security;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.net.ssl.HttpsURLConnection;


import java.io.File;
import java.io.FileWriter;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;

/**
 *
 * @author ads
 */
public class GetProxyCert {

    private String VO = "vostr";
    private String Fquan = "/GENESIDR_GBM";
    private HttpsURLConnection httpConn;
    private String proxycertlocation;

    public GetProxyCert(HttpsURLConnection urlConn) {

        try {
            this.httpConn = urlConn;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getProxyCertLocation() {
        return this.proxycertlocation;
    }

    public boolean ContactWebService(String proxyname, String proxypwd) {

        try {
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

            out.close();
            //get Response code
            int resCode = httpConn.getResponseCode();

            if (resCode == 200) {
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

    //convert pem format to pkcs12
    public String certkey2keystore(String keyfile) throws Exception {

        System.out.println("Temporary file : " + keyfile);
        String key_out_location = "c:/tmp/vo12.p12";
        assert key_out_location != null;

        String convertCommand = "openssl pkcs12 -export -in " + keyfile + " -inkey " + keyfile + " -out " + key_out_location + " -password pass:test123";
        Runtime.getRuntime().exec(convertCommand, null, null);

        return key_out_location;
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

            byte[] buf = new byte[8 * 1024];  // 8K buffer
            int bytesRead;
            while ((bytesRead = in.read(buf)) != -1) {
                out.write(buf, 0, bytesRead);
            }

            in.close();
            out.close();
            
            // Successfully saved the data

            
            // Create a builder factory
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);

            // Create the builder and parse the file
            Document doc = factory.newDocumentBuilder().parse("C:\\clean\\response.xml");
            org.w3c.dom.Element element = doc.getDocumentElement();
           
            File temp = File.createTempFile("voproxy", ".pem", new File("c:\\tmp"));
            //Write to temp file
            BufferedWriter bwriter = new BufferedWriter(new FileWriter(temp));
            bwriter.write(element.getTextContent());
            bwriter.close();
            
            this.proxycertlocation=certkey2keystore(temp.getPath());

        } catch (Exception e) {
            e.printStackTrace();
            
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
}
