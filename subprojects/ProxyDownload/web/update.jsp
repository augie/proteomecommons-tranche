<%@ page import="java.util.*, org.proteomecommons.tranche.proxy.*" %><%--
--%><%@ page contentType="text/plain" %><%--
--%><%
LinkedList <DownloadRequest> requestList = (LinkedList <DownloadRequest>) application.getAttribute("requestList");
if (requestList == null) {
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

int requestCodeInt = Integer.parseInt(requestCode);

DownloadRequest downloadRequest = null;
boolean found = false;
for (DownloadRequest dr : requestList) {
    if (dr.hashCode() == requestCodeInt) {
        found = true;
        downloadRequest = dr;
        break;
    }
}

if (!found) {
    out.println("No such request");
    out.flush();
    return;
}

int status = downloadRequest.getDownloadThread().getStatus();
if (status == DownloadThread.STATUS_FAILED) {
    out.println("STATUS = FAILED");
    if (downloadRequest.getDownloadThread().exception != null) {
        out.println("EXCEPTION: " + downloadRequest.getDownloadThread().exception.getMessage());
        out.println("HASH: "+downloadRequest.hash);
    }
} else if (status == DownloadThread.STATUS_COMPLETED) {
    out.println("STATUS = COMPLETE");
    out.println("URL = http://www.proteomecommons.org/data/download/" + downloadRequest.getDownloadThread().zipFile.getParentFile().getName() + "/" + downloadRequest.getDownloadThread().zipFile.getName());
} else {
    out.println("STATUS = INCOMPLETE");
    out.println("DATA TO DOWNLOAD = " + downloadRequest.getDownloadThread().getDataToDownload().toString());
    out.println("DATA DOWNLOADED = " + downloadRequest.getDownloadThread().getDataDownloaded().toString());
}
%>