/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package autocert;

/**
 *
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class Environment {
    
    /**
     * Directions on setting up new environment:
     * 1. Create a variable before
     * 
     */
    
    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    // Add new variables for any new environments
    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    
    private static final Environment PCorg = new Environment("PC.org production environment");
    private static final  Environment BryansMac = new Environment("Bryan's iBook G4");
    private static final Environment BryansKubuntu = new Environment("Bryan's Kubuntu");
    private static final Environment AugieVaio = new Environment("Augie's Vaio");
    
    /**
     * SET THE ENVIRONMENT TO USE HERE! IF PUT BACK ON PC.ORG, SET TO PCorg
     */
    //private static final Environment ENV = PCorg;
    private static final Environment ENV = PCorg;
    
    // Set the values for the environments
    static {
        // PC.org
        PCorg.setPathToUsersDatabase("/opt/tomcat5/db/auto_cert_users.db");
        PCorg.setPathToCertificateIssuesDatabase("/opt/tomcat5/db/auto_cert_issues.db");
        PCorg.setPathToMasterUserFile("/opt/tomcat5/webapps/proteomecommons/WEB-INF/data/tranche/auto-cert.zip.encrypted");
        PCorg.setPathToMasterPasswordFile("/opt/tomcat5/webapps/proteomecommons/WEB-INF/data/tranche/auto-cert.pass");
        PCorg.setEmailEnabled(true);
        
        // Bryan's iBook G4
        BryansMac.setPathToUsersDatabase("/Users/besmit/Ephemeral/AutoCertEnvironment/auto_cert_users.db");
        BryansMac.setPathToCertificateIssuesDatabase("/Users/besmit/Ephemeral/AutoCertEnvironment/auto_cert_issues.db");
        BryansMac.setPathToMasterUserFile("/Users/besmit/Ephemeral/AutoCertEnvironment/auto-cert.zip.encrypted");
        BryansMac.setPathToMasterPasswordFile("/Users/besmit/Ephemeral/AutoCertEnvironment/auto-cert.pass");
        BryansMac.setEmailEnabled(false);
        
        // --------------------------
        // ADD NEW ENVIRONMENTS BELOW
        // --------------------------
        
        // Minimal setup for migration script
        BryansKubuntu.setPathToUsersDatabase("/home/besmit/Sources/MigrationScripts/files/AutoCert/auto_cert_users.db");
        BryansKubuntu.setPathToCertificateIssuesDatabase("/home/besmit/Sources/MigrationScripts/files/AutoCert/auto_cert_issues.db");
        BryansKubuntu.setEmailEnabled(false);
        
        // Minimal setup for migration script
        AugieVaio.setPathToUsersDatabase("C:/Documents and Settings/James A Hill/Desktop/Tranche/hsqldb/AutoCert/auto_cert_users.db");
        AugieVaio.setPathToCertificateIssuesDatabase("C:/Documents and Settings/James A Hill/Desktop/Tranche/hsqldb/AutoCert/auto_cert_issues.db");
        AugieVaio.setEmailEnabled(false);
    }
    
    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    // Variables encapsulated by environment
    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    
    /**
     * 
     */
    private String description = null;
    /**
     * 
     */
    private String pathToMasterUserFile = null;
    /**
     * 
     */
    private String pathToMasterPasswordFile = null;
    /**
     * 
     */
    private String pathToUsersDatabase = null;
    /**
     * 
     */
    private String pathToCertificateIssuesDatabase = null;
    /**
     * 
     */
    private boolean emailEnabled = true;

    public String getDescription() {
        return description;
    }

    public void setDescription(String aDescription) {
        description = aDescription;
    }

    public String getPathToMasterUserFile() {
        return pathToMasterUserFile;
    }

    public void setPathToMasterUserFile(String aPathToMasterUserFile) {
        pathToMasterUserFile = aPathToMasterUserFile;
    }

    public String getPathToMasterPasswordFile() {
        return pathToMasterPasswordFile;
    }

    public void setPathToMasterPasswordFile(String aPathToMasterPasswordFile) {
        pathToMasterPasswordFile = aPathToMasterPasswordFile;
    }

    public String getPathToUsersDatabase() {
        return pathToUsersDatabase;
    }

    public void setPathToUsersDatabase(String aPathToUsersDatabase) {
        pathToUsersDatabase = aPathToUsersDatabase;
    }

    public String getPathToCertificateIssuesDatabase() {
        return pathToCertificateIssuesDatabase;
    }

    public void setPathToCertificateIssuesDatabase(String aPathToCertificateIssuesDatabase) {
        pathToCertificateIssuesDatabase = aPathToCertificateIssuesDatabase;
    }
    
    /**
     * 
     */
    private Environment(String description) {
        this.description = description;
    }
    
    /**
     * 
     * @return
     */
    public static Environment getEnvironment() {
        return ENV;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }
}
