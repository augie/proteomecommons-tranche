/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.scripts;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.tranche.TrancheServer;
import org.tranche.commons.TextUtil;
import org.tranche.configuration.ConfigKeys;
import org.tranche.configuration.Configuration;
import org.tranche.exceptions.UnexpectedEndOfDataBlockException;
import org.tranche.flatfile.DataDirectoryConfiguration;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.hash.*;
import org.tranche.hash.span.HashSpan;
import org.tranche.meta.*;
import org.tranche.network.*;
import org.tranche.users.*;
import org.tranche.util.IOUtil;

/**
 * <p>Given one or more data directories, inject everything to network using target hash span rules.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class InjectDataFromDataBlocksScript implements TrancheScript {

    private final static int DEFAULT_PREFERRED_COPIES = 3;
    private final static int DEFAULT_REQUIRED_COPIES = 3;
    private static final int DEFAULT_THREAD_COUNT_LIMIT = 10;
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final boolean DEFAULT_DELETE_INJECTED_CHUNKS = false;
    private static final boolean DEFAULT_IS_VERBOSE = false;
    private static int preferredCopies = DEFAULT_PREFERRED_COPIES;
    private static int requiredCopies = DEFAULT_REQUIRED_COPIES;
    private static int threadCountLimit = DEFAULT_THREAD_COUNT_LIMIT;
    private static Set<Thread> threads = new HashSet();
    private static int injectionSuccessCount = 0, injectionSkippedCount = 0, injectionFailedCount = 0;
    private static int batchSize = DEFAULT_BATCH_SIZE;
    private static final Object countLock = new Object();
    private static boolean isVerbose = DEFAULT_IS_VERBOSE;
    private static boolean isDeleteInjectedChunksMode = DEFAULT_DELETE_INJECTED_CHUNKS;
    private static final File replicationLockFile = new File("replications.lock");
    private static final File metaDataSaveFile = new File("replications.meta-replications-save");
    private static final File dataSaveFile = new File("replications.data-replications-save");
    private static final boolean[] isFailure = new boolean[1];
    private static Set<String> hostsToDeprioritize = new HashSet();
    private static DiskBackedBigHashList dataChunksToDelete = null, metaDataChunksToDelete = null;

    /**
     * 
     * @param args
     */
    public static void main(String[] args) throws Exception {

        isFailure[0] = false;

        try {
            if (replicationLockFile.exists()) {
                System.out.println("Replication lock file <" + replicationLockFile.getAbsolutePath() + "> exists; only one instance allowed at a time due to save files used. Delete it with caution!");
                // This doesn't use this script's exit(int) method since don't want to delete lock file
                System.exit(4);
            }

            replicationLockFile.createNewFile();

            if (!replicationLockFile.exists()) {
                System.out.println("Failed to create replication lock file. Verify that you have write permissions for the working directory and that the disk is not full.");
                // This doesn't use this script's exit(int) method since don't want to delete lock file
                System.exit(1);
            }

            String uzfPath = null, uzfPass = null;
            String userName = null, userPass = null;
            Set<String> ddcPaths = new HashSet();
            for (int i = 0; i < args.length; i += 2) {
                String flag = args[i];
                String val = args[i + 1];
                if (flag.equals("-U")) {
                    uzfPath = val;
                } else if (flag.equals("-P")) {
                    uzfPass = val;
                } else if (flag.equals("-u")) {
                    userName = val;
                } else if (flag.equals("-p")) {
                    userPass = val;
                } else if (flag.equals("-d")) {
                    ddcPaths.add(val);
                } else if (flag.equals("-t")) {
                    threadCountLimit = Integer.parseInt(val);
                } else if (flag.equals("-r")) {
                    requiredCopies = Integer.parseInt(val);
                } else if (flag.equals("-f")) {
                    preferredCopies = Integer.parseInt(val);
                } else if (flag.equals("-v")) {
                    isVerbose = Boolean.parseBoolean(val);
                } else if (flag.equals("-D")) {
                    isDeleteInjectedChunksMode = Boolean.parseBoolean(val);
                } else if (flag.equals("-B")) {
                    batchSize = Integer.parseInt(val);
                } else if (flag.equals("-S")) {
                    hostsToDeprioritize.add(val);
                } else {
                    System.err.println("Unrecognized flag: " + args[i]);
                    printUsage(System.err);
                    exit(3);
                }
            }

            boolean isUzfPassNull = (uzfPass == null);
            boolean isUzfFileNull = (uzfPath == null);
            boolean isUserNameNull = (userName == null);
            boolean isUserPassNull = (userPass == null);

            // Assert: pair are specified together
            if (isUzfPassNull != isUzfFileNull) {
                System.err.println("Both -U and -P must be specified together.");
                printUsage(System.err);
                exit(3);
            } else if (isUserNameNull != isUserPassNull) {
                System.err.println("Both -u and -p must be specified together.");
                printUsage(System.err);
                exit(3);
            }

            // Assert: only one pair specified
            if (isUzfPassNull == isUserNameNull) {

                if (!isUserPassNull) {
                    System.err.println("Either -U, -P or -u, -p must be specified, but not both pairs.");
                    printUsage(System.err);
                    exit(3);
                } else {
                    System.err.println("You did not specify your user zip file or log in. Either -U, -P or -u, -p must be specified.");
                    printUsage(System.err);
                    exit(1);
                }

            }

            if (ddcPaths.size() == 0) {
                System.err.println("No data directories specified, but at least one is required.");
                printUsage(System.err);
                exit(1);
            }

            ProteomeCommonsTrancheConfig.load();

            UserZipFile uzf = null;
            Set<File> dataDirectoryFiles = new HashSet();
            try {

                if (uzfPath != null) {

                    File uzfFile = new File(uzfPath);
                    if (!uzfFile.exists()) {
                        throw new Exception("Did not find user file: " + uzfPath);
                    }

                    uzf = new UserZipFile(uzfFile);
                    uzf.setPassphrase(uzfPass);

                } else {
                    uzf = UserZipFileUtil.getUserZipFile(userName, userPass);
                }

                uzf.getPrivateKey();

                if (uzf.isNotYetValid()) {
                    throw new RuntimeException("User certificate is not yet valid: " + uzf.getFile().getAbsolutePath());
                }

                if (uzf.isExpired()) {
                    throw new RuntimeException("User certificate is expired: " + uzf.getFile().getAbsolutePath());
                }

                System.out.println("Using user file: " + uzf.getFile().getAbsolutePath());

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
                exit(3);
            }

            File[] ddcsArr = dataDirectoryFiles.toArray(new File[0]);

            NetworkUtil.waitForStartup();

            FlatFileTrancheServer ffts = null;

            try {

                dataChunksToDelete = new DiskBackedBigHashList();
                metaDataChunksToDelete = new DiskBackedBigHashList();

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

                if (hostsToDeprioritize.size() > 0) {
                    System.out.println("The following server" + (hostsToDeprioritize.size() == 1 ? "" : "s") + " will only be used when necessary:");
                    for (String host : hostsToDeprioritize) {
                        System.out.println("    * " + host);
                    }
                }

                System.out.println("Waiting for data directories to load...");
                final long start = System.currentTimeMillis();

                ffts.waitToLoadExistingDataBlocks();

                System.out.println("... finished waiting for data directories to load, took: " + TextUtil.formatTimeLength(System.currentTimeMillis() - start));

                long dataChunkCount = 0, metaChunkCount = 0;

                BigInteger offset = BigInteger.ZERO;
                final BigInteger batchSizeBigInt = BigInteger.valueOf(batchSize);

                while (true) {
                    BigHash[] hashes = ffts.getDataHashes(offset, batchSizeBigInt);

                    if (hashes.length == 0) {
                        break;
                    }

                    dataChunkCount += hashes.length;
                    offset = offset.add(batchSizeBigInt);
                }

                offset = BigInteger.ZERO;

                while (true) {
                    BigHash[] hashes = ffts.getMetaDataHashes(offset, batchSizeBigInt);

                    if (hashes.length == 0) {
                        break;
                    }

                    metaChunkCount += hashes.length;
                    offset = offset.add(batchSizeBigInt);
                }

                System.out.println("Found a total of " + dataChunkCount + " data chunk(s) and " + metaChunkCount + " meta chunk(s).");

                System.out.println("Injecting meta data chunks:");

                offset = BigInteger.ZERO;
                {
                    long metaDataOffset = getSavedMetaDataBatch();

                    if (metaDataOffset > 0) {
                        System.out.println("    Resuming meta data at batch number: " + metaDataOffset);
                        offset = BigInteger.valueOf(metaDataOffset);
                    }
                }

                META_DATA_CHUNKS:
                while (true) {
                    BigHash[] hashes = ffts.getMetaDataHashes(offset, batchSizeBigInt);

                    if (hashes.length == 0) {
                        break META_DATA_CHUNKS;
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
                            System.err.println("UnexpectedEndOfDataBlockException for meta data chunk <" + hash + "> when reading from DDC, skipping: " + ueodbe.getMessage());
                            continue;
                        } catch (FileNotFoundException ffne) {
                            // Don't cry over spilt milk
                            System.err.println("FileNotFoundException for meta data chunk <" + hash + "> when reading from DDC, skipping: " + ffne.getMessage());
                            continue;
                        }
                    }

                    System.out.println("    * Finished meta data chunks " + offset.longValue() + "-" + offset.add(batchSizeBigInt).longValue() + " of " + metaChunkCount);

                    setSavedMetaDataBatch(offset.longValue());

                    offset = offset.add(batchSizeBigInt);
                }

                System.out.println("Injecting data chunks:");

                offset = BigInteger.ZERO;
                {
                    long dataOffset = getSavedDataBatch();

                    if (dataOffset > 0) {
                        System.out.println("    Resuming data at batch number: " + dataOffset);
                        offset = BigInteger.valueOf(dataOffset);
                    }
                }

                DATA_CHUNKS:
                while (true) {
                    BigHash[] hashes = ffts.getDataHashes(offset, batchSizeBigInt);

                    if (hashes.length == 0) {
                        break DATA_CHUNKS;
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
                            System.err.println("UnexpectedEndOfDataBlockException for data chunk <" + hash + "> when reading from DDC, skipping: " + ueodbe.getMessage());
                            // Don't cry over spilt milk
                            continue;
                        } catch (FileNotFoundException ffne) {
                            // Don't cry over spilt milk
                            System.err.println("FileNotFoundException for data chunk <" + hash + "> when reading from DDC, skipping: " + ffne.getMessage());
                            continue;
                        }
                    }

                    System.out.println("    * Finished data chunks " + offset.longValue() + "-" + offset.add(batchSizeBigInt).longValue() + " of " + dataChunkCount);
                    setSavedDataBatch(offset.longValue());

                    offset = offset.add(batchSizeBigInt);
                }

                for (Thread t : threads) {
                    t.join();
                }

                System.out.println("Stats:");
                System.out.println("    * Skipped: " + injectionSkippedCount);
                System.out.println("    * Success: " + injectionSuccessCount);

                if (!isDeleteInjectedChunksMode) {
                    System.out.println("   (* Any failures are critical errors, and so there were no failed chunks if you read this.)");
                } else {
                    System.out.println("    * Failures: " + injectionFailedCount);
                }

                // Delete everything to delete
                safeDeleteChunks(metaDataChunksToDelete, true, ffts);
                safeDeleteChunks(dataChunksToDelete, false, ffts);

            } finally {
                IOUtil.safeClose(ffts);
            }

            

        } catch (Exception e) {
            System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
            printUsage(System.err);
            exit(2);
        } finally {
            IOUtil.safeClose(dataChunksToDelete);
            IOUtil.safeClose(metaDataChunksToDelete);
        }

        System.out.println("~ fin ~");
        // For "delete on success" mode, might still have had failure. Only exit normally if no failures.
        if (!isFailure[0]) {
            exit(0);
        } else {
            exit(5);
        }
    }

    /**
     * 
     * @param code
     */
    private static void exit(int code) {
        IOUtil.safeDelete(replicationLockFile);
        System.exit(code);
    }

    /**
     * 
     * @return
     */
    private static long getSavedDataBatch() throws IOException {
        return getSavedNumber(dataSaveFile);
    }

    /**
     * 
     * @return
     */
    private static long getSavedMetaDataBatch() throws IOException {
        return getSavedNumber(metaDataSaveFile);
    }

    /**
     * 
     * @param f
     * @return
     */
    private static long getSavedNumber(File f) throws IOException {

        // If file not saved yet or in "delete on success" mode, start at beginning
        if (!f.exists() || isDeleteInjectedChunksMode) {
            return 0;
        }

        InputStream in = null;

        try {
            in = new BufferedInputStream(new FileInputStream(f));

            return IOUtil.readLong(in);
        } finally {
            IOUtil.safeClose(in);
        }
    }

    /**
     * 
     * @param offset
     */
    private static void setSavedDataBatch(long offset) throws IOException {
        setSavedNumber(offset, dataSaveFile);
    }

    /**
     * 
     * @param offset
     */
    private static void setSavedMetaDataBatch(long offset) throws IOException {
        setSavedNumber(offset, metaDataSaveFile);
    }

    /**
     * 
     * @param offset
     * @param f
     * @throws java.io.IOException
     */
    private static void setSavedNumber(long offset, File f) throws IOException {

        // Don't save in this mode; meaningless
        if (isDeleteInjectedChunksMode) {
            return;
        }

        OutputStream writer = null;

        try {
            writer = new BufferedOutputStream(new FileOutputStream(f, false));
            IOUtil.writeLong(offset, writer);
        } finally {
            IOUtil.safeClose(writer);
        }
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

    static enum Status {

        Injected, Failed, Skipped
    };

    private static void addChunkToDelete(BigHash hash, boolean isMetaData) {
        if (isMetaData) {
            metaDataChunksToDelete.add(hash);
        } else {
            dataChunksToDelete.add(hash);
        }
    }

    /**
     *
     * @param list
     * @param isMetaData
     */
    private static void safeDeleteChunks(DiskBackedBigHashList list, boolean isMetaData, FlatFileTrancheServer ffts) {
        System.out.println("Total " + (isMetaData ? "meta data" : "data") + " chunks to delete: " + list.size());
        Iterator<BigHash> it = list.iterator();

        while (it.hasNext()) {
            BigHash hash = it.next();
            safeDeleteChunk(hash, isMetaData, ffts);
        }
    }

    /**
     * <p>Attempts to delete chunk, and prints error message to stderr if fails, but does not throw exceptions.</p>
     * @param hash
     * @param isMetaData
     * @param ffts
     */
    private static void safeDeleteChunk(BigHash hash, boolean isMetaData, FlatFileTrancheServer ffts) {
        try {
            if (isMetaData) {
                ffts.getDataBlockUtil().deleteMetaData(hash, "Delete because injected or skipped.");
            } else {
                ffts.getDataBlockUtil().deleteData(hash, "Delete because injected or skipped.");
            }
        } catch (Exception e) {
            System.err.println(e.getClass().getSimpleName() + " occured while trying to delete " + (isMetaData ? "meta data" : "data") + " chunk: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    /**
     * 
     * @param bytes
     * @param hash
     * @param isMetaData
     */
    private static void injectChunk(final byte[] bytes, final BigHash hash, final boolean isMetaData, final UserZipFile uzf) {

        final boolean isDeleteOnSuccess = isDeleteInjectedChunksMode;

        // If chunk is corrupted or invalid, skip it. Remote server will reject it anyhow.
        if (!isValidChunk(bytes, hash, isMetaData)) {
            addChunkToDelete(hash, isMetaData);
            return;
        }

        Thread t = new Thread() {

            @Override()
            public void run() {

                final String chunkType = isMetaData ? "meta data" : "data";

                // Give it a lot of chances. Even if it takes 30 minutes to fail, not a big deal -- better
                // than dying due to temporary problem!
                final int MAX_ATTEMPTS = 30;

                ATTEMPT:
                for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {

                    Status s = performInjection(bytes, hash, isMetaData, uzf);

                    boolean wasSuccess = s.equals(Status.Injected) || s.equals(Status.Skipped);

                    if (isDeleteOnSuccess && wasSuccess) {
                        addChunkToDelete(hash, isMetaData);
                    }

                    switch (s) {

                        case Injected:
                            synchronized (countLock) {
                                injectionSuccessCount++;
                            }
                            if (isVerbose) {
                                System.out.println("INJECTED: " + chunkType + " chunk: " + hash);
                            }
                            return;

                        case Skipped:
                            synchronized (countLock) {
                                injectionSkippedCount++;
                            }
                            if (isVerbose) {
                                System.out.println("SKIPPED: " + chunkType + " chunk: " + hash);
                            }
                            return;

                        case Failed:
                            System.err.println("FAILED: Attempt #" + attempt + " of " + MAX_ATTEMPTS + " for " + chunkType + " chunk: " + hash);

                            try {
                                // Sleep a bit. Hope the problem goes away!
                                Thread.sleep(60 * 1000);
                            } catch (InterruptedException nope) { /* continue */ }

                            continue ATTEMPT;

                        default:
                            System.err.println("Unrecognized status flag: " + s);
                    }
                }

                // If gets here, failed.
                isFailure[0] = true;
                synchronized (countLock) {
                    injectionFailedCount++;
                }
            }

            /**
             * 
             */
            private Status performInjection(final byte[] bytes, final BigHash hash, final boolean isMetaData, final UserZipFile uzf) {
                final String chunkType = isMetaData ? "meta data" : "data";

                int injectionCount = 0;
                List<String> hostsToUse = new LinkedList();

                SERVERS_TO_USE:
                for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {
                    if (row.isOnline() && row.isWritable() && row.isReadable() && row.isCore()) {

                        boolean isTarget = false;
                        for (HashSpan hs : row.getTargetHashSpans()) {
                            if (hs.contains(hash)) {
                                isTarget = true;
                                break;
                            }
                        }

                        if (isTarget) {
                            hostsToUse.add(row.getHost());
                        }
                    }
                }

                if (hostsToUse.size() < requiredCopies) {
                    String msg = "Require " + requiredCopies + " cop(ies) of chunk, but only " + hostsToUse.size() + " host(s) available.";
                    System.err.println(msg);
                    return Status.Failed;
                }

                Collections.shuffle(hostsToUse);

                if (hostsToDeprioritize.size() > 0) {
                    Set<String> hostsToMoveToEnd = new HashSet();
                    for (String host : hostsToDeprioritize) {
                        if (hostsToUse.contains(host)) {
                            hostsToMoveToEnd.add(host);
                        }
                    }
                    hostsToUse.removeAll(hostsToMoveToEnd);
                    hostsToUse.addAll(hostsToMoveToEnd);
                }

                Set<String> hostsToRemove = new HashSet();

                SERVERS_WITH_COPIES:
                for (String host : hostsToUse) {
                    final boolean isConnected = ConnectionUtil.isConnected(host);
                    TrancheServer ts = null;
                    try {
                        if (!isConnected) {
                            ts = ConnectionUtil.connectHost(host, true);
                        } else {
                            ts = ConnectionUtil.getHost(host);
                        }

                        boolean contains = false;
                        if (isMetaData) {
                            contains = IOUtil.hasMetaData(ts, hash);
                        } else {
                            contains = IOUtil.hasData(ts, hash);
                        }

                        if (contains) {
                            injectionCount++;
                            hostsToRemove.add(host);

                            // If found enough copies online already, bail
                            if (injectionCount >= preferredCopies) {
                                return Status.Skipped;
                            }
                        }

                    } catch (Exception e) {
                        System.err.println(e.getClass().getSimpleName() + " occurred while injecting " + chunkType + " chunk to " + host + ": " + e.getMessage());
                        e.printStackTrace(System.err);
                        hostsToRemove.add(host);
                    } finally {
                        if (!isConnected) {
                            ConnectionUtil.unlockConnection(host);
                            ConnectionUtil.safeCloseHost(host);
                        }
                    }
                }

                // Remove everything with copy of chunk or problem
                hostsToUse.removeAll(hostsToRemove);

                INJECTIONS:
                for (String host : hostsToUse) {

                    final boolean isConnected = ConnectionUtil.isConnected(host);
                    TrancheServer ts = null;
                    try {
                        if (!isConnected) {
                            ts = ConnectionUtil.connectHost(host, true);
                        } else {
                            ts = ConnectionUtil.getHost(host);
                        }

                        boolean isSuccess = false;

                        if (isMetaData) {
                            IOUtil.setMetaData(ts, uzf.getCertificate(), uzf.getPrivateKey(), true, hash, bytes);
                            isSuccess = IOUtil.hasMetaData(ts, hash);
                        } else {
                            IOUtil.setData(ts, uzf.getCertificate(), uzf.getPrivateKey(), hash, bytes);
                            isSuccess = IOUtil.hasData(ts, hash);
                        }

                        // Verify server has chunk 
                        if (isSuccess) {
                            injectionCount++;

                            // If enough copies online now, bail
                            if (injectionCount >= preferredCopies) {
                                return Status.Injected;
                            }
                        }

                    } catch (Exception e) {
                        System.err.println(e.getClass().getSimpleName() + " occurred while injecting " + chunkType + " chunk to " + host + ": " + e.getMessage());
                        e.printStackTrace(System.err);
                    } finally {
                        if (!isConnected) {
                            ConnectionUtil.unlockConnection(host);
                            ConnectionUtil.safeCloseHost(host);
                        }
                    }
                }

                // If get here, didn't get number of preferred copies. Need to check if at least met required count.
                if (injectionCount < requiredCopies) {
                    return Status.Failed;
                } else {
                    return Status.Injected;
                }
            }
        };
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();

        threads.add(t);

        // Lazily remove finished threads when limit is met
        while (threads.size() >= threadCountLimit) {

            Set<Thread> toRemove = new HashSet();

            for (Thread next : threads) {
                if (!next.isAlive()) {
                    toRemove.add(next);
                }
            }

            threads.removeAll(toRemove);

            // Sleep to avoid spiking processor

            if (threads.size() >= threadCountLimit) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) { /* */ }
            }
        }

        // If there is a failure, wait for all submitted jobs to finish, then fail
        final int timeout = 20 * 1000;
        if (isFailure[0] && !isDeleteInjectedChunksMode) {
            for (Thread nextT : threads) {
                try {
                    nextT.join(timeout);
                } catch (Exception e) { /* nope */ }
            }

            System.err.println("Failure of a chunk to inject is a critical error. Aborting. Note that application saves state, so restart the application.");
            exit(5);
        }
    }

    /**
     * 
     * @param bytes
     * @param hash
     * @param isMetaData
     * @return
     */
    private static boolean isValidChunk(final byte[] bytes, final BigHash hash, final boolean isMetaData) {

        try {
            if (isMetaData) {

                MetaData md = MetaData.createFromBytes(bytes);

                if (md != null) {
                    return true;
                }
            } else {
                BigHash verifyHash = new BigHash(bytes);

                // Bytes should match
                if (verifyHash.equals(hash)) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Nope
        }

        return false;
    }

    /**
     * 
     * @param out
     */
    public static void printUsage(PrintStream out) {
        out.println();
        out.println("USAGE");
        out.println("   InjectDataFromDataBlocksScript [OPTIONAL PARAMETERS] [ONE OF TWO PAIR OF AUTHENTICATION PARAMETERS] -d <data directory> [-d <data directory>]");
        out.println();
        out.println("DESCRIPTION");
        out.println("   Injects chunks from one or more data directory. Behavior on failure depends on modes. See MODES for more information on behavior on failure.");
        out.println();
        out.println("MODES");
        out.println("   In the default mode, if tool cannot inject data or meta data chunk, it prints out hash and type of failed chunk to standard output and wait for running tasks to complete, then exit with error code. (See EXIT CODES for more information.)");
        out.println();
        out.println("   In \"delete on success\" mode, if tool cannot inject data or meta data chunk, it prints out hash and type of failed chunk. However, it will continue to run until it has attempted everything. After completed, it will return with normal exit code if no failures, else exit with error code. (See EXIT CODES for more information.)");
        out.println();
        out.println("SPECIAL FILES: SAVE FILES, LOCK FILES");
        out.println("   In the default mode, the tool will save its state so that if the tool fails or is stopped, it will resume at the same point. See: " + metaDataSaveFile.getAbsolutePath() + " and " + dataSaveFile.getAbsolutePath() + " for more information.");
        out.println();
        out.println("   (In \"delete on success\" mode, these files are ignored, since their index is meaningless when chunks are deleted.)");
        out.println();
        out.println("   Also, if the the lock file (" + replicationLockFile.getName() + ") is found in the tool's working directory, the tool will not start. This is done to prevent multiple instances from operating on the same DDCs. You can manually delete this file if you believe that another instance is not running.");
        out.println();
        out.println("REQUIRED PARAMETERS");
        out.println("   -d      Value: path to data block");
        out.println();
        out.println("Also, one of the following two pairs are required:");
        out.println("   -u      Value: username for log in");
        out.println("   -p      Value: password for log in");
        out.println();
        out.println("   -U      Value: path to user zip file");
        out.println("   -P      Value: passphrase for user zip file");
        out.println();
        out.println("OPTIONAL PARAMETERS");
        out.println("   -t      Value: positive number      The maximum number of threads to use concurrently. Default is " + DEFAULT_THREAD_COUNT_LIMIT + ".");
        out.println("   -f      Value: preferred copies     The preferred number of servers to receive copy of chunk. Note that script will try to get this, but will only fail if less than required number of chunks. The default is " + DEFAULT_PREFERRED_COPIES + ".");
        out.println("   -r      Value: required copies      The required number of servers that must have the chunk or else injection failed. The default is " + DEFAULT_REQUIRED_COPIES + ".");
        out.println("   -v      Value: true or false        Print out verbose information. Default is " + DEFAULT_IS_VERBOSE + ".");
        out.println("   -B      Value: postive number       Batch size. Tool prints progress after each batch. Default is " + DEFAULT_BATCH_SIZE + ".");
        out.println("   -S      Value: host name            If any hosts are particularly slow but you want to use them if others not available, this will de-prioritize them if they are being used.");
        out.println();
        out.println("MODES");
        out.println("   -D      Value: true of false        Switches to \"delete on success\" mode. Delete chunks that are successfully injected. Default is " + DEFAULT_DELETE_INJECTED_CHUNKS + ".");
        out.println();
        out.println("EXIT CODES");
        out.println("   0: Exit normally");
        out.println("   1: Missing parameters/wrong number of parameters");
        out.println("   2: Unknown error (see standard error)");
        out.println("   3: Problem with parameters");
        out.println("   4: Lock exists (check if another instance is running and, if not, delete the lock at specified location)");
        out.println("   5: Failed to inject required copies for chunk.");
        out.println();
    }
}


