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

<h2>Usage Configuration File</h2>
<DIV CLASS="indent">
	<p>Contains all upload parameters. None of the parameters are required. Each parameter must be limited to a single line of text. 
	If you want to have a line break or return in a text field, put "\n" instead.</p>
	
	<ul>
		<li>collaboration name = <i>anything</i></li>
		<p>The name of your organization.</p>
		<li>collaboration url = <i>a url to your homepage</i></li>
		<p>The URL of your organization.</p>
		<li>help url = <i>a url to your help page</i></li>
		<p>The URL on your website with some helpful information.</p>
		<li>show home button = <i>true/false</i></li>
		<p>Whether or not you want the user to be open the main Tranche tool to browse the network. (true by default)</p>
		<li>show servers = <i>true/false</i></li>
		<p>Whether or not you want the user to be able to change the Tranche servers they will be uploding to. (true by default)</p>
		<li>use core servers = <i>true/false</i></li>
		<p>Whether or not you want to use the ProteomeCommons.org Tranche network of Tranche servers. (true by default)</p>
		<li>show tags = <i>true/false</i></li>
		<p>Whether to let the user open the tags panel for adding annotations to their upload. This is only useful if you are also registering your uploads with ProteomeCommons.org. (true by default)</p>
		<li>default license = <i>public now/public later/custom</i></li>
		<p>Which license should be selected by default. If a license is selected, the user will not have the ability to change the license. (no default)</p>
		<li>custom license text = <i>anything</i></li>
		<p>The text for your custom license, should you choose to use a custom license.</p>
		<li>custom license encrypted = <i>true/false</i></li>
		<p>Whether the custom license should be encrypted. (false by default)</p>
		<li>log upload = <i>true/false</i></li>
		<p>Whether a log should be created for this upload. (true by default)</p>
		<li>register = <i>true/false</i></li>
		<p>Whter the upload should be registered with ProteomeCommons.org. (true by default)</p>
		<li>show register = <i>true/false</i></li>
		<p>Whether the user should have the option to register their upload. (true by default)</p>
		<li>skip chunks = <i>true/false</i></li>
		<p>Whether data chunks should not be uploaded if they already exist on the network. Having this option set for true can significantly decrease the amount of time it takes to upload. (true by default)</p>
		<li>show skip chunks = <i>true/false</i></li>
		<p>Whether the user should have the option to skip data chunks. (true by default)</p>
		<li>skip files = <i>true/false</i></li>
		<p>Whether entire files should not be uploaded if the already exist on the network. This option can be generally unreliable, so keeping it set to false is a good idea. (false by default)</p>
		<li>show skip files = <i>true/false</i></li>
		<p>Whether the user should have the option to skip files. (true by default)</p>
		<li>remote replication = <i>true/false</i></li>
		<p>When this is turned off, 3 copies of your data are sent to the network. When it is on, only 1 copy is sent and the servers are left to copy data chunks in the background. It is safer to send the 3 copies yourself, but turning this off can give much faster uploads. (false by default)</p>
		<li>show remote replication = <i>true/false</i></li>
		<p>Whether the user should have the option to use remote replication. (true by default)</p>
		<li>intro message = <i>anything</i></li>
		<p>When the tool is first opened, a message will appear with this text.</p>
	</ul>
	
	<br>
	
	<p>An example usage configuration file:</p>
	
	<DIV CLASS="indent" STYLE="font-family: Courier, mono;">
		collaboration name = My Collaboration<br>
		collaboration url = http://www.my.collab.com<br>
		register = true<br>
		show register = false<br>
	</DIV>
</DIV>
@docsFooter