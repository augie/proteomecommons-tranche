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

<h2>Look and Feel Configuration File Parameters</h2>
<DIV CLASS="indent">
	<p>Contains all visual parameters. None of the parameters are required. Note for all color parameters: colors can be described 
	in two different ways. Colors can either be the name of a very common color such as green, red, blue, or black, or they can be 
	any hexadecimal number between 000000 (black) and FFFFFF (white). You have a full range of colors to work with.</p>
	
	<DIV CLASS="indent">
		<b>First Screen of the Collaboration Tool</b>
		<IMG SRC="collab-tool-1.jpg">
	</DIV>
	
	<BR>
	<BR>
	
	<ul>
		<li>width = <i>number</i></li>
		<p>The width in pixels of the frame.</p>
		<li>height = <i>number</i></li>
		<p>The height in pixels of the frame.</p>
		<li>icon = <i>URL to the location of the icon</i></li>
		<p>The image that appears in the upper left hand corner of the frame.</p>
		<li>logo = <i>URL to the location of the logo</i></li>
		<p>The main logo that appears at the top of the frame. This logo should be 485 pixels wide by 55 pixels high.</p>
		<li>tranche logo color = <i>name or number of a color</i></li>
		<p>The base color for the growing fractal tree on the right side of the tool.</p>
		<li>tranche logo color 2 = <i>name or number of a color</i></li>
		<p>The secondary color for the growing fractal tree on the right side of the tool.</p>
		<li>tranche logo colors to use = <i>1 or 2</i></li>
		<p>Whether the tranche logo should use the first tranche logo color or both tranche logo colors to draw the tree. If only one is used, the tranche logo will automatically come up with a second one.</p>
		<li>menu background color = <i>name or number of a color</i></li>
		<p>The color of the menu that appears at top of the frame.</p>
		<li>menu selection background color = <i>name or number of a color</i></li>
		<p>The color that appears behind a selected menu.</p>
		<li>menu text color = <i>name or number of a color</i></li>
		<p>The color of the text in the menu bar at the top of the frame.</p>
		<li>menu selection text color = <i>name or number of a color</i></li>
		<p>The color of the menu text at the top when they are selected.</p>
		<li>trim color = <i>name or number of a color</i></li>
		<p>The color of the line under the menu bar and the selection background color of the menus at the top.</p>
		<li>main background color = <i>name or number of a color</i></li>
		<p>The color of the main panel's background.</p>
	</ul>
	
	<BR>
	
	<p>An example look and feel configuration file:</p>
	
	<DIV CLASS="indent" STYLE="font-family: Courier, mono;">
		logo = http://www.somewhere.com/img<br>
		menu background color = red<br>
		menu text color = FFFFFF<br>
		trim color = FF3333<br>
	</DIV>
	
	<BR>
	
</DIV>

@docsFooter