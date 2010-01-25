<%@include file="../header.inc"%>
<%
String userCertName = null;
String userCertPassword = null;
int issueCount = -1;
String issueCountMsg = null;

if (!ControllerUtil.verifyLogin(session.getAttribute("username"),session.getAttribute("password"))) {
    response.sendRedirect("../index.jsp?You must login first.");
    return;
}

// If no request information, redirect w/ message
if (request.getParameter("name") == null ||
    request.getParameter("password1") == null ||
    request.getParameter("password1") == null) {
    response.sendRedirect("home.jsp?flash=You must provide a name and password. Please type the password twice to verify.");
    return;
}

userCertName = request.getParameter("name").toString();
userCertPassword = request.getParameter("password1").toString();
String password2 = request.getParameter("password2").toString();

// User didn't provide info, has Javascript disabled or Javascript broke
if (userCertName.equals("") || userCertPassword.equals("")) {
    response.sendRedirect("home.jsp?flash=You must provide a name and password.");
    return;
}

if (!password2.equals(userCertPassword)) {
    response.sendRedirect("home.jsp?flash=Your passwords do not match.");
    return;
}

// Perchance parameter is nothing, give empty string
if (userCertPassword == null) {
    userCertPassword = "";
}


FileInputStream in = null;
File userFile = null;
ServletOutputStream servletOut = null;

try {
    userFile = ControllerUtil.createPrivilegedUserFile(userCertName,userCertPassword);
    servletOut = response.getOutputStream();
    
    // Going to write the user file out
    response.setContentType("application/octet-stream");
    response.setHeader("Content-Disposition","attachment; filename=\""+userFile.getName()+'"');
    
    in = new FileInputStream(userFile);
    
    int i;
    while ((i = in.read()) != -1) {
        servletOut.write(i);
    }
    
    in.close();
    userFile.delete();
    
    // Record the user file creation
    DatabaseUtil.createCertificateEntry(DatabaseUtil.getIdForUser(session.getAttribute("username").toString()),userCertName);
} catch(Exception e) {
    ControllerUtil.registerException(e);
    response.sendRedirect("../error.jsp");
    return;
} finally {
    if (in != null) {
        in.close();
    }
    if (servletOut != null) {
        servletOut.flush();
    }
    if (userFile != null) {
        userFile.delete();
    }
}

%>