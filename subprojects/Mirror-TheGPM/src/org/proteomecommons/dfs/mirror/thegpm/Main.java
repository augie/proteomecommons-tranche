package org.proteomecommons.dfs.mirror.thegpm;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.zip.GZIPInputStream;
import org.tranche.add.AddFileTool;
import org.tranche.hash.BigHash;
import org.tranche.util.IOUtil;
import org.tranche.util.SecurityUtil;

/**
 * This code automatically uploads all the data from TheGPM nightly backups.
 * @author Jayson Falker - jfalkner@umich.edu
 */
public class Main {
    // the DFS to send data to
//    static String dfsURL = null;
    static X509Certificate cert = null;
    static PrivateKey key = null;
    
    /**
     * Entry point for the program.
     * @param args the command line arguments
     * @throws java.lang.Exception Exceptions are bubbled out of the program.
     */
    public static void main(String[] args) throws Exception {
        // check for args
        if (args.length < 1) {
            System.out.println("Required parameters:");
            System.out.println("1. The URL of the DFS server to use.");
            System.out.println("2. The directory where TheGPM backup file are located.");
            return;
        }
        // set the DFS instance to use
 //       dfsURL = args[0];
        
        // directory with backup info
        File dir = new File(args[1]);
        
        // try to load the cert and key
        File certFile = new File("mirror.public.certificate");
        cert = SecurityUtil.getCertificate(certFile);
        File keyFile = new File("mirror.private.key");
        key = SecurityUtil.getPrivateKey(keyFile);
        
        // stack of files
        LinkedList<File> stack = new LinkedList();
        stack.add(dir);
        
        // loop through each of the files
        while (stack.size() > 0) {
            // get the last
            File f = stack.removeLast();
            // buffer directories file
            if (f.isDirectory()) {
                File[] files = f.listFiles();
                for (int i = 0; i < files.length; i++) {
                    stack.add(files[i]);
                }
            }
            // add the file to the DFS
            else {
                // try to get the content
                addContentToTheDFS(f);
            }
        }
    }
    
    
    private static void addContentToTheDFS(File f) throws Exception {
        System.out.println("Decompressing: "+f.getCanonicalPath()+"");
        
        // loop over the entries
        FileInputStream is = new FileInputStream(f);
        BufferedInputStream bis = new BufferedInputStream(is);
        GZIPInputStream gis = new GZIPInputStream(bis);
        try {
            // dump the bytes to a file
            byte[] buf = new byte[100000];
            // make the name be the same as the files, but minus the ".gz"
            String name = f.getName().substring(0, f.getName().length()-3);
            File tempFile = new File(name);
            try {
                FileOutputStream fos = new FileOutputStream(tempFile);
                try {
                    for (int bytesRead=gis.read(buf);bytesRead!=-1;bytesRead=gis.read(buf)) {
                        fos.write(buf, 0, bytesRead);
                    }
                } finally {
                    IOUtil.safeClose(fos);
                }
                
                // use the add file tool
                AddFileTool aft = new AddFileTool(cert, key);
                aft.addServerURL("remote://dfs1.proteomecommons.org:443");
                aft.addServerURL("remote://dfs2.proteomecommons.org:443");
                aft.addServerURL("remote://dfs3.proteomecommons.org:443");
                aft.addServerURL("remote://dfs4.proteomecommons.org:443");
                aft.addServerURL("remote://dfs5.proteomecommons.org:443");
                aft.addServerURL("remote://dfs6.proteomecommons.org:443");
                aft.addServerURL("remote://dfs7.proteomecommons.org:443");
                
                // add the file
                try{
                    BigHash hash = aft.addFile(tempFile);
                    System.out.println("Upload Complete. Hash is "+hash);
                    
//                    // check on the meta-data
//                    DistributedFileSystem dfs = IOUtil.connect(dfsURL);
//                    try {
//                        MetaData md = dfs.getMetaData(md5String);
//                        // check that the name is what is expected
//                        if (!md.getOriginalName().equals(tempFile.getName())) {
//                            System.out.println("Updating meta-data name from "+md.getOriginalName()+" to "+tempFile.getName());
//                            md.setOriginalName(tempFile.getName());
//                            // correct the meta-data
//                            IOUtil.addMetaDataToDFS(dfs, cert, key, md);
//                        }
//                    } finally {
//                        IOUtil.safeClose(dfs);
//                    }
                } catch (Exception e){
                    System.out.println("Upload Failed!");
                    e.printStackTrace(System.out);
                }
            } finally {
                // safely remove the file
                IOUtil.safeDelete(tempFile);
            }
        } finally {
            IOUtil.safeClose(gis);
            IOUtil.safeClose(bis);
            IOUtil.safeClose(is);
        }
    }
    
    // a helper method to use a specific directory for the temp directory
}
