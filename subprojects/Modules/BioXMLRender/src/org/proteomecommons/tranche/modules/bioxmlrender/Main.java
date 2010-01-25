/*
 *    Copyright 2005-2007 The Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.proteomecommons.tranche.modules.bioxmlrender;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.tranche.gui.ErrorFrame;
import org.tranche.gui.GUIUtil;
import org.tranche.gui.IndeterminateProgressBar;
import org.tranche.gui.Widgets;
import org.tranche.hash.BigHash;
import org.tranche.modules.LeftMenuAnnotation;
import org.tranche.modules.PopupMenuAnnotation;
import org.tranche.modules.TrancheMethodAnnotation;
import org.tranche.modules.TrancheModuleAnnotation;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.Text;

/**
 * Renders a BioXML file using TheGPM.org
 * @author besmit
 */
@TrancheModuleAnnotation(name = "TheGPM.org BioXML Renderer Module", description = "Renders a BioXML file using TheGPM.org.")
public class Main {
    @LeftMenuAnnotation(scope="Files")
    @PopupMenuAnnotation(scope="Files")
    @TrancheMethodAnnotation(fileExtension="*",mdAnnotation="Tranche:Peaklist->*",selectionMode="single",label="View BioXML -> TheGPM.org",description="Convert a peak list from one format to another using the IO Framework.")
    public static void renderBioXML(BigHash hash) {
        
        try {
            File xmlFile = Widgets.downloadFileWithProgress(hash,".xml");
            renderBioMLUsingTheGPM(xmlFile,true);
        }
        
        // Failed
        catch(Exception e) {
            ErrorFrame ef = new ErrorFrame();
            ef.show(e, GUIUtil.getAdvancedGUI());
            return;
        }
        
    }
    
    private static void renderBioMLUsingTheGPM(final File file, final boolean delete) throws Exception {
        
        final IndeterminateProgressBar progress = new IndeterminateProgressBar("Uploading file to the GPM.org...");
        
        Thread t = new Thread() {
            public void run() {
                progress.setLocationRelativeTo(GUIUtil.getAdvancedGUI());
                progress.start();
            }
        };
        SwingUtilities.invokeLater(t);
        
        PostMethod postMethod = null;
        BufferedReader in = null;
        BufferedWriter out = null;
        File temp = TempFileUtil.createTemporaryFile(".html");
        FileReader reader = null;
        FileWriter writer = null;
        
        try {
            HttpClient client = new HttpClient();
            postMethod = new PostMethod("http://gpmdb.thegpm.org/thegpm-cgi/upview.pl");
            
            // Hack for file part
            File normalFileHack = file;
            
            Part[] parts = {
                new FilePart("spectrum, path", normalFileHack)
            };
            
            postMethod.setRequestHeader("Content-type", "multipart/form-data");
            postMethod.setRequestEntity(new MultipartRequestEntity(parts,postMethod.getParams()));
            
            int statusCode1 = client.executeMethod(postMethod);
            
            if (statusCode1 == 200) {
                
                in = new BufferedReader(new InputStreamReader(postMethod.getResponseBodyAsStream()));
                out = new BufferedWriter(new FileWriter(temp));
                
                // Write to temp file
                String str = null;
                final String host = "http://gpmdb.thegpm.org/";
                while ((str = in.readLine()) != null) {
                    
                    if (str.contains("/thegpm-cgi")) {
                        str = str.replace("/thegpm-cgi", host+"thegpm-cgi");
                    }
                    
                    if (str.contains("src=\"/")) {
                        str = str.replace("src=\"/","src=\""+host);
                    }
                    
                    if (str.contains("href=\"/")) {
                        str = str.replace("href=\"/","href=\""+host);
                    }
                    
                    out.write(str + Text.getNewLine());
                }
                
                out.flush();
                IOUtil.safeClose(in);
                IOUtil.safeClose(out);
                in = null; out = null; // Already closed
                
                progress.stop();
                
                GUIUtil.displayURL(GUIUtil.createSafeURL("file://"+temp.getAbsolutePath()));
                
                // Give browser chance to read the file
                Thread.sleep(3000);
            }
            
            else {
                JOptionPane.showMessageDialog(
                        GUIUtil.getAdvancedGUI(),
                        "There was a problem processing your request: " + postMethod.getStatusText(),
                        "Bad request for TheGPM.org",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        }
        
        finally {
            
            progress.stop();
            
            if (postMethod != null) {
                postMethod.releaseConnection();
            }
            
            if (in != null) {
                IOUtil.safeClose(in);
            }
            
            if (out != null) {
                IOUtil.safeClose(out);
            }
            
            IOUtil.safeDelete(temp);
            
            if (delete) {
                IOUtil.safeDelete(file);
            }
        }
        
    }
    
}
