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
package org.proteomecommons.tranche.proxy;

import java.io.File;
import java.security.Security;
import java.util.List;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.tranche.add.AddFileTool;
import org.tranche.hash.BigHash;
import org.tranche.servers.ServerUtil;
import org.tranche.users.UserZipFile;

/**
 *
 * @author James "Augie" Hill - augie@828productions.com
 */
public class UploadThread extends Thread {

    public static final int STATUS_STARTING = 0;
    public static final int STATUS_UPLOADING = 1;
    public static final int STATUS_REGISTERING = 2;
    public static final int STATUS_COMPLETED = 3;
    public static final int STATUS_FAILED = 4;
    private int status = STATUS_STARTING;

    public int getStatus() {
        return status;
    }    // makes this upload thread have a nearly unique hash
    private String requestCode = null;
    private AddFileTool aft = null;
    public BigHash hash = null;
    public Exception exception = null;
    public File uploadedFile = null;
    public UserZipFile uzf = null;
    private String uzfPassphrase = null,  passphrase = null,  title = null,  description = null;
    private boolean uploadAsDirectory = true,  register = true,  useRemoteRep = false,  skipExistingFiles = false,  skipExistingChunks = true;
    private List<String> servers = null;

    public UploadThread(String requestCode) {
        this.requestCode = requestCode;
    }

    public String getRequestCode() {
        return requestCode;
    }

    public void setParameters(File uploadedFile, UserZipFile uzf, String uzfPassphrase, String passphrase, String title, String description, boolean uploadAsDirectory, boolean register, boolean useRemoteRep, boolean skipExistingFiles, boolean skipExistingChunks, List<String> servers) {
        this.uploadedFile = uploadedFile;
        this.uzf = uzf;
        this.uzfPassphrase = uzfPassphrase;
        this.passphrase = passphrase;
        this.title = title;
        this.description = description;
        this.uploadAsDirectory = uploadAsDirectory;
        this.register = register;
        this.useRemoteRep = useRemoteRep;
        this.skipExistingFiles = skipExistingFiles;
        this.skipExistingChunks = skipExistingChunks;
        this.servers = servers;
    }

    public void run() {
        try {
            ServerUtil.waitForStartup();

            // set the password of the user zip file
            uzf.setPassphrase(uzfPassphrase);

            // register the bouncy castle code
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            aft = new AddFileTool(uzf.getCertificate(), uzf.getPrivateKey());

            // should it upload as a directory as a single file?
            aft.setExplodeBeforeUpload(uploadAsDirectory);

            // set the parameters
            aft.setTitle(title);
            aft.setDescription(description);
            aft.setUseRemoteReplication(useRemoteRep);
            aft.setSkipExistingChunk(skipExistingChunks);

            if (passphrase != null && !passphrase.equals("")) {
                aft.setPassphrase(passphrase);
            }

            if (servers != null && servers.size() > 0) {
                for (String server : servers) {
                    aft.addServerURL(server);
                }
            }

            status = STATUS_UPLOADING;

            // there should only be one file - either a file or a directory
            hash = aft.addFile(uploadedFile);

            // register the upload with proteomecommons if desired
            if (register) {
                status = STATUS_REGISTERING;
                try {
                    // flag for registered
                    boolean registered = false;
                    // try to register
                    for (int registerAttempt = 0; registerAttempt < 3; registerAttempt++) {
                        // keep track of the status code
                        final int[] statusCode = new int[1];

                        // spawn registration in a thread so it can be timed out
                        Thread t = new Thread() {

                            public void run() {
                                try {
                                    // if the passphrase is null, save it as ""
                                    String p = passphrase;
                                    if (p == null) {
                                        p = "";                                    // make a new client
                                    }
                                    HttpClient c = new HttpClient();

                                    // make a post method
                                    PostMethod pm = new PostMethod("http://www.proteomecommons.org/dev/tranche/register.jsp");
                                    NameValuePair b = new NameValuePair("hash", hash.toString());
                                    NameValuePair a = new NameValuePair("passphrase", p);
                                    // set the values
                                    pm.setRequestBody(new NameValuePair[]{a, b});

                                    // execute the method
                                    statusCode[0] = c.executeMethod(pm);

                                    // release the connection
                                    pm.releaseConnection();
                                } catch (Exception e) {
                                    // do nothing
                                }
                            }
                        };
                        t.start();

                        // wait for up to 45 seconds
                        t.join(45 * 1000);
                        // pitch an exception
                        if (t.isAlive() || statusCode[0] != 200) {
                            throw new Exception("Can't register upload on ProteomeCommons.org");
                        }
                        break;
                    }
                } catch (Exception e) {
                    // do nothing
                }
            }

            status = STATUS_COMPLETED;
        } catch (Exception e) {
            exception = e;
            status = STATUS_FAILED;
        }
    }

    public long getDataUploaded() {
        if (aft == null) {
            return 0;
        }
        return aft.getDataUploaded();
    }

    public long getDataToUpload() {
        if (aft == null) {
            return 1;
        }
        return aft.getDataToUpload();
    }
}
