package autocert.model;

import autocert.Environment;
import autocert.controller.ControllerUtil;
import autocert.view.ViewUtil;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.tranche.util.IOUtil;
import org.tranche.util.Text;

/**
 * <p>Interfaces w/ the database on behalf of the system, encapsulates data.</p>
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 */
public class DatabaseUtil {
    
    protected final static File USERS_DATABASE_FILE = new File(Environment.getEnvironment().getPathToUsersDatabase());
    protected final static File CERTS_DATABASE_FILE = new File(Environment.getEnvironment().getPathToCertificateIssuesDatabase());

    protected final static String NEWLINE = "\n";
    
    /***************************************************************************
     * USERS
     ***************************************************************************/
    /**
     * Assigns a user id. Monotomically increments last record id to get next.
     */
    private static synchronized int assignUserId() throws Exception {
        lazyLoad();
        
        int userId = -1;
        BufferedReader reader = new BufferedReader(new FileReader(USERS_DATABASE_FILE));
        try {
            // Going to find the last entry
            String nextLine = null;
            while ((nextLine = reader.readLine())!= null) {
                // Skip blank lines
                if (nextLine.trim().equals("")) continue;
                int nextId = parseUserId(nextLine);
                // Replace pointer with highest value
                if (userId < nextId) {
                    userId = nextId;
                }
            }
            
            return userId+1;
        }
        
        finally {
            reader.close();
        }
    }
    
    /**
     * Add a new user to the database. User does not have an expiration date or a group.
     */
    public static synchronized void createUserEntry(
            String name,
            String password,
            String email,
            boolean isAdmin) throws Exception {
        lazyLoad();
        createUserEntryInternal(assignUserId(),System.currentTimeMillis(),name,password,email,isAdmin,false,Long.MAX_VALUE,"-","-","-","-");
    }
    
    /**
     * Add a new user to the database.
     */
    public static synchronized void createUserEntry(
            String name,
            String password,
            String email,
            boolean isAdmin,
            String firstName,
            String lastName,
            String affiliation) throws Exception {
        lazyLoad();
        createUserEntryInternal(assignUserId(),System.currentTimeMillis(),name,password,email,isAdmin,false,Long.MAX_VALUE,"-",firstName,lastName,affiliation);
    }
    
    /**
     * Add a new user in a group.
     */
    public static synchronized void createUserEntry(
            String name,
            String password,
            String email,
            boolean isAdmin,
            String group) throws Exception {
        lazyLoad();
        createUserEntryInternal(assignUserId(),System.currentTimeMillis(),name,password,email,isAdmin,false,Long.MAX_VALUE,group,"-","-","-");
    }
    
    /**
     * Add a new user with an expiration date.
     */
    public static synchronized void createUserEntry(
            String name,
            String password,
            String email,
            boolean isAdmin,
            long expiresOn) throws Exception {
        lazyLoad();
        createUserEntryInternal(assignUserId(),System.currentTimeMillis(),name,password,email,isAdmin,false,expiresOn,"-","-","-","-");
    }
    
    /**
     * Add a new user w/ an expiration date and a group.
     */
    public static synchronized void createUserEntry(
            String name,
            String password,
            String email,
            boolean isAdmin,
            long expiresOn,
            String group) throws Exception {
        lazyLoad();
        createUserEntryInternal(assignUserId(),System.currentTimeMillis(),name,password,email,isAdmin,false,expiresOn,group,"-","-","-");
    }
    
    private static synchronized void createUserEntryInternal(
            int id,
            long createdOn,
            String name,
            String password,
            String email,
            boolean isAdmin,
            boolean isApproved,
            long timestampExpiration,
            String group,
            String firstName,
            String lastName,
            String affiliation) throws Exception {
        lazyLoad();
        backupFiles();
        
        if (!isUsernameAvailable(name)) {
            throw new Exception("The username \"" + name + "\" is already taken.");
        }
        
        PrintWriter writer = new PrintWriter(new FileOutputStream(USERS_DATABASE_FILE,true));
        try {
            writer.write(id+","+escape(name)+","+escape(password)+","+createdOn+","+escape(email)+","+isAdmin+","+isApproved+","+timestampExpiration+","+escape(group)+","+escape(firstName)+","+escape(lastName)+","+escape(affiliation)+NEWLINE);
        }
        
        finally {
            writer.flush();
            writer.close();
            
            // Update cache
            ControllerUtil.clearCache();
        }
    }
    
