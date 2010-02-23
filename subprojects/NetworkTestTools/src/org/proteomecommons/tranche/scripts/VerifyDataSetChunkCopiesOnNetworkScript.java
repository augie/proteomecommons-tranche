/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.scripts;

import java.io.PrintStream;
import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.tranche.TrancheServer;
import org.tranche.get.GetFileTool;
import org.tranche.network.*;
import org.tranche.hash.BigHash;
import org.tranche.meta.MetaData;
import org.tranche.project.ProjectFile;
import org.tranche.project.ProjectFilePart;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class VerifyDataSetChunkCopiesOnNetworkScript implements TrancheScript {

    /**
     * <p>Maximum number of times to attempt to check server for chunk.</p>
     */
    private final static int MAX_ATTEMPTS = 3;

    public static void main(String[] args) {
        try {
            String hashStr = null, passphraseStr = null;

            for (int i = 0; i < args.length; i += 2) {
                String name = args[i];
                String value = args[i + 1];

                if (name.equals("-h")) {
                    hashStr = value;
                } else if (name.equals("-p")) {
                    passphraseStr = value;
                } else {
                    System.out.println("Unknown parameter: " + name);
                    printUsage(System.err);
                    System.exit(3);
                }
            }

            if (hashStr == null) {
                System.out.println("Missing: hash");
                printUsage(System.err);
                System.exit(1);
            }

            final BigHash dataSetHash = BigHash.createHashFromString(hashStr);
            final String passphrase = passphraseStr;

            ProteomeCommonsTrancheConfig.load();
            NetworkUtil.waitForStartup();

            System.out.println("\"chunk type\",\"hash\",\"copies\"");

            // 1. How many copies of ProjectFile meta data chunk?
            {
                int copies = getCopiesOfChunk(dataSetHash, true);
                System.out.println("\"Meta data (ProjectFile)\",\"" + dataSetHash + "\"," + copies);
            }

            GetFileTool gft = new GetFileTool();
            gft.setHash(dataSetHash);
            if (passphrase != null) {
                gft.setPassphrase(passphrase);
            }
            MetaData md = gft.getMetaData();
            ProjectFile pf = null;

            try {

                // 2. How many copies of ProjectFile data chunks?
                for (BigHash dataChunkHash : md.getParts()) {
                    int copies = getCopiesOfChunk(dataChunkHash, false);
                    System.out.println("\"Data (ProjectFile)\",\"" + dataChunkHash + "\", " + copies);
                }

                pf = gft.getProjectFile();
                for (ProjectFilePart pfp : pf.getParts()) {

                    // 3. How many copies of each file's meta data chunk?
                    {
                        int copies = getCopiesOfChunk(pfp.getHash(), true);
                        System.out.println("\"Meta data (" + pfp.getRelativeName() + ")\",\"" + pfp.getHash() + "\"," + copies);
                    }

                    gft.setHash(pfp.getHash());
                    MetaData fileMD = gft.getMetaData();

                    // 4. How many copies of each file's data chunks?
                    {
                        for (BigHash dataChunkHash : fileMD.getParts()) {
                            int copies = getCopiesOfChunk(dataChunkHash, false);
                            System.out.println("\"Data (" + pfp.getRelativeName() + ")\",\"" + dataChunkHash + "\"," + copies);
                        }
                    }
                }

            } finally {
                try {
                    pf.close();
                } catch (Exception e) { /* nope */ }
            }
        } catch (Exception e) {
            System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
            printUsage(System.err);
            System.exit(2);
        }
    }

    /**
     * 
     * @param h
     * @param isMetaData
     * @return
     */
    private static int getCopiesOfChunk(BigHash h, boolean isMetaData) {

        int copies = 0;

        for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {

            if (!row.isReadable() || !row.isOnline() || !row.isCore()) {
                continue;
            }
            
            final String host = row.getHost();
            
            ATTEMPTS:
            for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
                TrancheServer ts = null;
                boolean wasAlreadyConnected = true;
                try {
                    ts = ConnectionUtil.getHost(host);
                    
                    if (ts == null){
                        wasAlreadyConnected = false;
                        ts = ConnectionUtil.connectHost(host, true);
                    } else {
                        ConnectionUtil.lockConnection(host);
                    }
                    
                    final BigHash[] hashArr = { h };
                    boolean[] hasArr = null;
                    if (isMetaData) {
                         hasArr = ts.hasMetaData(hashArr);
                    } else {
                        hasArr = ts.hasData(hashArr);
                    }
                    
                    if (hasArr[0]) {
                        copies++;
                    }
                    
                    break ATTEMPTS;
                } catch (Exception e) {
                    // Nope
                } finally {
                    ConnectionUtil.unlockConnection(host);
                    if (!wasAlreadyConnected) {
                        ConnectionUtil.safeClose(row);
                    }
                }
            }

        }

        return copies;
    }

    /**
     * 
     * @param out
     */
    public static void printUsage(PrintStream out) {
        out.println();
        out.println("USAGE");
        out.println("   VerifyDataSetChunkCopiesOnNetworkScript -h <hash> [-p <passphrase>]");
        out.println();
        out.println("DESCRIPTION");
        out.println("   Prints number of copies of all chunks for project on network in CSV form:");
        out.println("   \"Chunk type (meta or data)\",\"hash\",\"copies\"");
        out.println("   The CSV goes to standard output; all other output goes to standard error.");
        out.println();
        out.println("PARAMETERS");
        out.println("   -h      Value: a hash           The hash for the data or meta data chunk");
        out.println("   -p      Value: passphrase       Optional. Only if data set is encrypted.");
        out.println();
        out.println("USAGE");
        out.println("   0: Exit normally");
        out.println("   1: Missing parameters/wrong number of parameters");
        out.println("   2: Unknown error (see standard error)");
        out.println("   3: Unknown parameter");
        out.println();
    }
}
