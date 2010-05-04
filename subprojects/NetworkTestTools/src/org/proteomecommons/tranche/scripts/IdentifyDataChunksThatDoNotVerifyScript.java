/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.proteomecommons.tranche.scripts;

import java.io.File;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.tranche.commons.TextUtil;
import org.tranche.configuration.ConfigKeys;
import org.tranche.configuration.Configuration;
import org.tranche.flatfile.DataDirectoryConfiguration;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.hash.*;
import org.tranche.network.NetworkUtil;
import org.tranche.util.IOUtil;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class IdentifyDataChunksThatDoNotVerifyScript implements TrancheScript {
    
    /**
     * 
     * @param args
     */
    public static void main(String[] args) throws Exception {
        // Stuff
        ProteomeCommonsTrancheConfig.load();
        NetworkUtil.waitForStartup();

        try {
            Set<String> ddcPaths = new HashSet();
            for (int i = 0; i < args.length; i += 2) {
                String flag = args[i];
                String val = args[i + 1];
                if (flag.equals("-d")) {
                    ddcPaths.add(val);
                } else {
                    System.err.println("Unrecognized flag: " + args[i]);
                    printUsage(System.err);
                    System.exit(1);
                }
            }

            if (ddcPaths.size() == 0) {
                System.err.println("No data directories specified, but at least one is required.");
                printUsage(System.err);
                System.exit(3);
            }

            Set<File> dataDirectoryFiles = new HashSet();
            try {

                for (String path : ddcPaths) {
                    File ddc = new File(path);

                    if (!ddc.exists()) {
                        throw new Exception("Did not find data directory: " + path);
                    }

                    dataDirectoryFiles.add(ddc);
                }
            } catch (Exception e) {
                System.err.println(e.getClass().getSimpleName() + " while reading in parameters: " + e.getMessage());
                e.printStackTrace(System.err);
                printUsage(System.err);
                System.exit(3);
            }

            File[] ddcsArr = dataDirectoryFiles.toArray(new File[0]);

            FlatFileTrancheServer ffts = null;

            try {
                ffts = new FlatFileTrancheServer(new File("./"));
                Configuration config = ffts.getConfiguration();

                // No healing thread, read-only
                config.setValue(ConfigKeys.HASHSPANFIX_SHOULD_HEALING_THREAD_RUN, String.valueOf(false));
                config.setValue(ConfigKeys.SERVER_MODE_FLAG_ADMIN, String.valueOf(1));

                for (int i = 0; i < ddcsArr.length; i++) {
                    DataDirectoryConfiguration ddc = new DataDirectoryConfiguration(ddcsArr[i].getAbsolutePath(), Long.MAX_VALUE);
                    config.addDataDirectory(ddc);
                }

                System.out.println("Loading FlatFileTrancheServer. Has " + ffts.getConfiguration().getDataDirectories().size() + " data directories:");
                for (DataDirectoryConfiguration ddc : ffts.getConfiguration().getDataDirectories()) {
                    System.out.println("    - " + ddc.getDirectory());
                }
                ffts.setConfiguration(config);
                ffts.saveConfiguration();

                Thread.sleep(1000);

                System.out.println("Waiting for data directories to load...");
                final long start = System.currentTimeMillis();

                ffts.waitToLoadExistingDataBlocks();

                System.out.println("... finished waiting for data directories to load, took: " + TextUtil.formatTimeLength(System.currentTimeMillis() - start));

                System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------------");
                BigInteger offset = BigInteger.ZERO;
                final BigInteger batch = BigInteger.valueOf(1000);
                int failedCount = 0;

                while (true) {
                    BigHash[] hashes = ffts.getDataHashes(offset, batch);

                    if (hashes.length == 0) {
                        break;
                    }
                    
                    for (BigHash hash : hashes) {
                        byte[] chunk = ffts.getDataBlockUtil().getData(hash);
                        BigHash verifyHash = new BigHash(chunk);
                        if (!verifyHash.equals(hash)) {
                            System.out.println(hash);
                            failedCount++;
                        }
                    }
                    offset = offset.add(batch);
                }
                System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------------");
                
                System.out.println("Total data chunks that failed verification: "+failedCount);

            } finally {
                IOUtil.safeClose(ffts);
            }

        } catch (Exception e) {
            System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
            printUsage(System.err);
            System.exit(2);
        }

        System.exit(0);
    }
    
    public static void printUsage(PrintStream out) {
        out.println();
        out.println("USAGE");
        out.println("   IdentifyDataChunksThatDoNotVerifyScript -d <data directory> [-d <data directory>]");
        out.println();
        out.println("DESCRIPTION");
        out.println("   Finds all data chunks in one or more data directories that are corrupted.");
        out.println();
        out.println("USAGE");
        out.println("   0: Exit normally");
        out.println("   1: Missing parameters/wrong number of parameters");
        out.println("   2: Unknown error (see standard error)");
        out.println("   3: Problem with parameters");
        out.println();
    }
}
