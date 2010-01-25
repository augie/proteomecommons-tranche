<%@include file="header.inc"%>
<%!
  String name = null,
         password = null,
         email = null,
         description = null,
         firstName = null,
         lastName = null,
         affiliation = null,
         captcha = null;
%>
<%
  try {
      name = request.getParameter("name").toString();
      password = request.getParameter("password1").toString();
      email = request.getParameter("email").toString();
      description = request.getParameter("description").toString();
      firstName = request.getParameter("first").toString();
      lastName = request.getParameter("last").toString();
      affiliation = request.getParameter("affiliation").toString();

      boolean isCaptchaRequired = true;
      try {
        captcha = request.getParameter("captcha").toString();
      } catch (Exception ex) {
        // If not found, not required!
        isCaptchaRequired = false;
      }

      if (isCaptchaRequired) {
        // Construct the captchas object
        // Use same settings as in query.jsp
        CaptchasDotNet captchas = new captchas.CaptchasDotNet(
          request.getSession(true),     // Ensure session
          "tranche",                       // client
          "t9z75m55Ox2bv427qXGuZ07CKGnJoMiVrpHwRRCn"                      // secret
        );

        // Check captcha
        switch (captchas.check(captcha)) {
          case 's':
            response.sendRedirect("signup.jsp?flash=Session seems to be timed out or broken. Please try again or report error to administrator.");
            return;
          case 'm':
            response.sendRedirect("signup.jsp?flash=Every captcha can only be used once. The given captcha has already been used.");
            return;
          case 'w':
            response.sendRedirect("signup.jsp?flash=You entered the wrong captcha.");
            return;
        }
      }

      if (!DatabaseUtil.isUsernameAvailable(name)) {
          response.sendRedirect("signup.jsp?flash=The username "+name+" is already in use. Please select a different name.");
          return;
      }

      DatabaseUtil.createUserEntry(name,password,email,false,firstName,lastName,affiliation);

      try {
        ControllerUtil.sendEmailRegardingSignup(email);
      } catch (Exception e) { /* No response, go on */ }

      try {
        ControllerUtil.sendEmailToAdminAboutRequest(name,email,description);
      } catch (Exception e) { /* No response, go on */ }

      response.sendRedirect("index.jsp?flash=Your application has been submitted. You should hear back from us soon.");
  } catch(Exception e) {
      ControllerUtil.registerException(e);
      response.sendRedirect("error.jsp");
  }

%>