    /**
     * Returns true if approved, false otherwise.
     */
    public static boolean isUserApproved(int id) throws Exception {
        lazyLoad();
        UserRecord user = getUserRecord(id);
        if (user != null) {
            return user.isApproved;
        }
        
        return false;
    }
    
    /**
     * Returns true if approved, false otherwise.
     */
    public static boolean isUserApproved(String username) throws Exception {
        lazyLoad();
        return isUserApproved(getIdForUser(username));
    }
    
    /**
     * Returns id of username.
     * @param The id. -1 if user not found
     */
    public static synchronized int getIdForUser(String username) throws Exception {
        lazyLoad();
        
        BufferedReader reader = new BufferedReader(new FileReader(USERS_DATABASE_FILE));
        try {
            // Going to find the last entry
            String nextLine ;
            while ((nextLine = reader.readLine())!= null) {
                // Skip blank lines
                if (nextLine.trim().equals("")) continue;
                // Parse token
                String[] tokens = nextLine.split(",");
                String readUserName = parseUserName(nextLine);
                if (username.equals(readUserName)){
                    return parseUserId(nextLine);
                }
            }
        }
        
        finally {
            reader.close();
        }
        
        // Not found
        return -1;
    }
    
    /**
     * Returns true if username is available, false if already in use.
     */
    public static synchronized boolean isUsernameAvailable(String username) throws Exception {
        lazyLoad();
        return getIdForUser(username) == -1;
    }
    
    /**
     * Removes user based on id.
     * @return returns true if found and removed, false otherwise
     */
    public static synchronized boolean removeUser(int id) throws Exception {
        lazyLoad();
        backupFiles();
        
        boolean removed = false;
        
        // Remove user expensive on disk, but infrequent
        File tmpFile = new File("todo_users.db.tmp");
        
        // For printing out new file w/ user removed
        BufferedReader reader = null;
        PrintWriter writer = null;
        try {
            
            // Copy over file byte-by-byte
            copyFile(USERS_DATABASE_FILE,tmpFile);
            
            reader = new BufferedReader(new FileReader(tmpFile));
            writer = new PrintWriter(new FileOutputStream(USERS_DATABASE_FILE,false));
            
            // Going to find the last entry
            String nextLine ;
            while ((nextLine = reader.readLine())!= null) {
                // Skip blank lines... cleaner file?
                if (nextLine.trim().equals("")) {
                    continue;
                }
                
                if (id == parseUserId(nextLine)){
                    // Found the id, skip user
                    removed = true;
                    continue;
                }
                writer.println(nextLine);
            }
            
            if (removed) {
                // Update cache
                ControllerUtil.clearCache();
            }
            
            return removed;
        }
        
        finally {
            // Close print streams
            reader.close();
            writer.flush();
            writer.close();
            
            // Delete temp file
            tmpFile.delete();
        }
    }
    
    /**
     * Update an existing user in the database.
     * @return True if found and updated, false otherwise.
     */
    public static synchronized boolean updateUser(UserRecord record) throws Exception {
        lazyLoad();
        boolean removed = DatabaseUtil.removeUser(record.id);
        if (removed) {
            DatabaseUtil.createUserEntryInternal(
                    record.id,
                    record.createdOn,
                    record.name,
                    record.password,
                    record.email,
                    record.isAdmin,
                    record.isApproved,
                    record.expiresOn,
                    record.group,
                    record.firstName,
                    record.lastName,
                    record.affiliation);
        }
        return removed;
    }
    
    /**
     * Retrieve the user record by the user's unique name.
     */
    public static synchronized UserRecord getUserRecord(String name) throws Exception {
        lazyLoad();
        return getUserRecord(getIdForUser(name));
    }
    
    /**
     * Retrieves user name based on id.
     */
    public static synchronized String getUserNameForId(int id) throws Exception {
        lazyLoad();
        return getUserRecord(id).name;
    }
    
