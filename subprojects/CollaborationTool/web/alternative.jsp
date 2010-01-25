@docsHeader

<STYLE>
.indent {
	margin-left: 30px;
}
</STYLE>

<h1>Tranche Collaboration Tool (<a href="index.jsp">Home</a>)</h1>

<h2>Setting Up On Another Server</h2>
<ol style="list-style-type: decimal;">
	<li><a href="#download">Download</a></li>
	<li><a href="#extract">Extract Files</a></li>
	<li><a href="#edit">Edit JNLP File</a></li>
	<li><a href="#link">Link</a></li>
	<li><a href="index.jsp#customize">Continue With the Setup Process</a></li>
</ol>

<h3><a name="download"></a>Download Current Version</h3>
<DIV CLASS="indent">
	<p><a href="ProteomeCommons.org-Tranche-Collab.zip">Download</a> the ZIP file that contains all of the libraries that will be needed to run the 
	Collaboration Tool. You should have the following file structure within your ZIP file:</p>
	
	<DIV CLASS="indent">
		ProteomeCommons.org-Tranche-Collab.jnlp
		lib/ProteomeCommons.org-Collab.jar
		lib/ProteomeCommons.org-Tranche.jar
		lib/ProteomeCommons.org-IO.jar
		lib/ProteomeCommons.org-JAF.jar
		lib/ProteomeCommons.org-Tags.jar
		lib/bcprov-jdk15-130.jar
		lib/commons-httpclient-3.0-rc4.jar
		lib/commons-logging.jar
		lib/commons-codec-1.3.jar
		lib/email-util.jar
	</DIV>
	
	<h3><a name="extract"></a>Extract Files from ZIP</h3>
	<p>Extract the files from the ZIP into the desired directory. You can change the location of the <i>/lib/</i> directory, but these changes will need
	to be reflected in the <i>ProteomeCommons.org-Tranche-Collab.jnlp</i> file. For the remainder of the example, we will assume that the file structure is
	exactly the same as it was in the ZIP file.</p>
	
	<h3><a name="edit"></a>Edit the JNLP File</h3>
	<p>Below is a copy of the <i>ProteomeCommons.org-Tranche-Collab.jnlp</i> file that will be used to launche the tool. Every set of brackets <i><b>{...}</b></i> 
	signifies a place where you need to change the text. Don't forget to remove the brackets!</p>
	
	<p style="paddin-left: 5px; font-family: "Courier New", Courier, mono; text-indent: 0; background-color: #DDDDDD; text-align: left;">
	&lt;?xml version=&quot;1.0&quot; encoding=&quot;utf-8&quot;?&gt;<BR>
	&lt;jnlp spec=&quot;1.0+&quot; codebase=&quot;<b>{location of the folder this JNLP file is in}</b>&quot;&gt;<BR>
	&nbsp;&nbsp;&lt;information&gt;<BR>
	&nbsp;&nbsp;&nbsp;&nbsp;&lt;title&gt;ProteomeCommons.org Tranche Collaboration Tool (Build: @buildNumber)&lt;/title&gt;<BR>
	&nbsp;&nbsp;&nbsp;&nbsp;&lt;vendor&gt;ProteomeCommons.org&lt;/vendor&gt;<BR>
	&nbsp;&nbsp;&nbsp;&nbsp;&lt;homepage href=&quot;@baseURL&quot;/&gt;<BR>
	&nbsp;&nbsp;&nbsp;&nbsp;&lt;description&gt;A tool for adding data to any ProteomeCommons.org Tranche network.&lt;/description&gt;<BR>
	&nbsp;&nbsp;&lt;/information&gt;<BR>
	&nbsp;&nbsp;&lt;security&gt;<BR>
	&nbsp;&nbsp;&nbsp;&nbsp;&lt;all-permissions/&gt;<BR>
	&nbsp;&nbsp;&lt;/security&gt;<BR>
	&nbsp;&nbsp;&lt;resources&gt;<BR>
	&nbsp;&nbsp;&nbsp;&nbsp;&lt;j2se version=&quot;1.5+&quot; max-heap-size=&quot;256m&quot;/&gt;<BR>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;jar href=&quot;<b>{location of the library folder}</b>/ProteomeCommons.org-Collab.jar&quot;/&gt;<BR>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;jar href=&quot;<b>{location of the library folder}</b>/ProteomeCommons.org-Tranche.jar&quot;/&gt;<BR>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;jar href=&quot;<b>{location of the library folder}</b>/ProteomeCommons.org-IO.jar&quot;/&gt;<BR>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;jar href=&quot;<b>{location of the library folder}</b>/ProteomeCommons.org-JAF.jar&quot;/&gt;<BR>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;jar href=&quot;<b>{location of the library folder}</b>/ProteomeCommons.org-Tags.jar&quot;/&gt;<BR>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;jar href=&quot;<b>{location of the library folder}</b>/bcprov-jdk15-130.jar&quot;/&gt;<BR>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;jar href=&quot;<b>{location of the library folder}</b>/commons-httpclient-3.0-rc4.jar&quot;/&gt;<BR>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;jar href=&quot;<b>{location of the library folder}</b>/commons-logging.jar&quot;/&gt;<BR>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;jar href=&quot;<b>{location of the library folder}</b>/commons-codec-1.3.jar&quot;/&gt;<BR>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;jar href=&quot;<b>{location of the library folder}</b>/email-util.jar&quot;/&gt;<BR>
	&nbsp;&nbsp;&lt;/resources&gt;<BR>
	&nbsp;&nbsp;&lt;application-desc main-class=&quot;org.proteomecommons.tranche.collaboration.Wizard&quot;&gt;<BR>
	&nbsp;&nbsp;&nbsp;&nbsp;&lt;argument&gt;--laf&lt;/argument&gt;&lt;argument&gt;<b>{location of the look and feel configuration file}</b>&lt;/argument&gt;<BR>
	&nbsp;&nbsp;&nbsp;&nbsp;&lt;argument&gt;--usage&lt;/argument&gt;&lt;argument&gt;<b>{location of the usage configuration file}</b>&lt;/argument&gt;<BR>
	&nbsp;&nbsp;&nbsp;&nbsp;&lt;argument&gt;--structure&lt;/argument&gt;&lt;argument&gt;<b>{location of the structure configuration file}</b>&lt;/argument&gt;<BR>
	&nbsp;&nbsp;&nbsp;&nbsp;&lt;argument&gt;--servers&lt;/argument&gt;&lt;argument&gt;<b>{location of the servers list configuration file}</b>&lt;/argument&gt;<BR>
	&nbsp;&nbsp;&nbsp;&nbsp;&lt;argument&gt;--tags&lt;/argument&gt;&lt;argument&gt;<b>{location of the tags list configuration file}</b>&lt;/argument&gt;<BR>
	&nbsp;&nbsp;&lt;/application-desc&gt;<BR>
	&lt;/jnlp&gt;<BR>
	</p>
</DIV>

<h3><a name="link"></a>Create a Link</h3>
<DIV CLASS="indent">
	<p>Linking to <i>ProteomeCommons.org-Tranche-Collab.jnlp</i> launches the application with Java WebStart. No special formatting of the link is necessary. The
	tool is usable as-is. It will take all of the default parameters for Tranche until the configuration files are created.</p>
	<p><a href="index.jsp#customize">Continue With the Setup Process</a></p>
</DIV>

<br>

@docsFooter