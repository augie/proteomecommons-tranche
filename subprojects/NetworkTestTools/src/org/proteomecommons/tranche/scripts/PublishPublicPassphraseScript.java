/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.scripts;

import java.io.File;
import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.tranche.tasks.TaskUtil;
import org.tranche.hash.BigHash;
import org.tranche.network.*;
import org.tranche.users.UserZipFile;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class PublishPublicPassphraseScript {

    public static void main(String[] args) throws Exception {

        ProteomeCommonsTrancheConfig.load();
        NetworkUtil.waitForStartup();

        try {
            
            if (args.length != 6) {
                throw new RuntimeException("Expected 6 arguments, instead found: "+args.length);
            }
            
            int serverCount = 0;

            for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {
                if (row.isCore() && row.isOnline()) {
                    serverCount++;
                }
            }

            System.out.println("Total online, core servers: " + serverCount);

            final BigHash hash = BigHash.createHashFromString(args[0]);
            final String newPublicPassphrase = args[1];
            final long uploadTimestamp = Long.parseLong(args[2]);
            final String uploaderName = args[3];
            final String userPath = args[4];
            final String userPassphrase = args[5];
            
            final String relativePathInDataSet = null;
            
            UserZipFile uzf = null;
            
            uzf = new UserZipFile(new File(userPath));
            uzf.setPassphrase(userPassphrase);
            uzf.getPrivateKey();

            TaskUtil.publishPassphrase(hash, newPublicPassphrase, uploaderName, uzf.getCertificate(), uzf.getPrivateKey(), uploadTimestamp, relativePathInDataSet, System.out);
        } catch (Exception e) {
            System.err.println(e.getClass().getSimpleName()+": "+e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
            printUsage();
        } finally {
        }
    }
    
    private static void printUsage() {
        System.err.println();
        System.err.println("USAGE: PublishPublicPassphraseScript <data set hash> <data set passphrase> <upload timestamp> <uploader name> <uzf path> <uzf passphrase>");
        System.err.println();
    }
}
