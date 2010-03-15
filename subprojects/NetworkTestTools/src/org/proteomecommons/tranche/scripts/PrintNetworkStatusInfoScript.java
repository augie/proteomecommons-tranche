/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.scripts;

import java.util.HashSet;
import java.util.Set;
import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.proteomecommons.tranche.scripts.status.TrancheStatusTable;
import org.proteomecommons.tranche.scripts.status.TrancheStatusTableRow;
import org.proteomecommons.tranche.scripts.utils.ScriptsUtil;
import org.tranche.TrancheServer;
import org.tranche.configuration.ConfigKeys;
import org.tranche.configuration.Configuration;
import org.tranche.configuration.ServerModeFlag;
import org.tranche.flatfile.DataDirectoryConfiguration;
import org.tranche.hash.span.AbstractHashSpan;
import org.tranche.hash.span.HashSpan;
import org.tranche.network.*;
import org.tranche.server.GetNetworkStatusItem;
import org.tranche.security.*;
import org.tranche.util.*;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class PrintNetworkStatusInfoScript implements TrancheScript {

    public static final boolean DEFAULT_REGISTER_SERVERS = false;
    private static final String HR = "------------------------------------------------------------------------------------------------------------------------------------------------------";
    private static long totalSizeLimit = 0;

    /**
     * 
     * @param args
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
        try {
            printOnlineServerInfo(args);
        } catch (Exception e) {
            System.err.println("Unknown exception " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    /**
     * 
     * @param args
     * @throws java.lang.Exception
     */
    private static void printOnlineServerInfo(String[] args) throws Exception {

        boolean isRegister = DEFAULT_REGISTER_SERVERS;

        for (int i = 0; i < args.length; i += 2) {
            try {
                String name = args[i];
                String value = args[i + 1];

                if (name.equals("-r") || name.equals("--register")) {
                    isRegister = Boolean.parseBoolean(value);
                    System.out.println("    Parameter: setting register to " + isRegister);
                } else {
                    System.err.println("Unrecognized parameter: " + name);
                    System.exit(3);
                }
            } catch (Exception e) {
                System.err.println("Problem parsing parameters, " + e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace(System.err);
                System.exit(2);
            }
        }

        ProteomeCommonsTrancheConfig.load();
        NetworkUtil.waitForStartup();

        Set<String> offlineHosts = new HashSet();

        System.out.println("Row count: " + NetworkUtil.getStatus().getRows());

        TrancheStatusTable table = new TrancheStatusTable();

        int count = 0;
        for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {

            TrancheStatusTableRow statusRow = getServerInformationByHost(row.getHost());

            if (statusRow == null && row != null) {
                offlineHosts.add(row.getHost());
            }

            if (statusRow != null && row.isCore() && row.isOnline()) {
                table.add(statusRow);
                count++;
            }
        }

        System.out.println();
        System.out.println("***********************************************************************************");
        System.out.println(" SUMMARY");
        System.out.println("***********************************************************************************");
        System.out.println();
        System.out.println(HR);
        System.out.println("Total offline servers: " + offlineHosts.size());
        System.out.println(HR);
        for (String host : offlineHosts) {
            System.out.println("    * " + ScriptsUtil.getServerNameAndHostName(host));
        }
        System.out.println();
        System.out.println(HR);
        System.out.println("Total online servers: " + count);
        System.out.println(HR);
        System.out.println();
        
        System.out.println(HR);
        System.out.println("Disk space summary");
        System.out.println(HR);
        System.out.println();
        
        long writableTotal = 0, writableUsed = 0, nonWritableTotal = 0, nonWritableUsed = 0;
        int writableCount = 0, nonWritableCount = 0;
        
        for (TrancheStatusTableRow row : table.getRows()) {
            if (row.isAdminWritabled) {
                writableTotal += row.diskSpace;
                writableUsed += row.diskSpaceUsed;
                writableCount++;
            } else {
                nonWritableTotal += row.diskSpace;
                nonWritableUsed += row.diskSpaceUsed;
                nonWritableCount++;
            }
        }
        
        System.out.println("                       Total                Used               Available");
        System.out.println("    Total              "+Text.getFormattedBytes(writableTotal+nonWritableTotal)+"              "+Text.getFormattedBytes(writableUsed+nonWritableUsed)+"            "+Text.getFormattedBytes((writableTotal+nonWritableTotal)-(writableUsed+nonWritableUsed)));
        System.out.println("    Non-writable ("+nonWritableCount+")   "+Text.getFormattedBytes(nonWritableTotal)+"              "+Text.getFormattedBytes(nonWritableUsed)+"            "+Text.getFormattedBytes(nonWritableTotal-nonWritableUsed));
        System.out.println("    Writable ("+writableCount+")       "+Text.getFormattedBytes(writableTotal)+"                "+Text.getFormattedBytes(writableUsed)+"            "+Text.getFormattedBytes(writableTotal-writableUsed));
        
        System.out.println();

        table.printTable();

        if (isRegister) {
            System.out.println();
            System.out.println(HR);
            System.out.println("Registering servers...");
            System.out.println(HR);
            Set<String> onlineUrls = new HashSet();
            for (TrancheStatusTableRow row : table.getRows()) {
                onlineUrls.add(row.url);
            }

            final int MAX_ATTEMPTS = 3;

            for (String url : onlineUrls) {

                final String host = IOUtil.parseHost(url);

                ATTEMPTS:
                for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {

                    TrancheServer ts = null;
                    boolean wasConnected = true;
                    try {
                        ts = ConnectionUtil.getHost(host);

                        if (ts == null) {
                            ts = ConnectionUtil.connectHost(host, true);
                            wasConnected = false;
                        }

                        int registerCount = 0;

                        REGISTERING_SERVERS:
                        for (String nextUrl : onlineUrls) {

                            String nextHost = IOUtil.parseHost(nextUrl);

                            // Don't register host with self
                            if (nextHost.equals(host)) {
                                continue REGISTERING_SERVERS;
                            }

                            ts.registerServer(nextUrl);
                            registerCount++;
                        }

                        System.out.println("    * Registered " + registerCount + " servers with: " + url);

                        break ATTEMPTS;
                    } catch (Exception e) {
                        if ((attempt + 1) == MAX_ATTEMPTS) {
                            System.err.println("    * Failed to register servers with " + host + "; " + e.getClass().getSimpleName() + ": " + e.getMessage());
                            e.printStackTrace(System.err);
                        }
                    } finally {
                        IOUtil.safeClose(ts);
                        if (!wasConnected) {
                            ConnectionUtil.unlockConnection(host);
                        }
                    }
                }
            }
        }

        System.out.println("Total size limit: " + Text.getFormattedBytes(totalSizeLimit));

        System.out.println();
        System.out.println("~ fin ~");
    }

    private static TrancheStatusTableRow getServerInformationByHost(final String host) throws Exception {
        final boolean isConnected = ConnectionUtil.isConnected(host);

        final TrancheServer[] tsArr = {null};

        try {
            Thread thread = new Thread("Connect to " + host + " thread") {

                @Override()
                public void run() {
                    try {
                        if (!isConnected) {
                            tsArr[0] = ConnectionUtil.connectHost(host, true);
                        } else {
                            tsArr[0] = ConnectionUtil.getHost(host);
                        }
                    } catch (Exception e) {
                    }
                }
            };
            thread.setDaemon(true);
            thread.start();

            thread.join(10 * 1000);

            if (thread.isAlive()) {
                thread.interrupt();
            }

            TrancheServer ts = tsArr[0];

            if (ts == null) {
                System.err.println(host + " is not online. Skipping.");
                return null;
            }

            try {
                Configuration config = IOUtil.getConfiguration(ts, SecurityUtil.getAnonymousCertificate(), SecurityUtil.getAnonymousKey());

                System.out.println();
                System.out.println(HR);

                String name = config.getName();

                if (name == null || name.trim().equals("")) {
                    System.out.println(host);
                } else {
                    System.out.println(name+" ("+host+")");
                }
                System.out.println(HR);

                final String buildNumber = config.getValue(ConfigKeys.BUILD_NUMBER);
//                final String name = config.getValue(ConfigKeys.NAME);

                if (buildNumber != null) {
                    System.out.println();
                    System.out.println("Build number: " + buildNumber);
                }

                System.out.println();
                System.out.println("Server status (admin): " + config.getValue(ConfigKeys.SERVER_MODE_DESCRIPTION_ADMIN));
                System.out.println("Server status (system): " + config.getValue(ConfigKeys.SERVER_MODE_DESCRIPTION_SYSTEM));
                System.out.println();

                long thisServerLimit = 0;
                
                System.out.println("Data directories (" + config.getDataDirectories().size() + "):");
                for (DataDirectoryConfiguration ddc : config.getDataDirectories()) {
                    // Tranche uses DDCs with this value, so include them, even if wrong limit
//                    if (ddc.getSizeLimit() == Long.MAX_VALUE) {
//                        continue;
//                    }
                    System.out.println("    * " + ddc.getDirectory() + ": " + Text.getFormattedBytes(ddc.getSizeLimit()));
                    thisServerLimit += ddc.getSizeLimit();
                    totalSizeLimit += ddc.getSizeLimit();
                }

                System.out.println();
                System.out.println("    Total size ... " + config.getValue(ConfigKeys.TOTAL_SIZE));
                System.out.println("    Used ......... " + config.getValue(ConfigKeys.TOTAL_SIZE_USED));
                System.out.println("    Available .... " + config.getValue(ConfigKeys.TOTAL_SIZE_UNUSED));
               
                System.out.println();
                System.out.println("Hash spans for <" + host + ">: " + config.getHashSpans().size());
                System.out.println();
                for (HashSpan hs : config.getHashSpans()) {
                    AbstractHashSpan ahs = new AbstractHashSpan(hs);
                    System.out.println("    - Start:  " + ahs.getAbstractionFirst());
                    System.out.println("      Finish: " + ahs.getAbstractionLast());
                }

                System.out.println();
                System.out.println("Target hash spans: " + config.getTargetHashSpans().size());
                System.out.println();
                for (HashSpan hs : config.getTargetHashSpans()) {
                    AbstractHashSpan ahs = new AbstractHashSpan(hs);
                    System.out.println("    - Start:  " + ahs.getAbstractionFirst());
                    System.out.println("      Finish: " + ahs.getAbstractionLast());
                }

                StatusTable t = ts.getNetworkStatusPortion(GetNetworkStatusItem.RETURN_ALL, GetNetworkStatusItem.RETURN_ALL);
                int online = 0, total = 0, core = 0, onlineCore = 0, onlineReadable = 0, onlineWritable = 0, onlineReadableAndWritable = 0;

                for (StatusTableRow r : t.getRows()) {
                    total++;
                    if (r.isOnline()) {
                        online++;
                    }
                    if (r.isCore()) {
                        core++;
                    }
                    if (r.isCore() && r.isOnline()) {
                        onlineCore++;
                    }
                    if (r.isReadable()) {
                        onlineReadable++;
                    }
                    if (r.isWritable()) {
                        onlineWritable++;
                    }
                    if (r.isReadable() && r.isWritable()) {
                        onlineReadableAndWritable++;
                    }
                }

                System.out.println();
                System.out.println("Network status table:");
                System.out.println("    Online:            " + online);
                System.out.println("    Core:              " + core);
                System.out.println("    Online & Core:     " + onlineCore);
                System.out.println();
                System.out.println("    Readable:          " + onlineReadable);
                System.out.println("    Writable:          " + onlineWritable);
                System.out.println("    Readable/Writable: " + onlineReadableAndWritable);
                System.out.println();
                System.out.println("    Total:             " + total);
                
                
                final byte serverModeAdmin = Byte.parseByte(config.getValue(ConfigKeys.SERVER_MODE_FLAG_ADMIN));
                final boolean isAdminWritable = ServerModeFlag.canWrite(serverModeAdmin);
                
                final long totalDisk = thisServerLimit;
                long totalDiskSpaceUsed = 0;
                
                for (String key : config.getValueKeys()) {
                    if (key.startsWith("actualBytesUsed:")) {
                        totalDiskSpaceUsed += Long.parseLong(config.getValue(key));
                    }
                }

                TrancheStatusTableRow trow = new TrancheStatusTableRow(host, name, buildNumber, IOUtil.createURL(ts), isAdminWritable, totalDisk, totalDiskSpaceUsed);
                trow.add(t);

                return trow;
            } catch (Exception e) {
                System.err.println(e.getClass().getSimpleName() + " for " + host + ": " + e.getMessage());
                e.printStackTrace(System.err);
                return null;
            }
        } finally {
            if (!isConnected) {
                ConnectionUtil.unlockConnection(host);
                ConnectionUtil.safeCloseHost(host);
            }
        }
    }
}
