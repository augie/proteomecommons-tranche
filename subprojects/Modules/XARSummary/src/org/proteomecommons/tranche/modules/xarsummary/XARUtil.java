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
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.tranche.util.IOUtil;

/**
 * <p>Parses a well-formated XAR file. For more info, see https://www.labkey.org/Wiki/home/Documentation/page.view?name=tutorial2</p>
 *
 * @author  Bryan Smith - bryanesmith@gmail.com
 * @version %I%, %G%
 * @since   1.0
 */
public class XARUtil {

    private final static File file = null;

    /**
     * Create a XAR summary from a file. Works with well-formed XAR files.
     *
     * @param   file        the file from which the summary is created
     * @return              the XAR summary
     * @throws  Exception   if any exception occurs
     * @since               1.0
     */
    public static XARSummary createXARSummary(File file) throws Exception {

        XARSummary summary = new XARSummary();

        BufferedReader in = null;

        // For gathering up multi-line nodes
        List<String> tempNode = null;

        try {
            in = new BufferedReader(new FileReader(file));

            String str;

            // Experiment node
            while ((str = in.readLine()) != null) {

                // Quit after node complete
                if (str.contains("</exp:Experiment>")) {
                    break;                // Get experiment name node
                }
                if (str.contains("<exp:Name>")) {
                    tempNode = new ArrayList();
                    tempNode.add(str);

                    while (!str.contains("</exp:Name>") && (str = in.readLine()) != null) {
                        tempNode.add(str);
                    }

                    summary.setExperimentName(extractValueFromNode(tempNode));
                } // Get experiment comments node
                else if (str.contains("<exp:Comments>")) {
                    tempNode = new ArrayList();
                    tempNode.add(str);

                    while (!str.contains("</exp:Comments>") && (str = in.readLine()) != null) {
                        tempNode.add(str);
                    }

                    summary.setExperimentComments(extractValueFromNode(tempNode));
                }
            }

            // ProtocolDefinitions node
            while ((str = in.readLine()) != null) {

                // Quit after node complete
                if (str.contains("</exp:ProtocolDefinitions>")) {
                    break;                // Get next Protocol node
                } else if (str.contains("<exp:Protocol")) {

                    tempNode = new ArrayList();
                    tempNode.add(str);

                    while (!str.contains("</exp:Protocol>") && (str = in.readLine()) != null) {
                        tempNode.add(str);
                    }

                    summary.addProtocol(extractProtocol(tempNode));
                }
            }
        } finally {
            IOUtil.safeClose(in);
        }

        return summary;
    }

    /**
     * Helper method to extract protocol information.
     *
     * @param   lines   the list of lines
     * @return          the XAR 
     * @since           1.0
     */
    private static XARProtocol extractProtocol(List<String> lines) {

        XARProtocol protocol = new XARProtocol();

        // For building up an XML node
        List<String> tempNode = null;

        String line;
        for (int ptr = 0; ptr < lines.size(); ptr++) {
            line = lines.get(ptr);

            // Try to get the LSID
            if (line.contains("rdf:about=")) {

                Pattern p = Pattern.compile(".*rdf:about=\"(.*)\".*");
                Matcher m = p.matcher(line);

                if (m.matches()) {
                    protocol.lsid = m.group(1);
                }
            } else if (line.contains("<exp:Name>")) {
                tempNode = new ArrayList();
                tempNode.add(line);

                while (!line.contains("</exp:Name>") && ptr < lines.size()) {
                    line = lines.get(++ptr);
                    tempNode.add(line);
                }

                protocol.name = extractValueFromNode(tempNode);
            } else if (line.contains("<exp:ProtocolDescription>")) {
                tempNode = new ArrayList();
                tempNode.add(line);

                while (!line.contains("</exp:ProtocolDescription>") && ptr < lines.size()) {
                    line = lines.get(++ptr);
                    tempNode.add(line);
                }

                protocol.description = extractValueFromNode(tempNode);
            } else if (line.contains("<exp:ApplicationType>")) {
                tempNode = new ArrayList();
                tempNode.add(line);

                while (!line.contains("</exp:ApplicationType>") && ptr < lines.size()) {
                    line = lines.get(++ptr);
                    tempNode.add(line);
                }

                protocol.applicationType = extractValueFromNode(tempNode);
            }

        }

        return protocol;
    }

    /**
     * Helper method to extract the value from an XML node.
     * 
     * @param   node    the XML node
     * @return          the buffer created as a string
     * @since           1.0
     */
    private static String extractValueFromNode(List<String> node) {

        StringBuffer singleLine = new StringBuffer();

        String element = null;

        for (String str : node) {
            if (str.contains("<")) {
                Pattern p = Pattern.compile(".*?<(.*?)>.*");
                Matcher m = p.matcher(str);

                if (m.matches()) {
                    element = m.group(1).split(" ")[0];
                    break;
                }
            }
        }

        if (element == null) {
            return null;
        }

        // Convert to single line for easy parsing
        for (String str : node) {
            singleLine.append(str.trim() + " ");
        }


        Pattern p = Pattern.compile(".*?<" + element + ".*?>(.*)</" + element + ">.*?");
        Matcher m = p.matcher(singleLine.toString());

        if (m.matches()) {
            return m.group(1).trim();
        }

        // Nothing
        return null;
    }

    /**
     * Prints the summary info from XAR to standard out.
     *
     * @param   args    the command line arguments
     * @since           1.0
     */
    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Only 1 argument, a XAR file path.");
            return;
        }

        File xar = new File(args[0]);

        XARSummary output = null;
        try {
            output = createXARSummary(xar);
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        System.out.println(output);
    }
}
