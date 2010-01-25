<%@ page import="java.util.*, org.proteomecommons.tranche.proxy.*" %><%--
--%><%@ page contentType="text/plain" %><%--
--%><%

LinkedList <UploadThread> uploadList = (LinkedList <UploadThread>) application.getAttribute("uploadList");
if (uploadList == null) {
    //throw new RuntimeException("No such request.");
    out.println("No such request");
    out.flush();
    return;
}

String requestCode = request.getParameter("requestCode");

// Allow for simpler alternative vars
if (requestCode == null) {
    requestCode = request.getParameter("request");
} 

if (requestCode == null) {
    requestCode = request.getParameter("id");
}

if (requestCode == null) {
    throw new RuntimeException("You must provide a \"requestCode\" parameter.");
}

UploadThread uploadThread = null;
boolean found = false;
for (UploadThread ut : uploadList) {
    if (ut.getRequestCode().equals(requestCode)) {
        found = true;
        uploadThread = ut;
        break;
    }
}

if (!found) {
    //throw new RuntimeException("No such request.");
    out.println("No such request");
    out.flush();
    return;
}

int status = uploadThread.getStatus();
if (status == UploadThread.STATUS_FAILED) {
    out.println("STATUS = FAILED");
    if (uploadThread.exception != null) {
        out.println("EXCEPTION: \n\n" + uploadThread.exception.toString());
    }
} else if (status == UploadThread.STATUS_COMPLETED) {
    out.println("STATUS = COMPLETE");
    out.println("HASH = " + uploadThread.hash.toString());
} else {
    out.println("STATUS = INCOMPLETE");
    out.println("DATA TO UPLOAD = " + String.valueOf(uploadThread.getDataToUpload()));
    out.println("DATA UPLOADED = " + String.valueOf(uploadThread.getDataUploaded()));
}
%>