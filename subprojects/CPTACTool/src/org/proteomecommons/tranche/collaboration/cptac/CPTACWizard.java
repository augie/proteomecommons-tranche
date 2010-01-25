/*
 *    Copyright 2005 The Regents of the University of Michigan
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
package org.proteomecommons.tranche.collaboration.cptac;

import java.io.File;
import java.io.InputStream;
import javax.swing.JOptionPane;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.proteomecommons.tranche.collaboration.Wizard;
import org.tranche.ConfigureTranche;
import org.tranche.add.AddFileTool;
import org.tranche.add.AddFileToolAdapter;
import org.tranche.gui.AddFileToolWizard;
import org.tranche.gui.GenericJOptionPane;
import org.tranche.hash.Base64;
import org.tranche.users.UserZipFile;
import org.tranche.util.EasySSLProtocolSocketFactory;
import org.tranche.util.EmailUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.SecurityUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class CPTACWizard extends Wizard {

    private static AddFileToolWizard aftw;
    

    static {
        Protocol.registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(), 443));
    }

    public static void main(String[] args) {
        // load PC.org Tranche!
        ProteomeCommonsTrancheConfig.load();

        Wizard.main(args);
        aftw = Wizard.getAFTWizard();

        // filter the arguments for tags
        String description = "";
        try {
            for (int i = 0; i < args.length - 1; i += 2) {
                if (args[i].equals("--vars")) {
                    String[] vars = args[i + 1].split(",,,");
                    if (vars[0] != null) {
                        aftw.tagPanel.addTag("Team Name", vars[0]);
                        description = description + "Team Name: " + vars[0] + ", ";
                    }
                    if (vars[1] != null) {
                        aftw.tagPanel.addTag("Site", vars[1]);
                        description = description + "Site: " + vars[1] + ", ";
                    }
                    if (vars[2] != null) {
                        aftw.tagPanel.addTag("Instrument", vars[2]);
                        description = description + "Instrument: " + vars[2] + ", ";
                    }
                    if (vars[3] != null) {
                        aftw.tagPanel.addTag("Sample", vars[3]);
                        description = description + "Sample: " + vars[3] + ", ";
                    }
                    if (vars[4] != null) {
                        aftw.tagPanel.addTag("Run ID", vars[4]);
                        description = description + "Run ID: " + vars[4];
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // try to load the cptac user certificate
        try {
            InputStream userIn = SecurityUtil.class.getClassLoader().getResourceAsStream("org/proteomecommons/tranche/collaboration/cptac/user.zip.encrypted");
            // load the user file
            try {
                UserZipFile uzf = new UserZipFile(userIn, "cpNacpassword");
                // set the user file
                aftw.setUserZipFile(uzf);
            } finally {
                IOUtil.safeClose(userIn);
            }
        } catch (Exception e) {
            Thread t = new Thread() {

                public void run() {
                    try {
                        EmailUtil.sendEmail("ERROR: Could not load CPTAC User Zip File", ConfigureTranche.getAdminEmailAccounts(), "");
                    } catch (Exception e) {
                    }
                }
            };
            t.start();
            e.printStackTrace();
        }

        // set the title
        aftw.step1Panel.title.setText("CPTAC; " + description);

        // set the description
        aftw.step1Panel.description.setText(description);

        // post to NIST when upload is complete
        aftw.addListener(new AddFileToolAdapter() {

            public void finishedUpload(AddFileTool aft) {
                // the upload failed - do not continue
                if (aft.projectFileHash == null) {
                    return;
                }

                // URL to post results
                String urlToPostResults = "https://chemdata.nist.gov/cptac/index.php";

                // post the result to NIST
                try {
                    System.out.println("Posting results to: " + urlToPostResults);

                    // post the results
                    HttpClient c = new HttpClient();

                    // make a post method
                    final PostMethod pm = new PostMethod(urlToPostResults);
                    NameValuePair b = new NameValuePair("hash", aft.projectFileHash.toString());
                    NameValuePair a = new NameValuePair("p", "");
                    if (aft.getPassphrase() != null && !aft.getPassphrase().equals("")) {
                        a = new NameValuePair("p", aft.getPassphrase());
                    }

                    // get the run ID from the tags
                    NameValuePair r = new NameValuePair("run_id", "0");
                    for (NameValuePair pair : aftw.tagPanel.getNameValuePairs()) {
                        if (pair.getName().equals("Run ID")) {
                            r = new NameValuePair("run_id", pair.getValue());
                        }
                    }

                    File selectedFile = null;
                    // the root directory
                    NameValuePair rootName = new NameValuePair("root", "Unknown");
                    try {
                        selectedFile = new File(aftw.step1Panel.fileText.getText());
                        rootName = new NameValuePair("root", selectedFile.getName());
                    } catch (Exception e) {
                    }
                    NameValuePair type = new NameValuePair("type", "directory");
                    if (selectedFile != null) {
                        type = new NameValuePair("type", selectedFile.isDirectory() ? "directory" : "file");
                    }

                    // set the values
                    pm.setRequestBody(new NameValuePair[]{a, b, r, rootName, type});

                    // execute the method
                    if (c.executeMethod(pm) != 200) {
                        Thread t = new Thread() {

                            public void run() {
                                try {
                                    EmailUtil.sendEmail("ERROR: CPTAC Tool NIST Registration", ConfigureTranche.getAdminEmailAccounts(), "A fatal error occurred in the CPTAC Add File Tool Wizard while trying to register on the NIST website.");
                                } catch (Exception e) {
                                }
                            }
                        };
                        t.start();
                        GenericJOptionPane.showMessageDialog(aftw, "There was a problem registering your upload with NIST.", "Error", JOptionPane.ERROR_MESSAGE);
                    }

                    // release the connection
                    pm.releaseConnection();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
