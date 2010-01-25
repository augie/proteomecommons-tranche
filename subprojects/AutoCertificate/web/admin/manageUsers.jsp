<%@include file="../header.inc"%>
<%--
* Manage Tranche users.
--%>
<%
if (!ControllerUtil.verifyLogin(session.getAttribute("username"),session.getAttribute("password")) || !ControllerUtil.isUserAdmin(session.getAttribute("username"))) {
    response.sendRedirect("../index.jsp?You must login as an admin first.");
    return;
}
%>
<%!
List users = null;
String usersMsg = null;
%>
<% 
// Get all applications
users = ViewUtil.sortUsersByName(DatabaseUtil.getApprovedUserRecords());

if (users.size() == 0) {
    throw new Exception("Must be at least one user in the system: admin is logged in!");
} else if (users.size() == 1) {
    usersMsg = "There is <strong>1</strong> user.";
} else {
    usersMsg = "There are <strong>"+users.size()+"</strong> users.";
}

%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Admin: manage users</title>
        <link rel="stylesheet" type="text/css" href="../styles/style.css">
        <script type="text/javascript" src="../javascripts/users.js"></script>
    </head>
    <body>
        
        <h1>Manage users</h1>
        
        <p><a href="home.jsp">&laquo; Return to admin home</a></p>
        
        <p><% out.print(usersMsg); %></p>
        
        <% 
        // Only show table if there are users (should be)
        if (users.size() > 0) {
        %>
        <form action="processManageUsers.jsp" method="post">
            <table id="users_table">
                <tr>
                    <th class="thin_column"></th>
                    <th>User Name</th>
                    <th>Full Name</th>
                    <th>Affiliation</th>
                    <th>Email</th>
                    <th>Is admin?</th>
                </tr> 
                
                <% 
                Iterator it = users.iterator();
                UserRecord user;
                
                while(it.hasNext()) {
                user = (UserRecord)it.next();
                
                String abbrAffiliation = user.affiliation;
                
                if (abbrAffiliation.length() > 33) {
                    abbrAffiliation = abbrAffiliation.substring(0, 30)+"...";
                }
                
                out.println("<tr>");
                out.println("<td><input type=\"checkbox\" id=\"user_"+user.id+"\" name=\"user_"+user.id+"\" /></td>");
                out.println("<td>"+user.name+"</td>");
                out.println("<td>"+user.firstName+" "+user.lastName+"</td>");
                out.println("<td>"+abbrAffiliation+"</td>");
                out.println("<td>"+user.email+"</td>");
                out.println("<td>"+ViewUtil.capitalize(user.isAdmin)+"</td>");
                out.println("</tr>");
                }
                %>
                
                
            </table>
            <p><a href="none" onclick="return selectAll();"><strong>select all</strong></a> &nbsp; <a href="none" onclick="return deselectAll();"><strong>deselect all</strong></a></p>
            
            <input type="submit" id="remove" name="remove" value="Remove selected users" class="submit" onclick="return verifyRemove();" />
            <p class="submit">The above action will <strong>remove</strong> the users without an email follow-up.</p>
        </form>
        <%
        } // If users, show table (if none, an exception was thrown)
        %>
    </body>
</html>