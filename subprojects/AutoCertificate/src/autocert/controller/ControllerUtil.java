package autocert.controller;

import autocert.Environment;
import autocert.model.DatabaseUtil;
import autocert.model.UserRecord;
import autocert.view.ViewUtil;
import java.io.File;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;
import org.tranche.users.MakeUserZipFileTool;
import org.tranche.users.UserZipFile;
import org.tranche.util.EmailUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;

/**
 * Simplify the application logic.
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 */
public class ControllerUtil {
    
    // Disable email, useful for testing
    private final static boolean isEmailDisabled = !Environment.getEnvironment().isEmailEnabled();

    // Flag. Only send admin one warning about certificate pending expiration
    private static boolean hasSentExpirationMessageToAdmin = false;
    
    private final static String adminEmailAddress = "jfalkner@umich.edu, markgj@umich.edu, bryanesmith@gmail.com, augman85@gmai.com";
    
    // Handles for master user file and password file
    private final static File MASTER_USER_FILE = new File(Environment.getEnvironment().getPathToMasterUserFile());
    private final static File MASTER_USER_PASSWORD_FILE = new File(Environment.getEnvironment().getPathToMasterPasswordFile());
    private static String MASTER_USER_FILE_PASSWORD = null;
    
    private static X509Certificate masterCertificate = null;
    private static PrivateKey masterPrivateKey = null;
    
    // Cache user records to go to disk less frequently
    private static Set userRecords = new HashSet();
    
    // If need to store an exception
    private static Exception exception = null;
    
