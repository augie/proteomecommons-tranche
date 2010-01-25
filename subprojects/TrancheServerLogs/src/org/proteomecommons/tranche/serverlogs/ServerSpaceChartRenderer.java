/*
 * ServerSpaceChartRenderer.java
 *
 * Created on February 9, 2008, 12:10 PM
 */

package org.proteomecommons.tranche.serverlogs;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.servlet.*;
import javax.servlet.http.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.tranche.configuration.Configuration;
import org.tranche.flatfile.DataDirectoryConfiguration;
import org.tranche.servers.ServerInfo;
import org.tranche.servers.ServerUtil;

/**
 * <p>Shows simple bar graph for all core servers comparing total and used space.</p>
 * @author Bryan E. Smith - bryanesmith@gmail.com
 * @version
 */
public class ServerSpaceChartRenderer extends HttpServlet {
    
    /** Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("image/png");
        ServletOutputStream sos = response.getOutputStream();
        
        try {
            // Store collections of ServerSummary objects
            List serverSummaryList = new ArrayList();
            
            ServerUtil.waitForStartup();
            
            // Get all server info
            List serverInfoList = new ArrayList();
            serverInfoList.addAll(ServerUtil.getServers(true));
            serverInfoList.addAll(ServerUtil.getServers(false));
            
            Iterator it = serverInfoList.iterator();
            ServerInfo info;
            Configuration config;
            
            while (it.hasNext()) {
                info = (ServerInfo)it.next();
                
                if (info == null) continue;
                
                config = info.getConfiguration();
                
                if (config == null) continue;
                
                // Get server name, fallback on URL
                String serverName = config.getValue("Tranche:Server Name");
                if (serverName == null || serverName.trim().equals("")) {
                    serverName = info.getUrl();
                }
                
                long used = 0, total = 0;
                
                // Get used first. Try real bytes used first.
                // Try to get real value first
                Iterator valuesIterator = config.getValueKeys().iterator();
//            for (String key : info.getConfiguration().getValueKeys()) {
                String key;
                while (valuesIterator.hasNext()) {
                    key = (String)valuesIterator.next();
                    if (key.startsWith("actualBytesUsed")) {
                        used += Long.parseLong(config.getValue(key));
                    }
                }
                
                // Fall back on estimated value
                if (used == 0) {
                    valuesIterator = config.getValueKeys().iterator();
//                for (String key : info.getConfiguration().getValueKeys()) {
                    while (valuesIterator.hasNext()) {
                        key = (String)valuesIterator.next();
                        if (key.startsWith("estimatedBytesUsed")) {
                            used += Long.parseLong(config.getValue(key));
                        }
                    }
                }
                
                //
//            for (DataDirectoryConfiguration conf : info.getConfiguration().getDataDirectories()) {
                Iterator ddcIterator = config.getDataDirectories().iterator();
                DataDirectoryConfiguration ddc;
                while (ddcIterator.hasNext()) {
                    ddc = (DataDirectoryConfiguration)ddcIterator.next();
                    if (ddc.getSizeLimit() != Long.MAX_VALUE) {
                        total += ddc.getSizeLimit();
                    }
                }
                
                // If there is any available space, make the object
                if (total != 0) {
                    serverSummaryList.add(new ServerSummary(serverName,total,used));
                }
            }
            
            // Sort the list
            Collections.sort(serverSummaryList);
            ServerSummary next;
            
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            
            // Place data in catagories
            for (int i=serverSummaryList.size()-1; i>=0; i--) {
                next = (ServerSummary)serverSummaryList.get(i);
                
                long totalInGB = (long)Math.round((double)next.getTotalSpace()/(1024*1024*1024));
                long usedInGB = (long)Math.round((double)next.getSpaceUsed()/(1024*1024*1024));
                
                dataset.setValue(totalInGB,"Total space in GB", next.getName());
                dataset.setValue(usedInGB,"Used space in GB", next.getName());
            }
            
//            JFreeChart chart = ChartFactory.createBarChart3D(
//                    "Tranche Core Server Disk Space",
//                    "Server",
//                    "Space (in MB)",
//                    dataset,
//                    PlotOrientation.HORIZONTAL,
//                    true,
//                    true,
//                    false);
            
            JFreeChart chart = ChartFactory.createBarChart(
                    null,
                    null,
                    "Space (in GB)",
                    dataset,
                    PlotOrientation.HORIZONTAL,
                    true,
                    true,
                    false);
            
            // draw the graphic at size 800x200
            int width = 400;
            int height = 400;
            BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = (Graphics2D)bi.getGraphics();
            
            chart.setAntiAlias(true);
            chart.setBackgroundPaint(Color.WHITE);
            
            chart.draw(graphics, new Rectangle(width, height));
            
            // write out the image
            ImageIO.write(bi, "png", sos);
            
        } catch (Exception ex) {
            throw new ServletException(ex);
        } finally {
            sos.flush();
            sos.close();
        }
    }
    
    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /** Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }
    
    /** Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }
    
    /** Returns a short description of the servlet.
     */
    public String getServletInfo() {
        return "Short description";
    }
    // </editor-fold>
}
