<%@include file="../header.inc"%>
<%
String username = null;
int issueCount = -1;
String issueCountMsg = null;

if (!ControllerUtil.verifyLogin(session.getAttribute("username"),session.getAttribute("password"))) {
    response.sendRedirect("../index.jsp?You must login first.");
    return;
}

username = (String)session.getAttribute("username");
try {
    issueCount = DatabaseUtil.getCertificateRecordsForUser(username).size();
    if (issueCount == 1) {
        issueCountMsg = "<strong>1</strong> certificate issued to date.";
    }
    else {
        issueCountMsg = "<strong>"+issueCount+"</strong> certificates issued to date.";
    }
} catch(Exception e) {
    ControllerUtil.registerException(e);
    response.sendRedirect("../error.jsp");
}
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Tranche user: <% out.print(username); %>'s home</title>
        <link rel="stylesheet" type="text/css" href="../styles/style.css">
        <script type="text/javascript" src="../javascripts/user.js"></script>
    </head>
    <body>

    <h1>Welcome back, <% out.print(username); %>!</h1>
    
    <%
    out.println(ViewUtil.getFormattedLogoutLink(1));
    
    if (ControllerUtil.isUserAdmin(username)) {
        out.println("<p><a href=\"../admin/home.jsp\">&laquo; Return to admin home</a></p>");
    }
    
    out.println("<p>"+issueCountMsg+" [<a href=\"none\" onclick=\"return refresh();\"><strong>refresh</strong></a>]</p>");
    
    if (ViewUtil.isParamSet(request.getParameter("flash"))) {
        out.println(ViewUtil.getFormattedFlashMessage(request.getParameter("flash")));
    }
    %>
    
        <h2>Download new user file</h2>
        <p>The certificate will be good for <strong>two weeks</strong> and will allow you to upload to the <a href="http://tranche.proteomecommons.org/" target="_blank">Tranche</a> network.</p>
        <p>If your browser does not prompt you for the download location, the file will be downloaded to the default directory (usually your desktop unless you set a different preference in your browser) after clicking &quot;Create user&quot;. This page will <strong>not</strong> change after submitting the form and downloading the file.</p>
        
        <h3>Fill out form to get user file</h3>
        <p>Please provide a <strong>user name</strong> and a <strong>password</strong> for the user file you are creating. (These do not need to match the user name and password for this site.)</p>
        <form action="createUserFile.jsp" method="post">
            <label for="name">Name for certificate (as will appear on ProteomeCommons and Tranche)</label>
            <input type="text" id="name" name="name" />
            
            <label for="password1">Password for user file</label>
            <input type="password" id="password1" name="password1" />
            <label for="password2">Re-enter password</label>
            <input type="password" id="password2" name="password2" />
            
            <input type="submit" value="Create user" class="submit" onclick="return verify();" />
        </form>
    </body>
</html>
