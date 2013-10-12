package org.proteomecommons.tranche.scripts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.tranche.TrancheServer;
import org.tranche.get.GetFileTool;
import org.tranche.hash.BigHash;
import org.tranche.meta.MetaData;
import org.tranche.network.ConnectionUtil;
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTable;
import org.tranche.network.StatusTableRow;
import org.tranche.project.ProjectFile;
import org.tranche.project.ProjectFilePart;
import org.tranche.remote.RemoteTrancheServer;
import org.tranche.server.PropagationReturnWrapper;
import org.tranche.users.UserZipFile;
import org.tranche.users.UserZipFileUtil;
import org.tranche.util.EmailUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;

/**
 *
 * @author James "Augie" Hill - augie@umich.edu
 */
public class ReplicationCountScript {

    public static PrintStream ps = System.out;
    // vars for this run
    public static String post = null;
    public static boolean checkPC = false;
    public static boolean randomize = false;
    public static boolean replicate = false;
    public static int replicationsRequired = 3;
    public static String pc_user = null;
    public static String pc_user_pass = null;
    public static String user = null;
    public static String pass = null;
    public static final ArrayList<File> tsvFiles = new ArrayList<File>();
    public static final ArrayList<File> csvFiles = new ArrayList<File>();
    public static boolean continuous = false;
    public static final ArrayList<String> hashesStartWith = new ArrayList<String>();
    public static final ArrayList<BigHash> hashesToCheck = new ArrayList<BigHash>();
    public static File logFile = null;
    public static final Set<String> emails = new HashSet<String>();

