package uk.ac.rdg.resc.ncwms.controller;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 *
 * @author ads
 */
public class Blog extends HttpServlet {

    //godiva uid and url for sending blog post
    String requestPostURL = "http://blogs.blogmydata.org/api/rest/addpost/uid/b13f3e037c6082ed20346a96520c496f";
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
            //get screenshot image
            String img = getScreenshotImage(request);

            //metadata for blog
            String datasetvar = request.getParameter("datasetid");
            String title = request.getParameter("title");
            String varname = request.getParameter("variable");
            String timeTxt = request.getParameter("timetxt");
            String elev = request.getParameter("elevation");
            String crs = request.getParameter("crs");
            String username = parseUsername(request.getParameter("bopenid"));
            String blogText = request.getParameter("btext");
            String geom = request.getParameter("geom");
            String url = request.getParameter("url");
            String[] p = title.split("&gt;");

            String geotxt = getGeoTxt(geom);

            //first getback a dataid
            String dataIdxml = getXMLDataRequest(p[1].trim(), img, url);

            //issue request
            String respXml = getXmlResponse(dataIdxml, requestDataURL);
            
            String dataid = getDataId(respXml);
            
            String xml = getXMLPostRequest(username, p[1].trim(), datasetvar, blogText, timeTxt, elev, geotxt, dataid, crs);
            
