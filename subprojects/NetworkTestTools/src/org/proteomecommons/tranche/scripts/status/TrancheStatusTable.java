/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.scripts.status;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.proteomecommons.tranche.scripts.utils.ScriptsUtil;
import org.tranche.commons.TextUtil;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class TrancheStatusTable {

    private List<TrancheStatusTableRow> rows;

    public TrancheStatusTable() {
        rows = new LinkedList();
    }

    public void add(TrancheStatusTableRow row) {
        rows.add(row);
        Collections.sort(rows);
    }

    public void printTable() {

        // Assign numbers to everything
        int count = 0;
        Map<Integer, String> keys = new HashMap();
        for (TrancheStatusTableRow row : getRows()) {
            for (TrancheStatusTableEntry e : row.getEntries()) {
                if (!keys.containsValue(e.host)) {
                    keys.put(count, e.host);
                    count++;
                }
            }
        }

        final int nameLen = 40;
        final int buildLen = 6;
        final int loadingLen = 12;
        final int fieldLen = 5;
        final int availableSpaceLen = 15;
        String hr = null;

        final String offlineFlag = "X",  notInTable = "-",  writableFlag = "w",  readableFlag = "r",  fullHashSpanFlag = "f",  fullTargetHashSpanFlag = "t";


        System.out.println("LEGEND:");
        System.out.println("    Offline................. " + offlineFlag);
        System.out.println("    Not in table............ " + notInTable);
        System.out.println("    Readable................ " + readableFlag);
        System.out.println("    Writable................ " + writableFlag);
        System.out.println("    Full hash span.......... " + fullHashSpanFlag);
        System.out.println("    Full target hash span... " + fullTargetHashSpanFlag);

        System.out.println("SERVERS:");
        List<Integer> keyList = new LinkedList<Integer>(keys.keySet());
        Collections.sort(keyList);
        for (Integer key : keyList) {
            System.out.println("    " + key + ": " + ScriptsUtil.getServerNameAndHostName(keys.get(key)));
        }

        System.out.println();

        // Create horizontal rule
        {
            StringBuffer hrBuf = new StringBuffer();
            for (int i = 0; i < nameLen; i++) {
                hrBuf.append("-");
            }
            for (int i = 0; i < buildLen + 1; i++) {
                hrBuf.append("-");
            }
            for (int i = 0; i < loadingLen + 1; i++) {
                hrBuf.append("-");
            }
            for (int i = 0; i < availableSpaceLen + 1; i++) {
                hrBuf.append("-");
            }
            for (Integer nextKey : keys.keySet()) {
                for (int i = 0; i < fieldLen; i++) {
                    hrBuf.append("-");
                }
                hrBuf.append("-");
            }
            hrBuf.append("-");
            hr = hrBuf.toString();
        }

        // Print out horizontal rule
        System.out.println(hr);

        // Print out header
        {
            // Print out space for name
            StringBuffer nameBuf = new StringBuffer();
            for (int i = 0; i < nameLen; i++) {
                nameBuf.append(" ");
            }

            System.out.print(nameBuf);

            // Print out header for build
            {
                final String label = "build";
                final int keyDiff = buildLen - label.length();

                StringBuffer header = new StringBuffer();
                header.append(label);
                for (int i = 0; i < keyDiff; i++) {
                    header.append(" ");
                }
                System.out.print("|" + header.toString());
            }

            // Print out header for starting up
            {
                final String label = "DBU Loading ";
                final int keyDiff = loadingLen - label.length();

                StringBuffer header = new StringBuffer();
                header.append(label);
                for (int i = 0; i < keyDiff; i++) {
                    header.append(" ");
                }
                System.out.print("|" + header.toString());
            }

            // Print out header for available space
            {
                final String label = "Available";
                final int keyDiff = availableSpaceLen - label.length();

                StringBuffer header = new StringBuffer();
                header.append(label);
                for (int i = 0; i < keyDiff; i++) {
                    header.append(" ");
                }
                System.out.print("|" + header.toString());
            }

            // Print out headers for other servers
            for (Integer nextKey : keyList) {
                StringBuffer keyBuf = new StringBuffer();
                keyBuf.append(nextKey);
                int keyDiff = fieldLen - keyBuf.length();

                for (int i = 0; i < keyDiff; i++) {
                    keyBuf.append(" ");
                }

                System.out.print("|" + keyBuf);
            }
            System.out.println("|");
        }

        // Print out horizontal rule
        System.out.println(hr);

        // Print out each row
        for (TrancheStatusTableRow row : getRows()) {
            int rowNumber = -1;

            for (Integer nextKey : keyList) {
                if (keys.get(nextKey).equals(row.host)) {
                    rowNumber = nextKey;
                    break;
                }
            }

            if (rowNumber == -1) {
                throw new RuntimeException("Assertion failed, couldn't find key for: " + row.host);
            }

            StringBuffer nameBuf = new StringBuffer();

            final String name = ScriptsUtil.getServerNameAndHostName(row.host, row.name);

            nameBuf.append(rowNumber + ". " + name);

            int nameDiff = nameLen - nameBuf.length();
            for (int i = 0; i < nameDiff; i++) {
                nameBuf.append(" ");
            }

            System.out.print(nameBuf);

            // Print out build
            {
                final String buildNumber = (row.buildNumber == null ? "" : row.buildNumber);
                final int diff = buildLen - buildNumber.length();

                StringBuffer val = new StringBuffer();
                val.append(buildNumber);
                for (int i = 0; i < diff; i++) {
                    val.append(" ");
                }

                System.out.print("|" + val);
            }

            // Print out whether loading
            {
                StringBuffer buf = new StringBuffer();

                if (row.isStartingUp) {
                    buf.append("yes");
                } else {
                    buf.append("-");
                }

                while (buf.length() < loadingLen - 1) {
                    buf.append(" ");
                }

                System.out.print("| " + buf);
            }

            // Print out available space
            {
                StringBuffer buf = new StringBuffer();

                // If not writeable, just put -
                if (!row.isAdminWritabled) {
                    buf.append("-");
                } else {
                    buf.append(TextUtil.formatBytes(row.diskSpace - row.diskSpaceUsed));
                }

                while (buf.length() < availableSpaceLen - 1) {
                    buf.append(" ");
                }

                System.out.print("| " + buf);
            }

            for (Integer nextKey : keyList) {

                String nextHost = keys.get(nextKey);

                StringBuffer keyBuf = new StringBuffer();

                // Find the entry
                TrancheStatusTableEntry e = null;

                for (TrancheStatusTableEntry nextE : row.getEntries()) {
                    if (nextE.host.equals(nextHost)) {
                        e = nextE;
                        break;
                    }
                }

                if (e == null) {
                    keyBuf.append("-");
                } else {
                    if (!e.isOnline) {
                        keyBuf.append(offlineFlag);
                    } else {
                        if (e.isReadable) {
                            keyBuf.append(readableFlag);
                        }
                        if (e.isWritable) {
                            keyBuf.append(writableFlag);
                        }
                        if (e.isFullHashSpan) {
                            keyBuf.append(fullHashSpanFlag);
                        }
                        if (e.isFullTargetHashSpan) {
                            keyBuf.append(fullTargetHashSpanFlag);
                        }
                    }
                }

                int keyDiff = fieldLen - keyBuf.length();

                for (int i = 0; i < keyDiff; i++) {
                    keyBuf.append(" ");
                }

                System.out.print("|" + keyBuf);
            }
            System.out.println("|");
        }

        // Print out horizontal rule
        System.out.println(hr);
    }

    public List<TrancheStatusTableRow> getRows() {
        return Collections.unmodifiableList(rows);
    }
}

