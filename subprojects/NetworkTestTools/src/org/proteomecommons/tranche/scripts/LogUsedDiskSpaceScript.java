/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.scripts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import org.proteomecommons.tranche.scripts.status.NetworkInformation;
import org.proteomecommons.tranche.scripts.status.StatusUtil;
import org.proteomecommons.tranche.scripts.status.TrancheStatusTable;
import org.proteomecommons.tranche.scripts.status.TrancheStatusTableRow;
import org.tranche.commons.TextUtil;
import org.tranche.util.IOUtil;

/**
 *
 * @author bryan
 */
public class LogUsedDiskSpaceScript {

    final public static String DEFAULT_OUPUT_PATH = "out";

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {

        try {
            String outputPath = DEFAULT_OUPUT_PATH;

            for (int i = 0; i < args.length; i++) {

                final String name = args[i];
                final String value = args[i + 1];

                if (name.equals("--dir") || name.equals("-d")) {
                    outputPath = value;
                } else {
                    System.err.println("Unrecognized parameter: " + name);
                    printUsage();
                    System.exit(1);
                }
            }

            File outputDir = new File(outputPath);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            if (!outputDir.exists()) {
                System.err.println("Unable to create output directory: " + outputDir.getAbsolutePath());
                printUsage();
                System.exit(2);
            }

            NetworkInformation info = StatusUtil.getTrancheStatusTable();

            TrancheStatusTable status = info.getStatusTable();

            for (TrancheStatusTableRow row : status.getRows()) {
                if (row.isAdminWritabled) {
                    File outputFile = new File(outputDir, row.host + ".csv");
                    final boolean isExists = outputFile.exists();

                    BufferedWriter writer = null;
                    try {
                        writer = new BufferedWriter(new FileWriter(outputFile, true));

                        if (!isExists) {
                            writer.append("\"Date\",\"Space used (out of " + TextUtil.formatBytes(row.diskSpace) + ")\",\"Space used in bytes (out of " + row.diskSpace+ ")\"");
                            writer.newLine();
                        }

                        writer.append("\"" + TextUtil.getFormattedDateSimple(System.currentTimeMillis()) + "\",\"" + TextUtil.formatBytes(row.diskSpaceUsed) + "\","+row.diskSpaceUsed);
                        writer.newLine();

                    } finally {
                        IOUtil.safeClose(writer);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
            printUsage();
            System.exit(3);
        }
    }

    /**
     *
     */
    private static void printUsage() {
        System.err.println();
        System.err.println("USAGE");
        System.err.println("    ./run.sh [-d /path/to/output/dir]");
        System.err.println();
        System.err.println("DESCRIPTION");
        System.err.println("    Logs used disk space for every core writable server.");
        System.err.println();
        System.err.println("    Uses (or creates) log file called \"host-name.csv\" in the output directory.");
        System.err.println();
        System.err.println("EXIT CODE");
        System.err.println("    0: Exit normally");
        System.err.println("    1: Unrecognized parameter");
        System.err.println("    2: Failed to create output directory");
        System.err.println("    3: Unknown error");
        System.err.println();
    }
}