            //issue request
            String respPostXml=getXmlResponse(xml, requestPostURL);
            out.print(respPostXml);

        } finally {
            out.close();
        }
    }

    public String getXmlResponse(String xml, String addr) {

        try {

            StringBuilder sb = new StringBuilder();
            String line;

            HttpClient client = new HttpClient();
            PostMethod method = new PostMethod(addr);
            method.addParameter("request", xml);

            // Send POST request
            int statusCode = client.executeMethod(method);

            InputStream rstream = null;

            // Get the response body
            rstream = method.getResponseBodyAsStream();

            // Process the response
            BufferedReader br = new BufferedReader(new InputStreamReader(rstream));
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();
            return sb.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getDataId(String xml) {

        try {
            StringReader reader = new StringReader(xml);
            InputSource inputSource = new InputSource(reader);

            // Create a builder factory
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);

            // Create the builder and parse the file
            Document doc = factory.newDocumentBuilder().parse(inputSource);
            org.w3c.dom.Element element = doc.getDocumentElement();

            NodeList nl = doc.getElementsByTagName("result");
            Element el = (Element) nl.item(0);
            String datasetid = getTextValue(el, "data_id");
            return datasetid;
        } catch (Exception e) {
            return null;
        }
    }

    private static String getTextValue(Element ele, String tagName) {
        String textVal = null;
        NodeList nl = ele.getElementsByTagName(tagName);
        if (nl != null && nl.getLength() > 0) {
            Element el = (Element) nl.item(0);
            textVal = el.getFirstChild().getNodeValue();
        }

        return textVal;
    }

    private static final class BoundingBox {

        float minXValue;
        float maxXValue;
        float minYValue;
        float maxYValue;
    }

    private String getGeoTxt(String geom) {
        String[] bbox = geom.split(",");
        String bformat = "POLYGON((" + bbox[0] + " " + bbox[1] + "," + bbox[2] + " " + bbox[1] + "," + bbox[2] + " " + bbox[3] + "," + bbox[0] + " " + bbox[3] + "," + bbox[0] + " " + bbox[1] + "))";
        return bformat;
    }

    private static BufferedImage downloadImage(String path) throws IOException {
        return ImageIO.read(new URL(path));
    }

    protected String getScreenshotImage(HttpServletRequest request) {

        try {

            String title = request.getParameter("title").replaceAll("&gt;", ">"); //"Hello World";
            String time = request.getParameter("time"); //"null";
            String elevation = request.getParameter("elevation"); //"null";
            String units = request.getParameter("units");
            String upperValue = request.getParameter("upperValue"); //1.0967412;
            String twoThirds = request.getParameter("twoThirds");
            String oneThird = request.getParameter("oneThird");
            String lowerValue = request.getParameter("lowerValue"); //-0.9546131;
            boolean isLatLon = "true".equalsIgnoreCase(request.getParameter("latLon"));

            // Find the URL of this server from the request
            StringBuffer requestUrl = request.getRequestURL();
            String server = requestUrl.substring(0, requestUrl.indexOf("blog"));

            String BGparam = request.getParameter("urlBG");
            String FGparam = request.getParameter("urlFG");
            String urlStringPalette = request.getParameter("urlPalette");

            if (BGparam == null || FGparam == null || urlStringPalette == null) {
                // TODO: better error handling
                throw new Exception("Null BG, FG or palette param");
            }

            String urlStringBG = BGparam;
            String urlStringFG = server + FGparam;

            BoundingBox BBOX = new BoundingBox();
            String[] serverName = urlStringBG.split("\\?");
            StringBuffer result = buildURL(serverName[1], serverName[0], "BG", BBOX);
            serverName = urlStringFG.split("\\?");
            StringBuffer resultFG = buildURL(serverName[1], serverName[0], "FG", BBOX);

            float minX1 = 0;
            float minX2 = 0;
            float maxX1 = 0;
            float maxX2 = 0;
            int WIDTH_OF_BG_IMAGE1 = 0;
            int WIDTH_OF_BG_IMAGE2 = 0;
            int START_OF_IMAGE3 = 0;
            int START_OF_IMAGE4 = 0;
            final int WIDTH_TOTAL = 512;
            final int HEIGHT_TOTAL = 400;
            final int WIDTH_OF_FINAL_IMAGE = 650;
            final int HEIGHT_OF_FINAL_IMAGE = 480;
            String URL1 = "";
            String URL2 = "";
            float coverage = 0;

            boolean isGT180 = false;
            boolean isReplicate = false;

            String bboxParam = "&BBOX=" + BBOX.minXValue + "," + BBOX.minYValue + "," + BBOX.maxXValue + "," + BBOX.maxYValue;

            if (isLatLon && (Float.compare(BBOX.minXValue, -180) < 0)) // means we need to generate two URLs
            {

                if ((Float.compare(BBOX.minXValue, -180) < 0)) {
                    minX1 = -180; //minXValue;
                    if (Float.compare(BBOX.maxXValue, 180) > 0) // It will only happen for the case of zoom out: when maxX > 180
                    {
                        maxX1 = BBOX.maxXValue - 360;
                        isReplicate = true;
                    } else {
                        maxX1 = BBOX.maxXValue;
                    }
                    minX2 = BBOX.minXValue + 360;
                    maxX2 = +180;

                    float rangeofImg1 = Math.abs(maxX1 - minX1);
                    float rangeofImg2 = Math.abs(maxX2 - minX2);
                    float totalSpan = rangeofImg1 + rangeofImg2;

                    // in normal viewing case, the span is 360
                    // with first zoom-in, the span becomes 180
                    // with first zoom out, the spam becoms 720
                    if (isReplicate) {
                        coverage = (rangeofImg1 / (totalSpan * 2));
                    } else {
                        coverage = (rangeofImg1 / totalSpan);
                    }

                    WIDTH_OF_BG_IMAGE1 = Math.round(((float) (WIDTH_TOTAL) * coverage));   // RHS Image

                    if (isReplicate) {
                        WIDTH_OF_BG_IMAGE2 = (WIDTH_TOTAL / 2) - WIDTH_OF_BG_IMAGE1;
                        START_OF_IMAGE3 = WIDTH_OF_BG_IMAGE1 + WIDTH_OF_BG_IMAGE2;
                        START_OF_IMAGE4 = START_OF_IMAGE3 + WIDTH_OF_BG_IMAGE2;
                    } else {
                        WIDTH_OF_BG_IMAGE2 = WIDTH_TOTAL - WIDTH_OF_BG_IMAGE1;          // LHS Image
                    }
                }

                String bboxParam1 = "&BBOX=" + minX1 + "," + BBOX.minYValue + "," + maxX1 + "," + BBOX.maxYValue;
                String bboxParam2 = "&BBOX=" + minX2 + "," + BBOX.minYValue + "," + maxX2 + "," + BBOX.maxYValue;

                URL1 = result.toString() + "WIDTH=" + WIDTH_OF_BG_IMAGE1 + "&HEIGHT=" + HEIGHT_TOTAL + bboxParam1;
                URL2 = result.toString() + "WIDTH=" + WIDTH_OF_BG_IMAGE2 + "&HEIGHT=" + HEIGHT_TOTAL + bboxParam2;

                isGT180 = true;
            } else {
                URL1 = result.toString() + "WIDTH=" + WIDTH_TOTAL + "&HEIGHT=" + HEIGHT_TOTAL + bboxParam;
            }

            String URL3 = resultFG.toString() + "WIDTH=" + WIDTH_TOTAL + "&HEIGHT=" + HEIGHT_TOTAL + bboxParam;

            BufferedImage bimgBG1 = null;
            BufferedImage bimgBG2 = null;

            BufferedImage bimgFG = null;
            BufferedImage bimgPalette = null;
            if (isGT180) {
                bimgBG1 = downloadImage(URL1); //(path[0]);  // right-hand side
                bimgBG2 = downloadImage(URL2); //(path[1]);  // left-hand side
            } else {
                bimgBG1 = downloadImage(URL1);
            }
            bimgFG = downloadImage(URL3);
            bimgPalette = downloadImage(urlStringPalette);//(path[2]);

            /* Prepare the final Image */
            int type = BufferedImage.TYPE_INT_RGB;
            BufferedImage image = new BufferedImage(WIDTH_OF_FINAL_IMAGE, HEIGHT_OF_FINAL_IMAGE, type);
            Graphics2D g = image.createGraphics();

            // The Font and Text
            Font font = new Font("SansSerif", Font.BOLD, 12);
            g.setFont(font);
            g.setBackground(Color.white);
            g.fillRect(0, 0, WIDTH_OF_FINAL_IMAGE, HEIGHT_OF_FINAL_IMAGE);

            g.setColor(Color.black);
            g.drawString(title, 0, 10);
            if (time != null) {
                g.drawString("Time: " + time, 0, 30);
            }
            if (elevation != null) {
                g.drawString(elevation, 0, 50);
            }

            // Now draw the image
            if (isGT180) {
                g.drawImage(bimgBG1, null, WIDTH_OF_BG_IMAGE2, 60);
                g.drawImage(bimgBG2, null, 0, 60);
                if (isReplicate) {
                    g.drawImage(bimgBG2, null, START_OF_IMAGE3, 60);
                    g.drawImage(bimgBG1, null, START_OF_IMAGE4, 60);
                }
            } else {
                g.drawImage(bimgBG1, null, 0, 60);
            }
            g.drawImage(bimgFG, null, 0, 60);
            g.drawImage(bimgPalette, WIDTH_TOTAL, 60, 45, HEIGHT_TOTAL, null);

            g.drawString(upperValue, 560, 63);
            g.drawString(twoThirds, 560, 192);
            if (units != null) {
                g.drawString("Units: " + units, 560, 258);
            }
            g.drawString(oneThird, 560, 325);
            g.drawString(lowerValue, 560, 460);

            g.dispose();


            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            baos.flush();
            byte[] encodedImage = Base64.encodeBase64(baos.toByteArray());
            String data = new String(encodedImage, "utf-8");
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    protected String parseUsername(String username) {
        username = username.replaceAll("([a-zA-Z]+://)", "");
        username = username.replaceAll("([a-zA-Z]+://)", "");
        username = username.replaceAll("/$", "");
        username = username.replaceAll("/?/i", "-");
        username = username.replaceAll("/&", "-");
        username = username.replaceAll("//", "-");
        username = username.replaceAll("/s", "");
        return username;
    }

    private static StringBuffer buildURL(String url, String serverName, String type, BoundingBox bb) {

        String[] params = url.split("&");
        StringBuffer result = new StringBuffer();
        result.append(serverName);
        result.append("?");
        String separator = "&";

        for (int i = 0; i < params.length; i++) {
            if (params[i].startsWith("BBOX")) {
                String tempParam = params[i];
                String bbValues = tempParam.substring(5); // to remove BBOX= from the start of the string
                String[] bbox = bbValues.split(",");
                if (type.equals("BG") == true) {
                    bb.minXValue = (float) Double.parseDouble(bbox[0]);
                    bb.maxXValue = (float) Double.parseDouble(bbox[2]);
                    bb.minYValue = (float) Double.parseDouble(bbox[1]);
                    bb.maxYValue = (float) Double.parseDouble(bbox[3]);
                }
                for (int indx = 0; indx < bbox.length; indx++) {
                    //out.print("bbox param " + indx + ": " + bbox[indx]);
                }
                continue;
            }
            if (params[i].startsWith("WIDTH") || params[i].startsWith("HEIGHT")) {
                continue;
            }

            result.append(params[i]);
            result.append(separator);
        }
        return result;
    }

    protected String getXMLDataRequest(
            String btitle, String data, String url) {
        String filename = "screenshot.png";

        String xmlRequest = new String();
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
                "<data><dataitem type=\"inline\" ext=\"png\" main=\"1\" filename=\"" + filename + "\">" + data + "</dataitem>");

        //replace & with &amp in the url
        xmlRequest =
                xmlRequest.concat(
                "<dataitem type=\"url\" ext=\"url\" filename=\"" + "\">" + url.replace("&", "&amp;") + "</dataitem></data>");

        xmlRequest =
                xmlRequest.concat("</dataset>");
        return xmlRequest;
    }

    protected String getXMLPostRequest(
            String username, String title, String datasetvar, String btext, String btime, String elev, String geom, String dataid, String crs) {

        String xmlRequest = new String();
        String[] dt = datasetvar.split("/");

        xmlRequest =
                xmlRequest.concat(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xmlRequest =
                xmlRequest.concat("<post>");

        xmlRequest =
                xmlRequest.concat(
                "<title>" + title + "</title>");
        xmlRequest =
                xmlRequest.concat(
                "<section>ahm2007</section>");
        xmlRequest =
                xmlRequest.concat(
                "<author>");

        xmlRequest =
                xmlRequest.concat(
                "<username>" + username + "</username>");

        xmlRequest =
                xmlRequest.concat(
                "</author>");


        xmlRequest =
                xmlRequest.concat(
                "<content>[data=size:525x363]" + dataid + "[/data]" + btext + "</content>");

        xmlRequest =
                xmlRequest.concat(
                "<datestamp>" + btime + "</datestamp>");

        xmlRequest =
                xmlRequest.concat("<blog_sname>godivasandpit</blog_sname>");


        xmlRequest =
                xmlRequest.concat(
                "<metadata><dataset>" + dt[0] + "</dataset>");

        xmlRequest =
                xmlRequest.concat(
                "<variable>" + dt[1] + "</variable>");

        xmlRequest =
                xmlRequest.concat(
                "<time>" + btime + "</time>");


        xmlRequest =
                xmlRequest.concat(
                "<elevation>" + elev + "</elevation>");

        xmlRequest =
                xmlRequest.concat(
                "<geometry>" + geom + "</geometry>");

        xmlRequest =
                xmlRequest.concat(
                "<crs>" + crs + "</crs></metadata>");

        xmlRequest =
                xmlRequest.concat("<attached_data><data type=\"local\">" + dataid + "</data></attached_data>");

        xmlRequest =
                xmlRequest.concat("</post>");
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
