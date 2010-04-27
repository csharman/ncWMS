package uk.ac.rdg.resc.ncwms.controller;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;

/**
 *
 * @author ads
 */
public class Data extends HttpServlet {

    //godiva uid and url for sending blog post
    String requestDataURL = "http://blogs.blogmydata.org/api/rest/adddata/uid/b13f3e037c6082ed20346a96520c496f";

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/xml;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            String blogTitle = request.getParameter("dtitle");
            String filename = request.getParameter("dfile");

            HttpClient client = new HttpClient();
            String xml = getXMLDataRequest(blogTitle, filename);
            PostMethod method = new PostMethod(requestDataURL);

            method.addParameter("request", xml);

            // Send POST request
            int statusCode = client.executeMethod(method);

            if (statusCode != HttpStatus.SC_OK) {
                out.println("Method failed: " + method.getStatusLine());
            }
            InputStream rstream = null;

            // Get the response body
            rstream = method.getResponseBodyAsStream();

            // Process the response
            BufferedReader br = new BufferedReader(new InputStreamReader(rstream));
            String line;
            while ((line = br.readLine()) != null) {
                out.println(line);
            }
            br.close();

        } finally {
            out.close();
        }
    }

    protected String getEncodedImage(String filename) {
        try {

            //  ENCODING
            BufferedImage img = ImageIO.read(new File("C:\\"+filename));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            baos.flush();
            byte[] encodedImage = Base64.encodeBase64(baos.toByteArray());
            String data = new String(encodedImage, "utf-8");
            return data;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    protected String getXMLDataRequest(
            String btitle, String filename) {

        String xmlRequest = new String();
        String data = getEncodedImage(filename);
        xmlRequest =
                xmlRequest.concat(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xmlRequest =
                xmlRequest.concat("<dataset>");

        xmlRequest =
                xmlRequest.concat(
                "<title>" + btitle + "</title>");

        xmlRequest =
                xmlRequest.concat(
                "<data><dataitem type=\"inline\" ext=\"png\" main=\"1\" filename=\"" + filename + "\">" + data + "</dataitem></data>");
        xmlRequest =
                xmlRequest.concat("</dataset>");
        return xmlRequest;
    }

// <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
