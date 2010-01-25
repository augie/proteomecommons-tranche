<%@include file="header.inc"%><%
String name = request.getParameter("name"),
        password = request.getParameter("password");

if (name == null || password == null) {
    throw new ServletException("Log in information cannot be blank.");
}

if (ControllerUtil.verifyLogin(name,password)) {
    
    response.setHeader("Log In Success", "true");
    
// Use this oppurtunity to send a warning message to admin. Only done
// once, so only one victim.
    if (ControllerUtil.isMasterCertExpiringWithinDuration(1000*60*60*24*7*3)) {
        ControllerUtil.sendEmailToAdminRegardingPendingCertExpiration();
    }
    
    File mainDirectory = new File("/opt/tomcat5/db/userZipFiles/");
    int userId = DatabaseUtil.getIdForUser(name);
    File userDirectory = new File("/opt/tomcat5/db/userZipFiles/" + userId + "/");
    if (!userDirectory.exists()) {
        userDirectory.mkdirs();
    }
    
    String userCertName = null;
    File userZipFile = null;
    for (File file : userDirectory.listFiles()) {
// check the date of the certificate
        try {
            UserZipFile uzf = new UserZipFile(file);
            uzf.setPassphrase(password);
            // The server is behind plus don't want a user's cert to go bad in middle of upload
            // check for two days in advance. (I had problem where I couldn't log in b/c funky server date)
            uzf.getCertificate().checkValidity(new Date(System.currentTimeMillis()+1000*60*60*24*2));
            uzf.getCertificate().checkValidity(new Date(System.currentTimeMillis()-1000*60*60*24));
            userCertName = file.getName();
            userZipFile = file;
        } catch (Exception e) {
// do nothing
        }
    }
    
// create a new user zip file
    if (userZipFile == null) {
// create the user zip file
        userZipFile = ControllerUtil.createPrivilegedUserFile(name, password);
// save a copy to the user directory
        userCertName = name + System.currentTimeMillis() + ".zip.encrypted";
        DatabaseUtil.copyFile(userZipFile, new File(userDirectory, userCertName));
        DatabaseUtil.createCertificateEntry(userId, userCertName);
    }
    
// respond with the user zip file
    response.setContentType("application/octet-stream");
    response.setHeader("Content-Disposition","attachment; filename=\"" + userZipFile.getName() + "\"");
    response.setHeader("File Name", userZipFile.getName());
    response.setHeader("User Email", DatabaseUtil.getUserRecord(userId).email);
    
    ServletOutputStream servletOut = response.getOutputStream();
    FileInputStream fis = null;
    try {
        fis = new FileInputStream(userZipFile);
        int i;
        while ((i = fis.read()) != -1) {
            servletOut.write(i);
        }
    } finally {
        if (fis != null) {
            fis.close();
        }
        if (servletOut != null) {
            servletOut.flush();
        }
    }
    
} else {
    
    response.setHeader("Log In Success", "false");
    
    if (DatabaseUtil.getIdForUser(name) != -1) {
        response.setHeader("User Approved", String.valueOf(DatabaseUtil.getUserRecord(name).isApproved));
    }
    
}
%>     