    public static void main(String[] args) throws Exception {

        ProteomeCommonsTrancheConfig.load();

        if (args.length == 1 && (args[0].trim().equals("--usage") || args[0].trim().equals("help") || args[0].trim().equals("--help"))) {
            System.out.println("This script takes inventory of data and meta data replications.");
            System.out.println("Items in brackets are optional. Usage:");
            System.out.println("");
            System.out.println("  -c file                      Optional. More than one can be added. Location of a comma separated value file to use as a list of projects to check. Must be in the format: \"hash,passphrase\\n\" for each line.");
            System.out.println("  -C true/false                Optional. Flag whether to continuously loop. Default false.");
            System.out.println("  -e true/false                Optional. Whether to replicate chunks with fewer than -E replications. Default false.");
            System.out.println("  -E integer                   Optional. The number of replications desired. Default 3.");
            System.out.println("  -h string                    Optional. More than one can be added. If set, will only check hashes that start with string.");
            System.out.println("  -H hash                      Optional. Non-encrypted project hash to check.");
            System.out.println("  -l log_file                  Optional. Path to the log file to write to. If already exists, will overwrite. Default prints to System.out.");
            System.out.println("  -n email                     Optional. Send an email to the given address when a data set is found to contain chunks with zero replications.");
            System.out.println("  -o url                       Optional. URL to post reports to.");
            System.out.println("  -p admin_pass                Optional when \"-r false\" else required. Sets the ProteomeCommons.org Tranche repository admin passphrase.");
            System.out.println("  -P pc_user_pass              Optional when \"-e false\" else required.");
            System.out.println("  -r true/false                Optional. Flag whether to check the ProteomeCommons.org Tranche repository. Default false.");
            System.out.println("  -R true/false                Optional. Flag whether to randomize the selection of hashes. Default false.");
            System.out.println("  -t file                      Optional. More than one can be added. Location of a tab separated value file to use as a list of projects to check. Must be in format: \"hash\\tpassphrase\\n\" for each line.");
            System.out.println("  -u admin_user                Optional when \"-r false\" else required. Sets the ProteomeCommons.org Tranche repository admin user.");
            System.out.println("  -U pc_user                   Optional when \"-e false\" else required.");
            return;
        }

        // parse the args
        for (int i = 0; i < args.length - 1; i += 2) {
            if (args[i].trim().equals("-e")) {
                try {
                    replicate = Boolean.valueOf(args[i + 1]);
                } catch (Exception e) {
                    return;
                }
            } else if (args[i].trim().equals("-E")) {
                try {
                    replicationsRequired = Integer.valueOf(args[i + 1]);
                } catch (Exception e) {
                    return;
                }
            } else if (args[i].trim().equals("-U")) {
                try {
                    pc_user = args[i + 1];
                } catch (Exception e) {
                    return;
                }
            } else if (args[i].trim().equals("-P")) {
                try {
                    pc_user_pass = args[i + 1];
                } catch (Exception e) {
                    return;
                }
            } else if (args[i].trim().equals("-o")) {
                try {
                    // test url
                    new File(args[i + 1]).toURL();
                    post = args[i + 1];
                } catch (Exception e) {
                    System.out.println("Bad post URL. Exiting.");
                    return;
                }
            } else if (args[i].trim().equals("-r")) {
                try {
                    checkPC = Boolean.valueOf(args[i + 1]);
                } catch (Exception e) {
                    System.out.println("Bad argument given for -r. Allowed \"true\" or \"false\".");
                    return;
                }
            } else if (args[i].trim().equals("-u")) {
                try {
                    user = args[i + 1];
                } catch (Exception e) {
                    System.out.println("Bad argument given for -u.");
                    return;
                }
            } else if (args[i].trim().equals("-p")) {
                try {
                    pass = args[i + 1];
                } catch (Exception e) {
                    System.out.println("Bad argument given for -p.");
                    return;
                }
            } else if (args[i].trim().equals("-t")) {
                File file = new File(args[i + 1]);
                // reality check
                if (!file.exists()) {
                    System.out.println(args[i + 1] + " does not exist! Exiting.");
                    return;
                }
                if (!file.isFile()) {
                    System.out.println(args[i + 1] + " is not a file! Exiting.");
                    return;
                }
                tsvFiles.add(file);
            } else if (args[i].trim().equals("-c")) {
                File file = new File(args[i + 1]);
                // reality check
                if (!file.exists()) {
                    System.out.println(args[i + 1] + " does not exist! Exiting.");
                    return;
                }
                if (!file.isFile()) {
                    System.out.println(args[i + 1] + " is not a file! Exiting.");
                    return;
                }
                csvFiles.add(file);
            } else if (args[i].trim().equals("-C")) {
                try {
                    continuous = Boolean.valueOf(args[i + 1]);
                } catch (Exception e) {
                    System.out.println("Bad argument given for -C. Allowed \"true\" or \"false\".");
                    return;
                }
            } else if (args[i].trim().equals("-R")) {
                try {
                    randomize = Boolean.valueOf(args[i + 1]);
                } catch (Exception e) {
                    System.out.println("Bad argument given for -R. Allowed \"true\" or \"false\".");
                    return;
                }
            } else if (args[i].trim().equals("-h")) {
                try {
                    hashesStartWith.add(args[i + 1]);
                } catch (Exception e) {
                    System.out.println("Bad argument given for -h.");
                    return;
                }
            } else if (args[i].trim().equals("-l")) {
                logFile = new File(args[i + 1]);
                // reality check
                if (!logFile.exists()) {
                    if (!logFile.createNewFile()) {
                        System.out.println("Could not create log file. Exiting.");
                        return;
                    }
                }
            } else if (args[i].trim().equals("-H")) {
                try {
                    hashesToCheck.add(BigHash.createHashFromString(args[i + 1]));
                } catch (Exception e) {
                    System.out.println("Bad argument given for -hash.");
                    return;
                }
            } else if (args[i].trim().equals("-n")) {
                emails.add(args[i + 1]);
            }
        }

        // reality checks
        if (!checkPC && tsvFiles.size() == 0 && csvFiles.size() == 0 && hashesToCheck.size() == 0) {
            System.out.println("No projects to check. Exiting.");
            return;
        }

        System.out.println("Starting to check the replication counts.");

        FileOutputStream fos = null;
        try {
            // make a printstream to the log file
            if (logFile != null) {
                fos = new FileOutputStream(logFile);
                ps = new PrintStream(fos);
                System.setErr(ps);
            }

            // run at least once
            run(tsvFiles, csvFiles, checkPC, user, pass, post, randomize, hashesStartWith);
            // keep checking over and over again?
            while (continuous) {
                println("");
                println("Restarting");
                run(tsvFiles, csvFiles, checkPC, user, pass, post, randomize, hashesStartWith);
            }
        } catch (Exception e) {
            println("FATAL ERROR");
            e.printStackTrace();
        } finally {
            try {
                ps.flush();
            } catch (Exception e) {
            }
            IOUtil.safeClose(ps);
            IOUtil.safeClose(fos);
            System.exit(1);
        }
    }

