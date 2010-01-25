*******************************************
**	  TRANCHE PROXY UPLOAD README      **
*******************************************

Example post form: http://www.proteomecommons.org/data/upload/form.jsp

POST Location: http://www.proteomecommons.org/data/upload/
POST Parameters:
	
	*******************
	REQUIRED PARAMETERS
	*******************

	Name: uzf
	Type: file
	Description: User zip file. The "*.zip.encrypted" file that is used to denote a user in a Tranche upload.

	Name: uzfPassphrase
	Type: text
	Description: The passphrase used to unencrypt the user zip file.

	Name: upload
	Type: file
	Description: The file to be uploaded to Tranche. This should be a ZIP or TAR file if this is a directory upload, or can simply be a file.

	Name: title
	Type: text
	Description: The title of project that is being uploaded.

	Name: description
	Type: text
	Description: The description of the project that is being uploaded.

	*******************
	OPTIONAL PARAMETERS
	*******************
	
	Name: server1 [, server2, etc...]
	Type: text
	Description: Any text field that starts with "server" will be added to a list of Tranche servers to be uploaded to.

	Name: uploadAsDirectory
	Type: boolean [String "true" or "false"]
	Description: Whether or not this upload is a compressed directory file that should be exploded before uploading. Default is true.

	Name: register
	Type: boolean [String "true" or "false"]
	Description: Whether or not this upload should be registered with the ProteomeCommons.org network. Default is true.

	Name: remoteRep
	Type: boolean [String "true" or "false"]
	Description: Whether or not this upload should use remote replication. Default is false.

	Name: skipFiles
	Type: boolean [String "true" or "false"]
	Description: Whether or not this upload should skip files if they are already online. Default is false.
	
	Name: skipChunks
	Type: boolean [String "true" or "false"]
	Description: Whether or not this upload should skip chunks if they are already online. Default is true.

Returns: text/plain containing only

	REQUEST = [requestCode]

Keep this requestCode. It will be used to get update information on this upload.


**********************************
GETTING AN UPDATE ON UPLOAD STATUS
**********************************

GET Location: http://www.proteomecommons.org/data/upload/update.jsp
GET Parameter:

	Name: requestCode
	Description: the request code returned from the initial upload request.

Returns: text/plain containing
	
	if the upload hasn't completed...

		STATUS = INCOMPLETE
		DATA TO UPLOAD = [size of the data being uploaded]
		DATA UPLOADED = [amount of data that has been uploaded]

	if the upload has completed...
	
		STATUS = COMPLETE
		HASH = [hash]

	if the upload failed...
	
		STATUS = FAILED
		EXCEPTION: [the reason for failure]