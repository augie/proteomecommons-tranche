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
package org.proteomecommons.tranche.cacheupdater;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.proteomecommons.tags.Database;
import org.proteomecommons.tags.Entry;
import org.proteomecommons.tags.Tag;
import org.proteomecommons.tags.TagNames;
import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.proteomecommons.tranche.encrypteduploads.Passphrases;
import org.tranche.Signature;
import org.tranche.TrancheServer;
import org.tranche.add.AddFileTool;
import org.tranche.exceptions.CantVerifySignatureException;
import org.tranche.flatfile.DataBlockUtil;
import org.tranche.get.GetFileTool;
import org.tranche.gui.GUIUtil;
import org.tranche.gui.caches.ProjectCache;
import org.tranche.hash.BigHash;
import org.tranche.meta.MetaData;
import org.tranche.meta.MetaDataAnnotation;
import org.tranche.meta.MetaDataUtil;
import org.tranche.project.file.ProjectFile;
import org.tranche.project.file.ProjectFileUtil;
import org.tranche.project.file.part.ProjectFilePart;
import org.tranche.servers.ServerInfo;
import org.tranche.servers.ServerUtil;
import org.tranche.users.UserZipFile;
import org.tranche.util.EmailUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.Text;

public class CacheUpdater {

    public boolean makeChanges = true,  validate = false,  updateTagsDatabase = true,  makeNewCache = true,  indexTagsDatabase = true;
    private final File workingDirectory = new File("/opt/tomcat5/webapps/proteomecommons/WEB-INF/tags/cache/runs/" + System.currentTimeMillis() + "/"),  publishDirectory = new File("/opt/tomcat5/webapps/proteomecommons/data/tranche/cache/");
    private Set<String> servers = new HashSet<String>();
    private Set<BigHash> hashesOnNetwork = new HashSet<BigHash>(),  hashesInTags = new HashSet<BigHash>();
    private Set<Long> editedEntries = new HashSet<Long>(),  addedEntries = new HashSet<Long>();
    private long start = 0,  stop = 0,  editedTagCount = 0,  addedTagCount = 0,  removedTagCount = 0,  missingProjects = 0,  invalidProjects = 0,  projectsInCache = 0,  chunkAndMetaCount = 0;
    public UserZipFile user;
    private PrintStream log = null,  err = null,  changesLog = null,  missingLog = null,  invalidLog = null;
    // for calculating network file type information
    private HashMap<String, BigInteger> numFilesFileTypeMap = new HashMap<String, BigInteger>();
    private HashMap<String, BigInteger> sizeFileTypeMap = new HashMap<String, BigInteger>();

    private void populateHashesSet() {
        log.println("Discovering known servers");

        // wait for bootup
        ServerUtil.waitForStartup();

        // add the servers
        for (ServerInfo server : ServerUtil.getServers()) {
            log.println("Adding server to list: " + server.getUrl());
            servers.add(server.getUrl());
        }

        // get the second level servers that haven't already been checked
        HashSet<String> secondaryServers = new HashSet<String>();
        for (String server : servers) {
            ServerInfo si = ServerUtil.getServerInfo(server);
            secondaryServers.addAll(si.getKnownServers());
        }

        // add all the secondary servers to the list of servers
        for (String url : secondaryServers) {
            if (!servers.contains(url)) {
                log.println("Adding secondary server to list: " + url);
                servers.add(url);
            }
        }

        log.println("Finished discovering servers.");

        Thread t = new Thread("Cache Updater Hash Finder") {

            public void run() {
                List<Thread> threadList = new ArrayList<Thread>();
                for (final String server : servers) {
                    Thread s = new Thread("Cache Updater Hash Finder: " + server) {

                        public void run() {
                            try {
                                log.println("Connecting to " + server);
                                // bootstrap
                                ServerUtil.isServerOnline(server);
                                // connect to the server
                                TrancheServer ts = IOUtil.connect(server);
                                try {
                                    // get all of the projects
                                    BigInteger limit = BigInteger.valueOf(100);
                                    BigInteger offset = BigInteger.ZERO;

                                    // get the hashes
                                    for (BigHash[] serverHashes = ts.getProjectHashes(offset, limit); serverHashes.length > 0; serverHashes = ts.getProjectHashes(offset, limit)) {
                                        // increment the offset
                                        offset = offset.add(BigInteger.valueOf(serverHashes.length));

                                        // add each hash
                                        for (BigHash hash : serverHashes) {
                                            synchronized (hashesOnNetwork) {
                                                hashesOnNetwork.add(hash);
                                            }
                                        }
                                    }
                                } finally {
                                    IOUtil.safeClose(ts);
                                }
                            } catch (Exception e) {
                                log.println("ERROR: Could not get project hashes from " + server);
                                err.println(server + ": " + e.getMessage());
                            }
                        }
                    };
                    s.start();
                    threadList.add(s);
                }

                for (Thread t : threadList) {
                    try {
                        t.join();
                    } catch (Exception e) {
                    }
                }
            }
        };
        t.start();

        // move on after three minutes of looking
        try {
            t.join(3 * 60 * 1000);
        } catch (Exception e) {
        }

        log.println(hashesOnNetwork.size() + " project hashes discovered.");
    }

