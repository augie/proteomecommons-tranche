/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.scripts.status;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.tranche.network.*;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class TrancheStatusTableRow implements Comparable {

    private final List<TrancheStatusTableEntry> entries;
    public final String host,  name,  buildNumber,  url;
    public final boolean isAdminWritabled;
    public final long diskSpace,  diskSpaceUsed;

    /**
     * 
     * @param host
     * @param name
     * @param buildNumber
     */
    public TrancheStatusTableRow(String host, String name, String buildNumber, String url, boolean isAdminWritable, long diskSpace, long diskSpaceUsed) {
        this.host = host;
        this.name = name;
        this.buildNumber = buildNumber;
        this.url = url;
        this.entries = new LinkedList();
        this.isAdminWritabled = isAdminWritable;
        this.diskSpace = diskSpace;
        this.diskSpaceUsed = diskSpaceUsed;
    }

    public void add(StatusTable t) {
        for (StatusTableRow row : t.getRows()) {
            TrancheStatusTableEntry tst = TrancheStatusTableEntry.create(row);
            if (!this.entries.contains(tst)) {
                this.entries.add(tst);
            }
        }

        Collections.sort(this.entries);
    }

    public List<TrancheStatusTableEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public int compareTo(Object o) {
        if (o instanceof TrancheStatusTableRow) {
            TrancheStatusTableRow other = (TrancheStatusTableRow) o;
            return other.host.compareTo(this.host);
        }
        throw new RuntimeException("Can't compare to non-TrancheStatusTableRow object!");
    }

    @Override()
    public int hashCode() {
        return this.host.hashCode();
    }

    @Override()
    public boolean equals(Object o) {

        if (o instanceof TrancheStatusTableRow) {
            TrancheStatusTableRow other = (TrancheStatusTableRow) o;
            return other.host.equals(this.host);
        }

        return false;
    }
}
