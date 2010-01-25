/*
 *    Copyright 2005 The Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.proteomecommons.tranche.modules.xarsummary;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.Text;

/**
 * <p>Summary of the most important information from a XAR file.</p>
 * <p>Changed to be disk backed to avoid out-of-memory errors</p>
 *
 * @author  Bryan Smith - bryanesmith@gmail.com
 * @version %I%, %G%
 * @since   1.0
 */
public class XARSummary {
    // Disk backed to avoid out of memory errors
    private File file = TempFileUtil.createTemporaryFile();
    private File fileForIterator = null;

    /**
     * @return  the buffer created as a string
     * @since   1.0
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        if (getExperimentName() != null) {
            buffer.append("Experiment name: " + getExperimentName() + Text.getNewLine());
        }
        if (getExperimentComments() != null) {
            buffer.append("Experiment comments: " + getExperimentComments() + Text.getNewLine());
        }
        int count = 1;
        XARProtocol p;
        while ((p = this.getNextProtocol()) != null) {
            buffer.append(Text.getNewLine() + "Protocol #" + count + Text.getNewLine());
            buffer.append(p + Text.getNewLine());
            count++;
        }

        return buffer.toString();
    }

    public String getExperimentName() {
        return Text.detokenizeNewlines(getValue(file, "<NAME>", "</NAME>"));
    }

    public void setExperimentName(String experimentName) {
        writeToDisk(file, "<NAME>", Text.tokenizeNewlines(experimentName), "</NAME>");
    }

    public String getExperimentComments() {
        return Text.detokenizeNewlines(getValue(file, "<COMMENTS>", "</COMMENTS>"));
    }

    public void setExperimentComments(String experimentComments) {
        writeToDisk(file, "<COMMENTS>", Text.tokenizeNewlines(experimentComments), "</COMMENTS>");
    }

    public void addProtocol(XARProtocol protocol) {
        writeToDisk(file, "<PROTOCOL>", Text.tokenizeNewlines(protocol.toString()), "</PROTOCOL>");
    }

    /**
     * Get each protocol in turn to be nice to memory.
     */
    public XARProtocol getNextProtocol() {

        // See whether file exists. If not, create
        if (fileForIterator == null) {
            buildIteratorFromDisk();
        }

        // If iterated through, recreate (perchance later needed) and return null
        if (!fileContainsAnotherElement(fileForIterator, "<PROTOCOL>", "</PROTOCOL>")) {
            buildIteratorFromDisk();
            return null;
        }

        return XARProtocol.createFromString(Text.detokenizeNewlines(getValueDestructive("<PROTOCOL>", "</PROTOCOL>")));
    }

    public void setNextProtocol(XARProtocol p) {
        writeToDisk(file, "<PROTOCOL>", p.toString(), "</PROTOCOL>");
    }

    protected void finalize() throws Throwable {
        destroy();
    }
    /**
     * Destroy the item explicitly. Otherwise, finalize will handle this later.
     */
    private boolean isDestroyed = false;

    public void destroy() {
        if (isDestroyed) {
            return;
        }
        isDestroyed = true;
        IOUtil.safeDelete(file);
        IOUtil.safeDelete(fileForIterator);

        // Flag components for GC
        file = null;
        fileForIterator = null;
    }

    /**
     * Helper method appends contents to file.
     */
    private void writeToDisk(File file, String openTag, String str, String closeTag) {

        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(file, true));
            out.write(openTag + Text.getNewLine() + str + Text.getNewLine() + closeTag + Text.getNewLine());
        } catch (Exception e) {/* Keep going */

        } finally {
            IOUtil.safeClose(out);
        }
    }

    /**
     * Helper method, returns true if iterator file contains text for tags...
     */
    private boolean fileContainsAnotherElement(File file, String openTag, String closeTag) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(file));
            String line;
            while ((line = in.readLine()) != null) {
                // Found opening tag
                if (line.equals(openTag)) {
                    while ((line = in.readLine()) != null) {

                        // Find closing tag
                        if (line.equals(closeTag)) {
                            return true;
                        }
                    }
                }
            }

        } catch (Exception e) {
            /* Keep going */
            System.out.println(e.getMessage());
        } finally {
            IOUtil.safeClose(in);
        }

        // Fail
        return false;
    }

    /**
     * Helper method retrieves contents from disk.
     */
    private String getValue(File file, String openTag, String closeTag) {
        StringBuffer value = new StringBuffer();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(file));
            String line;
            while ((line = in.readLine()) != null) {

                // Found opening tag
                if (line.equals(openTag)) {
                    while ((line = in.readLine()) != null) {

                        if (line.equals(closeTag)) {
                            return value.toString();
                        }

                        value.append(line);
                    }
                }
            }

        } catch (Exception e) {/* Keep going */

        } finally {
            IOUtil.safeClose(in);
        }
        return null;
    }

    /**
     * Helper method retrieves contents from disk. Removes entry from file.
     */
    private String getValueDestructive(String openTag, String closeTag) {
        StringBuffer value = new StringBuffer();
        BufferedReader in = null;
        BufferedWriter out = null;
        File copy = TempFileUtil.createTemporaryFile();
        try {
            in = new BufferedReader(new FileReader(fileForIterator));
            out = new BufferedWriter(new FileWriter(copy));
            String line;
            while ((line = in.readLine()) != null) {

                // Found opening tag
                if (line.equals(openTag)) {
                    while ((line = in.readLine()) != null) {

                        if (line.equals(closeTag)) {

                            // Write remaining file
                            while ((line = in.readLine()) != null) {
                                out.write(line + Text.getNewLine());
                            }

                            return value.toString();
                        }

                        value.append(line);
                    }
                }

                // Write pre-element entries
                out.write(line + Text.getNewLine());
            }

        } catch (Exception e) {/* Keep going */

        } finally {
            IOUtil.safeClose(in);
            IOUtil.safeClose(out);

            // Remove previous iterator, replace
            IOUtil.safeDelete(fileForIterator);
            fileForIterator = copy;
        }
        return null;
    }

    /**
     * Helper method to return a disk copy of current file (for new iterator file)
     */
    private void buildIteratorFromDisk() {

        // If existing iterator file, delete
        if (fileForIterator != null) {
            IOUtil.safeDelete(fileForIterator);
        }

        // Create new iterator file
        fileForIterator = TempFileUtil.createTemporaryFile();
        BufferedReader in = null;
        BufferedWriter out = null;

        try {
            in = new BufferedReader(new FileReader(file));
            out = new BufferedWriter(new FileWriter(fileForIterator));

            String str = null;
            while ((str = in.readLine()) != null) {
                out.write(str + Text.getNewLine());
            }
        } catch (Exception e) { /* no */ } finally {
            IOUtil.safeClose(in);
            IOUtil.safeClose(out);
        }
    }
}
