/*
 * PopulateLogDirsWithIPFilesScript.java
 *
 * Created on January 22, 2008, 3:03 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.serverlogs.scripts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import org.proteomecommons.tranche.serverlogs.ServerCacheUtil;
import org.tranche.util.IOUtil;

/**
 * <p></p>
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 */
public class PopulateLogDirsWithIPFilesScript {

    public static void main(String[] args) {
        try {
            populateServerLogDirsWithIPFiles(ServerCacheUtil.serverLogDirectory);
        } catch (Exception ex) {
            System.out.println("Exception while trying to build IP files: " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }

    public static void populateServerLogDirsWithIPFiles(File dir) throws Exception {
        File[] serverLogDirs = dir.listFiles();

        File nextDir;
        String nextIP;
        File nextIPFile;

        int total = 0, created = 0, skipped = 0;
        for (int i = 0; i < serverLogDirs.length; i++) {
            nextDir = serverLogDirs[i];
            if (nextDir.getName().matches("[0-9]+\\-[0-9]+\\-[0-9]+\\-[0-9]+\\-[0-9]+")) {
                String[] o = nextDir.getName().split("-");

                nextIP = o[0] + "." + o[1] + "." + o[2] + "." + o[3] + ":" + o[4];

                nextIPFile = new File(nextDir, "IP");
                if (!nextIPFile.exists()) {

                    nextIPFile.createNewFile();
                    BufferedWriter writer = null;
                    try {
                        writer = new BufferedWriter(new FileWriter(nextIPFile, false));
                        writer.write(nextIP);
                        System.out.println("Wrote IP " + nextIP + " to file: " + nextIPFile.getAbsolutePath());
                        created++;
                    } finally {
                        IOUtil.safeClose(writer);
                    }
                } else {
                    System.out.println("  Already exists: " + nextIPFile.getAbsolutePath());
                    skipped++;
                }
                total++;
            } else {
                System.out.println("Skipped file, doesn't match log dir regex: " + nextDir.getAbsolutePath());
            }
        }

        System.out.println("Total of " + total + " server log dirs found. " + created + " IP files created, " + skipped + " already existed and were skipped.");
    }
}
