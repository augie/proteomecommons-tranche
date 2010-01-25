/*
 * ReplicationsChartRenderer.java
 *
 * Created on March 20, 2008, 3:58 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.proteomecommons.tranche.serverlogs;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * Renders number of replications for project. Expects non-null entry!
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ReplicationsChartRenderer extends HttpServlet {
    /** Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("image/png");
        ServletOutputStream sos = response.getOutputStream();
        
        try {
            
            /**
             * String representation of the ProjectReplicationsEntry
             */
            String replicationsStr = request.getParameter("replicationsEntry");
            
            if (replicationsStr == null || replicationsStr.trim().equals("")) {
                throw new RuntimeException("Must provide servlet with ProjectReplicationsEntry string.");
            }
            
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            
            replicationsStr = replicationsStr.trim();
            
            ProjectReplicationsEntry reps = ProjectReplicationsEntry.fromString(replicationsStr);
            
            String xLabel0 = "0";
            dataset.setValue(reps.reps0, "Number of chunk replications", xLabel0);
            String xLabel1 = "1";
            dataset.setValue(reps.reps1, "Number of chunk replications", xLabel1);
            String xLabel2 = "2";
            dataset.setValue(reps.reps2, "Number of chunk replications", xLabel2);
            String xLabel3 = "3";
            dataset.setValue(reps.reps3, "Number of chunk replications", xLabel3);
            String xLabel4 = "4";
            dataset.setValue(reps.reps4, "Number of chunk replications", xLabel4);
            String xLabel5orMore = "5+";
            dataset.setValue(reps.reps5orMore, "Number of chunk replications", xLabel5orMore);
            
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
                    "Number of replications",
                    "Number of chunks",
                    dataset,
                    PlotOrientation.VERTICAL,
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