    /**
     * Retrieve the user record by the user's unique id.
     */
    public static synchronized UserRecord getUserRecord(int id) throws Exception {
        lazyLoad();
        BufferedReader reader = new BufferedReader(new FileReader(USERS_DATABASE_FILE));
        
        // If -1, return general user info (kept out of db so can't be used for login)
        if (id == -1) {
            UserRecord record = new UserRecord();
            record.name = "General Todo";
            record.email = "proteomecommons-tranche-dev@googlegroups.com";
            record.password = "none";
            record.createdOn = -1;
            record.isAdmin = false;
            record.id = -1;
            return record;
        }
        
        try {
            String nextLine;
            UserRecord nextRecord;
            while ((nextLine = reader.readLine())!= null) {
                // Skip blank lines
                if (nextLine.trim().equals("")) continue;
                
                nextRecord = parseUserRecord(nextLine);
                if (nextRecord.id == id) {
                    return nextRecord;
                }
            }
        }
        
        finally {
            reader.close();
        }
        
        return null;
    }
    
    /**
     * Returns all user records in the database.
     */
    public static synchronized Set getUserRecords() throws Exception {
        lazyLoad();
        Set userRecords = new HashSet();
        BufferedReader reader = new BufferedReader(new FileReader(USERS_DATABASE_FILE));
        
        try {
            String nextLine ;
            while ((nextLine = reader.readLine())!= null) {
                // Skip blank lines
                if (nextLine.trim().equals("")) continue;
                userRecords.add(parseUserRecord(nextLine));
            }
        }
        
        finally {
            reader.close();
        }
        
        return userRecords;
    }
    
    /**
     * Returns all applications (user records without approval)
     */
    public static synchronized Set getUserApplications() throws Exception {
        lazyLoad();
        Set applications = new HashSet();
        Iterator it = getUserRecords().iterator();
        
        UserRecord nextRecord;
        while(it.hasNext()) {
            nextRecord = (UserRecord)it.next();
            if (!nextRecord.isApproved)
                applications.add(nextRecord);
        }
        
        return applications;
    }
    
    /**
     * Returns all users in a group
     */
    public static synchronized Set getUsersInGroup(String group) throws Exception {
        lazyLoad();
        Set users = new HashSet();
        Iterator it = getUserRecords().iterator();
        
        UserRecord nextRecord;
        while(it.hasNext()) {
            nextRecord = (UserRecord)it.next();
            if (nextRecord.group.equals(group))
                users.add(nextRecord);
        }
        
        return users;
    }
    
    /**
     *
     */
    public static synchronized Set getGroups() throws Exception {
        lazyLoad();
        
        Set groups = new HashSet();
        Set users = DatabaseUtil.getUserRecords();
        
        Iterator it = users.iterator();
        UserRecord nextRecord;
        while(it.hasNext()) {
            nextRecord = (UserRecord)it.next();
            if (!nextRecord.group.equals("-") && !groups.contains(nextRecord.group)) {
                groups.add(nextRecord.group);
            }
        }
        
        return groups;
    }
    
    /**
     *
     */
    public static synchronized Set getValidGroups() throws Exception {
        lazyLoad();
        
        Set groups = new HashSet();
        Set users = DatabaseUtil.getUserRecords();
        
        Iterator it = users.iterator();
        UserRecord nextRecord;
        while(it.hasNext()) {
            nextRecord = (UserRecord)it.next();
            if (!nextRecord.group.equals("-") && !groups.contains(nextRecord.group) && System.currentTimeMillis() < nextRecord.expiresOn) {
                groups.add(nextRecord.group);
            }
        }
        
        return groups;
    }
    
    /**
     *
     */
    public static synchronized Set getExpiredGroups() throws Exception {
        lazyLoad();
        
        Set groups = new HashSet();
        Set users = DatabaseUtil.getUserRecords();
        
        Iterator it = users.iterator();
        UserRecord nextRecord;
        while(it.hasNext()) {
            nextRecord = (UserRecord)it.next();
            if (!nextRecord.group.equals("-") && !groups.contains(nextRecord.group) && System.currentTimeMillis() >= nextRecord.expiresOn) {
                groups.add(nextRecord.group);
            }
        }
        
        return groups;
    }
    
    /**
     * Expires (not deletes) all users in group.
     */
    public static synchronized void expireGroup(String group) throws Exception {
        lazyLoad();
        
        Iterator it = getUsersInGroup(group).iterator();
        UserRecord nextRecord;
        while(it.hasNext()) {
            nextRecord = (UserRecord)it.next();
            nextRecord.expiresOn = System.currentTimeMillis();
            DatabaseUtil.updateUser(nextRecord);
        }
    }
    
