/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.proteomecommons.tranche.scripts;

import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.tranche.FileEncoding;
import org.tranche.get.GetFileTool;
import org.tranche.hash.BigHash;
import org.tranche.meta.MetaData;
import org.tranche.network.NetworkUtil;

/**
 *
 * @author Tranche
 */
public class PrintPublicPassphraseForProjectScript implements TrancheScript {
    public static void main(String[] args) throws Exception {
        ProteomeCommonsTrancheConfig.load();
        
        if (args.length != 1) {
            System.err.println("Expecting 1 parameter (project hash), instead found: "+args.length);
            System.exit(1);
        }
        
        BigHash hash = BigHash.createHashFromString(args[0]);
        System.out.println("Looking for public passphrase for data set: "+hash);
        
        NetworkUtil.waitForStartup();
        
        GetFileTool gft = new GetFileTool();
        gft.setHash(hash);
        
        MetaData md = gft.getMetaData();
        
        if (md.isPublicPassphraseSet()) {
            System.out.println("Public passphrase: "+md.getPublicPassphrase());
        } else {
            System.out.println("No public passphrase.");
        }
        
        System.out.println("File encodings for meta data:");
        for (FileEncoding fe : md.getEncodings()) {
            System.out.println(" * "+fe.getName()+": "+fe.getHash());
        }
        
        System.exit(0);
    }
}
