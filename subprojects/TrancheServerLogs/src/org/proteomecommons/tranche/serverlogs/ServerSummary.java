/*
 * ServerSummary.java
 *
 * Created on February 9, 2008, 12:28 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.proteomecommons.tranche.serverlogs;

/**
 * Quick server summaries. Compares based on total disk space.
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 */
public class ServerSummary implements Comparable {
    
    private String name;
    private long totalSpace;
    private long spaceUsed;
    
    /** Creates a new instance of ServerSummary */
    public ServerSummary(String name, long totalSpace, long spaceUsed) {
        this.setName(name);
        this.setTotalSpace(totalSpace);
        this.setSpaceUsed(spaceUsed);
    }
    
    public int compareTo(Object o) {
        if (o instanceof ServerSummary) {
            ServerSummary s = (ServerSummary)o;
            
            // Check for equality first
            if (s.getTotalSpace() == this.getTotalSpace())
                return 0;
            
            // Determine non-equality
            return this.getTotalSpace() < s.getTotalSpace() ? -1 : 1;
        }
        
        // If gets here, nonsense
        return -1;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTotalSpace() {
        return totalSpace;
    }

    public void setTotalSpace(long totalSpace) {
        this.totalSpace = totalSpace;
    }

    public long getSpaceUsed() {
        return spaceUsed;
    }

    public void setSpaceUsed(long spaceUsed) {
        this.spaceUsed = spaceUsed;
    }
    
}