    /**
     * Sets all members of group to not expire.
     */
    public static synchronized void setExpirationForGroup(String group, long expiresOn) throws Exception {
        lazyLoad();
        
        Iterator it = getUsersInGroup(group).iterator();
        UserRecord nextRecord;
        while(it.hasNext()) {
            nextRecord = (UserRecord)it.next();
            nextRecord.expiresOn = expiresOn;
            DatabaseUtil.updateUser(nextRecord);
        }
    }
    
    /**
     * Returns all approved users
     */
    public static synchronized Set getApprovedUserRecords() throws Exception {
        lazyLoad();
        Set users = new HashSet();
        Iterator it = getUserRecords().iterator();
        
        UserRecord nextRecord;
        while(it.hasNext()) {
            nextRecord = (UserRecord)it.next();
            if (nextRecord.isApproved)
                users.add(nextRecord);
        }
        
        return users;
    } 
    
    /**
     * Removes user based on id.
     * @return returns true if found and removed, false otherwise
     */
    public static synchronized boolean removeUser(String user) throws Exception {
        lazyLoad();
        return removeUser(getIdForUser(user));
    } // removeUserEntry
    
    /***************************************************************************
     * CERTIFICATE ISSUES
     ***************************************************************************/
    
    /**
     * Assigns a user id. Monotomically increments last record id to get next.
     */
    private static synchronized int assignCertId() throws Exception {
        lazyLoad();
        
        int certId = -1;
        BufferedReader reader = new BufferedReader(new FileReader(CERTS_DATABASE_FILE));
        try {
            // Going to find the last entry
            String nextLine = null;
            while ((nextLine = reader.readLine())!= null) {
                // Skip blank lines
                if (nextLine.trim().equals("")) continue;
                int nextId = parseCertId(nextLine);
                if (certId < nextId)
                    certId = nextId;
            }
            
            return certId+1;
        }
        
        finally {
            reader.close();
        }
    }
    
    /**
     * Add a new user to the database.
     */
    public static synchronized void createCertificateEntry(
            int userId,
            String certName) throws Exception {
        lazyLoad();
        createCertificateEntryInternal(assignCertId(),userId,certName,System.currentTimeMillis());
    }
    
    private static synchronized void createCertificateEntryInternal(
            int id,
            int userId,
            String certName,
            long timestamp) throws Exception {
        lazyLoad();
        backupFiles();
        
        PrintWriter writer = new PrintWriter(new FileOutputStream(CERTS_DATABASE_FILE,true));
        try {
            writer.write(id+","+userId+","+certName+","+timestamp+NEWLINE);
        }
        
        finally {
            writer.flush();
            writer.close();
        }
    }
    
    public static synchronized Set getCertificateRecords() throws Exception {
        lazyLoad();
        Set certIssueRecords = new HashSet();
        BufferedReader reader = new BufferedReader(new FileReader(CERTS_DATABASE_FILE));
        
        try {
            String nextLine ;
            while ((nextLine = reader.readLine())!= null) {
                // Skip blank lines
                if (nextLine.trim().equals("")) continue;
                certIssueRecords.add(DatabaseUtil.parseCertificateRecord(nextLine));
            }
        }
        
        finally {
            reader.close();
        }
        
        return certIssueRecords;
    }
    
    public static synchronized Set getCertificateRecordsForUser(String name) throws Exception {
        lazyLoad();
        return getCertificateRecordsForUser(DatabaseUtil.getIdForUser(name));
    }
    
    public static synchronized Set getCertificateRecordsForUser(int id) throws Exception {
        lazyLoad();
        Iterator it = getCertificateRecords().iterator();
        Set certRecords = new HashSet();
        CertificateRecord r = null;
        while(it.hasNext()) {
            r = (CertificateRecord)it.next();
            if (r.userId == id) {
                certRecords.add(r);
            }
        }
        
        return certRecords;
    }
    
    /***************************************************************************
     * DATABASE EXTRACTION
     ***************************************************************************/
    /**
     * Represents the index of fields in CSV database for user record.
     */
    private static final int
            USER_ID_INDEX = 0,          // The primary key for user
            USER_NAME_INDEX = 1,        // The user's name
            USER_PASSWORD_INDEX = 2,    // The user's password
            USER_TIMESTAMP_INDEX = 3,   // Timestamp of creation of user
            USER_EMAIL_INDEX = 4,       // User's email address
            USER_IS_ADMIN_INDEX = 5,
            USER_IS_APPROVED_INDEX = 6, // User cannot log in until approved
            USER_EXPIRES_TIMESTAMP_INDEX = 7,
            USER_GROUP_INDEX = 8,
            USER_FIRST_NAME_INDEX = 9,
            USER_LAST_NAME_INDEX = 10,
            USER_AFFILIATION_INDEX = 11; 
    
