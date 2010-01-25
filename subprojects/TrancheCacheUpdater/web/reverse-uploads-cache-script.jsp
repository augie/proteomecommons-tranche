<%@ page import="org.proteomecommons.tranche.cacheupdater.*,java.util.*,java.io.*" %><%@ page contentType="text/plain" %><%

try {
    long time = System.currentTimeMillis();

    File scriptDirectory = new File("/opt/tomcat5/webapps/proteomecommons/tags/scripts/");
    if (!scriptDirectory.exists()) {
        throw new RuntimeException("Could not locate the script directory.");
    }

    // try to set up a log file
    File logFile = new File(scriptDirectory, "reverse-run" + time + ".log");
    logFile.createNewFile();
    
    File uploadsFile = new File(scriptDirectory, "uploads.cache");
    if (!uploadsFile.exists()) {
        throw new RuntimeException("Could not locate the uploads cache.");
    }

    // spawns a new thread
    ReverseScript.runOn(uploadsFile, logFile);
    
    out.println("The script has started successfully. The log file is available at: http://proteomecommons.org/tags/scripts/reverse-run" + time + ".log");
} catch (Exception e) {
    out.println("FATAL ERROR: "+e.getMessage());
}
%>