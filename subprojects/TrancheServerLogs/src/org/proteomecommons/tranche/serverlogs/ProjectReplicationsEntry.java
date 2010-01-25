/*
 * ProjectReplicationsEntry.java
 *
 * Created on March 20, 2008, 3:22 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.proteomecommons.tranche.serverlogs;

import org.tranche.hash.Base16;
import org.tranche.hash.BigHash;

/**
 * Encapsulates relevant information about a project and its hash replications
 * @author Bryan E. Smith <bryanesmith@gmail.com>
 */
public class ProjectReplicationsEntry {
    
    public BigHash hash = null;
    public long reps0 = 0, reps1 = 0, reps2 = 0, reps3 = 0, reps4 = 0, reps5orMore = 0;
    
    public long lastUpdateTimestamp = -1;
    public int updateCount = 0;
    
    /** Creates a new instance of ProjectReplicationsEntry */
    private ProjectReplicationsEntry() {
    }
    
    /**
     * Create a ProjectReplicationsEntry using hash and number of replications.
     * @param hash
     * @param reps0
     * @param reps1
     * @param reps2
     * @param reps3
     * @param reps4
     * @param reps5orMore
     */
    public static ProjectReplicationsEntry createEntry(BigHash hash, long timestampLastUpdate, long reps0, long reps1, long reps2, long reps3, long reps4, long reps5orMore, int updateCount) {
        ProjectReplicationsEntry entry = new ProjectReplicationsEntry();
        
        entry.hash = hash;
        entry.reps0 = reps0;
        entry.reps1 = reps1;
        entry.reps2 = reps2;
        entry.reps3 = reps3;
        entry.reps4 = reps4;
        entry.reps5orMore = reps5orMore;
        entry.lastUpdateTimestamp = timestampLastUpdate;
        entry.updateCount = updateCount;
        
        return entry;
    }
    
    /**
     * <p>Create a ProjectReplicationsEntry from a CSV entry with following format:</p>
     * <p>[title],[hash],[timestamp last update],[0 rep count],[1 rep count],[2 rep count],[3 rep count],[4 rep count],[5 or more rep count],[num updates]</p>
     * <p>Title and status are ignored</p>
     * @param csvEntry The entry is a valid CSV entry
     */
    public static ProjectReplicationsEntry createEntryFromCSV(String csvEntry) throws Exception {
        String[] tokens = csvEntry.split(",");
        
        // Get hash
        String hashStr = tokens[1];
        if (hashStr.startsWith("\"")) {
            hashStr = hashStr.substring(1);
        }
        if (hashStr.endsWith("\"")) {
            hashStr = hashStr.substring(0,hashStr.length()-1);
        }
        BigHash hash = BigHash.createHashFromString(hashStr);
        
        long reps0 = 0, reps1 = 0, reps2 = 0, reps3 = 0, reps4 = 0, reps5orMore = 0;
        
        reps0 = Long.parseLong(tokens[3]);
        reps1 = Long.parseLong(tokens[4]);
        reps2 = Long.parseLong(tokens[5]);
        reps3 = Long.parseLong(tokens[6]);
        reps4 = Long.parseLong(tokens[7]);
        reps5orMore = Long.parseLong(tokens[8]);
        
        // The following may or may not have been recorded. Gracefully handle...
        long lastUpdateTimestamp = -1;
        int numUpdates = 1;
        
        try {
            lastUpdateTimestamp = Long.parseLong(tokens[2]);
        } catch (Exception ignore) { /* nope */ }
        
        try {
            numUpdates = Integer.parseInt(tokens[9]);
        } catch (Exception ignore) { /* nope */ }
        
        return createEntry(hash, lastUpdateTimestamp, reps0, reps1, reps2, reps3, reps4, reps5orMore, numUpdates);
    }
    
    /**
     * Produces a String representing entry. fromString will recreate the object from the string. 
     * Particularly useful if passing as a request to servlet.
     */
    @Override()
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        
        String hashStr = "-";
        if (this.hash != null) {
            hashStr = Base16.encode(this.hash.toByteArray());
        }
        
        buffer.append(hashStr+":");
        buffer.append(this.reps0+":");
        buffer.append(this.reps1+":");
        buffer.append(this.reps2+":");
        buffer.append(this.reps3+":");
        buffer.append(this.reps4+":");
        buffer.append(this.reps5orMore);
        
        return buffer.toString();
    }
    
    /**
     * Create ProjectReplicationsEntry from String produced by toString method. Used primarily for recreating object from a request string passed to a servlet.
     * Particularly useful if passing as a request to servlet.
     */
    public static ProjectReplicationsEntry fromString(String str) {
        long reps0 = 0, reps1 = 0, reps2 = 0, reps3 = 0, reps4 = 0, reps5orMore = 0;
        
        String[] tokens = str.split(":");
        
        // Make sure hash. A hyphen means no hash.
        BigHash hash = null;
        if (!tokens[0].trim().equals("-")) {
            hash = BigHash.createHashFromString(tokens[0]);
        }
        reps0 = Long.parseLong(tokens[1]);
        reps1 = Long.parseLong(tokens[2]);
        reps2 = Long.parseLong(tokens[3]);
        reps3 = Long.parseLong(tokens[4]);
        reps4 = Long.parseLong(tokens[5]);
        reps5orMore = Long.parseLong(tokens[6]);
        
        // The following may or may not have been recorded. Gracefully handle...
        long lastUpdateTimestamp = -1;
        int numUpdates = 1;
        
        try {
            lastUpdateTimestamp = Long.parseLong(tokens[2]);
        } catch (Exception ignore) { /* nope */ }
        
        try {
            numUpdates = Integer.parseInt(tokens[9]);
        } catch (Exception ignore) { /* nope */ }
        
        return createEntry(hash, lastUpdateTimestamp, reps0, reps1, reps2, reps3, reps4, reps5orMore, numUpdates);
    }
}
