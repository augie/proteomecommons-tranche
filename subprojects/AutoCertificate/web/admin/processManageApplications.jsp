<%@include file="../header.inc"%>
<%--
* Processes application requests
--%>
<%
if (!ControllerUtil.verifyLogin(session.getAttribute("username"),session.getAttribute("password")) || !ControllerUtil.isUserAdmin(session.getAttribute("username"))) {
    response.sendRedirect("../index.jsp?You must login as an admin first.");
    return;
}

final byte NULL = 0,
        APPROVE = 1,
        REQUEST = 2,
        REMOVE = 3;

int action = NULL;

// Keep a collection of all ids
Set ids = new HashSet();

if (request.getParameter("approve") != null) {
    action = APPROVE;
} else if (request.getParameter("request") != null) {
    action = REQUEST;
} else if (request.getParameter("remove") != null) {
    action = REMOVE;
} else {
    // Nothing to do!
    response.sendRedirect("home.jsp?flash=Nothing to do. Did you request to do something with applications?");
    return;
}

Enumeration paramNames = request.getParameterNames();

while(paramNames.hasMoreElements()) {
    // Need to find, parse out and collect and selected ids
    String param = paramNames.nextElement().toString();
    if (param.startsWith("application_")) {
        ids.add(Integer.decode(param.substring(12).trim()));
    }
}

switch(action) {
    case APPROVE:
        ControllerUtil.approveApplications(ids);
        response.sendRedirect("home.jsp?flash=Approved "+ids.size()+" application(s)");
        break;
    case REQUEST:
        ControllerUtil.requestMoreInfoForApplications(ids);
        response.sendRedirect("home.jsp?flash=Requested more info from "+ids.size()+" applicant(s)");
        break;
    case REMOVE:
        ControllerUtil.removeApplications(ids);
        response.sendRedirect("home.jsp?flash=Removed "+ids.size()+" application(s)");
        break;
    default:
        throw new Exception("Unrecognized state: switch doesn't recognize action");
}

%>