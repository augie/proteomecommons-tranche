/*
 * RebuildCacheFilesScript.java
 *
 * Created on January 22, 2008, 9:50 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.serverlogs.scripts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.proteomecommons.tranche.serverlogs.ServerCacheUtil;

/*
 * RebuildCacheScript.java
 *
 * Created on January 19, 2008, 10:28 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
import org.tranche.util.IOUtil;
import org.tranche.util.Text;

/**
 * <p>BEST TO RUN WHEN SERVER IS OFFLINE!</p>
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 */
public class RebuildCacheFilesScript {

    /**
     * Invokes buildCacheFromServerLogs. View it's JavaDoc entry for more information.
     */
    public static void main(String[] args) {
        buildCacheFromServerLogs();
    }

    /**
     * <p>A very, very expensive operation. Parses all logs and build the cache for each server.</p>
     * <p>The code is used to move to new format OR if cache is somehow destroyed, nothing else.</p>
     * <p>Note that this is synchronized with lazyLoad. Stop all operations when rebuilding cache.</p>
     */
    private static synchronized void buildCacheFromServerLogs() {

        // It's okay if not empty -- recheck whether log file added
        // using SimpleDiskBackedHashList
//        // Bail if cache directory has any files
//        if (ServerCacheUtil.serverCacheDirectory.exists() && ServerCacheUtil.serverCacheDirectory.list().length != 0) {
//            System.out.println("Cannot rebuild cache: cache directory is not empty!");
//            System.out.println("Check that directory is empty: "+ServerCacheUtil.serverCacheDirectory.getAbsolutePath());
//            System.out.println("If you are not trying to rebuild the cache, please notify developers of bug in ControllerUtil for server logs system.");
//            System.out.println("Bailing.");
//            return;
//        }

        long start = System.currentTimeMillis();
        System.out.println("Starting to rebuild cache from server logs. Current time: " + Text.getFormattedDate(start));

        // Avoid excessive file handles, so store paths.
        List serverLogDirs = new ArrayList();
        File[] potentialDirs = ServerCacheUtil.serverLogDirectory.listFiles();
        File nextPotentialDir;
        for (int i = 0; i < potentialDirs.length; i++) {
            nextPotentialDir = potentialDirs[i];

            boolean isCacheOrTempDir = nextPotentialDir.getName().equals("cache") || nextPotentialDir.getName().equals("temp");

            if (nextPotentialDir.isDirectory() && !isCacheOrTempDir) {
                System.out.println("  Adding server log directory: " + nextPotentialDir.getAbsolutePath());
                serverLogDirs.add(nextPotentialDir.getAbsolutePath());
            }
        }

        String serverLogDirPath;
        File serverLogDir;
        // For each server log dir
        for (serverLogDirPath = (String) serverLogDirs.remove(0); !serverLogDirs.isEmpty(); serverLogDirPath = (String) serverLogDirs.remove(0)) {
            long serverStart = System.currentTimeMillis();
            serverLogDir = new File(serverLogDirPath);

            System.out.println("Rebuilding for next server from log directory: " + serverLogDirPath);

            try {

                String ip = null;

                // Read in IP address from ip file
                File ipFile = new File(serverLogDir, "IP");
                if (!ipFile.exists()) {
                    throw new FileNotFoundException("Where's the IP file? Should be at: " + ipFile.getAbsolutePath() + ". Please make that file. Type in IP address, not Tranche URL. No newline, just the IP address. Format: xxx.xxx.xxx:xxxx, no leading zeros.");
                }

                BufferedReader ipFileReader = null;
                try {
                    ipFileReader = new BufferedReader(new FileReader(ipFile));
                    ip = ipFileReader.readLine();
                } finally {
                    if (ipFileReader != null) {
                        IOUtil.safeClose(ipFileReader);
                    }
                }

                if (ip == null || ip.trim().equals("")) {
                    throw new Exception("Read in ip address but didn't get an IP address! For ip file: " + ipFile.getAbsolutePath());
                }

                // For each file in directory, read in chronological order
                File nextServerLog;
                String nextServerLogPath;
                String[] potentialServerLogs = serverLogDir.list();

                // Very important -- must be sorted or cache will fail
                // Actually, above is old, but doesn't hurt. =) Should work regardless.
                List toSort = new ArrayList();
                for (int j = 0; j < potentialServerLogs.length; j++) {
                    toSort.add(potentialServerLogs[j]);
                }
                Collections.sort(toSort);

                for (int i = 0; i < potentialServerLogs.length; i++) {
                    potentialServerLogs[i] = (String) toSort.get(i);
                }

                // Go thru each of server's logs, and parse
                for (int j = 0; j < potentialServerLogs.length; j++) {
                    nextServerLogPath = potentialServerLogs[j];
                    nextServerLog = new File(serverLogDir, nextServerLogPath);

                    if (!nextServerLog.exists()) {
                        System.err.println("  WARNING: potential log does not exist: " + nextServerLog.getAbsolutePath());
                        continue;
                    }

                    if (!nextServerLog.getName().endsWith("log")) {
                        // Only give message if unexpected -- IP files should be there
                        if (!nextServerLog.getName().equals("IP")) {
                            System.err.println("  WARNING: Not a server log, file doesn't not end with .log: " + nextServerLogPath);
                        }
                        continue;
                    }

                    // Queue up information, but yield up for a while if full. Give real
                    // servers some reasonable priority.
                    int count = 0;

                    if (ServerCacheUtil.queueSize() > 10) {
                        final long blockStart = System.currentTimeMillis();
                        while (count < 10000 && ServerCacheUtil.queueSize() > 10) {
                            Thread.yield();
                        }
                        System.out.println("  Blocked on busy queue for " + Text.getPrettyEllapsedTimeString(System.currentTimeMillis() - blockStart));
                    }

                    ServerCacheUtil.enqueueLogFile(nextServerLog.getAbsolutePath(), ip);

                } // For each server log in a server's log directory

            } catch (Exception ex) {
                System.err.println("Problem processing logs from " + serverLogDirPath + ": " + ex.getMessage());
                ex.printStackTrace(System.err);
            } finally {
                System.out.println("  Finished building cache for server, took: " + Text.getPrettyEllapsedTimeString(System.currentTimeMillis() - serverStart));
                System.out.println("  Time ellapsed for entire process: " + Text.getPrettyEllapsedTimeString(System.currentTimeMillis() - start) + ", " + serverLogDirs.size() + " servers left to process.");
            }
        }

        // Merge files for each server!
        System.out.println("Finished building code. Ellapsed time: " + Text.getPrettyEllapsedTimeString(System.currentTimeMillis() - start));
    }
}

