<%@ page import="org.tranche.users.*, org.tranche.flatfile.*, java.io.*,java.util.*,org.apache.commons.io.*,org.apache.commons.fileupload.*,org.apache.commons.fileupload.servlet.*,org.apache.commons.fileupload.disk.*,org.apache.commons.httpclient.*,org.proteomecommons.tranche.proxy.*" %><%@ page contentType="text/plain" %><%

            LinkedList<UploadThread> uploadList = (LinkedList<UploadThread>) application.getAttribute("uploadList");
            if (uploadList == null) {
                uploadList = new LinkedList<UploadThread>();
                application.setAttribute("uploadList", uploadList);
            }

// clear the upload folders of unkown directories
            File webDirectory = new File("/opt/tomcat5/webapps/proteomecommons/data/upload/");
            if (!webDirectory.exists()) {
                webDirectory.mkdirs();
            }
            webDirectory.mkdirs();
            try {
                for (File file : webDirectory.listFiles()) {
                    boolean found = false;
                    for (UploadThread ut : uploadList) {
                        if (ut.uzf.getFile().getParentFile().getAbsolutePath().equals(file.getAbsolutePath())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found && file.isDirectory()) {
                        FileUtil.recursiveDelete(file);
                    }
                }

// perform some cleanup of the upload list
                if (uploadList.size() >= 5) {
// try to remove some failed uploads
                    for (int i = uploadList.size() - 1; i >= 0; i--) {
                        if (uploadList.get(i).getStatus() == UploadThread.STATUS_FAILED) {
                            FileUtil.recursiveDelete(uploadList.get(i).uploadedFile.getParentFile());
                            uploadList.remove(i);
                        }
                    }
// if we didn't remove enough, remove some of the oldest completed ones
                    List<UploadThread> complete = new ArrayList<UploadThread>();
                    for (int i = 0; i < uploadList.size(); i++) {
                        if (uploadList.get(i).getStatus() == UploadThread.STATUS_COMPLETED) {
                            complete.add(uploadList.get(i));
                        }
                    }
                    for (UploadThread ut : complete) {
                        if (uploadList.size() <= 3) {
                            break;
                        }
                        FileUtil.recursiveDelete(ut.uploadedFile.getParentFile());
                        uploadList.remove(ut);
                    }
                    application.setAttribute("uploadList", uploadList);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

// check that we have a file upload request
            boolean isMultipart = ServletFileUpload.isMultipartContent(request);
            if (!isMultipart) {
                throw new RuntimeException("Your request could not be handled.");
            }

// create and start the upload thread
            final UploadThread uploadThread = new UploadThread(System.currentTimeMillis() + String.valueOf(uploadList.size()));
            uploadList.add(uploadThread);
            application.setAttribute("uploadList", uploadList);

            out.println("REQUEST = " + uploadThread.getRequestCode());
            out.flush();

// current timestamp used in making a folder for this upload
            final File parentDirectory = new File("/opt/tomcat5/webapps/proteomecommons/data/upload/" + String.valueOf(System.currentTimeMillis()));
            parentDirectory.mkdirs();

// Create a factory for disk-based file items
            DiskFileItemFactory factory = new DiskFileItemFactory(DataBlockUtil.ONE_MB * 5, parentDirectory);
// Create a new file upload handler
            ServletFileUpload upload = new ServletFileUpload(factory);

// Parse the request
            List<FileItem> items = upload.parseRequest(request);
// Process the uploaded items
            final Iterator iter = items.iterator();

            Thread t = new Thread("Process POST Upload Data") {

                public void run() {
                    try {
// upload parameters
                        File uploadedFile = null;
                        UserZipFile uzf = null;
                        String uzfPassphrase = null, passphrase = null, title = null, description = null;
                        Boolean uploadAsDirectory = true, register = true, useRemoteRep = false, skipExistingFiles = false, skipExistingChunks = true;
                        List<String> servers = null;

                        while (iter.hasNext()) {
                            FileItem item = (FileItem) iter.next();

                            if (item.isFormField()) {
// Process a regular form field
                                String name = item.getFieldName();
                                String value = item.getString();

                                if (name.equals("uzfPassphrase")) {
                                    uzfPassphrase = value;
                                } else if (name.equals("passphrase")) {
                                    passphrase = value;
                                } else if (name.equals("title")) {
                                    title = value;
                                } else if (name.equals("description")) {
                                    description = value;
                                } else if (name.equals("uploadAsDirectory")) {
                                    uploadAsDirectory = Boolean.parseBoolean(value);
                                } else if (name.equals("register")) {
                                    register = Boolean.parseBoolean(value);
                                } else if (name.equals("remoteRep")) {
                                    useRemoteRep = Boolean.parseBoolean(value);
                                } else if (name.equals("skipFiles")) {
                                    skipExistingFiles = Boolean.parseBoolean(value);
                                } else if (name.equals("skipChunks")) {
                                    skipExistingChunks = Boolean.parseBoolean(value);
                                } else if (name.startsWith("server")) {
                                    if (value != null && !value.equals("")) {
                                        if (servers == null) {
                                            servers = new ArrayList<String>();
                                        }
                                        servers.add(value);
                                    }
                                }
                            } else {
// Process a file upload
                                String fieldName = item.getFieldName();
                                String fileName = item.getName();
                                String contentType = item.getContentType();
                                boolean isInMemory = item.isInMemory();
                                long sizeInBytes = item.getSize();

// figure out the file location
                                File saveToLocation = null;
                                if (fieldName.equals("uzf") || fieldName.equals("upload")) {
                                    saveToLocation = new File(parentDirectory.getCanonicalPath() + File.separator + fileName.substring(fileName.lastIndexOf("\\") + 1));
                                    saveToLocation.createNewFile();
                                } else {
// this is an irrelevant item
                                    continue;
                                }

// process the file as a stream
                                InputStream uploadedStream = item.getInputStream();
                                FileOutputStream fos = new FileOutputStream(saveToLocation);

// copy the file
                                while (uploadedStream.available() > 0) {
                                    fos.write(uploadedStream.read());
                                }

// close the file
                                fos.close();
                                uploadedStream.close();

                                if (fieldName.equals("uzf")) {
                                    uzf = new UserZipFile(saveToLocation);
                                } else if (fieldName.equals("upload")) {
                                    uploadedFile = saveToLocation;
                                }
                            }
                        }

                        uploadThread.setParameters(uploadedFile, uzf, uzfPassphrase, passphrase, title, description, uploadAsDirectory, register, useRemoteRep, skipExistingFiles, skipExistingChunks, servers);
                        uploadThread.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            t.start();
%>