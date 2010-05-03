/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.scripts;

import java.io.PrintStream;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.tranche.TrancheServer;
import org.tranche.commons.TextUtil;
import org.tranche.get.GetFileTool;
import org.tranche.hash.BigHash;
import org.tranche.meta.*;
import org.tranche.network.ConnectionUtil;
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTableRow;
import org.tranche.security.*;
import org.tranche.time.*;

/**
 * <p>Finds all project files on network that belong to user and prints out data set information for each.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class FindProjectFilesForUserScript implements TrancheScript {

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {
        try {

            if (args.length < 1) {
                System.err.println("Expecting one or more arguments (one or more username), but instead found: " + args.length);
                printUsage(System.err);
                System.exit(1);
            }

            Set<String> usernames = new HashSet();
            System.out.println("Looking for all data sets uploaded by the following " + args.length + " user(s):");
            for (int i = 0; i < args.length; i++) {
                String username = args[i];
                System.out.println("    * " + username);
                usernames.add(username);
            }


            ProteomeCommonsTrancheConfig.load();
            NetworkUtil.waitForStartup();

            int onlineCoreCount = 0;

            for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {
                if (row.isOnline()) {
                    onlineCoreCount++;
                }
            }

            System.out.println("Total of " + onlineCoreCount + " online, core servers.");

            // Step 1: collect all project hashes from servers
            final Set<BigHash> projectHashes = getAllProjectHashes();
            System.out.println("Found a total of " + projectHashes.size() + " projects.");

            int count = 0;
            final int maxAttempts = 3;
            int matchCount = 0;

            // Step 2: one project at a time
            for (BigHash projectHash : projectHashes) {
                count++;
                if (count % 1000 == 0) {
                    System.out.println("    ... finished "+count+" of "+projectHashes.size()+" projects: "+TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()));
                }
                ATTEMPT:
                for (int attempt = 0; attempt < maxAttempts; attempt++) {
                    try {
                        MetaData projectMD = getMetaData(projectHash);
                        for (int i = 0; i < projectMD.getUploaderCount(); i++) {
                            projectMD.selectUploader(i);
                            Signature sig = projectMD.getSignature();
                            final String sigName = sig.getUserName();

//                            System.out.println("DEBUG> Project #" + String.valueOf(count + 1) + " of " + String.valueOf(projectHashes.size()) + ": " + sigName);

                            for (String username : usernames) {
                                // Found user!
                                if (sigName.equals(username)) {
                                    printProjectInfo(projectHash, projectMD, username);
                                    matchCount++;
                                    break ATTEMPT;
                                }
                            }
                        }
                        break ATTEMPT;
                    } catch (Exception nope) { /* skip */ }
                }
            }
            
            System.out.println("Total matching data sets: "+matchCount);
        } catch (Exception e) {
            System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
            printUsage(System.err);
            System.exit(2);
        } finally {
        }

        System.out.println("~ fin ~");
        System.exit(0);
    }

    /**
     * 
     * @param out
     */
    private static void printUsage(PrintStream out) {
        out.println();
        out.println("USAGE");
        out.println("   FindProjectFilesForUserScript <username>");
        out.println();
        out.println("DESCRIPTION");
        out.println("   Finds all project files on network that belong to user and prints out data set information for each.");
        out.println();
        out.println("USAGE");
        out.println("   0: Exit normally");
        out.println("   1: Missing parameters/wrong number of parameters");
        out.println("   2: Unknown error (see standard error)");
        out.println();
    }

    private static void printProjectInfo(BigHash hash, MetaData metaData, String username) {
        final String HR = "----------------------------------------------------------------------------------------------------------------";
        System.out.println();
        System.out.println(HR);
        System.out.println(hash);
        System.out.println();
        System.out.println(" * " + metaData.getUploaderCount() + " uploader(s)");
        for (int uploader = 0; uploader < metaData.getUploaderCount(); uploader++) {
            metaData.selectUploader(uploader);
            System.out.println("    " + String.valueOf(uploader + 1) + ". " + metaData.getSignature().getUserName());
        }
        System.out.println();
        System.out.println(" * " + metaData.getDataSetFiles() + " files, " + TextUtil.formatBytes(metaData.getDataSetSize()));
        System.out.println();
        if (metaData.isEncrypted()) {
            if (metaData.isPublicPassphraseSet()) {
                System.out.println(" * Public (public passphrase)");
            } else {
                System.out.println(" * Encrypted");
            }
        } else {
            System.out.println(" * Public (unencrypted)");
        }
        System.out.println(HR);
    }

    /**
     * 
     * @param hash
     * @return
     * @throws java.lang.Exception
     */
    private static MetaData getMetaData(BigHash hash) throws Exception {
        try {
            GetFileTool gft = new GetFileTool();
            gft.setHash(hash);

            return gft.getMetaData();
        } finally {
        }
    }

    /**
     * 
     * @return
     */
    private static Set<BigHash> getAllProjectHashes() {
        Set<BigHash> projectHashes = new HashSet();

        for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {
            final boolean isConnected = ConnectionUtil.isConnected(row.getHost());
            if (row.isOnline()) {
                ATTEMPT:
                for (int attempt = 0; attempt < 3; attempt++) {
                    TrancheServer ts = null;
                    try {
                        if (!isConnected) {
                            ts = ConnectionUtil.connectHost(row.getHost(), true);
                        } else {
                            ts = ConnectionUtil.getHost(row.getHost());
                        }

                        BigInteger offset = BigInteger.ZERO;
                        BigInteger batch = BigInteger.valueOf(100);

                        while (true) {
                            BigHash[] hashes = ts.getProjectHashes(offset, batch);

                            if (hashes.length == 0) {
                                break;
                            }

                            for (BigHash h : hashes) {
                                projectHashes.add(h);
                            }
                            offset = offset.add(batch);
                        }

                        break ATTEMPT;
                    } catch (Exception e) {
                        System.err.println(e.getClass().getSimpleName() + " occurred while injecting chunk to " + row.getHost() + ": " + e.getMessage());
                        e.printStackTrace(System.err);
                    } finally {
                        if (!isConnected) {
                            ConnectionUtil.unlockConnection(row.getHost());
                            ConnectionUtil.safeCloseHost(row.getHost());
                        }
                    }
                } // Attempt loop
            } // If online
        } // For each server

        return projectHashes;
    } // getAllProjectHashes
}
