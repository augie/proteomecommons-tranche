<%@include file="header.inc"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Apply: Tranche User Accounts</title>
        <link rel="stylesheet" type="text/css" href="styles/style.css">
        <script type="text/javascript" src="javascripts/user.js"></script>
    </head>
    <body>

    <h1>Tranche User Account Application</h1>
    
    <%
      // Handle any flash params signifying error
      if (ViewUtil.isParamSet(request.getParameter("flash"))) {
          out.println(ViewUtil.getFormattedFlashMessage(request.getParameter("flash")));
      }
    %>
    
    <p><a href="index.jsp">&laquo; Return to login page</a></p>
    
    <h2>Apply for new account</h2>
    
    <p>You will not be able to sign in until your account is <strong>approved</strong>. When your account is approved, you will be able to create a user file. We review all pending applications weekly following our Monday developer meeting as well as following our <a href="http://www.proteomecommons.org/dev/dfs/webcasts/index.html" target="_blank">user meetings</a>.</p>
    
    <p>If you apply but don't receive a timely response (or you need immediate access), you should <a href="http://www.proteomecommons.org/dev/dfs/#contact" target="_blank">contact us</a>.</p>
    <form action="processSignup.jsp" method="post">
        
        <label for="first">First name</label>
        <input type="text" name="first" id="first" />
        
        <label for="last">Last name</label>
        <input type="text" name="last" id="last" />
        
        <label for="name">User name <a href="none" onclick="return whatIsUserName();">What is this?</a></label>
        <input type="text" name="name" id="name" />
        
        <label for="password">Password (Type twice)</label>
        <input type="password" name="password1" id="password1" />
        <input type="password" name="password2" id="password2" />
        
        <label for="email">Email</label>
        <input type="text" name="email" id="email" />
        
        <label for="affiliation">Affiliation <a href="none" onclick="return whatIsAffiliation();">What is this?</a></label>
        <input type="text" id="affiliation" name="affiliation" />
        
        <label for="description">Why are you interested in Tranche?</label>
        <textarea id="description" name="description"></textarea>

        <label for="captcha">Are you human?</label>
        <input type="text" id="captcha" name="captcha" />
        <%
        // Construct the captchas object (Default Values)
        CaptchasDotNet captchas = new captchas.CaptchasDotNet(
          request.getSession(true),     // Ensure session
          "tranche",                       // client
          "t9z75m55Ox2bv427qXGuZ07CKGnJoMiVrpHwRRCn"                      // secret
          );
        // Construct the captchas object (Extended example)
        // CaptchasDotNet captchas = new captchas.CaptchasDotNet(
        //  request.getSession(true),     // Ensure session
        //  "demo",                       // client
        //  "secret",                     // secret
        //  "01",                         // alphabet
        //  16,                           // letters
        //  500,                          // width
        //  80                            // height
        //  );
        %>
        <%--
           % it's possible to set a random in captchas.image("xyz"),
           % captchas.imageUrl("xyz") and captchas.audioUrl("xyz").
           % This is only needed at the first request
        --%>
        <%= captchas.image() %><br>
        <a href="<%= captchas.audioUrl() %>">Phonetic spelling (mp3)</a>
        
        <input type="submit" class="submit" value="Apply" onclick="return verify();" />
        
    </form>
    
    </body>
</html>
