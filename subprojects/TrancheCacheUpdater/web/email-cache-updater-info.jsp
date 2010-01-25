<%@ page import="java.util.*,java.io.*" %><%@ page contentType="text/plain" %>
<%
            final boolean debug = false;
            long addedEntries = 0, editedEntries = 0, addedTags = 0, editedTags = 0, removedTags = 0, entriesInCache = 0, dataAndMetaCount = 0, missing = 0;
            final List<File> generalLogFiles = new ArrayList<File>();
            File latestRunDirectoryWithMissingOrInvalid = null;
            File latestRunDirectory = null;
            long latestRunTimestampWithMissingOrInvalid = 0;
            long latestRunTimestamp = 0;
            try {
                File runsDirectory = new File("/opt/tomcat5/webapps/proteomecommons/WEB-INF/tags/cache/runs/");
                if (!runsDirectory.exists()) {
                    throw new Exception("Stop the email cache updater info script - no logs to check.");
                }

                if (debug) {
                    out.println("Checking the runs directory for the runs in the past week.");
                }

                // get a list of general log files from the last week
                long oneWeekAgo = System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 7;
                for (String directoryName : runsDirectory.list()) {
                    if (debug) {
                        out.println("Directory Name: " + directoryName);
                    }
                    try {
                        // double check this is a directory
                        File directory = new File(runsDirectory, directoryName);
                        if (!directory.isDirectory()) {
                            continue;
                        }

                        // get the timestamp of this directory
                        Long timestamp = Long.valueOf(directoryName);
                        if (debug) {
                            out.println("Timestamp for directory: " + timestamp);
                            out.println("Latest Run Timestamp: " + latestRunTimestamp);
                            out.println("Latest Full Run Timestamp: " + latestRunTimestampWithMissingOrInvalid);
                        }
                        if (timestamp >= oneWeekAgo) {
                            try {
                                File generalLog = new File(directory, "general.log");
                                generalLogFiles.add(generalLog);
                                if (generalLog.exists()) {
                                    if (debug) {
                                        out.println("Trying general log at " + generalLog.getAbsolutePath());
                                    }

                                    BufferedReader br = null;
                                    FileReader fr = null;
                                    try {
                                        fr = new FileReader(generalLog);
                                        br = new BufferedReader(fr);

                                        if (debug) {
                                            out.println("Opened the general log for reading.");
                                        }

                                        // skip 3 lines
                                        for (int i = 0; i < 3; i++) {
                                            br.readLine();
                                        }

                                        // read the values
                                        String line = br.readLine();
                                        addedEntries += Long.valueOf(line.substring(0, line.indexOf(' ')).trim());
                                        line = br.readLine();
                                        editedEntries += Long.valueOf(line.substring(0, line.indexOf(' ')).trim());
                                        line = br.readLine();
                                        addedTags += Long.valueOf(line.substring(0, line.indexOf(' ')).trim());
                                        line = br.readLine();
                                        editedTags += Long.valueOf(line.substring(0, line.indexOf(' ')).trim());
                                        line = br.readLine();
                                        removedTags += Long.valueOf(line.substring(0, line.indexOf(' ')).trim());

                                        // get the number of entries, making sure the cache didn't fail
                                        line = br.readLine();
                                        Long thisDataAndMetaCount = Long.valueOf(line.substring(0, line.indexOf(" ")).trim());

                                        if (debug) {
                                            out.println("# data and meta data on Tranche: " + thisDataAndMetaCount);
                                        }
                                        
                                        if (thisDataAndMetaCount > 0 && timestamp > latestRunTimestampWithMissingOrInvalid) {
                                            dataAndMetaCount = thisDataAndMetaCount;
                                            if (debug) {
                                                out.println("Set the latest data and meta data count to " + dataAndMetaCount);
                                            }
                                        }
                                        
                                        // get the number of entries, making sure the cache didn't fail
                                        line = br.readLine();
                                        Long thisEntriesInCache = Long.valueOf(line.substring(0, line.indexOf(" ")).trim());

                                        if (debug) {
                                            out.println("# entries in cache in this directory: " + thisEntriesInCache);
                                        }

                                        if (thisEntriesInCache > 0 && timestamp > latestRunTimestampWithMissingOrInvalid) {
                                            entriesInCache = thisEntriesInCache;
                                            if (debug) {
                                                out.println("Set the latest entries in cache to " + entriesInCache);
                                            }
                                        }

                                        // get the last number of missing
                                        line = br.readLine();
                                        Long thisMissing = Long.valueOf(line.substring(0, line.indexOf(" ")).trim());

                                        if (debug) {
                                            out.println("# missing in this directory: " + thisMissing);
                                        }

                                        if (thisMissing > 0 && timestamp > latestRunTimestampWithMissingOrInvalid) {
                                            missing = thisMissing;
                                            if (debug) {
                                                out.println("Set the latest missing Tranche data sets to " + missing);
                                            }
                                        }

                                    } catch (Exception e) {
                                        if (debug) {
                                            out.println("An error occurred while trying to read a general log with the class: " + e.getClass().getName());
                                        }
                                    } finally {
                                        try {
                                            br.close();
                                            fr.close();
                                        } catch (Exception e) {
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                if (debug) {
                                    out.println("An error occurred while trying to open the general log with the class: " + e.getClass().getName());
                                }
                            }

                            try {
                                if (latestRunDirectory == null || timestamp > latestRunTimestamp) {
                                    latestRunDirectory = directory;
                                    latestRunTimestamp = timestamp;
                                }
                                File invalidFile = new File(directory, "invalid.log"), missingFile = new File(directory, "missing.log"), changesFile = new File(directory, "changes.log");
                                if ((invalidFile.exists() && invalidFile.length() > 0) || (missingFile.exists() && missingFile.length() > 0) || (changesFile.exists() && changesFile.length() > 0)) {
                                    latestRunDirectoryWithMissingOrInvalid = directory;
                                    if (timestamp > latestRunTimestampWithMissingOrInvalid) {
                                        latestRunTimestampWithMissingOrInvalid = timestamp;
                                    }
                                }
                            } catch (Exception e) {
                                if (debug) {
                                    out.println("An error occurred while trying to update the latest run info with the class: " + e.getClass().getName());
                                }
                            }
                        }
                    } catch (Exception e) {
                        if (debug) {
                            out.println("An error occurred while trying to open the directory with the class: " + e.getClass().getName());
                        }
                    }
                }
            } catch (Exception e) {
                if (debug) {
                    out.println("An unexpected error occurred with the class: " + e.getClass().getName());
                }
            }


// turn the counts into strings for the message
            final String addedEntriesStr = String.valueOf(addedEntries),  editedEntriesStr = String.valueOf(editedEntries),  addedTagsStr = String.valueOf(addedTags),  editedTagsStr = String.valueOf(editedTags),  removedTagsStr = String.valueOf(removedTags),  dataAndMetaCountStr = String.valueOf(dataAndMetaCount), entriesInCacheStr = String.valueOf(entriesInCache),  missingStr = String.valueOf(missing);

// send email
            final File fLatestRunDirectory = latestRunDirectoryWithMissingOrInvalid;
            Thread t = new Thread() {

                public void run() {

                    // create the message
                    String message = "WEEKLY TRANCHE UPLOAD CACHE REPORT\n\n" + "In the past week, the upload cache updater ran  " + generalLogFiles.size() + "  times, and performed the following actions:\n\n" + addedEntriesStr + "  entries added to the tags database.\n" + editedEntriesStr + "  entries edited within the tags database.\n" + addedTagsStr + "  tags added to the tags database.\n" + editedTagsStr + "  tags edited within the tags database.\n" + removedTagsStr + "  sets of tags removed from the tags database.\n\n" + "As of now, there are  " + dataAndMetaCountStr + "  total data and meta data on the network.\nAs of now, there are  " + entriesInCacheStr + "  entries in the cache.\n" + "As of now, there are  " + missingStr + "  Tranche data sets missing.";

                    BufferedReader br = null;
                    FileReader fr = null;

                    message = message + "\n\n\n------ CHANGES MADE -------\n\n";
                    for (File generalLogFile : generalLogFiles) {
                        // print the changes log contents
                        File changesLog = new File(generalLogFile.getParent(), "changes.log");
                        try {
                            fr = new FileReader(changesLog);
                            br = new BufferedReader(fr);
                            while (br.ready()) {
                                message = message + br.readLine() + "\n";
                            }
                        } catch (Exception e) {
                        } finally {
                            try {
                                br.close();
                                fr.close();
                            } catch (Exception e) {
                            }
                        }
                    }

                    if (fLatestRunDirectory != null) {
                        // print the missing log contents
                        File missingLog = new File(fLatestRunDirectory, "missing.log");
                        message = message + "\n\n\n------ LATEST MISSING TRANCHE DATA SETS -------\n\n";
                        try {
                            fr = new FileReader(missingLog);
                            br = new BufferedReader(fr);
                            while (br.ready()) {
                                message = message + br.readLine() + "\n";
                            }
                        } catch (Exception e) {
                        } finally {
                            try {
                                br.close();
                                fr.close();
                            } catch (Exception e) {
                            }
                        }

                        // print the invalid log contents
                        File invalidLog = new File(fLatestRunDirectory, "invalid.log");
                        message = message + "\n\n\n------ LATEST INVALID TRANCHE DATA SETS -------\n\n";

                        try {
                            fr = new FileReader(invalidLog);
                            br = new BufferedReader(fr);
                            while (br.ready()) {
                                message = message + br.readLine() + "\n";
                            }
                        } catch (Exception e) {
                        } finally {
                            try {
                                br.close();
                                fr.close();
                            } catch (Exception e) {
                            }
                        }
                    }

                    try {
                        String[] live = new String[]{"proteomecommons-tranche-dev@googlegroups.com", "jfalkner@umich.edu", "bryanesmith@gmail.com", "augman85@gmail.com", "oddtodd@gmail.com"};
                        String[] test = new String[]{"augman85@gmail.com"};
                        String[] emails = live;
                        if (debug) {
                            emails = test;
                        }
                        EmailUtil.sendEmail("Tranche: Weekly Upload Cache Report", emails, message);
                    } catch (Exception e) {
                    }
                }
            };
            t.start();
%>