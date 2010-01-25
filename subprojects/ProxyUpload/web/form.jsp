<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Upload to Tranche</title>
    </head>
    <body>

    <h1>Upload a file or set of files to Tranche.</h1>
    
    <form action="index.jsp" method="post" enctype="multipart/form-data">
        
        <table>
            <tr>
                <td>User Zip File: </td>
                <td><input type="file" name="uzf"></td>
            </tr>
            <tr>
                <td>User Passphrase: </td>
                <td><input type="password" name="uzfPassphrase"></td>
            </tr>
            <tr>
                <td>Upload File: </td>
                <td><input type="file" name="upload"></td>
            </tr>
            <tr>
                <td>Try to upload as a directory (file must be a TAR, ZIP, GZIP, LZMA, or BZIP2): </td>
                <td><input type="checkbox" name="uploadAsDirectory" checked value="true"></td>
            </tr>
            <tr>
                <td>Title: </td>
                <td><input type="text" name="title"></td>
            </tr>
            <tr>
                <td>Description: </td>
                <td><textarea name="description"></textarea></td>
            </tr>
            <tr>
                <td>Passphrase: </td>
                <td><input type="password" name="passphrase"></td>
            </tr>
            <tr>
                <td>Server: </td>
                <td><input type="text" name="server"></td>
            </tr>
            <tr>
                <td>Register this upload with ProteomeCommons.org's network: </td>
                <td><input type="checkbox" name="register" checked value="true"></td>
            </tr>
            <tr>
                <td>Use remote replication: </td>
                <td><input type="checkbox" name="remoteRep" value="true"></td>
            </tr>
            <tr>
                <td>Skip existing files: </td>
                <td><input type="checkbox" name="skipFiles" value="true"></td>
            </tr>
            <tr>
                <td>Skip existing chunks: </td>
                <td><input type="checkbox" name="skipChunks" checked value="true"></td>
            </tr>
            <tr>
                <td colspan="2" align="center"><input type="submit" value="Upload"></td>
            </tr>
        </table>
        
    </form>
    
    </body>
</html>
