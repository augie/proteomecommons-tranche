<%@include file="../header.inc"%>
<%--
* Admin's home page, administrative tasks.
--%>
<%
if (!ControllerUtil.verifyLogin(session.getAttribute("username"),session.getAttribute("password")) || !ControllerUtil.isUserAdmin(session.getAttribute("username"))) {
    response.sendRedirect("../index.jsp?You must login as an admin first.");
    return;
}

// Any group to expire
String groupToExpire = request.getParameter("group_to_expire");
if (groupToExpire != null && !groupToExpire.trim().equals("") && !groupToExpire.trim().equals("none")) {
    DatabaseUtil.expireGroup(groupToExpire);
}

// Any group to validate
String groupToValidate = request.getParameter("group_to_validate");
long expiresOn = -1;
try {
    expiresOn = Long.parseLong(request.getParameter("expires_on"));
} catch (Exception exc) { /* nope */ }

if (groupToValidate != null && !groupToValidate.trim().equals("") && !groupToValidate.trim().equals("none") && expiresOn != -1) {
    DatabaseUtil.setExpirationForGroup(groupToValidate,expiresOn);
}

String flashMsg = request.getParameter("flash");
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Admin: home</title>
        <link rel="stylesheet" type="text/css" href="../styles/style.css">
        <script type="text/javascript" src="../javascripts/user.js"></script>
        <script type="text/javascript" src="../javascripts/library.js"></script>
    </head>
    <body>

    <h1>Welcome back, Admin!</h1>
    
    <%
    out.println(ViewUtil.getFormattedLogoutLink(1));
    
    if (ViewUtil.isParamSet(request.getParameter("flash"))) {
        out.println(ViewUtil.getFormattedFlashMessage(request.getParameter("flash")));
    }
    %>
    
    <p><a href="../user/home.jsp">View user home &raquo;</a></p>
    
    <h2>Certificate</h2>
    <p>The authority certificate is good from <strong><%= ViewUtil.getFormattedTime(ControllerUtil.getMasterCertValidStarting()) %></strong> until <strong><%= ViewUtil.getFormattedTime(ControllerUtil.getMasterCertValidEnding()) %></strong>.</p>
    
    <h2>Administrative tasks</h2>
    <ul>
        <li><a href="manageApplications.jsp">Manage applications</a></li>
        <li><a href="manageUsers.jsp">Manage users</a></li>
        <li><a href="addUsers.jsp">Create users/group</a></li>
    </ul>
    
    <% 
    int validCount = DatabaseUtil.getValidGroups().size();
    int expiredCount = DatabaseUtil.getExpiredGroups().size();
    int totalCount = validCount + expiredCount;
    %>
    
    <%--
    <h2>Groups</h2>
    
    <ul>
        <li>Total of <strong><%= totalCount %></strong> group(s)</li>
        <li> <strong><%= validCount %></strong> valid group(s)</li>
        <li> <strong><%= expiredCount %></strong> expired group(s)</li>
    </ul>
    --%>
    
    <%-- If there are any valid groups, give admin user ability to expire --%>
    <% if (validCount > 0) { %>
    <h2>Expire a group</h2>
    
    <form method="post">
        
        <input type="hidden" name="flash" id="flash" value="Group was expired" />
        
        <label for="group_to_expire">Select a group to expire</label>
        <select id="group_to_expire" name="group_to_expire">
            <option value="none" selected="selected">Select a group...</option>
            <%
            Iterator it = DatabaseUtil.getValidGroups().iterator();
            
            String nextGroup;
            while (it.hasNext()) {
                nextGroup = (String)it.next();
                out.println("<option value=\""+nextGroup+"\">"+nextGroup+"</option>");
            }
            
            %>
        </select>
        
        <br /><input type="submit" value="Expire selected group" onclick="return verifyExpireGroup();" />
    </form>
    <% } %>
    
    <%-- If there are any expired groups, give admin user ability to validate them --%>
    <% if (expiredCount > 0) { %>
    <h2>Validate a group</h2>
    
    <form method="post">
        
        <label for="expires_on">Expires after</label>
        <select id="expires_on" name="expires_on">
            <option value="<%= (System.currentTimeMillis()+1000*60*60*24*7) %>">1 week</option>
            <option value="<%= (System.currentTimeMillis()+1000*60*60*24*7*2) %>">2 weeks</option>
            <option value="<%= (System.currentTimeMillis()+1000*60*60*24*30)%>">30 days</option>
            <option value="9223372036854775807">Unlimited</option>
        </select>
        
        <input type="hidden" name="flash" id="flash" value="Group was validated" />
        
        <label for="group_to_validate">Select a group to validate</label>
        <select id="group_to_validate" name="group_to_validate">
            <option value="none" selected="selected">Select a group...</option>
            <%
            Iterator it = DatabaseUtil.getExpiredGroups().iterator();
            
            String nextGroup;
            while (it.hasNext()) {
                nextGroup = (String)it.next();
                out.println("<option value=\""+nextGroup+"\">"+nextGroup+"</option>");
            }
            
            %>
        </select>
        
        <br /><input type="submit" value="Validate selected group" onclick="return verifyValidateGroup();" />
    </form>
    <% } %>
    
    </body>
</html>
