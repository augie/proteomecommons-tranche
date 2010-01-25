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

import org.tranche.util.Text;

/**
 * <p>Represents a single protocol node in a XAR file.</p>
 *
 * @author  Bryan Smith - bryanesmith@gmail.com
 * @version %I%, %G%
 * @since   1.0
 */
class XARProtocol {

    public String name;
    public String description;
    public String applicationType;
    public String lsid;

    /**
     * @since   1.0
     */
    public XARProtocol() {
        name = null;
        description = null;
        applicationType = null;
    }

    /**
     * @return  the buffer created as a string
     * @since   1.0
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        if (name != null) {
            buffer.append("Protocol name: " + Text.tokenizeNewlines(name) + Text.getNewLine());
        }
        if (lsid != null) {
            buffer.append("LSID: " + Text.tokenizeNewlines(lsid) + Text.getNewLine());
        }
        if (description != null) {
            buffer.append("Description: " + Text.tokenizeNewlines(description) + Text.getNewLine());
        }
        if (applicationType != null) {
            buffer.append("Application-type: " + Text.tokenizeNewlines(applicationType));
        }
        return buffer.toString();
    }

    public static XARProtocol createFromString(String str) {

        XARProtocol protocol = new XARProtocol();

        String[] lines = str.split(Text.getNewLine());

        for (String line : lines) {
            if (line.startsWith("Protocol name: ")) {
                protocol.name = Text.detokenizeNewlines(line.substring(15));
            } else if (line.startsWith("LSID: ")) {
                protocol.lsid = Text.detokenizeNewlines(line.substring(6));
            } else if (line.startsWith("Description: ")) {
                protocol.description = Text.detokenizeNewlines(line.substring(13));
            } else if (line.startsWith("Application-type: ")) {
                protocol.applicationType = Text.detokenizeNewlines(line.substring(18));
            }
        }

        return protocol;
    }
}
