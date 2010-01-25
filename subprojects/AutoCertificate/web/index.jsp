<%@include file="header.inc"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Login: Tranche User Accounts</title>
        <link rel="stylesheet" type="text/css" href="styles/style.css">
        <script type="text/javascript" src="javascripts/library.js"></script>
    </head>
    <body>

    <h1>Tranche User Accounts</h1>
    
    <%
      // Handle any flash params signifying error
      if (ViewUtil.isParamSet(request.getParameter("flash"))) {
          out.println(ViewUtil.getFormattedFlashMessage(request.getParameter("flash")));
      }
    %>
    
    <p>You must login to get a user file to <strong>upload to Tranche</strong>. If you do not have an account, you can <a href="signup.jsp"><strong>apply for an account</strong></a> in less than 30 seconds!</p>
    
    <h2>Login</h2>
    <form action="login.jsp" method="post">
        <label for="name">User name</label>
        <input type="text" name="name" id="name" />
        
        <label for="password">Password</label>
        <input type="password" name="password" id="password" />
        
        <input type="submit" value="login" class="submit" />
        
        <ul>
            <li>Don't have an account? <a href="signup.jsp"><strong>Apply</strong> &raquo;</a></li>
            <li>Are you having <a href="msie6-security.html" onclick="return smallPopUp('msie6-security.html');" target="_blank">problems logging in with <strong>Internet Explorer 6</strong></a>?</li>
        </ul>
    </form>
    
    </body>
</html>
