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

<h2>Master Configuration File</h2>
<DIV CLASS="indent">
	<p>Contains the urls to all configuration fiels. None of the urls are required. Each url must be limited to a single line of text.</p>
	
	<ul>
		<li>laf = <i>a url</i></li>
		<p>The complete URL to the look and feel configuration file.</p>
		<li>usage = <i>a url</i></li>
		<p>The complete URL to the usage configuration file.</p>
		<li>structure = <i>a url</i></li>
		<p>The complete URL to the upload structure configuration file.</p>
		<li>servers = <i>a url</i></li>
		<p>The complete URL to the servers configuration file.</p>
		<li>tags = <i>a url</i></li>
		<p>The complete URL to the tags configuration file.</p>
	</ul>
	
	<br>
	
	<p>An example master configuration file:</p>
	
	<DIV CLASS="indent" STYLE="font-family: Courier, mono;">
		laf = http://tranche.proteomecommons.org/examples/tool/lookAndFeel.conf<br>
		usage = http://tranche.proteomecommons.org/examples/tool/usage.conf<br>
		structure = http://tranche.proteomecommons.org/examples/tool/structure.conf<br>
		servers = http://tranche.proteomecommons.org/examples/tool/servers.conf<br>
		tags = http://tranche.proteomecommons.org/examples/tool/tags.conf<br>
	</DIV>
</DIV>
@docsFooter