    /**
     * Parse out the unique user id from a user record.
     */
    private static int parseUserId(String userRecordStr) {
        if (userRecordStr == null) return -1;
        String[] tokens = userRecordStr.split(",");
        return Integer.parseInt(tokens[USER_ID_INDEX].trim());
    }
    
    /**
     * Parse unique user name from user record.
     */
    private static String parseUserName(String userRecordStr) {
        if (userRecordStr == null) return null;
        String[] tokens = userRecordStr.split(",");
        return tokens[USER_NAME_INDEX].trim();
    }
    
    /**
     * Parse out the user's password from the user record.
     */
    private static String parseUserPassword(String userRecordStr) {
        if (userRecordStr == null) return null;
        String[] tokens = userRecordStr.split(",");
        return tokens[USER_PASSWORD_INDEX].trim();
    }
    
    /**
     * Parse out the user's creation timestamp from the user record.
     */
    private static long parseUserTimestamp(String userRecordStr) {
        if (userRecordStr == null) return -1;
        String[] tokens = userRecordStr.split(",");
        return Long.parseLong(tokens[USER_TIMESTAMP_INDEX].trim());
    }
    
    /**
     * Parse out the user's email from the user record.
     */
    private static String parseUserEmail(String userRecordStr) {
        if (userRecordStr == null) return null;
        String[] tokens = userRecordStr.split(",");
        return tokens[USER_EMAIL_INDEX].trim();
    }
    
    /**
     * Parse out whether the user is an admin from the user record.
     */
    private static boolean parseUserIsAdmin(String userRecordStr) {
        if (userRecordStr == null) return false;
        String[] tokens = userRecordStr.split(",");
        return Boolean.parseBoolean(tokens[USER_IS_ADMIN_INDEX].trim());
    }
    
    /**
     * Parse out whether the user is approved yet from the user record.
     */
    private static boolean parseUserIsApproved(String userRecordStr) {
        if (userRecordStr == null) return false;
        String[] tokens = userRecordStr.split(",");
        return Boolean.parseBoolean(tokens[USER_IS_APPROVED_INDEX].trim());
    }
    
    /**
     * Parse out the user's expiration date from the user record.
     */
    private static long parseUserExpiresTimestamp(String userRecordStr) {
        if (userRecordStr == null) return -1;
        String[] tokens = userRecordStr.split(",");
        return Long.parseLong(tokens[USER_EXPIRES_TIMESTAMP_INDEX].trim());
    }
    
    /**
     * Parse out the user's group from the user record.
     */
    private static String parseUserGroup(String userRecordStr) {
        if (userRecordStr == null) return null;
        String[] tokens = userRecordStr.split(",");
        return tokens[USER_GROUP_INDEX].trim();
    }
    
    /**
     * Parse out the user's first name from the user record.
     */
    private static String parseUserFirstName(String userRecordStr) {
        if (userRecordStr == null) return null;
        String[] tokens = userRecordStr.split(",");
        return tokens[USER_FIRST_NAME_INDEX].trim();
    }
    
    /**
     * Parse out the user's last name from the user record.
     */
    private static String parseUserLastName(String userRecordStr) {
        if (userRecordStr == null) return null;
        String[] tokens = userRecordStr.split(",");
        return tokens[USER_LAST_NAME_INDEX].trim();
    }
    
    /**
     * Parse out the user's affiliation from the user record.
     */
    private static String parseUserAffiliation(String userRecordStr) {
        if (userRecordStr == null) return null;
        String[] tokens = userRecordStr.split(",");
        return tokens[USER_AFFILIATION_INDEX].trim();
    }
    
