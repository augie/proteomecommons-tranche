package org.proteomecommons.dfs.mirror.opd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.tranche.util.IOUtil;

/**
 * This code automatically downloads all the files from the Open Proteomics Database on-line repository, decompresses them, and adds them to the ProteomeCommons.org Distributed File System. The code is intended to run as a service where it repeatedly mirrors all the content found on the open proteomics database.
 * @author Jayson Falker - jfalkner@umich.edu
 */
public class Main {
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
        String url = "http://apropos.icmb.utexas.edu/OPD/";
        
        // make a get method
        GetMethod gm = new GetMethod(url);
        client.executeMethod(gm);
        // get the body
        String content = gm.getResponseBodyAsString();
        
        // close the method
        gm.releaseConnection();
        
        // first split by all <tr>
        String[] trSplit = content.split("<tr>");
        
        // loop through each
        for (String tr : trSplit) {
            // check if it has 12 sub-parts
            String[] tdSplit = tr.trim().split("<td class=db[^>]*>");
            if (tdSplit.length < 2 || !tdSplit[1].startsWith("<div id=\"acc\">")) {
                continue;
            }
            
            // get the sub-parts
            System.out.println("\nHandling Project:");
            
            // parse out the name
            String projectName = tdSplit[1].split("<div id=\"acc\">|</div>")[1];
            LinkedList<String> links = new LinkedList();
            for (int i=2;i<tdSplit.length;i++) {
                String[] ls = tdSplit[i].split("href=");
                // handle each link
                for (int j=1;j<ls.length;j++) {
                    String link = ls[j].split(">")[0];
                    // skip non-relative links
                    if (link.indexOf("http") != -1 || link.indexOf("not_available.txt") != -1 || (link.indexOf(".zip")==-1 && link.indexOf(".txt")==-1)) {
                        continue;
                    }
                    links.add(link);
                }
                
            }
            
            System.out.println("Project Name: "+projectName);
            // make a directory
            File projectFileDir = new File(projectName);
            projectFileDir.mkdirs();
            for (String link : links) {
                System.out.println("Link: "+link);
                try {
                    addContentToTheDFS(url+link, projectFileDir);
                } catch (Exception e) {
                    System.out.println("Problem while uploading "+link);
                    e.printStackTrace(System.out);
                }
            }
        }
    }
    
    
    private static void addContentToTheDFS(String contentUrl, File dir) throws Exception {
        String[] parts = contentUrl.split("\\.|/");
        String name = parts[parts.length-2]+"."+parts[parts.length-1];
        System.out.println("Downloading: "+contentUrl);
//        if (true) {
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
        
        // get the file
        File f = new File(dir, name);
        FileOutputStream fos = new FileOutputStream(f);
        InputStream is = gm.getResponseBodyAsStream();
        IOUtil.getBytes(is, fos);
        fos.flush();
        fos.close();
        is.close();
        
        gm.releaseConnection();
        
//        try {
//            // make the temporary location for the data
//            File tempDir = new File("tempDirOpenProteomicsDatabase/"+projectName);
//            // delete ant old data
//            IOUtil.recursiveDelete(tempDir.getParentFile());
//            
//            // loop over the entries
//            InputStream is = gm.getResponseBodyAsStream();
//            ZipInputStream zis = new ZipInputStream(is);
//            try {
//                // iterate over entries
//                for(ZipEntry ze = zis.getNextEntry(); ze != null; ze = zis.getNextEntry()) {
//                    System.out.println("  Extracted: "+ze.getName());
//                    
//                    // dump the bytes to a file
//                    byte[] buf = new byte[100000];
//                    File tempFile = new File(tempDir, ze.getName().replaceAll("\\./", ""));
//                    // make appropriate directories
//                    if (ze.isDirectory()) {
//                        tempFile.mkdirs();
//                        continue;
//                    } else {
//                        tempFile.getParentFile().mkdirs();
//                    }
//                    
//                    // temporarily save the file and upload it
//                    FileOutputStream fos = new FileOutputStream(tempFile);
//                    try {
//                        for (int bytesRead=zis.read(buf);bytesRead!=-1;bytesRead=zis.read(buf)) {
//                            fos.write(buf, 0, bytesRead);
//                        }
//                    } finally {
//                        IOUtil.safeClose(fos);
//                    }
//                }
//            } finally {
//                IOUtil.safeClose(zis);
//                IOUtil.safeClose(is);
//            }
//            
//            // add all of the files as a project
//            AddFileTool aft = new AddFileTool();
//            aft.addListener(new CommandLineAddFileToolListener(System.out));
//            List<String> coreServers = ServerUtil.getCoreServers();
//            for (String s : coreServers) {
//                aft.addServerURL(s);
//            }
//            
//            // add the files
//            try{
//                String md5String = aft.addFile(tempDir);
//                System.out.println("Upload Complete. Hash is "+md5String);
//            } catch (Exception e){
//                System.out.println("Upload Failed!");
//                e.printStackTrace(System.out);
//            } finally {
//                IOUtil.recursiveDeleteWithWarning(tempDir);
//            }
//            
//        } finally {
//            // release the connection
//            gm.releaseConnection();
//        }
    }
}
