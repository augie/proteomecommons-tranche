/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.scripts;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.PrintStream;
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
public class DeleteMetaDataChunkScript implements TrancheScript {

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {
        if (args == null || args.length != 3) {
            int count = (args == null ? 0 : args.length);
            System.err.println("Expected 3 parameters, instead found: " + count);
            printUsage(System.err);
            System.exit(1);
        }

        try {

            BigHash hash = null;
            UserZipFile uzf = null;

            try {
                File user = new File(args[0]);
                if (!user.exists()) {
                    throw new Exception("Did not find user file: "+args[0]);
                }
                String passphrase = args[1];

                uzf = new UserZipFile(user);
                uzf.setPassphrase(passphrase);

                hash = BigHash.createHashFromString(args[2]);
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
                        System.out.println("    - Has meta data! Going to delete");

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

                        if (md.getUploaderCount() == 1) {
                            PropagationReturnWrapper rm = IOUtil.deleteMetaData(ts, uzf.getCertificate(), uzf.getPrivateKey(), hash);
                            if (rm.isAnyErrors()) {
                                System.out.println("    - Total of " + rm.getErrors().size() + " error(s) while deleting:");
                                for (PropagationExceptionWrapper pew : rm.getErrors()) {
                                    System.out.println("        * " + pew.exception.getClass() + " on " + pew.host + ": " + pew.exception.getMessage());
                                }
                            }
                            System.out.println("    - Deleted meta data. Does it have any longer?: " + (ts.hasMetaData(hashArr)[0] ? "yes, why!?!" : "nope, successfully deleted"));
                        } else {
                            throw new RuntimeException("Hoping only had one uploader, but has " + md.getUploaderCount() + ". Manually deal with this.");
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

    public static void printUsage(PrintStream out) {
        out.println();
        out.println("USAGE");
        out.println("   DeleteMetaDataChunkScript <path to user file> <user file passphrase> <hash>");
        out.println();
        out.println("DESCRIPTION");
        out.println("   Deletes a meta data chunk if found.");
        out.println();
        out.println("USAGE");
        out.println("   0: Exit normally");
        out.println("   1: Missing parameters/wrong number of parameters");
        out.println("   2: Unknown error (see standard error)");
        out.println("   3: Problem with parameters");
        out.println();
    }
}
