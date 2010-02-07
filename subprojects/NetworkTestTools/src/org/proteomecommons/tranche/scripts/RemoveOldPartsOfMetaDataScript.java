/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.scripts;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.tranche.TrancheServer;
import org.tranche.hash.*;
import org.tranche.meta.*;
import org.tranche.network.*;
import org.tranche.server.PropagationExceptionWrapper;
import org.tranche.server.PropagationReturnWrapper;
import org.tranche.users.*;
import org.tranche.util.*;

/**
 * <p>Script to delete meta data chunk from network. Ignores what servers think is online and attempts to connect to everything.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class RemoveOldPartsOfMetaDataScript implements TrancheScript {

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {
        if (args == null || args.length != 4) {
            int count = (args == null ? 0 : args.length);
            System.err.println("Expected 4 parameters, instead found: " + count);
            printUsage(System.err);
            System.exit(1);
        }

        try {

            BigHash hash = null;
            UserZipFile uzf = null;
            long removeBeforeTimestamp = -1;

            try {
                File user = new File(args[0]);
                if (!user.exists()) {
                    throw new Exception("Did not find user file: " + args[0]);
                }
                String passphrase = args[1];

                uzf = new UserZipFile(user);
                uzf.setPassphrase(passphrase);

                hash = BigHash.createHashFromString(args[2]);
                removeBeforeTimestamp = Long.parseLong(args[3]);
            } catch (Exception e) {
                System.err.println(e.getClass().getSimpleName() + " while reading in parameters: " + e.getMessage());
                e.printStackTrace(System.err);
                printUsage(System.err);
                System.exit(3);
            }

            // Stuff
            ProteomeCommonsTrancheConfig.load();
            NetworkUtil.waitForStartup();

            // Since must be thorough, ignore status table and try to connect to everything!
            for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {
                final String host = row.getHost();
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
                    thread.setPriority(Thread.MIN_PRIORITY);
                    thread.setDaemon(true);
                    thread.start();

                    thread.join(10 * 1000);

                    if (thread.isAlive()) {
                        thread.interrupt();
                    }

                    TrancheServer ts = tsArr[0];

                    if (ts == null) {
                        System.err.println(host + " is not online. Skipping.");
                        continue;
                    }

                    System.out.println("Checking: " + host);

                    final BigHash[] hashArr = {hash};

                    if (ts.hasMetaData(hashArr)[0]) {
                        System.out.println("    - Has meta data! Checking whether anything to delete...");

                        // First, get meta data
                        PropagationReturnWrapper prw = ts.getMetaData(hashArr, false);

                        byte[] metaDataBytes = ((byte[][]) prw.getReturnValueObject())[0];
                        MetaData md = null;

                        ByteArrayInputStream bais = null;
                        try {
                            bais = new ByteArrayInputStream(metaDataBytes);
                            md = MetaDataUtil.read(bais);
                        } finally {
                            IOUtil.safeClose(bais);
                        }
                    
                        if (md.getUploaderCount() == 1 && md.getTimestampUploaded() < removeBeforeTimestamp) {
                            PropagationReturnWrapper rm = IOUtil.deleteMetaData(ts, uzf.getCertificate(), uzf.getPrivateKey(), hash);
                            if (rm.isAnyErrors()) {
                                System.out.println("    - Total of " + rm.getErrors().size() + " error(s) while deleting:");
                                for (PropagationExceptionWrapper pew : rm.getErrors()) {
                                    System.out.println("        * " + pew.exception.getClass() + " on " + pew.host + ": " + pew.exception.getMessage());
                                }
                            }
                            System.out.println("    - Deleted meta data. Does it have any longer?: " + (ts.hasMetaData(hashArr)[0] ? "yes, why!?!" : "nope, successfully deleted"));
                        } else {
                            
                            Set<TripleToRemove> removeSet = new HashSet();
                            for (int i = 0; i < md.getUploaderCount(); i++) {
                                md.selectUploader(i);
                                if (md.getTimestampUploaded() < removeBeforeTimestamp) {
                                    removeSet.add(new TripleToRemove(md.getSignature().getUserName(), md.getTimestampUploaded(), md.getRelativePathInDataSet()));
                                }
                            }

                            for (TripleToRemove remove : removeSet) {
                                md.removeUploader(remove.uploaderName, remove.uploadTimestamp, remove.pathInDataSet);
                            }

                            if (md.getUploaderCount() != 1) {
                                throw new Exception("Expected one uploader, instead found: " + md.getUploaderCount());
                            }

                            PropagationReturnWrapper rm = IOUtil.deleteMetaData(ts, uzf.getCertificate(), uzf.getPrivateKey(), hash);

                            if (rm.isAnyErrors()) {
                                System.out.println("    - Total of " + rm.getErrors().size() + " error(s) while deleting:");
                                for (PropagationExceptionWrapper pew : rm.getErrors()) {
                                    System.out.println("        * " + pew.exception.getClass() + " on " + pew.host + ": " + pew.exception.getMessage());
                                }
                            }
                            
                            if (ts.hasMetaData(hashArr)[0]) {
                                throw new Exception("Should have deleted meta data from " + host + ", but still has!");
                            }

                            IOUtil.setMetaData(ts, uzf.getCertificate(), uzf.getPrivateKey(), true, hash, md.toByteArray());

                            System.out.println("    - Replaced meta data on "+host);
                        }

                    } else {
                        System.out.println("    - Does not have meta data");
                    }

                } finally {
                    if (!isConnected) {
                        ConnectionUtil.unlockConnection(host);
                        ConnectionUtil.safeCloseHost(host);
                    }
                }

            }
        } catch (Exception e) {
            System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
            printUsage(System.err);
            System.exit(2);
        }

        System.exit(0);
    }

    /**
     * 
     */
    static class TripleToRemove {

        final String uploaderName;
        final long uploadTimestamp;
        final String pathInDataSet;

        TripleToRemove(String uploaderName, long uploadTimestamp, String pathInDataSet) {
            this.uploaderName = uploaderName;
            this.uploadTimestamp = uploadTimestamp;
            this.pathInDataSet = pathInDataSet;
        }
    }

    public static void printUsage(PrintStream out) {
        out.println();
        out.println("USAGE");
        out.println("   RemoveOldPartsOfMetaDataScript <path to user file> <user file passphrase> <hash> <timestamp-before-which-to-remove>");
        out.println();
        out.println("DESCRIPTION");
        out.println("   Removes all portions of meta data that were uploaded before a certain timestamp.");
        out.println();
        out.println("USAGE");
        out.println("   0: Exit normally");
        out.println("   1: Missing parameters/wrong number of parameters");
        out.println("   2: Unknown error (see standard error)");
        out.println("   3: Problem with parameters");
        out.println();
    }
}
