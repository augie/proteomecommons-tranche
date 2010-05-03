/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.scripts;

import java.util.HashSet;
import java.util.Set;
import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.proteomecommons.tranche.scripts.status.NetworkInformation;
import org.proteomecommons.tranche.scripts.status.StatusUtil;
import org.proteomecommons.tranche.scripts.status.TrancheStatusTableRow;
import org.proteomecommons.tranche.scripts.utils.ScriptsUtil;
import org.tranche.TrancheServer;
import org.tranche.commons.TextUtil;
import org.tranche.network.*;
import org.tranche.util.*;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class PrintNetworkStatusInfoScript implements TrancheScript {

    public static final boolean DEFAULT_REGISTER_SERVERS = false;
    
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

        System.out.println("Row count: " + NetworkUtil.getStatus().getRows());

        NetworkInformation networkInfo = StatusUtil.getTrancheStatusTableAndPrintInformation();

        System.out.println();
        System.out.println("***********************************************************************************");
        System.out.println(" SUMMARY");
        System.out.println("***********************************************************************************");
        System.out.println();
        System.out.println(StatusUtil.HR);
        System.out.println("Total offline servers: " + networkInfo.getOfflineHosts().size());
        System.out.println(StatusUtil.HR);
        for (String host : networkInfo.getOfflineHosts()) {
            System.out.println("    * " + ScriptsUtil.getServerNameAndHostName(host));
        }
        System.out.println();
        System.out.println(StatusUtil.HR);
        System.out.println("Total online servers: " + networkInfo.getStatusTable().getRows().size());
        System.out.println(StatusUtil.HR);
        System.out.println();

        System.out.println(StatusUtil.HR);
        System.out.println("Disk space summary");
        System.out.println(StatusUtil.HR);
        System.out.println();

        long writableTotal = 0, writableUsed = 0, nonWritableTotal = 0, nonWritableUsed = 0;
        int writableCount = 0, nonWritableCount = 0;

        for (TrancheStatusTableRow row : networkInfo.getStatusTable().getRows()) {
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
        System.out.println("    Total              " + TextUtil.formatBytes(writableTotal + nonWritableTotal) + "              " + TextUtil.formatBytes(writableUsed + nonWritableUsed) + "            " + TextUtil.formatBytes((writableTotal + nonWritableTotal) - (writableUsed + nonWritableUsed)));
        System.out.println("    Non-writable (" + nonWritableCount + ")   " + TextUtil.formatBytes(nonWritableTotal) + "              " + TextUtil.formatBytes(nonWritableUsed) + "            " + TextUtil.formatBytes(nonWritableTotal - nonWritableUsed));
        System.out.println("    Writable (" + writableCount + ")       " + TextUtil.formatBytes(writableTotal) + "                " + TextUtil.formatBytes(writableUsed) + "            " + TextUtil.formatBytes(writableTotal - writableUsed));

        System.out.println();

        networkInfo.getStatusTable().printTable();

        System.out.println();
        System.out.println("Writable hash spans:");
        System.out.println("    - Target: " + networkInfo.getNumberOfWritableTargetHashSpans() + " (at least)");
        System.out.println("    - Normal: " + networkInfo.getNumberOfWritableHashSpans() + " (at least)");

        System.out.println();
        System.out.println("Total size limit: " + TextUtil.formatBytes(networkInfo.getTotalSizeLimit()));
        System.out.println();

        if (isRegister) {
            System.out.println();
            System.out.println(StatusUtil.HR);
            System.out.println("Registering servers...");
            System.out.println(StatusUtil.HR);
            Set<String> onlineUrls = new HashSet();
            for (TrancheStatusTableRow row : networkInfo.getStatusTable().getRows()) {
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

        System.out.println();
        System.out.println("~ fin ~");
    }

    
}
