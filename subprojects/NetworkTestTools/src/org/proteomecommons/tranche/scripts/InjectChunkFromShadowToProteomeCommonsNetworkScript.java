/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.scripts;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.tranche.TrancheServer;
import org.tranche.hash.BigHash;
import org.tranche.hash.span.HashSpan;
import org.tranche.network.ConnectionUtil;
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTableRow;
import org.tranche.server.PropagationExceptionWrapper;
import org.tranche.server.PropagationReturnWrapper;
import org.tranche.users.UserZipFile;
import org.tranche.util.IOUtil;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class InjectChunkFromShadowToProteomeCommonsNetworkScript implements TrancheScript {

    public static void main(String[] args) {

        try {

            if (args.length != 4) {
                throw new Exception("Expecting 4 arguments, but found: " + args.length);
            }

            BigHash chunkHash = BigHash.createHashFromString(args[0]);
            final boolean isMetaData = Boolean.parseBoolean(args[1]);
            final File userFile = new File(args[2]);
            final String userPass = args[3];

            UserZipFile uzf = new UserZipFile(userFile);
            uzf.setPassphrase(userPass);

            // If something wrong, trigger it now
            uzf.getPrivateKey();

            final String shadowURL = "tranche://141.214.65.205:1045";

            try {

                TrancheServer shadowTs = ConnectionUtil.connectURL(shadowURL, true);
                if (shadowTs == null) {
                    throw new Exception("Could not connect to server: " + shadowURL);
                }

                ProteomeCommonsTrancheConfig.load();
                NetworkUtil.waitForStartup();

                PropagationReturnWrapper prw = null;

                final BigHash[] hashArr = {chunkHash};

                if (isMetaData) {
                    prw = shadowTs.getMetaData(hashArr, false);
                } else {
                    prw = shadowTs.getData(hashArr, false);
                }

                byte[] chunk = ((byte[][]) prw.getReturnValueObject())[0];

                final String chunkType = isMetaData ? "meta data" : "data";
                if (prw.isVoid()) {
                    System.err.println("Filed to download " + chunkType + " chunk. Here are the " + prw.getErrors().size() + " error message(s):");
                    for (PropagationExceptionWrapper pew : prw.getErrors()) {
                        System.err.println("    * " + pew.exception.getClass().getSimpleName() + " on " + pew.host + ": " + pew.exception.getMessage());
                    }
                    System.exit(2);
                }

                System.out.println("Downloaded " + chunkType + " chunk.");

                int onlineCoreServerCount = 0;
                Set<String> inHashSpanHosts = new HashSet();
                for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {
                    if (row.isCore() && row.isOnline()) {
                        onlineCoreServerCount++;

                        if (row.isWritable()) {
                            HASH_SPANS:
                            for (HashSpan hs : row.getTargetHashSpans()) {
                                if (hs.contains(chunkHash)) {
                                    inHashSpanHosts.add(row.getHost());
                                    break HASH_SPANS;
                                }
                            }
                        }
                    }
                }

                System.out.println("Total online, core server on ProteomeCommons.org Tranche Network: " + onlineCoreServerCount);

                if (inHashSpanHosts.size() == 0) {
                    System.err.println("No online servers to which to inject this chunk. Were all the servers online?");
                    System.exit(3);
                }

                System.out.println("Total writable servers to receive this chunk: " + inHashSpanHosts.size());

                for (String host : inHashSpanHosts) {
                    boolean wasConnected = true;
                    try {

                        TrancheServer ts = ConnectionUtil.getHost(host);

                        if (ts == null) {
                            ts = ConnectionUtil.connectHost(host, true);
                            wasConnected = false;
                        } else {
                            ConnectionUtil.lockConnection(host);
                        }

                        boolean wasSuccess = false;

                        ATTEMPT:
                        for (int attempt = 0; attempt < 3; attempt++) {
                            try {
                                PropagationReturnWrapper prwSet = null;
                                if (isMetaData) {
                                    prwSet = IOUtil.setMetaData(ts, uzf.getCertificate(), uzf.getPrivateKey(), true, chunkHash, chunk);
                                } else {
                                    prwSet = IOUtil.setData(ts, uzf.getCertificate(), uzf.getPrivateKey(), chunkHash, chunk);
                                }
                                
                                // Just throw any errors
                                if (prwSet.isAnyErrors()) {
                                    for (PropagationExceptionWrapper pew : prwSet.getErrors()) {
                                        throw pew.exception;
                                    }
                                }
                                
                                wasSuccess = true;
                                
                            } catch (Exception e) {
                                System.err.println("Attempt #"+attempt+" for "+host+": "+e.getClass().getSimpleName()+" -- "+e.getMessage());
                            }
                        }

                        if (wasSuccess) {
                            System.out.println("Success! Injected to: " + host);
                        } else {
                            System.err.println("Failed to inject to: " + host);
                        }

                    } catch (Exception e) {
                        System.err.println(e.getClass().getSimpleName()+" on "+host+": "+e.getMessage());
                    } finally {
                        ConnectionUtil.unlockConnection(host);
                        if (!wasConnected && !ConnectionUtil.isLocked(host)) {
                            ConnectionUtil.safeCloseHost(host);
                        }
                    }
                }

            } finally {
            }
        } catch (Exception e) {
            System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
