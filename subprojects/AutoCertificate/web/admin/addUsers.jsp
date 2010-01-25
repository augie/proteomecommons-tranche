<%@include file="../header.inc"%>

<%--
* Admin can add users.
--%>
<%
if (!ControllerUtil.verifyLogin(session.getAttribute("username"),session.getAttribute("password")) || !ControllerUtil.isUserAdmin(session.getAttribute("username"))) {
    response.sendRedirect("../index.jsp?You must login as an admin first.");
    return;
}

%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Admin: Add Users</title>
        <link rel="stylesheet" type="text/css" href="../styles/style.css">
        <script type="text/javascript" src="../javascripts/user.js"></script>
        <script type="text/javascript" src="../javascripts/admin.js"></script>
    </head>
    <body>
        
        <h1>Create users/group</h1>
        
        <ul>
            <li>Select an existing group, add a new group or choose to not use a group.</li>
            <li>Add users. The selected group (or no group) will apply to all new users you add.</li>
        </ul>
        
        <form id="create_users_form" action="processAddUsers.jsp" method="POST">
            <h2>Group settings</h2>
            <label for="group">Select a group</label>
            <select id="group" name="group" onchange="checkForNewGroup();">
                <option value="-" selected="selected">No group</option>
                <option value="CREATE">Create new group...</option>
                <option value="-">-------------------------------</option>
                
                <%
                Iterator it = DatabaseUtil.getGroups().iterator();
                String nextGroup;
                while (it.hasNext()) {
                nextGroup = (String)it.next();
                out.println("<option value=\""+nextGroup+"\">"+nextGroup+"</option>");
                }
                %>
            </select>
            
            <!--
            Reserved for new group name, if selected
            -->
            <div id="new_group_box">
                <label for="new_group">Enter name for new group</label>
                <input type="text" name="new_group" id="new_group" />
            </div>
            
            <label for="expires_on">Expires after</label>
            <select id="expires_on" name="expires_on">
                <option value="<%= (System.currentTimeMillis()+1000*60*60*24*7) %>">1 week</option>
                <option value="<%= (System.currentTimeMillis()+1000*60*60*24*7*2) %>">2 weeks</option>
                <option value="<%= (System.currentTimeMillis()+1000*60*60*24*30)%>">30 days</option>
                <option value="9223372036854775807">Unlimited</option>
            </select>
            
            <h2>Add users...</h2>
            <input type="button" value="Add user..." id="add-user" onclick="return addAnotherUser();" />
            
            <!-- Holds users added to the DOM dynamically -->
            <div id="space_for_users">
                <!-- This paragraph is not displayed if no users -->
                <p id="no-added-users">You have not added any users.</p>
                
                <!-- Space reserved for users -->
                
            </div>
            
            <h2>Submit form</h2>
            <input type="submit" value="Create new users/group" onclick="return verifyNewUsersForm();" />
        </form>
    </body>
</html>
