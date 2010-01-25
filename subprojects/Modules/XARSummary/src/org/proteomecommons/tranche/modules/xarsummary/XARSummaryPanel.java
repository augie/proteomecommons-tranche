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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.tranche.gui.Styles;
import org.tranche.util.Text;

/**
 * Panel for generic popup that renders XAR summary info.
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
public class XARSummaryPanel extends JPanel {

    public final static Dimension RECOMMENDED_DIMENSION = new Dimension(500, 400);

    public XARSummaryPanel(XARSummary xar) {

        this.setBackground(Styles.COLOR_BACKGROUND);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        int row = 0;

        // Name
        {
            JLabel label = new JLabel("Experiment Name");
            label.setFont(Styles.FONT_14PT_BOLD);
            label.setBorder(Styles.UNDERLINE_BLACK);

            gbc.gridwidth = gbc.REMAINDER;
            gbc.anchor = gbc.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = gbc.HORIZONTAL;
            gbc.insets = new Insets(10, 10, 0, 10);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(label, gbc);

            JTextArea descBox = null;

            if (xar.getExperimentName() != null) {
                descBox = new JTextArea(xar.getExperimentName());
            } else {
                descBox = new JTextArea("No name specified");
            }

            descBox.setFont(Styles.FONT_12PT);
            descBox.setEditable(false);
            descBox.setOpaque(false);
            descBox.setBorder(Styles.BORDER_NONE);
            descBox.setWrapStyleWord(true);
            descBox.setLineWrap(true);

            gbc.gridwidth = gbc.REMAINDER;
            gbc.anchor = gbc.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = gbc.BOTH;
            gbc.insets = new Insets(10, 10, 0, 10);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(new JScrollPane(descBox), gbc);
        }

        // Description
        {
            JLabel label = new JLabel("Experiment Comments");
            label.setFont(Styles.FONT_14PT_BOLD);
            label.setBorder(Styles.UNDERLINE_BLACK);

            gbc.gridwidth = gbc.REMAINDER;
            gbc.anchor = gbc.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = gbc.HORIZONTAL;
            gbc.insets = new Insets(10, 10, 0, 10);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(label, gbc);

            JTextArea descBox = null;

            if (xar.getExperimentComments() != null) {
                descBox = new JTextArea(xar.getExperimentComments());
            } else {
                descBox = new JTextArea("No comments");
            }
            descBox.setFont(Styles.FONT_12PT);
            descBox.setEditable(false);
            descBox.setOpaque(false);
            descBox.setBorder(Styles.BORDER_NONE);
            descBox.setWrapStyleWord(true);
            descBox.setLineWrap(true);

            gbc.gridwidth = gbc.REMAINDER;
            gbc.anchor = gbc.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = gbc.BOTH;
            gbc.insets = new Insets(10, 10, 0, 10);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(new JScrollPane(descBox), gbc);
        }

        // Show the protocols
        {
            // Going to relabel in a minute
            JLabel label = new JLabel();
            label.setFont(Styles.FONT_14PT_BOLD);
            label.setBorder(Styles.UNDERLINE_BLACK);

            gbc.gridwidth = gbc.REMAINDER;
            gbc.anchor = gbc.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = gbc.HORIZONTAL;
            gbc.insets = new Insets(10, 10, 0, 10);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(label, gbc);

            {

                StringBuffer buffer = new StringBuffer();
                XARProtocol protocol = xar.getNextProtocol();
                int count = 0;
                while (protocol != null) {
                    buffer.append(protocol.toString() + Text.getNewLine() + Text.getNewLine());
                    protocol = xar.getNextProtocol();
                    count++;
                }

                // Relabel
                if (count == 1) {
                    label.setText("1 Protocol");
                } else {
                    label.setText(count + " Protocols");
                }
                JTextArea descBox = new JTextArea(buffer.toString());
                descBox.setFont(Styles.FONT_12PT);
                descBox.setEditable(false);
                descBox.setOpaque(false);
                descBox.setBorder(Styles.BORDER_NONE);
                descBox.setWrapStyleWord(true);
                descBox.setLineWrap(true);

                gbc.gridwidth = gbc.REMAINDER;
                gbc.anchor = gbc.PAGE_START;
                gbc.weightx = 1;
                gbc.weighty = 1;
                gbc.fill = gbc.BOTH;
                gbc.insets = new Insets(10, 10, 15, 10);
                gbc.gridx = 0; // <-- Cell
                gbc.gridy = row++; // <-- Row

                this.add(new JScrollPane(descBox), gbc);
            }
        }


    }
}
