/*
 * RenameOldServerLogsScript.java
 *
 * Created on January 21, 2008, 10:16 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.serverlogs.scripts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.proteomecommons.tranche.serverlogs.ServerCacheUtil;
import org.tranche.server.logs.LogEntry;
import org.tranche.server.logs.LogReader;
import org.tranche.util.IOUtil;
import org.tranche.util.Text;

/**
 * <p>BEST TO RUN WHEN SERVER IS OFFLINE!</p>
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 */
public class RenameOldServerLogsScript {

    /**
     * <p>Uses renameOldServerLogs to rename old logs to new name scheme.</p>
     * <p>See JavaDoc for renameOldServerLogs for more details.</p>
     */
    public static void main(String[] args) {
        final long start = System.currentTimeMillis();
        try {
            System.out.println("Tool started at " + Text.getFormattedDate(start));

            //
//            renameOldServerLogs(Database.databaseDirectory);

            // Bryan's Mac test dir
            renameOldServerLogs(ServerCacheUtil.serverLogDirectory);
        } catch (Exception ex) {
            System.err.println("Problem renaming old server logs: " + ex.getMessage());
            ex.printStackTrace(System.err);
        } finally {
            System.out.println("Tool finished, took: " + Text.getPrettyEllapsedTimeString(System.currentTimeMillis() - start));
        }
    }

    /**
     * <p>Finds server logs with ".tmp" in the file name, and converts them
     *    to [timestamp].log, where timestamp is the latest timestamp
     *    in the file + 1000ms. (Heuristic)</p>
     *
     * @param dir The root dir for server logs. Recursively goes through
     */
    public static void renameOldServerLogs(File dir) throws Exception {
        final long start = System.currentTimeMillis();
        if (!dir.exists()) {
            throw new FileNotFoundException("Can't find server log directory, doesn't exist: " + dir.getAbsolutePath());
        }
        if (!dir.canRead() || !dir.canWrite()) {
            throw new IOException("Permissions exception -- can read: " + dir.canRead() + ", can write: " + dir.canWrite());        // Depth-first for efficient memory use
        // Use strings instead of files to avoid excessive file handles
        }
        List potentialFilePaths = new LinkedList();
        potentialFilePaths.add(dir.getAbsolutePath());

        File nextFile;
        int renameCount = 0;
        int failCount = 0;
        int totalCount = 0;
        while (!potentialFilePaths.isEmpty()) {
            nextFile = new File((String) potentialFilePaths.remove(0));

            // If directory, add contents first
            if (nextFile.isDirectory()) {
                File[] moreFiles = nextFile.listFiles();

                // Add each element at beginning (depth-first)
                for (int i = 0; i < moreFiles.length; i++) {
                    potentialFilePaths.add(0, moreFiles[i].getAbsolutePath());
                }
                System.out.println("  + Added " + moreFiles.length + " files from directory: " + nextFile.getAbsolutePath());
            } else if (nextFile.getName().endsWith("tmp.log")) {
                try {
                    renameIndividualLog(nextFile);
                    renameCount++;
                } catch (Exception ex) {
                    System.err.println("  ? Problem mining and renaming " + nextFile.getAbsolutePath() + ": " + ex.getMessage());
                    ex.printStackTrace(System.err);
                    failCount++;
                } finally {
                    totalCount++;

                    // Offer update of progress
                    if (totalCount % 50 == 0) {
                        System.out.println("  ! Progress report: renamed total of " + renameCount + " logs, failed total of " + failCount + " logs, total of " + totalCount + " old log files found.");
                        System.out.println("  ! Run time so far: " + Text.getPrettyEllapsedTimeString(System.currentTimeMillis() - start));
                    }
                }
            } else {
                // Nope, most files should fall here
//                System.out.println("  ? Ignoring irrelevant file: "+nextFile.getAbsolutePath());
            } // Is file a directory or log file?
        } // While still files to process...

        System.out.println("Finished, renamed total of " + renameCount + " logs, failed total of " + failCount + " logs, total of " + totalCount + " old log files found.");
    }

    /**
     *
     */
    private static void renameIndividualLog(File logFile) throws Exception {
        LogReader reader = null;
        try {
            reader = new LogReader(logFile);
            LogEntry next;
            long newestTimestamp = Long.MIN_VALUE;
            while (reader.hasNext()) {
                next = reader.next();

                // Has newest timestamp?
                long timestamp = next.getTimestamp();
                if (timestamp > newestTimestamp) {
                    newestTimestamp = timestamp;
                }
            }

            // Only care if there is at least one record
            if (newestTimestamp != Long.MIN_VALUE) {

                // Add a second to simulate transfer + processing time
                newestTimestamp += 1000;

                File renamedLogFile = new File(logFile.getParent(), newestTimestamp + ".log");
                logFile.renameTo(renamedLogFile);
            } else {
                System.out.println("  - Deleting empty log file: " + logFile.getAbsolutePath());
                IOUtil.safeDelete(logFile);
            }

        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }
}
