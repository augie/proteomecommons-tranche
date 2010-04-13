/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.proteomecommons.tranche.scripts.status;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.proteomecommons.tranche.scripts.sets.PowerSet;
import org.tranche.hash.span.AbstractHashSpan;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class NetworkInformation {
    protected final Set<String> offlineHosts;
    protected final TrancheStatusTable statusTable;
    protected final List<AbstractHashSpan> writableHashSpans, writableTargetHashSpans;
    protected long totalSizeLimit;
    
    /**
     * 
     */
    public NetworkInformation() {
        this.offlineHosts = new HashSet();
        this.statusTable = new TrancheStatusTable();
        this.totalSizeLimit = 0;
        this.writableHashSpans = new LinkedList();
        this.writableTargetHashSpans = new LinkedList();
    }

    /**
     * 
     * @return
     */
    public Set<String> getOfflineHosts() {
        return offlineHosts;
    }

    /**
     * 
     * @return
     */
    public TrancheStatusTable getStatusTable() {
        return statusTable;
    }

    /**
     * 
     * @return
     */
    public long getTotalSizeLimit() {
        return totalSizeLimit;
    }
    
    /**
     * 
     * @return
     */
    public int getNumberOfWritableHashSpans() {
        return getHashSpanCount(writableHashSpans);
    }
    
    /**
     * 
     * @return
     */
    public int getNumberOfWritableTargetHashSpans() {
        return getHashSpanCount(writableTargetHashSpans);
    }
    
    /**
     * 
     * @param spans
     * @return
     */
    private static int getHashSpanCount(Collection<AbstractHashSpan> spans) {

        List<AbstractHashSpan> partialSpans = new LinkedList();

        int count = 0;
        for (AbstractHashSpan ahs : spans) {

            // If it is full, count it
            if (ahs.getAbstractionFirst() == AbstractHashSpan.ABSTRACTION_FIRST && ahs.getAbstractionLast() == AbstractHashSpan.ABSTRACTION_LAST) {
                count++;
                continue;
            }

            partialSpans.add(ahs);
        }

        while (isFindAnotherHashSpan(partialSpans)) {
            count++;
        }

        return count;
    }

    /**
     * 
     */
    private static boolean isFindAnotherHashSpan(List<AbstractHashSpan> spans) {

        PowerSet powerSet = new PowerSet(spans);

        Iterator<Set<AbstractHashSpan>> powerSetIterator = powerSet.iterator();

        Set<AbstractHashSpan> subset = null;
        boolean wasSuccess = false;

        LOOK_FOR_COMPLETE_SUBSET:
        while (powerSetIterator.hasNext()) {

            subset = powerSetIterator.next();

            if (isCompleteHashSpan(subset)) {
                wasSuccess = true;
                break LOOK_FOR_COMPLETE_SUBSET;
            }
        }

        // If was a success, remove from parent collection
        if (wasSuccess) {
            spans.removeAll(subset);
        }

        return wasSuccess;
    }

    /**
     * 
     * @param set
     * @return
     */
    private static boolean isCompleteHashSpan(Set<AbstractHashSpan> set) {

        // This is a complete hash span if 
        // 1. Hash span start is found
        // 2. Hash span is found
        // 3. All elements overlap
        // 
        // Admittedly, #3 is not required because a superfluous hash span would be included. However,
        // if this is the case, the correct subset will be found anyhow. =)
        boolean isFirstHashFound = false;
        boolean isLastHashFound = false;
        boolean isAllOverlap = true;

        CHECK:
        for (AbstractHashSpan ahs : set) {
            if (ahs.getAbstractionFirst() == AbstractHashSpan.ABSTRACTION_FIRST) {
                isFirstHashFound = true;
            }
            if (ahs.getAbstractionLast() == AbstractHashSpan.ABSTRACTION_LAST) {
                isLastHashFound = true;
            }

            boolean foundOverlap = false;

            // Look for overlap with this with any other member of subset
            for (AbstractHashSpan otherAhs : set) {
                if (otherAhs.getAbstractionFirst() == ahs.getAbstractionFirst() && otherAhs.getAbstractionLast() == ahs.getAbstractionLast()) {
                    continue;
                }

                if (ahs.overlaps(otherAhs) || ahs.isAdjecentTo(otherAhs)) {
                    foundOverlap = true;
                }
            }

            if (!foundOverlap) {
                isAllOverlap = false;
                break CHECK;
            }
        }

        return isFirstHashFound && isLastHashFound && isAllOverlap;
    }
}
