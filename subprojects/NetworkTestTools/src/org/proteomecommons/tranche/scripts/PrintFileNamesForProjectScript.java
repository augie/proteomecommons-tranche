/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.scripts;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.tranche.commons.TextUtil;
import org.tranche.get.GetFileTool;
import org.tranche.hash.BigHash;
import org.tranche.network.NetworkUtil;
import org.tranche.project.ProjectFile;
import org.tranche.project.ProjectFilePart;
import org.tranche.util.IOUtil;

/**
 *
 * @author bryan
 */
public class PrintFileNamesForProjectScript implements TrancheScript {

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        try {

            BigHash dataSetHash = null;
            List<BigHash> fileHashes = new LinkedList();
            String passphrase = null;

            for (int i = 0; i < args.length; i += 2) {
                final String name = args[i], value = args[i + 1];

                try {
                    if (name.equals("-d") || name.equals("--dataset")) {
                        dataSetHash = BigHash.createHashFromString(value);
                    } else if (name.equals("-p") || name.equals("--passphrase")) {
                        passphrase = value;
                    } else if (name.equals("-f") || name.equals("--file")) {
                        fileHashes.add(BigHash.createHashFromString(value));
                    } else {
                        throw new RuntimeException("Unrecognized parameter: " + name);
                    }
                } catch (Exception e) {
                    System.err.println(e.getClass().getSimpleName() + " occured while parsing parameters: " + e.getMessage());
                    e.printStackTrace(System.err);
                    printUsage();
                    System.exit(3);
                }
            }

            // Checking missing parameters (exit code 2)
            if (dataSetHash == null) {
                System.err.println("Data set hash is missing.");
                printUsage();
                System.exit(2);
            }

            if (fileHashes.size() == 0) {
                System.err.println("At least one file hash required, but none were provided.");
                printUsage();
                System.exit(2);
            }

            ProteomeCommonsTrancheConfig.load();
            NetworkUtil.waitForStartup();

            GetFileTool gft = new GetFileTool();

            if (passphrase != null) {
                gft.setPassphrase(passphrase);
            }

            gft.setHash(dataSetHash);

            Map<BigHash, String> fileNamesHash = new HashMap();

            ProjectFile pf = null;

            try {

                pf = gft.getProjectFile();

                System.out.println();
                System.out.println("Name: "+pf.getName());
                System.out.println("Description: "+pf.getDescription());
                System.out.println("Files: "+pf.getParts().size());
                System.out.println("Size: "+TextUtil.formatBytes(pf.getSize().longValue()));

                for (ProjectFilePart pfp : pf.getParts()) {
                    if (fileHashes.contains(pfp.getHash())) {
                        fileNamesHash.put(pfp.getHash(), pfp.getRelativeName());
                    }
                }

                for (BigHash fileHash : fileHashes) {
                    String relativeName = fileNamesHash.get(fileHash);

                    if (relativeName == null) {
                        relativeName = "Not found";
                    }

                    System.out.println("    * "+fileHash+": "+relativeName);
                }
                System.out.println();

                System.exit(0);

            } finally {
                IOUtil.safeClose(pf);
            }

        } catch (Exception e) {
            System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
            printUsage();
            System.exit(1);
        } finally {
        }
    }

    /**
     *
     */
    private static void printUsage() {
        System.out.println();
        System.out.println("USAGE");
        System.out.println("    PrintFileNamesForProjectScript -d <dataset-hash> [-p <dataset-passphrase>] -f <file-hash> [-f <file-hash> ...]");
        System.out.println();
        System.out.println("DESCRIPTION");
        System.out.println("    Prints information about one or more files from dataset.");
        System.out.println();
        System.out.println("PARAMETERS");
        System.out.println("    -d, --dataset           hash            The hash for the project. Required.");
        System.out.println("    -p, --passphrase        String          Passphrase for dataset if encrypted.");
        System.out.println("    -f, --file              hash            The has for file to check. At least one required.");
        System.out.println();
        System.out.println("RETURN CODES");
        System.out.println("    0: Exit normally");
        System.out.println("    1: Unknown error (see standard error)");
        System.out.println("    2: Missing parameters");
        System.out.println("    3: Problem with parameter");
        System.out.println();
    }
}
