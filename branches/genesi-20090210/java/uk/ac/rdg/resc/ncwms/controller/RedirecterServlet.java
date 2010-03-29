/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.rdg.resc.ncwms.controller;

import java.io.IOException;
import java.net.URLDecoder;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import ucar.nc2.dataset.NetcdfDataset;

/**
 *
 * @author Jon
 */
public class RedirecterServlet extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        String newUrl = request.getParameter("url");
        if (newUrl == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String url = URLDecoder.decode(newUrl, "UTF-8");
        response.sendRedirect(url);
    }

    public static void main(String[] args) throws Exception
    {
        String targetUrl = "https://genesi.nilu.no/thredds/dodsC/GLOBmodel/Aggregates/CTRL/o3column.nc";
        String redirecterUrl = "http://localhost:8080/ncWMS/RedirecterServlet?url=" + targetUrl;

        NetcdfDataset nc = NetcdfDataset.openDataset(redirecterUrl);
        nc.toString();

        if (nc != null) nc.close();
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
