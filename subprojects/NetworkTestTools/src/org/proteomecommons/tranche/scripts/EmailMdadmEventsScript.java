/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.scripts;

import java.util.HashSet;
import java.util.Set;
import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.tranche.commons.TextUtil;
import org.tranche.util.EmailUtil;

/**
 *
 * @author bryan
 */
public class EmailMdadmEventsScript {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {


        Set<String> emailAddresses = new HashSet();
        String device = null;
        String event = null;
        String host = null;

        try {
            for (int i = 0; i < args.length; i += 2) {
                final String name = args[i];
                final String val = args[i + 1];

                if (name.equals("-e") || name.equals("--email")) {
                    emailAddresses.add(val);
                } else if (name.equals("-d") || name.equals("--device")) {
                    device = val;
                } else if (name.equals("-v") || name.equals("--event")) {
                    event = val;
                } else if (name.equals("-h") || name.equals("--host")) {
                    host = val;
                } else {
                    System.err.println("Unrecognized parameter: " + name);
                    printUsage();
                    System.exit(1);
                }
            }

            if (emailAddresses.size() == 0) {
                System.err.println("Missing required parameter: email address");
                printUsage();
                System.exit(2);
            }
            if (device == null) {
                System.err.println("Missing required parameter: device");
                printUsage();
                System.exit(2);
            }
            if (event == null) {
                System.err.println("Missing required parameter: event");
                printUsage();
                System.exit(2);
            }
            if (host == null) {
                System.err.println("Missing required parameter: host");
                printUsage();
                System.exit(2);
            }

            final String msg = getMessage(host, event, device);
            final String subject = getSubject(host, event, device);

            ProteomeCommonsTrancheConfig.load();

            EmailUtil.sendEmail(subject, emailAddresses.toArray(new String[0]), msg);
            
            System.out.println("-----------------------------------------------------------------------");
            System.out.println("The following email was sent: ");
            System.out.println("-----------------------------------------------------------------------");
            System.out.println("Recipients:");
            for (String email : emailAddresses) {
                System.out.println("    - "+email);
            }
            System.out.println("Subject: "+subject);
            System.out.println();
            System.out.println(msg);

        } catch (Exception e) {
            System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
            printUsage();
            System.exit(3);
        }

        System.exit(0);
    }

    /**
     *
     * @param host
     * @param event
     * @param device
     * @return
     */
    private static String getSubject(String host, String event, String device) {
        return "mdadm@" + host + ": " + event + " on " + device;
    }

    /**
     *
     * @param host
     * @param event
     * @param device
     * @return
     */
    private static String getMessage(String host, String event, String device) {
        StringBuffer str = new StringBuffer();

        str.append("A mdadm RAID event has occured:"+TextUtil.getNewLine());
        str.append("-----------------------------------------------------------------------");
        str.append(TextUtil.getNewLine() + TextUtil.getNewLine());
        str.append("Host: " + host + TextUtil.getNewLine());
        str.append("Event: " + event + TextUtil.getNewLine());
        str.append("Device: " + device + TextUtil.getNewLine());

        return str.toString();
    }

    /**
     *
     */
    private static void printUsage() {
        System.out.println();
        System.out.println("DESCRIPTION");
        System.out.println("    Send an email to one or more recipients describing a mdadm RAID event");
        System.out.println();
        System.out.println("REQUIRED PARAMETERS");
        System.out.println("    -e, --email     String      Email address for recipient. Multiple allowed, but at least one is required.");
        System.out.println("    -d, --device    String      Device effected by event");
        System.out.println("    -v, --event     String      The event that occured");
        System.out.println("    -h, --host      String      The host on which event occured. Can be an arbitrary name (e.g., Wolverine, 146.9.4.104, etc.)");
        System.out.println();
        System.out.println("ERROR CODES");
        System.out.println("    0: Exited normally");
        System.out.println("    1: Problem with parameters");
        System.out.println("    2: Missing required parameters");
        System.out.println("    3: Unknown (see standard error)");
        System.out.println();
    }
}
