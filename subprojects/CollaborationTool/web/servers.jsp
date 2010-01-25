@docsHeader

<STYLE>
.indent {
	margin-left: 30px;
}
</STYLE>

<h1>Tranche Collaboration Upload Tool (<a href="index.jsp">Home</a>)</h1>

<h3>Customizing Your Collaboration Tool</h3>
<ul>
	<li><a href="master.jsp">Master Configuration</a></li>
	<li><a href="laf.jsp">Look and Feel Configuration</a></li>
	<li><a href="usage.jsp">Usage Configuration</a></li>
	<li><a href="structure.jsp">Upload Structure Configuration</a></li>
	<li><a href="servers.jsp">Servers Configuration</a></li>
	<li><a href="tags.jsp">Tags Configuration</a></li>
</ul>

<br>

<h2>Servers Configuration File Parameters</h2>
<DIV CLASS="indent">
	<p>The servers configuration file contains a return-separated list of servers that should be added to the servers panel. 
	As a user logs into the system, all servers that are contained within the the servers panel will be asked what the users's
	status is on that server. If the user is found to have "Read/Write" priveleges or higher on a server, that server will
	automatically be selected.</p>
	
	<p>Be sure to include <i>tranche://</i> at the start of every server, and the port number at the end. Usually the port number
	is 443, so <i>:443</i> would be appended to the IP address or domain name of the server.</p>
	
	<p>An example servers configuration file:</p>
	
	<DIV CLASS="indent" STYLE="font-family: Courier, mono;">
		tranche://124.524.424.44:443<BR>
		tranche://proteomecommons.org:443<BR>
		tranche://www.proteomecommons.org:1045<BR>
	</DIV>
	
	<BR>
	
</DIV>

@docsFooter