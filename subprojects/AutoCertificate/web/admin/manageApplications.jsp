<%@include file="../header.inc"%>
<%--
* Manage user applications to open a Tranche user account.
--%>
<%
if (!ControllerUtil.verifyLogin(session.getAttribute("username"),session.getAttribute("password")) || !ControllerUtil.isUserAdmin(session.getAttribute("username"))) {
    response.sendRedirect("../index.jsp?You must login as an admin first.");
    return;
}
%>
<%!
List applications = null;
String applicationMsg = null;
%>
<% 
// Get all applications
applications = ViewUtil.sortUsersByName(DatabaseUtil.getUserApplications());

if (applications.size() == 0) {
    applicationMsg = "There aren't any user applications requiring your approval.";
} else if (applications.size() == 1) {
    applicationMsg = "There is <strong>1</strong> application pending review.";
} else {
    applicationMsg = "There are <strong>"+applications.size()+"</strong> applications pending review.";
}

%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Admin: manage applications</title>
        <link rel="stylesheet" type="text/css" href="../styles/style.css">
        <script type="text/javascript" src="../javascripts/applications.js"></script>
    </head>
    <body>
        
        <h1>Manage applications</h1>
        
        <p><a href="home.jsp">&laquo; Return to admin home</a></p>
        
        <p><% out.print(applicationMsg); %></p>
        
        <% 
        // Only show table if there are applications
        if (applications.size() > 0) {
        %>
        <form action="processManageApplications.jsp" method="post">
            <table id="applications_table">
                <tr>
                    <th class="thin_column"></th>
                    <th>User Name</th>
                    <th>Full Name</th>
                    <th>Affiliation</th>
                    <th>Email</th>
                </tr> 
                
                <% 
                Iterator it = applications.iterator();
                UserRecord user;
                
                while(it.hasNext()) {
                user = (UserRecord)it.next();
                
                String abbrAffiliation = user.affiliation;
                
                if (abbrAffiliation.length() > 33) {
                    abbrAffiliation = abbrAffiliation.substring(0, 30)+"...";
                }
                
                out.println("<tr>");
                out.println("<td><input type=\"checkbox\" id=\"application_"+user.id+"\" name=\"application_"+user.id+"\" /></td>");
                out.println("<td>"+user.name+"</td>");
                out.println("<td>"+user.firstName+" "+user.lastName+"</td>");
                out.println("<td>"+abbrAffiliation+"</td>");
                out.println("<td>"+user.email+"</td>");
                out.println("</tr>");
                }
                %>
                
                
            </table>
            <p><a href="none" onclick="return selectAll();"><strong>select all</strong></a> &nbsp; <a href="none" onclick="return deselectAll();"><strong>deselect all</strong></a></p>
            
            <input type="submit" id="approve" name="approve" value="Approve selected applications" class="submit" onclick="return verifyApprove();" />
            <p class="submit">The above action will <strong>approve</strong> the selected users for immediate use.</p>
            
            <input type="submit" id="request" name="request" value="Request more info for selected applications" class="submit" onclick="return verifyRequest();" />
            <p class="submit">The above action will <strong>email the applicant</strong> requesting more information.</p>
            
            <input type="submit" id="remove" name="remove" value="Remove selected applications" class="submit" onclick="return verifyRemove();" />
            <p class="submit">The above action will <strong>remove</strong> the application without an email follow-up.</p>
        </form>
        <%
        } // If applications, show table
        %>
    </body>
</html>