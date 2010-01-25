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

<h2>Upload Structure Configuration File</h2>
<DIV CLASS="indent">
	<p>The upload structure config. file contains a list of entries that represent your lab's typical upload file structure. For example, let's say all of my raw data is stored in the following format:</p>
	
	<DIV CLASS="indent" STYLE="font-family: Courier, mono;">
	/topFolder1/subFolder1/data1.raw<br>
	/topFolder1/subFolder1/data2.raw<br>
	/topFolder1/subFolder2/data3.raw<br>
	/topFolder1/description.html<br>
	/topFolder2/subFolder3/data4.raw<br>
	</DIV>
	
	<BR>
	
	<p>With the above as the contents of our upload structure configuration file, the behavior of the collaboration tool will be the following:</p>
	
	<ol>
		<li>User selects the file <i>C:/user/home/directory/topFolder1/subFolder1/data1.raw</i></li>
		<li>Tool notices that <i>topFolder1/subFolder1/data1.raw</i> matches one of the entries in the configuration file.</li>
		<li>Tool asks the user whether they would rather upload <i>C:/user/home/directory/</i>, which is assumed to be the full data set, as opposed to a small part.</li>
		<li>User has the option to stick with their first selection or upload the selected directory instead.</li>
	</ol>
	
	<br>
	
	<DIV CLASS="indent">
		<b>Example image 1 - Selecting a file:</b><br>
		<img src="collab-tool-2.jpg">
		
		<br>
		<br>
		
		<b>Example image 2 - Given that there is an entry in the upload structure configuration file with the value <i>/data/mydata.raw</i>, this message will pop up:</b><br>
		<img src="collab-tool-3.jpg">
	</DIV>
</DIV>

@docsFooter