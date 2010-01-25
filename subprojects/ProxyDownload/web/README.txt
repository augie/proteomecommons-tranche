*******************************************
**	 TRANCHE PROXY DOWNLOAD README     **
*******************************************

GET Location: http://www.proteomecommons.org/data/download/
GET Parameters:
	
	*******************
	REQUIRED PARAMETERS
	*******************

	Name: hash
	Type: text
	Description: The hash of the Tranche project or file to be downloaded. Must be in Base16.

	*******************
	OPTIONAL PARAMETERS
	*******************
	
	Name: passphrase
	Type: text
	Description: The passphrase of the encrypted Tranche project or file that is to be downloaded.

	Name regex
	Type: text
	Description: The regular expression of the files within the Tranche project that should be downloaded.

	Name: server1 [, server2, etc...]
	Type: text
	Description: Any text field that starts with "server" will be added to a list of Tranche servers to download from.

Return: text/plain containing

	if the download hasn't completed...

		REQUEST = [requestCode]

	Keep this requestCode. It will be used to get update information on this download.
	
	if the download was previously downloaded...
	
		STATUS = COMPLETE
		URL = [download location]


************************************
GETTING AN UPDATE ON DOWNLOAD STATUS
************************************

GET Location: http://www.proteomecommons.org/data/download/update.jsp
GET Parameter:

	Name: requestCode
	Description: the request code returned from the initial download request.

Return: text/plain containing
	
	if the download hasn't completed...

		STATUS = INCOMPLETE
		DATA TO DOWNLOAD = [size of the data being downloaded]
		DATA DOWNLOADED = [amount of data that has been downloaded]

	if the download has completed...
	
		STATUS = COMPLETE
		URL = [download location]

	if the download failed...
	
		STATUS = FAILED
		EXCEPTION: [the reason for failure]