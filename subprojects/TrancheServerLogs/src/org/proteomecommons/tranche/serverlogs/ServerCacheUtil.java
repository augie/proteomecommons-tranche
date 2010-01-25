package org.proteomecommons.tranche.serverlogs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.proteomecommons.email.EmailUtil;
import org.proteomecommons.tranche.serverlogs.scripts.SafeMigrateScript;
import org.tranche.gui.caches.ProjectCache;
import org.tranche.hash.Base16;
import org.tranche.hash.BigHash;
import org.tranche.hash.DiskBackedHashList;
import org.tranche.hash.SimpleDiskBackedBigHashList;
import org.tranche.server.logs.ActionByte;
import org.tranche.server.logs.LogEntry;
import org.tranche.server.logs.LogReader;
import org.tranche.servers.ServerUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.Text;

/**
 * <p>Manages incoming logs -- parsing them and placing them in appropriate cache.</p>
 * <p>Also helps manage raw server logs.</p>
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 */
public class ServerCacheUtil {

    // -[START CONFIGURATION]- (OK TO CHANGE)
    // Only change items in this section. If you need to add paths for
    // new environments, do so above w/o removing. Select env paths
    // below.
    /**
     * Set to false to not print out tracers.
     */
    private static final boolean isPrintTimesIncomingLogs = false;
    /**
     * Prints times to standard out for processing download times.
     */
    private static final boolean isPrintTimesFindProjectDownloads = false;
    /**
     * Turn debug tracers on or off
     */
    private static final boolean isDebug = false;
    /**
     * Simply create a new build environment below
     */
    private static final BuildEnvironment PCBuild = BuildEnvironment.createEnvironment("PC.org build environment");
    private static final BuildEnvironment BryanMacBuild = BuildEnvironment.createEnvironment("Bryan's iBook environment");
    /**
     * Set the build environment! Change this if building in different environment.
     */
    private static final BuildEnvironment BUILD = PCBuild;

