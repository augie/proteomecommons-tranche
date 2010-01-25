<%@include file="header.inc"%>
<% 
session.invalidate();
response.sendRedirect("index.jsp?flash=User successfully logged out");
%>