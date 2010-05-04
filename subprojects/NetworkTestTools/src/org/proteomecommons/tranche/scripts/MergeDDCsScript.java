/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.scripts;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.tranche.commons.TextUtil;
import org.tranche.flatfile.DataBlockUtil;
import org.tranche.flatfile.DataDirectoryConfiguration;
import org.tranche.flatfile.ProjectFindingThread;
import org.tranche.hash.BigHash;
import org.tranche.util.IOUtil;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class MergeDDCsScript {

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {

        Set<String> inputDDCs = new HashSet(),
                outputDDCs = new HashSet();

        final long processStart = System.currentTimeMillis();

        long dataChunksAdded = 0, dataChunksSkipped = 0, dataChunksError = 0;
        long metaChunksAdded = 0, metaChunksSkipped = 0, metaChunksError = 0;

        try {

            // The specific network shouldn't matter (since no network I/O),
            // but include perchance some aspect of code assumes load was
            // invokes (i.e., avoid npe)
            ProteomeCommonsTrancheConfig.load();
            
            for (int i = 0; i < args.length; i += 2) {
                String flag = args[i];
                String val = args[i + 1];

                if (flag.equals("-i")) {
                    inputDDCs.add(val);
                } else if (flag.equals("-o")) {
                    outputDDCs.add(val);
                } else {
                    System.err.println("Unrecognized argument: " + flag);
                    printUsage();
                    System.exit(1);
                }
            }

            if (inputDDCs.size() < 1 || outputDDCs.size() < 1) {
                System.err.println("Must include at least one input data directory and at least one output data directory.");
                printUsage();
                System.exit(2);
            }

            // Verify all input DDCs
            for (String path : inputDDCs) {
                if (!isDirectoryUsable(path)) {
                    System.err.println("Input directory not usable: " + path);
                    printUsage();
                    System.exit(3);
                }
            }

            // Verify all output DDCs
            for (String path : outputDDCs) {
                if (!isDirectoryUsable(path)) {
                    System.err.println("Output directory not usable: " + path);
                    printUsage();
                    System.exit(3);
                }
            }

            DataBlockUtil outputDBU = null;
            try {
                outputDBU = new DataBlockUtil();

                for (String out : outputDDCs) {
                    DataDirectoryConfiguration ddc = new DataDirectoryConfiguration(out, Long.MAX_VALUE);
                    outputDBU.add(ddc);
                }

                // Load each input DDCs
                for (String in : inputDDCs) {
                    final long start = System.currentTimeMillis();
                    System.out.println("Starting: " + in + " (" + TextUtil.getFormattedDate(start) + ")");

                    DataBlockUtil inputDBU = null;
                    try {
                        inputDBU = new DataBlockUtil();
                        DataDirectoryConfiguration ddc = new DataDirectoryConfiguration(in, Long.MAX_VALUE);
                        inputDBU.add(ddc);

                        ProjectFindingThread.loadDataBlocks(inputDBU);
                        printEllapsedMessage("Finished loading data blocks", start);

                        // Add: meta data chunks
                        {
                            long offset = 0;
                            final long batch = 1000;
                            BigHash[] hashes = inputDBU.getMetaDataHashes(offset, batch);

                            long hashesCount = 0;
                            while (hashes != null && hashes.length > 0) {

                                for (BigHash hash : hashes) {
                                    hashesCount++;
                                    try {
                                        if (!outputDBU.hasMetaData(hash)) {
                                            byte[] chunk = inputDBU.getMetaData(hash);
                                            outputDBU.addMetaData(hash, chunk);
                                            metaChunksAdded++;
                                        } else {
                                            metaChunksSkipped++;
                                        }
                                    } catch (Exception e) {
                                        printEllapsedMessage(e.getClass().getSimpleName() + " while reading/writing meta data chunk <" + hash + ">: " + e.getMessage(), start);
                                        e.printStackTrace();
                                        metaChunksError++;
                                    }
                                }

                                offset += batch;
                                hashes = inputDBU.getMetaDataHashes(offset, batch);
                            }

                            printEllapsedMessage("Finished loading meta data chunks from <" + in + ">, found total of " + hashesCount + " chunks.", start);
                        }

                        // Add: data chunks
                        {
                            long offset = 0;
                            final long batch = 1000;
                            BigHash[] hashes = inputDBU.getDataHashes(offset, batch);

                            long hashesCount = 0;
                            while (hashes != null && hashes.length > 0) {

                                for (BigHash hash : hashes) {
                                    hashesCount++;
                                    try {
                                        if (!outputDBU.hasData(hash)) {
                                            byte[] chunk = inputDBU.getData(hash);
                                            outputDBU.addData(hash, chunk);
                                            dataChunksAdded++;
                                        } else {
                                            dataChunksSkipped++;
                                        }
                                    } catch (Exception e) {
                                        printEllapsedMessage(e.getClass().getSimpleName() + " while reading/writing data chunk <" + hash + ">: " + e.getMessage(), start);
                                        dataChunksError++;
                                    }
                                }

                                offset += batch;
                                hashes = inputDBU.getDataHashes(offset, batch);
                            }

                            printEllapsedMessage("Finished loading data chunks from <" + in + ">, found total of " + hashesCount + " chunks.", start);
                        }
                    } finally {
                        IOUtil.safeClose(inputDBU);
                    }
                } // For each input DDC



            } finally {
                IOUtil.safeClose(outputDBU);
            }
        } catch (Exception e) {
            System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
            printUsage();
            System.exit(4);
        } finally {
            System.out.println();
            System.out.println("dataChunksAdded = " + dataChunksAdded + ", dataChunksSkipped = " + dataChunksSkipped + ", dataChunksError = " + dataChunksError);
            System.out.println("metaChunksAdded = " + metaChunksAdded + ", metaChunksSkipped = " + metaChunksSkipped + ", metaChunksError = " + metaChunksError);
            System.out.println();
            System.out.println("~ fin, process ran for " + TextUtil.formatTimeLength(System.currentTimeMillis() - processStart) + " ~");
        }
    }

    /**
     * 
     * @param msg
     * @param start
     */
    private static void printEllapsedMessage(String msg, long start) {
        long delta = System.currentTimeMillis() - start;
        System.out.println("    " + TextUtil.formatTimeLength(delta) + ": " + msg);
    }

    /**
     * 
     * @param path
     * @return
     */
    private static boolean isDirectoryUsable(String path) {
        File f = new File(path);
        if (!f.exists()) {
            f.mkdirs();
        }

        return f.exists() && f.isDirectory();
    }

    /**
     * 
     */
    private static void printUsage() {
        System.err.println();
        System.err.println("USAGE");
        System.err.println("    java -Xmx512m -jar MergeDDCsSCript.jar -i <input-ddc-1> [-i <input-ddc-2> ...] -o <output-ddc-1> [-o <output-ddc-2>]");
        System.err.println();
        System.err.println("DESCRIPTION");
        System.err.println("    Merge together chunks from multiple input DDCs and write out to one or more DDCs. Handles duplicate chunks correctly.");
        System.err.println();
        System.err.println("RETURN CODES");
        System.err.println("    0: Exited normally");
        System.err.println("    1: Unrecognized argument");
        System.err.println("    2: Missing required argument");
        System.err.println("    3: Directory does not exist and could not be created");
        System.err.println("    4: Unknown error (see standard error for more information)");
        System.err.println();
    }
}
