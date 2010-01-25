/*
 * CertificateRecord.java
 *
 * Created on August 26, 2007, 7:40 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package autocert.model;

/**
 * Represents a certificate record in the database.
 */
public class CertificateRecord {
    public int id;
    public int userId;
    public String userCertName; // The user name selected for the user zip file.
    public long createdOn;
}