    public static void println(String msg) {
        try {
            if (ps != null) {
                synchronized (ps) {
                    ps.println(msg);
                }
            }
        } catch (Exception e) {
        }
    }

    private static void report(String url, BigHash projectHash, long reps0, long reps1, long reps2, long reps3, long reps4, long reps5Plus) throws Exception {
        if (url != null) {
            postReplicationReport(url, projectHash, reps0, reps1, reps2, reps3, reps4, reps5Plus);
        }
    }

    private static void postReplicationReport(String url, BigHash projectHash, long reps0, long reps1, long reps2, long reps3, long reps4, long reps5Plus) throws Exception {
        // make a new client
        HttpClient c = new HttpClient();

        // make a post method
        PostMethod pm = new PostMethod(url);

        List<NameValuePair> pairs = new ArrayList<NameValuePair>();

        // for the register.jsp page
        String servers = "";
        for (StatusTableRow row : ConnectionUtil.getConnectedRows()) {
            if (!servers.equals("")) {
                servers = servers + ", ";
            }
            servers = servers + row.getURL();
        }
        pairs.add(new NameValuePair("hash", projectHash.toString()));
        pairs.add(new NameValuePair("reps0", String.valueOf(reps0)));
        pairs.add(new NameValuePair("reps1", String.valueOf(reps1)));
        pairs.add(new NameValuePair("reps2", String.valueOf(reps2)));
        pairs.add(new NameValuePair("reps3", String.valueOf(reps3)));
        pairs.add(new NameValuePair("reps4", String.valueOf(reps4)));
        pairs.add(new NameValuePair("reps5OrMore", String.valueOf(reps5Plus)));
        pairs.add(new NameValuePair("servers", servers));

        NameValuePair[] pairArray = new NameValuePair[pairs.size()];
        for (int i = 0; i < pairs.size(); i++) {
            pairArray[i] = pairs.get(i);
        }

        // set the values
        pm.setRequestBody(pairArray);

        // execute the method
        int status = c.executeMethod(pm);
        System.out.println("  Replication information posted. HTTP status returned = " + status);

        if (status != 200) {
            throw new Exception("Problems sending in project replication information. (HTTP response=" + status + ")");
        }

        // notify
        if (!emails.isEmpty() && reps0 > 0) {
            String email = "ProteomeCommons.org Tranche Repository\n"
                    + "A data set has been found to be missing chunks:\n\n"
                    + "<a href=\"https://proteomecommons.org/dataset.jsp?i=" + projectHash.toWebSafeString() + "\">ProteomeCommons.org Data Page</a>\n"
                    + "Data Set Hash: " + projectHash + "\n"
                    + "Chunks with 0 replications: " + String.valueOf(reps0) + "\n"
                    + "Chunks with 1 replications: " + String.valueOf(reps1) + "\n"
                    + "Chunks with 2 replications: " + String.valueOf(reps2) + "\n"
                    + "Chunks with 3 replications: " + String.valueOf(reps3) + "\n"
                    + "Chunks with 4 replications: " + String.valueOf(reps4) + "\n"
                    + "Chunks with 5+ replications: " + String.valueOf(reps5Plus) + "\n"
                    + "Connections (" + ConnectionUtil.size() + "):\n";
            for (String server : ConnectionUtil.getConnectedHosts()) {
                email = email + "   " + server + "\n";
            }
            EmailUtil.sendEmail("Missing Data Report", emails.toArray(new String[0]), email);
        }
    }

