/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.proteomecommons.tranche.serverlogs;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import javax.servlet.*;
import javax.servlet.http.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.tranche.util.IOUtil;
import org.tranche.util.Text;

/**
 *
 * @author Bryan E. Smith
 */
public class StressTestResultsChartRenderer extends HttpServlet {
   
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("image/png");
        ServletOutputStream sos = response.getOutputStream();
        
        BufferedReader reader = null;
        try {
            
            /**
             * String representation of the ProjectReplicationsEntry
             */
            String serverLogPath = request.getParameter("server-log");
            
            if (serverLogPath == null || serverLogPath.trim().equals("")) {
                throw new RuntimeException("Must provide servlet with ProjectReplicationsEntry string.");
            }
            
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            
            File logFile = new File(serverLogPath);
            
            reader = new BufferedReader(new FileReader(logFile));
            
            int numEntries = 0;
            
            String line;
            boolean isFirstLineBurned = false;
            while((line = reader.readLine()) != null) {
                
                // Skip blank lines
                if (line.trim().equals("") || line.trim().startsWith("#")) {
                    continue;
                }
                
                // Lazy burn the first line! Just a header!
                if (!isFirstLineBurned) {
                    isFirstLineBurned = true;
                    continue;
                }
                
                // Catch any parsing errors and skip
                try {
                    String[] tokens = line.split(",");
                    
                    int clientConnections = Integer.parseInt(tokens[0]);
                    long numFilesPerProject = Long.parseLong(tokens[1]);
                    long maxFileSize = Long.parseLong(tokens[2]);
                    long perhapsTotalTestSizeButIgnore = Long.parseLong(tokens[3]);
                    boolean isPerformedDeletes = Boolean.parseBoolean(tokens[4]);
                    long testTime = Long.parseLong(tokens[5]);
                    boolean isPassed = Boolean.parseBoolean(tokens[6]);
                    
                    double timeInHours = (double)testTime / (1000*60*60);
                    
                    if (isPassed) {                    
                        dataset.setValue(timeInHours, "Data uploaded and downloaded", Text.getFormattedBytes(numFilesPerProject*maxFileSize*clientConnections));
                    } else {
                        dataset.setValue(0.0, "Data uploaded and downloaded", Text.getFormattedBytes(numFilesPerProject*maxFileSize*clientConnections));
                    }
                    
                    numEntries++;
                } catch (Exception ex) {
                    System.err.println("Problems parsing stress test line \""+line+"\": "+ex.getMessage());
                    ex.printStackTrace(System.err);
                }
            }
            
//            dataset.setValue(reps.reps0, "Number of chunk replications", "0");
//            dataset.setValue(reps.reps1, "Number of chunk replications", "1");
//            dataset.setValue(reps.reps2, "Number of chunk replications", "2");
//            dataset.setValue(reps.reps3, "Number of chunk replications", "3");
//            dataset.setValue(reps.reps4, "Number of chunk replications", "4");
//            dataset.setValue(reps.reps5orMore, "Number of chunk replications", "5+");
            
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
                    "Size of test",
                    "Time to upload and download (in hours)",
                    dataset,
                    PlotOrientation.VERTICAL,
                    true,
                    true,
                    false);
            
            // draw the graphic at size 800x200
            int width = numEntries * 100;
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
            
            IOUtil.safeClose(reader);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /** 
    * Handles the HTTP <code>GET</code> method.
    * @param request servlet request
    * @param response servlet response
    */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    } 

    /** 
    * Handles the HTTP <code>POST</code> method.
    * @param request servlet request
    * @param response servlet response
    */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
    * Returns a short description of the servlet.
    */
    public String getServletInfo() {
        return "Short description";
    }
    // </editor-fold>
}
