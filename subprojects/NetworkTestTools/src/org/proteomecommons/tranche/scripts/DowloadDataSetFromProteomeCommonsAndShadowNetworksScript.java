/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.scripts;

import java.io.File;
import java.io.PrintStream;
import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.tranche.get.CommandLineGetFileToolListener;
import org.tranche.get.GetFileTool;
import org.tranche.get.GetFileToolReport;
import org.tranche.hash.BigHash;
import org.tranche.network.NetworkUtil;
import org.tranche.server.PropagationExceptionWrapper;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class DowloadDataSetFromProteomeCommonsAndShadowNetworksScript {

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {

        if (args.length != 2 && args.length != 3) {
            System.err.println("Wrong number of arguments. Expected 2 (or 3 if passphrase), but found: "+args.length);
            printUsage(System.err);
            System.exit(1);
        }
        
        BigHash hash = null;
        try {
            hash = BigHash.createHashFromString(args[0]);
        } catch (Exception e) {
            System.err.println("Could not create hash from input: "+e.getMessage());
            System.err.println("Please make sure that the hash you provided was correct.");
            printUsage(System.err);
            System.exit(1);
        }
        
        try {
            
            File downloadDir = new File(args[1]);
            
            if (!downloadDir.exists() || !downloadDir.isDirectory()) {
                System.err.println("Cannot download to directory: "+downloadDir.getAbsolutePath());
                System.err.println("Please make sure that it exists and that it is a directory.");
                printUsage(System.err);
                System.exit(1);
            }
            
            String passphrase = null;
            
            if (args.length == 3) {
                passphrase = args[2];
            }
            
            ProteomeCommonsTrancheConfig.load();
            NetworkUtil.waitForStartup();

            final String shadowURL = "tranche://141.214.65.205:1045";
            
            GetFileTool gft = new GetFileTool();
            gft.setHash(hash);
            if (passphrase != null) {
                StringBuffer passphraseHidden = new StringBuffer();
                for (int i=0; i<passphrase.length(); i++) {
                    passphraseHidden.append("*");
                }
                
                System.out.println("Using passphrase: "+passphraseHidden);
                gft.setPassphrase(passphrase);
            }
            
            // DEBUG> Put this back!
            gft.addExternalServerURLToUse(shadowURL);
            CommandLineGetFileToolListener clc = new CommandLineGetFileToolListener(gft, System.out);
            gft.addListener(clc);
            gft.setSaveFile(downloadDir);
            
            GetFileToolReport report = gft.getDirectory();

            if (report.isFailed()) {
                System.err.println("Download failed with the following "+report.getFailureExceptions().size()+" error(s):");
                for (PropagationExceptionWrapper pew : report.getFailureExceptions()) {
                    System.err.println("    * "+pew.exception.getClass().getSimpleName()+" on "+pew.host+": "+pew.exception.getMessage());
                }
                System.exit(3);
            } else {
                System.out.println("~ fin ~");
                System.exit(0);
            }
        } catch (Exception e) {
            System.err.println(e.getClass().getSimpleName()+": "+e.getMessage());
            e.printStackTrace(System.err);
            System.exit(2);
        }
    }

    /**
     * 
     * @param out
     */
    private static void printUsage(PrintStream out) {
        out.println();
        out.println("USAGE");
        out.println("   DowloadDataSetFromProteomeCommonsAndShadowNetworksScript <hash> </path/to/download/dir> [<passphrase>]");
        out.println();
        out.println("DESCRIPTION");
        out.println("   Download a data set from ProteomeCommons.org-Tranche and its shadow network.");
        out.println();
        out.println("USAGE");
        out.println("   0: Exit normally");
        out.println("   1: Missing parameters/wrong number of parameters");
        out.println("   2: Unknown error (see standard error)");
        out.println("   3: Download failed (see standard error)");
        out.println();
    }
}
