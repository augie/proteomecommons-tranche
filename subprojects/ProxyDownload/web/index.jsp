<%@ page import="java.io.*, java.util.*, org.proteomecommons.tranche.proxy.*" %><%@ page contentType="text/plain" %><%

            try {
                LinkedList<DownloadRequest> requestList = (LinkedList<DownloadRequest>) application.getAttribute("requestList");
                if (requestList == null) {
                    requestList = new LinkedList<DownloadRequest>();
                    application.setAttribute("requestList", requestList);
                }

// clear the download folder of unkown directories
                File webDirectory = new File("/opt/tomcat5/webapps/proteomecommons/data/download/");
                if (webDirectory.exists()) {
                    for (File file : webDirectory.listFiles()) {
                        boolean found = false;
                        for (DownloadRequest dr : requestList) {
                            if (dr.getDownloadThread().downloadDirectory.getAbsolutePath().equals(file.getAbsolutePath())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found && file.isDirectory()) {
                            FileUtil.recursiveDelete(file);
                        }
                    }
                }

// perform some cleanup of the request list
                if (requestList.size() >= 5) {
// try to remove some failed requests
                    for (int i = requestList.size() - 1; i >= 0; i--) {
                        if (requestList.get(i).getDownloadThread().getStatus() == DownloadThread.STATUS_FAILED) {
                            try {
                                FileUtil.recursiveDelete(requestList.get(i).getDownloadThread().downloadDirectory);
                                if (requestList.get(i).getDownloadThread().zipFile.exists()) {
                                    requestList.get(i).getDownloadThread().zipFile.delete();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            requestList.remove(i);
                        }
                    }
// if we didn't remove enough, remove some of the oldest completed ones
                    List<DownloadRequest> complete = new ArrayList<DownloadRequest>();
                    for (int i = 0; i < requestList.size(); i++) {
                        if (requestList.get(i).getDownloadThread().getStatus() == DownloadThread.STATUS_COMPLETED) {
                            complete.add(requestList.get(i));
                        }
                    }
                    for (DownloadRequest dr : complete) {
                        if (requestList.size() <= 3) {
                            break;
                        }
                        try {
                            FileUtil.recursiveDelete(dr.getDownloadThread().downloadDirectory);
                            if (dr.getDownloadThread().zipFile.exists()) {
                                dr.getDownloadThread().zipFile.delete();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        requestList.remove(dr);
                    }
                    application.setAttribute("requestList", requestList);
                }

                String hash = request.getParameter("hash");

// Quick hack: if spaces, convert to '+'
                hash = hash.replaceAll(" ", "+");

                if (hash == null) {
                    throw new RuntimeException("You must provide a \"hash\" parameter.");
                }

                String passphrase = request.getParameter("passphrase");
                String regex = request.getParameter("regex");
                List<String> servers = new ArrayList<String>();

                for (Enumeration en = request.getParameterNames(); en.hasMoreElements();) {
                    String name = (String) en.nextElement();
                    if (name.startsWith("server")) {
                        String value = request.getParameter(name);
                        if (value != null && !value.equals("")) {
                            servers.add(value);
                        }
                    }
                }

                if (servers.size() == 0) {
                    servers = null;
                }

                DownloadRequest downloadRequest = new DownloadRequest(hash, passphrase, regex, servers);

                boolean found = false;
                for (DownloadRequest dr : requestList) {
                    if (downloadRequest.equals(dr)) {
                        if (dr.getDownloadThread().getStatus() != DownloadThread.STATUS_FAILED) {
                            found = true;
                            downloadRequest.setDownloadThread(dr.getDownloadThread());
                            break;
                        }
                    }
                }

// if no suitable download thread is already available
                if (!found) {
                    DownloadThread dt = new DownloadThread(hash, passphrase, regex, servers);
                    downloadRequest.setDownloadThread(dt);
                    dt.start();
                }

// check the status of the download thread
                int status = downloadRequest.getDownloadThread().getStatus();

                if (status == DownloadThread.STATUS_COMPLETED) {
                    out.println("STATUS = COMPLETE");
                    out.println("URL = http://www.proteomecommons.org/data/download/" + downloadRequest.getDownloadThread().zipFile.getParentFile().getName() + "/" + downloadRequest.getDownloadThread().zipFile.getName());
                } else {
// update the request list
                    requestList.add(downloadRequest);
                    application.setAttribute("requestList", requestList);
// the download is running, notify the user to wait and give them their download requets's hash code
                    out.println("REQUEST = " + downloadRequest.hashCode());
                }
            } catch (Exception e) {
                out.println(e.toString());
                e.printStackTrace(System.err);
            }
%>