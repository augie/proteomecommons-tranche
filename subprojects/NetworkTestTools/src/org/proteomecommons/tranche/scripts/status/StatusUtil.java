/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.scripts.status;

import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.tranche.TrancheServer;
import org.tranche.commons.TextUtil;
import org.tranche.configuration.ConfigKeys;
import org.tranche.configuration.Configuration;
import org.tranche.configuration.ServerModeFlag;
import org.tranche.flatfile.DataDirectoryConfiguration;
import org.tranche.hash.span.AbstractHashSpan;
import org.tranche.hash.span.HashSpan;
import org.tranche.network.ConnectionUtil;
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTable;
import org.tranche.network.StatusTableRow;
import org.tranche.security.SecurityUtil;
import org.tranche.server.GetNetworkStatusItem;
import org.tranche.util.IOUtil;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class StatusUtil {

    public static final String HR = "------------------------------------------------------------------------------------------------------------------------------------------------------";

    /**
     * 
     * @return
     */
    public static NetworkInformation getTrancheStatusTable() throws Exception {
        return getTrancheStatusTable(false);
    }
    
    /**
     * 
     * @return
     * @throws java.lang.Exception
     */
    public static NetworkInformation getTrancheStatusTableAndPrintInformation() throws Exception {
        return getTrancheStatusTable(true);
    }

    /**
     * 
     * @param isPrintOutInformation
     * @return
     * @throws java.lang.Exception
     */
    private static NetworkInformation getTrancheStatusTable(boolean isPrintOutInformation) throws Exception {
        ProteomeCommonsTrancheConfig.load();
        NetworkUtil.waitForStartup();

        NetworkInformation networkInfo = new NetworkInformation();

        for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {

            TrancheStatusTableRow statusRow = addServerInformationByHost(row.getHost(), networkInfo, isPrintOutInformation);

            if (statusRow == null && row != null) {
                networkInfo.getOfflineHosts().add(row.getHost());
            }

            if (statusRow != null && row.isCore() && row.isOnline()) {
                networkInfo.getStatusTable().add(statusRow);
            }
        }

        return networkInfo;
    }

    /**
     * 
     * @param host
     * @return
     * @throws java.lang.Exception
     */
    private static TrancheStatusTableRow addServerInformationByHost(final String host, final NetworkInformation networkInfo, boolean isPrintOutInformation) throws Exception {
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
                if (isPrintOutInformation) {
                    System.err.println(host + " is not online. Skipping.");
                }
                return null;
            }

            try {
                Configuration config = IOUtil.getConfiguration(ts, SecurityUtil.getAnonymousCertificate(), SecurityUtil.getAnonymousKey());

                if (isPrintOutInformation) {
                    System.out.println();
                    System.out.println(HR);
                }

                String name = config.getName();

                if (isPrintOutInformation) {
                    if (name == null || name.trim().equals("")) {
                        System.out.println(host);
                    } else {
                        System.out.println(name + " (" + host + ")");
                    }
                    System.out.println(HR);
                }

                final String buildNumber = config.getValue(ConfigKeys.BUILD_NUMBER);
//                final String name = config.getValue(ConfigKeys.NAME);

                if (buildNumber != null && isPrintOutInformation) {
                    System.out.println();
                    System.out.println("Build number: " + buildNumber);
                }

                if (isPrintOutInformation) {
                    System.out.println();
                    System.out.println("Server status (admin): " + config.getValue(ConfigKeys.SERVER_MODE_DESCRIPTION_ADMIN));
                    System.out.println("Server status (system): " + config.getValue(ConfigKeys.SERVER_MODE_DESCRIPTION_SYSTEM));
                    System.out.println();
                }

                long thisServerLimit = 0;

                if (isPrintOutInformation) {
                    System.out.println("Data directories (" + config.getDataDirectories().size() + "):");
                }
                for (DataDirectoryConfiguration ddc : config.getDataDirectories()) {

                    if (isPrintOutInformation) {
                        System.out.println("    * " + ddc.getDirectory() + ": " + TextUtil.formatBytes(ddc.getSizeLimit()));
                    }
                    thisServerLimit += ddc.getSizeLimit();
                    networkInfo.totalSizeLimit += ddc.getSizeLimit();
                }

                if (isPrintOutInformation) {
                    System.out.println();
                    System.out.println("    Total size ... " + config.getValue(ConfigKeys.TOTAL_SIZE));
                    System.out.println("    Used ......... " + config.getValue(ConfigKeys.TOTAL_SIZE_USED));
                    System.out.println("    Available .... " + config.getValue(ConfigKeys.TOTAL_SIZE_UNUSED));

                    System.out.println();
                    System.out.println("Hash spans for <" + host + ">: " + config.getHashSpans().size());
                    System.out.println();
                }
                for (HashSpan hs : config.getHashSpans()) {
                    AbstractHashSpan ahs = new AbstractHashSpan(hs);

                    if (isPrintOutInformation) {
                        System.out.println("    - Start:  " + ahs.getAbstractionFirst());
                        System.out.println("      Finish: " + ahs.getAbstractionLast());
                    }

                    if (config.canWrite()) {
                        networkInfo.writableHashSpans.add(ahs);
                    }
                }

                if (isPrintOutInformation) {
                    System.out.println();
                    System.out.println("Target hash spans: " + config.getTargetHashSpans().size());
                    System.out.println();
                }

                for (HashSpan hs : config.getTargetHashSpans()) {
                    AbstractHashSpan ahs = new AbstractHashSpan(hs);
                    if (isPrintOutInformation) {
                        System.out.println("    - Start:  " + ahs.getAbstractionFirst());
                        System.out.println("      Finish: " + ahs.getAbstractionLast());
                    }

                    if (config.canWrite()) {
                        networkInfo.writableTargetHashSpans.add(ahs);
                    }
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

                if (isPrintOutInformation) {
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
                }


                final byte serverModeAdmin = Byte.parseByte(config.getValue(ConfigKeys.SERVER_MODE_FLAG_ADMIN));
                final boolean isAdminWritable = ServerModeFlag.canWrite(serverModeAdmin);

                final long totalDisk = thisServerLimit;
                long totalDiskSpaceUsed = 0;

                for (String key : config.getValueKeys()) {
                    if (key.startsWith("actualBytesUsed:")) {
                        totalDiskSpaceUsed += Long.parseLong(config.getValue(key));
                    }
                }

                boolean isStartingUp = false;

                // Determine whether server is still starting up
                {
//                    byte systemFlag = Byte.parseByte(config.getValue(ConfigKeys.SERVER_MODE_FLAG_SYSTEM));
//
//                    if (ServerModeFlag.canRead(systemFlag) && ServerModeFlag.canWrite(systemFlag)) {
//                        isStartingUp = false;
//                    }
                    
                    String serverStartupMessage = config.getValue(ConfigKeys.SERVER_STARTUP_THREAD_STATUS);
                    
                    if (serverStartupMessage != null && serverStartupMessage.contains("Waiting for datablocks to load")) {
                        isStartingUp = true;
                    }
                }

                TrancheStatusTableRow trow = new TrancheStatusTableRow(host, name, buildNumber, IOUtil.createURL(ts), isAdminWritable, totalDisk, totalDiskSpaceUsed, isStartingUp);
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
