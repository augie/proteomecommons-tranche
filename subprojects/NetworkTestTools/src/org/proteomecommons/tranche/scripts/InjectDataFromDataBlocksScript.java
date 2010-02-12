/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.scripts;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.tranche.TrancheServer;
import org.tranche.configuration.ConfigKeys;
import org.tranche.configuration.Configuration;
import org.tranche.exceptions.ChunkAlreadyExistsSecurityException;
import org.tranche.exceptions.UnexpectedEndOfDataBlockException;
import org.tranche.flatfile.DataDirectoryConfiguration;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.hash.*;
import org.tranche.hash.span.HashSpan;
import org.tranche.meta.*;
import org.tranche.network.*;
import org.tranche.server.PropagationExceptionWrapper;
import org.tranche.server.PropagationReturnWrapper;
import org.tranche.users.*;
import org.tranche.util.IOUtil;
import org.tranche.util.Text;

/**
 * <p>Given one or more data directories, inject everything to network using target hash span rules.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class InjectDataFromDataBlocksScript implements TrancheScript {

    /**
     * 
     * @param args
     */
    public static void main(String[] args) throws Exception {

        // Stuff
        ProteomeCommonsTrancheConfig.load();
        NetworkUtil.waitForStartup();

        try {
            String userPath = null, userPassphrase = null;
            Set<String> ddcPaths = new HashSet();
            for (int i = 0; i < args.length; i += 2) {
                String flag = args[i];
                String val = args[i + 1];
                if (flag.equals("-u")) {
                    userPath = val;
                } else if (flag.equals("-p")) {
                    userPassphrase = val;
                } else if (flag.equals("-d")) {
                    ddcPaths.add(val);
                } else if (flag.equals("-t")) {
                    threadCountLimit = Integer.parseInt(val);
                } else {
                    System.err.println("Unrecognized flag: " + args[i]);
                    printUsage(System.err);
                    System.exit(1);
                }
            }

            if (userPath == null) {
                System.err.println("Missing: path to user file.");
                printUsage(System.err);
                System.exit(3);
            }

            if (userPassphrase == null) {
                System.err.println("Missing: user passphrase.");
                printUsage(System.err);
                System.exit(3);
            }

            if (ddcPaths.size() == 0) {
                System.err.println("No data directories specified, but at least one is required.");
                printUsage(System.err);
                System.exit(3);
            }

            UserZipFile uzf = null;
            Set<File> dataDirectoryFiles = new HashSet();
            try {
                File userFile = new File(userPath);
                if (!userFile.exists()) {
                    throw new Exception("Did not find user file: " + userPath);
                }

                uzf = new UserZipFile(userFile);
                uzf.setPassphrase(userPassphrase);

                uzf.getPrivateKey();
                
                if (uzf.isNotYetValid()) {
                    throw new RuntimeException("User certificate is not yet valid: "+uzf.getFile().getAbsolutePath());
                }
                
                if (uzf.isExpired()) {
                    throw new RuntimeException("User certificate is expired: "+uzf.getFile().getAbsolutePath());
                }
                
                System.out.println("Using user file: " + userFile.getAbsolutePath());

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

                System.out.println("... finished waiting for data directories to load, took: " + Text.getPrettyEllapsedTimeString(System.currentTimeMillis() - start));

                long dataChunkCount = 0, metaChunkCount = 0;

                BigInteger offset = BigInteger.ZERO;
                final BigInteger batch = BigInteger.valueOf(1000);

                while (true) {
                    BigHash[] hashes = ffts.getDataHashes(offset, batch);

                    if (hashes.length == 0) {
                        break;
                    }

                    dataChunkCount += hashes.length;
                    offset = offset.add(batch);
                }

                offset = BigInteger.ZERO;

                while (true) {
                    BigHash[] hashes = ffts.getMetaDataHashes(offset, batch);

                    if (hashes.length == 0) {
                        break;
                    }

                    metaChunkCount += hashes.length;
                    offset = offset.add(batch);
                }

                System.out.println("Found a total of " + dataChunkCount + " data chunk(s) and " + metaChunkCount + " meta chunk(s).");

                System.out.println("Injecting meta data chunks:");

                offset = BigInteger.ZERO;

                while (true) {
                    BigHash[] hashes = ffts.getMetaDataHashes(offset, batch);

                    if (hashes.length == 0) {
                        break;
                    }

                    BATCH:
                    for (BigHash hash : hashes) {
                        try {
                            byte[] chunk = ffts.getDataBlockUtil().getMetaData(hash);

                            // Verify can read meta data or discard
                            ByteArrayInputStream bais = null;
                            try {
                                bais = new ByteArrayInputStream(chunk);
                                MetaData md = MetaDataUtil.read(bais);
                                if (md == null) {
                                    throw new NullPointerException("MetaData is null");
                                }
                            } catch (Exception e) {
                                // Hey, failed!
                                System.err.println(e.getClass().getSimpleName() + " occurred while reading in meta data: " + e.getMessage());
                                continue BATCH;
                            } finally {
                                IOUtil.safeClose(bais);
                            }

                            injectMetaDataChunk(chunk, hash, uzf);
                        } catch (UnexpectedEndOfDataBlockException ueodbe) {
                            // Don't cry over spilt milk
                            continue;
                        }
                    }

                    System.out.println("    * Finished meta data chunks " + offset.longValue() + "-" + offset.add(batch).longValue() + " of " + metaChunkCount);

                    offset = offset.add(batch);
                }

                System.out.println("Injecting data chunks:");

                offset = BigInteger.ZERO;
                while (true) {
                    BigHash[] hashes = ffts.getDataHashes(offset, batch);

                    if (hashes.length == 0) {
                        break;
                    }

                    BATCH:
                    for (BigHash hash : hashes) {
                        try {
                            byte[] chunk = ffts.getDataBlockUtil().getData(hash);

                            // Verify won't be problem while injecting; else, discard
                            BigHash verifyHash = new BigHash(chunk);
                            if (!hash.equals(verifyHash)) {
                                System.err.println("Data chunk doesn't verify. Expect <" + hash + ">, found <" + verifyHash + ">.");
                                continue BATCH;
                            }

                            injectDataChunk(chunk, hash, uzf);
                        } catch (UnexpectedEndOfDataBlockException ueodbe) {
                            // Don't cry over spilt milk
                            continue;
                        }
                    }

                    System.out.println("    * Finished data chunks " + offset.longValue() + "-" + offset.add(batch).longValue() + " of " + dataChunkCount);

                    offset = offset.add(batch);
                }

                for (Thread t : threads) {
                    t.join();
                }
            } finally {
                IOUtil.safeClose(ffts);
            }

        } catch (Exception e) {
            System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
            printUsage(System.err);
            System.exit(2);
        }

        System.out.println("~ fin ~");
        System.exit(0);
    }

    /**
     * 
     * @param bytes
     * @param hash
     */
    private static void injectDataChunk(byte[] bytes, BigHash hash, UserZipFile uzf) {
        injectChunk(bytes, hash, false, uzf);
    }

    /**
     * 
     * @param bytes
     * @param hash
     */
    private static void injectMetaDataChunk(byte[] bytes, BigHash hash, UserZipFile uzf) {
        injectChunk(bytes, hash, true, uzf);
    }
    private static final int THREAD_COUNT_LIMIT_DEFAULT = 25;
    private static int threadCountLimit = THREAD_COUNT_LIMIT_DEFAULT;
    private static Set<Thread> threads = new HashSet();

    /**
     * 
     * @param bytes
     * @param hash
     * @param isMetaData
     */
    private static void injectChunk(final byte[] bytes, final BigHash hash, final boolean isMetaData, final UserZipFile uzf) {

        Thread t = new Thread() {

            @Override()
            public void run() {
                final boolean[] wasInjected = {false};
                SERVERS:
                for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {
                    if (!row.isOnline() || !row.isWritable()) {
                        continue;
                    }

                    boolean isTarget = false;
                    for (HashSpan hs : row.getTargetHashSpans()) {
                        if (hs.contains(hash)) {
                            isTarget = true;
                            break;
                        }
                    }

                    if (isTarget) {
                        final boolean isConnected = ConnectionUtil.isConnected(row.getHost());
                        TrancheServer ts = null;
                        try {
                            if (!isConnected) {
                                ts = ConnectionUtil.connectHost(row.getHost(), true);
                            } else {
                                ts = ConnectionUtil.getHost(row.getHost());
                            }
                            PropagationReturnWrapper prw = null;
                            if (isMetaData) {
                                prw = IOUtil.setMetaData(ts, uzf.getCertificate(), uzf.getPrivateKey(), true, hash, bytes);
                                if (prw.isAnyErrors()) {
                                }
                            } else {
                                prw = IOUtil.setData(ts, uzf.getCertificate(), uzf.getPrivateKey(), hash, bytes);
                            }
                            boolean isErrors = false;
                            if (prw.isAnyErrors()) {
                                for (PropagationExceptionWrapper pew : prw.getErrors()) {
                                    System.err.println("PropogationExceptionWrapper: " + pew.exception.getClass().getSimpleName() + " on " + pew.host + ": " + pew.exception.getMessage());
                                    if (!pew.exception.getClass().equals(ChunkAlreadyExistsSecurityException.class)) {
                                        isErrors = true;
                                    }
                                }
                            }

                            if (!isErrors) {
                                wasInjected[0] = true;
                                break SERVERS;
                            }
                        } catch (Exception e) {
                            System.err.println(e.getClass().getSimpleName() + " occurred while injecting chunk to " + row.getHost() + ": " + e.getMessage());
                            e.printStackTrace(System.err);
                        } finally {
                            if (!isConnected) {
                                ConnectionUtil.unlockConnection(row.getHost());
                                ConnectionUtil.safeCloseHost(row.getHost());
                            }
                        }
                    }
                }

                if (!wasInjected[0]) {
                    System.err.println("Failed to inject " + (isMetaData ? "meta data" : "data") + " chunk: " + hash);
                }
            }
        };
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();

        threads.add(t);

//        System.out.println("DEBUG> Injecting " + (isMetaData ? "meta data" : "data") + " chunk (" + Text.getFormattedBytes(bytes.length) + ", thread count: " + threads.size() + "): " + hash);

        // Lazily remove finished threads when limit met
        while (threads.size() >= threadCountLimit) {

            Set<Thread> toRemove = new HashSet();

            for (Thread next : threads) {
                if (!next.isAlive()) {
                    toRemove.add(next);
                }
            }

            for (Thread next : toRemove) {
                threads.remove(next);
            }

            if (threads.size() >= threadCountLimit) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) { /* */ }
            }
        }

    }

    public static void printUsage(PrintStream out) {
        out.println();
        out.println("USAGE");
        out.println("   InjectDataFromDataBlocksScript [OPTIONAL PARAMETERS] -u <path to user file> -p <user file passphrase> -d <data directory> [-d <data directory>]");
        out.println();
        out.println("DESCRIPTION");
        out.println("   Injects chunks from one or more data directory. If cannot inject data or meta data chunk, prints out hash and type of failed chunk to standard output and continues. (Standard error will contain error messages.)");
        out.println();
        out.println("OPTIONAL PARAMETERS");
        out.println("   -t      Value: positive number      The maximum number of threads to use concurrently. Default is " + THREAD_COUNT_LIMIT_DEFAULT + ".");
        out.println();
        out.println("USAGE");
        out.println("   0: Exit normally");
        out.println("   1: Missing parameters/wrong number of parameters");
        out.println("   2: Unknown error (see standard error)");
        out.println("   3: Problem with parameters");
        out.println();
    }
}


