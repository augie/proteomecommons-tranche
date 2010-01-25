@docsHeader

<STYLE>
.indent {
	margin-left: 30px;
}
</STYLE>

<h1>Tranche Collaboration Upload Tool</h1>
<ul>
	<li><a href="#what">What is it?</a></li>
	<li><a href="#example">Example</a></li>
	<li><a href="#make">Make Your Own</a></li>
</uL>

<hr id="navigation-rule" />

<a name="what"></a><h2>What is it?</h2>
<DIV CLASS="indent">
	<p>A tool has been made for groups to customize the standard Tranche Upload Tool for the group's use. There are a full range of features that are
	available for customization. These features include:</p>
	<ul>
		<li>Servers to be used</li>
		<li>Upload parameters (remote replication, skipping files or chunks, registration with ProteomeCommons.org)</li>
		<li>Look and feel (logo, icon, colors)</li>
		<li>Upload license</li>
		<li>etc...</li>
	</ul>
</DIV>

<hr id="navigation-rule" />

<a name="example"></a><h2>Example Collaboration Tool</h2>
<DIV CLASS="indent">
	<p>We made a collaboration tool for the NCI-CPTAC Tranche collaboration.</p>
	<DIV CLASS="indent">
		<a href="http://tranche.proteomecommons.org/collab-tool.jsp?config=http://tranche.proteomecommons.org/examples/nci-cptac/master.conf">
			NCI-CPTAC Tranche Collaboration Tool<BR>
			<BR>
			<img src="collab-tool-cptac.jpg" border="0">
		</a>
	</DIV>
</DIV>

<hr id="navigation-rule" />

<a name="make"></a><h2>Make Your Own</h2>
<DIV CLASS="indent">
	<ol style="list-style-type: decimal;">
		<li><a href="#setUp">Set it Up</a></li>
		<li><a href="#customize">Customize</a></li>
		<li><a href="#information">Send Us Some Information</a></li>
	</ol>
	
	<br>
	
	<a name="setUp"></a><h3>Set Up</h3>
	<DIV CLASS="indent">
		<p>Setting up the ability to launche the Tranche Collaboration Tool is very simple. All you will need to do is create a hyperlink to
		a webpage on ProteomeCommons.org that will open up the program. The program that will open up is the most current version of the software.</p>
		<p>If you do not want to rely upon ProteomeCommons.org for the availability of your collaboration tool, you have the option of 
		<a href="alternative.jsp">setting up on another server</a>. This can be more tricky and the software will have to be manually updated, so
		we recommend linking to the webpage on ProteomeCommons.org.</p>
		<p>Link to: http://tranche.proteomecommons.org/collab-tool.jsp</p>
		<p>Pass in the arguments (all are optional):<br>
		<ul>
			<li><b>config</b> - The URL of the master configuration file, which contains the URL's of all of the following arguments.</li>
			<li><b>laf</b> - The URL of the look and feel configuration file.</li>
			<li><b>usage</b> - The URL of the usage configuration file.</li>
			<li><b>structure</b> - The URL of the upload structure configuration file.</li>
			<li><b>servers</b> - The URL of the servers configuration file.</li>
			<li><b>tags</b> - The URL of the tags file.</li>
		</ul>
		</p>
		<p>If you don't want to include a configuration file for any of the above, do not include the argument.</p>
		<p>Using the master configuration option, the final link would be in the following format: http://tranche.proteomecommons.org/collab-tool.jsp?config=<b>{URL}</b></i></p>
		<br>
		<b>Need help making your URL?</b>
		<FORM NAME="makeForm">
		<table width="100%" cellpadding="5" cellspacing="0" border="0">
			<tr><td>Master Configuration File URL: <INPUT TYPE="text" NAME="masterURL" SIZE="30"><INPUT TYPE="button" ONCLICK="document.makeForm.url.value = 'http://tranche.proteomecommons.org/collab-tool.jsp?config=' + document.makeForm.masterURL.value;" VALUE="Make Collab Tool URL"></td></tr>
			<tr><td><INPUT NAME="url" TYPE="button" STYLE="background-color: transparent; border: 0;" VALUE=""></td></tr>
		</table>
		</FORM>
		<br>
	</DIV>
	
	<BR>
	
	<a name="customize"></a><h3>Customize Your Collaboration Tool</h3>
	<DIV CLASS="indent">
		<ul>
			<li><a href="master.jsp">Master Configuration</a></li>
			<li><a href="laf.jsp">Look and Feel Configuration</a></li>
			<li><a href="usage.jsp">Usage Configuration</a></li>
			<li><a href="structure.jsp">Upload Structure Configuration</a></li>
			<li><a href="servers.jsp">Servers Configuration</a></li>
			<li><a href="tags.jsp">Tags Configuration</a></li>
		</ul>
	</DIV>
	
	<br>
	
	<a name="information"></a><h3>Send Us Some Information</h3>
	<DIV CLASS="indent">
		<p>If you are downloading this tool for use on your own private Tranche network, this option is not required.
		The main reason to send us information is so that we can set up your collaboration in our automated user certificate
		management system, which gives you the ability to upload to the ProteomeCommons.org Tranche network. If you want to 
		upload to the ProteomeCommons.org Tranche network, you will need to <a href="mailto:proteomecommons-tranche-dev@googlegroups.com">send us</a> the following information:</p>
		<ul>
			<li>Your general collaboration information (name, url, etc.)</li>
			<li>A list of user names and corresponding passphrases for each user.</li>
			<li>For each user, dates for when the user should be able to upload (start and end dates).</li>
		</ul>
	</DIV>
</DIV>

@docsFooter