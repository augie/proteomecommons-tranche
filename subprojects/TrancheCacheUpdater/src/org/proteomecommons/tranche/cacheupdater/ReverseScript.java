package org.proteomecommons.tranche.cacheupdater;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.proteomecommons.tags.Database;
import org.proteomecommons.tags.Entry;
import org.proteomecommons.tags.Tag;
import org.proteomecommons.tags.TagNames;
import org.tranche.hash.BigHash;
import org.tranche.util.IOUtil;

/**
 *
 * @author James "Augie" Hill - augie@828productions.com
 */
public class ReverseScript {

    public static void runOn(final File file, final File logFile) throws Exception {

        Thread t = new Thread() {

            public void run() {
                PrintStream log = null;
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(logFile);
                    log = new PrintStream(fos);
                    log.println("Starting to reverse engineer the uploads cache named " + file.getName());

                    FileReader fr = null;
                    BufferedReader br = null;
                    try {
                        fr = new FileReader(file);
                        br = new BufferedReader(fr);
                        log.println("Opened the uploads cache.");

                        try {
                            // throw away the first line
                            br.readLine();
                            // cycle through the lines of text
                            while (br.ready()) {
                                log.println("Text available to be read from the file.");
                                try {
                                    // read a new line
                                    String line = br.readLine();
                                    if (line == null) {
                                        throw new RuntimeException("Read a null line out of the file.");
                                    }
                                    line = line.trim();

                                    // make sure the line read is the hash
                                    if (!line.startsWith("HASH: ")) {
                                        throw new RuntimeException("Expected a hash as the first line of the new entry.");
                                    }

                                    // that first line should be the hash
                                    String hash = line.replaceFirst("HASH: ", "");
                                    try {
                                        BigHash.createHashFromString(hash);
                                    } catch (Exception e) {
                                        throw new RuntimeException("Given hash is not valid.");
                                    }
                                    log.println("Working with hash: " + hash);

                                    // read all the information about this entry
                                    String title = null, description = null, newVersion = null, oldVersion = null, type = null, encrypted = null, size = null, files = null, date = null, sigs = null, deleted = null, share = null;
                                    while (br.ready()) {
                                        line = br.readLine();
                                        if (line == null || line.equals("") || line.equals("-----")) {
                                            break;
                                        }
                                        line = line.trim();

                                        if (line.startsWith("DELETED: ")) {
                                            deleted = line.replaceFirst("DELETED: ", "");
                                            log.println("Deleted found with a value of \"" + deleted + "\".");
                                        }
                                        if (line.startsWith("ENCRYPTED: ")) {
                                            encrypted = line.replaceFirst("ENCRYPTED: ", "");
                                            log.println("Encrypted found with a value of \"" + encrypted + "\".");
                                        }
                                        if (line.startsWith("TITLE: ")) {
                                            title = line.replaceFirst("TITLE: ", "");
                                            log.println("Title found with a value of \"" + title + "\".");
                                        }
                                        if (line.startsWith("DESC: ")) {
                                            description = line.replaceFirst("DESC: ", "");
                                            log.println("Description found with a value of \"" + description + "\".");
                                        }
                                        if (line.startsWith("SIZE: ")) {
                                            size = line.replaceFirst("SIZE: ", "");
                                            log.println("Size found with a value of \"" + size + "\".");
                                        }
                                        if (line.startsWith("FILES: ")) {
                                            files = line.replaceFirst("FILES: ", "");
                                            log.println("Files found with a value of \"" + files + "\".");
                                        }
                                        if (line.startsWith("DATE: ")) {
                                            date = line.replaceFirst("DATE: ", "");
                                            log.println("Date found with a value of \"" + date + "\".");
                                        }
                                        if (line.startsWith("SIGS: ")) {
                                            sigs = line.replaceFirst("SIGS: ", "");
                                            log.println("Signatures found with a value of \"" + sigs + "\".");
                                            if (sigs.contains("||||")) {
                                                sigs = sigs.replaceAll("||||", ", ");
                                                log.println("Changed signatures to \"" + sigs + "\".");
                                            }
                                        }
                                        if (line.startsWith("OLD: ")) {
                                            oldVersion = line.replaceFirst("OLD: ", "");
                                            log.println("Old Version found with a value of \"" + oldVersion + "\".");
                                        }
                                        if (line.startsWith("NEW: ")) {
                                            newVersion = line.replaceFirst("NEW: ", "");
                                            log.println("New Version found with a value of \"" + newVersion + "\".");
                                        }
                                        if (line.startsWith("TYPE: ")) {
                                            type = line.replaceFirst("TYPE: ", "");
                                            log.println("Type found with a value of \"" + type + "\".");
                                        }
                                        if (line.startsWith("SHARE MD IF ENC: ")) {
                                            share = line.replaceFirst("SHARE MD IF ENC: ", "");
                                            log.println("Share meta data if encrypted found with a value of \"" + share + "\".");
                                        }
                                    }

                                    // got all the info we're going to get, do something with it
                                    log.println("Checking against tags database.");
                                    List<Entry> entries = Database.getEntries(TagNames.TRANCHE_LINK + "%", hash);
                                    log.println(entries.size() + " entries found with the same Tranche Link.");
                                    if (entries.size() > 0) {
                                        for (Entry entry : entries) {
                                            try {
                                                log.println("Checking info against entry #" + entry.getId());
                                                // make a tag map
                                                List<Tag> tags = Database.getTags(entry.getId());
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
                                                log.println("Made a tag map.");

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
                                                log.println("There are " + links + " links in this entry.");

                                                // get the link number
                                                int linkNum = 1;
                                                for (int i = linkNum; i <= links; i++) {
                                                    try {
                                                        if (tagMap.get(TagNames.TRANCHE_LINK + " " + i).get(0).equals(hash)) {
                                                            linkNum = i;
                                                            break;
                                                        }
                                                    } catch (Exception e) {
                                                        throw new RuntimeException("Could not find the link number for the hash.");
                                                    }
                                                }
                                                log.println("Hash is link #" + linkNum + " in this entry.");

                                                // if there is no title, add it
                                                if (tagMap.get(TagNames.TITLE) == null && title != null) {
                                                    Database.addTag(entry.getId(), TagNames.TITLE, title);
                                                    log.println("Added new tag with name \"" + TagNames.TITLE + "\" and value \"" + title + "\"");
                                                }

                                                // if there is no description, add it
                                                if (tagMap.get(TagNames.DESCRIPTION) == null && description != null) {
                                                    Database.addTag(entry.getId(), TagNames.DESCRIPTION, description);
                                                    log.println("Added new tag with name \"" + TagNames.DESCRIPTION + "\" and value \"" + description + "\"");
                                                }

                                                // if there are no new links at all, add this one
                                                if (tagMap.get(TagNames.TRANCHE_NEW_LINK + " " + linkNum) == null && newVersion != null) {
                                                    Database.addTag(entry.getId(), TagNames.TRANCHE_NEW_LINK + " " + linkNum, newVersion);
                                                    log.println("Added new tag with name \"" + TagNames.TRANCHE_NEW_LINK + " " + linkNum + "\" and value \"" + newVersion + "\"");
                                                }
                                                // there could be more than one new tranche link, check if this one is in there
                                                if (tagMap.get(TagNames.TRANCHE_NEW_LINK + " " + linkNum) != null && newVersion != null) {
                                                    boolean found = false;
                                                    for (String value : tagMap.get(TagNames.TRANCHE_NEW_LINK + " " + linkNum)) {
                                                        if (value.trim().equals(newVersion)) {
                                                            found = true;
                                                            break;
                                                        }
                                                    }
                                                    if (!found) {
                                                        Database.addTag(entry.getId(), TagNames.TRANCHE_NEW_LINK + " " + linkNum, newVersion);
                                                        log.println("Added new tag with name \"" + TagNames.TRANCHE_NEW_LINK + " " + linkNum + "\" and value \"" + newVersion + "\"");
                                                    }
                                                }

                                                // if there are no old links at all, add this one
                                                if (tagMap.get(TagNames.TRANCHE_OLD_LINK + " " + linkNum) == null && oldVersion != null) {
                                                    Database.addTag(entry.getId(), TagNames.TRANCHE_OLD_LINK + " " + linkNum, oldVersion);
                                                    log.println("Added new tag with name \"" + TagNames.TRANCHE_OLD_LINK + " " + linkNum + "\" and value \"" + oldVersion + "\"");
                                                }
                                                // there could be more than one old tranche link, check if this one is in there
                                                if (tagMap.get(TagNames.TRANCHE_OLD_LINK + " " + linkNum) != null && oldVersion != null) {
                                                    boolean found = false;
                                                    for (String value : tagMap.get(TagNames.TRANCHE_OLD_LINK + " " + linkNum)) {
                                                        if (value.trim().equals(oldVersion)) {
                                                            found = true;
                                                            break;
                                                        }
                                                    }
                                                    if (!found) {
                                                        Database.addTag(entry.getId(), TagNames.TRANCHE_OLD_LINK + " " + linkNum, oldVersion);
                                                        log.println("Added new tag with name \"" + TagNames.TRANCHE_OLD_LINK + " " + linkNum + "\" and value \"" + oldVersion + "\"");
                                                    }
                                                }

                                                // if there are no old links at all, add this one
                                                if (tagMap.get(TagNames.TRANCHE_TYPE + " " + linkNum) == null && type != null) {
                                                    Database.addTag(entry.getId(), TagNames.TRANCHE_TYPE + " " + linkNum, type);
                                                    log.println("Added new tag with name \"" + TagNames.TRANCHE_TYPE + " " + linkNum + "\" and value \"" + type + "\"");
                                                }
                                                // there could be more than one type, check if this one is in there
                                                if (tagMap.get(TagNames.TRANCHE_TYPE + " " + linkNum) != null && type != null) {
                                                    boolean found = false;
                                                    for (String value : tagMap.get(TagNames.TRANCHE_TYPE + " " + linkNum)) {
                                                        if (value.trim().equals(type)) {
                                                            found = true;
                                                            break;
                                                        }
                                                    }
                                                    if (!found) {
                                                        Database.addTag(entry.getId(), TagNames.TRANCHE_TYPE + " " + linkNum, type);
                                                        log.println("Added new tag with name \"" + TagNames.TRANCHE_TYPE + " " + linkNum + "\" and value \"" + type + "\"");
                                                    }
                                                }

                                                // only make sure there is an encrypted tag - cache updater should fix anything incorrect
                                                if (tagMap.get(TagNames.TRANCHE_ENCRYPTED + " " + linkNum) == null && encrypted != null) {
                                                    Database.addTag(entry.getId(), TagNames.TRANCHE_ENCRYPTED + " " + linkNum, encrypted);
                                                    log.println("Added new tag with name \"" + TagNames.TRANCHE_ENCRYPTED + " " + linkNum + "\" and value \"" + encrypted + "\"");
                                                }

                                                // only make sure there is a size tag - cache updater should fix anything incorrect
                                                if (tagMap.get(TagNames.TRANCHE_SIZE + " " + linkNum) == null && size != null) {
                                                    Database.addTag(entry.getId(), TagNames.TRANCHE_SIZE + " " + linkNum, size);
                                                    log.println("Added new tag with name \"" + TagNames.TRANCHE_SIZE + " " + linkNum + "\" and value \"" + size + "\"");
                                                }

                                                // only make sure there is a files tag - cache updater should fix anything incorrect
                                                if (tagMap.get(TagNames.TRANCHE_FILES + " " + linkNum) == null && files != null) {
                                                    Database.addTag(entry.getId(), TagNames.TRANCHE_FILES + " " + linkNum, files);
                                                    log.println("Added new tag with name \"" + TagNames.TRANCHE_FILES + " " + linkNum + "\" and value \"" + files + "\"");
                                                }

                                                // only make sure there is a timestamp tag - cache updater should fix anything incorrect
                                                if (tagMap.get(TagNames.TIMESTAMP) == null && date != null) {
                                                    Database.addTag(entry.getId(), TagNames.TIMESTAMP, date);
                                                    log.println("Added new tag with name \"" + TagNames.TIMESTAMP + "\" and value \"" + date + "\"");
                                                }

                                                // only make sure there is a tranche timestamp tag - cache updater should fix anything incorrect
                                                if (tagMap.get(TagNames.TRANCHE_TIMESTAMP + " " + linkNum) == null && date != null) {
                                                    Database.addTag(entry.getId(), TagNames.TRANCHE_TIMESTAMP + " " + linkNum, date);
                                                    log.println("Added new tag with name \"" + TagNames.TRANCHE_TIMESTAMP + " " + linkNum + "\" and value \"" + date + "\"");
                                                }

                                                // only make sure there is a signatures tag - cache updater should fix anything incorrect
                                                if (tagMap.get(TagNames.TRANCHE_SIGNATURES + " " + linkNum) == null && sigs != null) {
                                                    Database.addTag(entry.getId(), TagNames.TRANCHE_SIGNATURES + " " + linkNum, sigs);
                                                    log.println("Added new tag with name \"" + TagNames.TRANCHE_SIGNATURES + " " + linkNum + "\" and value \"" + sigs + "\"");
                                                }

                                                // only make sure there is a deleted tag - cache updater should fix anything incorrect
                                                if (tagMap.get(TagNames.TRANCHE_DELETED + " " + linkNum) == null && deleted != null) {
                                                    Database.addTag(entry.getId(), TagNames.TRANCHE_DELETED + " " + linkNum, deleted);
                                                    log.println("Added new tag with name \"" + TagNames.TRANCHE_DELETED + " " + linkNum + "\" and value \"" + deleted + "\"");
                                                }

                                                // only make sure there is a show md if enc tag - cache updater should fix anything incorrect
                                                if (tagMap.get(TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum) == null && share != null) {
                                                    Database.addTag(entry.getId(), TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum, share);
                                                    log.println("Added new tag with name \"" + TagNames.TRANCHE_SHOW_MD_IF_ENC + " " + linkNum + "\" and value \"" + share + "\"");
                                                }

                                            } catch (Exception e) {
                                                log.println("ERROR: There was a problem editing entry #" + entry.getId() + ":" + e.getMessage());
                                            } finally {
                                                log.println("Finished editing entry #" + entry.getId());
                                            }
                                        }
                                    } else {
                                        try {
                                            log.println("Adding new entry.");
                                            long entryId = Database.createEntry();
                                            log.println("New entry is #" + entryId);
                                            Database.addTag(entryId, TagNames.TRANCHE_LINK + " 1", hash);
                                            log.println("Added new tag with name \"" + TagNames.TRANCHE_LINK + " 1\" and value \"" + hash + "\"");

                                            if (title != null) {
                                                Database.addTag(entryId, TagNames.TITLE, title);
                                                log.println("Added new tag with name \"" + TagNames.TITLE + "\" and value \"" + title + "\"");
                                            }
                                            if (description != null) {
                                                Database.addTag(entryId, TagNames.DESCRIPTION, description);
                                                log.println("Added new tag with name \"" + TagNames.DESCRIPTION + "\" and value \"" + description + "\"");
                                            }
                                            if (newVersion != null) {
                                                Database.addTag(entryId, TagNames.TRANCHE_NEW_LINK + " 1", newVersion);
                                                log.println("Added new tag with name \"" + TagNames.TRANCHE_NEW_LINK + " 1\" and value \"" + newVersion + "\"");
                                            }
                                            if (oldVersion != null) {
                                                Database.addTag(entryId, TagNames.TRANCHE_OLD_LINK + " 1", oldVersion);
                                                log.println("Added new tag with name \"" + TagNames.TRANCHE_OLD_LINK + " 1\" and value \"" + oldVersion + "\"");
                                            }
                                            if (type != null) {
                                                Database.addTag(entryId, TagNames.TRANCHE_TYPE + " 1", type);
                                                log.println("Added new tag with name \"" + TagNames.TRANCHE_TYPE + " 1\" and value \"" + type + "\"");
                                            }
                                            if (encrypted != null) {
                                                Database.addTag(entryId, TagNames.TRANCHE_ENCRYPTED + " 1", encrypted);
                                                log.println("Added new tag with name \"" + TagNames.TRANCHE_ENCRYPTED + " 1\" and value \"" + encrypted + "\"");
                                            }
                                            if (size != null) {
                                                Database.addTag(entryId, TagNames.TRANCHE_SIZE + " 1", size);
                                                log.println("Added new tag with name \"" + TagNames.TRANCHE_SIZE + " 1\" and value \"" + size + "\"");
                                            }
                                            if (files != null) {
                                                Database.addTag(entryId, TagNames.TRANCHE_FILES + " 1", files);
                                                log.println("Added new tag with name \"" + TagNames.TRANCHE_FILES + " 1\" and value \"" + files + "\"");
                                            }
                                            if (date != null) {
                                                Database.addTag(entryId, TagNames.TIMESTAMP, date);
                                                log.println("Added new tag with name \"" + TagNames.TIMESTAMP + "\" and value \"" + date + "\"");
                                                Database.addTag(entryId, TagNames.TRANCHE_TIMESTAMP + " 1", date);
                                                log.println("Added new tag with name \"" + TagNames.TRANCHE_TIMESTAMP + " 1\" and value \"" + date + "\"");
                                            }
                                            if (sigs != null) {
                                                Database.addTag(entryId, TagNames.TRANCHE_SIGNATURES + " 1", sigs);
                                                log.println("Added new tag with name \"" + TagNames.TRANCHE_SIGNATURES + " 1\" and value \"" + sigs + "\"");
                                            }
                                            if (deleted != null) {
                                                Database.addTag(entryId, TagNames.TRANCHE_DELETED + " 1", deleted);
                                                log.println("Added new tag with name \"" + TagNames.TRANCHE_DELETED + " 1\" and value \"" + deleted + "\"");
                                            }
                                            if (share != null) {
                                                Database.addTag(entryId, TagNames.TRANCHE_SHOW_MD_IF_ENC + " 1", share);
                                                log.println("Added new tag with name \"" + TagNames.TRANCHE_SHOW_MD_IF_ENC + " 1\" and value \"" + share + "\"");
                                            }
                                        } catch (Exception e) {
                                            log.println("ERROR: Problem adding entry information: " + e.getMessage());
                                        } finally {
                                            log.println("Finished adding entry.");
                                        }
                                    }
                                } catch (Exception e) {
                                    log.println("ERROR: " + e.getMessage());
                                }
                            }
                        } catch (Exception e) {
                            log.println("ERROR: " + e.getMessage());
                        }
                    } catch (Exception e) {
                        log.println("FATAL ERROR: " + e.getMessage());
                    } finally {
                        IOUtil.safeClose(br);
                        IOUtil.safeClose(fr);
                        log.println("Finished");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        log.flush();
                        log.close();
                        fos.flush();
                        fos.close();
                    } catch (Exception e) {
                    }
                }
            }
        };
        t.start();

    }
}
