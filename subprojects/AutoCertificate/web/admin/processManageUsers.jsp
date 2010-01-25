<%@include file="../header.inc"%>
<%--
* Processes user messages set, removed or modified by admin
--%>
<%
if (!ControllerUtil.verifyLogin(session.getAttribute("username"),session.getAttribute("password")) || !ControllerUtil.isUserAdmin(session.getAttribute("username"))) {
    response.sendRedirect("../index.jsp?You must login as an admin first.");
    return;
}

// Collect ids for action from param
Set ids = new HashSet();

final byte NULL = 0,
        REMOVE = 1;
        
byte action = NULL;

if (request.getParameter("remove") != null) {
    action = REMOVE;
} else {
    // Nothing to do!
    response.sendRedirect("home.jsp?flash=Nothing to do. Did you request to do something with users?");
    return;
}

Enumeration paramNames = request.getParameterNames();

while(paramNames.hasMoreElements()) {
    // Need to find, parse out and collect and selected ids
    String param = paramNames.nextElement().toString();
    if (param.startsWith("user_")) {
        ids.add(Integer.decode(param.substring(5).trim()));
    }
}

// Perform the action
switch(action) {
    case REMOVE:
        ControllerUtil.removeUsers(ids);
        response.sendRedirect("home.jsp?flash="+ids.size()+" users removed.");
        break;
    default:
        throw new Exception("Unknown action in switch, should never get here!");
}

%>