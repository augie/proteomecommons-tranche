/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.scripts;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.proteomecommons.tranche.scripts.status.NetworkInformation;
import org.proteomecommons.tranche.scripts.status.StatusUtil;
import org.proteomecommons.tranche.scripts.status.TrancheStatusTableRow;
import org.tranche.ConfigureTranche;
import org.tranche.time.TimeUtil;
import org.tranche.util.EmailUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.Text;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class EmailNetworkChangesScript {

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {

        try {
            ProteomeCommonsTrancheConfig.load();

            Set<String> emailAddresses = new HashSet();

            for (String adminEmail : ConfigureTranche.getAdminEmailAccounts()) {
                emailAddresses.add(adminEmail);
            }

            File inputFile = null;
            String optionalMessage = null;

            for (int i = 0; i < args.length; i += 2) {
                String name = args[i];
                String value = args[i + 1];

                if (name.equals("-f") || name.equals("--file")) {
                    inputFile = new File(value);
                } else if (name.equals("-e") || name.equals("--email")) {
                    emailAddresses.add(value);
                } else if (name.equals("-m") || name.equals("--message")) {
                    optionalMessage = value;
                } else {
                    throw new Exception("Unrecognized flag: " + name);
                }
            }

            if (inputFile == null) {
                throw new Exception("Missing required parameter for input file.");
            }

            if (!inputFile.exists()) {
                inputFile.createNewFile();
            }

            // Step 1. Read in servers
            Set<String> lastOnlineServerHosts = new HashSet();

            BufferedReader in = null;
            try {
                in = new BufferedReader(new FileReader(inputFile));

                String host;
                while ((host = in.readLine()) != null) {
                    lastOnlineServerHosts.add(host);
                }
            } finally {
                IOUtil.safeClose(in);
            }

            // Step 2. Get online servers
            NetworkInformation networkInfo = StatusUtil.getTrancheStatusTable();

            Set<String> onlineServerHosts = new HashSet();
            for (TrancheStatusTableRow row : networkInfo.getStatusTable().getRows()) {
                onlineServerHosts.add(row.host);
            }

            // Step 3: Determine whether any changes
            List<String> hostsWentOffline = new LinkedList();
            List<String> hostsNewlyOnline = new LinkedList();

            for (String host : lastOnlineServerHosts) {
                if (!onlineServerHosts.contains(host)) {
                    hostsWentOffline.add(host);
                }
            }

            for (String host : onlineServerHosts) {
                if (!lastOnlineServerHosts.contains(host)) {
                    hostsNewlyOnline.add(host);
                }
            }

            if (hostsWentOffline.size() > 0 || hostsNewlyOnline.size() > 0) {

                System.out.println("There were changes to network servers status, sending email notification.");
                System.out.println();

                StringBuffer message = new StringBuffer();

                if (optionalMessage != null) {
                    message.append(optionalMessage);
                    message.append(Text.getNewLine());
                    message.append(Text.getNewLine());
                }

                if (hostsWentOffline.size() > 0) {
                    message.append("Disappeared: " + Text.getCommaSeparatedString(hostsWentOffline));
                    message.append(Text.getNewLine());
                    message.append(Text.getNewLine());
                }
                if (hostsNewlyOnline.size() > 0) {
                    message.append("Appeared: " + Text.getCommaSeparatedString(hostsNewlyOnline));
                    message.append(Text.getNewLine());
                    message.append(Text.getNewLine());
                }

                StringBuffer subject = new StringBuffer();
                subject.append(Text.getFormattedDateSimple(System.currentTimeMillis())+": PC.org-T server(s) ");

                if (hostsWentOffline.size() > 0 && hostsNewlyOnline.size() > 0) {
                    subject.append("changes");
                } else if (hostsWentOffline.size() > 0) {
                    subject.append("went offline");
                } else {
                    subject.append("came online");
                }

                System.out.println("Subject: " + Text.getNewLine() + subject);
                System.out.println("Message: " + Text.getNewLine() + message);

                EmailUtil.sendEmail(subject.toString(), emailAddresses.toArray(new String[0]), message.toString());

                // Clobber the old file with current online servers
                BufferedWriter out = null;
                try {
                    out = new BufferedWriter(new FileWriter(inputFile, false));
                    for (String host : onlineServerHosts) {
                        out.write(host);
                        out.newLine();
                    }
                } finally {
                    IOUtil.safeClose(out);
                }
            } else {
                System.out.println("There were no changes to network server status.");
            }
        } catch (Exception e) {
            System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
            printUsage();
            System.exit(1);
        }
    }

    /**
     * 
     */
    private static void printUsage() {
        System.out.println();
        System.out.println("DESCRIPTION");
        System.out.println("Looks for any changes to core servers network status and sends out email to admin if change");
        System.out.println();
        System.out.println("REQUIRED ARGUMENTS");
        System.out.println("    -f, --file      Path to file to read in last run's status and overwrite with this run's status");
        System.out.println();
        System.out.println("OPTIONAL ARGUMENTS");
        System.out.println("    -e, --email     Additional email addresses to which to send message");
        System.out.println("    -m, --message   Additional message to include");
        System.out.println();
    }
}
