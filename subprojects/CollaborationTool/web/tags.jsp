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

<h2>Tags Configuration File Parameters</h2>
<DIV CLASS="indent">
	<p>This configuration file is only necessary if you have chosen to register your upload with ProteomeCommons.org.
	All tags must be limited to a single line of text. If you want to have a line break or return character as a tag value, 
	put <i>\n</i> instead. Tag names cannot have these return characters.</p>
	
	<p>An example tags configuration file:</p>
	
	<DIV CLASS="indent" STYLE="font-family: Courier, mono;">
		Tranche:Type 1 = My Collaboration<br>
		Another Tag = Some other tag information.<br>
	</DIV>
</DIV>
@docsFooter