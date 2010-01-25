<%@ page import="org.proteomecommons.tranche.cacheupdater.*, java.io.*, org.tranche.users.*, org.tranche.util.*" %><%@ page contentType="text/plain" %><%

            try {
                final CacheUpdater cu = new CacheUpdater();

                // set the important boolean values for the updater
                if (request.getParameter("makeChanges") != null && !request.getParameter("makeChanges").equals("")) {
                    try {
                        cu.makeChanges = Boolean.valueOf((String) request.getParameter("makeChanges")).booleanValue();
                    } catch (Exception e) {
                    }
                }
                if (request.getParameter("validate") != null && !request.getParameter("validate").equals("")) {
                    try {
                        cu.validate = Boolean.valueOf((String) request.getParameter("validate")).booleanValue();
                    } catch (Exception e) {
                    }
                }
                if (request.getParameter("updateTagsDatabase") != null && !request.getParameter("updateTagsDatabase").equals("")) {
                    try {
                        cu.updateTagsDatabase = Boolean.valueOf((String) request.getParameter("updateTagsDatabase")).booleanValue();
                    } catch (Exception e) {
                    }
                }
                if (request.getParameter("makeNewCache") != null && !request.getParameter("makeNewCache").equals("")) {
                    try {
                        cu.makeNewCache = Boolean.valueOf((String) request.getParameter("makeNewCache")).booleanValue();
                    } catch (Exception e) {
                    }
                }
                if (request.getParameter("indexTagsDatabase") != null && !request.getParameter("indexTagsDatabase").equals("")) {
                    try {
                        cu.indexTagsDatabase = Boolean.valueOf((String) request.getParameter("indexTagsDatabase")).booleanValue();
                    } catch (Exception e) {
                    }
                }

                File userFile = new File("/opt/tomcat5/webapps/proteomecommons/WEB-INF/tags/cache/cache-updater.zip.encrypted");
                if (!userFile.exists()) {
                    throw new RuntimeException("Could not locate the given user file.");
                }
                cu.user = new UserZipFile(userFile);

                File passwordFile = new File("/opt/tomcat5/webapps/proteomecommons/WEB-INF/tags/cache/cache-updater.psswd");
                if (!passwordFile.exists()) {
                    throw new RuntimeException("Could not locate the user password file.");
                }
                FileReader fileReader = new FileReader(passwordFile);
                String passphrase = "";
                while (fileReader.ready()) {
                    passphrase = passphrase + (char) fileReader.read();
                }
                cu.user.setPassphrase(passphrase);

                if (cu.user.isExpired()) {
                    Thread t = new Thread() {

                        public void run() {
                            try {
                                EmailUtil.sendEmail("ERROR: Cache Updater Cert Is Expired", new String[]{"proteomecommons-tranche-dev@googlegroups.com", "augman85@gmail.com", "jfalkner@umich.edu"}, "The certificate used in the cache updater is no longer valid.");
                            } catch (Exception e) {
                            }
                        }
                    };
                    t.start();
                    throw new RuntimeException("The certificate for the user is expired.");
                }

                if (cu.user.isNotYetValid()) {
                    Thread t = new Thread() {

                        public void run() {
                            try {
                                EmailUtil.sendEmail("ERROR: Cache Updater Cert Is Not Yet Valid", new String[]{"proteomecommons-tranche-dev@googlegroups.com", "augman85@gmail.com", "jfalkner@umich.edu"}, "The certificate used in the cache updater is no longer valid.");
                            } catch (Exception e) {
                            }
                        }
                    };
                    t.start();
                    throw new RuntimeException("The certificate for the user is not yet valid.");
                }

                if (cu.user.getCertificate() == null) {
                    Thread t = new Thread() {

                        public void run() {
                            try {
                                EmailUtil.sendEmail("ERROR: Cache Updater Cert No Longer Valid", new String[]{"proteomecommons-tranche-dev@googlegroups.com", "augman85@gmail.com", "jfalkner@umich.edu"}, "The certificate used in the cache updater is no longer valid.");
                            } catch (Exception e) {
                            }
                        }
                    };
                    t.start();
                    throw new RuntimeException("The password for the given user file is incorrect.");
                }

                // delete old runs
                try {
                    long oneMonthAgo = System.currentTimeMillis() - (long) 864E6;
                    File runsDirectory = new File("/opt/tomcat5/webapps/proteomecommons/WEB-INF/tags/cache/runs/");
                    for (File runDirectory : runsDirectory.listFiles()) {
                        if (runDirectory.isDirectory() && !runsDirectory.getAbsolutePath().equals(runDirectory.getAbsolutePath())) {
                            try {
                                long timestamp = Long.valueOf(runDirectory.getName());
                                if (timestamp != 0 && timestamp < oneMonthAgo) {
                                    IOUtil.recursiveDelete(runDirectory);
                                }
                            } catch (Exception e) {
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                // when the file is run - it's saved in the root directory - need to delete these
                try {
                    File rootDirectory = new File("/root/");
                    for (File file : rootDirectory.listFiles()) {
                        try {
                            if (file.isFile() && (file.getName().startsWith("run-cache-updater.jsp") || file.getName().startsWith("email-cache-updater-info.jsp"))) {
                                IOUtil.safeDelete(file);
                            }
                        } catch (Exception e) {
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Thread t = new Thread() {

                    public void run() {
                        cu.execute();
                    }
                };
                t.start();
            } catch (Exception e) {
                e.printStackTrace();
            }

%>