    private static void run(ArrayList<File> tsvFiles, ArrayList<File> csvFiles, boolean checkPC, String tagsUser, String tagsPass, String post, boolean randomize, ArrayList<String> hashesStartWith) throws Exception {
        // get the list of projects - recheck every loop
        HashMap<BigHash, String> projects = new HashMap<BigHash, String>();
        LinkedList<BigHash> hashes = new LinkedList<BigHash>();
        for (File file : tsvFiles) {
            FileReader fr = null;
            BufferedReader br = null;
            try {
                fr = new FileReader(file);
                br = new BufferedReader(fr);
                while (br.ready()) {
                    try {
                        String line = br.readLine();
                        BigHash hash = null;
                        String passphrase = "";
                        if (line.contains("\t")) {
                            hash = BigHash.createHashFromString(line.substring(0, line.indexOf("\t")).trim());
                            passphrase = line.substring(line.indexOf("\t") + 1).trim();
                        } else {
                            hash = BigHash.createHashFromString(line.trim());
                        }
                        if (hashesStartWith.size() == 0) {
                            projects.put(hash, passphrase);
                            hashes.add(hash);
                        } else {
                            for (String startWith : hashesStartWith) {
                                if (hash.toString().startsWith(startWith)) {
                                    projects.put(hash, passphrase);
                                    hashes.add(hash);
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        println("Problem reading a line in " + file);
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                println("Problem reading tsv file: " + file);
                e.printStackTrace();
            } finally {
                IOUtil.safeClose(br);
                IOUtil.safeClose(fr);
            }
        }
        for (File file : csvFiles) {
            FileReader fr = null;
            BufferedReader br = null;
            try {
                fr = new FileReader(file);
                br = new BufferedReader(fr);
                while (br.ready()) {
                    try {
                        String line = br.readLine();
                        BigHash hash = null;
                        String passphrase = "";
                        if (line.contains(",")) {
                            hash = BigHash.createHashFromString(line.substring(0, line.indexOf(",")).trim());
                            passphrase = line.substring(line.indexOf(",") + 1).trim();
                        } else {
                            hash = BigHash.createHashFromString(line.trim());
                        }
                        if (hashesStartWith.size() == 0) {
                            projects.put(hash, passphrase);
                            hashes.add(hash);
                        } else {
                            for (String startWith : hashesStartWith) {
                                if (hash.toString().startsWith(startWith)) {
                                    projects.put(hash, passphrase);
                                    hashes.add(hash);
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        println("Problem reading a line in " + file);
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                println("Problem reading csv file: " + file);
                e.printStackTrace();
            } finally {
                IOUtil.safeClose(br);
                IOUtil.safeClose(fr);
            }
        }
        if (checkPC) {
            File file = getListOfAllProjectsFile(tagsUser, tagsPass);
            FileReader fr = null;
            BufferedReader br = null;
            try {
                fr = new FileReader(file);
                br = new BufferedReader(fr);
                while (br.ready()) {
                    try {
                        String line = br.readLine();
                        BigHash hash = null;
                        String passphrase = "";
                        if (line.contains("\t")) {
                            hash = BigHash.createHashFromString(line.substring(0, line.indexOf("\t")).trim());
                            passphrase = line.substring(line.indexOf("\t") + 1).trim();
                        } else {
                            hash = BigHash.createHashFromString(line.trim());
                        }
                        if (hashesStartWith.size() == 0) {
                            projects.put(hash, passphrase);
                            hashes.add(hash);
                        } else {
                            for (String startWith : hashesStartWith) {
                                if (hash.toString().startsWith(startWith)) {
                                    projects.put(hash, passphrase);
                                    hashes.add(hash);
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        println("Problem reading a line in ProteomeCommons.org projects file.");
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                println("Problem reading ProteomeCommons.org projects file.");
                e.printStackTrace();
            } finally {
                IOUtil.safeClose(br);
                IOUtil.safeClose(fr);
            }
        }
        for (BigHash hash : hashesToCheck) {
            projects.put(hash, "");
            hashes.add(hash);
        }

        // make a collection of hashes
        if (randomize) {
            Collections.shuffle(hashes);
        }

        // go through each
        for (BigHash hash : hashes) {
            println("Checking reps of " + hash);
            String passphrase = projects.get(hash);

            // start a list of files to check
            int[] reps = new int[6];
            for (int i = 0; i <= 5; i++) {
                reps[i] = 0;
            }

            ArrayList<BigHash> files = new ArrayList<BigHash>();
            try {
                // get the meta data for this hash
                MetaData md = null;
                GetFileTool gft = new GetFileTool();
                gft.setHash(hash);
                if (passphrase != null && !passphrase.equals("")) {
                    gft.setPassphrase(passphrase);
                }
                try {
                    md = gft.getMetaData();
                } catch (Exception e) {
                    println("Could not download meta data for " + hash);
                    reps[0] = 1;
                    continue;
                }

                // if file,
                if (!md.isProjectFile()) {
                    // just check this file
                    files.add(hash);
                } else {
                    // check the project file
                    files.add(hash);
                    // download the project file and add all the project parts
                    try {
                        ProjectFile pf = gft.getProjectFile();
                        for (ProjectFilePart pfp : pf.getParts()) {
                            files.add(pfp.getHash());
                        }
                    } catch (Exception e) {
                        println("Could not download project file for " + hash);
                        e.printStackTrace();
                    }
                }

                // check all of the files
                final Map<BigHash, Integer> dataHashes = new HashMap<BigHash, Integer>();
                final Map<BigHash, Integer> metaHashes = new HashMap<BigHash, Integer>();
                for (BigHash fileHash : files) {
                    println("Checking file " + fileHash);

                    // get the meta data for this hash
                    MetaData fileMD = null;
                    GetFileTool fileGFT = new GetFileTool();
                    fileGFT.setHash(fileHash);
                    if (passphrase != null && !passphrase.equals("")) {
                        fileGFT.setPassphrase(passphrase);
                    }

                    try {
                        fileMD = fileGFT.getMetaData();
                        if (fileMD == null) {
                            throw new Exception("Could not download meta data.");
                        }
                    } catch (Exception e) {
                        println("Could not download file meta data for " + fileHash);
                        reps[0]++;
                        continue;
                    }

                    // check the meta data for this file
                    metaHashes.put(fileHash, 0);

                    // for all the chunks in the md
                    for (BigHash chunkHash : fileMD.getParts()) {
                        // check the chunk
                        dataHashes.put(chunkHash, 0);
                    }
                }
                println("Meta data to check: " + metaHashes.keySet().size());
                println("Data to check: " + dataHashes.keySet().size());

                // make the meta hash list of lists
                final List<List<BigHash>> metaHashListList = new LinkedList<List<BigHash>>();
                int listIndex = 0;
                for (BigHash metaHash : metaHashes.keySet()) {
                    try {
                        if (metaHashListList.size() == 0 || metaHashListList.get(listIndex) == null) {
                            metaHashListList.add(new LinkedList<BigHash>());
                        }
                    } catch (IndexOutOfBoundsException e) {
                        metaHashListList.add(new LinkedList<BigHash>());
                    }
                    metaHashListList.get(listIndex).add(metaHash);
                    if (metaHashListList.get(listIndex).size() == RemoteTrancheServer.BATCH_HAS_LIMIT) {
                        listIndex++;
                    }
                }
                println("Split meta data into " + metaHashListList.size() + " list(s)");

                // make the data hash list of lists
                final List<List<BigHash>> dataHashListList = new LinkedList<List<BigHash>>();
                listIndex = 0;
                for (BigHash dataHash : dataHashes.keySet()) {
                    try {
                        if (dataHashListList.size() == 0 || dataHashListList.get(listIndex) == null) {
                            dataHashListList.add(new LinkedList<BigHash>());
                        }
                    } catch (IndexOutOfBoundsException e) {
                        dataHashListList.add(new LinkedList<BigHash>());
                    }
                    dataHashListList.get(listIndex).add(dataHash);
                    if (dataHashListList.get(listIndex).size() == RemoteTrancheServer.BATCH_HAS_LIMIT) {
                        listIndex++;
                    }
                }
                println("Split data into " + dataHashListList.size() + " list(s)");

                // make sure there is a connection with all servers
                for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {
                    if (row.isCore()) {
                        try {
                            ConnectionUtil.connect(row, true);
                        } catch (Exception e) {
                        }
                    }
                }

                // for all the servers
                for (final List<BigHash> metaHashList : metaHashListList) {
                    Set<Thread> threads = new HashSet<Thread>();
                    for (final String host : ConnectionUtil.getConnectedHosts()) {
                        Thread t = new Thread() {

                            public void run() {
                                try {
                                    TrancheServer ts = ConnectionUtil.connectHost(host, true);
                                    // get back the booleans
                                    boolean[] booleanList = ts.hasMetaData(metaHashList.toArray(new BigHash[0]));
                                    for (int i = 0; i < metaHashList.size(); i++) {
                                        if (booleanList[i]) {
                                            synchronized (metaHashes) {
                                                int repCount = metaHashes.get(metaHashList.get(i));
                                                metaHashes.put(metaHashList.get(i), repCount + 1);
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                        t.setDaemon(true);
                        t.start();
                        threads.add(t);
                    }
                    for (Thread t : threads) {
                        try {
                            t.join();
                        } catch (Exception e) {
                        }
                    }
                }
                for (final List<BigHash> dataHashList : dataHashListList) {
                    Set<Thread> threads = new HashSet<Thread>();
                    for (final String host : ConnectionUtil.getConnectedHosts()) {
                        Thread t = new Thread() {

                            public void run() {
                                try {
                                    TrancheServer ts = ConnectionUtil.connectHost(host, true);
                                    // get back the booleans
                                    boolean[] booleanList = ts.hasData(dataHashList.toArray(new BigHash[0]));
                                    for (int i = 0; i < dataHashList.size(); i++) {
                                        if (booleanList[i]) {
                                            synchronized (dataHashes) {
                                                int repCount = dataHashes.get(dataHashList.get(i));
                                                dataHashes.put(dataHashList.get(i), repCount + 1);
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                        t.setDaemon(true);
                        t.start();
                        threads.add(t);
                    }
                    for (Thread t : threads) {
                        try {
                            t.join();
                        } catch (Exception e) {
                        }
                    }
                }

                UserZipFile userZipFile = null;
                if (pc_user != null && pc_user_pass != null) {
                    try {
                        userZipFile = UserZipFileUtil.getUserZipFile(pc_user, pc_user_pass);
                        userZipFile.setPassphrase(pc_user_pass);
                        if (userZipFile.getCertificate() == null || userZipFile.getPrivateKey() == null) {
                            userZipFile = null;
                        } else {
                            println("Signed in as " + pc_user);
                        }
                    } catch (Exception e) {
                    }
                }
                final UserZipFile finalUserZipFile = userZipFile;
                final StatusTable finalTable = NetworkUtil.getStatus();

                // input the rep counts
                for (final BigHash metaDataHash : metaHashes.keySet()) {
                    int repCount = metaHashes.get(metaDataHash);
                    if (repCount >= 5) {
                        reps[5]++;
                    } else {
                        if (replicate && repCount < replicationsRequired && repCount > 0 && userZipFile != null) {
                            byte[] metaDataBytes = null;
                            ROWS:
                            for (String host : ConnectionUtil.getConnectedHosts()) {
                                try {
                                    TrancheServer ts = ConnectionUtil.connectHost(host, true);
                                    if (ts == null) {
                                        throw new Exception();
                                    }
                                    try {
                                        PropagationReturnWrapper pew = IOUtil.getMetaData(ts, metaDataHash, false);
                                        if (pew != null && pew.isByteArrayDoubleDimension()) {
                                            metaDataBytes = ((byte[][]) pew.getReturnValueObject())[0];
                                            if (metaDataBytes != null) {
                                                break;
                                            }
                                        }
                                    } finally {
                                        ConnectionUtil.unlockConnection(host);
                                    }
                                } catch (Exception e) {
                                }
                            }
                            if (metaDataBytes != null) {
                                final byte[] finalMetaDataBytes = metaDataBytes;
                                Set<Thread> threads = new HashSet<Thread>();
                                for (final String host : ConnectionUtil.getConnectedHosts()) {
                                    Thread t = new Thread() {

                                        public void run() {
                                            try {
                                                TrancheServer ts = ConnectionUtil.connectHost(host, true);
                                                if (ts == null) {
                                                    throw new Exception();
                                                }
                                                try {
                                                    if (finalTable.getRow(host).isWritable() && !IOUtil.hasMetaData(ts, metaDataHash)) {
                                                        IOUtil.setMetaData(ts, finalUserZipFile.getCertificate(), finalUserZipFile.getPrivateKey(), false, metaDataHash, finalMetaDataBytes);
                                                    }
                                                } finally {
                                                    ConnectionUtil.unlockConnection(host);
                                                }
                                            } catch (Exception e) {
                                            }
                                        }
                                    };
                                    t.setDaemon(true);
                                    t.start();
                                    threads.add(t);
                                }
                                for (Thread t : threads) {
                                    try {
                                        t.join();
                                    } catch (Exception e) {
                                    }
                                }
                            }
                        }
                        reps[repCount]++;
                    }
                }
                for (final BigHash dataHash : dataHashes.keySet()) {
                    int repCount = dataHashes.get(dataHash);
                    if (repCount >= 5) {
                        reps[5]++;
                    } else {
                        if (replicate && repCount < replicationsRequired && userZipFile != null) {
                            byte[] dataBytes = null;
                            ROWS:
                            for (String host : ConnectionUtil.getConnectedHosts()) {
                                try {
                                    TrancheServer ts = ConnectionUtil.connectHost(host, true);
                                    if (ts == null) {
                                        throw new Exception();
                                    }
                                    try {
                                        PropagationReturnWrapper pew = IOUtil.getData(ts, dataHash, false);
                                        if (pew != null && pew.isByteArrayDoubleDimension()) {
                                            dataBytes = ((byte[][]) pew.getReturnValueObject())[0];
                                            if (dataBytes != null) {
                                                break;
                                            }
                                        }
                                    } finally {
                                        ConnectionUtil.unlockConnection(host);
                                    }
                                } catch (Exception e) {
                                }
                            }
                            if (dataBytes != null) {
                                final byte[] finalDataBytes = dataBytes;
                                Set<Thread> threads = new HashSet<Thread>();
                                for (final String host : ConnectionUtil.getConnectedHosts()) {
                                    Thread t = new Thread() {

                                        public void run() {
                                            try {
                                                TrancheServer ts = ConnectionUtil.connectHost(host, true);
                                                if (ts == null) {
                                                    throw new Exception();
                                                }
                                                try {
                                                    if (finalTable.getRow(host).isWritable() && !IOUtil.hasData(ts, dataHash)) {
                                                        IOUtil.setData(ts, finalUserZipFile.getCertificate(), finalUserZipFile.getPrivateKey(), dataHash, finalDataBytes);
                                                    }
                                                } finally {
                                                    ConnectionUtil.unlockConnection(host);
                                                }
                                            } catch (Exception e) {
                                            }
                                        }
                                    };
                                    t.setDaemon(true);
                                    t.start();
                                    threads.add(t);
                                }
                                for (Thread t : threads) {
                                    try {
                                        t.join();
                                    } catch (Exception e) {
                                    }
                                }
                            }
                        }
                        reps[repCount]++;
                    }
                }
                println("Rep counting finished for " + hash);
                println("   " + reps[0] + ", " + reps[1] + ", " + reps[2] + ", " + reps[3] + ", " + reps[4] + ", " + reps[5]);
            } catch (Exception e) {
                println("Problem checking reps of " + hash);
                e.printStackTrace();
            } finally {
                try {
                    report(post, hash, reps[0], reps[1], reps[2], reps[3], reps[4], reps[5]);
                } catch (Exception e) {
                    println("Could not report replications to server.");
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * <p>Returns a list of all projects. Each line is a single entry with a hash and passphrase separated by a tab.</p>
     * <p>Note that there may be comments, which start with a #, and there may be blank lines.</p>
     * @param username
     * @param passphrase
     * @return
     * @throws java.lang.Exception
     */
    public static File getListOfAllProjectsFile(
            String username, String passphrase) throws Exception {
        InputStream is = null;
        File cacheFile = null;
        FileWriter fw = null;
        try {

            // make a new client
            HttpClient c = new HttpClient();

            // make a post method
            PostMethod pm = new PostMethod("https://proteomecommons.org/scripts/data/query/all.jsp");

            List<NameValuePair> pairs = new ArrayList<NameValuePair>();

            // for the register.jsp page
            pairs.add(new NameValuePair("password", passphrase));
            pairs.add(new NameValuePair("username", username));

            NameValuePair[] pairArray = new NameValuePair[pairs.size()];
            for (int i = 0; i
                    < pairs.size(); i++) {
                pairArray[i] = pairs.get(i);
            }

// set the values
            pm.setRequestBody(pairArray);

            // execute the method
            int status = c.executeMethod(pm);
            System.out.println("HTTP status returned = " + status);

            if (status != 200) {
                throw new Exception("Did not authenticate or there is a problem with the server. (HTTP response=" + status + ")");
            }

// For some reason, getting string first helps!
            pm.getResponseBodyAsString();
            is =
                    pm.getResponseBodyAsStream();

            // create a file with the same info
            cacheFile =
                    TempFileUtil.createTemporaryFile(".cache");
            fw =
                    new FileWriter(cacheFile);
            try {
                while (is.available() > 0) {
                    fw.write(is.read());
                }

            } finally {
                IOUtil.safeClose(fw);
            }

// read the cache file
            return cacheFile;
        } finally {
            IOUtil.safeClose(is);
        }
    }
}
