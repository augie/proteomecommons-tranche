<%@include file="header.inc"%>
<%--
* Process login. Route admin/user traffic differently.
--%>
<%!
String name = null, password = null;
%>

<%
name = request.getParameter("name");
password = request.getParameter("password");

try {
    if (ControllerUtil.verifyLogin(name,password)) {
        
        // Use this oppurtunity to send a warning message to admin. Only done
        // once, so only one victim.
        if (ControllerUtil.isMasterCertExpiringWithinDuration(1000*60*60*24*7*3)) {
            ControllerUtil.sendEmailToAdminRegardingPendingCertExpiration();
        }
        
        // Start the session
        session.setAttribute("username",name);
        session.setAttribute("password",password);
        
        if (ControllerUtil.isUserAdmin(name)) {
            response.sendRedirect("admin/home.jsp");
            return;
        }
        else {
            response.sendRedirect("user/home.jsp");
            return;
        }
        
    } else {
        
        // Discourage brute force attacks
        Thread.sleep(3000);
        
        response.sendRedirect("index.jsp?flash=Either your application is still being reviewed or your login information was incorrect");
        return;
    }
} catch (Exception e) {
    ControllerUtil.registerException(e);
    response.sendRedirect("error.jsp");
}
%>