    private static boolean lazyLoaded = false;
    private static void lazyLoad() {
        if (lazyLoaded)
            return;
        lazyLoaded = true;
        
        try {
            
            if (MASTER_USER_FILE_PASSWORD == null) {
                byte[] passwordBytes = IOUtil.getBytes(MASTER_USER_PASSWORD_FILE);
                MASTER_USER_FILE_PASSWORD = new String(passwordBytes).trim();
            }
            
            // Load the user cache
            userRecords.addAll(DatabaseUtil.getUserRecords());
            
            // Read in master file
            if (MASTER_USER_FILE != null) {
                UserZipFile uzf = new UserZipFile(MASTER_USER_FILE);
                
                // Really should store password in clear text, so hopefully another way
                if (MASTER_USER_FILE_PASSWORD != null) {
                    uzf.setPassphrase(MASTER_USER_FILE_PASSWORD);
                }
                
                masterCertificate = uzf.getCertificate();
                masterPrivateKey = uzf.getPrivateKey();
            }
        } catch (Exception ex) {
            /* Ignore, will just have to go to disk more often */
            System.out.println("Failed to load master cert: " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }
    
    /**
     * Called when changes to the database mean the cache needs updated.
     */
    public static void clearCache() {
        userRecords.clear();
        lazyLoaded = false;
    }
    
    /**
     * Registers exception. Allows other pages to parse the exception.
     */
    public static void registerException(Exception e) {
        lazyLoad();
        ControllerUtil.exception = e;
    }
    
    /**
     * Returns any registered exceptions.
     */
    public static Exception getRegisteredException() {
        lazyLoad();
        return ControllerUtil.exception;
    }
    
    /**
     * Returns true if login verifies, false otherwise
     */
    public static boolean verifyLogin(Object name, Object password) throws Exception {
        // If session over or just attempted a page directly w/o login
        if (name == null || password == null)
            return false;
        
        return verifyLogin(name.toString(),password.toString());
    }
    
    /**
     * Returns true if login verifies, false otherwise
     */
    public static boolean verifyLogin(String name, String password) throws Exception {
        lazyLoad();
        
        // If session over or just attempted a page directly w/o login
        if (name == null || password == null)
            return false;
        
        // Try cache first
        UserRecord user = getUserFromCache(name);
        
        // If not in cache, attempt from database
        if (user == null) {
            user = DatabaseUtil.getUserRecord(name);
        }
        
        // Check the database for expiration
        if (user != null && user.expiresOn < System.currentTimeMillis()) {
            return false;
        }
        
        // User must exist, have the correct password and be approved
        if (user != null && user.password.equals(password) && user.isApproved) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Attempts to get from cache, falls back on db.
     */
    public static String getUsernameForId(int id) throws Exception {
        lazyLoad();
        
        UserRecord user = getUserFromCache(id);
        
        if (user != null)
            return user.name;
        
        return DatabaseUtil.getUserNameForId(id);
    }
    
    /**
     * Returns true if user is admin.
     */
    public static boolean isUserAdmin(Object name) throws Exception {
        lazyLoad();
        // If session over or just attempted a page directly w/o login
        if (name == null)
            return false;
        
        return isUserAdmin(name.toString());
    }
    
    public static File createPrivilegedUserFile(String name, String passphrase) throws Exception {
        lazyLoad();
        File f = new File(TempFileUtil.getTemporaryDirectory(),name+".zip.encrypted");
        f.createNewFile();
        
        if (!f.exists())
            throw new RuntimeException("Can't make temp file for user.");
        
        MakeUserZipFileTool maker = new MakeUserZipFileTool();
        maker.setName(name);
        maker.setPassphrase(passphrase);
        maker.setUserFile(f);
        maker.setOrganizationalUnit("");
        maker.setOrganization("");
        maker.setLocation("");
        maker.setState("");
        maker.setCountry("");
        
        Date yesterday = new Date(System.currentTimeMillis()-1000*60*60*24);
        maker.setValidDays(yesterday,14);
        
        // Sign if cert. Will always be a cert in production; leave as conditional
        // for development
        if (getMasterCertificate() != null) {
            maker.setSignerCertificate(getMasterCertificate());
            maker.setSignerPrivateKey(getMasterPrivateKey());
        }
        UserZipFile uzf = (UserZipFile) maker.makeCertificate();
        
        return uzf.getFile();
    }
    
    public static X509Certificate getMasterCertificate() throws Exception {
        lazyLoad();
        return masterCertificate;
    }
    
    private static PrivateKey getMasterPrivateKey() throws Exception {
        lazyLoad();
        return masterPrivateKey;
    }
    
    /**
     * Returns true if user is admin.
     */
    public static boolean isUserAdmin(String name) throws Exception {
        lazyLoad();
        // If session over or just attempted a page directly w/o login
        if (name == null)
            return false;
        
        // Try cache first
        UserRecord user = getUserFromCache(name);
        
        // If not in cache, attempt from database
        if (user == null) {
            user = DatabaseUtil.getUserRecord(name);
        }
        
        if (user != null) {
            return user.isAdmin;
        }
        
        return false;
    }
    
    /**
     * Returns user record if in cache, else null.
     */
    private static UserRecord getUserFromCache(String name) {
        lazyLoad();
        Iterator it = userRecords.iterator();
        
        UserRecord nextRecord;
        while(it.hasNext()) {
            nextRecord = (UserRecord)it.next();
            if (nextRecord.name.equals(name)) {
                return nextRecord;
            }
        }
        
        return null;
    }
    
    /**
     * Returns user record if in cache, else null.
     */
    private static UserRecord getUserFromCache(int id) {
        lazyLoad();
        Iterator it = userRecords.iterator();
        
        UserRecord nextRecord;
        while(it.hasNext()) {
            nextRecord = (UserRecord)it.next();
            if (nextRecord.id == id) {
                return nextRecord;
            }
        }
        
        return null;
    }
    
    /**
     * Create timestamp based on days from now. Use -1 to mean indeterminate.
     */
    public static final long ONE_DAY = 1000 * 60 * 60 * 24;
    public static long createFutureTimestamp(int daysFromNow) {
        lazyLoad();
        if (daysFromNow == -1) return -1;
        return (System.currentTimeMillis() + daysFromNow*ONE_DAY);
    }
    
    /**
     * Returns difference in days between two timestamps
     */
    public static long getDaysBetween(long start, long finish) {
        lazyLoad();
        if (finish == -1) {
            return -1;
        }
        if (finish == 0) {
            return 0;
        }
        return (finish-start)/ONE_DAY;
    }
    
    /**
     * Send an email to the group.
     */
    public static void sendEmailToGroup(String subject, String message) throws Exception {
        if (isEmailDisabled)
            return;
        
        sendEmailToGroup(null,subject,message);
    }
    
    /**
     * Send an email to group w/ a copy to another user.
     */
    public static void sendEmailToGroup(String copyMessageTo,String subject,String message) throws Exception {
        if (isEmailDisabled)
            return;
        
        final String GROUP_EMAIL = "proteomecommons-tranche-dev@googlegroups.com";
        String[] recipients = {GROUP_EMAIL};
        if (copyMessageTo != null) {
            String[] tmp = {copyMessageTo,GROUP_EMAIL};
            recipients = tmp;
        }
        EmailUtil.sendEmail(subject, recipients,message);
    }
    
    /**
     * Send an email to a user.
     */
    public static void sendEmailToUser(UserRecord user, String subject, String message) throws Exception {
        if (isEmailDisabled)
            return;
        
        String[] recipients = {user.email};
        EmailUtil.sendEmail(subject,recipients,message);
    }
    
    /**
     * Send an email to an address.
     */
    public static void sendEmailToAddress(String email, String subject, String message) throws Exception {
        if (isEmailDisabled)
            return;
        
        String[] recipients = null;
        
        if (email.contains(",")) {
            recipients = email.split(Pattern.quote(","));
        } else {
           recipients = new String[1];
           recipients[0] = email;
        }
        EmailUtil.sendEmail(subject,recipients,message);
    }
    
    /**
     * Send an email about applying.
     */
    public static void sendEmailRegardingSignup(String email) throws Exception {
        String subject = "Thanks for applying to use Tranche";
        String message = "Thanks for applying to use Tranche. Your request must be approved before you can sign in to create a new user. We check new applications weekly on Mondays, and you should expect a response soon.\n\n"+
                "While you wait, you can still run Tranche to download existing projects, start your own server or other activities. To run Tranche, visit http://tranche.proteomecommons.org/ and select \"Launch Tranche\". If you have any questions, you can contact us using the contact information contained on this page.";
        ControllerUtil.sendEmailToAddress(email,subject,message);
    }
    
    /**
     * Send an email about being approved.
     */
    public static void sendEmailRegardingApproval(String email, String username, String password) throws Exception {
        String subject = "Approval: thanks for signing up to use Tranche";
        String message = "Thanks for signing up. You were approved! Here's your information:\n\n"+
                "User name: "+username+"\n"+
                "Password: "+password+"\n\n"+
                "To run Tranche, visit http://tranche.proteomecommons.org/ and select \"Launch Tranche\". There will be a link on this page to create a user. You will need the login information contained in this email to use Tranche.";
        ControllerUtil.sendEmailToAddress(email,subject,message);
    }
    
    /**
     * Send an email about being denied.
     */
    public static void sendEmailRegardingDenial(String email) throws Exception {
        String subject = "More information requested about your application to use Tranche";
        String message = "Thanks for applying to use Tranche. We will need more information to approve your application. This is common; we are simply moderating use of the network to ensure the quality and legality of our service.\n\n"+
                "Please go to http://tranche.proteomecommons.org/ and use the Contact Us information to contact us. Please let us know more about your interest in Tranche.\n\n" +
                "While you wait, you can still use Tranche to download existing projects, start your own server or other activities. To run Tranche, visit http://tranche.proteomecommons.org/ and select \"Launch Tranche\". Thank you for your interest and we will be looking forward to hearing from you!";
        ControllerUtil.sendEmailToAddress(email,subject,message);
    }
    
    /**
     * Send an email about being denied.
     */
    public static void sendEmailToAdminAboutRequest(String requesterName, String requesterEmail, String description) throws Exception {
        String subject = requesterName + " applied to create a Tranche account on " + ViewUtil.getFormattedTime(System.currentTimeMillis());
        String message = "A new application has come in from <"+requesterEmail+"> with the following comments:\n\n"+
                description + "\n\n" +
                "You will need to login as an admin to approve, request more information or remove the application.";
        ControllerUtil.sendEmailToAddress(adminEmailAddress,subject,message);
    }
    
    /**
     * Send an email about being denied.
     */
    public static void sendEmailToAdminRegardingPendingCertExpiration() throws Exception {
        
        // Only do once
        if (hasSentExpirationMessageToAdmin)
            return;
        hasSentExpirationMessageToAdmin = true;
        
        String certExpirationDate = ViewUtil.getFormattedTime(ControllerUtil.getMasterCertValidEnding());
        
        String subject = "Tranche Auto Certificate System: master certificate expires " + certExpirationDate;
        String message = "This is just a warning that the master certificate is no longer valid on " + certExpirationDate;
        ControllerUtil.sendEmailToAddress(adminEmailAddress,subject,message);
    }
    
    /**
     * Approves a collection of applications and sends out email notice.
     * @param ids A set of Integers representing ids for UserRecords holding applications.
     */
    public static void approveApplications(Set ids) throws Exception {
        Iterator it = ids.iterator();
        while(it.hasNext()) {
            int nextId = ((Integer)it.next()).intValue();
            
            UserRecord user = DatabaseUtil.getUserRecord(nextId);
            user.isApproved = true;
            
            DatabaseUtil.updateUser(user);
            
            ControllerUtil.sendEmailRegardingApproval(user.email,user.name,user.password);
        }
    }
    
    /**
     * Returns timestamp when the master cert is first valid.
     */
    public static long getMasterCertValidStarting() {
        lazyLoad();
        return masterCertificate.getNotBefore().getTime();
    }
    
    /**
     * Returns timestamp when the master cert is last valid.
     */
    public static long getMasterCertValidEnding() {
        lazyLoad();
        return masterCertificate.getNotAfter().getTime();
    }
    
    /**
     * Returns true if certificate is ending within user-defined time span.
     * @param duration Time span (in milliseconds). E.g., if want to know whether expiring within a week, duration = 1000 * 60 * 60 * 24 * 7
     */
    public static boolean isMasterCertExpiringWithinDuration(long duration) {
        lazyLoad();
        return masterCertificate.getNotAfter().getTime() < duration + System.currentTimeMillis();
    }
    
    /**
     * Requests more information for a collection of applications and sends out email notice.
     * @param ids A set of Integers representing ids for UserRecords holding applications.
     */
    public static void requestMoreInfoForApplications(Set ids) throws Exception {
        Iterator it = ids.iterator();
        while(it.hasNext()) {
            int nextId = ((Integer)it.next()).intValue();
            
            UserRecord user = DatabaseUtil.getUserRecord(nextId);
            
            ControllerUtil.sendEmailRegardingDenial(user.email);
        }
    }
    
    /**
     * Removes a collection of applications. An alias for removeUsers.
     * @param ids A set of Integers representing ids for UserRecords holding applications.
     */
    public static void removeApplications(Set ids) throws Exception {
        removeUsers(ids);
    }
    
    /**
     * Removes a collection of users.
     * @param ids A set of Integers representing ids for UserRecords holding applications.
     */
    public static void removeUsers(Set ids) throws Exception {
        Iterator it = ids.iterator();
        while(it.hasNext()) {
            int nextId = ((Integer)it.next()).intValue();
            DatabaseUtil.removeUser(nextId);
        }
    }
}

