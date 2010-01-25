/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.scripts.status;

import org.tranche.hash.span.HashSpanCollection;
import org.tranche.network.StatusTableRow;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class TrancheStatusTableEntry implements Comparable {

    final String host;
    final boolean isOnline;
    final boolean isFullHashSpan;
    final boolean isFullTargetHashSpan;
    final boolean isReadable;
    final boolean isWritable;

    public static TrancheStatusTableEntry create(StatusTableRow row) {
        return new TrancheStatusTableEntry(row.getHost(), row.isOnline(), new HashSpanCollection(row.getTargetHashSpans()).isFullHashSpan(), new HashSpanCollection(row.getHashSpans()).isFullHashSpan(), row.isReadable(), row.isWritable());
    }

    public TrancheStatusTableEntry(String host, boolean isOnline, boolean isFullTargetHashSpan, boolean isFullHashSpan, boolean isReadable, boolean isWritable) {
        this.host = host;
        this.isOnline = isOnline;
        this.isFullTargetHashSpan = isFullTargetHashSpan;
        this.isFullHashSpan = isFullHashSpan;
        this.isReadable = isReadable;
        this.isWritable = isWritable;
    }

    public int compareTo(Object o) {
        if (o instanceof TrancheStatusTableEntry) {
            TrancheStatusTableEntry other = (TrancheStatusTableEntry) o;
            return other.host.compareTo(this.host);
        }
        throw new RuntimeException("Can't compare to non-StatusTableEntry object!");
    }

    @Override()
    public int hashCode() {
        return this.host.hashCode();
    }

    @Override()
    public boolean equals(Object o) {

        if (o instanceof TrancheStatusTableEntry) {
            TrancheStatusTableEntry other = (TrancheStatusTableEntry) o;
            return other.host.equals(this.host);
        }

        return false;
    }
}

