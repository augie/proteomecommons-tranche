<%@include file="header.inc"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Error</title>
        <link rel="stylesheet" type="text/css" href="styles/todo.css">
    </head>
    <body>

    <h1>Error</h1>
    
    <%
      // Create a message. Use registered exception or get a default message.
      String msg;
      
      // Create message from registered exception
      if (ControllerUtil.getRegisteredException() != null) {
          msg = ViewUtil.getFormattedException(ControllerUtil.getRegisteredException(),true);
      }
      // Fall back on flash message
      else if (ViewUtil.isParamSet(request.getParameter("flash"))) {
          msg = ViewUtil.getFormattedFlashMessage(request.getParameter("flash"));
      }
      // Fall back on default
      else {
          msg = "No exception was registered. Please offer a good description of the activity when filing the bug report.";
      }
      
      out.println("<p>An exception has occurred. Please open a bug report with a description of what you are doing and the following message:</p>");
      out.println("<div id=\"exception\">"+msg+"</div>");
    %>
    
    <p><a href="index.jsp">&laquo; Return home</a></p>
    
    </body>
</html>