    /**
     * Set the parameters for any build environment
     */
    static {
        // PC.org
        PCBuild.setServerLogPath("/media/sdc1");
        PCBuild.setServerCachePath("/opt/tomcat5/db/server_logs_cache");
        PCBuild.setProjectDownloadPath("/media/sdc1/project_downloads_logs");
        PCBuild.setReplicationCSVPath("/opt/tomcat5/webapps/proteomecommons/WEB-INF/all-projects-replications.csv");
        PCBuild.setUrlPathPrefix("http://tranche.proteomecommons.org/activity/");
        PCBuild.setStressTestLogPath("/opt/tomcat5/webapps/proteomecommons/stress/logs");

        // Bryan's iBook
        BryanMacBuild.setServerLogPath("/Users/besmit/Ephemeral/ServerLogsWebEnvironment/server_logs");
        BryanMacBuild.setServerCachePath("/Users/besmit/Ephemeral/ServerLogsWebEnvironment/server_logs_cache");
        BryanMacBuild.setProjectDownloadPath("/Users/besmit/Ephemeral/ServerLogsWebEnvironment/project_download_logs");
        BryanMacBuild.setReplicationCSVPath("/Users/besmit/Ephemeral/ServerLogsWebEnvironment/all-projects-replications.csv");
        BryanMacBuild.setUrlPathPrefix("http://localhost:8084/TrancheServerLogs/");
        BryanMacBuild.setStressTestLogPath("/Users/besmit/Ephemeral/ServerLogsWebEnvironment/stress_test_logs/");

        // SET VARIABLES FOR NEW ENVIRONMENTS BELOW


        // Lastly, print out current build environment to aid with debugging
        System.out.println("Using build environment \"" + BUILD.getEnvironmentName() + "\" at " + Text.getFormattedDate(System.currentTimeMillis()));
    }
//    private final static String PC_SERVER_LOG_PATH = "/media/hdc1",
//            BRYAN_MAC_SERVER_LOG_PATH = "/Users/besmit/ServerLogsWebEnvironment/server_logs";
//    private final static String PC_SERVER_CACHE_PATH = "/opt/tomcat5/db/server_logs_cache",
//            BRYAN_MAC_SERVER_CACHE_PATH = "/Users/besmit/ServerLogsWebEnvironment/server_logs_cache";
//    private final static String PC_PROJECT_DOWNLOAD_LOGS_PATH = "/media/hdc1/project_downloads_logs",
//            BRYAN_MAC_PROJECT_DOWNLOAD_LOGS_PATH = "/Users/besmit/ServerLogsWebEnvironment/project_download_logs";
//    private final static String CSV_FILE_WITH_ALL_PROJECTS_REPLICATIONS_PC_ORG = "/opt/tomcat5/webapps/proteomecommons/WEB-INF/all-projects-replications.csv";
    /**
     * DON'T CHANGE. TO CHANGE VALUE, CREATE A BUILD ENVIRONMENT AND SET
     * VARIABLE "BUILD" TO THAT ENVIRONMENT.
     * 
     * DON'T DELETE EXISTING ENVIRONMENTS. CREATE A NEW ENVIRONMENT AND
     * SET "BUILD" TO THAT ENVIRONMENT.
     */
    private final static String SERVER_LOG_PATH = BUILD.getServerLogPath();
    /**
     * DON'T CHANGE. TO CHANGE VALUE, CREATE A BUILD ENVIRONMENT AND SET
     * VARIABLE "BUILD" TO THAT ENVIRONMENT.
     * 
     * DON'T DELETE EXISTING ENVIRONMENTS. CREATE A NEW ENVIRONMENT AND
     * SET "BUILD" TO THAT ENVIRONMENT.
     */
    private final static String SERVER_CACHE_PATH = BUILD.getServerCachePath();
    /**
     * DON'T CHANGE. TO CHANGE VALUE, CREATE A BUILD ENVIRONMENT AND SET
     * VARIABLE "BUILD" TO THAT ENVIRONMENT.
     * 
     * DON'T DELETE EXISTING ENVIRONMENTS. CREATE A NEW ENVIRONMENT AND
     * SET "BUILD" TO THAT ENVIRONMENT.
     */
    private final static String PROJECT_DOWNLOAD_LOGS_PATH = BUILD.getProjectDownloadPath();
    /**
     * DON'T CHANGE. TO CHANGE VALUE, CREATE A BUILD ENVIRONMENT AND SET
     * VARIABLE "BUILD" TO THAT ENVIRONMENT.
     * 
     * DON'T DELETE EXISTING ENVIRONMENTS. CREATE A NEW ENVIRONMENT AND
     * SET "BUILD" TO THAT ENVIRONMENT.
     */
    private final static String CSV_FILE_WITH_ALL_PROJECTS_REPLICATIONS = BUILD.getReplicationCSVPath();
    /**
     * DON'T CHANGE. TO CHANGE VALUE, CREATE A BUILD ENVIRONMENT AND SET
     * VARIABLE "BUILD" TO THAT ENVIRONMENT.
     * 
     * DON'T DELETE EXISTING ENVIRONMENTS. CREATE A NEW ENVIRONMENT AND
     * SET "BUILD" TO THAT ENVIRONMENT.
     */
    private final static String STRESS_TEST_LOGS_PATH = BUILD.getStressTestLogPath();
    // -[END CONFIGURATION]-
    /**
     * <p>Single read/write lock.</p>
     */
    private static final Object readWriteLock = new Object();
    private static final int LONG_BYTES = 8;
    /**
     * <p>Size, in bytes, of one record.</p>
     * <p>Note that a record contains the timestamp, data uploaded and data uploaded -- three longs.</p>
     */
    private static final int RECORD_BYTES = LONG_BYTES * 3;
    /**
     * <p>Directory holding all server log directories. May be periodically trimmed.</p>
     */
    public static final File serverLogDirectory = new File(SERVER_LOG_PATH);
    /**
     * <p>Directory holding all server cache directories.</p>
     */
    public static final File serverCacheDirectory = new File(SERVER_CACHE_PATH);
    /**
     * <p>Directory holding all of the project download logs.</p>
     */
    public static final File projectDownloadLogsDirectory = new File(PROJECT_DOWNLOAD_LOGS_PATH);
    /**
     * CSV file with all replications for each project
     */
    public static final File csvFileWithAllProjectsReplications = new File(CSV_FILE_WITH_ALL_PROJECTS_REPLICATIONS);
    /**
     *
     */
    private static final DiskBackedHashList servicedLogs = null;
    /**
     *
     */
    private static boolean isLazyLoaded = false;
    /**
     * Stores persistent collection of BigHashes in binary format.
     */
    public static final File persistentHashListFile = new File(serverLogDirectory, "log.hashes");
    /**
     * Holds collection of BigHashes representing all logs ever received.
     */
    private static SimpleDiskBackedBigHashList logHashes = null;
    /**
     * <p>Holds reference to log file paths queued for parsing and storage.</p>
     * <p>Contains paths <String> instead of File to avoid excessive file handles.</p>
     */
    private static List logFileQueue = null;
    /**
     * <p>Maps IP or URL of server to log file.</p>
     */
    private static Map logFileQueueMapToIP = null;
    /**
     * <p>All records start on this timestamp.</p>
     */
    public static final long startingTimestamp = (long) 1195094616014L;
//    private static final long startingTimestamp = (long) 9223372036854775807L;
    /**
     *
     */
    private static void lazyLoad() {
        if (isLazyLoaded) {
            return;
        }
        isLazyLoaded = true;

        // Queue logFileQueue
        logFileQueue = new LinkedList();
        logFileQueueMapToIP = new HashMap();

        // Create a persistent collection of received log hashes
        if (!persistentHashListFile.exists()) {
            try {
                persistentHashListFile.createNewFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        logHashes = new SimpleDiskBackedBigHashList(persistentHashListFile);

        // Start a queue processing thread
        new LogFileProcessorThread().start();

        // Run backup script. Safe if nothing to do.
        SafeMigrateScript.main(new String[0]);
    }

    /**
     * 
     * @param log
     */
    public static void processNewStressTestLog(final File log) throws Exception {
        try {
            File persistentStorage = new File(ServerCacheUtil.getStressTestLogsDirectoryPath(), log.getName());
            IOUtil.copyFile(log, persistentStorage);

            Thread t = new Thread("Email thread") {

                public void run() {
                    try {
                        StringBuffer message = new StringBuffer();

                        message.append("A new stress test report was submitted:\n\n");
                        message.append("http://tranche.proteomecommons.org/activity/stress/test.jsp?log=" + log.getName() + "\n\n");

                        String subject = "New stress test results";

                        String[] toEmailAddresses = {
                            "proteomecommons-tranche-dev@googlegroups.com"
                        };

                        EmailUtil.sendEmail(subject, toEmailAddresses, message.toString());

                    } catch (Exception ex) {
                    // nope
                    }
                }
            };
            t.setDaemon(true);
            t.setPriority(t.MIN_PRIORITY);
            t.start();
        } finally {
            IOUtil.safeDelete(log);
        }
    }

    /**
     * 
     * @return
     */
    public static String getStressTestLogsDirectoryPath() {
        return STRESS_TEST_LOGS_PATH;
    }

    /**
     * <p>Check whether a log file has already processed.</p>
     * <p>Automatically checks when enqueuing. Just used to check.</p>
     */
    public static boolean containsLogInPersistentList(String logPath) {
        lazyLoad();

        synchronized (logHashes) {
            File logFile = new File(logPath);
            try {
                return logHashes.contains(new BigHash(IOUtil.getBytes(logFile)));
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        // Compiler happy
        return false;
    }

    /**
     * <p>Add a log to collection of hashes for processed logs.</p>
     */
    private static void addLogToPersistentList(String logPath) {
        lazyLoad();

        synchronized (logHashes) {
            File logFile = new File(logPath);
            try {
                if (!containsLogInPersistentList(logPath)) {
                    logHashes.add(new BigHash(IOUtil.getBytes(logFile)));
                } else {
                    System.out.println("  Skipping, already add log at " + logPath);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * <p>Only called if problem adding the log so can perhaps add later.</p>
     */
    public static void removeLogFromPersistentList(String logPath) {
        lazyLoad();

        synchronized (logHashes) {
            File logFile = new File(logPath);
            try {
                logHashes.delete(new BigHash(IOUtil.getBytes(logFile)));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * <p>Enqueue a file to be logged.</p>
     * @param logFilePath Must be the absolute path to the tmp log file!
     * @param serverIP The IP (or URL) for the server submitting log.
     */
    public static void enqueueLogFile(String logFilePath, String serverIP) {
        lazyLoad();

        // Add as serviced so next request can't get thru.
        // Will be removed later if fails.
        addLogToPersistentList(logFilePath);

        // Keep a mapped reference to IP for processing purposed.
        logFileQueueMapToIP.put(logFilePath, serverIP);

        synchronized (logFileQueue) {
            logFileQueue.add(logFilePath);
        }
    }

    /**
     * <p>Returns the size of the queue. Only used by scripts that don't want to slow down server submissions.</p>
     */
    public static int queueSize() {
        lazyLoad();
        synchronized (logFileQueue) {
            return logFileQueue.size();
        }
    }

    /**
     * Utility method. Returns month for a timestamp
     * @return integer representation of calendar date. See Calendar.
     */
    public static int extractMonthFromTimestamp(long timestamp) {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date(timestamp));

        return c.get(Calendar.MONTH);
    }

    /**
     * Utility method. Returns month for a timestamp
     * @return integer representation of calendar date. See Calendar.
     */
    public static int extractYearFromTimestamp(long timestamp) {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date(timestamp));

        return c.get(Calendar.YEAR);
    }

    /**
     * <p>Converts an IP address or URL <String> to new string used for directories that store server's logs and cache.</p>
     * <p>E.g., 127.0.0.1:1500           -> 127-0-0-1-1500</p>
     * <p>E.g., tranche://127.0.0.1:1500 -> 127-0-0-1-1500</p>
     */
    public static String convertIPToSafeName(String ip) {
        StringBuffer logDirName = new StringBuffer();

        byte[] bytes = ip.getBytes();

        // Quick trick: if given a URL instead of IP, simply don't add anything
        // until first number is seen!
        boolean isFirstNumberSeen = false;

        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] >= '0' && bytes[i] <= '9') {
                logDirName.append((char) bytes[i]);
                isFirstNumberSeen = true;
            } else if (isFirstNumberSeen) {
                // Use a hyphen to replace every non-numeric value
                logDirName.append("-");
            }
        }

        return logDirName.toString();
    }
    // USED TO SEEK TO CORRECT SPOT ON RANDOM ACCESS FILE
    private static final int MINUTES_CACHE = 1000 * 60,  HOURS_CACHE = MINUTES_CACHE * 60,  DAYS_CACHE = HOURS_CACHE * 24;

    /**
     *
     */
    private static void parseLogFile(File logFile, String serverIP) throws Exception {
        lazyLoad();

        final long start = System.currentTimeMillis();
        long projectHashesCount = 0, nonProjectHashesCount = 0;

        int uploadCount = 0, downloadCount = 0;
        long count = 0;
        synchronized (readWriteLock) {

            LogReader reader = null;
            RandomAccessFile rasDaysCache = null,
                    rasMinutesCache = null,
                    rasHoursCache = null;

            // Will only happen once per server boot
            ProjectCache.waitForStartup();

            // No longer parse this information. See GetFileToolUtil.
//            // Build hash set of project hashes for quick look ups
//            Set projectHashes = new HashSet();
//
//            Iterator it = ProjectCacheUtil.getProjects().iterator();
//
//            ProjectFileSummary pfs;
//            while (it.hasNext()) {
//                pfs = (ProjectFileSummary) it.next();
//                projectHashes.add(pfs.projectHash);
//            }

            try {

                // Get the server's cache directory
                File thisServerCacheDir = new File(serverCacheDirectory, convertIPToSafeName(serverIP));
                if (!thisServerCacheDir.exists()) {
                    thisServerCacheDir.mkdirs();
                    if (!thisServerCacheDir.exists()) {
                        throw new IOException("Cannot make server cache directory: " + thisServerCacheDir.getAbsolutePath());
                    }
                }

                File minutesCache = new File(thisServerCacheDir, "minutes.cache");
                File hoursCache = new File(thisServerCacheDir, "hours.cache");
                File daysCache = new File(thisServerCacheDir, "days.cache");

                if (!daysCache.exists()) {
                    daysCache.createNewFile();
                    if (!daysCache.exists()) {
                        throw new Exception("Cannot make new file: " + daysCache.getAbsolutePath());
                    }
                }

                if (!minutesCache.exists()) {
                    minutesCache.createNewFile();
                    if (!minutesCache.exists()) {
                        throw new Exception("Cannot make new file: " + minutesCache.getAbsolutePath());
                    }
                }

                if (!hoursCache.exists()) {
                    hoursCache.createNewFile();
                    if (!hoursCache.exists()) {
                        throw new Exception("Cannot make new file: " + hoursCache.getAbsolutePath());
                    }
                }

                rasDaysCache = new RandomAccessFile(daysCache, "rw");
                rasMinutesCache = new RandomAccessFile(minutesCache, "rw");
                rasHoursCache = new RandomAccessFile(hoursCache, "rw");

                reader = new LogReader(logFile);
                LogEntry next;
                while (reader.hasNext()) {
                    count++;
                    next = reader.next();

                    // Has newest timestamp?
                    long timestamp = next.getTimestamp();

                    // Fairly efficient -- will initialize in
                    // advance so won't grow too often

                    initializeRASToDate(rasMinutesCache, MINUTES_CACHE, timestamp);
                    initializeRASToDate(rasHoursCache, HOURS_CACHE, timestamp);
                    initializeRASToDate(rasDaysCache, DAYS_CACHE, timestamp);

                    switch (next.getAction()) {
                        case ActionByte.IPv4_GetData:
                        case ActionByte.IPv6_GetData:

                        // GetFileToolUtil now registers downloads
//                            // Safely add entry if not already exist
//                            if (projectHashes.contains(next.getHash())) {
//                                projectHashesCount++;
//                                addProjectDownloadEntrySafe(next.getHash(), next.getTimestamp());
//                            } else {
//                                nonProjectHashesCount++;
//                            }

                        // Don't break -- spill over to meta...
                        case ActionByte.IPv4_GetMeta:
                        case ActionByte.IPv6_GetMeta:

                            long downloaded = next.getHash().getLength();

                            writeEntryToMinutesCache(rasMinutesCache, timestamp, 0, downloaded);
                            writeEntryToHoursCache(rasHoursCache, timestamp, 0, downloaded);
                            writeEntryToDaysCache(rasDaysCache, timestamp, 0, downloaded);

                            downloadCount++;

                            break;

                        case ActionByte.IPv4_GetNonce:
                        case ActionByte.IPv6_GetNonce:
                            long nonceBytes = 8;

                            writeEntryToMinutesCache(rasMinutesCache, timestamp, 0, nonceBytes);
                            writeEntryToHoursCache(rasHoursCache, timestamp, 0, nonceBytes);
                            writeEntryToDaysCache(rasDaysCache, timestamp, 0, nonceBytes);

                            downloadCount++;

                            break;

                        case ActionByte.IPv4_SetData:
                        case ActionByte.IPv4_SetMeta:
                        case ActionByte.IPv6_SetData:
                        case ActionByte.IPv6_SetMeta:
                            long uploaded = next.getHash().getLength();

                            writeEntryToMinutesCache(rasMinutesCache, timestamp, uploaded, 0);
                            writeEntryToHoursCache(rasHoursCache, timestamp, uploaded, 0);
                            writeEntryToDaysCache(rasDaysCache, timestamp, uploaded, 0);

                            uploadCount++;

                            break;

                        // Other actions are irrelevant
                    }
                }

            } finally {
                if (reader != null) {
                    reader.close();
                }
                if (rasDaysCache != null) {
                    rasDaysCache.close();
                }
                if (rasMinutesCache != null) {
                    rasMinutesCache.close();
                }
                if (rasHoursCache != null) {
                    rasHoursCache.close();
                }

                printTracer("Time to process log file: " + Text.getPrettyEllapsedTimeString(System.currentTimeMillis() - start) + "; total of " + count + " entries read; " + uploadCount + " uploads read, " + downloadCount + " downloads read.");

                if (isPrintTimesIncomingLogs) {
                    System.out.println("Time to process log file from " + serverIP + " with size " + Text.getFormattedBytes(logFile.length()) + ": " + Text.getPrettyEllapsedTimeString(System.currentTimeMillis() - start));
//                    System.out.println("  Total of " + projectHashesCount + " project hashes downloaded, " + nonProjectHashesCount + " other hashes downloaded. I.e., approximately " + Math.round((double) projectHashesCount / (projectHashesCount + nonProjectHashesCount)) + "% of downloaded hashes were projects (" + projectHashes.size() + " known projects at moment).");
                }
            }
        }
    }

    /**
     * <p>Initializes to zeros</p>
     * @param cacheType Either SECONDS_CACHE, MINUTES_CACHE, HOURS_CACHE
     */
    private static void initializeRASToDate(RandomAccessFile ras, int cacheType, long toTimestamp) throws Exception {

        final long start = System.currentTimeMillis();

        // Quick math to determine how much need to initialize
        final long oldLength = ras.length();

        // First, get difference b/w date and when starting
        long timeDeltaInMillisNeeded = toTimestamp - startingTimestamp;

        // Now see how far ras already initialized
        long entries = Math.round((double) oldLength / RECORD_BYTES);
        long timeDeltaInMillisActual = cacheType * entries;

        // Only if need to grow file
        if (timeDeltaInMillisNeeded > timeDeltaInMillisActual) {

            long discrepancyInMillis = timeDeltaInMillisNeeded - timeDeltaInMillisActual;

            long neededEntries = Math.round((double) discrepancyInMillis / cacheType);

            // To avoid overhead in recent future, add extra 10K entries
            neededEntries += 10000;

            // New length of file
            long newLength = oldLength + RECORD_BYTES * neededEntries;

            ras.setLength(newLength);

            long prevTimestamp = startingTimestamp;

            if (oldLength >= RECORD_BYTES) {
                ras.seek(oldLength - RECORD_BYTES);
                prevTimestamp = ras.readLong();
            }

            long nextTimestamp = prevTimestamp;

            // Seek to position to start initializing
            ras.seek(oldLength);
            for (int i = 0; i < neededEntries; i++) {
                nextTimestamp += cacheType;

                // Write in timestamp, download time and upload time
                ras.writeLong(nextTimestamp);
                ras.writeLong(0);
                ras.writeLong(0);
            }

            printTracer("Setting RAS from " + Text.getFormattedBytes(oldLength) + " to " + Text.getFormattedBytes(newLength) + ", adding " + neededEntries + " entries. Took: " + Text.getPrettyEllapsedTimeString(System.currentTimeMillis() - start));
        } else {
//            printTracer("RAS doesn't need to grow. Only need "+timeDeltaInMillisNeeded+" millis, already found "+timeDeltaInMillisActual+" millis.");
        }
    }

    /**
     *
     */
    private static void writeEntryToMinutesCache(RandomAccessFile ras, long timestamp, long uploaded, long downloaded) throws Exception {
        // First, get difference b/w date and when starting
        long timeDeltaInMillisNeeded = timestamp - startingTimestamp;

        // The zero-indexed based index of entry
        long index = (long) Math.floor((double) timeDeltaInMillisNeeded / MINUTES_CACHE);

        // What's the seek?
        final long seek = index * RECORD_BYTES;
        ras.seek(seek);

        // Read and assert the timestamp
        long readTimestamp = ras.readLong();

        // Assert timestamp is accurate
        if (readTimestamp > timestamp + MINUTES_CACHE || readTimestamp < timestamp - MINUTES_CACHE) {
            throw new DataAssertionException("Expecting " + timestamp + " +- " + MINUTES_CACHE + ", instead found " + readTimestamp);
        }

        long prevUploaded = Math.abs(ras.readLong());
        long prevDownloaded = Math.abs(ras.readLong());

        // Seek back to overwrite previous upload/download values!
        ras.seek(seek + 8);

        ras.writeLong(uploaded + prevUploaded);
        ras.writeLong(downloaded + prevDownloaded);
    }

    /**
     *
     */
    private static void writeEntryToHoursCache(RandomAccessFile ras, long timestamp, long uploaded, long downloaded) throws Exception {
        // First, get difference b/w date and when starting
        long timeDeltaInMillisNeeded = timestamp - startingTimestamp;

        // The zero-indexed based index of entry
        long index = (long) Math.floor((double) timeDeltaInMillisNeeded / HOURS_CACHE);

        // What's the seek?
        final long seek = index * RECORD_BYTES;

        ras.seek(seek);

        // Read and assert the timestamp
        long readTimestamp = ras.readLong();

        // Assert timestamp is accurate
        if (readTimestamp > timestamp + HOURS_CACHE || readTimestamp < timestamp - HOURS_CACHE) {
            throw new DataAssertionException("Expecting " + timestamp + " +- " + HOURS_CACHE + ", instead found " + readTimestamp);
        }

        long prevUploaded = Math.abs(ras.readLong());
        long prevDownloaded = Math.abs(ras.readLong());

        // Seek back to overwrite previous upload/download values!
        ras.seek(seek + 8);

        ras.writeLong(uploaded + prevUploaded);
        ras.writeLong(downloaded + prevDownloaded);
    }

    /**
     *
     */
    private static void writeEntryToDaysCache(RandomAccessFile ras, long timestamp, long uploaded, long downloaded) throws Exception {
        // First, get difference b/w date and when starting
        long timeDeltaInMillisNeeded = timestamp - startingTimestamp;

        // The zero-indexed based index of entry
        long index = (long) Math.floor((double) timeDeltaInMillisNeeded / DAYS_CACHE);

        // What's the seek?
        final long seek = index * RECORD_BYTES;

        ras.seek(seek);

        // Read and assert the timestamp
        long readTimestamp = ras.readLong();

        // Assert timestamp is accurate
        if (readTimestamp > timestamp + DAYS_CACHE || readTimestamp < timestamp - DAYS_CACHE) {
            throw new DataAssertionException("Expecting " + timestamp + " +- " + DAYS_CACHE + ", instead found " + readTimestamp);
        }

        long prevUploaded = Math.abs(ras.readLong());
        long prevDownloaded = Math.abs(ras.readLong());

        // Seek back to overwrite previous upload/download values!
        ras.seek(seek + 8);

        ras.writeLong(uploaded + prevUploaded);
        ras.writeLong(downloaded + prevDownloaded);
    }

//    /**
//     * <p>Writes data to provided cache file.</p>
//     * @param cacheFile The cache file
//     * @param cacheType Either SECONDS_CACHE, MINUTES_CACHE, HOURS_CACHE
//     * @param startTimestamp
//     * @param bytesUploaded
//     * @param bytesDownloaded
//     */
//    private static void writeDataToCacheFile(File cacheFile, int cacheType, long startTimestamp, long bytesUploaded, long bytesDownloaded) throws Exception {
//
//
//        try {
//
//        } finally {
//
//        }
//    }
//}
    /**
     *
     */
    private static void copyLogToPersistentStorage(File logFile, String serverIP) throws Exception {
        lazyLoad();

        // Make the server directory if need to
        File serverDir = new File(serverLogDirectory, convertIPToSafeName(serverIP));
        if (!serverDir.exists()) {
            serverDir.mkdirs();
            if (!serverDir.exists()) {
                throw new Exception("Cannot create directory for server's logs: " + serverDir.getAbsolutePath());
            }
        }

        // If log file is already in the server log directory, ignore:
        // we're building cache or something scriptable!
        if (logFile.getParent().equals(serverDir.getName())) {
            return;
        }

        try {
            // Check for IP file. If doesn't exist, create it.
            File ipFile = new File(serverDir, "IP");
            BufferedWriter ipFileWriter = null;
            try {
                if (!ipFile.exists()) {
                    ipFile.createNewFile();
                    ipFileWriter = new BufferedWriter(new FileWriter(ipFile, false));
                    ipFileWriter.write(serverIP);
                }
            } catch (Exception ex) {
                System.err.println("Can't write ip file: " + ex.getMessage());
                ex.printStackTrace(System.err);
            } finally {
                if (ipFileWriter != null) {
                    // Safe flush and close
                    IOUtil.safeClose(ipFileWriter);
                }
            }

            // Write the new log!
            File perminentLog = new File(serverDir, System.currentTimeMillis() + ".log");
            IOUtil.copyFile(logFile, perminentLog);

        } finally {
            // Delete the tmp log file: we're done with it!
            IOUtil.safeDelete(logFile);
        }
    }

    /**
     *
     */
    public static Map getMostRecentDownloadedByMinutes(String serverIP, int entries) {

        lazyLoad();

//        synchronized(readWriteLock) {

        File serverCacheDir = new File(serverCacheDirectory, convertIPToSafeName(serverIP));
        File cacheFile = new File(serverCacheDir, "minutes.cache");

        // Calculate the starting timestamp
        final long timestamp = System.currentTimeMillis() - (long) entries * MINUTES_CACHE;

        RandomAccessFile ras = null;
        try {
            // If cache file doesn't exist, no data transfer!
            if (!cacheFile.exists()) {
                Map values = new HashMap();
                long nextTimestamp = timestamp;
                for (int i = 0; i < entries; i++) {
                    values.put(Long.valueOf(nextTimestamp), Long.valueOf(0));
                    nextTimestamp += MINUTES_CACHE;
                }
                return values;
            }

            // Quick assertion: expect file to be even multiple of RECORD_BYTES
            double totalEntries = (double) cacheFile.length() / RECORD_BYTES;
            double flooredTotalEntries = Math.floor(totalEntries);
            if (totalEntries != flooredTotalEntries) {
                throw new DataAssertionException("Expected file to be multiple of " + RECORD_BYTES + ", instead found " + cacheFile.length() + " for: " + cacheFile.exists());
            }

//                ras = new RandomAccessFile(cacheFile,"r");
//
//                long seek = ras.length() - entries*RECORD_BYTES;
//                ras.seek(seek);
//
//                // We need to find timestamp [entries] entries back
//                long timestamp = ras.readLong();
//                ras.close();

            return getDownloadedByMinutes(serverIP, timestamp, entries);
        } catch (DataAssertionException dae) {
            throw dae;
        } catch (Exception ex) {
            // Expected if records don't exist
            Map values = new HashMap();
            long nextTimestamp = timestamp;
            for (int i = 0; i < entries; i++) {
                values.put(Long.valueOf(nextTimestamp), Long.valueOf(0));
                nextTimestamp += MINUTES_CACHE;
            }
            return values;
        } finally {
            if (ras != null) {
                try {
                    ras.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
//        } // synch
    }

    /**
     *
     */
    public static Map getMostRecentDownloadedByMinutesAggregate(int entries) {
        return _getMostRecentDownloadedByMinutesAll(false, entries);
    }

    /**
     *
     */
    public static Map getMostRecentDownloadedByMinutesCore(int entries) {
        return _getMostRecentDownloadedByMinutesAll(true, entries);
    }

    /**
     * 
     * @param isJustCore
     * @param entries
     * @return
     */
    public static Map _getMostRecentDownloadedByMinutesAll(boolean isJustCore, int entries) {
        Map aggregateData = null;

        // align the current timestamp to minutes
        long offset = System.currentTimeMillis() - ServerCacheUtil.startingTimestamp;

        // Align the offset so that it is even number minute from starting timestamp
        offset = offset - offset % (1000 * 60);

        // Calculate the timestamp
        long endingTimestamp = startingTimestamp + offset;
        long startingTimestamp = endingTimestamp - (long) entries * (1000 * 60);

        // If starting time precedes absolute earliest entry, just set to that
        if (startingTimestamp < ServerCacheUtil.startingTimestamp) {
            startingTimestamp = ServerCacheUtil.startingTimestamp;
        }

        String url;
        Set serversSet = null;

        if (isJustCore) {
            serversSet = getCoreServersWithLogData();
        } else {
            serversSet = getServersWithLogData();
        }
        Iterator serverIterator = serversSet.iterator();

        // For each server...
        while (serverIterator.hasNext()) {
            url = (String) serverIterator.next();

            Map nextServerData = getDownloadedByMinutes(url, startingTimestamp, entries);

            // If no map yet, just use first Map
            if (aggregateData == null) {
                aggregateData = nextServerData;
            } // All values to aggregate collection
            else {
                Iterator timestampIterator = nextServerData.keySet().iterator();
                long timestamp;
                Long timestampKey, valueObj, aggregateObj;
                while (timestampIterator.hasNext()) {
                    timestampKey = (Long) timestampIterator.next();
                    timestamp = timestampKey.longValue();

                    valueObj = (Long) nextServerData.get(timestampKey);

                    if (valueObj == null) {
                        continue;
                    }

                    long value = valueObj.longValue();

                    aggregateObj = (Long) aggregateData.get(timestampKey);

                    if (aggregateObj == null) {
                        continue;
                    }

                    long aggregateValue = aggregateObj.longValue();

                    aggregateValue += value;
                    aggregateData.put(timestampKey, Long.valueOf(aggregateValue));
                }
            }
        }

        // If no data found, return empty map
        if (aggregateData == null) {
            return new HashMap();
        }
        return aggregateData;
    }

    /**
     *
     */
    public static Map getMostRecentDownloadedByHours(String serverIP, int entries) {

        lazyLoad();

//        synchronized(readWriteLock) {

        File serverCacheDir = new File(serverCacheDirectory, convertIPToSafeName(serverIP));
        File cacheFile = new File(serverCacheDir, "hours.cache");

        // Calculate the starting timestamp
        long timestamp = System.currentTimeMillis() - (long) entries * HOURS_CACHE;

        RandomAccessFile ras = null;
        try {
            // If cache file doesn't exist, no data transfer!
            if (!cacheFile.exists()) {
                long nextTimestamp = timestamp;
                Map values = new HashMap();
                for (int i = 0; i < entries; i++) {
                    values.put(Long.valueOf(nextTimestamp), Long.valueOf(0));
                    nextTimestamp += HOURS_CACHE;
                }
                return values;
            }

            // Quick assertion: expect file to be even multiple of RECORD_BYTES
            double totalEntries = (double) cacheFile.length() / RECORD_BYTES;
            double flooredTotalEntries = Math.floor(totalEntries);
            if (totalEntries != flooredTotalEntries) {
                throw new DataAssertionException("Expected file to be multiple of " + RECORD_BYTES + ", instead found " + cacheFile.length() + " for: " + cacheFile.exists());
            }

//                ras = new RandomAccessFile(cacheFile,"r");
//
//                long seek = ras.length() - entries*RECORD_BYTES;
//                ras.seek(seek);
//
//                // We need to find timestamp [entries] entries back
//                long timestamp = ras.readLong();
//                ras.close();

            return getDownloadedByHours(serverIP, timestamp, entries);
        } catch (DataAssertionException dae) {
            throw dae;
        } catch (Exception ex) {
            long nextTimestamp = timestamp;
            Map values = new HashMap();
            for (int i = 0; i < entries; i++) {
                values.put(Long.valueOf(nextTimestamp), Long.valueOf(0));
                nextTimestamp += HOURS_CACHE;
            }
            return values;
        } finally {
            if (ras != null) {
                try {
                    ras.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
//        } // synch
    }

    /**
     *
     */
    public static Map getMostRecentDownloadedByHoursAggregate(int entries) {
        return _getMostRecentDownloadedByHoursAll(false, entries);
    }

    /**
     *
     */
    public static Map getMostRecentDownloadedByHoursCore(int entries) {
        return _getMostRecentDownloadedByHoursAll(true, entries);
    }

    /**
     * 
     * @param isJustCoreServers
     * @param entries
     * @return
     */
    public static Map _getMostRecentDownloadedByHoursAll(boolean isJustCore, int entries) {
        Map aggregateData = null;

        // align the current timestamp to minutes
        long offset = System.currentTimeMillis() - ServerCacheUtil.startingTimestamp;

        // Align the offset so that it is even number minute from starting timestamp
        offset = offset - offset % (1000 * 60 * 60);

        // Calculate the timestamp
        long endingTimestamp = startingTimestamp + offset;
        long startingTimestamp = endingTimestamp - (long) entries * (1000 * 60 * 60);

        // If starting time precedes absolute earliest entry, just set to that
        if (startingTimestamp < ServerCacheUtil.startingTimestamp) {
            startingTimestamp = ServerCacheUtil.startingTimestamp;
        }

        String url;
        Set serversSet = null;

        if (isJustCore) {
            serversSet = getCoreServersWithLogData();
        } else {
            serversSet = getServersWithLogData();
        }
        Iterator serverIterator = serversSet.iterator();

        // For each server...
        while (serverIterator.hasNext()) {
            url = (String) serverIterator.next();

            Map nextServerData = getDownloadedByHours(url, startingTimestamp, entries);

            // If no map yet, just use first Map
            if (aggregateData == null) {
                aggregateData = nextServerData;
            } // All values to aggregate collection
            else {
                Iterator timestampIterator = nextServerData.keySet().iterator();
                long timestamp;
                Long timestampKey, valueObj, aggregateObj;
                while (timestampIterator.hasNext()) {
                    timestampKey = (Long) timestampIterator.next();
                    timestamp = timestampKey.longValue();

                    valueObj = (Long) nextServerData.get(timestampKey);

                    if (valueObj == null) {
                        printTracer("No value found for " + url + " at " + timestamp);
                        continue;
                    }

                    long value = valueObj.longValue();

                    aggregateObj = (Long) aggregateData.get(timestampKey);

                    if (aggregateObj == null) {
                        printTracer("No aggregate value for " + url + " at " + timestamp);
                        continue;
                    }

                    long aggregateValue = aggregateObj.longValue();

                    printTracer("aggregate from " + aggregateValue + " to " + (aggregateValue + value) + " for " + timestamp);

                    aggregateValue += value;
                    aggregateData.put(timestampKey, Long.valueOf(aggregateValue));
                }
            }
        }

        // If no data found, return empty map
        if (aggregateData == null) {
            return new HashMap();
        }
        return aggregateData;
    }

    /**
     *
     */
    public static Map getMostRecentDownloadedByDays(String serverIP, int entries) {

        lazyLoad();

//        synchronized(readWriteLock) {

        File serverCacheDir = new File(serverCacheDirectory, convertIPToSafeName(serverIP));
        File cacheFile = new File(serverCacheDir, "days.cache");

        // Calculate timestamp
        long timestamp = System.currentTimeMillis() - (long) entries * DAYS_CACHE;

        RandomAccessFile ras = null;
        try {
            // If cache file doesn't exist, no data transfer!
            if (!cacheFile.exists()) {
                Map values = new HashMap();
                long nextTimestamp = timestamp;
                for (int i = 0; i < entries; i++) {
                    values.put(Long.valueOf(nextTimestamp), Long.valueOf(0));
                    nextTimestamp += DAYS_CACHE;
                }
                return values;
            }

            // Quick assertion: expect file to be even multiple of RECORD_BYTES
            double totalEntries = (double) cacheFile.length() / RECORD_BYTES;
            double flooredTotalEntries = Math.floor(totalEntries);
            if (totalEntries != flooredTotalEntries) {
                throw new DataAssertionException("Expected file to be multiple of " + RECORD_BYTES + ", instead found " + cacheFile.length() + " for: " + cacheFile.exists());
            }

//                ras = new RandomAccessFile(cacheFile,"r");
//
//                long seek = ras.length() - entries*RECORD_BYTES;
//                ras.seek(seek);
//
//                // We need to find timestamp [entries] entries back
//                long timestamp = ras.readLong();
//                ras.close();

            printTracer("Looking for " + entries + " daily entries starting on: " + Text.getFormattedDate(timestamp));

            return getDownloadedByDays(serverIP, timestamp, entries);
        } catch (DataAssertionException dae) {
            throw dae;
        } catch (Exception ex) {
            Map values = new HashMap();
            long nextTimestamp = timestamp;
            for (int i = 0; i < entries; i++) {
                values.put(Long.valueOf(nextTimestamp), Long.valueOf(0));
                nextTimestamp += DAYS_CACHE;
            }
            return values;
        } finally {
            if (ras != null) {
                try {
                    ras.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
//        } // sych
    }

    /**
     *
     */
    public static Map getMostRecentDownloadedByDaysAggregate(int entries) {
        return _getMostRecentDownloadedByDaysAll(false, entries);
    }

    /**
     *
     */
    public static Map getMostRecentDownloadedByDaysCore(int entries) {
        return _getMostRecentDownloadedByDaysAll(true, entries);
    }

    /**
     * 
     * @param isJustCore
     * @param entries
     * @return
     */
    private static Map _getMostRecentDownloadedByDaysAll(boolean isJustCore, int entries) {
        Map aggregateData = null;

        // align the current timestamp to minutes
        long offset = System.currentTimeMillis() - ServerCacheUtil.startingTimestamp;

        // Align the offset so that it is even number minute from starting timestamp
        offset = offset - offset % (1000 * 60 * 60 * 24);

        printTracer("offset=" + offset);

        // Calculate the timestamp
        long endingTimestamp = startingTimestamp + offset;

        long deltaInMillis = ((long) entries * (1000 * 60 * 60 * 24));

        printTracer("deltaInMillis=" + deltaInMillis);

        long startingTimestamp = endingTimestamp - deltaInMillis;

        // If starting time precedes absolute earliest entry, just set to that
        if (startingTimestamp < ServerCacheUtil.startingTimestamp) {
            printTracer("startingTimestamp{" + startingTimestamp + "} < ServerCacheUtil.startingTimestamp[" + ServerCacheUtil.startingTimestamp + "], so setting to ServerCacheUtil.startingTimestamp");
            startingTimestamp = ServerCacheUtil.startingTimestamp;
        }

        String url;
        Set serversSet = null;

        if (isJustCore) {
            serversSet = getCoreServersWithLogData();
        } else {
            serversSet = getServersWithLogData();
        }
        Iterator serverIterator = serversSet.iterator();

        printTracer("startingTimestamp=" + startingTimestamp + ", endingTimestamp=" + endingTimestamp);
        printTracer("Text.getFormattedDate(startingTimestamp)=" + Text.getFormattedDate(startingTimestamp) + ", Text.getFormattedDate(endingTimestamp)=" + Text.getFormattedDate(endingTimestamp));

        // For each server...
        while (serverIterator.hasNext()) {
            url = (String) serverIterator.next();

            Map nextServerData = getDownloadedByDays(url, startingTimestamp, entries);

            // If no map yet, just use first Map
            if (aggregateData == null) {
                aggregateData = nextServerData;
            } // All values to aggregate collection
            else {
                Iterator timestampIterator = nextServerData.keySet().iterator();
                long timestamp;
                Long timestampKey, valueObj, aggregateObj;
                while (timestampIterator.hasNext()) {
                    timestampKey = (Long) timestampIterator.next();
                    timestamp = timestampKey.longValue();

                    valueObj = (Long) nextServerData.get(timestampKey);

                    if (valueObj == null) {
                        printTracer("No value found for " + url + " at " + timestamp);
                        continue;
                    }

                    long value = valueObj.longValue();

                    aggregateObj = (Long) aggregateData.get(timestampKey);

                    if (aggregateObj == null) {
                        printTracer("No aggregate value for " + url + " at " + timestamp);
                        continue;
                    }

                    long aggregateValue = aggregateObj.longValue();

                    aggregateValue += value;
                    aggregateData.put(timestampKey, Long.valueOf(aggregateValue));
                }
            }
        }

        // If no data found, return empty map
        if (aggregateData == null) {
            return new HashMap();
        }
        return aggregateData;
    }

    /**
     *
     */
    public static Map getMostRecentUploadedByMinutes(String serverIP, int entries) {

        lazyLoad();

//        synchronized(readWriteLock) {

        File serverCacheDir = new File(serverCacheDirectory, convertIPToSafeName(serverIP));
        File cacheFile = new File(serverCacheDir, "minutes.cache");

        // Calculate the timestamp
        final long timestamp = System.currentTimeMillis() - (long) entries * MINUTES_CACHE;

        RandomAccessFile ras = null;
        try {
            // If cache file doesn't exist, no data transfer!
            if (!cacheFile.exists()) {
                Map values = new HashMap();
                long nextTimestamp = timestamp;
                for (int i = 0; i < entries; i++) {
                    values.put(Long.valueOf(nextTimestamp), Long.valueOf(0));
                    nextTimestamp += MINUTES_CACHE;
                }
                return values;
            }

            // Quick assertion: expect file to be even multiple of RECORD_BYTES
            double totalEntries = (double) cacheFile.length() / RECORD_BYTES;
            double flooredTotalEntries = Math.floor(totalEntries);
            if (totalEntries != flooredTotalEntries) {
                throw new DataAssertionException("Expected file to be multiple of " + RECORD_BYTES + ", instead found " + cacheFile.length() + " for: " + cacheFile.exists());
            }

//                ras = new RandomAccessFile(cacheFile,"r");
//
//                long seek = ras.length() - entries*RECORD_BYTES;
//                ras.seek(seek);
//
//                // We need to find timestamp [entries] entries back
//                long timestamp = ras.readLong();
//                ras.close();

            return getUploadedByMinutes(serverIP, timestamp, entries);
        } catch (DataAssertionException dae) {
            throw dae;
        } catch (Exception ex) {

            printTracer(ex.getMessage());

            Map values = new HashMap();
            long nextTimestamp = timestamp;
            for (int i = 0; i < entries; i++) {
                values.put(Long.valueOf(nextTimestamp), Long.valueOf(0));
                nextTimestamp += MINUTES_CACHE;
            }
            return values;
        } finally {
            if (ras != null) {
                try {
                    ras.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
//        } // sync
    }

    /**
     *
     */
    public static Map getMostRecentUploadedByMinutesAggregate(int entries) {
        return _getMostRecentUploadedByMinutesAll(false, entries);
    }

    /**
     *
     */
    public static Map getMostRecentUploadedByMinutesCore(int entries) {
        return _getMostRecentUploadedByMinutesAll(true, entries);
    }

    /**
     * 
     * @param isJustCore
     * @param entries
     * @return
     */
    public static Map _getMostRecentUploadedByMinutesAll(boolean isJustCore, int entries) {
        Map aggregateData = null;

        // align the current timestamp to minutes
        long offset = System.currentTimeMillis() - ServerCacheUtil.startingTimestamp;

        // Align the offset so that it is even number minute from starting timestamp
        offset = offset - offset % (1000 * 60);

        // Calculate the timestamp
        long endingTimestamp = startingTimestamp + offset;
        long startingTimestamp = endingTimestamp - (long) entries * (1000 * 60);

        // If starting time precedes absolute earliest entry, just set to that
        if (startingTimestamp < ServerCacheUtil.startingTimestamp) {
            startingTimestamp = ServerCacheUtil.startingTimestamp;
        }

        String url;
        Set serversSet = null;

        if (isJustCore) {
            serversSet = getCoreServersWithLogData();
        } else {
            serversSet = getServersWithLogData();
        }
        Iterator serverIterator = serversSet.iterator();

        // For each server...
        while (serverIterator.hasNext()) {
            url = (String) serverIterator.next();

            Map nextServerData = getUploadedByMinutes(url, startingTimestamp, entries);

            // If no map yet, just use first Map
            if (aggregateData == null) {
                aggregateData = nextServerData;
            } // All values to aggregate collection
            else {
                Iterator timestampIterator = nextServerData.keySet().iterator();
                long timestamp;
                Long timestampKey, valueObj, aggregateObj;
                while (timestampIterator.hasNext()) {
                    timestampKey = (Long) timestampIterator.next();
                    timestamp = timestampKey.longValue();

                    valueObj = (Long) nextServerData.get(timestampKey);

                    if (valueObj == null) {
                        printTracer("No value found for " + url + " at " + timestamp);
                        continue;
                    }

                    long value = valueObj.longValue();

                    aggregateObj = (Long) aggregateData.get(timestampKey);

                    if (aggregateObj == null) {
                        printTracer("No aggregate value for " + url + " at " + timestamp);
                        continue;
                    }

                    long aggregateValue = aggregateObj.longValue();

                    aggregateValue += value;
                    aggregateData.put(timestampKey, Long.valueOf(aggregateValue));
                }
            }
        }

        // If no data found, return empty map
        if (aggregateData == null) {
            return new HashMap();
        }
        return aggregateData;
    }

    /**
     *
     */
    public static Map getMostRecentUploadedByHours(String serverIP, int entries) {

        lazyLoad();

//        synchronized(readWriteLock) {

        File serverCacheDir = new File(serverCacheDirectory, convertIPToSafeName(serverIP));
        File cacheFile = new File(serverCacheDir, "hours.cache");

        // Calculate the timestamp
        final long timestamp = System.currentTimeMillis() - (long) entries * HOURS_CACHE;

        RandomAccessFile ras = null;
        try {
            // If cache file doesn't exist, no data transfer!
            if (!cacheFile.exists()) {
                Map values = new HashMap();
                long nextTimestamp = timestamp;
                for (int i = 0; i < entries; i++) {
                    values.put(Long.valueOf(nextTimestamp), Long.valueOf(0));
                    nextTimestamp += HOURS_CACHE;
                }
                return values;
            }

            // Quick assertion: expect file to be even multiple of RECORD_BYTES
            double totalEntries = (double) cacheFile.length() / RECORD_BYTES;
            double flooredTotalEntries = Math.floor(totalEntries);
            if (totalEntries != flooredTotalEntries) {
                throw new DataAssertionException("Expected file to be multiple of " + RECORD_BYTES + ", instead found " + cacheFile.length() + " for: " + cacheFile.exists());
            }

//                ras = new RandomAccessFile(cacheFile,"r");
//
//                long seek = ras.length() - entries*RECORD_BYTES;
//                ras.seek(seek);
//
//                // We need to find timestamp [entries] entries back
//                long timestamp = ras.readLong();
//                ras.close();

            return getUploadedByHours(serverIP, timestamp, entries);
        } catch (DataAssertionException dae) {
            throw dae;
        } catch (Exception ex) {
            // Expected if records don't exist
            Map values = new HashMap();
            long nextTimestamp = timestamp;
            for (int i = 0; i < entries; i++) {
                values.put(Long.valueOf(nextTimestamp), Long.valueOf(0));
                nextTimestamp += HOURS_CACHE;
            }
            return values;
        } finally {
            if (ras != null) {
                try {
                    ras.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
//        } // synch
    }

    /**
     *
     */
    public static Map getMostRecentUploadedByHoursAggregate(int entries) {
        return _getMostRecentUploadedByHoursAll(false, entries);
    }

    /**
     *
     */
    public static Map getMostRecentUploadedByHoursCore(int entries) {
        return _getMostRecentUploadedByHoursAll(true, entries);
    }

    public static Map _getMostRecentUploadedByHoursAll(boolean isJustCore, int entries) {
        Map aggregateData = null;

        // align the current timestamp to minutes
        long offset = System.currentTimeMillis() - ServerCacheUtil.startingTimestamp;

        // Align the offset so that it is even number minute from starting timestamp
        offset = offset - offset % (1000 * 60 * 60);

        // Calculate the timestamp
        long endingTimestamp = startingTimestamp + offset;
        long startingTimestamp = endingTimestamp - (long) entries * (1000 * 60 * 60);

        // If starting time precedes absolute earliest entry, just set to that
        if (startingTimestamp < ServerCacheUtil.startingTimestamp) {
            startingTimestamp = ServerCacheUtil.startingTimestamp;
        }

        String url;
        Set serversSet = null;

        if (isJustCore) {
            serversSet = getCoreServersWithLogData();
        } else {
            serversSet = getServersWithLogData();
        }
        Iterator serverIterator = serversSet.iterator();

        // For each server...
        while (serverIterator.hasNext()) {
            url = (String) serverIterator.next();

            Map nextServerData = getUploadedByHours(url, startingTimestamp, entries);

            // If no map yet, just use first Map
            if (aggregateData == null) {
                aggregateData = nextServerData;
            } // All values to aggregate collection
            else {
                Iterator timestampIterator = nextServerData.keySet().iterator();
                long timestamp;
                Long timestampKey, valueObj, aggregateObj;
                while (timestampIterator.hasNext()) {
                    timestampKey = (Long) timestampIterator.next();
                    timestamp = timestampKey.longValue();

                    valueObj = (Long) nextServerData.get(timestampKey);

                    if (valueObj == null) {
                        printTracer("No value found for " + url + " at " + timestamp);
                        continue;
                    }

                    long value = valueObj.longValue();

                    aggregateObj = (Long) aggregateData.get(timestampKey);

                    if (aggregateObj == null) {
                        printTracer("No aggregate value for " + url + " at " + timestamp);
                        continue;
                    }

                    long aggregateValue = aggregateObj.longValue();

                    aggregateValue += value;
                    aggregateData.put(timestampKey, Long.valueOf(aggregateValue));
                }
            }
        }

        // If no data found, return empty map
        if (aggregateData == null) {
            return new HashMap();
        }
        return aggregateData;
    }

    /**
     *
     */
    public static Map getMostRecentUploadedByDays(String serverIP, int entries) {

        lazyLoad();

//        synchronized(readWriteLock) {

        File serverCacheDir = new File(serverCacheDirectory, convertIPToSafeName(serverIP));
        File cacheFile = new File(serverCacheDir, "days.cache");

        // Calculate timestamp
        final long timestamp = System.currentTimeMillis() - (long) entries * DAYS_CACHE;

        RandomAccessFile ras = null;
        try {
            // If cache file doesn't exist, no data transfer!
            if (!cacheFile.exists()) {
                Map values = new HashMap();
                long nextTimestamp = timestamp;
                for (int i = 0; i < entries; i++) {
                    values.put(Long.valueOf(nextTimestamp), Long.valueOf(0));
                    nextTimestamp += DAYS_CACHE;
                }
                return values;
            }

            // Quick assertion: expect file to be even multiple of RECORD_BYTES
            double totalEntries = (double) cacheFile.length() / RECORD_BYTES;
            double flooredTotalEntries = Math.floor(totalEntries);
            if (totalEntries != flooredTotalEntries) {
                throw new DataAssertionException("Expected file to be multiple of " + RECORD_BYTES + ", instead found " + cacheFile.length() + " for: " + cacheFile.exists());
            }

//                ras = new RandomAccessFile(cacheFile,"r");
//
//                long seek = ras.length() - entries*RECORD_BYTES;
//                ras.seek(seek);
//
//                // We need to find timestamp [entries] entries back
//                long timestamp = ras.readLong();
//                ras.close();

            return getUploadedByDays(serverIP, timestamp, entries);
        } catch (DataAssertionException dae) {
            throw dae;
        } catch (Exception ex) {
            Map values = new HashMap();
            long nextTimestamp = timestamp;
            for (int i = 0; i < entries; i++) {
                values.put(Long.valueOf(nextTimestamp), Long.valueOf(0));
                nextTimestamp += DAYS_CACHE;
            }
            return values;
        } finally {
            if (ras != null) {
                try {
                    ras.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
//        } // synch
    }

    /**
     *
     */
    public static Map getMostRecentUploadedByDaysAggregate(int entries) {
        return _getMostRecentUploadedByDaysAll(false, entries);
    }

    /**
     *
     */
    public static Map getMostRecentUploadedByDaysCore(int entries) {
        return _getMostRecentUploadedByDaysAll(true, entries);
    }

    /**
     * 
     * @param isJustCore
     * @param entries
     * @return
     */
    public static Map _getMostRecentUploadedByDaysAll(boolean isJustCore, int entries) {
        Map aggregateData = null;

        // align the current timestamp to minutes
        long offset = System.currentTimeMillis() - ServerCacheUtil.startingTimestamp;

        printTracer("offset=" + offset);

        // Align the offset so that it is even number minute from starting timestamp
        offset = offset - offset % (1000 * 60 * 60 * 24);

        // Calculate the timestamp
        long endingTimestamp = startingTimestamp + offset;
        long startingTimestamp = endingTimestamp - (long) entries * (1000 * 60 * 60 * 24);

        // If starting time precedes absolute earliest entry, just set to that
        if (startingTimestamp < ServerCacheUtil.startingTimestamp) {
            startingTimestamp = ServerCacheUtil.startingTimestamp;
        }

        printTracer("startingTimestamp=" + startingTimestamp + ", endingTimestamp=" + endingTimestamp);
        printTracer("Text.getFormattedDate(startingTimestamp)=" + Text.getFormattedDate(startingTimestamp) + ", Text.getFormattedDate(endingTimestamp)=" + Text.getFormattedDate(endingTimestamp));

        String url;
        Set serversSet = null;

        if (isJustCore) {
            serversSet = getCoreServersWithLogData();
        } else {
            serversSet = getServersWithLogData();
        }
        Iterator serverIterator = serversSet.iterator();

        // For each server...
        while (serverIterator.hasNext()) {
            url = (String) serverIterator.next();

            Map nextServerData = getUploadedByDays(url, startingTimestamp, entries);

            // If no map yet, just use first Map
            if (aggregateData == null) {
                aggregateData = nextServerData;
            } // All values to aggregate collection
            else {
                Iterator timestampIterator = nextServerData.keySet().iterator();
                long timestamp;
                Long timestampKey, valueObj, aggregateObj;
                while (timestampIterator.hasNext()) {
                    timestampKey = (Long) timestampIterator.next();
                    timestamp = timestampKey.longValue();

                    valueObj = (Long) nextServerData.get(timestampKey);

                    if (valueObj == null) {
                        System.out.println("DEBUG> No value found for " + url + " at " + timestamp);
                        continue;
                    }

                    long value = valueObj.longValue();

                    aggregateObj = (Long) aggregateData.get(timestampKey);

                    if (aggregateObj == null) {
                        System.out.println("DEBUG> No aggregate value for " + url + " at " + timestamp);
                        continue;
                    }

                    long aggregateValue = aggregateObj.longValue();

                    aggregateValue += value;
                    aggregateData.put(timestampKey, Long.valueOf(aggregateValue));
                }
            }
        }

        // If no data found, return empty map
        if (aggregateData == null) {
            return new HashMap();
        }
        return aggregateData;
    }

    /**
     *
     */
    public static Map getDownloadedByMinutes(String serverIP, long fromTimestamp, int entries) {

        lazyLoad();

        // What's the maximum number of entries?
        final long deltaStartToNow = System.currentTimeMillis() - ServerCacheUtil.startingTimestamp;
        final int maxEntries = (int) Math.floor((double) deltaStartToNow / (1000 * 60));

        // Bring down to max entries if exceeds
        if (maxEntries < entries) {
            entries = maxEntries;
        }

        Map values = new HashMap(entries);
        long nextTimestamp = fromTimestamp;
        // Initialize to zero
        for (int i = 0; i < entries; i++) {
            values.put(new Long(nextTimestamp), new Long(0));
            nextTimestamp += MINUTES_CACHE;
        }

//        synchronized(readWriteLock) {
        File serverCacheDir = new File(serverCacheDirectory, convertIPToSafeName(serverIP));
        File cacheFile = new File(serverCacheDir, "minutes.cache");

        // Cache doesn't exist!
        if (!cacheFile.exists()) {
            return values;
        }

        RandomAccessFile ras = null;
        try {
            ras = new RandomAccessFile(cacheFile, "r");

            // Get time difference since start of file
            long timeDeltaInMillis = fromTimestamp - startingTimestamp;

            // Get entry index
            long index = (long) Math.floor((double) timeDeltaInMillis / MINUTES_CACHE);

            // Get byte offset
            long seek = index * RECORD_BYTES;

            ras.seek(seek);

            long expectedTimestamp = fromTimestamp;
            for (int i = 0; i < entries; i++) {

                long readTimestamp = ras.readLong();
                long readUpload = Math.abs(ras.readLong());
                long readDownload = Math.abs(ras.readLong());

                // Assert timestamp is accurate
                if (readTimestamp > expectedTimestamp + MINUTES_CACHE || readTimestamp < expectedTimestamp - MINUTES_CACHE) {
                    throw new DataAssertionException("Expecting " + expectedTimestamp + " +- " + MINUTES_CACHE + ", instead found " + readTimestamp);
                }

                // Remove zero entry, replace with new value
//                    values.remove(i);

                // Assert size didn't change.
                int prevSize = values.size();
                values.put(Long.valueOf(expectedTimestamp), Long.valueOf(readDownload));
                int newSize = values.size();

                if (prevSize != newSize) {
                    throw new DataAssertionException("Collection changed size, shouldn't. Programmer error.");
                }

                //
                expectedTimestamp += MINUTES_CACHE;
            }

        } catch (DataAssertionException dae) {
            throw dae;
        } catch (Exception ex) {
            printTracer("Exception in getDownloadedByMinutes: " + ex.getMessage());
            // Expected if records don't exist
            return values;
        } finally {
            if (ras != null) {
                try {
                    ras.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
//        } // synch

        return values;
    }

    /**
     *
     */
    public static Map getDownloadedByHours(String serverIP, long fromTimestamp, int entries) {

        lazyLoad();

        // What's the maximum number of entries?
        final long deltaStartToNow = System.currentTimeMillis() - ServerCacheUtil.startingTimestamp;
        final int maxEntries = (int) Math.floor((double) deltaStartToNow / (1000 * 60 * 60));

        // Bring down to max entries if exceeds
        if (maxEntries < entries) {
            entries = maxEntries;
        }

        Map values = new HashMap(entries);
        long nextTimestamp = fromTimestamp;
        // Initialize to zero
        for (int i = 0; i < entries; i++) {
            values.put(new Long(nextTimestamp), new Long(0));
            nextTimestamp += HOURS_CACHE;
        }

//        synchronized(readWriteLock) {
        File serverCacheDir = new File(serverCacheDirectory, convertIPToSafeName(serverIP));
        File cacheFile = new File(serverCacheDir, "hours.cache");

        // Cache doesn't exist!
        if (!cacheFile.exists()) {
            return values;
        }

        RandomAccessFile ras = null;
        try {
            ras = new RandomAccessFile(cacheFile, "r");

            // Get time difference since start of file
            long timeDeltaInMillis = fromTimestamp - startingTimestamp;

            // Get entry index
            long index = (long) Math.floor((double) timeDeltaInMillis / HOURS_CACHE);

            // Get byte offset
            long seek = index * RECORD_BYTES;

            ras.seek(seek);

            long expectedTimestamp = fromTimestamp;
            for (int i = 0; i < entries; i++) {

                long readTimestamp = ras.readLong();
                long readUpload = Math.abs(ras.readLong());
                long readDownload = Math.abs(ras.readLong());

                // Assert timestamp is accurate
                if (readTimestamp > expectedTimestamp + HOURS_CACHE || readTimestamp < expectedTimestamp - HOURS_CACHE) {
                    throw new DataAssertionException("Expecting " + expectedTimestamp + " +- " + HOURS_CACHE + ", instead found " + readTimestamp);
                }

                // Remove zero entry, replace with new value
//                    values.remove(i);

                // Assert size didn't change.
                int prevSize = values.size();
                values.put(Long.valueOf(expectedTimestamp), Long.valueOf(readDownload));
                int newSize = values.size();

                if (prevSize != newSize) {
                    throw new DataAssertionException("Collection changed size, shouldn't. Programmer error.");
                }

                //
                expectedTimestamp += HOURS_CACHE;
            }

        } catch (DataAssertionException dae) {
            throw dae;
        } catch (Exception ex) {
            printTracer("Exception in getDownloadedByHours: " + ex.getMessage());
            // Expected if records don't exist
            return values;
        } finally {
            if (ras != null) {
                try {
                    ras.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
//        } // synch

        return values;
    }

    /**
     *
     */
    public static Map getDownloadedByDays(String serverIP, long fromTimestamp, int entries) {

        lazyLoad();

        // What's the maximum number of entries?
        final long deltaStartToNow = System.currentTimeMillis() - ServerCacheUtil.startingTimestamp;
        final int maxEntries = (int) Math.floor((double) deltaStartToNow / (1000 * 60 * 60 * 24));

        // Bring down to max entries if exceeds
        if (maxEntries < entries) {
            entries = maxEntries;
        }

        Map values = new HashMap(entries);
        long nextTimestamp = fromTimestamp;
        // Initialize to zero
        for (int i = 0; i < entries; i++) {
            values.put(new Long(nextTimestamp), new Long(0));
            nextTimestamp += DAYS_CACHE;
        }

//        synchronized(readWriteLock) {
        File serverCacheDir = new File(serverCacheDirectory, convertIPToSafeName(serverIP));
        File cacheFile = new File(serverCacheDir, "days.cache");

        // Cache doesn't exist!
        if (!cacheFile.exists()) {
            return values;
        }

        RandomAccessFile ras = null;
        try {
            ras = new RandomAccessFile(cacheFile, "r");

            // Get time difference since start of file
            long timeDeltaInMillis = fromTimestamp - startingTimestamp;

            // Get entry index
            long index = (long) Math.floor((double) timeDeltaInMillis / DAYS_CACHE);

            // Get byte offset
            long seek = index * RECORD_BYTES;

            ras.seek(seek);

            long expectedTimestamp = fromTimestamp;
            for (int i = 0; i < entries; i++) {

                long readTimestamp = ras.readLong();
                long readUpload = Math.abs(ras.readLong());
                long readDownload = Math.abs(ras.readLong());

                // Assert timestamp is accurate
                if (readTimestamp > expectedTimestamp + DAYS_CACHE || readTimestamp < expectedTimestamp - DAYS_CACHE) {
                    throw new DataAssertionException("Expecting " + expectedTimestamp + " +- " + DAYS_CACHE + ", instead found " + readTimestamp);
                }

                // Remove zero entry, replace with new value
//                    values.remove(i);

                // Assert size didn't change.
                int prevSize = values.size();
                values.put(Long.valueOf(expectedTimestamp), Long.valueOf(readDownload));
                int newSize = values.size();

                if (prevSize != newSize) {
                    throw new DataAssertionException("Collection changed size, shouldn't. Programmer error.");
                }

                //
                expectedTimestamp += DAYS_CACHE;
            }

        } catch (DataAssertionException dae) {
            throw dae;
        } catch (Exception ex) {
            printTracer("Exception in getDownloadedByDays: " + ex.getMessage());
            // Expected if records don't exist
            return values;
        } finally {
            if (ras != null) {
                try {
                    ras.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
//        }

        return values;
    }

    /**
     *
     */
    public static Map getUploadedByMinutes(String serverIP, long fromTimestamp, int entries) {

        lazyLoad();

        // What's the maximum number of entries?
        final long deltaStartToNow = System.currentTimeMillis() - ServerCacheUtil.startingTimestamp;
        final int maxEntries = (int) Math.floor((double) deltaStartToNow / (1000 * 60));

        // Bring down to max entries if exceeds
        if (maxEntries < entries) {
            entries = maxEntries;
        }

        Map values = new HashMap(entries);
        long nextTimestamp = fromTimestamp;
        // Initialize to zero
        for (int i = 0; i < entries; i++) {
            values.put(new Long(nextTimestamp), new Long(0));
            nextTimestamp += MINUTES_CACHE;
        }

//        synchronized(readWriteLock) {
        File serverCacheDir = new File(serverCacheDirectory, convertIPToSafeName(serverIP));
        File cacheFile = new File(serverCacheDir, "minutes.cache");

        // Cache doesn't exist!
        if (!cacheFile.exists()) {
            return values;
        }

        RandomAccessFile ras = null;
        try {
            ras = new RandomAccessFile(cacheFile, "r");

            // Get time difference since start of file
            long timeDeltaInMillis = fromTimestamp - startingTimestamp;

            // Get entry index
            long index = (long) Math.floor((double) timeDeltaInMillis / MINUTES_CACHE);

            // Get byte offset
            long seek = index * RECORD_BYTES;

            ras.seek(seek);

            long expectedTimestamp = fromTimestamp;
            for (int i = 0; i < entries; i++) {

                long readTimestamp = ras.readLong();
                long readUpload = Math.abs(ras.readLong());
                long readDownload = Math.abs(ras.readLong());

                // Assert timestamp is accurate
                if (readTimestamp > expectedTimestamp + MINUTES_CACHE || readTimestamp < expectedTimestamp - MINUTES_CACHE) {
                    throw new DataAssertionException("Expecting " + expectedTimestamp + " +- " + MINUTES_CACHE + ", instead found " + readTimestamp);
                }

                // Remove zero entry, replace with new value
//                    values.remove(i);

                // Assert size didn't change.
                int prevSize = values.size();
                values.put(Long.valueOf(expectedTimestamp), Long.valueOf(readUpload));
                int newSize = values.size();

                if (prevSize != newSize) {
                    throw new DataAssertionException("Collection changed size, shouldn't. Programmer error.");
                }

                //
                expectedTimestamp += MINUTES_CACHE;
            }

        } catch (DataAssertionException dae) {
            throw dae;
        } catch (Exception ex) {
            printTracer("Exception in getUploadedByMinutes: " + ex.getMessage());
            // Expected if records don't exist
            return values;
        } finally {
            if (ras != null) {
                try {
                    ras.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
//        } // synch

        return values;
    }

    /**
     *
     */
    public static Map getUploadedByHours(String serverIP, long fromTimestamp, int entries) {

        lazyLoad();

        // What's the maximum number of entries?
        final long deltaStartToNow = System.currentTimeMillis() - ServerCacheUtil.startingTimestamp;
        final int maxEntries = (int) Math.floor((double) deltaStartToNow / (1000 * 60 * 60));

        // Bring down to max entries if exceeds
        if (maxEntries < entries) {
            entries = maxEntries;
        }

        Map values = new HashMap(entries);
        long nextTimestamp = fromTimestamp;
        // Initialize to zero
        for (int i = 0; i < entries; i++) {
            values.put(new Long(nextTimestamp), new Long(0));
            nextTimestamp += HOURS_CACHE;
        }

//        synchronized(readWriteLock) {
        File serverCacheDir = new File(serverCacheDirectory, convertIPToSafeName(serverIP));
        File cacheFile = new File(serverCacheDir, "hours.cache");

        // Cache doesn't exist!
        if (!cacheFile.exists()) {
            return values;
        }

        RandomAccessFile ras = null;
        try {
            ras = new RandomAccessFile(cacheFile, "r");

            // Get time difference since start of file
            long timeDeltaInMillis = fromTimestamp - startingTimestamp;

            // Get entry index
            long index = (long) Math.floor((double) timeDeltaInMillis / HOURS_CACHE);

            // Get byte offset
            long seek = index * RECORD_BYTES;

            ras.seek(seek);

            long expectedTimestamp = fromTimestamp;
            for (int i = 0; i < entries; i++) {

                long readTimestamp = ras.readLong();
                long readUpload = Math.abs(ras.readLong());
                long readDownload = Math.abs(ras.readLong());

                // Assert timestamp is accurate
                if (readTimestamp > expectedTimestamp + HOURS_CACHE || readTimestamp < expectedTimestamp - HOURS_CACHE) {
                    throw new DataAssertionException("Expecting " + expectedTimestamp + " +- " + HOURS_CACHE + ", instead found " + readTimestamp);
                }

                // Remove zero entry, replace with new value
//                    values.remove(i);

                // Assert size didn't change.
                int prevSize = values.size();
                values.put(Long.valueOf(expectedTimestamp), Long.valueOf(readUpload));
                int newSize = values.size();

                if (prevSize != newSize) {
                    throw new DataAssertionException("Collection changed size, shouldn't. Programmer error.");
                }

                //
                expectedTimestamp += HOURS_CACHE;
            }

        } catch (DataAssertionException dae) {
            throw dae;
        } catch (Exception ex) {
            printTracer("Exception in getUploadedByHours: " + ex.getMessage());
            // Expected if records don't exist
            return values;
        } finally {
            if (ras != null) {
                try {
                    ras.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
//        } // sync

        return values;
    }

    /**
     * 
     * @return
     */
    public static Map getAllDownloadedAggregate() {

        Set allServers = getServersWithLogData();

        Iterator serverIterator = allServers.iterator();
        Map aggregateData = null;
        String url;

        // For each server...
        while (serverIterator.hasNext()) {
            url = (String) serverIterator.next();

            Map nextServerData = getDownloadedByDays(url, ServerCacheUtil.startingTimestamp, Integer.MAX_VALUE);

            // If no map yet, just use first Map
            if (aggregateData == null) {
                aggregateData = nextServerData;
            } // All values to aggregate collection
            else {
                Iterator timestampIterator = nextServerData.keySet().iterator();
                long timestamp;
                Long timestampKey, valueObj, aggregateObj;
                while (timestampIterator.hasNext()) {
                    timestampKey = (Long) timestampIterator.next();
                    timestamp = timestampKey.longValue();

                    valueObj = (Long) nextServerData.get(timestampKey);

                    if (valueObj == null) {
                        printTracer("No value found for " + url + " at " + timestamp);
                        continue;
                    }

                    long value = valueObj.longValue();

                    aggregateObj = (Long) aggregateData.get(timestampKey);

                    if (aggregateObj == null) {
                        printTracer("No aggregate value for " + url + " at " + timestamp);
                        continue;
                    }

                    long aggregateValue = aggregateObj.longValue();

                    aggregateValue += value;
                    aggregateData.put(timestampKey, Long.valueOf(aggregateValue));
                }
            }
        }

        // If no data found, return empty map
        if (aggregateData == null) {
            return new HashMap();
        }
        return aggregateData;
    }

    /**
     * 
     * @return
     */
    public static Map getAllDownloadedCore() {
        Set coreServers = getCoreServersWithLogData();

        Iterator serverIterator = coreServers.iterator();
        Map aggregateData = null;
        String url;

        // For each server...
        while (serverIterator.hasNext()) {
            url = (String) serverIterator.next();

            Map nextServerData = getDownloadedByDays(url, ServerCacheUtil.startingTimestamp, Integer.MAX_VALUE);

            // If no map yet, just use first Map
            if (aggregateData == null) {
                aggregateData = nextServerData;
            } // All values to aggregate collection
            else {
                Iterator timestampIterator = nextServerData.keySet().iterator();
                long timestamp;
                Long timestampKey, valueObj, aggregateObj;
                while (timestampIterator.hasNext()) {
                    timestampKey = (Long) timestampIterator.next();
                    timestamp = timestampKey.longValue();

                    valueObj = (Long) nextServerData.get(timestampKey);

                    if (valueObj == null) {
                        printTracer("No value found for " + url + " at " + timestamp);
                        continue;
                    }

                    long value = valueObj.longValue();

                    aggregateObj = (Long) aggregateData.get(timestampKey);

                    if (aggregateObj == null) {
                        printTracer("No aggregate value for " + url + " at " + timestamp);
                        continue;
                    }

                    long aggregateValue = aggregateObj.longValue();

                    aggregateValue += value;
                    aggregateData.put(timestampKey, Long.valueOf(aggregateValue));
                }
            }
        }

        // If no data found, return empty map
        if (aggregateData == null) {
            return new HashMap();
        }
        return aggregateData;
    }

    /**
     * 
     * @return
     */
    public static Map getAllUploadedAggregate() {
        Set allServers = getServersWithLogData();

        Iterator serverIterator = allServers.iterator();
        Map aggregateData = null;
        String url;

        // For each server...
        while (serverIterator.hasNext()) {
            url = (String) serverIterator.next();

            Map nextServerData = getUploadedByDays(url, ServerCacheUtil.startingTimestamp, Integer.MAX_VALUE);

            // If no map yet, just use first Map
            if (aggregateData == null) {
                aggregateData = nextServerData;
            } // All values to aggregate collection
            else {
                Iterator timestampIterator = nextServerData.keySet().iterator();
                long timestamp;
                Long timestampKey, valueObj, aggregateObj;
                while (timestampIterator.hasNext()) {
                    timestampKey = (Long) timestampIterator.next();
                    timestamp = timestampKey.longValue();

                    valueObj = (Long) nextServerData.get(timestampKey);

                    if (valueObj == null) {
                        printTracer("No value found for " + url + " at " + timestamp);
                        continue;
                    }

                    long value = valueObj.longValue();

                    aggregateObj = (Long) aggregateData.get(timestampKey);

                    if (aggregateObj == null) {
                        printTracer("No aggregate value for " + url + " at " + timestamp);
                        continue;
                    }

                    long aggregateValue = aggregateObj.longValue();

                    aggregateValue += value;
                    aggregateData.put(timestampKey, Long.valueOf(aggregateValue));
                }
            }
        }

        // If no data found, return empty map
        if (aggregateData == null) {
            return new HashMap();
        }
        return aggregateData;
    }

    /**
     * 
     * @return
     */
    public static Map getAllUploadedCore() {
        Set coreServers = getCoreServersWithLogData();

        Iterator serverIterator = coreServers.iterator();
        Map aggregateData = null;
        String url;

        // For each server...
        while (serverIterator.hasNext()) {
            url = (String) serverIterator.next();

            Map nextServerData = getUploadedByDays(url, ServerCacheUtil.startingTimestamp, Integer.MAX_VALUE);

            // If no map yet, just use first Map
            if (aggregateData == null) {
                aggregateData = nextServerData;
            } // All values to aggregate collection
            else {
                Iterator timestampIterator = nextServerData.keySet().iterator();
                long timestamp;
                Long timestampKey, valueObj, aggregateObj;
                while (timestampIterator.hasNext()) {
                    timestampKey = (Long) timestampIterator.next();
                    timestamp = timestampKey.longValue();

                    valueObj = (Long) nextServerData.get(timestampKey);

                    if (valueObj == null) {
                        printTracer("No value found for " + url + " at " + timestamp);
                        continue;
                    }

                    long value = valueObj.longValue();

                    aggregateObj = (Long) aggregateData.get(timestampKey);

                    if (aggregateObj == null) {
                        printTracer("No aggregate value for " + url + " at " + timestamp);
                        continue;
                    }

                    long aggregateValue = aggregateObj.longValue();

                    aggregateValue += value;
                    aggregateData.put(timestampKey, Long.valueOf(aggregateValue));
                }
            }
        }

        // If no data found, return empty map
        if (aggregateData == null) {
            return new HashMap();
        }
        return aggregateData;
    }

    /**
     *
     */
    public static Map getUploadedByDays(String serverIP, long fromTimestamp, int entries) {

        lazyLoad();

        // What's the maximum number of entries?
        final long deltaStartToNow = System.currentTimeMillis() - ServerCacheUtil.startingTimestamp;
        final int maxEntries = (int) Math.floor((double) deltaStartToNow / (1000 * 60 * 60 * 24));

        // Bring down to max entries if exceeds
        if (maxEntries < entries) {
            entries = maxEntries;
        }

        Map values = new HashMap(entries);
        long nextTimestamp = fromTimestamp;
        // Initialize to zero
        for (int i = 0; i < entries; i++) {
            values.put(new Long(nextTimestamp), new Long(0));
            nextTimestamp += DAYS_CACHE;
        }

//        synchronized(readWriteLock) {
        File serverCacheDir = new File(serverCacheDirectory, convertIPToSafeName(serverIP));
        File cacheFile = new File(serverCacheDir, "days.cache");

        // Cache doesn't exist!
        if (!cacheFile.exists()) {
            return values;
        }

        RandomAccessFile ras = null;
        try {
            ras = new RandomAccessFile(cacheFile, "r");

            // Get time difference since start of file
            long timeDeltaInMillis = fromTimestamp - startingTimestamp;

            // Get entry index
            long index = (long) Math.floor((double) timeDeltaInMillis / DAYS_CACHE);

            // Get byte offset
            long seek = index * RECORD_BYTES;

            ras.seek(seek);

            long expectedTimestamp = fromTimestamp;
            for (int i = 0; i < entries; i++) {

                long readTimestamp = ras.readLong();
                long readUpload = Math.abs(ras.readLong());
                long readDownload = Math.abs(ras.readLong());

                // Assert timestamp is accurate
                if (readTimestamp > expectedTimestamp + DAYS_CACHE || readTimestamp < expectedTimestamp - DAYS_CACHE) {
                    throw new DataAssertionException("Expecting " + expectedTimestamp + " +- " + DAYS_CACHE + ", instead found " + readTimestamp);
                }

                // Remove zero entry, replace with new value
//                    values.remove(i);

                // Assert size didn't change.
                int prevSize = values.size();
                values.put(Long.valueOf(expectedTimestamp), Long.valueOf(readUpload));
                int newSize = values.size();

                if (prevSize != newSize) {
                    throw new DataAssertionException("Collection changed size, shouldn't. Programmer error.");
                }

                //
                expectedTimestamp += DAYS_CACHE;
            }

        } catch (DataAssertionException dae) {
            throw dae;
        } catch (Exception ex) {
            printTracer("Exception in getUploadedByDays: " + ex.getMessage());

            if (isDebug) {
                ex.printStackTrace(System.err);
            }
            // Expected if records don't exist
            return values;
        } finally {
            if (ras != null) {
                try {
                    ras.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
//        } // synch

        return values;
    }

    /**
     * Thread processes queue of log files waiting to be processed.
     */
    private static class LogFileProcessorThread extends Thread {

        private LogFileProcessorThread() {
            super("LogFileProcessorThread");
            setDaemon(true);
            setPriority(Thread.MIN_PRIORITY);
        }

        public void run() {
            File nextLogFile;
            String serverIP = null;
            while (true) {

                // Yield so can enqueue
                Thread.yield();

                synchronized (logFileQueue) {

                    // If queue is empty, reliquish lock
                    if (logFileQueue.isEmpty()) {
                        continue;
                    }

                    nextLogFile = new File((String) logFileQueue.remove(0));

                    // Used to skip storage if failed when parsing
                    boolean isFailed = false;

                    try {
                        // Grab server IP
                        serverIP = (String) logFileQueueMapToIP.remove(nextLogFile.getAbsolutePath());

                        // Parse up and add to cache
                        parseLogFile(nextLogFile, serverIP);
                    } catch (Exception ex) {
                        isFailed = true;
                        try {
                            // Failed, remove from list of serviced logs
                            removeLogFromPersistentList(nextLogFile.getAbsolutePath());
                        } catch (Exception nope) { /* nada */ }
                        System.err.println("Failed to parse next log: " + nextLogFile.getAbsolutePath() + ", message: " + ex.getMessage());
                        ex.printStackTrace(System.err);
                    }

                    if (!isFailed) {
                        try {
                            // Move to perminent storage
                            copyLogToPersistentStorage(nextLogFile, serverIP);

                            // If gets here, delete!
                            IOUtil.safeDelete(nextLogFile);

                        } catch (Exception ex) {
                            try {
                                // Failed, remove from list of serviced logs
                                removeLogFromPersistentList(nextLogFile.getAbsolutePath());
                            } catch (Exception nope) { /* nada */ }
                            System.err.println("Failed to perminently store server log: " + ex.getMessage());
                        }
                    }
                } // synchronized on server log path queue
            } // while true
        } // run
    } // LogFileProcessorThread

    /**
     *
     */
    private static void printTracer(String msg) {
        if (isDebug) {
            System.out.println("SERVER_CACHE_UTIL> " + msg);
        }
    }

    /**
     *
     */
    public static Set getServersWithLogData() {
        Set servers = new HashSet();

        // Find all the log files
        String[] serverLogDirs = serverLogDirectory.list();
        Set ipFilePaths = new HashSet();

        File nextLogDir, nextLogFile;
        for (int i = 0; i < serverLogDirs.length; i++) {
            nextLogDir = new File(serverLogDirectory, serverLogDirs[i]);
            nextLogFile = new File(nextLogDir, "IP");

            if (nextLogFile.exists()) {
                ipFilePaths.add(nextLogFile.getAbsolutePath());
            }
        }

        // Read the log files and grab the URLs
        BufferedReader reader = null;
        Iterator it = ipFilePaths.iterator();

        String nextPath;
        while (it.hasNext()) {
            nextPath = (String) it.next();
            try {
                reader = new BufferedReader(new FileReader(new File(nextPath)));
                String url = reader.readLine();

                // If url doens't start with protocol, add it. Some do, some don't.
                if (!url.contains("tranche")) {
                    url = "tranche://" + url;
                }

                servers.add(url);
            } catch (Exception ex) {
                /* nope */
                ex.printStackTrace(System.err);
            } finally {
                IOUtil.safeClose(reader);
            }
        }

        return servers;
    }

    /**
     *
     */
    public static Set getCoreServersWithLogData() {
        return getRestrictedServer(true);
    }

    /**
     *
     */
    public static Set getNonCoreServersWithLogData() {
        return getRestrictedServer(false);
    }

    /**
     *
     */
    private static Set getRestrictedServer(boolean isGetCore) {
        Set servers = getServersWithLogData();

        ServerUtil.waitForStartup();

        // Get list of core server's ip. Will have to trim out protocol.
        List coreServers = new ArrayList();

        List coreServersURLs = ServerUtil.getCoreServers();
        String url;
        for (int i = 0; i < coreServersURLs.size(); i++) {
            url = (String) coreServersURLs.get(i);
//            // Parse out next IP...
//            try {
//                coreServers.add(IOUtil.parseHost(url)+":"+IOUtil.parsePort(url));
//            } catch (Exception ex) {
//                // nope...
//            }
            // No, just add it.
            coreServers.add(url);
        }

        Iterator it = servers.iterator();

        String nextServer;
        while (it.hasNext()) {
            nextServer = (String) it.next();


            // If not a core server and only want core servers, remove it
            if (isGetCore && !coreServers.contains(nextServer)) {
                it.remove();
            }
            // If it is a core server and want other, remove
            if (!isGetCore && coreServers.contains(nextServer)) {
                it.remove();
            }
        }

        return servers;
    }
    private static Object projectDownloadLogLock = new Object();

    /**
     *
     */
    public static void addProjectDownloadEntrySafe(BigHash hash, long timestamp) {
        lazyLoad();
        try {
            if (!containsProjectDownloadEntry(hash, timestamp)) {
                addProjectDownloadEntry(hash, timestamp);
            }
        } catch (Exception ex) {
            System.err.println("Problem adding server log entry for " + hash.toString().substring(0, 8) + "...: " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }

    /**
     * <p>Adds timestamp download to project even if already an entry.</p>
     * <p>If want a safe way to avoid duplicate entries, use {@link #addProjectDownloadEntrySafe()}</p>
     */
    public static void addProjectDownloadEntry(BigHash hash, long timestamp) throws Exception {
        lazyLoad();
        File projectFile = getProjectLogFile(hash);

        synchronized (projectDownloadLogLock) {
            // Just add timestamp entry!
            DataOutputStream out = null;
            try {
                out = new DataOutputStream(new FileOutputStream(projectFile, true));
                out.writeLong(timestamp);
            } finally {
                if (out != null) {
                    IOUtil.safeClose(out);
                }
            }
        }
    }

    /**
     * <p>A very expensive process that tries to find timestamp for project.</p>
     * @deprecated  Use in-memory {@link #getProjectDownloadTimestamps()} for much quicker timestamp matching.
     */
    public static boolean containsProjectDownloadEntry(BigHash hash, long timestamp) throws Exception {
        lazyLoad();
        File projectFile = getProjectLogFile(hash);

        synchronized (projectDownloadLogLock) {
            // Read in timestamps for file. If match, return true. Else false.
            DataInputStream in = null;
            try {
                in = new DataInputStream(new FileInputStream(projectFile));

                long nextTimestamp = -1;
                while ((nextTimestamp = in.readLong()) != -1) {

//                    try {
                    if (nextTimestamp == timestamp) {
                        return true;
                    }
//                    } catch (NumberFormatException skip) {
//                        /* nope */
//                        System.err.println("Exception while determing if project download entry already exists: "+skip.getMessage());
//                        skip.printStackTrace(System.err);
//                    }
                }
            } catch (Exception nope) {
            // Done!
            } finally {
                if (in != null) {
                    IOUtil.safeClose(in);
                }
            }
        }

        return false;
    }

    /**
     * <p>Returns all the download timestamps for a project.</p>
     * @return A set of Long timestamps for registered downloads.
     */
    public static Set getProjectDownloadTimestamps(BigHash hash) throws Exception {
        lazyLoad();

        Set timestamps = new HashSet();
        File projectFile = getProjectLogFile(hash);

        synchronized (projectDownloadLogLock) {
            // Read in timestamps for file. If match, return true. Else false.
            BufferedReader in = null;
            try {
                in = new BufferedReader(new FileReader(projectFile));

                String nextLine;

                while ((nextLine = in.readLine()) != null) {
                    try {
                        long readTimestamp = Long.parseLong(nextLine);
                        timestamps.add(Long.valueOf(readTimestamp));
                    } catch (NumberFormatException skip) { /* nope */ }
                }
            } finally {
                if (in != null) {
                    IOUtil.safeClose(in);
                }
            }
        }

        return timestamps;
    }

    /**
     * <p>Returns a list of files for projects with logs.</p>
     */
    public static SimpleDiskBackedBigHashList getProjectHashesWithLogs() {
        SimpleDiskBackedBigHashList l = new SimpleDiskBackedBigHashList();

        // Depth-first search
        List files = new ArrayList();

        files.add(ServerCacheUtil.projectDownloadLogsDirectory.getAbsolutePath());

        while (!files.isEmpty()) {
            String path = (String) files.remove(0);

            File nextFile = new File(path);

            if (nextFile.isDirectory()) {
                String[] names = nextFile.list();
                for (int i = 0; i < names.length; i++) {
                    files.add(new File(nextFile, names[i]).getAbsolutePath());
                }

            } else {
                try {
                    // Build the hash from the file path
                    l.add(getHashProjectLogFile(nextFile));
                } catch (Exception ex) {
                    System.err.println("Failed to determine hash from project log: " + ex.getMessage());
                    System.err.println("Project log at: " + nextFile.getAbsolutePath());
                    ex.printStackTrace(System.err);
                }
            }
        }

        return l;
    }

    /**
     *
     */
    public static List getMostPopularProjects(int count) throws Exception {

        return getAllProjectsRankedByPopularityInternal(count);
//        SimpleDiskBackedBigHashList dbl = getProjectHashesWithLogs();
//
//        try {
//
//            // Don't want ridiculous initialization...
//            int initialSize = (count > 100) ? 100 : count + 1;
//
//            // Initialize a little larger so can prune
//            List largestHashes = new ArrayList(initialSize);
//
//            // Handle up to 1000 at a time
//            long offset = 0;
//            long limit = 1000;
//            List l = dbl.get(offset, limit);
//
//            long smallestOfLargest = Long.MIN_VALUE;
//
//            while (l != null && !l.isEmpty()) {
//
//                Iterator it = l.iterator();
//
//                BigHash h;
//                while(it.hasNext()) {
//                    h = (BigHash)it.next();
//
//                    File nextLog = getProjectLogFile(h);
//
//                    // If larger than smallest known hash in list of largest, add
////                    if (smallestOfLargest <= nextLog.length()) {
//                        largestHashes.add(h);
////                    }
//
//                    // Prune list of largest
//                    while (largestHashes.size() > count) {
//                        long smallestSize = Long.MAX_VALUE;
//                        int smallestIndex = -1;
//                        BigHash nextHash;
//
//                        for (int i=0; i<largestHashes.size(); i++) {
//                            nextHash = (BigHash)largestHashes.get(i);
//
//                            File log = getProjectLogFile(nextHash);
//
//                            if (log.length() < smallestSize) {
//                                smallestIndex = i;
//                                smallestSize = log.length();
//                            }
//                        }
//
//                        // Remove it
//                        largestHashes.remove(smallestIndex);
//                    }
//
//                    smallestOfLargest = Long.MIN_VALUE;
//                }
//
//
//                // Get next batch of hashes
//                offset+=limit;
//                l = dbl.get(offset, limit);
//            }
//
//            return largestHashes;
//        } finally {
//            dbl.close();
//        }
    }
    static List orderedList = null;

    public static synchronized List getAllProjectsRankedByPopularity() throws Exception {
        lazyLoad();

//        // If thread hasn't had time build project list, get it now
//        if (orderedList == null) {
//            // Fire off the thread
//            if (!getProjectsRankedByPopularityThread.isAlive()) {
//                try {
//                    getProjectsRankedByPopularityThread.setDaemon(true);
//                    getProjectsRankedByPopularityThread.setPriority(Thread.MIN_PRIORITY);
//                    getProjectsRankedByPopularityThread.start();
//                    System.out.println("Started project download thread...");
//                } catch (Exception ex) { /* nope */ }
//            }
//
//            orderedList = getAllProjectsRankedByPopularityInternal();
//        }
//
//        return orderedList;

        return getAllProjectsRankedByPopularityInternal();
    }
    static Thread getProjectsRankedByPopularityThread = new Thread("getProjectsRankedByPopularityThread") {

        public void run() {
            while (true) {
                try {
                    orderedList = getAllProjectsRankedByPopularityInternal();
                    Thread.sleep(30 * 1000);
                } catch (Exception ex) {
                    ex.printStackTrace(System.err);
                }
            }
        }
    };

    /**
     */
    private static List getAllProjectsRankedByPopularityInternal() throws Exception {
        return getAllProjectsRankedByPopularityInternal(Integer.MAX_VALUE);
    }

    private static List getAllProjectsRankedByPopularityInternal(final int limit) throws Exception {

        final long start = System.currentTimeMillis();
        SimpleDiskBackedBigHashList dbl = getProjectHashesWithLogs();

        try {
            // Move to list...
            List hashes = dbl.get(0, dbl.size());

            // And put log files in a map for quick access...
            Map hashesAndLogs = new HashMap();

            for (int i = 0; i < hashes.size(); i++) {
                File log = getProjectLogFile((BigHash) hashes.get(i));

                // Only add log if exists
                if (log != null && log.exists() && log.length() > 0) {
                    hashesAndLogs.put(hashes.get(i), log.getAbsolutePath());
                }
            }

            List orderedList = new ArrayList(hashes.size());

            // Sort the map
            while (!hashesAndLogs.isEmpty()) {

                BigHash largestHash = null;
                long largestSize = -1;
                BigHash nextHash;
                File nextLogFile;

                Iterator it = hashesAndLogs.keySet().iterator();

                int i = 0;

                // Find the next largest file (number downloads)
                while (it.hasNext()) {
                    nextHash = (BigHash) it.next();
                    nextLogFile = new File((String) hashesAndLogs.get(nextHash));
                    if (largestHash == null || largestSize < nextLogFile.length()) {
                        largestHash = nextHash;
                        largestSize = nextLogFile.length();
                    }
                    i++;
                }

                // Add to ordered list and remove from map
                orderedList.add(largestHash);
                hashesAndLogs.remove(largestHash);

                if (orderedList.size() >= limit) {
                    break;
                }
            }

            if (isPrintTimesFindProjectDownloads) {
                System.out.println("Returning ordered list of projects of size " + orderedList.size() + ", took: " + Text.getPrettyEllapsedTimeString(System.currentTimeMillis() - start));
            }
            return orderedList;
        } finally {
            dbl.close();
        }
    }

    /**
     * Returns file for project log. Will create file for you if doesn't exist.
     */
    public static File getProjectLogFile(BigHash hash) throws IOException {
        final String base16Hash = Base16.encode(hash.toByteArray());

        String projectLogPath =
                ServerCacheUtil.projectDownloadLogsDirectory.getAbsolutePath() + File.separator +
                base16Hash.substring(0, 4) + File.separator +
                base16Hash.substring(4, 8) + File.separator +
                base16Hash.substring(8, 12) + File.separator +
                base16Hash.substring(12, 16) + File.separator +
                base16Hash.substring(16, base16Hash.length());

        File projectLog = new File(projectLogPath);

        // Make all the necessary directories and create file
        if (!projectLog.exists()) {
            projectLog.getParentFile().mkdirs();
            projectLog.createNewFile();
        }

        printTracer("Created project log at " + projectLog.getAbsolutePath());

        return projectLog;
    }

    /**
     *
     */
    public static BigHash getHashProjectLogFile(File projectLogFile) throws IOException {

        String logPath = projectLogFile.getAbsolutePath();
        String absPath = ServerCacheUtil.projectDownloadLogsDirectory.getAbsolutePath();

        // Strip off the path for the project logs to get relative location
        String hashPath = logPath.substring(absPath.length());

        // Remove the file separators.
        hashPath = hashPath.replaceAll(File.separator, "");

        return BigHash.createHashFromString(hashPath);
    }

    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    // CHUNK REPLICATIONS SECTION
    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    /**
     * 
     * @return
     */
    private static boolean isReplicationsFileExist() {
        if (csvFileWithAllProjectsReplications == null || !csvFileWithAllProjectsReplications.exists()) {

            if (csvFileWithAllProjectsReplications == null) {
                System.out.println("Could not find replications for project: csvFileWithAllProjectsReplications is null!");
            } else if (!csvFileWithAllProjectsReplications.exists()) {
                System.out.println("Could not find replications for project: csvFileWithAllProjectsReplications doesn't exist at " + csvFileWithAllProjectsReplications.getAbsolutePath());
            }
            return false;
        }

        return true;
    }

    /**
     * 
     * @return
     * @throws java.lang.Exception
     */
    public static ProjectReplicationsEntry getReplicationsForProjects() throws Exception {
        if (!isReplicationsFileExist()) {
            return null;
        }

        long reps0 = 0, reps1 = 0, reps2 = 0, reps3 = 0, reps4 = 0, reps5orMore = 0;
        synchronized (csvFileWithAllProjectsReplications) {

            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(csvFileWithAllProjectsReplications));

                String entry;
                while ((entry = reader.readLine()) != null) {
                    // Skip blank lines or comments
                    if (entry == null || entry.trim().equals("") || entry.trim().startsWith("#")) {
                        continue;
                    }

                    // Does line match BigHash
                    try {
                        String[] tokens = entry.split(",");
                        String hashStr = tokens[1];
                        reps0 += Long.parseLong(tokens[3]);
                        reps1 += Long.parseLong(tokens[4]);
                        reps2 += Long.parseLong(tokens[5]);
                        reps3 += Long.parseLong(tokens[6]);
                        reps4 += Long.parseLong(tokens[7]);
                        reps5orMore += Long.parseLong(tokens[8]);

                    } catch (Exception ex) {
                        System.err.println("Could not find replications for project: Cannot parse out project replications entry from CSV: " + ex.getMessage());
                    }
                }
            } finally {
                if (reader != null) {
                    IOUtil.safeClose(reader);
                }
            }
        } // synchronized

        return ProjectReplicationsEntry.createEntry((BigHash) null, System.currentTimeMillis(), reps0, reps1, reps2, reps3, reps4, reps5orMore,1);
    }

    /**
     * 
     * @return
     * @throws java.lang.Exception
     */
    public static Set<BigHash> getProjectsMissingData() throws Exception {

        Set<BigHash> projectsMissingData = new HashSet();

        if (!isReplicationsFileExist()) {
            return projectsMissingData;
        }
        synchronized (csvFileWithAllProjectsReplications) {
            long reps0 = 0, reps1 = 0, reps2 = 0, reps3 = 0, reps4 = 0, reps5orMore = 0;

            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(csvFileWithAllProjectsReplications));

                String entry;
                while ((entry = reader.readLine()) != null) {
                    // Skip blank lines or comments
                    if (entry == null || entry.trim().equals("") || entry.trim().startsWith("#")) {
                        continue;
                    }

                    // Does line match BigHash
                    try {
                        String[] tokens = entry.split(",");
                        String hashStr = tokens[1];

                        if (hashStr.startsWith("\"")) {
                            hashStr = hashStr.substring(1);
                        }
                        if (hashStr.endsWith("\"")) {
                            hashStr = hashStr.substring(0, hashStr.length() - 1);
                        }
                        BigHash hash = BigHash.createHashFromString(hashStr);
                        reps0 = Long.parseLong(tokens[3]);

                        // If any chunks have zero replications, we have a problem
                        if (reps0 > 0) {
                            projectsMissingData.add(hash);
                        }

                    } catch (Exception ex) {
                        System.err.println("Could not find replications for project: Cannot parse out project replications entry from CSV: " + ex.getMessage());
                    }
                }
            } finally {
                if (reader != null) {
                    IOUtil.safeClose(reader);
                }
            }
        } // synchronized

        return projectsMissingData;
    }

    /**
     * 
     * @param hash
     * @param reps0
     * @param reps1
     * @param reps2
     * @param reps3
     * @param reps4
     * @param reps5
     * @throws java.lang.Exception
     */
    public static void setReplicationsForProject(
            final BigHash hash,
            final long reps0,
            final long reps1,
            final long reps2,
            final long reps3,
            final long reps4,
            final long reps5OrMore) throws Exception {
        if (!isReplicationsFileExist()) {
            throw new Exception("Cannot find replication CSV file: " + ServerCacheUtil.CSV_FILE_WITH_ALL_PROJECTS_REPLICATIONS);
        }

        synchronized (csvFileWithAllProjectsReplications) {
            BufferedReader reader = null;
            BufferedWriter writer = null;

            File tmpFile = null;
            try {
                // Copy contents of file over to tmp file
                tmpFile = TempFileUtil.createTemporaryFile();
                IOUtil.copyFile(csvFileWithAllProjectsReplications, tmpFile);

                // Clover replications CSV file
                reader = new BufferedReader(new FileReader(tmpFile));
                writer = new BufferedWriter(new FileWriter(csvFileWithAllProjectsReplications, false));

                boolean isFound = false;

                String entry;
                while ((entry = reader.readLine()) != null) {
                    // Skip blank lines or comments
                    if (entry == null || entry.trim().equals("") || entry.trim().startsWith("#")) {
                        continue;
                    }

                    // Does line match BigHash
                    try {
                        String[] tokens = entry.split(",");
                        String hashStr = tokens[1];

                        if (hashStr.startsWith("\"")) {
                            hashStr = hashStr.substring(1);
                        }
                        if (hashStr.endsWith("\"")) {
                            hashStr = hashStr.substring(0, hashStr.length() - 1);
                        }
                        BigHash verifyHash = BigHash.createHashFromString(hashStr);

                        // If hashes match, replace
                        if (verifyHash.equals(hash)) {
                            String title = tokens[0];
                            
                            int updateCount = 0;
                            
                            try {
                                // Field may not be there yet. Catch and add it!
                                updateCount = Integer.parseInt(tokens[9]);
                                updateCount++;
                            } catch (Exception ignore) {
                                updateCount = 1;
                            }

                            writer.write(title + ",\"" + hashStr + "\"," + System.currentTimeMillis() + "," + reps0 + "," + reps1 + "," + reps2 + "," + reps3 + "," + reps4 + "," + reps5OrMore + "," + updateCount + "\n");

                            isFound = true;
                        } // No match, just copy line over
                        else {
                            writer.write(entry + "\n");
                        }

                    } catch (Exception ex) {
                        System.err.println("Could not find replications for project: Cannot parse out project replications entry from CSV: " + ex.getMessage());
                    }
                }

                // If not found, create new line
                if (!isFound) {
                    // Skip title and status, since they don't really matter
                    writer.write("\"Unknown\",\"" + hash.toString() + "\","+System.currentTimeMillis()+"," + reps0 + "," + reps1 + "," + reps2 + "," + reps3 + "," + reps4 + "," + reps5OrMore + ",1\n");
                }
            } finally {
                if (reader != null) {
                    IOUtil.safeClose(reader);
                }
                if (writer != null) {
                    IOUtil.safeClose(writer);
                }
                if (tmpFile != null) {
                    IOUtil.safeDelete(tmpFile);
                }
            }
        } // synchronized
    }

    /**
     *
     */
    public static ProjectReplicationsEntry getReplicationsForProject(BigHash hash) throws Exception {
        if (csvFileWithAllProjectsReplications == null || !csvFileWithAllProjectsReplications.exists() || hash == null) {

            if (csvFileWithAllProjectsReplications == null) {
                System.out.println("Could not find replications for project: csvFileWithAllProjectsReplications is null!");
            } else if (!csvFileWithAllProjectsReplications.exists()) {
                System.out.println("Could not find replications for project: csvFileWithAllProjectsReplications doesn't exist at " + csvFileWithAllProjectsReplications.getAbsolutePath());
            } else {
                System.out.println("Could not find replications for project: hash is null");
            }
            return null;
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(csvFileWithAllProjectsReplications));

            String entry;
            while ((entry = reader.readLine()) != null) {
                // Skip blank lines or comments
                if (entry == null || entry.trim().equals("") || entry.trim().startsWith("#")) {
                    continue;
                }

                // Does line match BigHash
                try {
                    String[] tokens = entry.split(",");
                    String hashStr = tokens[1];
                    if (hashStr.startsWith("\"")) {
                        hashStr = hashStr.substring(1);
                    }
                    if (hashStr.endsWith("\"")) {
                        hashStr = hashStr.substring(0, hashStr.length() - 1);
                    }
                    BigHash verifyHash = BigHash.createHashFromString(hashStr);
                    if (verifyHash.equals(hash)) {
                        return ProjectReplicationsEntry.createEntryFromCSV(entry);
                    }

                } catch (Exception ex) {
                    System.err.println("Could not find replications for project: Cannot parse out project replications entry from CSV: " + ex.getMessage());
                }
            }
        } finally {
            if (reader != null) {
                IOUtil.safeClose(reader);
            }
        }
        return null;
    }

    /**
     * <p>Returns the string representing the base URL for the build.</p>
     * <p>For the production build environment, this will be "http://tranche.proteomecommons.org/activity/"</p>
     * <p>This is used so that different environments can still provide correct HTTP paths, particularly important for servlets</p>
     * @return The string representing the base URL for the build.
     */
    public static String getUrlPrefix() {
        return BUILD.getUrlPathPrefix();
    }
} // ServerCacheUtil

/**
 * 
 */
class BuildEnvironment {

    private String environmentName;
    private String serverLogPath = null,  serverCachePath = null,  projectDownloadPath = null,  replicationCSVPath = null,  urlPathPrefix = null,  stressTestLogPath = null;

    private BuildEnvironment(String environmentName) {
        this.environmentName = environmentName;
    }

    /**
     * 
     * @param environmentName Just a label for environment. Used mostly for readability.
     */
    public static BuildEnvironment createEnvironment(String environmentName) {
        return new BuildEnvironment(environmentName);
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public String getServerLogPath() {
        return serverLogPath;
    }

    public void setServerLogPath(String serverLogPath) {
        this.serverLogPath = serverLogPath;
    }

    public String getServerCachePath() {
        return serverCachePath;
    }

    public void setServerCachePath(String serverCachePath) {
        this.serverCachePath = serverCachePath;
    }

    public String getProjectDownloadPath() {
        return projectDownloadPath;
    }

    public void setProjectDownloadPath(String projectDownloadPath) {
        this.projectDownloadPath = projectDownloadPath;
    }

    public String getReplicationCSVPath() {
        return replicationCSVPath;
    }

    public void setReplicationCSVPath(String replicationCSVPath) {
        this.replicationCSVPath = replicationCSVPath;
    }

    public String getUrlPathPrefix() {
        return urlPathPrefix;
    }

    public void setUrlPathPrefix(String urlPathPrefix) {
        this.urlPathPrefix = urlPathPrefix;
    }

    public String getStressTestLogPath() {
        return stressTestLogPath;
    }

    public void setStressTestLogPath(String stressTestLogPath) {
        this.stressTestLogPath = stressTestLogPath;
    }
}

/**
 * 
 * @author besmit
 */
class DataAssertionException extends RuntimeException {

    DataAssertionException(String msg) {
        super(msg);
    }
}
