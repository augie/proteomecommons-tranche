package org.proteomecommons.dfs.mirror.peptideatlas;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.tranche.util.IOUtil;

/**
 * This code automatically downloads all the files from PeptideAtlas.org's on-line repository, decompresses them, and adds them to the ProteomeCommons.org Distributed File System. The code is intended to run as a service where it repeatedly mirrors all the content found on PeptideAtlas.org.
 * @author Jayson Falker - jfalkner@umich.edu
 */
public class Main {
    static boolean skipUpUntil = true;
    
    static X509Certificate cert = null;
    static PrivateKey key = null;
    
    /**
     * Entry point for the program.
     * @param args the command line arguments
     * @throws java.lang.Exception Exceptions are bubbled out of the program.
     */
    public static void main(String[] args) throws Exception {
        // get the pages content
        HttpClient client = new HttpClient();
        
        // the URL to get data from
        String url = "http://www.peptideatlas.org/repository/";
        
        // make a get method
        GetMethod gm = new GetMethod(url);
        client.executeMethod(gm);
        // get the body
        String content = gm.getResponseBodyAsString();
        
        
        // first part of the split
        String start = "http://db.systemsbiology.net/webapps/repository/sForm";
        
        // close the method
        gm.releaseConnection();
        
        String[] split = content.split(start+"|\">");
        
        System.out.println("Found "+split.length+" possible links.");
        
        // loop through each of the URLs
        for (int i=1;i<split.length-1;i++) {
            // skip matches that dont' start with what is expected
            if (split[i].startsWith("<br/>") || !split[i].startsWith("?expTagAndReqFil")) {
                continue;
            }
            String contentUrl = start+split[i];
            try {
                // try to get the content
                addContentToTheDFS(contentUrl);
            } catch (Exception e) {
                System.out.println("Can't upload file: "+contentUrl);
                e.printStackTrace(System.out);
            }
        }
        
        System.out.println("Finished handling links.");
    }
    
    private static int count = 0;
    private static void addContentToTheDFS(String contentUrl) throws Exception {
//        // if it is a README file, adjust for their error
//        if (contentUrl.indexOf("README")!=-1) {
//            // skip
//            System.out.println("Skipping the invalid README file...they forgot to GZIP");
//            return;
//        }
        
        // figure out the project
        String projectName = contentUrl.split("expTagAndReqFile=")[1];
        projectName = projectName.split(":")[0];
        
//        System.out.println("Downloading: "+projectName+", "+contentUrl);
//        if (count == 0) {
//            count++;
//            return;
//        }
        
        // get the pages content
        HttpClient client = new HttpClient();
        
        // make a get method);
        GetMethod gm = new GetMethod(contentUrl);
        
        // make up some fake request parameters to play with their logging
        gm.setRequestHeader("user-agent", "Chunk Norris");
        gm.setRequestHeader("accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
        gm.setRequestHeader("Referer", "http://whitehouse.com");
        // must specify an encoding, else their servlet craps out
        gm.setRequestHeader("accept-encoding", "gzip,deflate");
        gm.setRequestHeader("connection", "keep-alive");
        gm.setRequestHeader("accept-language", "en-us,en;q=0.5");
        gm.setRequestHeader("host", "whitehouse.com");
        gm.setRequestHeader("accept-charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        
        // execute the method
        client.executeMethod(gm);
        
        // get the suggested file name
        String filename = gm.getResponseHeader("Content-Disposition").getValue().split("filename=")[1].split(";")[0];
        System.out.println("Filename: "+filename);
//        for (Header header : gm.getResponseHeaders()) {
//            System.out.println(header.getName()+" = "+header.getValue());
//        }
        
        // make a directory name
        String dirName = filename.split("_")[0];
        File dir = new File(dirName);
        dir.mkdirs();
        
        // save the file
        File file = new File(dir, filename);
        // copy the file's contents
        FileOutputStream fos = new FileOutputStream(file);
        InputStream is = gm.getResponseBodyAsStream();
        IOUtil.getBytes(is, fos);
        IOUtil.safeClose(fos);
        IOUtil.safeClose(is);
        
        
//        Header[] headers = gm.getResponseHeaders();
//        for (Header header : headers) {
//            System.out.println(header.getName()+" = "+header.getValue());
//        }
        gm.releaseConnection();
        
//        try {
//            // make the temporary location for the data
//            File tempDir = new File("tempDirPeptideAtlas/"+projectName);
//            // delete ant old data
//            IOUtil.recursiveDelete(tempDir.getParentFile());
//
//            // loop over the entries
//            InputStream is = gm.getResponseBodyAsStream();
//            GZIPInputStream gis = new GZIPInputStream(is);
//            TarInputStream tis = new TarInputStream(gis);
//            try {
//                // dump the bytes to a file
//                byte[] buf = new byte[100000];
//
//                // iterate over entries and extract all to a local directory
//                for(TarEntry te = tis.getNextEntry(); te != null; te = tis.getNextEntry()) {
//                    System.out.println("  Extracted: "+te.getName());
//
//                    // make a temp file in the directory -- replace all "./"
//                    File tempFile = new File(tempDir, te.getName().replaceAll("\\./", ""));
//                    // if it is a directory, make it
//                    if (te.isDirectory()) {
//                        tempFile.mkdirs();
//                        continue;
//                    }
//                    // if it is a file, make the file
//                    else {
//                        File parent = tempFile.getParentFile();
//                        if (parent != null){
//                            parent.mkdirs();
//                        }
//                    }
//                    // save the temporary file and upload to the DFS
//                    FileOutputStream fos = new FileOutputStream(tempFile);
//                    try {
//                        for (int bytesRead=tis.read(buf);bytesRead!=-1;bytesRead=tis.read(buf)) {
//                            fos.write(buf, 0, bytesRead);
//                        }
//                    } finally {
//                        IOUtil.safeClose(fos);
//                    }
//                }
//
//                // upload the data as a project
//                AddFileTool aft = new AddFileTool();
//                List<String> coreServers = ServerUtil.getCoreServers();
//                for (String s : coreServers) {
//                    aft.addServerURL(s);
//                }
//                aft.addListener(new CommandLineAddFileToolListener(System.out));
//
//                // add the file
//                try{
//                    String md5String = aft.addFile(tempDir);
//                    System.out.println("Upload Complete. Hash is "+md5String);
//                } catch (Exception e){
//                    System.out.println("Upload Failed!");
//                    e.printStackTrace(System.out);
//                }
//
//            } finally {
//                IOUtil.safeClose(tis);
//                IOUtil.safeClose(gis);
//                IOUtil.safeClose(is);
//            }
//        } finally {
//            // release the connection
//            gm.releaseConnection();
//        }
    }
}