    /**
     * Parse out a UserRecord from a user record.
     */
    private static UserRecord parseUserRecord(String userRecordStr) {

        UserRecord record = new UserRecord();
        
        record.id = parseUserId(userRecordStr);
        record.name = unescape(parseUserName(userRecordStr));
        record.password = unescape(parseUserPassword(userRecordStr));
        record.createdOn = parseUserTimestamp(userRecordStr);
        record.email = parseUserEmail(userRecordStr);
        record.isAdmin = parseUserIsAdmin(userRecordStr);
        record.isApproved = parseUserIsApproved(userRecordStr);
        record.expiresOn = parseUserExpiresTimestamp(userRecordStr);
        record.group = unescape(parseUserGroup(userRecordStr));
        record.firstName = unescape(parseUserFirstName(userRecordStr));
        record.lastName = unescape(parseUserLastName(userRecordStr));
        record.affiliation = unescape(parseUserAffiliation(userRecordStr));
        
        return record;
    }
    
    /**
     * Represents the index of fields in CSV database for user record.
     */
    private static final int
            CERT_ID_INDEX = 0,              // The primary key for cert
            CERT_USER_ID_INDEX = 1,         // Foreign key for user
            CERT_USER_CERT_NAME_INDEX = 2,  // Index for name choosen for cert
            CERT_TIMESTAMP_INDEX = 3;       // Index for issue timestamp
    
    /**
     * Parse out the unique cert id from a cert issue record.
     */
    private static int parseCertId(String certRecordStr) {
        if (certRecordStr == null) return -1;
        String[] tokens = certRecordStr.split(",");
        return Integer.parseInt(tokens[CERT_ID_INDEX].trim());
    }
    
    /**
     * Parse out user id (foreign key) from a cert issue record.
     */
    private static int parseCertUserId(String certRecordStr) {
        if (certRecordStr == null) return -1;
        String[] tokens = certRecordStr.split(",");
        return Integer.parseInt(tokens[CERT_USER_ID_INDEX].trim());
    }
    
    /**
     * Parse out user-selected name from cert issue record
     */
    private static String parseCertCertName(String certRecordStr) {
        if (certRecordStr == null) return null;
        String[] tokens = certRecordStr.split(",");
        return tokens[CERT_USER_CERT_NAME_INDEX].trim();
    }
    
    /**
     * Parse out timestamp of issue from a cert issue record
     */
    private static long parseCertTimestamp(String certRecordStr) {
        if (certRecordStr == null) return -1;
        String[] tokens = certRecordStr.split(",");
        return Long.parseLong(tokens[CERT_TIMESTAMP_INDEX].trim());
    }
    
    private static CertificateRecord parseCertificateRecord(String certRecordStr) {

        CertificateRecord record = new CertificateRecord();
        
        record.id = parseCertId(certRecordStr);
        record.userId = parseCertUserId(certRecordStr);
        record.userCertName = parseCertCertName(certRecordStr);
        record.createdOn = parseCertTimestamp(certRecordStr);
        
        return record;
    }
    
    /***************************************************************************
     * MISC
     ***************************************************************************/
    /**
     * Backup files before a write operation.
     */
    private static synchronized void backupFiles() throws Exception {
//        copyFile(USERS_DATABASE_FILE,USERS_DATABASE_BACKUP);
//        copyFile(CERTS_DATABASE_FILE,CERTS_DATABASE_BACKUP);
    }
    
    /**
     * Copy file byte-by-byte.
     * @param in Input file
     * @param out Output file
     */
    public static synchronized void copyFile(File in, File out) throws Exception {
        lazyLoad();
        
        FileInputStream fis  = new FileInputStream(in);
        FileOutputStream fos = new FileOutputStream(out);
        try {
            byte[] buf = new byte[1024];
            int i = 0;
            while((i=fis.read(buf))!=-1) {
                fos.write(buf, 0, i);
            }
        }
        
        finally {
            fis.close();
            fos.flush();
            fos.close();
        }
    }
    
    private static boolean lazyLoaded = false;
    
    /**
     * Check databases, etc.
     */
    private static synchronized void lazyLoad() throws Exception {
        if (lazyLoaded) {
            return;
        }
        
        // Any lazy loading code goes here
        if (!USERS_DATABASE_FILE.exists() && !CERTS_DATABASE_FILE.exists()) {
            throw new Exception("Missing users and certs databases!");
        }
        
        else if (!USERS_DATABASE_FILE.exists()) {
            throw new Exception("Missing users database!");
        }
        
        else if (!CERTS_DATABASE_FILE.exists()) {
            throw new Exception("Missing certs database!");
        }
        
        // Migrate any records without first name, last name or affiliation
        migrateToFirstLastNameAffiliation();
        
        // If no exception, it was loaded
        lazyLoaded = true;
    }
    
