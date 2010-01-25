/*
 * PrintRandomPerformanceStatsScript.java
 *
 * Created on January 22, 2008, 11:49 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.serverlogs.scripts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.proteomecommons.tranche.serverlogs.ServerCacheUtil;
import org.tranche.util.Text;

/**
 * <p>Just to test syncing and integrity of files...</p>
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 */
public class PrintRandomPerformanceStatsScript {

    public static void main(String args[]) {
        try {
            testSomeStatsShow(ServerCacheUtil.serverLogDirectory, 20);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void testSomeStatsShow(File serverLogsRoot, int testCount) throws Exception {
        int count = 0;

        // Only used to pick a server
        Random r = new Random();

        File[] randomDirs;
        File nextServerDir;

        while (count < testCount) {

            randomDirs = serverLogsRoot.listFiles();

            nextServerDir = randomDirs[r.nextInt(randomDirs.length)];

            // Verify its a dir...
            if (!nextServerDir.getName().matches("[0-9]+\\-[0-9]+\\-[0-9]+\\-[0-9]+\\-[0-9]+")) {
                continue;
            }

            long start = System.currentTimeMillis();

            // Get the IP file to read in proper IP
            BufferedReader reader = null;
            File ipFile = new File(nextServerDir, "IP");
            String ip = null;
            try {
                reader = new BufferedReader(new FileReader(ipFile));
                ip = reader.readLine();
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }

            if (ip == null) {
                throw new RuntimeException("Failed to read in IP address from ip file: " + ipFile.getAbsolutePath());
            }

            boolean isUpload = r.nextBoolean();

            if (isUpload) {
                System.out.println("Upload report for " + ip);
            } else {
                System.out.println("Download report for " + ip);
            }

            Map minutes = null, hours = null, days = null;

            if (isUpload) {
                hours = ServerCacheUtil.getMostRecentUploadedByHours(ip, 50);
                days = ServerCacheUtil.getMostRecentUploadedByDays(ip, 50);
                minutes = ServerCacheUtil.getMostRecentUploadedByMinutes(ip, 50);
            } else {
                hours = ServerCacheUtil.getMostRecentDownloadedByHours(ip, 50);
                days = ServerCacheUtil.getMostRecentDownloadedByDays(ip, 50);
                minutes = ServerCacheUtil.getMostRecentDownloadedByMinutes(ip, 50);
            }

            // ------------------------------------------
            System.out.println("  Minutes: ");

            // Convert timestamp keyset to list and sort
            Set timestampsSet = minutes.keySet();
            List timestampsList = new ArrayList();
            Iterator setIterator = timestampsSet.iterator();
            Long nextTimestamp;
            while (setIterator.hasNext()) {
                nextTimestamp = (Long) setIterator.next();
                timestampsList.add(nextTimestamp);
            }
            Collections.sort(timestampsList);

            while (!timestampsList.isEmpty()) {
                Long timestampObj = (Long) timestampsList.remove(0);
                long timestamp = timestampObj.longValue();
                long bytes = ((Long) minutes.get(timestampObj)).longValue();
                System.out.println(" " + Text.getFormattedDate(timestamp) + ": " + Text.getFormattedBytes(bytes));
            }
            System.out.println();

            // ------------------------------------------
            System.out.println("  Hours: ");

            // Convert timestamp keyset to list and sort
            timestampsSet = hours.keySet();
            timestampsList = new ArrayList();
            setIterator = timestampsSet.iterator();
            while (setIterator.hasNext()) {
                nextTimestamp = (Long) setIterator.next();
                timestampsList.add(nextTimestamp);
            }
            Collections.sort(timestampsList);

            while (!timestampsList.isEmpty()) {
                Long timestampObj = (Long) timestampsList.remove(0);
                long timestamp = timestampObj.longValue();
                long bytes = ((Long) hours.get(timestampObj)).longValue();
                System.out.println(" " + Text.getFormattedDate(timestamp) + ": " + Text.getFormattedBytes(bytes));
            }
            System.out.println();

            // ------------------------------------------
            System.out.println("  Days: ");

            // Convert timestamp keyset to list and sort
            timestampsSet = days.keySet();
            timestampsList = new ArrayList();
            setIterator = timestampsSet.iterator();
            while (setIterator.hasNext()) {
                nextTimestamp = (Long) setIterator.next();
                timestampsList.add(nextTimestamp);
            }
            Collections.sort(timestampsList);

            while (!timestampsList.isEmpty()) {
                Long timestampObj = (Long) timestampsList.remove(0);
                long timestamp = timestampObj.longValue();
                long bytes = ((Long) days.get(timestampObj)).longValue();
                System.out.println(" " + Text.getFormattedDate(timestamp) + ": " + Text.getFormattedBytes(bytes));
            }
            System.out.println();

            count++;
            System.out.println("Finished count " + count + " of " + testCount + ", took: " + Text.getPrettyEllapsedTimeString(System.currentTimeMillis() - start));

        }
    }
}
