/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.scripts;

import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.proteomecommons.tranche.scripts.status.TrancheStatusTable;
import org.proteomecommons.tranche.scripts.status.TrancheStatusTableRow;
import org.tranche.TrancheServer;
import org.tranche.configuration.Configuration;
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

    public static void main(String[] args) throws Exception {
        printOnlineServerInfo();
    }
    
    private static void printOnlineServerInfo() throws Exception {
        ProteomeCommonsTrancheConfig.load();
        NetworkUtil.waitForStartup();

        System.out.println("Row count: " + NetworkUtil.getStatus().getRows());

        TrancheStatusTable table = new TrancheStatusTable();

        int count = 0;
        for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {

            TrancheStatusTableRow statusRow = getServerInformationByHost(row.getHost());

            if (statusRow != null) {
                table.add(statusRow);
                count++;
            }
        }

        System.out.println();
        System.out.println("***********************************************************************************");
        System.out.println(" SUMMARY");
        System.out.println("***********************************************************************************");
        System.out.println("Total online servers: " + count);
        System.out.println();

        table.printTable();
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

            System.out.println();
            System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------");
            System.out.println(host);
            System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------");

            try {
                Configuration config = IOUtil.getConfiguration(ts, SecurityUtil.getAnonymousCertificate(), SecurityUtil.getAnonymousKey());

                System.out.println();
                System.out.println("Hash spans for <" + host + ">: " + config.getHashSpans().size());
                System.out.println();
                for (HashSpan hs : config.getHashSpans()) {
                    AbstractHashSpan ahs = new AbstractHashSpan(hs);
                    System.out.println("    - Start:  " + ahs.getAbstractionFirst());
                    System.out.println("      Finish: " + ahs.getAbstractionLast());
                }

                System.out.println();
                System.out.println("Target hash spans for <" + host + ">: " + config.getTargetHashSpans().size());
                System.out.println();
                for (HashSpan hs : config.getTargetHashSpans()) {
                    AbstractHashSpan ahs = new AbstractHashSpan(hs);
                    System.out.println("    - Start:  " + ahs.getAbstractionFirst());
                    System.out.println("      Finish: " + ahs.getAbstractionLast());
                }

                StatusTable t = ts.getNetworkStatusPortion(GetNetworkStatusItem.RETURN_ALL, GetNetworkStatusItem.RETURN_ALL);
                int online = 0, total = 0, core = 0, onlineCore = 0;

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
                }

                System.out.println();
                System.out.println("Network status table:");
                System.out.println("    Online:         " + online);
                System.out.println("    Core:           " + core);
                System.out.println("    Online & Core:  " + onlineCore);
                System.out.println("    Total:          " + total);

                TrancheStatusTableRow trow = new TrancheStatusTableRow(host);
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