    /**
     *
     */
    private static void migrateOldRecords() throws Exception {
        
        // This is no longer valid, so return
        if (true) {
            return;
        }
        
//        File migrationTmpFile = new File(USERS_DATABASE_FILE.getParent(),USERS_DATABASE_FILE.getName()+".migrate");
//                
//        BufferedReader reader = new BufferedReader(new FileReader(USERS_DATABASE_FILE));
//        BufferedWriter writer = new BufferedWriter(new FileWriter(migrationTmpFile));
//        try {
//            
//            String nextLine;
//            int nextTokenCount = 0;
//            while ((nextLine = reader.readLine()) != null) {
//                nextTokenCount = nextLine.split(",").length;
//                
//                if (nextTokenCount == 7) {
//                    // Add a never-expires timestamp and no-group token
//                    writer.write(nextLine+","+Long.MAX_VALUE+",-"+NEWLINE);
//                } else if (nextTokenCount == 8) {
//                    // Add a no-group token
//                    writer.write(nextLine+",-"+NEWLINE);
//                } else if (nextTokenCount == 9) {
//                    // Just copy the line
//                    writer.write(nextLine+NEWLINE);
//                } else {
//                    // Bad line...
//                }
//            }
//            
//        } finally {
//            reader.close();
//            writer.flush();
//            writer.close();
//            
//            // Naw, manually swap files to be safe... =)
////            copyFile(migrationTmpFile,USERS_DATABASE_FILE);
//        }
    }
    
    /**
     * 
     */
    private static synchronized void migrateToFirstLastNameAffiliation() throws Exception {
        
        BufferedReader reader = null;
        BufferedWriter writer = null;
        
        try {
            // First see whether we need to migrate. If not, we wont
            boolean isNeedToMigrate = false;
            
            reader = new BufferedReader(new FileReader(USERS_DATABASE_FILE));
            String nextRecord;
            while ((nextRecord = reader.readLine()) != null) {
                String[] records = nextRecord.split(",");
                
                // If there are nine entries, we'll need to migrate
                if (records.length == 9) {
                    isNeedToMigrate = true;
                    
                    // Don't break -- run through entire file to determine
                    // whether we have malformed entries
                    //break;
                } 
                
                // If there aren't 9 or 12 entries, error
                else if (records.length != 12) {
                    throw new Exception("Unexpected number of entries.");
                }
            }
            
            IOUtil.safeClose(reader);
            
            if (isNeedToMigrate) {
                
                System.out.println("Auto certificate user database is being migrated to include new fields: first name, last name and affiliation");
                
                // Save a backup. It will include the date at the end.
                File backupFile = new File(USERS_DATABASE_FILE.getParent(),USERS_DATABASE_FILE.getName()+"."+Text.getFormattedDateSimple(System.currentTimeMillis()));
                IOUtil.copyFile(USERS_DATABASE_FILE, backupFile);
                
                reader = new BufferedReader(new FileReader(backupFile));
                writer = new BufferedWriter(new FileWriter(USERS_DATABASE_FILE,false));
            
                while ((nextRecord = reader.readLine()) != null) {
                    String[] records = nextRecord.split(",");
                    
                    if (records.length == 9) {
                        nextRecord+=",-,-,-";
                    }
                    
                    writer.write(nextRecord+NEWLINE);
                }
            }
        } finally {
            if (reader != null) {
                IOUtil.safeClose(reader);
            }
            if (writer != null) {
                IOUtil.safeClose(writer);
            }
            
            // Don't delete the temp file. It should remain perchance we need
            // to restore data.
        }
    }
    
    /**
     * Replace newline w/ a token for database integrity.
     */
    private static String escape(String str) {
        if (str == null) return null;
        str = ViewUtil.convertBreakTagsToNewlines(str);
        return str.replaceAll("\r","").replaceAll(NEWLINE,"[NEWLINE]").replaceAll(",","[COMMA]");
    }
    
    /**
     * Restore newline from tokens to restore originial message.
     */
    private static String unescape(String str) {
        if (str == null) return null;
        str = str.replaceAll("\\[COMMA\\]",",").replaceAll("\\[NEWLINE\\]",NEWLINE);
        return ViewUtil.convertNewlinesToBreakTags(str);
    }
}