    private void updateTagsDatabase() {

        log.println("Starting to update the tags db");

        log.println("Deleting entries with no associated tags.");
        try {
            // check every entry
            List<Entry> entries = Database.getEntries();
            for (Entry entry : entries) {
                try {
                    int size = Database.getTags(entry.getId()).size();
                    if (size == 0) {
                        Database.deleteEntry(entry.getId());
                        log.println("Deleted entry #" + entry.getId());
                    }
                } catch (Exception e) {
                    log.println("ERROR: " + e.getMessage());
                    err.println(e.getMessage());
                }
            }
        } catch (Exception e) {
            log.println("ERROR: " + e.getMessage());
            err.println(e.getMessage());
        }

        log.println("Correcting tag names in all entries.");
        try {
            // check for and update tag names according to the TagNames list
            for (String name : TagNames.changedNames.keySet()) {
                for (Tag tag : Database.getTags(name)) {
                    removedTag(tag.getEntryId(), name, tag.getValue().trim());
                    addedTag(tag.getEntryId(), TagNames.changedNames.get(name), tag.getValue().trim());
                    Database.updateTag(tag.getId(), TagNames.changedNames.get(name), tag.getValue().trim());
                }
                for (Tag tag : Database.getTags(name + "%")) {
                    // make sure there was not another tag name that starts the same ("Tranche:Link" and "Tranche:Link Name")
                    String following = "";
                    {
                        // if there is more to the tag name than the one we are looking for
                        if (tag.getName().length() > name.length()) {
                            // skip this tag if what is left is not a number
                            try {
                                Long.valueOf(tag.getName().substring(name.length()).trim());
                                following = tag.getName().substring(name.length());
                            } catch (Exception e) {
                                break;
                            }
                        }
                    }

                    // get the new name
                    String newName = TagNames.changedNames.get(name) + following;

                    removedTag(tag.getEntryId(), name, tag.getValue().trim());
                    addedTag(tag.getEntryId(), newName, tag.getValue().trim());
                    Database.updateTag(tag.getId(), newName, tag.getValue().trim());
                }
            }
        } catch (Exception e) {
            log.println("ERROR: An unknown problem occurred revising tag names.");
            err.println(e.getMessage());
        }

        log.println("Making sure all tags that start with \"Tranche:\" are accompanied by a number.");
        try {
            // check every entry
            List<Entry> entries = Database.getEntries();
            for (Entry entry : entries) {
                // create a data structure for the set of tags
                Map<String, List<String>> tagMap = makeTagMap(entry.getId());

                // for all other links that start with "Tranche:", if it does not end in a number, then move it to the first available spot
                List<String> toRemove = new ArrayList<String>();
                Map<String, String> toAdd = new HashMap<String, String>();
                for (String name : tagMap.keySet()) {
                    if (name.startsWith("Tranche:")) {

                        // stop here if the last part of this tag is a number
                        try {
                            Long.valueOf(name.split(" ")[name.split(" ").length - 1].trim());
                            // go to the next tag
                            continue;
                        } catch (Exception e) {
                        }

                        int index = 1;
                        for (String value : tagMap.get(name)) {
                            String newName = name + " " + index;

                            // find an empty number
                            while (tagMap.get(newName) != null) {
                                index++;
                                newName = name + " " + index;
                            }

                            removedTag(entry.getId(), name, value.trim());
                            toAdd.put(newName, value.trim());
                            addedTag(entry.getId(), newName, value.trim());
                        }
                        toRemove.add(name);
                    }
                }
                // implement the removal the tags
                for (String name : toRemove) {
                    tagMap.remove(name);
                }
                // implement the addition of tags
                for (String name : toAdd.keySet()) {
                    if (tagMap.get(name) == null) {
                        tagMap.put(name, new ArrayList<String>());
                    }
                    tagMap.get(name).add(toAdd.get(name));
                }

                // update the database
                try {
                    // delete all the old tags
                    if (makeChanges) {
                        Database.deleteTags(entry.getId());
                    }
                    // add all the tags
                    for (String tagName : tagMap.keySet()) {
                        for (String tagValue : tagMap.get(tagName)) {
                            if (makeChanges) {
                                Database.addTag(entry.getId(), tagName, tagValue.trim());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.println("ERROR: There was a problem changing the database.");
                    err.println(e.getMessage());
                }
            }
        } catch (Exception e) {
            log.println("ERROR: An unknown problem occurred switching bad Tranche tag names.");
            err.println(e.getMessage());
        }

        log.println("Updating tags for all existing entries.");
        try {
            // for all of the entries that have a tag with the name that starts with TagNames.TRANCHE_LINK
            for (Entry entry : Database.getEntries()) {

                log.println("Updating the tags for entry " + entry.getId());

                // create a data structure for the set of tags
                Map<String, List<String>> tagMap = makeTagMap(entry.getId());

                // get the number of links in this entry
                int links = getNumberOfLinks(tagMap);
                log.println(links + " Tranche Links found in the entry.");

                // for all of the tranche links
                for (int linkNum = 1; linkNum <= links; linkNum++) {
                    try {
                        // try to make the hash for this tranche link
                        BigHash hash = null;
                        try {
                            hash = BigHash.createHashFromString(tagMap.get(TagNames.TRANCHE_LINK + " " + linkNum).get(0));
                            // remember that this hash is in the tags db
                            hashesInTags.add(hash);
                        } catch (Exception e) {
                            // bad hash - remove it
                            for (String value : tagMap.get(TagNames.TRANCHE_LINK + " " + linkNum)) {
                                removedTag(entry.getId(), TagNames.TRANCHE_LINK + " " + linkNum, value);
                            }
                            tagMap.remove(TagNames.TRANCHE_LINK + " " + linkNum);
                            editedEntries.add(entry.getId());
                            // update the database
                            saveToDatabase(entry.getId(), tagMap);
                            continue;
                        }

                        log.println("Trying hash: " + hash.toString());

                        // set up for the update
                        MetaData md = null;
                        ProjectFile pf = null;
                        GetFileTool gft = null;

                        // need to know if the meta data has changed
                        boolean metaDataChanged = false;

                        try {
                            // set up the getfiletool
                            gft = new GetFileTool();
                            gft.setValidate(validate);
                            gft.setHash(hash);

                            // increment the chunk meta count by the meta data
                            chunkAndMetaCount++;

                            // get the meta data
                            if (md == null) {
                                try {
                                    md = gft.getMetaData();
                                } catch (CantVerifySignatureException e) {
                                    addInvalid(entry.getId(), hash.toString(), tagMap, linkNum, e);
                                    log.println("ERROR: Downloaded meta data is invalid.");
                                } catch (Exception e) {
                                    if (e.getMessage() != null && e.getMessage().toLowerCase().contains("can't find metadata")) {
                                        addMissing(entry.getId(), hash.toString(), tagMap, linkNum);
                                        log.println("ERROR: Could not get meta data.");
                                    } else {
                                        addInvalid(entry.getId(), hash.toString(), tagMap, linkNum, e);
                                        log.println("ERROR: Downloaded meta data is invalid.");
                                    }
                                }
                            }
                        } catch (Exception e) {
                            err.println(e.getMessage());
                        }

                        // tags that require the meta data to check or add
                        if (md != null) {
                            // make sure there only valid share meta data if encrypted annotations
                            for (MetaDataAnnotation mda : md.getAnnotations()) {
                                // remove if this is not a valid share md if encrypted annotation
                                if (!mda.getName().equals(MetaDataAnnotation.SHARE_META_DATA_IF_ENCRYPTED_ANNOTATION.getName()) && mda.getValue().toLowerCase().equals(MetaDataAnnotation.SHARE_META_DATA_IF_ENCRYPTED_ANNOTATION.getValue().toLowerCase())) {
                                    removedTag(entry.getId(), hash, MetaDataAnnotation.SHARE_META_DATA_IF_ENCRYPTED_ANNOTATION.getName(), MetaDataAnnotation.SHARE_META_DATA_IF_ENCRYPTED_ANNOTATION.getValue(), true);
                                    metaDataChanged = true;
                                    md.getAnnotations().remove(mda);
                                }
                            }

                            // sift through all of the meta data annotations
                            for (MetaDataAnnotation mda : md.getAnnotations()) {
                                if (mda.getName().equals(MetaDataAnnotation.SHARE_META_DATA_IF_ENCRYPTED_ANNOTATION.getName())) {
                                    if (tagMap.get(TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum) == null) {
                                        tagMap.put(TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum, new ArrayList());
                                    }
                                    // should only be one entry
                                    if (tagMap.get(TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum).size() > 1) {
                                        // remove all but the first entry for this tag name
                                        for (int i = tagMap.get(TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum).size() - 1; i > 0; i--) {
                                            String removedValue = tagMap.get(TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum).remove(i);
                                            removedTag(entry.getId(), hash, TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum, removedValue, false);
                                        }
                                    }
                                    // in case there are no entries, just add the tag
                                    if (tagMap.get(TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum).size() == 0) {
                                        // add the tag
                                        tagMap.get(TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum).add(mda.getValue().trim());
                                        addedTag(entry.getId(), hash, TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum, mda.getValue(), false);
                                    } else if (tagMap.get(TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum).size() == 1) {
                                        if (!tagMap.get(TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum).get(0).equals(mda.getValue().trim())) {
                                            // edit the tag
                                            editedTag(entry.getId(), hash, TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum, tagMap.get(TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum).get(0), mda.getValue(), false);
                                            tagMap.get(TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum).clear();
                                            tagMap.get(TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum).add(mda.getValue().trim());
                                        }
                                    }
                                } else if (mda.getName().equals(MetaDataAnnotation.PROP_DELETE_NEW_VERSION)) {
                                    if (tagMap.get(TagNames.TRANCHE_DELETE_NEW_LINK + " " + linkNum) == null) {
                                        tagMap.put(TagNames.TRANCHE_DELETE_NEW_LINK + " " + linkNum, new ArrayList());
                                    }
                                    if (!tagMap.get(TagNames.TRANCHE_DELETE_NEW_LINK + " " + linkNum).contains(mda.getValue())) {
                                        try {
                                            BigHash deleteNewVersionHash = BigHash.createHashFromString(mda.getValue());
                                            tagMap.get(TagNames.TRANCHE_DELETE_NEW_LINK + " " + linkNum).add(deleteNewVersionHash.toString());
                                            addedTag(entry.getId(), hash, TagNames.TRANCHE_DELETE_NEW_LINK + " " + linkNum, deleteNewVersionHash.toString(), false);
                                            if (!metaDataContainsAnnotation(deleteNewVersionHash, MetaDataAnnotation.PROP_DELETE_OLD_VERSION, hash.toString())) {
                                                addMetaDataAnnotationNow(deleteNewVersionHash, MetaDataAnnotation.PROP_DELETE_OLD_VERSION, hash.toString());
                                            }
                                        } catch (Exception e) {
                                            // the hash is bad - remove the annotation
                                            removedTag(entry.getId(), hash, MetaDataAnnotation.PROP_DELETE_NEW_VERSION, mda.getValue(), true);
                                            metaDataChanged = true;
                                            md.getAnnotations().remove(mda);
                                        }
                                    }
                                } else if (mda.getName().equals(MetaDataAnnotation.PROP_DELETE_OLD_VERSION)) {
                                    if (tagMap.get(TagNames.TRANCHE_DELETE_OLD_LINK + " " + linkNum) == null) {
                                        tagMap.put(TagNames.TRANCHE_DELETE_OLD_LINK + " " + linkNum, new ArrayList());
                                    }
                                    if (!tagMap.get(TagNames.TRANCHE_DELETE_OLD_LINK + " " + linkNum).contains(mda.getValue())) {
                                        try {
                                            BigHash deleteOldVersionHash = BigHash.createHashFromString(mda.getValue());
                                            tagMap.get(TagNames.TRANCHE_DELETE_OLD_LINK + " " + linkNum).add(deleteOldVersionHash.toString());
                                            addedTag(entry.getId(), hash, TagNames.TRANCHE_DELETE_OLD_LINK + " " + linkNum, deleteOldVersionHash.toString(), false);
                                            if (!metaDataContainsAnnotation(deleteOldVersionHash, MetaDataAnnotation.PROP_DELETE_NEW_VERSION, hash.toString())) {
                                                addMetaDataAnnotationNow(deleteOldVersionHash, MetaDataAnnotation.PROP_DELETE_NEW_VERSION, hash.toString());
                                            }
                                        } catch (Exception e) {
                                            // the hash is bad - remove the annotation
                                            removedTag(entry.getId(), hash, MetaDataAnnotation.PROP_DELETE_OLD_VERSION, mda.getValue(), true);
                                            metaDataChanged = true;
                                            md.getAnnotations().remove(mda);
                                        }
                                    }
                                } else if (mda.getName().equals(MetaDataAnnotation.PROP_UNDELETED)) {
                                    if (tagMap.get(TagNames.TRANCHE_UNDELETED + " " + linkNum) == null) {
                                        tagMap.put(TagNames.TRANCHE_UNDELETED + " " + linkNum, new ArrayList());
                                    }
                                    if (!tagMap.get(TagNames.TRANCHE_UNDELETED + " " + linkNum).contains(mda.getValue())) {
                                        try {
                                            Long undeletedTimestamp = Long.valueOf(mda.getValue());
                                            tagMap.get(TagNames.TRANCHE_UNDELETED + " " + linkNum).add(String.valueOf(undeletedTimestamp));
                                            addedTag(entry.getId(), hash, TagNames.TRANCHE_UNDELETED + " " + linkNum, String.valueOf(undeletedTimestamp), false);
                                        } catch (Exception e) {
                                            // the value is not a timestamp - remove it
                                            removedTag(entry.getId(), hash, MetaDataAnnotation.PROP_UNDELETED, mda.getValue(), true);
                                            metaDataChanged = true;
                                            md.getAnnotations().remove(mda);
                                        }
                                    }
                                } else if (mda.getName().equals(MetaDataAnnotation.PROP_NEW_VERSION)) {
                                    if (tagMap.get(TagNames.TRANCHE_NEW_LINK + " " + linkNum) == null) {
                                        tagMap.put(TagNames.TRANCHE_NEW_LINK + " " + linkNum, new ArrayList());
                                    }
                                    if (!tagMap.get(TagNames.TRANCHE_NEW_LINK + " " + linkNum).contains(mda.getValue().trim())) {
                                        try {
                                            BigHash newVersionHash = BigHash.createHashFromString(mda.getValue());
                                            tagMap.get(TagNames.TRANCHE_NEW_LINK + " " + linkNum).add(newVersionHash.toString());
                                            addedTag(entry.getId(), hash, TagNames.TRANCHE_NEW_LINK + " " + linkNum, newVersionHash.toString(), false);
                                            if (!metaDataContainsAnnotation(newVersionHash, MetaDataAnnotation.PROP_OLD_VERSION, hash.toString())) {
                                                addMetaDataAnnotationNow(newVersionHash, MetaDataAnnotation.PROP_OLD_VERSION, hash.toString());
                                            }
                                        } catch (Exception e) {
                                            // the hash is bad - remove the annotation
                                            removedTag(entry.getId(), hash, MetaDataAnnotation.PROP_NEW_VERSION, mda.getValue(), true);
                                            metaDataChanged = true;
                                            md.getAnnotations().remove(mda);
                                        }
                                    }
                                } else if (mda.getName().equals(MetaDataAnnotation.PROP_OLD_VERSION)) {
                                    if (tagMap.get(TagNames.TRANCHE_OLD_LINK + " " + linkNum) == null) {
                                        tagMap.put(TagNames.TRANCHE_OLD_LINK + " " + linkNum, new ArrayList());
                                    }
                                    if (!tagMap.get(TagNames.TRANCHE_OLD_LINK + " " + linkNum).contains(mda.getValue().trim())) {
                                        try {
                                            BigHash oldVersionHash = BigHash.createHashFromString(mda.getValue());
                                            tagMap.get(TagNames.TRANCHE_OLD_LINK + " " + linkNum).add(oldVersionHash.toString());
                                            addedTag(entry.getId(), hash, TagNames.TRANCHE_OLD_LINK + " " + linkNum, oldVersionHash.toString(), false);
                                            if (!metaDataContainsAnnotation(oldVersionHash, MetaDataAnnotation.PROP_NEW_VERSION, hash.toString())) {
                                                addMetaDataAnnotationNow(oldVersionHash, MetaDataAnnotation.PROP_NEW_VERSION, hash.toString());
                                            }
                                        } catch (Exception e) {
                                            // the hash is bad - remove the annotation
                                            removedTag(entry.getId(), hash, MetaDataAnnotation.PROP_OLD_VERSION, mda.getValue(), true);
                                            metaDataChanged = true;
                                            md.getAnnotations().remove(mda);
                                        }
                                    }
                                }
                            }

                            // resolve conflicts between delete/undelete tags
                            if (tagMap.get(TagNames.TRANCHE_DELETED + " " + linkNum) != null && tagMap.get(TagNames.TRANCHE_UNDELETED + " " + linkNum) != null) {

                                // remember the action that last occurred
                                String latestTagName = "";
                                long latestActionTaken = 0;

                                for (String value : tagMap.get(TagNames.TRANCHE_DELETED + " " + linkNum)) {
                                    try {
                                        if (Long.valueOf(value) > latestActionTaken) {
                                            latestTagName = TagNames.TRANCHE_DELETED + " " + linkNum;
                                            latestActionTaken = Long.valueOf(value);
                                        }
                                    } catch (Exception e) {
                                    }
                                }
                                for (String value : tagMap.get(TagNames.TRANCHE_UNDELETED + " " + linkNum)) {
                                    try {
                                        if (Long.valueOf(value) > latestActionTaken) {
                                            latestTagName = TagNames.TRANCHE_UNDELETED + " " + linkNum;
                                            latestActionTaken = Long.valueOf(value);
                                        }
                                    } catch (Exception e) {
                                    }
                                }

                                for (String value : tagMap.get(TagNames.TRANCHE_DELETED + " " + linkNum)) {
                                    removedTag(entry.getId(), hash, TagNames.TRANCHE_DELETED + " " + linkNum, value, false);
                                }
                                for (String value : tagMap.get(TagNames.TRANCHE_UNDELETED + " " + linkNum)) {
                                    removedTag(entry.getId(), hash, TagNames.TRANCHE_UNDELETED + " " + linkNum, value, false);
                                }
                                tagMap.remove(TagNames.TRANCHE_DELETED + " " + linkNum);
                                tagMap.remove(TagNames.TRANCHE_UNDELETED + " " + linkNum);

                                // only put it back if it's a deleted tag                                    
                                if (latestTagName.equals(TagNames.TRANCHE_DELETED + " " + linkNum)) {

                                    tagMap.put(latestTagName, new ArrayList<String>());
                                    tagMap.get(latestTagName).add(String.valueOf(latestActionTaken));
                                    addedTag(entry.getId(), hash, latestTagName, String.valueOf(latestActionTaken), false);

                                    boolean found = false;
                                    // make sure the meta data annotations have no undeleted annotations
                                    for (MetaDataAnnotation mda : md.getAnnotations()) {
                                        if (mda.getName().equals(mda.PROP_UNDELETED)) {
                                            removedTag(entry.getId(), hash, MetaDataAnnotation.PROP_UNDELETED, mda.getValue(), true);
                                            metaDataChanged = true;
                                            md.getAnnotations().remove(mda);
                                        } else if (mda.getName().equals(mda.PROP_DELETED) && !mda.getValue().equals(String.valueOf(latestActionTaken))) {
                                            removedTag(entry.getId(), hash, MetaDataAnnotation.PROP_DELETED, mda.getValue(), true);
                                            metaDataChanged = true;
                                            md.getAnnotations().remove(mda);
                                        } else if (mda.getName().equals(mda.PROP_DELETED) && mda.getValue().equals(String.valueOf(latestActionTaken))) {
                                            found = true;
                                        }
                                    }
                                    if (!found) {
                                        md.addAnnotation(new MetaDataAnnotation(MetaDataAnnotation.PROP_DELETED, String.valueOf(latestActionTaken)));
                                        metaDataChanged = true;
                                        addedTag(entry.getId(), hash, MetaDataAnnotation.PROP_DELETED, String.valueOf(latestActionTaken), true);
                                    }
                                } else if (latestTagName.equals(TagNames.TRANCHE_UNDELETED + " " + linkNum)) {
                                    // make sure the meta data annotations have no undeleted annotations
                                    for (MetaDataAnnotation mda : md.getAnnotations()) {
                                        if (mda.getName().equals(mda.PROP_UNDELETED)) {
                                            removedTag(entry.getId(), hash, MetaDataAnnotation.PROP_UNDELETED, mda.getValue(), true);
                                            metaDataChanged = true;
                                            md.getAnnotations().remove(mda);
                                        } else if (mda.getName().equals(mda.PROP_DELETED)) {
                                            removedTag(entry.getId(), hash, MetaDataAnnotation.PROP_DELETED, mda.getValue(), true);
                                            metaDataChanged = true;
                                            md.getAnnotations().remove(mda);
                                        }
                                    }
                                }
                            }

                            // resolve conflicts between new link/delete new link tags
                            if (tagMap.get(TagNames.TRANCHE_DELETE_NEW_LINK + " " + linkNum) != null) {
                                for (String deleteValue : tagMap.get(TagNames.TRANCHE_DELETE_NEW_LINK + " " + linkNum)) {
                                    // remove new links with the same value
                                    if (tagMap.get(TagNames.TRANCHE_NEW_LINK + " " + linkNum) != null) {
                                        if (tagMap.get(TagNames.TRANCHE_NEW_LINK + " " + linkNum).remove(deleteValue.trim())) {
                                            removedTag(entry.getId(), hash, TagNames.TRANCHE_DELETE_NEW_LINK + " " + linkNum, deleteValue, false);
                                        }
                                    }
                                    removedTag(entry.getId(), hash, TagNames.TRANCHE_DELETE_NEW_LINK + " " + linkNum, deleteValue.trim(), false);
                                }
                                tagMap.remove(TagNames.TRANCHE_DELETE_NEW_LINK + " " + linkNum);
                            }
                            // resolve conflicts between old link/delete old link tags
                            if (tagMap.get(TagNames.TRANCHE_DELETE_OLD_LINK + " " + linkNum) != null) {
                                for (String deleteValue : tagMap.get(TagNames.TRANCHE_DELETE_OLD_LINK + " " + linkNum)) {
                                    // remove old links with the same value
                                    if (tagMap.get(TagNames.TRANCHE_OLD_LINK + " " + linkNum) != null) {
                                        if (tagMap.get(TagNames.TRANCHE_OLD_LINK + " " + linkNum).remove(deleteValue.trim())) {
                                            removedTag(entry.getId(), hash, TagNames.TRANCHE_OLD_LINK + " " + linkNum, deleteValue.trim(), false);
                                        }
                                    }
                                    removedTag(entry.getId(), hash, TagNames.TRANCHE_DELETE_OLD_LINK + " " + linkNum, deleteValue.trim(), false);
                                }
                                tagMap.remove(TagNames.TRANCHE_DELETE_OLD_LINK + " " + linkNum);
                            }
                            // make sure the meta data has all the old/new links from tags
                            if (tagMap.get(TagNames.TRANCHE_NEW_LINK + " " + linkNum) != null) {
                                for (String newLink : tagMap.get(TagNames.TRANCHE_NEW_LINK + " " + linkNum)) {
                                    boolean foundInMetaData = false;
                                    for (MetaDataAnnotation mda : md.getAnnotations()) {
                                        if (mda.getName().equals(MetaDataAnnotation.PROP_NEW_VERSION)) {
                                            if (mda.getValue().equals(newLink)) {
                                                foundInMetaData = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (!foundInMetaData) {
                                        md.addAnnotation(new MetaDataAnnotation(MetaDataAnnotation.PROP_NEW_VERSION, newLink));
                                        metaDataChanged = true;
                                        addedTag(entry.getId(), hash, MetaDataAnnotation.PROP_NEW_VERSION, newLink, true);
                                    }

                                    // make sure the new hash's meta data has this as it's old version
                                    try {
                                        BigHash newHash = BigHash.createHashFromString(newLink);
                                        if (!metaDataContainsAnnotation(newHash, MetaDataAnnotation.PROP_OLD_VERSION, hash.toString())) {
                                            addMetaDataAnnotationNow(newHash, MetaDataAnnotation.PROP_OLD_VERSION, hash.toString());
                                        }
                                    } catch (Exception e) {
                                    }
                                }
                            }
                            if (tagMap.get(TagNames.TRANCHE_OLD_LINK + " " + linkNum) != null) {
                                for (String oldLink : tagMap.get(TagNames.TRANCHE_OLD_LINK + " " + linkNum)) {
                                    boolean foundInMetaData = false;
                                    for (MetaDataAnnotation mda : md.getAnnotations()) {
                                        if (mda.getName().equals(MetaDataAnnotation.PROP_OLD_VERSION)) {
                                            if (mda.getValue().equals(oldLink)) {
                                                foundInMetaData = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (!foundInMetaData) {
                                        md.addAnnotation(new MetaDataAnnotation(MetaDataAnnotation.PROP_OLD_VERSION, oldLink));
                                        metaDataChanged = true;
                                        addedTag(entry.getId(), hash, MetaDataAnnotation.PROP_OLD_VERSION, oldLink, true);
                                    }

                                    // make sure the old hash's meta data has this as it's new version
                                    try {
                                        BigHash oldHash = BigHash.createHashFromString(oldLink);
                                        if (!metaDataContainsAnnotation(oldHash, MetaDataAnnotation.PROP_NEW_VERSION, hash.toString())) {
                                            addMetaDataAnnotationNow(oldHash, MetaDataAnnotation.PROP_NEW_VERSION, hash.toString());
                                        }
                                    } catch (Exception e) {
                                    }
                                }
                            }
                            // make sure the meta data annotation for showing meta info is there if it is in the tags
                            if (tagMap.get(TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum) != null) {
                                // should only be one entry
                                if (tagMap.get(TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum).size() > 1) {
                                    for (int i = tagMap.get(TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum).size() - 1; i > 0; i--) {
                                        String removedValue = tagMap.get(TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum).remove(i);
                                        removedTag(entry.getId(), hash, TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum, removedValue, false);
                                    }
                                }
                                // check if it needs to be added to the meta data
                                if (tagMap.get(TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum).size() == 1 && tagMap.get(TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum).get(0).toLowerCase().equals(MetaDataAnnotation.SHARE_META_DATA_IF_ENCRYPTED_ANNOTATION.getValue().toLowerCase())) {
                                    if (!tagMap.get(TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum).get(0).toLowerCase().equals(MetaDataAnnotation.SHARE_META_DATA_IF_ENCRYPTED_ANNOTATION.getValue().toLowerCase())) {
                                        // remove the tag - it's meaningless
                                        String removedValue = tagMap.get(TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum).remove(0);
                                        removedTag(entry.getId(), hash, TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum, removedValue, false);
                                    } else {
                                        // make sure the value is in the meta data
                                        boolean inMetaData = false;
                                        for (MetaDataAnnotation mda : md.getAnnotations()) {
                                            if (mda.getName().equals(MetaDataAnnotation.SHARE_META_DATA_IF_ENCRYPTED_ANNOTATION.getName()) && mda.getValue().toLowerCase().equals(MetaDataAnnotation.SHARE_META_DATA_IF_ENCRYPTED_ANNOTATION.getValue().toLowerCase())) {
                                                inMetaData = true;
                                            }
                                        }
                                        // add to the meta data?
                                        if (!inMetaData) {
                                            md.addAnnotation(MetaDataAnnotation.SHARE_META_DATA_IF_ENCRYPTED_ANNOTATION);
                                            metaDataChanged = true;
                                            addedTag(entry.getId(), hash, MetaDataAnnotation.SHARE_META_DATA_IF_ENCRYPTED_ANNOTATION.getName(), MetaDataAnnotation.SHARE_META_DATA_IF_ENCRYPTED_ANNOTATION.getValue(), true);
                                        }
                                    }
                                }
                            }

                            // set the signatures
                            {
                                String signatures = "";
                                for (Signature signature : md.getSignatures()) {
                                    String signatureStr = signature.getCert().getSubjectDN().getName().split("CN=")[1].split(",")[0];
                                    // skip adding if already exists
                                    if (tagMap.get(TagNames.TRANCHE_SIGNATURES + " " + linkNum) != null) {
                                        if (tagMap.get(TagNames.TRANCHE_SIGNATURES + " " + linkNum).get(0).contains(signatureStr)) {
                                            continue;
                                        }
                                    }
                                    signatures = signatures + signatureStr + ", ";
                                }
                                if (signatures.length() != 0) {
                                    signatures = signatures.substring(0, signatures.length() - 2);
                                    if (tagMap.get(TagNames.TRANCHE_SIGNATURES + " " + linkNum) == null) {
                                        tagMap.put(TagNames.TRANCHE_SIGNATURES + " " + linkNum, new ArrayList<String>());
                                        tagMap.get(TagNames.TRANCHE_SIGNATURES + " " + linkNum).add(signatures);
                                        addedTag(entry.getId(), hash, TagNames.TRANCHE_SIGNATURES + " " + linkNum, signatures, false);
                                    } else if (!tagMap.get(TagNames.TRANCHE_SIGNATURES + " " + linkNum).get(0).equals(signatures)) {
                                        editedTag(entry.getId(), hash, TagNames.TRANCHE_SIGNATURES + " " + linkNum, tagMap.get(TagNames.TRANCHE_SIGNATURES + " " + linkNum).get(0), signatures, false);
                                        tagMap.get(TagNames.TRANCHE_SIGNATURES + " " + linkNum).clear();
                                        tagMap.get(TagNames.TRANCHE_SIGNATURES + " " + linkNum).add(signatures);
                                    }
                                }
                            }

                            // set the timestamps
                            if (tagMap.get(TagNames.TIMESTAMP) == null) {
                                tagMap.put(TagNames.TIMESTAMP, new ArrayList<String>());
                                tagMap.get(TagNames.TIMESTAMP).add(String.valueOf(md.getTimestamp()));
                                addedTag(entry.getId(), hash, TagNames.TIMESTAMP, String.valueOf(md.getTimestamp()), false);
                            }
                            if (tagMap.get(TagNames.TRANCHE_TIMESTAMP + " " + linkNum) == null) {
                                tagMap.put(TagNames.TRANCHE_TIMESTAMP + " " + linkNum, new ArrayList<String>());
                                tagMap.get(TagNames.TRANCHE_TIMESTAMP + " " + linkNum).add(String.valueOf(md.getTimestamp()));
                                addedTag(entry.getId(), hash, TagNames.TRANCHE_TIMESTAMP + " " + linkNum, String.valueOf(md.getTimestamp()), false);
                            } else if (!tagMap.get(TagNames.TRANCHE_TIMESTAMP + " " + linkNum).get(0).equals(String.valueOf(md.getTimestamp()))) {
                                editedTag(entry.getId(), hash, TagNames.TRANCHE_TIMESTAMP + " " + linkNum, tagMap.get(TagNames.TRANCHE_TIMESTAMP + " " + linkNum).get(0), String.valueOf(md.getTimestamp()), false);
                                tagMap.get(TagNames.TRANCHE_TIMESTAMP + " " + linkNum).clear();
                                tagMap.get(TagNames.TRANCHE_TIMESTAMP + " " + linkNum).add(String.valueOf(md.getTimestamp()));
                            }

                            // date uploaded
                            String dateUploaded = makeDate(md.getTimestamp());
                            // if there were no tags, add this date uploaded
                            if (tagMap.get(TagNames.TRANCHE_DATE_UPLOADED + " " + linkNum) == null || tagMap.get(TagNames.TRANCHE_DATE_UPLOADED + " " + linkNum).size() == 0) {
                                tagMap.put(TagNames.TRANCHE_DATE_UPLOADED + " " + linkNum, new ArrayList<String>());
                                tagMap.get(TagNames.TRANCHE_DATE_UPLOADED + " " + linkNum).add(dateUploaded);
                                addedTag(entry.getId(), hash, TagNames.TRANCHE_DATE_UPLOADED + " " + linkNum, dateUploaded, false);
                            } // if the first date uploaded did not match, set this one as the only one
                            else if (!tagMap.get(TagNames.TRANCHE_DATE_UPLOADED + " " + linkNum).get(0).equals(dateUploaded)) {
                                editedTag(entry.getId(), hash, TagNames.TRANCHE_DATE_UPLOADED + " " + linkNum, tagMap.get(TagNames.TRANCHE_DATE_UPLOADED + " " + linkNum).get(0), dateUploaded, false);
                                tagMap.get(TagNames.TRANCHE_DATE_UPLOADED + " " + linkNum).clear();
                                tagMap.get(TagNames.TRANCHE_DATE_UPLOADED + " " + linkNum).add(dateUploaded);
                            }
                            // if there are more than one date uploaded tags, delete all but the first
                            while (tagMap.get(TagNames.TRANCHE_DATE_UPLOADED + " " + linkNum).size() > 1) {
                                String toDelete = tagMap.get(TagNames.TRANCHE_DATE_UPLOADED + " " + linkNum).remove(1);
                                removedTag(entry.getId(), hash, TagNames.TRANCHE_DATE_UPLOADED + " " + linkNum, toDelete, false);
                            }

                            if (md.isDeleted()) {
                                MetaDataAnnotation deletedAnnotation = null;
                                for (MetaDataAnnotation mda : md.getAnnotations()) {
                                    if (mda.getName().equals(MetaDataAnnotation.PROP_DELETED)) {
                                        try {
                                            if (deletedAnnotation == null || Long.valueOf(deletedAnnotation.getValue()) < Long.valueOf(mda.getValue())) {
                                                deletedAnnotation = mda;
                                            }
                                        } catch (Exception e) {
                                        }
                                    }
                                }
                                if (deletedAnnotation != null) {
                                    if (tagMap.get(TagNames.TRANCHE_DELETED + " " + linkNum) == null) {
                                        tagMap.put(TagNames.TRANCHE_DELETED + " " + linkNum, new ArrayList<String>());
                                        tagMap.get(TagNames.TRANCHE_DELETED + " " + linkNum).add(deletedAnnotation.getValue());
                                        addedTag(entry.getId(), hash, TagNames.TRANCHE_DELETED + " " + linkNum, deletedAnnotation.getValue(), false);
                                    } else {
                                        String highestValue = null;
                                        for (String value : tagMap.get(TagNames.TRANCHE_DELETED + " " + linkNum)) {
                                            try {
                                                if (highestValue == null || Long.valueOf(value) > Long.valueOf(highestValue)) {
                                                    highestValue = value;
                                                }
                                            } catch (Exception e) {
                                            }
                                        }
                                        if (highestValue != null && Long.valueOf(highestValue) < Long.valueOf(deletedAnnotation.getValue())) {
                                            tagMap.get(TagNames.TRANCHE_DELETED + " " + linkNum).add(deletedAnnotation.getValue());
                                            addedTag(entry.getId(), hash, TagNames.TRANCHE_DELETED + " " + linkNum, deletedAnnotation.getValue(), false);
                                        }

                                        // date deleted
                                        String dateDeleted = makeDate(Long.valueOf(highestValue));
                                        if (tagMap.get(TagNames.TRANCHE_DATE_DELETED + " " + linkNum) == null) {
                                            tagMap.put(TagNames.TRANCHE_DATE_DELETED + " " + linkNum, new ArrayList<String>());
                                            tagMap.get(TagNames.TRANCHE_DATE_DELETED + " " + linkNum).add(dateDeleted);
                                            addedTag(entry.getId(), hash, TagNames.TRANCHE_DATE_DELETED + " " + linkNum, dateDeleted, false);
                                        } else if (!tagMap.get(TagNames.TRANCHE_DATE_DELETED + " " + linkNum).get(0).equals(dateDeleted)) {
                                            editedTag(entry.getId(), hash, TagNames.TRANCHE_DATE_DELETED + " " + linkNum, tagMap.get(TagNames.TRANCHE_DATE_DELETED + " " + linkNum).get(0), dateDeleted, false);
                                            tagMap.get(TagNames.TRANCHE_DATE_DELETED + " " + linkNum).clear();
                                            tagMap.get(TagNames.TRANCHE_DATE_DELETED + " " + linkNum).add(dateDeleted);
                                        }
                                    }
                                }
                            } else {
                                if (tagMap.get(TagNames.TRANCHE_DELETED + " " + linkNum) != null) {
                                    String highestValue = "0";
                                    for (String value : tagMap.get(TagNames.TRANCHE_DELETED + " " + linkNum)) {
                                        if (highestValue == null || Long.valueOf(value) > Long.valueOf(highestValue)) {
                                            highestValue = value;
                                        }
                                    }
                                    if (!highestValue.equals("0")) {
                                        MetaDataAnnotation mda = new MetaDataAnnotation(MetaDataAnnotation.PROP_DELETED, highestValue);
                                        md.addAnnotation(mda);
                                        metaDataChanged = true;
                                        addedTag(entry.getId(), hash, MetaDataAnnotation.PROP_DELETED, highestValue, true);
                                    }
                                }
                            }

                            if (md.isEncrypted()) {
                                // if there are no encrypted tags, add "True"
                                if (tagMap.get(TagNames.TRANCHE_ENCRYPTED + " " + linkNum) == null || tagMap.get(TagNames.TRANCHE_ENCRYPTED + " " + linkNum).size() == 0) {
                                    tagMap.put(TagNames.TRANCHE_ENCRYPTED + " " + linkNum, new ArrayList<String>());
                                    tagMap.get(TagNames.TRANCHE_ENCRYPTED + " " + linkNum).add("True");
                                    addedTag(entry.getId(), hash, TagNames.TRANCHE_ENCRYPTED + " " + linkNum, "True", false);
                                } // otherwise if the first is not "True", change it
                                else if (!tagMap.get(TagNames.TRANCHE_ENCRYPTED + " " + linkNum).get(0).equals("True")) {
                                    editedTag(entry.getId(), hash, TagNames.TRANCHE_ENCRYPTED + " " + linkNum, tagMap.get(TagNames.TRANCHE_ENCRYPTED + " " + linkNum).get(0), "True", false);
                                    tagMap.get(TagNames.TRANCHE_ENCRYPTED + " " + linkNum).clear();
                                    tagMap.get(TagNames.TRANCHE_ENCRYPTED + " " + linkNum).add("True");
                                }

                                // if there's no passphrase tag yet, check the meta data
                                if (tagMap.get(TagNames.TRANCHE_PASSPHRASE + " " + linkNum) == null || tagMap.get(TagNames.TRANCHE_PASSPHRASE + " " + linkNum).size() == 0) {
                                    // is there a public passphrase?
                                    String publicPassphrase = md.getPublicPassphrase();
                                    // no public passphrase in this meta data?
                                    if (publicPassphrase == null) {
                                        // check for meta data on every server to see if there is a public passphrase set there
                                        // possible a lost meta data was recovered that was not available during publishing
                                        for (String url : servers) {
                                            TrancheServer ts = null;
                                            try {
                                                ts = IOUtil.connect(url);
                                                // get the meta data from the server
                                                MetaData mdForPassphrase = MetaDataUtil.read(new ByteArrayInputStream(ts.getMetaData(hash)));
                                                // does this md have a passphrase?
                                                String passphraseInMD = mdForPassphrase.getPublicPassphrase();
                                                // got one! break the loop
                                                if (passphraseInMD != null) {
                                                    publicPassphrase = passphraseInMD;
                                                    break;
                                                }
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            } finally {
                                                IOUtil.safeClose(ts);
                                            }
                                        }
                                    }
                                    // if still nothing, try to get the passphrase from the passphrases database
                                    if (publicPassphrase == null) {
                                        // need to go to the private passphrases to download the project file
                                        if (Passphrases.getPassphrase(hash) != null) {
                                            gft.setPassphrase(Passphrases.getPassphrase(hash));
                                            log.println("Set passphrase from private passphrase db");
                                        } else {
                                            log.println("No public or private passphrase found - cannot get project information.");
                                        }
                                    } else {
                                        log.println("Public passphrase set from the meta data.");
                                        tagMap.put(TagNames.TRANCHE_PASSPHRASE + " " + linkNum, new ArrayList<String>());
                                        tagMap.get(TagNames.TRANCHE_PASSPHRASE + " " + linkNum).add(publicPassphrase);
                                        addedTag(entry.getId(), hash, TagNames.TRANCHE_PASSPHRASE + " " + linkNum, publicPassphrase, false);
                                    }
                                }

                                // the passphrase could have been set if it was found in the meta data
                                if (tagMap.get(TagNames.TRANCHE_PASSPHRASE + " " + linkNum) != null && !tagMap.get(TagNames.TRANCHE_PASSPHRASE + " " + linkNum).isEmpty()) {
                                    // remove all but the one of the passphrases - should never be more than one
                                    if (tagMap.get(TagNames.TRANCHE_PASSPHRASE + " " + linkNum).size() > 1) {
                                        log.println("More than one passphrase tag found - removing all but the first.");
                                        // delete all but the first
                                        while (tagMap.get(TagNames.TRANCHE_PASSPHRASE + " " + linkNum).size() > 1) {
                                            String toDelete = tagMap.get(TagNames.TRANCHE_PASSPHRASE + " " + linkNum).remove(1);
                                            removedTag(entry.getId(), hash, TagNames.TRANCHE_PASSPHRASE + " " + linkNum, toDelete, false);
                                        }
                                    }

                                    // determine which public passphrase to use
                                    String publicPassphrase = null;

                                    // if the published passphrase and the tags passphrase are different, use the most recent
                                    if (md.isPublicPassphraseSet() && !tagMap.get(TagNames.TRANCHE_PASSPHRASE + " " + linkNum).get(0).equals(md.getPublicPassphrase())) {
                                        // get the date from the tags
                                        String dateTags = null;
                                        if (tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum) != null && !tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum).isEmpty()) {
                                            dateTags = tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum).get(0);
                                        }
                                        // get the date from the meta data
                                        String dateMetaData = null;
                                        for (MetaDataAnnotation mda : md.getAnnotations()) {
                                            try {
                                                if (mda.getName().equals(mda.PROP_PUBLISHED_TIMESTAMP)) {
                                                    dateMetaData = makeDate(Long.valueOf(mda.getValue()));
                                                }
                                            } catch (Exception e) {
                                            }
                                        }

                                        boolean useTags = true;
                                        if (dateMetaData != null && dateTags == null) {
                                            useTags = false;
                                        } else if (dateMetaData != null && dateTags != null) {
                                            if (dateMetaData.compareTo(dateTags) >= 0) {
                                                useTags = false;
                                            }
                                        }

                                        if (useTags) {
                                            publicPassphrase = tagMap.get(TagNames.TRANCHE_PASSPHRASE + " " + linkNum).get(0);
                                        } else {
                                            // change the tags to be the same as the md public passphrase
                                            String toDelete = tagMap.get(TagNames.TRANCHE_PASSPHRASE + " " + linkNum).remove(0);
                                            removedTag(entry.getId(), hash, TagNames.TRANCHE_PASSPHRASE + " " + linkNum, toDelete, false);
                                            tagMap.get(TagNames.TRANCHE_PASSPHRASE + " " + linkNum).add(md.getPublicPassphrase());
                                            addedTag(entry.getId(), hash, TagNames.TRANCHE_PASSPHRASE + " " + linkNum, md.getPublicPassphrase(), false);
                                            publicPassphrase = md.getPublicPassphrase();
                                        }
                                    } else {
                                        publicPassphrase = tagMap.get(TagNames.TRANCHE_PASSPHRASE + " " + linkNum).get(0);
                                    }

                                    if (publicPassphrase != null) {
                                        // just go ahead and publish the passphras to all meta data no matter what
                                        // in the future, need to check all meta data to see if it's necessary to republish the passphrase
                                        // set the gft passphrase
                                        gft.setPassphrase(publicPassphrase);
                                        // set the passphrase in the meta data
                                        md.setPublicPassphrase(publicPassphrase);
                                        metaDataChanged = true;
                                    }
                                }

                                // how many published passphrase annotations are there?
                                int publishedMetaDataAnnotationCount = 0;
                                for (MetaDataAnnotation mda : md.getAnnotations()) {
                                    if (mda.getName().equals(MetaDataAnnotation.PROP_PUBLISHED_TIMESTAMP)) {
                                        publishedMetaDataAnnotationCount++;
                                    }
                                }
                                // if need to determine date published
                                if (md.getPublicPassphrase() == null) {
                                    // make sure there are no published meta annotations
                                    while (publishedMetaDataAnnotationCount > 0) {
                                        for (MetaDataAnnotation mda : md.getAnnotations()) {
                                            if (mda.getName().equals(MetaDataAnnotation.PROP_PUBLISHED_TIMESTAMP)) {
                                                removedTag(entry.getId(), hash, MetaDataAnnotation.PROP_PUBLISHED_TIMESTAMP, mda.getValue(), true);
                                                metaDataChanged = true;
                                                md.getAnnotations().remove(mda);
                                                publishedMetaDataAnnotationCount--;
                                                break;
                                            }
                                        }
                                    }
                                    // set the published tag to "Unknown"
                                    if (tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum) == null || tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum).size() == 0) {
                                        tagMap.put(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum, new ArrayList<String>());
                                        tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum).add("Unknown");
                                        addedTag(entry.getId(), hash, TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum, "Unknown", false);
                                    } else if (!tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum).get(0).equals("Unknown")) {
                                        editedTag(entry.getId(), hash, TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum, tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum).get(0), "Unknown", false);
                                        tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum).clear();
                                        tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum).add("Unknown");
                                    }
                                } else {
                                    // are there no published annotations in the md? very odd
                                    if (publishedMetaDataAnnotationCount == 0) {
                                        // add a published meta data annotation with the current timestamp
                                        String timestampStr = String.valueOf(System.currentTimeMillis());
                                        addedTag(entry.getId(), hash, MetaDataAnnotation.PROP_PUBLISHED_TIMESTAMP, timestampStr, true);
                                        metaDataChanged = true;
                                        md.getAnnotations().add(new MetaDataAnnotation(MetaDataAnnotation.PROP_PUBLISHED_TIMESTAMP, timestampStr));
                                        publishedMetaDataAnnotationCount++;
                                    } else {
                                        // remove all but one of the published timestamps
                                        while (publishedMetaDataAnnotationCount > 1) {
                                            for (MetaDataAnnotation mda : md.getAnnotations()) {
                                                if (mda.getName().equals(MetaDataAnnotation.PROP_PUBLISHED_TIMESTAMP)) {
                                                    removedTag(entry.getId(), hash, MetaDataAnnotation.PROP_PUBLISHED_TIMESTAMP, mda.getValue(), true);
                                                    metaDataChanged = true;
                                                    md.getAnnotations().remove(mda);
                                                    publishedMetaDataAnnotationCount--;
                                                    break;
                                                }
                                            }
                                        }
                                        // get the date published
                                        String datePublished = null;
                                        for (MetaDataAnnotation mda : md.getAnnotations()) {
                                            if (mda.getName().equals(MetaDataAnnotation.PROP_PUBLISHED_TIMESTAMP)) {
                                                // exception thrown if value is not a timestamp
                                                try {
                                                    datePublished = makeDate(Long.valueOf(mda.getValue()));
                                                } catch (Exception e) {
                                                    // delete this annotation
                                                    removedTag(entry.getId(), hash, MetaDataAnnotation.PROP_PUBLISHED_TIMESTAMP, mda.getValue(), true);
                                                    metaDataChanged = true;
                                                    md.getAnnotations().remove(mda);
                                                    publishedMetaDataAnnotationCount--;
                                                } finally {
                                                    break;
                                                }
                                            }
                                        }

                                        // if the date published was bad - had to remove it, so put in a new one
                                        if (datePublished == null) {
                                            // set the new date published to now
                                            long timestamp = System.currentTimeMillis();
                                            String timestampStr = String.valueOf(timestamp);
                                            datePublished = makeDate(timestamp);

                                            // add the date published annotation to the meta data
                                            if (publishedMetaDataAnnotationCount == 0) {
                                                // add a meta data annotation
                                                addedTag(entry.getId(), hash, MetaDataAnnotation.PROP_PUBLISHED_TIMESTAMP, datePublished, true);
                                                metaDataChanged = true;
                                                md.getAnnotations().add(new MetaDataAnnotation(MetaDataAnnotation.PROP_PUBLISHED_TIMESTAMP, datePublished));
                                                publishedMetaDataAnnotationCount++;
                                            }
                                        }

                                        // if no date published tag, add it
                                        if (tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum) == null || tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum).size() == 0) {
                                            tagMap.put(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum, new ArrayList<String>());
                                            tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum).add(datePublished);
                                            addedTag(entry.getId(), hash, TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum, datePublished, false);
                                        } // if our date published annotation is wrong, reset it
                                        else if (!tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum).get(0).equals(datePublished)) {
                                            editedTag(entry.getId(), hash, TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum, tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum).get(0), datePublished, false);
                                            tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum).clear();
                                            tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum).add(datePublished);
                                        }
                                    }
                                }
                            } else {
                                // if there are no encrypted tags, add "False"
                                if (tagMap.get(TagNames.TRANCHE_ENCRYPTED + " " + linkNum) == null || tagMap.get(TagNames.TRANCHE_ENCRYPTED + " " + linkNum).size() == 0) {
                                    tagMap.put(TagNames.TRANCHE_ENCRYPTED + " " + linkNum, new ArrayList<String>());
                                    tagMap.get(TagNames.TRANCHE_ENCRYPTED + " " + linkNum).add("False");
                                    addedTag(entry.getId(), hash, TagNames.TRANCHE_ENCRYPTED + " " + linkNum, "False", false);
                                } // otherwise if the first is not "False", change it
                                else if (!tagMap.get(TagNames.TRANCHE_ENCRYPTED + " " + linkNum).get(0).equals("False")) {
                                    editedTag(entry.getId(), hash, TagNames.TRANCHE_ENCRYPTED + " " + linkNum, tagMap.get(TagNames.TRANCHE_ENCRYPTED + " " + linkNum).get(0), "False", false);
                                    tagMap.get(TagNames.TRANCHE_ENCRYPTED + " " + linkNum).clear();
                                    tagMap.get(TagNames.TRANCHE_ENCRYPTED + " " + linkNum).add("False");
                                }

                                // remove any passphrase tags if they exist
                                if (tagMap.get(TagNames.TRANCHE_PASSPHRASE + " " + linkNum) != null) {
                                    for (String toDelete : tagMap.get(TagNames.TRANCHE_PASSPHRASE + " " + linkNum)) {
                                        if (tagMap.get(TagNames.TRANCHE_PASSPHRASE + " " + linkNum).remove(toDelete)) {
                                            removedTag(entry.getId(), hash, TagNames.TRANCHE_PASSPHRASE + " " + linkNum, toDelete, false);
                                        }
                                    }
                                    // remove the arraylist so there is no confusion
                                    tagMap.remove(TagNames.TRANCHE_PASSPHRASE + " " + linkNum);
                                }

                                // the date published equals to the meta data timestamp
                                // if there were no tags, add this date uploaded as the date published
                                if (tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum) == null || tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum).size() == 0) {
                                    tagMap.put(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum, new ArrayList<String>());
                                    tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum).add(dateUploaded);
                                    addedTag(entry.getId(), hash, TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum, dateUploaded, false);
                                } // if the first date published did not match the date uploaded, set this one as the only one
                                else if (!tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum).get(0).equals(dateUploaded)) {
                                    editedTag(entry.getId(), hash, TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum, tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum).get(0), dateUploaded, false);
                                    tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum).clear();
                                    tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum).add(dateUploaded);
                                }
                            }
                            // if there are more than one encrypted tags, delete all but the first
                            if (tagMap.get(TagNames.TRANCHE_ENCRYPTED + " " + linkNum) != null) {
                                while (tagMap.get(TagNames.TRANCHE_ENCRYPTED + " " + linkNum).size() > 1) {
                                    String toDelete = tagMap.get(TagNames.TRANCHE_ENCRYPTED + " " + linkNum).remove(1);
                                    removedTag(entry.getId(), hash, TagNames.TRANCHE_ENCRYPTED + " " + linkNum, toDelete, false);
                                }
                            }
                            // if there are more than one date published tags, delete all but the first
                            if (tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum) != null) {
                                while (tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum).size() > 1) {
                                    String toDelete = tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum).remove(1);
                                    removedTag(entry.getId(), hash, TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum, toDelete, false);
                                }
                            }

                            // get the project file
                            if (pf == null && md.isProjectFile()) {
                                try {
                                    if (md != null && md.isProjectFile()) {
                                        // increment the chunk meta count by the size of the project file
                                        chunkAndMetaCount += md.getParts().size();

                                        File tempFile = TempFileUtil.createTemporaryFile();
                                        try {
                                            // catch invalid downloads, throw otherwise
                                            try {
                                                gft.getFile(tempFile);
                                            } catch (CantVerifySignatureException e) {
                                                addInvalid(entry.getId(), hash.toString(), tagMap, linkNum, e);
                                                log.println("ERROR: Downloaded project file is invalid.");
                                            } catch (Exception e) {
                                                if (e.getMessage() != null && (e.getMessage().toLowerCase().contains("invalid") || e.getMessage().toLowerCase().contains("validate") || e.getMessage().toLowerCase().contains("Decoded file does not match the expected file!"))) {
                                                    addInvalid(entry.getId(), hash.toString(), tagMap, linkNum, e);
                                                    log.println("ERROR: Project file invalid.");
                                                } else {
                                                    err.println(e.getMessage());
                                                    throw e;
                                                }
                                            }
                                            // treat it as if it is a project file
                                            FileInputStream fis = null;
                                            BufferedInputStream bis = null;
                                            try {
                                                fis = new FileInputStream(tempFile);
                                                bis = new BufferedInputStream(fis);
                                                pf = ProjectFileUtil.read(bis);
                                            } catch (Exception e) {
                                                log.println("ERROR: Project file invalid.");
                                                addInvalid(entry.getId(), hash.toString(), tagMap, linkNum, e);
                                                bis.close();
                                                fis.close();
                                            }
                                        } finally {
                                            try {
                                                tempFile.delete();
                                            } catch (Exception e) {
                                                err.println(e.getMessage());
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    log.println("ERROR: Could not get project file");
                                    err.println(e.getMessage());
                                }
                            }

                            if (pf != null && md.isProjectFile()) {
                                // go through all of the files getting the file type and size
                                try {
                                    for (ProjectFilePart pfp : pf.getParts()) {
                                        try {
                                            // update the number of meta data and chunks count
                                            // meta data
                                            chunkAndMetaCount++;
                                            // # chunks = ceiling of the size of the file divided by one MB 
                                            chunkAndMetaCount += Math.ceil(Double.valueOf(pfp.getPaddingAdjustedLength()) / Double.valueOf(DataBlockUtil.ONE_MB));

                                            // read the name and the size
                                            String name = pfp.getRelativeName().trim().toLowerCase();
                                            if (name.contains("/")) {
                                                name = name.substring(name.lastIndexOf('/') + 1);
                                            }
                                            if (!name.contains(".")) {
                                                continue;
                                            }
                                            long size = pfp.getPaddingAdjustedLength();

                                            // parse the file type(s)
                                            //while (name.contains(".")) {
                                            name = name.substring(name.lastIndexOf(".") + 1);

                                            // create the keys if there are none
                                            if (!numFilesFileTypeMap.containsKey(name)) {
                                                numFilesFileTypeMap.put(name, BigInteger.ZERO);
                                            }
                                            if (!sizeFileTypeMap.containsKey(name)) {
                                                sizeFileTypeMap.put(name, BigInteger.ZERO);
                                            }

                                            // increment the values appropriately
                                            numFilesFileTypeMap.put(name, numFilesFileTypeMap.get(name).add(BigInteger.ONE));
                                            sizeFileTypeMap.put(name, sizeFileTypeMap.get(name).add(BigInteger.valueOf(size)));
//                                            }
                                        } catch (Exception e) {
                                            log.println("ERROR: Problem reading a file's information.");
                                            err.println(e.getMessage());
                                        }
                                    }
                                    printFileTypeLog();
                                } catch (Exception e) {
                                    log.println("ERROR: Problem reading file type information.");
                                    err.println(e.getMessage());
                                }

                                // set the files no matter what
                                if (tagMap.get(TagNames.TRANCHE_FILES + " " + linkNum) == null) {
                                    tagMap.put(TagNames.TRANCHE_FILES + " " + linkNum, new ArrayList<String>());
                                    tagMap.get(TagNames.TRANCHE_FILES + " " + linkNum).add(String.valueOf(pf.getParts().size()));
                                    addedTag(entry.getId(), hash, TagNames.TRANCHE_FILES + " " + linkNum, String.valueOf(pf.getParts().size()), false);
                                } else if (!tagMap.get(TagNames.TRANCHE_FILES + " " + linkNum).get(0).equals(String.valueOf(pf.getParts().size()))) {
                                    editedTag(entry.getId(), hash, TagNames.TRANCHE_FILES + " " + linkNum, tagMap.get(TagNames.TRANCHE_FILES + " " + linkNum).get(0), String.valueOf(pf.getParts().size()), false);
                                    tagMap.get(TagNames.TRANCHE_FILES + " " + linkNum).clear();
                                    tagMap.get(TagNames.TRANCHE_FILES + " " + linkNum).add(String.valueOf(pf.getParts().size()));
                                }

                                // set the size no matter what
                                if (tagMap.get(TagNames.TRANCHE_SIZE + " " + linkNum) == null) {
                                    tagMap.put(TagNames.TRANCHE_SIZE + " " + linkNum, new ArrayList<String>());
                                    tagMap.get(TagNames.TRANCHE_SIZE + " " + linkNum).add(pf.getSize().toString());
                                    addedTag(entry.getId(), hash, TagNames.TRANCHE_SIZE + " " + linkNum, pf.getSize().toString(), false);
                                } else if (!tagMap.get(TagNames.TRANCHE_SIZE + " " + linkNum).get(0).equals(pf.getSize().toString())) {
                                    editedTag(entry.getId(), hash, TagNames.TRANCHE_SIZE + " " + linkNum, tagMap.get(TagNames.TRANCHE_SIZE + " " + linkNum).get(0), pf.getSize().toString(), false);
                                    tagMap.get(TagNames.TRANCHE_SIZE + " " + linkNum).clear();
                                    tagMap.get(TagNames.TRANCHE_SIZE + " " + linkNum).add(pf.getSize().toString());
                                }

                                // title
                                if (tagMap.get(TagNames.TITLE) == null && pf.getName() != null) {
                                    tagMap.put(TagNames.TITLE, new ArrayList<String>());
                                    tagMap.get(TagNames.TITLE).add(pf.getName());
                                    addedTag(entry.getId(), hash, TagNames.TITLE, pf.getName(), false);
                                }

                                // tranche link name
                                //  only have a tranche link name if it is different than the title
                                if (tagMap.get(TagNames.TRANCHE_LINK_NAME + " " + linkNum) != null && pf.getName() != null && tagMap.get(TagNames.TRANCHE_LINK_NAME + " " + linkNum).get(0).equals(tagMap.get(TagNames.TITLE).get(0))) {
                                    for (String value : tagMap.get(TagNames.TRANCHE_LINK_NAME + " " + linkNum)) {
                                        removedTag(entry.getId(), hash, TagNames.TRANCHE_LINK_NAME + " " + linkNum, value, false);
                                    }
                                    tagMap.remove(TagNames.TRANCHE_LINK_NAME + " " + linkNum);
                                }

                                // description
                                if (tagMap.get(TagNames.DESCRIPTION) == null && pf.getDescription() != null) {
                                    tagMap.put(TagNames.DESCRIPTION, new ArrayList<String>());
                                    tagMap.get(TagNames.DESCRIPTION).add(pf.getDescription());
                                    addedTag(entry.getId(), hash, TagNames.DESCRIPTION, pf.getDescription(), false);
                                }

                                // tranche description
                                //  only have a tranche link if it is different from the entry description
                                if (tagMap.get(TagNames.TRANCHE_DESCRIPTION + " " + linkNum) != null && pf.getDescription() != null && tagMap.get(TagNames.TRANCHE_DESCRIPTION + " " + linkNum).get(0).equals(tagMap.get(TagNames.DESCRIPTION).get(0))) {
                                    for (String value : tagMap.get(TagNames.TRANCHE_DESCRIPTION + " " + linkNum)) {
                                        removedTag(entry.getId(), hash, TagNames.TRANCHE_DESCRIPTION + " " + linkNum, value, false);
                                    }
                                    tagMap.remove(TagNames.TRANCHE_DESCRIPTION + " " + linkNum);
                                }
                            }

                            if (!md.isProjectFile()) {
                                // # chunks = ceiling of the size of the file divided by one MB 
                                chunkAndMetaCount += md.getParts().size();

                                // set the files no matter what
                                if (tagMap.get(TagNames.TRANCHE_FILES + " " + linkNum) == null) {
                                    tagMap.put(TagNames.TRANCHE_FILES + " " + linkNum, new ArrayList<String>());
                                    tagMap.get(TagNames.TRANCHE_FILES + " " + linkNum).add("1");
                                    addedTag(entry.getId(), hash, TagNames.TRANCHE_FILES + " " + linkNum, "1", false);
                                } else if (!tagMap.get(TagNames.TRANCHE_FILES + " " + linkNum).get(0).equals("1")) {
                                    editedTag(entry.getId(), hash, TagNames.TRANCHE_FILES + " " + linkNum, tagMap.get(TagNames.TRANCHE_FILES + " " + linkNum).get(0), "1", false);
                                    tagMap.get(TagNames.TRANCHE_FILES + " " + linkNum).clear();
                                    tagMap.get(TagNames.TRANCHE_FILES + " " + linkNum).add("1");
                                }

                                // set the size no matter what
                                if (tagMap.get(TagNames.TRANCHE_SIZE + " " + linkNum) == null) {
                                    tagMap.put(TagNames.TRANCHE_SIZE + " " + linkNum, new ArrayList<String>());
                                    tagMap.get(TagNames.TRANCHE_SIZE + " " + linkNum).add(String.valueOf(hash.getLength()));
                                    addedTag(entry.getId(), hash, TagNames.TRANCHE_SIZE + " " + linkNum, String.valueOf(hash.getLength()), false);
                                } else if (!tagMap.get(TagNames.TRANCHE_SIZE + " " + linkNum).get(0).equals(String.valueOf(hash.getLength()))) {
                                    editedTag(entry.getId(), hash, TagNames.TRANCHE_SIZE + " " + linkNum, tagMap.get(TagNames.TRANCHE_SIZE + " " + linkNum).get(0), String.valueOf(hash.getLength()), false);
                                    tagMap.get(TagNames.TRANCHE_SIZE + " " + linkNum).clear();
                                    tagMap.get(TagNames.TRANCHE_SIZE + " " + linkNum).add(String.valueOf(hash.getLength()));
                                }

                                // add the title
                                if (md.getName() != null) {
                                    if (tagMap.get(TagNames.TITLE) == null) {
                                        tagMap.put(TagNames.TITLE, new ArrayList<String>());
                                        tagMap.get(TagNames.TITLE).add(md.getName());
                                        addedTag(entry.getId(), hash, TagNames.TITLE, md.getName(), false);
                                    }

                                    // check if the tranche link name is different from the title
                                    if (tagMap.get(TagNames.TRANCHE_LINK_NAME + " " + linkNum) != null && tagMap.get(TagNames.TRANCHE_LINK_NAME + " " + linkNum).get(0).equals(tagMap.get(TagNames.TITLE).get(0))) {
                                        for (String value : tagMap.get(TagNames.TRANCHE_LINK_NAME + " " + linkNum)) {
                                            removedTag(entry.getId(), hash, TagNames.TRANCHE_LINK_NAME + " " + linkNum, value, false);
                                        }
                                        tagMap.remove(TagNames.TRANCHE_LINK_NAME + " " + linkNum);
                                    }
                                }
                            }
                        }

                        // just go ahead and possibly overwrite all older values of TagNames.HAS_DATA
                        if (tagMap.get(TagNames.HAS_DATA) == null) {
                            tagMap.put(TagNames.HAS_DATA, new ArrayList<String>());
                            tagMap.get(TagNames.HAS_DATA).add("Yes");
                            addedTag(entry.getId(), hash, TagNames.HAS_DATA, "Yes", false);
                        } else if (!tagMap.get(TagNames.HAS_DATA).get(0).equals("Yes")) {
                            editedTag(entry.getId(), hash, TagNames.HAS_DATA, tagMap.get(TagNames.HAS_DATA).get(0), "Yes", false);
                            tagMap.get(TagNames.HAS_DATA).clear();
                            tagMap.get(TagNames.HAS_DATA).add("Yes");
                        }

                        if (tagMap.get(TagNames.TYPE) == null) {
                            tagMap.put(TagNames.TYPE, new ArrayList<String>());
                            tagMap.get(TagNames.TYPE).add("Data");
                            addedTag(entry.getId(), hash, TagNames.TYPE, "Data", false);
                        }

                        if (metaDataChanged && makeChanges) {
                            log.println("Publishing changes to the meta data.");

                            // create the bytestream
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            // turn the metaData into a byte stream
                            MetaDataUtil.write(md, baos);

                            for (String url : servers) {
                                try {
                                    // connect
                                    TrancheServer ts = IOUtil.connect(url);
                                    try {
                                        if (ts.hasMetaData(hash)) {
                                            // upload the changes - try up to 3 times
                                            Exception ex = null;
                                            for (int i = 0; i < 3; i++) {
                                                try {
                                                    IOUtil.setMetaData(ts, user.getCertificate(), user.getPrivateKey(), hash, baos.toByteArray());
                                                    log.println("Set meta data to " + url);
                                                    ex = null;
                                                    break;
                                                } catch (Exception e) {
                                                    ex = e;
                                                    continue;
                                                }
                                            }
                                            if (ex != null) {
                                                throw ex;
                                            }
                                        }
                                    } finally {
                                        IOUtil.safeClose(ts);
                                    }
                                } catch (Exception e) {
                                    err.println(e.getMessage());
                                    log.println("ERROR: Could not set meta data to " + url);
                                }
                            }

                            log.println("Done publishing meta data");
                        }
                    } catch (Exception e) {
                        log.println("ERROR: A problem occurred while editing the entry");
                        err.println(e.getMessage());
                    }

                    // update the database
                    saveToDatabase(entry.getId(), tagMap);
                }
            }
        } catch (Exception e) {
            err.println(e.getMessage());
        }

        log.println("Checking if there is any new data on the network.");

        try {
            for (BigHash hash : hashesOnNetwork) {
                // do not add this data to the tags if it already exists in there
                if (hashesInTags.contains(hash)) {
                    continue;
                }
                // add this data to the tags db
                try {
                    // reset the meta data and the project file
                    MetaData md = null;
                    ProjectFile pf = null;

                    // set up the getfiletool
                    GetFileTool gft = new GetFileTool();
                    gft.setValidate(validate);
                    gft.setHash(hash);

                    // get the meta data
                    try {
                        md = gft.getMetaData();
                    } catch (CantVerifySignatureException e) {
                        addInvalid(-1, hash.toString(), "", "", "", "", e);
                        log.println("ERROR: Downloaded meta data is invalid.");
                    } catch (Exception e) {
                        if (e.getMessage() != null && e.getMessage().toLowerCase().contains("can't find metadata")) {
                            // note that this meta data is missing from the network
                            addMissing(-1, hash.toString(), "", "", "", "");
                            log.println("ERROR: Could not get meta data.");
                            continue;
                        } else {
                            addInvalid(-1, hash.toString(), "", "", "", "", e);
                            log.println("ERROR: Downloaded meta data is invalid.");
                            continue;
                        }
                    }

                    // if the tag entry doesnt exists, create a new one
                    long entryId = -1;
                    if (makeChanges) {
                        entryId = Database.createEntry();
                    }

                    // add all of the info as tags
                    if (makeChanges) {
                        Database.addTag(entryId, TagNames.HAS_DATA, "Yes");
                    }
                    addedTag(entryId, hash, TagNames.HAS_DATA, "Yes", false, false);
                    if (makeChanges) {
                        Database.addTag(entryId, TagNames.TYPE, "Data");
                    }
                    addedTag(entryId, hash, TagNames.TYPE, "Data", false, false);
                    if (makeChanges) {
                        Database.addTag(entryId, TagNames.TRANCHE_LINK + " 1", hash.toString());
                    }
                    addedTag(entryId, hash, TagNames.TRANCHE_LINK + " 1", hash.toString(), false, false);
                    if (makeChanges) {
                        Database.addTag(entryId, TagNames.TIMESTAMP, String.valueOf(md.getTimestamp()));
                    }
                    addedTag(entryId, hash, TagNames.TIMESTAMP, String.valueOf(md.getTimestamp()), false, false);
                    if (makeChanges) {
                        Database.addTag(entryId, TagNames.TRANCHE_TIMESTAMP + " 1", String.valueOf(md.getTimestamp()));
                    }
                    addedTag(entryId, hash, TagNames.TRANCHE_TIMESTAMP + " 1", String.valueOf(md.getTimestamp()), false, false);

                    String datePublished = null;
                    if (md != null) {
                        // set the new/old version
                        for (MetaDataAnnotation mda : md.getAnnotations()) {
                            if (mda.getName().equals(MetaDataAnnotation.PROP_DELETE_NEW_VERSION)) {
                                try {
                                    BigHash newVersionHash = BigHash.createHashFromString(mda.getValue());
                                    if (makeChanges) {
                                        Database.addTag(entryId, TagNames.TRANCHE_DELETE_NEW_LINK + " 1", newVersionHash.toString());
                                    }
                                    addedTag(entryId, hash, TagNames.TRANCHE_DELETE_NEW_LINK + " 1", newVersionHash.toString(), false, false);
                                } catch (Exception e) {
                                }
                            } else if (mda.getName().equals(MetaDataAnnotation.PROP_DELETE_OLD_VERSION)) {
                                try {
                                    BigHash oldVersionHash = BigHash.createHashFromString(mda.getValue());
                                    if (makeChanges) {
                                        Database.addTag(entryId, TagNames.TRANCHE_DELETE_OLD_LINK + " 1", oldVersionHash.toString());
                                    }
                                    addedTag(entryId, hash, TagNames.TRANCHE_DELETE_OLD_LINK + " 1", oldVersionHash.toString(), false, false);
                                } catch (Exception e) {
                                }
                            } else if (mda.getName().equals(MetaDataAnnotation.PROP_UNDELETED)) {
                                if (makeChanges) {
                                    Database.addTag(entryId, TagNames.TRANCHE_UNDELETED + " 1", mda.getValue());
                                }
                                addedTag(entryId, hash, TagNames.TRANCHE_UNDELETED + " 1", mda.getValue(), false, false);
                            } else if (mda.getName().equals(MetaDataAnnotation.PROP_NEW_VERSION)) {
                                try {
                                    BigHash newVersionHash = BigHash.createHashFromString(mda.getValue());
                                    if (makeChanges) {
                                        Database.addTag(entryId, TagNames.TRANCHE_NEW_LINK + " 1", newVersionHash.toString());
                                    }
                                    addedTag(entryId, hash, TagNames.TRANCHE_NEW_LINK + " 1", newVersionHash.toString(), false, false);
                                } catch (Exception e) {
                                }
                            } else if (mda.getName().equals(MetaDataAnnotation.PROP_OLD_VERSION)) {
                                try {
                                    BigHash oldVersionHash = BigHash.createHashFromString(mda.getValue());
                                    if (makeChanges) {
                                        Database.addTag(entryId, TagNames.TRANCHE_OLD_LINK + " 1", oldVersionHash.toString());
                                    }
                                    addedTag(entryId, hash, TagNames.TRANCHE_OLD_LINK + " 1", oldVersionHash.toString(), false, false);
                                } catch (Exception e) {
                                }
                            } else if (mda.getName().equals(MetaDataAnnotation.PROP_PUBLISHED_TIMESTAMP)) {
                                try {
                                    // date published
                                    datePublished = makeDate(Long.valueOf(mda.getValue()));
                                    if (makeChanges) {
                                        Database.addTag(entryId, TagNames.TRANCHE_DATE_PUBLISHED + " 1", datePublished);
                                    }
                                    addedTag(entryId, hash, TagNames.TRANCHE_DATE_PUBLISHED + " 1", datePublished, false, false);
                                } catch (Exception e) {
                                }
                            }
                        }

                        String dateUploaded = makeDate(md.getTimestamp());
                        if (makeChanges) {
                            Database.addTag(entryId, TagNames.TRANCHE_DATE_UPLOADED + " 1", dateUploaded);
                        }
                        addedTag(entryId, hash, TagNames.TRANCHE_DATE_UPLOADED + " 1", dateUploaded, false, false);

                        if (md.isEncrypted()) {
                            if (makeChanges) {
                                Database.addTag(entryId, TagNames.TRANCHE_ENCRYPTED + " 1", "True");
                            }
                            addedTag(entryId, hash, TagNames.TRANCHE_ENCRYPTED + " 1", "True", false, false);
                            if (makeChanges) {
                                Database.addTag(entryId, TagNames.TRANCHE_DATE_PUBLISHED + " 1", "Unknown");
                            }
                            addedTag(entryId, hash, TagNames.TRANCHE_DATE_PUBLISHED + " 1", "Unknown", false, false);
                            try {
                                String passphrase = Passphrases.getPassphrase(hash);
                                if (passphrase != null) {
                                    gft.setPassphrase(passphrase);
                                    log.println("Set the passphrase from the private passphrase db.");
                                }
                            } catch (Exception e) {
                            }
                        } else {
                            if (makeChanges) {
                                Database.addTag(entryId, TagNames.TRANCHE_ENCRYPTED + " 1", "False");
                            }
                            addedTag(entryId, hash, TagNames.TRANCHE_ENCRYPTED + " 1", "False", false, false);
                            if (datePublished == null) {
                                if (makeChanges) {
                                    Database.addTag(entryId, TagNames.TRANCHE_DATE_PUBLISHED + " 1", dateUploaded);
                                }
                                addedTag(entryId, hash, TagNames.TRANCHE_DATE_PUBLISHED + " 1", dateUploaded, false, false);
                            }
                        }

                        if (md.isDeleted()) {
                            MetaDataAnnotation deletedAnnotation = null;
                            for (MetaDataAnnotation mda : md.getAnnotations()) {
                                if (mda.getName().equals(MetaDataAnnotation.PROP_DELETED)) {
                                    try {
                                        if (deletedAnnotation == null || Long.valueOf(deletedAnnotation.getValue()) < Long.valueOf(mda.getValue())) {
                                            deletedAnnotation = mda;
                                        }
                                    } catch (Exception e) {
                                    }
                                }
                            }
                            if (deletedAnnotation != null) {
                                if (makeChanges) {
                                    Database.addTag(entryId, TagNames.TRANCHE_DELETED + " 1", deletedAnnotation.getValue());
                                }
                                addedTag(entryId, hash, TagNames.TRANCHE_DELETED + " 1", deletedAnnotation.getValue(), false, false);

                                // date deleted
                                try {
                                    String dateDeleted = makeDate(Long.valueOf(deletedAnnotation.getValue()));
                                    if (makeChanges) {
                                        Database.addTag(entryId, TagNames.TRANCHE_DATE_DELETED + " 1", dateDeleted);
                                    }
                                    addedTag(entryId, hash, TagNames.TRANCHE_DATE_DELETED + " 1", dateDeleted, false, false);
                                } catch (Exception e) {
                                }
                            }
                        }

                        String signatures = "";
                        for (Signature signature : md.getSignatures()) {
                            String signatureStr = signature.getCert().getSubjectDN().getName().split("CN=")[1].split(",")[0];
                            signatures = signatures + signatureStr + ", ";
                        }

                        if (signatures.length() != 0) {
                            signatures = signatures.substring(0, signatures.length() - 2);
                            if (makeChanges) {
                                Database.addTag(entryId, TagNames.TRANCHE_SIGNATURES + " 1", signatures);
                            }
                            addedTag(entryId, hash, TagNames.TRANCHE_SIGNATURES + " 1", signatures, false, false);
                        }

                        if (md.isProjectFile()) {
                            try {
                                if (md != null && md.isProjectFile()) {
                                    File tempFile = TempFileUtil.createTemporaryFile();
                                    try {
                                        // catch invalid downloads, throw otherwise
                                        try {
                                            gft.getFile(tempFile);
                                        } catch (CantVerifySignatureException e) {
                                            addInvalid(entryId, hash.toString(), "", "", "", "", e);
                                            log.println("ERROR: Downloaded project file is invalid.");
                                        } catch (Exception e) {
                                            if (e.getMessage() != null && (e.getMessage().toLowerCase().contains("validate") || e.getMessage().toLowerCase().contains("Decoded file does not match the expected file!"))) {
                                                addInvalid(entryId, hash.toString(), "", "", "", "", e);
                                                log.println("ERROR: Project file invalid.");
                                            } else {
                                                throw e;
                                            }
                                        }

                                        // treat it as if it is a project file
                                        FileInputStream fis = null;
                                        BufferedInputStream bis = null;
                                        try {
                                            fis = new FileInputStream(tempFile);
                                            bis = new BufferedInputStream(fis);
                                            pf = ProjectFileUtil.read(bis);
                                        } catch (Exception e) {
                                            log.println("ERROR: Project file invalid.");
                                            addInvalid(entryId, hash.toString(), "", "", "", "", e);
                                            bis.close();
                                            fis.close();
                                        }
                                    } finally {
                                        try {
                                            tempFile.delete();
                                        } catch (Exception e) {
                                            err.println(e.getMessage());
                                        }
                                    }
                                }

                                if (pf != null) {
                                    if (pf.getName() != null) {
                                        if (makeChanges) {
                                            Database.addTag(entryId, TagNames.TITLE, pf.getName());
                                        }
                                        addedTag(entryId, hash, TagNames.TITLE, pf.getName(), false, false);
                                    }
                                    if (pf.getDescription() != null) {
                                        if (makeChanges) {
                                            Database.addTag(entryId, TagNames.DESCRIPTION, pf.getDescription());
                                        }
                                        addedTag(entryId, hash, TagNames.DESCRIPTION, pf.getDescription(), false, false);
                                    }
                                    if (makeChanges) {
                                        Database.addTag(entryId, TagNames.TRANCHE_SIZE + " 1", pf.getSize().toString());
                                    }
                                    addedTag(entryId, hash, TagNames.TRANCHE_SIZE + " 1", pf.getSize().toString(), false, false);
                                    if (makeChanges) {
                                        Database.addTag(entryId, TagNames.TRANCHE_FILES + " 1", String.valueOf(pf.getParts().size()));
                                    }
                                    addedTag(entryId, hash, TagNames.TRANCHE_FILES + " 1", String.valueOf(pf.getParts().size()), false, false);
                                }
                            } catch (Exception e) {
                                log.println("ERROR: Could not load the project file.");
                                err.println(e.getMessage());
                            }
                        } else {
                            if (makeChanges) {
                                Database.addTag(entryId, TagNames.TITLE, md.getName());
                            }
                            addedTag(entryId, hash, TagNames.TITLE, md.getName(), false, false);
                            if (makeChanges) {
                                Database.addTag(entryId, TagNames.TRANCHE_FILES + " 1", "1");
                            }
                            addedTag(entryId, hash, TagNames.TRANCHE_FILES + " 1", "1", false, false);
                            if (makeChanges) {
                                Database.addTag(entryId, TagNames.TRANCHE_SIZE + " 1", String.valueOf(hash.getLength()));
                            }
                            addedTag(entryId, hash, TagNames.TRANCHE_SIZE + " 1", String.valueOf(hash.getLength()), false, false);
                        }
                    }
                    addedEntries.add(entryId);
                    hashesInTags.add(hash);
                } catch (Exception e) {
                    log.println("ERROR: There was a problem adding the new entry");
                    err.println(e.getMessage());
                }
            }
        } catch (Exception e) {
            log.println("ERROR: There was a problem checking for new data on the network.");
            err.println(e.getMessage());
        }

        log.println("Finished updating the tags database");
    }

    private void addMissing(long entryId, String hash, Map<String, List<String>> tagMap, int linkNum) {
        // log that this hash could not be found on the network
        String title = "", signatures = "", files = "", size = "";
        if (tagMap.get(TagNames.TITLE) != null) {
            title = tagMap.get(TagNames.TITLE).get(0);
            if (tagMap.get(TagNames.TRANCHE_LINK_NAME + " " + linkNum) != null) {
                title = title + ": " + tagMap.get(TagNames.TRANCHE_LINK_NAME + " " + linkNum).get(0);
            }
        }
        if (tagMap.get(TagNames.TRANCHE_SIGNATURES + " " + linkNum) != null) {
            signatures = tagMap.get(TagNames.TRANCHE_SIGNATURES + " " + linkNum).get(0);
        }
        if (tagMap.get(TagNames.TRANCHE_FILES + " " + linkNum) != null) {
            files = tagMap.get(TagNames.TRANCHE_FILES + " " + linkNum).get(0);
        }
        if (tagMap.get(TagNames.TRANCHE_SIZE + " " + linkNum) != null) {
            size = tagMap.get(TagNames.TRANCHE_SIZE + " " + linkNum).get(0);
        }
        addMissing(entryId, hash, title, signatures, files, size);
    }

    private void addMissing(long entryId, String hash, String title, String signatures, String files, String size) {
        missingProjects++;
        missingLog.println(entryId + ", " + hash.toString() + ", " + title + ", " + signatures + ", files: " + files + ", size: " + size);
    }

    private void addInvalid(long entryId, String hash, Map<String, List<String>> tagMap, int linkNum, Exception e) {
        // log that this hash could not be found on the network
        String title = "", signatures = "", files = "", size = "";
        if (tagMap.get(TagNames.TITLE) != null) {
            title = tagMap.get(TagNames.TITLE).get(0);
            if (tagMap.get(TagNames.TRANCHE_LINK_NAME + " " + linkNum) != null) {
                title = title + ": " + tagMap.get(TagNames.TRANCHE_LINK_NAME + " " + linkNum).get(0);
            }
        }
        if (tagMap.get(TagNames.TRANCHE_SIGNATURES + " " + linkNum) != null) {
            signatures = tagMap.get(TagNames.TRANCHE_SIGNATURES + " " + linkNum).get(0);
        }
        if (tagMap.get(TagNames.TRANCHE_FILES + " " + linkNum) != null) {
            files = tagMap.get(TagNames.TRANCHE_FILES + " " + linkNum).get(0);
        }
        if (tagMap.get(TagNames.TRANCHE_SIZE + " " + linkNum) != null) {
            size = tagMap.get(TagNames.TRANCHE_SIZE + " " + linkNum).get(0);
        }
        addInvalid(entryId, hash, title, signatures, files, size, e);
    }

    private void addInvalid(long entryId, String hash, String title, String signatures, String files, String size, Exception e) {
        String message = "";
        if (e.getMessage() != null) {
            message = e.getMessage().replace("\n", " ");
        }
        invalidProjects++;
        invalidLog.println(entryId + ", " + hash.toString() + ", " + title + ", " + signatures + ", files: " + files + ", size: " + size + ", message: " + message);
    }

    private void addedTag(long entryId, String tagName, String tagValue) {
        addedTagCount++;
        log.println("Added \"" + tagName + "\" tag with value \"" + tagValue + "\"");
        changesLog.println(entryId + ", , added tag, " + tagName + ", " + tagValue);
    }

    private void addedTag(long entryId, BigHash hash, String tagName, String tagValue, boolean isMetaDataAnnotation) {
        addedTag(entryId, hash, tagName, tagValue, isMetaDataAnnotation, true);
    }

    private void addedTag(long entryId, BigHash hash, String tagName, String tagValue, boolean isMetaDataAnnotation, boolean isOldEntry) {
        if (isOldEntry) {
            editedEntries.add(entryId);
        }

        addedTagCount++;
        if (isMetaDataAnnotation) {
            log.println("Added \"" + tagName + "\" meta data annotation with value \"" + tagValue + "\"");
            changesLog.println(entryId + ", " + hash.toString() + ", added meta data annotation, " + tagName + ", " + tagValue);
        } else {
            log.println("Added \"" + tagName + "\" tag with value \"" + tagValue + "\"");
            changesLog.println(entryId + ", " + hash.toString() + ", added tag, " + tagName + ", " + tagValue);
        }
    }

    private void editedTag(long entryId, String tagName, String from, String to) {
        editedTagCount++;
        log.println("Edited \"" + tagName + "\" tag from \"" + from + "\" to \"" + to + "\"");
        changesLog.println(entryId + ", , edited tag, " + tagName + ", " + from + ", " + to);
    }

    private void editedTag(long entryId, BigHash hash, String tagName, String from, String to, boolean isMetaDataAnnotation) {
        editedTag(entryId, hash, tagName, from, to, isMetaDataAnnotation, true);
    }

    private void editedTag(long entryId, BigHash hash, String tagName, String from, String to, boolean isMetaDataAnnotation, boolean isOldEntry) {
        if (isOldEntry) {
            editedEntries.add(entryId);
        }

        editedTagCount++;
        if (isMetaDataAnnotation) {
            log.println("Edited \"" + tagName + "\" meta data annotation from \"" + from + "\" to \"" + to + "\"");
            changesLog.println(entryId + ", " + hash.toString() + ", edited meta data annotation, " + tagName + ", " + from + ", " + to);
        } else {
            log.println("Edited \"" + tagName + "\" tag from \"" + from + "\" to \"" + to + "\"");
            changesLog.println(entryId + ", " + hash.toString() + ", edited tag, " + tagName + ", " + from + ", " + to);
        }
    }

    private void removedTag(long entryId, String tagName, String tagValue) {
        removedTagCount++;
        log.println("Removed \"" + tagName + "\" tag with the value \"" + tagValue + "\"");
        changesLog.println(entryId + ", , removed tag, " + tagName + ", " + tagValue);
    }

    private void removedTag(long entryId, BigHash hash, String tagName, String tagValue, boolean isMetaDataAnnotation) {
        removedTag(entryId, hash, tagName, tagValue, isMetaDataAnnotation, true);
    }

    private void removedTag(long entryId, BigHash hash, String tagName, String tagValue, boolean isMetaDataAnnotation, boolean isOldEntry) {
        if (isOldEntry) {
            editedEntries.add(entryId);
        }

        removedTagCount++;
        if (isMetaDataAnnotation) {
            log.println("Removed \"" + tagName + "\" meta data annotation with the value \"" + tagValue + "\"");
            changesLog.println(entryId + ", " + hash.toString() + ", removed meta data annotation, " + tagName + ", " + tagValue);
        } else {
            log.println("Removed \"" + tagName + "\" tag with the value \"" + tagValue + "\"");
            changesLog.println(entryId + ", " + hash.toString() + ", removed tag, " + tagName + ", " + tagValue);
        }
    }

    private void saveToDatabase(long entryId, Map<String, List<String>> tagMap) {
        try {
            if (editedEntries.contains(entryId) && makeChanges) {
                log.println("Making changes to the database.");

                // delete all the old tags
                Database.deleteTags(entryId);
                // add all the tags
                for (String tagName : tagMap.keySet()) {
                    for (String tagValue : tagMap.get(tagName)) {
                        Database.addTag(entryId, tagName, tagValue);
                    }
                }

                log.println("Changes made");
            }
        } catch (Exception e) {
            log.println("ERROR: There was a problem saving the changes.");
            err.println(e.getMessage());
        }
    }

    private void createCacheFile() throws Exception {

        log.println("Starting to create the cache file");

        // cache output location
        File uploadsCache = new File(publishDirectory, "uploads.cache");
        uploadsCache.createNewFile();
        PrintStream projectCacheWriter = new PrintStream(new FileOutputStream(uploadsCache));

        List<Entry> entries = Database.getEntries();
        Set<BigHash> cachedHashes = new HashSet<BigHash>();

        log.println(entries.size() + " entries with data found in the tags database");

        try {
            for (Entry entry : entries) {
                // create a data structure for the set of tags
                Map<String, List<String>> tagMap = makeTagMap(entry.getId());

                // get the number of links
                int links = getNumberOfLinks(tagMap);

                for (int linkNum = 1; linkNum <= links; linkNum++) {
                    // do not cache hashes that have already been cached
                    try {
                        if (cachedHashes.contains(BigHash.createHashFromString(tagMap.get(TagNames.TRANCHE_LINK + " " + linkNum).get(0)))) {
                            continue;
                        }
                    } catch (Exception e) {
                        continue;
                    }

                    try {
                        // Token separating project records
                        projectCacheWriter.println("-----");
                        projectCacheWriter.println("HASH: " + tagMap.get(TagNames.TRANCHE_LINK + " " + linkNum).get(0).trim());

                        try {
                            try {
                                // do not cache any more info about deleted data
                                if (tagMap.get(TagNames.TRANCHE_DELETED + " " + linkNum) != null) {
                                    projectCacheWriter.println("DELETED: true");
                                }
                            } catch (Exception e) {
                                err.println(e.getMessage());
                            }

                            try {
                                if (tagMap.get(TagNames.TRANCHE_ENCRYPTED + " " + linkNum) != null && tagMap.get(TagNames.TRANCHE_ENCRYPTED + " " + linkNum).size() > 0) {
                                    String encryptedStr = tagMap.get(TagNames.TRANCHE_ENCRYPTED + " " + linkNum).get(0).trim().toLowerCase();
                                    String publishedStr = "";
                                    if (tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum) != null && tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum).size() > 0) {
                                        publishedStr = tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " " + linkNum).get(0);
                                        if (!publishedStr.toLowerCase().equals("unknown")) {
                                            String dateNow = makeDate(System.currentTimeMillis());
                                            if (publishedStr.compareTo(dateNow) < 0 && encryptedStr.equals("true")) {
                                                encryptedStr = "published";
                                            }
                                        }
                                    }

                                    projectCacheWriter.println("ENCRYPTED: " + encryptedStr);
                                }
                            } catch (Exception e) {
                                err.println(e.getMessage());
                            }

                            try {
                                if (tagMap.get(TagNames.TRANCHE_LINK_NAME + " " + linkNum) != null) {
                                    projectCacheWriter.println("TITLE: " + removeHTMLTags(tagMap.get(TagNames.TITLE).get(0).trim()) + ": " + removeHTMLTags(tagMap.get(TagNames.TRANCHE_LINK_NAME + " " + linkNum).get(0).trim()));
                                } else if (tagMap.get(TagNames.TITLE) != null) {
                                    projectCacheWriter.println("TITLE: " + removeHTMLTags(tagMap.get(TagNames.TITLE).get(0).trim()));
                                }

                            } catch (Exception e) {
                                err.println(e.getMessage());
                            }

                            try {
                                if (tagMap.get(TagNames.TRANCHE_DESCRIPTION + " " + linkNum) != null) {
                                    projectCacheWriter.println("DESC: " + removeHTMLTags(tagMap.get(TagNames.TRANCHE_DESCRIPTION + " " + linkNum).get(0).trim().replaceAll("\n", "\\n")));
                                } else if (tagMap.get(TagNames.DESCRIPTION) != null) {
                                    projectCacheWriter.println("DESC: " + removeHTMLTags(tagMap.get(TagNames.DESCRIPTION).get(0).trim().replaceAll("\n", "\\n")));
                                }
                            } catch (Exception e) {
                            }

                            try {
                                if (tagMap.get(TagNames.TRANCHE_SIZE + " " + linkNum) != null) {
                                    projectCacheWriter.println("SIZE: " + tagMap.get(TagNames.TRANCHE_SIZE + " " + linkNum).get(0).trim());
                                }
                            } catch (Exception e) {
                                err.println(e.getMessage());
                            }

                            try {
                                if (tagMap.get(TagNames.TRANCHE_FILES + " " + linkNum) != null) {
                                    projectCacheWriter.println("FILES: " + tagMap.get(TagNames.TRANCHE_FILES + " " + linkNum).get(0).trim());
                                }
                            } catch (Exception e) {
                                err.println(e.getMessage());
                            }

                            try {
                                if (tagMap.get(TagNames.TRANCHE_TIMESTAMP + " " + linkNum) != null) {
                                    projectCacheWriter.println("DATE: " + tagMap.get(TagNames.TRANCHE_TIMESTAMP + " " + linkNum).get(0).trim());
                                } else if (tagMap.get(TagNames.TIMESTAMP) != null) {
                                    projectCacheWriter.println("DATE: " + tagMap.get(TagNames.TIMESTAMP).get(0).trim());
                                }
                            } catch (Exception e) {
                                err.println(e.getMessage());
                            }

                            try {
                                if (tagMap.get(TagNames.TRANCHE_SIGNATURES + " " + linkNum) != null) {
                                    String signatures = tagMap.get(TagNames.TRANCHE_SIGNATURES + " " + linkNum).get(0).trim().replaceAll(", ", "||||");
                                    if (signatures.endsWith(",")) {
                                        signatures = signatures.substring(0, signatures.length() - 1);
                                    }

                                    projectCacheWriter.println("SIGS: " + signatures);
                                }
                            } catch (Exception e) {
                                err.println(e.getMessage());
                            }

                            try {
                                if (tagMap.get(TagNames.TRANCHE_OLD_LINK + " " + linkNum) != null) {
                                    projectCacheWriter.println("OLD: " + tagMap.get(TagNames.TRANCHE_OLD_LINK + " " + linkNum).get(0).trim());
                                }
                            } catch (Exception e) {
                                err.println(e.getMessage());
                            }

                            try {
                                if (tagMap.get(TagNames.TRANCHE_NEW_LINK + " " + linkNum) != null) {
                                    projectCacheWriter.println("NEW: " + tagMap.get(TagNames.TRANCHE_NEW_LINK + " " + linkNum).get(0).trim());
                                }
                            } catch (Exception e) {
                                err.println(e.getMessage());
                            }

                            try {
                                if (tagMap.get(TagNames.TRANCHE_TYPE + " " + linkNum) != null) {
                                    projectCacheWriter.println("TYPE: " + tagMap.get(TagNames.TRANCHE_TYPE + " " + linkNum).get(0).trim());
                                }
                            } catch (Exception e) {
                                err.println(e.getMessage());
                            }

                            try {
                                if (tagMap.get(TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum) != null) {
                                    projectCacheWriter.println("SHARE MD IF ENC: " + tagMap.get(TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum).get(0).trim());
                                }
                            } catch (Exception e) {
                                err.println(e.getMessage());
                            }

                        } finally {
                            cachedHashes.add(BigHash.createHashFromString(tagMap.get(TagNames.TRANCHE_LINK + " " + linkNum).get(0).trim()));
                            projectsInCache++;
                        }
                    } catch (Exception e) {
                        log.println("ERROR: There was a problem caching " + tagMap.get(TagNames.TRANCHE_LINK + " " + linkNum).get(0).trim());
                        err.println(e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            err.println(e.getMessage());
        } finally {
            log.println("Finished creating the cache");
            log.println(projectsInCache + " added to the cache");
        }

    }

    private void publishCacheFile() throws Exception {

        log.println("Starting to publish the cache file");

        // upload the cache file
        BigHash cacheHash = null, prevCacheHash = null;
        try {
            // check user
            if (user == null) {
                log.println("User is null.");
            }
            if (user.getCertificate() == null) {
                log.println("User certificate is null.");
            }
            if (user.getPrivateKey() == null) {
                log.println("User's private key is null.");
            }

            AddFileTool aft = new AddFileTool(user.getCertificate(), user.getPrivateKey());
            aft.setServersToUploadTo(4);
            aft.setSingleFileUpload(true);
            aft.setSkipExistingChunk(false);
            aft.setShowUploadSummary(true);
            aft.setUseRemoteReplication(false);
            aft.setMaxRetries(5);
            aft.setTitle("Upload Cache " + Text.getWeekdayAndHour(System.currentTimeMillis()));
            aft.setDescription("Upload Cache " + Text.getWeekdayAndHour(System.currentTimeMillis()));

            if (publishDirectory == null) {
                log.println("Publish directory is null.");
            }
            File cacheFile = new File(publishDirectory, "uploads.cache");
            if (cacheFile == null) {
                log.println("Cache file is null.");
            }

            cacheHash = aft.addFile(cacheFile);

            if (cacheHash == null) {
                log.println("Cache hash returned is null.");
            }

            log.println("New cache hash is " + cacheHash.toString());
        } catch (Exception e) {
            log.println("ERROR: Could not upload file.");
            err.println(e.getMessage());
            throw e;
        }

// Set new version for previous cache
        try {
            log.println("Trying to set the new cache version.");
            prevCacheHash = ProjectCache.getNewestProjectCacheHash();

            // make sure this new hash is not the same
            if (prevCacheHash.toString().equals(cacheHash.toString())) {
                return;
            }

            for (String url : servers) {
                try {
                    // connect to the server
                    TrancheServer ts = IOUtil.connect(url);

                    try {
                        // make sure this server has the meta data
                        if (!ts.hasMetaData(prevCacheHash)) {
                            continue;
                        }

                        // set up the getfiletool
                        GetFileTool gft = new GetFileTool();
                        gft.setValidate(false);
                        gft.setHash(prevCacheHash);

                        // use only this server
                        List<String> serversToUse = new ArrayList<String>();
                        serversToUse.add(url);
                        gft.setServersToUse(serversToUse);

                        try {
                            // get the meta data
                            MetaData prevMD = gft.getMetaData();

                            // create and add the new version annotation
                            MetaDataAnnotation prevMDA = new MetaDataAnnotation(MetaDataAnnotation.PROP_NEW_VERSION, cacheHash.toString());
                            prevMD.addAnnotation(prevMDA);

                            // create the bytestream
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            // turn the metaData into a byte stream
                            MetaDataUtil.write(prevMD, baos);

                            try {
                                // set the meta data back to the server - try 3 times
                                Exception ex = null;
                                for (int i = 0; i < 3; i++) {
                                    try {
                                        IOUtil.setMetaData(ts, user.getCertificate(), user.getPrivateKey(), prevCacheHash, baos.toByteArray());
                                        log.println("Set meta data to " + url);
                                        ex = null;
                                        break;
                                    } catch (Exception e) {
                                        ex = e;
                                        continue;
                                    }
                                }
                                if (ex != null) {
                                    throw ex;
                                }
                            } catch (Exception e) {
                                log.println("ERROR: Could not set meta data to " + url);
                                err.println(e.getMessage());
                            }
                        } catch (Exception e) {
                            log.println("ERROR: Could not get meta data.");
                            err.println(e.getMessage());
                        }
                    } finally {
                        IOUtil.safeClose(ts);
                    }
                } catch (Exception e) {
                    log.println("ERROR: Could not connect to " + url);
                    err.println(e.getMessage());
                }
            }
        } catch (Exception e) {
            log.println("ERROR: Could not edit previous cache meta data.");
            err.println(e.getMessage());
            throw e;
        }

        // Set old version for new cache
        try {
            log.println("Trying to set the old cache version.");
            for (String url : servers) {
                try {
                    // connect to the server
                    TrancheServer ts = IOUtil.connect(url);

                    try {
                        // make sure this server has the meta data
                        if (!ts.hasMetaData(cacheHash)) {
                            continue;
                        }

                        // set up the getfiletool
                        GetFileTool gft = new GetFileTool();
                        gft.setValidate(false);
                        gft.setHash(cacheHash);

                        // use only this server
                        List<String> serversToUse = new ArrayList<String>();
                        serversToUse.add(url);
                        gft.setServersToUse(serversToUse);

                        try {
                            // get the meta data
                            MetaData newMD = gft.getMetaData();
                            try {
                                // create and add the previous version annotation
                                MetaDataAnnotation newMDA = new MetaDataAnnotation(MetaDataAnnotation.PROP_OLD_VERSION, prevCacheHash.toString());
                                newMD.addAnnotation(newMDA);

                                // create the bytestream
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                // turn the metaData into a byte stream
                                MetaDataUtil.write(newMD, baos);

                                // set the meta data back to the server - try 3 times
                                Exception ex = null;
                                for (int i = 0; i < 3; i++) {
                                    try {
                                        IOUtil.setMetaData(ts, user.getCertificate(), user.getPrivateKey(), cacheHash, baos.toByteArray());
                                        log.println("Set meta data to " + url);
                                        ex = null;
                                        break;
                                    } catch (Exception e) {
                                        ex = e;
                                        continue;
                                    }
                                }
                                if (ex != null) {
                                    throw ex;
                                }
                            } catch (Exception e) {
                                log.println("ERROR: Could not set meta data to " + url);
                                err.println(e.getMessage());
                            }
                        } catch (Exception e) {
                            log.println("ERROR: Could not get meta data.");
                            err.println(e.getMessage());
                        }
                    } finally {
                        IOUtil.safeClose(ts);
                    }
                } catch (Exception e) {
                    log.println("ERROR: Could not connect to " + url);
                    err.println(e.getMessage());
                }
            }
        } catch (Exception e) {
            log.println("ERROR: Could not edit new cache meta data.");
            err.println(e.getMessage());
        }
        log.println("Finished publishing cache file");
    }

    private void indexTagsDatabase() {
        log.println("Starting to index the tags database.");

        IndexWriter writer = null;
        try {
            // get all annotation items
            List<Entry> entries = Database.getEntries(TagNames.TYPE, "Data");
            log.println("Entries to parse: " + entries.size());

            // make the index directory under "WEB-INF"
            File indexDirectory = new File("/opt/tomcat5/webapps/proteomecommons/WEB-INF/data/index/");
            indexDirectory.mkdirs();

            // make a new index writer
            writer = new IndexWriter(indexDirectory, new StandardAnalyzer(), true);

            // use up the stack
            for (Entry entry : entries) {
                try {
                    log.println("Indexing entry " + entry.getId());

                    // the URL to get data from
                    String url = "http://www.proteomecommons.org/data/show.jsp?id=" + entry.getId();

                    // parse and index
                    HttpClient client = new HttpClient();

                    // make a get method and get the page
                    GetMethod gm = new GetMethod(url);
                    client.executeMethod(gm);
                    String content = gm.getResponseBodyAsString();

                    // create a data structure for the set of tags
                    Map<String, List<String>> tagMap = makeTagMap(entry.getId());

                    // get the number of links
                    int links = getNumberOfLinks(tagMap);

                    // parse if the project was deleted
                    String deleted = "";
                    if (links == 1 && tagMap.get(TagNames.TRANCHE_DELETED + " 1") != null) {
                        deleted = "This project was deleted on " + new Date(Long.valueOf(tagMap.get(TagNames.TRANCHE_DELETED + " 1").get(0))).toGMTString().substring(0, 11) + ".";
                    }

                    // parse the title
                    String title = "*** No Title ***";
                    if (tagMap.get(TagNames.TITLE) != null) {
                        title = tagMap.get(TagNames.TITLE).get(0);
                    }
                    title = title + " (<a href=\"http://www.proteomecommons.org/tags/ProteomeCommons.org-Tags.jsp?data=http://www.proteomecommons.org/tags/tags.jsp?id=" + entry.getId() + "\">Edit This Page</a>)";
                    if (links == 1 && tagMap.get(TagNames.TRANCHE_PASSPHRASE + " 1") == null && tagMap.get(TagNames.TRANCHE_LINK + " 1") != null && tagMap.get(TagNames.TRANCHE_ENCRYPTED + " 1") != null && tagMap.get(TagNames.TRANCHE_ENCRYPTED + " 1").get(0).equals("True")) {
                        title = title + "&nbsp;(<a href=\"http://www.proteomecommons.org/data/publishPassphrase.jsp?hash=" + tagMap.get(TagNames.TRANCHE_LINK + " 1").get(0).replace("+", "%2B") + "\">Publish Passphrase</a>)";
                    }
                    if (links == 1 && tagMap.get(TagNames.TRANCHE_DELETED + " 1") == null && tagMap.get(TagNames.TRANCHE_LINK + " 1") != null) {
                        title = title + "&nbsp;(<a href=\"http://www.proteomecommons.org/data/delete.jsp?hash=" + tagMap.get(TagNames.TRANCHE_LINK + " 1").get(0).replace("+", "%2B") + "\">Delete</a>)";
                    }

                    // parse the contributors
                    String authors = "";

                    // parse the pubmed id
                    String description = "";
                    if (tagMap.get(TagNames.DESCRIPTION) != null) {
                        description = description + tagMap.get(TagNames.DESCRIPTION).get(0);
                        if (description.length() > 300) {
                            description = description.substring(0, 300) + " <a href=\"" + url + "\">[Click for rest...]</a>";
                        }
                    }

                    // parse the instrument
                    String massSpec = "";
                    if (tagMap.get("Instrument Name") != null) {
                        massSpec = tagMap.get("Instrument Name").get(0);
                    }

                    // parse the organism
                    String organism = "";
                    if (tagMap.get("Organism") != null) {
                        organism = tagMap.get("Organism").get(0);
                    }

                    // the dfs link
                    String tranche = "";
                    if (tagMap.get(TagNames.TRANCHE_LINK + " 1") != null) {
                        tranche = tagMap.get(TagNames.TRANCHE_LINK + " 1").get(0);
                    }

                    // the journal's name
                    String journal = "";
                    if (tagMap.get("Journal") != null) {
                        journal = tagMap.get("Journal").get(0);
                    }

                    // Determine project status
                    String status = "public";
                    // Do a lookup for isEncrypted and Passphrase.
                    if (tagMap.get(TagNames.TRANCHE_ENCRYPTED + " 1") != null) {
                        if (!tagMap.get(TagNames.TRANCHE_ENCRYPTED + " 1").get(0).equals("False")) {
                            status = "encrypted";
                            // the project is encrypted unless the "Tranche:Date Published" tag is a future one
                            String currentDate = makeDate(System.currentTimeMillis());
                            if (tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " 1") != null) {
                                if (tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " 1").get(0).compareTo(currentDate) > 0 && !tagMap.get(TagNames.TRANCHE_DATE_PUBLISHED + " 1").get(0).equals("Unknown")) {
                                    status = "pending";
                                }

                            }
                        }
                    }

                    List<String> trancheTypes = new ArrayList();

                    for (Tag t : Database.getTags(entry.getId())) {
                        if (t.getName().startsWith(TagNames.TRANCHE_TYPE)) {
                            trancheTypes.add(t.getValue());
                        } else {
                            // Nothing, burned
                        }

                    }

                    // make a document for the field parts
                    Document doc = new Document();
                    doc.add(Field.Text("contents", content));
                    doc.add(Field.Text("organism", organism));
                    doc.add(Field.Text("massSpec", massSpec));
                    doc.add(Field.Text("tranche", tranche));
                    doc.add(Field.Text("journal", journal));
                    // four critical parts
                    doc.add(Field.UnIndexed("link", url));
                    doc.add(Field.UnIndexed("description", description));
                    doc.add(Field.UnIndexed("authors", authors));
                    doc.add(Field.UnIndexed("title", title));
                    doc.add(Field.UnIndexed("deleted", deleted));
                    // new fields for project subsites
                    doc.add(Field.Text("status", status));
                    for (String trancheType : trancheTypes) {
                        doc.add(Field.Text("trancheType", trancheType));
                    }

                    // add the document
                    writer.addDocument(doc);
                } catch (Exception e) {
                    log.println("ERROR: Problem indexing tags.");
                    err.println(e.getMessage());
                }
            }
        } catch (Exception e) {
            log.println("ERROR: Could not index any tags.");
            err.println(e.getMessage());
        } finally {
            try {
                // close that writer
                writer.optimize();
                writer.close();
            } catch (Exception e) {
            }
        }
        log.println("Finished the index of the tags database.");
    }

    private void printReportOfAction() {
        log.println((stop - start) + " milliseconds elapsed.");
        log.println(servers.size() + " servers checked for projects.");
        log.println(hashesOnNetwork.size() + " hashes found on the network.");
        log.println(addedEntries.size() + " entries added to the tags database.");
        log.println(editedEntries.size() + " entries edited within the tags database.");
        log.println(addedTagCount + " tags added to the tags database.");
        log.println(editedTagCount + " tags edited within the tags database.");
        log.println(removedTagCount + " sets of tags removed from the tags database.");
        log.println(chunkAndMetaCount + " chunks and meta data on the network.");
        log.println(projectsInCache + " entries added to the cache.");
        log.println(missingProjects + " projects missing from the Tranche network.");

    }

    private void printFileTypeLog() {
        try {
            File fileTypeLogFile = new File(workingDirectory, "filetype.log");
            if (fileTypeLogFile.exists()) {
                fileTypeLogFile.delete();
            }
            if (!fileTypeLogFile.createNewFile()) {
                throw new RuntimeException("There was a problem creating the file type log file.");
            }
            PrintStream fileTypeLog = new PrintStream(new FileOutputStream(fileTypeLogFile));

            try {
                fileTypeLog.println("filetype\tfiles\tsize\tfileshr\tsizehr");
                for (String fileType : numFilesFileTypeMap.keySet()) {
                    try {
                        String filesHR = GUIUtil.integerFormat.format(numFilesFileTypeMap.get(fileType).longValue()), sizeHR = Text.getFormattedBytes(sizeFileTypeMap.get(fileType).longValue());
                        fileTypeLog.println(fileType + "\t" + numFilesFileTypeMap.get(fileType).toString() + "\t" + sizeFileTypeMap.get(fileType).toString() + "\t" + filesHR + "\t" + sizeHR);
                    } catch (Exception e) {
                        // noop
                    }
                }
            } finally {
                fileTypeLog.flush();
                fileTypeLog.close();
            }
        } catch (Exception e) {
            log.println("ERROR: Could not print file type log.");
            err.println(e.getMessage());
        }
    }

    public void execute() {
        // first and foremost, make sure the proteomecommons tranche configuration is loaded
        ProteomeCommonsTrancheConfig.load();
        
        // set up directories
        if (!workingDirectory.exists()) {
            workingDirectory.mkdirs();
        }

        if (!publishDirectory.exists()) {
            publishDirectory.mkdirs();
        }

        PrintStream detailsLog = null, errorLog = null, generalLog = null, stdOutLog = null, stdErrLog = null;
        try {
            // create the log files
            File detailsLogFile = new File(workingDirectory, "details.log");
            if (!detailsLogFile.createNewFile()) {
                throw new RuntimeException("There was a problem creating the detailed log file.");
            }
            detailsLog = new PrintStream(new FileOutputStream(detailsLogFile));

            File errorLogFile = new File(workingDirectory, "errors.log");
            if (!errorLogFile.createNewFile()) {
                throw new RuntimeException("There was a problem creating the error log file.");
            }
            errorLog = new PrintStream(new FileOutputStream(errorLogFile));

            File generalLogFile = new File(workingDirectory, "general.log");
            if (!generalLogFile.createNewFile()) {
                throw new RuntimeException("There was a problem creating the general log file.");
            }
            generalLog = new PrintStream(new FileOutputStream(generalLogFile));

            File changesLogFile = new File(workingDirectory, "changes.log");
            if (!changesLogFile.createNewFile()) {
                throw new RuntimeException("There was a problem creating the changes log file.");
            }
            changesLog = new PrintStream(new FileOutputStream(changesLogFile));

            File missingLogFile = new File(workingDirectory, "missing.log");
            if (!missingLogFile.createNewFile()) {
                throw new RuntimeException("There was a problem creating the missing log file.");
            }
            missingLog = new PrintStream(new FileOutputStream(missingLogFile));

            File invalidLogFile = new File(workingDirectory, "invalid.log");
            if (!invalidLogFile.createNewFile()) {
                throw new RuntimeException("There was a problem creating the invalid log file.");
            }
            invalidLog = new PrintStream(new FileOutputStream(invalidLogFile));

            File stdOutFile = new File(workingDirectory, "stdout.log");
            if (!stdOutFile.createNewFile()) {
                throw new RuntimeException("There was a problem creating the standard out log file.");
            }
            stdOutLog = new PrintStream(new FileOutputStream(stdOutFile));

            File stdErrFile = new File(workingDirectory, "stderr.log");
            if (!stdErrFile.createNewFile()) {
                throw new RuntimeException("There was a problem creating the standard error log file.");
            }
            stdErrLog = new PrintStream(new FileOutputStream(stdErrFile));

            // change standard out and err
            System.setOut(stdOutLog);
            System.setErr(stdErrLog);

            // set output to the detailed log
            log = detailsLog;
            err = errorLog;

            // run the cache updater
            start = System.currentTimeMillis();

            if (updateTagsDatabase || makeNewCache) {
                populateHashesSet();
            }
            if (updateTagsDatabase) {
                updateTagsDatabase();
            }
            if (makeNewCache) {
                createCacheFile();
                if (makeChanges) {
                    publishCacheFile();
                }
            }
            if (indexTagsDatabase) {
                indexTagsDatabase();
            }
        } catch (Exception e) {
            log.println("ERROR: Fatal error. Program terminating.");
            err.println(e.getMessage());
            Thread t = new Thread() {

                public void run() {
                    try {
                        EmailUtil.sendEmail("FATAL ERROR: Cache Updater", new String[]{"augman85@gmail.com", "jfalkner@umich.edu", "bryanesmith@gmail.com"}, "A fatal error occurred on the cache updater. Check the logs on the proteomecommons.org server under \"" + workingDirectory.getAbsolutePath() + "\" for more information.");
                    } catch (Exception e) {
                    }
                }
            };
            t.start();
        } finally {
            stop = System.currentTimeMillis();

            detailsLog.flush();
            detailsLog.close();

            printFileTypeLog();

            log = generalLog;
            printReportOfAction();
            generalLog.flush();
            generalLog.close();

            changesLog.flush();
            changesLog.close();
            missingLog.flush();
            missingLog.close();
            invalidLog.flush();
            invalidLog.close();
            errorLog.flush();
            errorLog.close();
            stdOutLog.flush();
            stdOutLog.close();
            stdErrLog.flush();
            stdErrLog.close();

            System.out.println("*** FINISHED BUILDING " + Text.getFormattedDate(System.currentTimeMillis()) + " ***");
        }
    }

    /**
     * Helper method to remove all HTML tags from a string
     */
    private String removeHTMLTags(String string) {
        while (string.indexOf("<") != -1 && string.indexOf(">") > string.indexOf("<")) {
            string = string.substring(0, string.indexOf("<")) + string.substring(string.indexOf(">") + 1);
        }
        return string;
    }

    private boolean metaDataContainsAnnotation(BigHash hash, String name, String value) throws Exception {
        GetFileTool gft = new GetFileTool();
        gft.setHash(hash);
        gft.setValidate(false);
        MetaData md = gft.getMetaData();
        for (MetaDataAnnotation mda : md.getAnnotations()) {
            if (mda.getName().equals(name) && mda.getValue().equals(value)) {
                return true;
            }
        }
        return false;
    }

    private void addMetaDataAnnotationNow(BigHash hash, String name, String value) throws Exception {
        if (makeChanges) {
            log.println("Publishing meta data with added annotation.");

            GetFileTool gft = new GetFileTool();
            gft.setHash(hash);
            gft.setValidate(false);
            MetaData md = gft.getMetaData();
            md.addAnnotation(new MetaDataAnnotation(name, value));

            // create the bytestream
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // turn the metaData into a byte stream
            MetaDataUtil.write(md, baos);

            for (String url : servers) {
                try {
                    // connect
                    TrancheServer ts = IOUtil.connect(url);
                    try {
                        if (ts.hasMetaData(hash)) {
                            // upload the changes - try up to 3 times
                            Exception ex = null;
                            for (int i = 0; i < 3; i++) {
                                try {
                                    IOUtil.setMetaData(ts, user.getCertificate(), user.getPrivateKey(), hash, baos.toByteArray());
                                    log.println("Set meta data to " + url);
                                    ex = null;
                                    break;
                                } catch (Exception e) {
                                    ex = e;
                                    continue;
                                }
                            }
                            if (ex != null) {
                                throw ex;
                            }
                        }
                    } finally {
                        IOUtil.safeClose(ts);
                    }
                } catch (Exception e) {
                    err.println(e.getMessage());
                    log.println("ERROR: Could not set meta data to " + url);
                }
            }

            log.println("Done publishing meta data");
        }
    }

    private String makeDate(long timestamp) {
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMinimumIntegerDigits(2);
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(timestamp);
        return calendar.get(calendar.YEAR) + "/" + nf.format(calendar.get(calendar.MONTH) + 1) + "/" + nf.format(calendar.get(calendar.DAY_OF_MONTH)) + " " + nf.format(calendar.get(calendar.HOUR_OF_DAY)) + ":" + nf.format(calendar.get(calendar.MINUTE)) + ":" + nf.format(calendar.get(calendar.SECOND));
    }

    private Map<String, List<String>> makeTagMap(long entryID) throws Exception {
        // create a data structure for the set of tags
        List<Tag> tags = Database.getTags(entryID);
        Map<String, List<String>> tagMap = new HashMap<String, List<String>>();
        for (Tag tag : tags) {
            // get the old value
            List<String> values = tagMap.get(tag.getName());
            // if there was no prior value, make a new list
            if (values == null) {
                values = new ArrayList<String>();
            }
            // add the value
            values.add(tag.getValue());
            tagMap.put(tag.getName(), values);
        }
        return tagMap;
    }

    private int getNumberOfLinks(Map<String, List<String>> tagMap) {
        // get the number of links
        int links = 0;
        for (String name : tagMap.keySet()) {
            if (name.startsWith(TagNames.TRANCHE_LINK)) {
                // if there is more to the tag name than the one we are looking for (there should be a number)
                if (name.length() > TagNames.TRANCHE_LINK.length()) {
                    // do not increment tags if what is left is not a number
                    try {
                        Long.valueOf(name.substring(TagNames.TRANCHE_LINK.length()).trim());
                        links++;
                    } catch (Exception e) {
                    }
                }
            }
        }
        return links;
    }
}
