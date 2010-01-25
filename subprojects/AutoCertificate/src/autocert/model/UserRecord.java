/*
 * UserRecord.java
 *
 * Created on August 26, 2007, 7:32 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package autocert.model;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Represents single user record.
 */
public class UserRecord {
    
    // The values are required
    public int id = -1;
    public String name = null;
    public String password = null;
    public long createdOn = -1;
    public String email = null;
    public boolean isAdmin = false;
    public boolean isApproved = false;
    public String affiliation = "none";
    public String firstName = null;
    public String lastName = null;
    
    // Following optional values with defaults
    public String group = "-";
    public long expiresOn = Long.MAX_VALUE;
}

