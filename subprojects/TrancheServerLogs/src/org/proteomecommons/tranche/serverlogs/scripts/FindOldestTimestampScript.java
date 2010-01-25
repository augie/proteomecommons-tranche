/*
 * FindOldestTimestampScript.java
 *
 * Created on January 22, 2008, 12:30 AM
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
import org.tranche.util.Text;

/**
 *
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 */
public class FindOldestTimestampScript {
    
    /**
     * <p>Uses renameOldServerLogs to rename old logs to new name scheme.</p>
     * <p>See JavaDoc for renameOldServerLogs for more details.</p>
     */
    public static void main(String[] args) {
        final long start = System.currentTimeMillis();
        try {
            System.out.println("Tool started at "+Text.getFormattedDate(start));

            // Bryan's Mac test dir
            findOldestTimestampInDirectory(ServerCacheUtil.serverLogDirectory);
        } catch (Exception ex) {
            System.err.println("Problem finding oldest timestamp: "+ex.getMessage());
            ex.printStackTrace(System.err);
        } finally {
            System.out.println("Tool finished, took: "+Text.getPrettyEllapsedTimeString(System.currentTimeMillis()-start));
        }
    }
    
    /**
     * <p>Finds oldest timestamp from any server log recursively, starting at root dir.</p>
     *
     * @param dir The root dir for server logs. Recursively goes thru.
     */
    public static void findOldestTimestampInDirectory(File dir) throws Exception {
        final long start = System.currentTimeMillis();
        if (!dir.exists())
            throw new FileNotFoundException("Can't find server log directory, doesn't exist: "+dir.getAbsolutePath());
        if (!dir.canRead() || !dir.canWrite())
            throw new IOException("Permissions exception -- can read: "+dir.canRead()+", can write: "+dir.canWrite());
        
        // Depth-first for efficient memory use
        // Use strings instead of files to avoid excessive file handles
        List potentialFilePaths = new LinkedList();
        potentialFilePaths.add(dir.getAbsolutePath());
        
        File nextFile;
        int renameCount = 0;
        int failCount = 0;
        int totalCount = 0;
        
        long oldestTimestamp = Long.MAX_VALUE;
        while(!potentialFilePaths.isEmpty()) {
            nextFile = new File((String)potentialFilePaths.remove(0));
            
            // If directory, add contents first
            if (nextFile.isDirectory()) {
                File[] moreFiles = nextFile.listFiles();
                
                // Add each element at beginning (depth-first)
                for(int i=0; i<moreFiles.length; i++) {
                    potentialFilePaths.add(0,moreFiles[i].getAbsolutePath());
                }
                System.out.println("  + Added "+moreFiles.length+" files from directory: "+nextFile.getAbsolutePath());
            } else if (nextFile.getName().endsWith(".log")) {
                try {
                    long timestamp = findOldestTimestampInLog(nextFile);
                    
                    if (timestamp < oldestTimestamp)
                        oldestTimestamp = timestamp;
                    renameCount++;
                } catch (Exception ex) {
                    System.err.println("  ? Problem mining "+nextFile.getAbsolutePath()+": "+ex.getMessage());
                    ex.printStackTrace(System.err);
                    failCount++;
                } finally {
                    totalCount++;
                    
                    // Offer update of progress
                    if (totalCount % 50 == 0) {
                        System.out.println("  ! Progress report: successfully mined total of "+renameCount+" logs, failed total of "+failCount+" logs, total of "+totalCount+" old log files found.");
                        System.out.println("  ! Run time so far: "+Text.getPrettyEllapsedTimeString(System.currentTimeMillis()-start));
                    }
                }
            } else {
                System.out.println("  ? Ignoring irrelevant file: "+nextFile.getAbsolutePath());
            } // Is file a directory or log file?
        } // While still files to process...
        
        System.out.println("Finished, successfully mined total of "+renameCount+" logs, failed total of "+failCount+" logs, total of "+totalCount+" old log files found.");
    
        System.out.println("OLDEST TIMESTAMP: "+oldestTimestamp);
        System.out.println("  FROM DATE: "+Text.getFormattedDate(oldestTimestamp));
    }
    
    /**
     * Returns oldest timestamp from a log file.
     */
    private static long findOldestTimestampInLog(File logFile) throws Exception {
        LogReader reader = null;
        long oldestTimestamp = Long.MAX_VALUE;
        try {
            reader = new LogReader(logFile);
            LogEntry next;
            while(reader.hasNext()) {
                next = reader.next();
                
                // Has newest timestamp?
                long timestamp = next.getTimestamp();
                if (timestamp < oldestTimestamp) {
                    oldestTimestamp = timestamp;
                }
            }
            
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        
        return oldestTimestamp;
    }
}
