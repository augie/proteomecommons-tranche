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
import java.math.BigInteger;
import java.security.Security;
import java.util.List;
import org.tranche.get.GetFileTool;
import org.tranche.hash.BigHash;
import org.tranche.meta.MetaData;
import org.tranche.servers.ServerUtil;

/**
 *
 * @author James "Augie" Hill - augie@828productions.com
 */
public class DownloadThread extends Thread {

    public static final int STATUS_STARTING = 0;
    public static final int STATUS_DOWNLOADING = 1;
    public static final int STATUS_ZIPPING = 2;
    public static final int STATUS_COMPLETED = 3;
    public static final int STATUS_FAILED = 4;    // the status of this thread
    private int status = STATUS_STARTING;

    public int getStatus() {
        return status;
    }
    private GetFileTool gft = null;
    public Exception exception = null;
    // get file tool parameters
    public String hash = null,  passphrase = null,  regex = null;
    public List<String> servers = null;
    public File downloadDirectory = new File("/opt/tomcat5/webapps/proteomecommons/data/download/" + System.currentTimeMillis()),  zipFile = null;

    public DownloadThread(String hash, String passphrase, String regex, List<String> servers) {
        this.hash = hash;
        this.passphrase = passphrase;
        this.regex = regex;
        this.servers = servers;
    }

    public boolean equals(Object object) {
        if (!(object instanceof DownloadThread)) {
            return false;
        }
        DownloadThread dt = (DownloadThread) object;
        if ((dt.hash == null ^ hash == null) || (hash != null && !dt.hash.equals(hash))) {
            return false;
        }
        if ((dt.passphrase == null ^ passphrase == null) || (passphrase != null && !dt.passphrase.equals(passphrase))) {
            return false;
        }
        if ((dt.regex == null ^ regex == null) || (regex != null && !dt.regex.equals(regex))) {
            return false;
        }
        if (dt.servers == null ^ servers == null) {
            return false;
        }
        if (servers != null) {
            for (String server : dt.servers) {
                if (!servers.contains(server)) {
                    return false;
                }
            }
            if (dt.servers.size() != servers.size()) {
                return false;
            }
        }
        return true;
    }

    public void run() {
        try {
            // validate the hash
            BigHash bigHash = BigHash.createHashFromString(hash);

            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            gft = new GetFileTool();

            // set parameters
            gft.setHash(bigHash);
            gft.setValidate(false);

            // optionally set some parameters
            if (passphrase != null && !passphrase.trim().equals("")) {
                gft.setPassphrase(passphrase);
            } else if (regex != null && !regex.trim().equals("")) {
                gft.setRegex(regex);
            } else if (servers != null) {
                for (String server : servers) {
                    gft.getServersToUse().add(server);
                }
            }

            // bootstrap the core servers
            ServerUtil.waitForStartup();

            // if the file is null, set to the default
            File saveTo = null;
            MetaData md = null;
            if (saveTo == null) {
                md = gft.getMetaData();
                // if it is a project file, save as such
                if (md.isProjectFile()) {
                    saveTo = new File(downloadDirectory + File.separator + "projectDownload");
                    saveTo.mkdirs();
                } else {
                    saveTo = new File(downloadDirectory + File.separator + md.getName());
                    saveTo.getParentFile().mkdirs();
                }
            }

            // set the status as downloading
            status = STATUS_DOWNLOADING;

            // Save single request to the specified file
            if (saveTo.isDirectory()) {
                gft.getDirectory(saveTo);
            } else {
                gft.getFile(saveTo);
            }

            // getting read to zip
            status = STATUS_ZIPPING;

            // zip up the data if this was a project download
            if (md.isProjectFile()) {
                zipFile = new File(saveTo + ".zip");
                zipFile.createNewFile();
                ZipUtil.zip(saveTo.getAbsolutePath(), zipFile.getAbsolutePath());
            } else {
                zipFile = saveTo;
            }

            // all done - the zipped file is ready to be downloaded
            status = STATUS_COMPLETED;
        } catch (Exception e) {
            exception = e;
            status = STATUS_FAILED;
        }
    }

    public BigInteger getDataDownloaded() {
        if (gft == null) {
            return BigInteger.ZERO;
        }
        return gft.getDataDownloaded();
    }

    public BigInteger getDataToDownload() {
        if (gft == null) {
            return BigInteger.ONE;
        }
        return gft.getDataToDownload();
    }
}
