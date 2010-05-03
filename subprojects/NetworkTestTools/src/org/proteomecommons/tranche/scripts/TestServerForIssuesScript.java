/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.scripts;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.tranche.TrancheServer;
import org.tranche.commons.RandomUtil;
import org.tranche.commons.TextUtil;
import org.tranche.hash.BigHash;
import org.tranche.network.*;
import org.tranche.server.PropagationExceptionWrapper;
import org.tranche.server.PropagationReturnWrapper;
import org.tranche.users.UserZipFile;
import org.tranche.users.UserZipFileUtil;
import org.tranche.util.IOUtil;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class TestServerForIssuesScript {

    /**
     * 
     * @param args
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println("Expecting four arguments: <server host> <server port> <username> <passphrase>");
        }

        final String host = args[0];
        final int port = Integer.parseInt(args[1]);
        final String username = args[2];
        final String passphrase = args[3];

        ProteomeCommonsTrancheConfig.load();
        NetworkUtil.waitForStartup();

        TrancheServer ts = null;
        try {

            for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {
                if (row.getHost().equals(host)) {
                    System.out.println("Found host:");
                    System.out.println("    - Online: " + row.isOnline());
                    System.out.println("    - Port: " + row.getPort());
                    break;
                }
            }

            ts = ConnectionUtil.connect(host, port, false, true);

            if (ts == null) {
                throw new NullPointerException("Failed to connect to host: " + host);
            }

            testChunks(ts, true);
            testChunks(ts, false);

            // Write a few data chunks to make sure no problems
            final int chunksToWrite = 10;

            UserZipFile uzf = UserZipFileUtil.getUserZipFile(username, passphrase);

            System.out.println("Going to write out " + chunksToWrite + " test data chunks.");

            for (int i = 0; i < chunksToWrite; i++) {

                final byte[] bytes = new byte[RandomUtil.getInt(1024 * 1024) + 1];
                RandomUtil.getBytes(bytes);
                final BigHash hash = new BigHash(bytes);
                final long start = System.currentTimeMillis();
                PropagationReturnWrapper prw = IOUtil.setData(ts, uzf.getCertificate(), uzf.getPrivateKey(), hash, bytes);
                long total = System.currentTimeMillis() - start;

                if (prw.isAnyErrors()) {
                    System.err.println("    Failed to upload chunk #" + (i + 1) + " of " + chunksToWrite+", "+TextUtil.formatBytes(bytes.length)+". Took: "+TextUtil.getEllapsedTimeString(total));

                    for (PropagationExceptionWrapper pew : prw.getErrors()) {
                        System.err.println("        - " + pew.exception.getClass().getSimpleName() + ": " + pew.exception.getMessage());
                    }

                } else {
                    System.out.println("    Wrote chunk #" + (i + 1) + " of " + chunksToWrite + ", " + TextUtil.formatBytes(bytes.length) + ". Took: " + TextUtil.getEllapsedTimeString(total));
                }


            }

        } finally {
            ConnectionUtil.unlockConnection(host);
            IOUtil.safeClose(ts);
        }
    }

    /**
     * 
     * @param isMetaData
     * @throws java.lang.Exception
     */
    private static void testChunks(TrancheServer ts, boolean isMetaData) throws Exception {
        final int maxHashesToSave = 10;
        Set<BigHash> hashesToSave = new HashSet();

        int hashCount = 0;
        BigInteger offset = BigInteger.ZERO;
        BigInteger length = BigInteger.valueOf(100);

        BigHash[] hashes = null;

        if (isMetaData) {
            hashes = ts.getMetaDataHashes(offset, length);
        } else {
            hashes = ts.getDataHashes(offset, length);
        }

        hashCount += hashes.length;

        // Iterate through all hashes
        while (hashes.length == length.intValue()) {

            // If haven't stored maximum, add one randomly
            if (hashesToSave.size() < maxHashesToSave) {
                int randomIndex = RandomUtil.getInt(hashes.length);
                hashesToSave.add(hashes[randomIndex]);
            }

            offset = offset.add(length);

            if (isMetaData) {
                hashes = ts.getMetaDataHashes(offset, length);
            } else {
                hashes = ts.getDataHashes(offset, length);
            }

            hashCount += hashes.length;
        }

        System.out.println("Found a total of " + hashCount + " " + (isMetaData ? "meta data" : "data") + " hashes. Going to try to download " + hashesToSave.size() + "...");

        int count = 0;
        for (BigHash hash : hashesToSave) {
            final BigHash[] hashArr = {hash};
            byte[] chunk = null;
            PropagationReturnWrapper prw = null;

            if (isMetaData) {
                prw = ts.getMetaData(hashArr, false);
            } else {
                prw = ts.getData(hashArr, false);
            }

            count++;

            if (prw.isAnyErrors()) {
                System.err.println("    Failed to download chunk #" + count + " of " + hashesToSave.size() + ":");

                for (PropagationExceptionWrapper pew : prw.getErrors()) {
                    System.err.println("        - " + pew.exception.getClass().getSimpleName() + ": " + pew.exception.getMessage());
                }

            } else {
                chunk = ((byte[][]) prw.getReturnValueObject())[0];
                System.out.println("    Downloaded chunk #" + count + " of " + hashesToSave.size() + ", " + TextUtil.formatBytes(chunk.length));
            }
        }
    }
}
