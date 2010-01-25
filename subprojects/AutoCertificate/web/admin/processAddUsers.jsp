<%@include file="../header.inc"%><%--
* Processes new users added through addUsers.jsp
--%><%
if (!ControllerUtil.verifyLogin(session.getAttribute("username"),session.getAttribute("password")) || !ControllerUtil.isUserAdmin(session.getAttribute("username"))) {
    response.sendRedirect("../index.jsp?You must login as an admin first.");
    return;
}

boolean isDebug = false;

// If debugging, show text dump of parameters
if (isDebug) {
    response.setContentType("text/plain");
    out.println("DEBUG FLAG SET TO TRUE.");
    out.println("\n\nDUMPING PARAMETERS:");
    for (String name : (Set<String>)request.getParameterMap().keySet()) {
        out.println("* "+name+" => "+request.getParameter(name));
    }
    
    out.println("\n\nGOING TO DETERMINE GROUP INFORMATION:");
    
// Get the group
    String group = request.getParameter("group");
    
// If group is equal to CREATE, get the new group name
    if (group.trim().equals("CREATE")) {
        group = request.getParameter("new_group");
    }
    
// If there was an error and no string, replace with '-'
    if (group.trim().equals("")) {
        group = "-";
    }
    
    out.println("* Group: "+group+" (is a group?: "+(!group.equals("-"))+")");
    
    out.println("\n\nGOING TO DETERMINE NEW USER INFORMATION:");
    
    Set<Integer> ids = new HashSet();
    for (String name : (Set<String>)request.getParameterMap().keySet()) {
        
// Some params are numbers, the rest aren't. Gather the numbers,
// which will be used to build the following other params
// * name_i
// * passphrase_i
// * passphrase_confirm_i
        try {
            ids.add(Integer.parseInt(name));
        } catch (NumberFormatException nfe) { /* nope */ }
    }
    
    String name, passphrase, passphrase_confirm;
    for (int id : ids) {
        name = request.getParameter("name_"+id);
        passphrase = request.getParameter("passphrase_"+id);
        passphrase_confirm = request.getParameter("passphrase_confirm_"+id);
        
        out.println("* name="+name+",passphrase="+passphrase+",confirm passphrase="+passphrase_confirm);
    }
    
// Done w/ output
    return;
}

// Not debugging, perform operations and redirect
// Get the group
String group = request.getParameter("group");

// If group is equal to CREATE, get the new group name
if (group.trim().equals("CREATE")) {
    group = request.getParameter("new_group");
}

// If there was an error and no string, replace with '-'
if (group.trim().equals("")) {
    group = "-";
}

long expiresOn = Long.parseLong(request.getParameter("expires_on"));

Set<Integer> ids = new HashSet();
for (String name : (Set<String>)request.getParameterMap().keySet()) {
    
// Some params are numbers, the rest aren't. Gather the numbers,
// which will be used to build the following other params
// * name_i
// * passphrase_i
// * passphrase_confirm_i
    try {
        ids.add(Integer.parseInt(name));
    } catch (NumberFormatException nfe) { /* nope */ }
}

// Assume that the Javascript caught all potential problems.
String name, passphrase, passphrase_confirm, email;

Set<Integer> userIds = new HashSet();
for (int id : ids) {
    name = request.getParameter("name_"+id);
    passphrase = request.getParameter("passphrase_"+id);
    passphrase_confirm = request.getParameter("passphrase_confirm_"+id);
    email = request.getParameter("email_"+id);
    
    DatabaseUtil.createUserEntry(name,passphrase,email,false,expiresOn,group);
    
    // Collect the user ids
    userIds.add(DatabaseUtil.getIdForUser(name));
}

// Approve the applications -- admin added them
ControllerUtil.approveApplications(userIds);

response.sendRedirect("index.jsp?flash=Successfully added.");
return;
%>