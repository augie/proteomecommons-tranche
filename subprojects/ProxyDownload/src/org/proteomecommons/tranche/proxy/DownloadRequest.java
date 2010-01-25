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

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author James "Augie" Hill - augie@828productions.com
 */
public class DownloadRequest {
    
    public DownloadThread downloadThread = null;
    
    // request paramters
    public String hash = null, passphrase = null, regex = null;
    public List <String> servers = null;
    private long timestamp = System.currentTimeMillis();
    
    public DownloadRequest(String hash, String passphrase, String regex, List <String> servers) {
        this.hash = hash;
        this.passphrase = passphrase;
        this.regex = regex;
        this.servers = servers;
    }
    
    public void setDownloadThread(DownloadThread downloadThread) {
        this.downloadThread = downloadThread;
    }
    
    public DownloadThread getDownloadThread() {
        return downloadThread;
    }
    
    public boolean equals(Object object) {
        if (!(object instanceof DownloadRequest)) {
            return false;
        }
        DownloadRequest dr = (DownloadRequest) object;
        if ((dr.hash == null ^ hash == null) || (hash != null && !dr.hash.equals(hash))) {
            return false;
        }
        if ((dr.passphrase == null ^ passphrase == null) || (passphrase != null && !dr.passphrase.equals(passphrase))) {
            return false;
        }
        if ((dr.regex == null ^ regex == null) || (regex != null && !dr.regex.equals(regex))) {
            return false;
        }
        if (dr.servers == null ^ servers == null) {
            return false;
        }
        if (servers != null) {
            for (String server : dr.servers) {
                if (!servers.contains(server)) {
                    return false;
                }
            }
            if (dr.servers.size() != servers.size()) {
                return false;
            }
        }
        return true;
    }
    
}
