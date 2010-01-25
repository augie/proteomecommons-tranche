package org.proteomecommons.tranche.serverlogs;
/*
 * PerformanceChartRenderer.java
 *
 * Created on January 23, 2008, 11:49 AM
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;

import javax.servlet.*;
import javax.servlet.http.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * <p>Shows upload and download information for one or all servers.</p>
 * <p>Expects the following HTTP parameters:</p>
 * <ul>
 *   <li>url: The url (or IP address) of server, or "aggregate" to specify all servers, or "core" to specify core servers</li>
 *   <li>type: minutes, hours, days</li>
 * </ul>
 * <p>Optional parameters, which will override the above:</p>
 * <ul>
 *   <li>bandwidth-window</li>
 *   <li>bandwidth-servers</li>
 * </ul>
 * @author besmit
 * @version
 */
public class PerformanceChartRenderer extends HttpServlet {

    /** Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("image/png");
        ServletOutputStream sos = response.getOutputStream();

        // Need server URL or ip. Will queury ControllerUtil for
        // appropriate buffered information.
        String url = request.getParameter("url");

        // Need to know: is this for "minutes", "hours" or "days".
        String type = request.getParameter("type");

        if (url == null || url.trim().equals("")) {
            throw new RuntimeException("Must provide servlet with url.");
        } else if (type == null || type.trim().equals("")) {
            throw new RuntimeException("Must provide servlet with type: minutes, hours or days.");
        }

        // Optional parameters
        String bandwidthWindow = request.getParameter("bandwidth-window");
        if (!BandwidthChartUtil.isValidBandwidthWindowOption(bandwidthWindow)) {
            // If not valid, don't use it!
            bandwidthWindow = "";
        }

        String bandwidthServers = request.getParameter("bandwidth-servers");
        if (!BandwidthChartUtil.isValidBandwidthServerOption(bandwidthServers)) {
            // If not valid, don't use it!
            bandwidthServers = "";
        }

        int timeUnitCounts = 30;

        // Use bandwidth window to set the type and timeUnitCounts
        if (bandwidthWindow.equals(BandwidthChartUtil.bandwidthWindow30Hour)) {
            timeUnitCounts = 30;
            type = "hours";
        } else if (bandwidthWindow.equals(BandwidthChartUtil.bandwidthWindow14day)) {
            timeUnitCounts = 14;
            type = "days";
        } else if (bandwidthWindow.equals(BandwidthChartUtil.bandwidthWindow30day)) {
            timeUnitCounts = 30;
            type = "days";
        } else if (bandwidthWindow.equals(BandwidthChartUtil.bandwidthWindow365day)) {
            timeUnitCounts = 365;
            type = "days";
        } else if (bandwidthWindow.equals(BandwidthChartUtil.bandwidthWindowAll)) {
            type = "days";

            // Blah, calculate max number of day records
            final long deltaStartToNow = System.currentTimeMillis() - ServerCacheUtil.startingTimestamp;
            final int maxEntries = (int) Math.floor((double) deltaStartToNow / (1000 * 60 * 60 * 24));

            timeUnitCounts = maxEntries;
        }

        //
        if (bandwidthServers.equals(BandwidthChartUtil.bandwidthServerAll)) {
            url = "aggregate";
        } else if (bandwidthServers.equals(BandwidthChartUtil.bandwidthServerCore)) {
            url = "core";
        }

        // Grab the data from ServerCacheUtil!
        Map uploadMap = null, downloadMap = null;

        DateTickUnit tick = null;

        // If want all data, handle specially
        if (bandwidthWindow.equals(BandwidthChartUtil.bandwidthWindowAll)) {
            if (url.trim().equalsIgnoreCase("aggregate")) {
                uploadMap = ServerCacheUtil.getAllUploadedAggregate();
                downloadMap = ServerCacheUtil.getAllDownloadedAggregate();
            } else if (url.trim().equalsIgnoreCase("core")) {
                uploadMap = ServerCacheUtil.getAllUploadedCore();
                downloadMap = ServerCacheUtil.getAllDownloadedCore();
            } else {
                uploadMap = ServerCacheUtil.getUploadedByDays(url, ServerCacheUtil.startingTimestamp, Integer.MAX_VALUE);
                downloadMap = ServerCacheUtil.getDownloadedByDays(url, ServerCacheUtil.startingTimestamp, Integer.MAX_VALUE);
            }

            int tickUnits = 7;

            // Don't allow more than 6 ticks (labels in the x-axis)
            if (timeUnitCounts > tickUnits * 6) {
                tickUnits = timeUnitCounts / 6;
            }

            tick = new DateTickUnit(DateTickUnit.DAY, tickUnits);

        } else if (type.equalsIgnoreCase("minutes")) {

            if (url.trim().equalsIgnoreCase("aggregate")) {
                uploadMap = ServerCacheUtil.getMostRecentUploadedByMinutesAggregate(timeUnitCounts);
                downloadMap = ServerCacheUtil.getMostRecentDownloadedByMinutesAggregate(timeUnitCounts);
            } else if (url.trim().equalsIgnoreCase("core")) {
                uploadMap = ServerCacheUtil.getMostRecentUploadedByMinutesCore(timeUnitCounts);
                downloadMap = ServerCacheUtil.getMostRecentDownloadedByMinutesCore(timeUnitCounts);
            } else {
                uploadMap = ServerCacheUtil.getMostRecentUploadedByMinutes(url, timeUnitCounts);
                downloadMap = ServerCacheUtil.getMostRecentDownloadedByMinutes(url, timeUnitCounts);
            }

            int tickUnits = 10;

            // Don't allow more than 6 ticks (labels in the x-axis)
            if (timeUnitCounts > tickUnits * 6) {
                tickUnits = timeUnitCounts / 6;
            }

            tick = new DateTickUnit(DateTickUnit.MINUTE, tickUnits);
        } else if (type.equalsIgnoreCase("hours")) {

            if (url.trim().equalsIgnoreCase("aggregate")) {
                uploadMap = ServerCacheUtil.getMostRecentUploadedByHoursAggregate(timeUnitCounts);
                downloadMap = ServerCacheUtil.getMostRecentDownloadedByHoursAggregate(timeUnitCounts);
            } else if (url.trim().equalsIgnoreCase("core")) {
                uploadMap = ServerCacheUtil.getMostRecentUploadedByHoursCore(timeUnitCounts);
                downloadMap = ServerCacheUtil.getMostRecentDownloadedByHoursCore(timeUnitCounts);
            } else {
                uploadMap = ServerCacheUtil.getMostRecentUploadedByHours(url, timeUnitCounts);
                downloadMap = ServerCacheUtil.getMostRecentDownloadedByHours(url, timeUnitCounts);
            }

            int tickUnits = 6;

            // Don't allow more than 6 ticks (labels in the x-axis)
            if (timeUnitCounts > tickUnits * 6) {
                tickUnits = timeUnitCounts / 6;
            }

            tick = new DateTickUnit(DateTickUnit.HOUR, tickUnits);
        } else if (type.equalsIgnoreCase("days")) {

            if (url.trim().equalsIgnoreCase("aggregate")) {
                uploadMap = ServerCacheUtil.getMostRecentUploadedByDaysAggregate(timeUnitCounts);
                downloadMap = ServerCacheUtil.getMostRecentDownloadedByDaysAggregate(timeUnitCounts);
            } else if (url.trim().equalsIgnoreCase("core")) {
                uploadMap = ServerCacheUtil.getMostRecentUploadedByDaysCore(timeUnitCounts);
                downloadMap = ServerCacheUtil.getMostRecentDownloadedByDaysCore(timeUnitCounts);
            } else {
                uploadMap = ServerCacheUtil.getMostRecentUploadedByDays(url, timeUnitCounts);
                downloadMap = ServerCacheUtil.getMostRecentDownloadedByDays(url, timeUnitCounts);
            }

            int tickUnits = 7;

            // Don't allow more than 6 ticks (labels in the x-axis)
            if (timeUnitCounts > tickUnits * 6) {
                tickUnits = timeUnitCounts / 6;
            }

            tick = new DateTickUnit(DateTickUnit.DAY, tickUnits);
        } else {
            throw new RuntimeException("Unrecognized time unit \"" + type + "\", expecting \"minutes\", \"hours\" or \"days\".");
        }


        try {

            // Holds collection of XYSeries
            XYSeriesCollection dataset = new XYSeriesCollection();

            // ----------------- Find highest val for Uploads -----------------
            Long timestampObj, valueObj;
            Iterator it = uploadMap.keySet().iterator();

            // Add timestamps to a list so can sort
            List uploadsTimestampList = new ArrayList(timeUnitCounts);
            while (it.hasNext()) {
                timestampObj = (Long) it.next();
                uploadsTimestampList.add(timestampObj);
            }

            // Sort list
            Collections.sort(uploadsTimestampList);

            // Find highest value. Use that to scale.
            long highestValueUpload = Long.MIN_VALUE;
            //for (int i = 0; i < timeUnitCounts; i++) {
            for (int i = 0; i < uploadsTimestampList.size(); i++) {
                timestampObj = (Long) uploadsTimestampList.get(i);
                valueObj = (Long) uploadMap.get(timestampObj);
                if (valueObj.longValue() > highestValueUpload) {
                    highestValueUpload = valueObj.longValue();
                }
            }

            // --------------- Find highest val for Downloads ---------------
            it = downloadMap.keySet().iterator();

            // Add timestamps to a list so can sort
            List downloadsTimestampList = new ArrayList(30);
            while (it.hasNext()) {
                timestampObj = (Long) it.next();
                downloadsTimestampList.add(timestampObj);
            }

            // Sort list
            Collections.sort(downloadsTimestampList);

            // Find highest value. Use that to scale.
            long highestValueDownload = Long.MIN_VALUE;
//            for (int i = 0; i < timeUnitCounts; i++) {
            for (int i = 0; i < downloadsTimestampList.size(); i++) {
                timestampObj = (Long) downloadsTimestampList.get(i);
                valueObj = (Long) downloadMap.get(timestampObj);
                if (valueObj.longValue() > highestValueDownload) {
                    highestValueDownload = valueObj.longValue();
                }
            }

            // --------------- FIGURE OUT SCALE --------------------

            // What's the highest value?
            long highestValue = highestValueUpload > highestValueDownload ? highestValueUpload : highestValueDownload;

            // Units. Will be 1K, 1M, 1G or 1T
            long units = 1;
            String unitsStr = "Bytes";
            if (highestValue > 1000000000000L) {
                units = 1000000000000L;
                unitsStr = "TB";
            } else if (highestValue > 1000000000L) {
                units = 1000000000L;
                unitsStr = "GB";
            } else if (highestValue > 1000000L) {
                units = 1000000L;
                unitsStr = "MB";
            } else if (highestValue > 1000L) {
                units = 1000L;
                unitsStr = "KB";
            }

            // ------------- ADD DATA SERIES FOR [UP|DOWN]LOADS

            XYSeries uploadSeries = new XYSeries("Uploads (in " + unitsStr + ")");

            // Add to XY collections
//            for (int i = 0; i < timeUnitCounts; i++) {
            for (int i = 0; i < uploadsTimestampList.size(); i++) {
                timestampObj = (Long) uploadsTimestampList.get(i);
                valueObj = (Long) uploadMap.get(timestampObj);

                // Scale to units
                double scaledValue = (double) valueObj.longValue() / units;
                uploadSeries.add(timestampObj.longValue(), scaledValue);
            }

            dataset.addSeries(uploadSeries);

            // Pretent download information
            XYSeries downloadSeries = new XYSeries("Downloads (in " + unitsStr + ")");

            // Add to XY collections
//            for (int i = 0; i < timeUnitCounts; i++) {
            for (int i = 0; i < downloadsTimestampList.size(); i++) {
                timestampObj = (Long) downloadsTimestampList.get(i);
                valueObj = (Long) downloadMap.get(timestampObj);

                // Scale to units
                double scaledValue = (double) valueObj.longValue() / units;
                downloadSeries.add(timestampObj.longValue(), scaledValue);
            }

            dataset.addSeries(downloadSeries);

//            JFreeChart chart = ChartFactory.createScatterPlot("Test plot", // Title
//                    "Independent var", // x-axis Label
//                    "Dependent (psuedo-random)", // y-axis Label
//                    dataset, // Dataset
//                    PlotOrientation.VERTICAL, // Plot Orientation
//                    true, // Show Legend
//                    true, // Use tooltips
//                    false // Configure chart to generate URLs?
//                    );
            JFreeChart chart = ChartFactory.createTimeSeriesChart(
                    null, // Title for chart
                    null, // Title for time axis
                    "Data Transfer (" + unitsStr + ")", // Time for dependent axis
                    dataset,
                    true, // Show Legend
                    true, // Use tooltips
                    false // Configure chart to generate URLs?
                    );

            // draw the graphic at size 800x200
            int width = 700;
            int height = 200;
            BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = (Graphics2D) bi.getGraphics();

            chart.setAntiAlias(true);
            chart.setBackgroundPaint(Color.WHITE);
            chart.getXYPlot().setBackgroundPaint(new Color(0xEE, 0xEE, 0xEE, 0xFF));
            chart.getXYPlot().setRangeCrosshairPaint(Color.BLACK);
            chart.getXYPlot().setDomainCrosshairPaint(Color.BLACK);
            chart.getXYPlot().setWeight(3);

            DateAxis dateAxis = (DateAxis) chart.getXYPlot().getDomainAxis();
            dateAxis.setDateFormatOverride(new SimpleDateFormat("EEE MMM dd HH:mm"));

            // Set the tick!
            dateAxis.setTickUnit(tick);

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
