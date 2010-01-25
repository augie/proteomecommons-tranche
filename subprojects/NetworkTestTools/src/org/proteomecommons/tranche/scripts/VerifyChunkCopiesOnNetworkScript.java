/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.scripts;

import java.io.ByteArrayInputStream;
import java.io.PrintStream;
import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.tranche.TrancheServer;
import org.tranche.hash.*;
import org.tranche.meta.*;
import org.tranche.network.ConnectionUtil;
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTableRow;
import org.tranche.server.PropagationExceptionWrapper;
import org.tranche.server.PropagationReturnWrapper;
import org.tranche.util.IOUtil;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class VerifyChunkCopiesOnNetworkScript implements TrancheScript {

    private static boolean isReturn = false;

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {
        try {
            
            ProteomeCommonsTrancheConfig.load();
            NetworkUtil.waitForStartup();
            
            String hashStr = null, typeStr = null;

            for (int i = 0; i < args.length; i += 2) {
                String flag = args[i];
                String val = args[i + 1];

                if (flag.equals("-h")) {
                    hashStr = val;
                } else if (flag.equals("-t")) {
                    typeStr = val;
                } else if (flag.equals("-r")) {
                    isReturn = Boolean.parseBoolean(val);
                } else {
                    System.err.println("Unknown parameter: " + flag);
                    printUsage(System.err);
                    if (!isReturn) {
                        System.exit(3);
                    } else {
                        System.err.println("Return code: 3");
                        return;
                    }
                }
            }

            if (hashStr == null) {
                System.err.println("Missing parameter: hash string, -h");
                printUsage(System.err);
                if (!isReturn) {
                    System.exit(2);
                } else {
                    System.err.println("Return code: 2");
                    return;
                }
            }

            if (typeStr == null) {
                System.err.println("Missing parameter: type string, -t (\"meta\" or \"data\")");
                printUsage(System.err);
                if (!isReturn) {
                    System.exit(2);
                } else {
                    System.err.println("Return code: 2");
                    return;
                }
            }

            if (!typeStr.equals("meta") && !typeStr.equals("data")) {
                System.err.println("Invalid valid value for type string. Expected \"meta\" or \"data\", but found: " + typeStr);
                printUsage(System.err);
                if (!isReturn) {
                    System.exit(3);
                } else {
                    System.err.println("Return code: 3");
                    return;
                }
            }

            BigHash hash = BigHash.createHashFromString(hashStr);

            if (typeStr.equals("meta")) {
                checkChunk(hash, true);
            } else if (typeStr.equals("data")) {
                checkChunk(hash, false);
            } else {
                throw new RuntimeException("ASSERTION FAILED: Expected value of \"meta\" or \"data\", but found \"" + typeStr + "\". Should have checked already, programmer error.");
            }

        } catch (Exception e) {
            System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
            printUsage(System.err);
            if (!isReturn) {
                System.exit(2);
            } else {
                System.err.println("Return code: 2");
                return;
            }
        }

        if (!isReturn) {
            System.exit(0);
        } else {
            System.out.println("Return code: 0");
            return;
        }
    }

    /**
     * 
     * @param hash
     * @param isMetaData
     */
    private static void checkChunk(BigHash hash, boolean isMetaData) {
        SERVERS:
        for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {
            
            // Can't do anything if server isn't readable
            if (!row.isReadable()) {
                continue SERVERS;
            }
            
            ATTEMPT:
            for (int attempt = 0; attempt < 3; attempt++) {
                try {
                    checkChunk(hash, isMetaData, row.getHost());
                    break ATTEMPT;
                } catch (Exception e) {
                    System.err.println(e.getClass().getSimpleName() + " for "+row.getHost()+": " + e.getMessage());
                    e.printStackTrace(System.err);
                }
            } // attempts
        }
    }

    /**
     * 
     * @param hash
     * @param isMetaData
     * @param host
     * @throws java.lang.Exception
     */
    private static void checkChunk(final BigHash hash, final boolean isMetaData, final String host) throws Exception {
//        System.out.println("DEBUG> hash:"+hash+" isMetaData:"+isMetaData+" host:"+host);
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

            thread.join(15 * 1000);

            if (thread.isAlive()) {
                thread.interrupt();
            }

            TrancheServer ts = tsArr[0];

            if (ts == null) {
//                System.err.println(host + " is not online. Skipping.");
                return;
            }

            final BigHash[] hashArr = {hash};

            if (isMetaData) {
                if (ts.hasMetaData(hashArr)[0]) {
                    PropagationReturnWrapper prw = ts.getMetaData(hashArr, false);
                    
                    if (prw.isVoid()) {
                        if (prw.isAnyErrors()) {
                            for (PropagationExceptionWrapper pew : prw.getErrors()) {
                                System.err.println("    * "+pew.exception.getClass()+" on "+pew.host+": "+pew.exception.getLocalizedMessage());
                            }
                            throw new NullPointerException("Return void. See above messages.");
                        } else {
                            throw new NullPointerException("Did not return any errors, but returned void!?!");
                        }
                    }
                    
                    byte[] bytes = ((byte[][]) prw.getReturnValueObject())[0];
                    boolean verifies = false;

                    ByteArrayInputStream bais = null;
                    MetaData md = null;
                    try {
                        bais = new ByteArrayInputStream(bytes);
                        md = MetaDataUtil.read(bais);
                        verifies = true;
                    } catch (Exception e) {
                    } finally {
                        IOUtil.safeClose(bais);
                    }

                    System.out.println("    Server<" + host + "> has meta data chunk<" + hash + ">, verifies?: " + verifies);
                }
            } else {
                if (ts.hasData(hashArr)[0]) {
                    PropagationReturnWrapper prw = ts.getData(hashArr, false);
                    
                    if (prw.isVoid()) {
                        if (prw.isAnyErrors()) {
                            for (PropagationExceptionWrapper pew : prw.getErrors()) {
                                System.err.println("    * "+pew.exception.getClass()+" on "+pew.host+": "+pew.exception.getLocalizedMessage());
                            }
                            throw new NullPointerException("Return void. See above messages.");
                        } else {
                            throw new NullPointerException("Did not return any errors, but returned void!?!");
                        }
                    }
                    
                    byte[] bytes = ((byte[][]) prw.getReturnValueObject())[0];
                    boolean verifies = false;

                    BigHash verifyHash = new BigHash(bytes);
                    if (hash.equals(verifyHash)) {
                        verifies = true;
                    }
                    System.out.println("    Server<" + host + "> has data chunk<" + hash + ">, verifies?: " + verifies);
                }
            }
        } finally {
            if (!isConnected) {
                ConnectionUtil.unlockConnection(host);
                ConnectionUtil.safeCloseHost(host);
            }
        }
    }

    /**
     * 
     * @param out
     */
    public static void printUsage(PrintStream out) {
        out.println();
        out.println("USAGE");
        out.println("   VerifyChunkCopiesOnNetworkScript [OPTIONS] -h <hash> -t <type>");
        out.println();
        out.println("DESCRIPTION");
        out.println("   Finds all copies of chunk on network, and prints out information.");
        out.println();
        out.println("PARAMETERS");
        out.println("   -h      Value: a hash           The hash for the data or meta data chunk");
        out.println("   -t      Value: meta or data     \"meta\" if meta data chunk, \"data\" if data chunk");
        out.println();
        out.println("OPTIONAL PARAMETERS");
        out.println("   -r      Value: true or false    If \"true\", returns instead of exiting. Appropriate when being called from within a Java process.");
        out.println();
        out.println("USAGE");
        out.println("   0: Exit normally");
        out.println("   1: Missing parameters/wrong number of parameters");
        out.println("   2: Unknown error (see standard error)");
        out.println("   3: Problem with parameters");
        out.println();
    }
}
