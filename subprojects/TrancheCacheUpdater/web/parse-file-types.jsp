<%@ page import="org.proteomecommons.tranche.cacheupdater.*,java.util.*,java.io.*" %><%@ page contentType="text/plain" %><%
// spawn off a new thread because this could take a long time
try {
    long time = System.currentTimeMillis();

    File scriptDirectory = new File("/opt/tomcat5/webapps/proteomecommons/tags/scripts/");
    if (!scriptDirectory.exists()) {
        throw new RuntimeException("Could not locate the script directory.");
    }

    // try to set up a log file
    File logFile = new File(scriptDirectory, "filetypes-run" + time + ".log");
    logFile.createNewFile();
    
    File fileTypesLogFile = new File(scriptDirectory, "filetype.log");
    if (!fileTypesLogFile.exists()) {
        throw new RuntimeException("Could not locate the file types log.");
    }

    // spawns a new thread
    FileTypeLogParseScript.runOn(fileTypesLogFile, logFile);
    
    out.println("The script has started successfully. The log file is available at: http://proteomecommons.org/tags/scripts/reverse-run" + time + ".log");
} catch (Exception e) {
    out.println("FATAL ERROR: "+e.getMessage());
}
%>