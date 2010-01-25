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
package org.proteomecommons.tranche.modules.advancedsearch;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.tranche.gui.AdvancedGUI;
import org.tranche.gui.DownloadSummary;
import org.tranche.gui.ErrorFrame;
import org.tranche.gui.GUIUtil;
import org.tranche.gui.IndeterminateProgressBar;
import org.tranche.gui.MainPanel;
import org.tranche.gui.Styles;
import org.tranche.gui.pools.DownloadPool;
import org.tranche.hash.BigHash;
import org.tranche.util.IOUtil;

/**
 * Advanced search for projects.
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
public class AdvancedSearchPanel extends JPanel implements ActionListener {

    private static final boolean isDebug = false;
    private AdvancedGUI main = null;
    public static final Dimension RECOMMENDED_DIMENSION = new Dimension(600, 350);
    private int MARGIN = 10;    // When adding rows, need a ptr for gbc
    private int rowPtr = 0;    // On subsequent searchs, need to reset the row pointer to add new components
    // to the layout. This will point to the row after the search button.
    private final int RESET_ROW_PTR;    // These are the components resulting from a search, which are cleared on
    // subsequent searches
    private Set<Component> ephemeralComponents = new HashSet();
    private JTextField keywordText,  researcherText,  organismText;
    private JComboBox journalDropDown,  ionSourceDropDown,  instrumentDropDown;
    private JCheckBox onlineResultsOnlyBox;
    private JButton searchButton;
    private String[] journalOptions = {
        "Any",
        "Molecular and Cellular Proteomics",
        "Journal of Proteome Research",
        "Bioinformatics",
        "Nature Biotechnology"
    };
    private String[] ionSourceOptions = {
        "Any",
        "ESI",
        "MALDI"
    };
    private String[] instrumentOptions = {
        "Any",
        "Voyager-DETM STR",
        "1100 series LC/MSD Trap XCT Ultra",
        "4000 Q TRAP",
        "4700 Proteomics Analyzer",
        "Autoflex III",
        "Axima CFR ",
        "DE-STR MALDI TOF",
        "FT-ICR",
        "LCQ Classic",
        "LCQ DECA XP",
        "LCQ DECA XP PLUS",
        "LCQ Deca XP MAX",
        "LTQ",
        "LTQ FT Ultra hybrid",
        "LTQ Orbitrap Hybrid",
        "MALDI micro MX",
        "MALDI-TOF/TOF Tandem",
        "NanoMate 100",
        "Q-TOF Ultima",
        "Q-TOF Ultima API",
        "Q-TOF micro",
        "QSTAR Elite Hybrid",
        "QSTAR Pulsar",
        "QSTAR Pulsar i",
        "QStar Pulsar",
        "Qstar Q-TOF",
        "Qstar XL Hybrid",
        "Reflex IV",
        "TSQ Quantun Discovery MAX",
        "TofSpec 2E",
        "Ultraflex III TOF/TOF",
        "Voyager 4700",
        "Voyager Elite XL",
        "Voyager-DE PRO",
        "Voyager-DE STR",
        "esquire HCT"
    };
    private final JPanel scrollablePanel = new JPanel();

    public AdvancedSearchPanel(AdvancedGUI main) {

        this.main = main;
        this.setBackground(Color.WHITE);

        this.setBorder(Styles.BORDER_NONE);

        // Set the layout for the outer container
        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Set the layout for the inner container
        scrollablePanel.setLayout(new GridBagLayout());
        scrollablePanel.setBackground(Color.WHITE);

        // Aids correct word wrap
        this.setSize(RECOMMENDED_DIMENSION);

        // Key words text
        {
            JLabel l = new JLabel("Keywords:");
            l.setFont(Styles.FONT_11PT_BOLD);

            gbc.gridwidth = 1;
            gbc.anchor = gbc.FIRST_LINE_START;
            gbc.weightx = 0;
            gbc.weighty = 0;
            gbc.fill = gbc.NONE;
            gbc.insets = new Insets(MARGIN * 2, MARGIN, 0, 0);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = rowPtr; // <-- Row

            scrollablePanel.add(l, gbc);

            keywordText = new JTextField();

            gbc.gridwidth = gbc.REMAINDER;
            gbc.anchor = gbc.FIRST_LINE_START;
            gbc.weightx = 1.0;
            gbc.weighty = 0;
            gbc.fill = gbc.HORIZONTAL;
            gbc.insets = new Insets(MARGIN * 2 - (5), MARGIN, 0, MARGIN);
            gbc.gridx = 1; // <-- Cell
            gbc.gridy = rowPtr; // <-- Row

            scrollablePanel.add(keywordText, gbc);

            rowPtr++;
        }

        // Journal drop down
        {
            JLabel l = new JLabel("Journal:");
            l.setFont(Styles.FONT_11PT);

            gbc.gridwidth = 1;
            gbc.anchor = gbc.FIRST_LINE_START;
            gbc.weightx = 0;
            gbc.weighty = 0;
            gbc.fill = gbc.NONE;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, 0);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = rowPtr; // <-- Row

            scrollablePanel.add(l, gbc);

            journalDropDown = new JComboBox(journalOptions);
            journalDropDown.setSelectedIndex(0);
            journalDropDown.setBackground(Color.WHITE);

            gbc.gridwidth = gbc.REMAINDER;
            gbc.anchor = gbc.FIRST_LINE_START;
            gbc.weightx = 1.0;
            gbc.weighty = 0;
            gbc.fill = gbc.HORIZONTAL;
            gbc.insets = new Insets(MARGIN - (5), MARGIN, 0, MARGIN);
            gbc.gridx = 1; // <-- Cell
            gbc.gridy = rowPtr; // <-- Row

            scrollablePanel.add(journalDropDown, gbc);
            rowPtr++;
        }

        // Research/Institute text
        {
            JLabel l = new JLabel("Researcher/Institute:");
            l.setFont(Styles.FONT_11PT);

            gbc.gridwidth = 1;
            gbc.anchor = gbc.FIRST_LINE_START;
            gbc.weightx = 0;
            gbc.weighty = 0;
            gbc.fill = gbc.NONE;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, 0);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = rowPtr; // <-- Row

            scrollablePanel.add(l, gbc);

            researcherText = new JTextField();

            gbc.gridwidth = gbc.REMAINDER;
            gbc.anchor = gbc.FIRST_LINE_START;
            gbc.weightx = 1.0;
            gbc.weighty = 0;
            gbc.fill = gbc.HORIZONTAL;
            gbc.insets = new Insets(MARGIN - (5), MARGIN, 0, MARGIN);
            gbc.gridx = 1; // <-- Cell
            gbc.gridy = rowPtr; // <-- Row

            scrollablePanel.add(researcherText, gbc);

            rowPtr++;
        }

        // Organism text
        {
            JLabel l = new JLabel("Organism:");
            l.setFont(Styles.FONT_11PT);

            gbc.gridwidth = 1;
            gbc.anchor = gbc.FIRST_LINE_START;
            gbc.weightx = 0;
            gbc.weighty = 0;
            gbc.fill = gbc.NONE;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, 0);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = rowPtr; // <-- Row

            scrollablePanel.add(l, gbc);

            organismText = new JTextField();

            gbc.gridwidth = gbc.REMAINDER;
            gbc.anchor = gbc.FIRST_LINE_START;
            gbc.weightx = 1.0;
            gbc.weighty = 0;
            gbc.fill = gbc.HORIZONTAL;
            gbc.insets = new Insets(MARGIN - (5), MARGIN, 0, MARGIN);
            gbc.gridx = 1; // <-- Cell
            gbc.gridy = rowPtr; // <-- Row

            scrollablePanel.add(organismText, gbc);

            rowPtr++;
        }

        // Ion source drop down
        {
            JLabel l = new JLabel("Ion Source:");
            l.setFont(Styles.FONT_11PT);

            gbc.gridwidth = 1;
            gbc.anchor = gbc.FIRST_LINE_START;
            gbc.weightx = 0;
            gbc.weighty = 0;
            gbc.fill = gbc.NONE;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, 0);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = rowPtr; // <-- Row

            scrollablePanel.add(l, gbc);

            ionSourceDropDown = new JComboBox(ionSourceOptions);
            ionSourceDropDown.setSelectedIndex(0);
            ionSourceDropDown.setBackground(Color.WHITE);

            gbc.gridwidth = gbc.REMAINDER;
            gbc.anchor = gbc.FIRST_LINE_START;
            gbc.weightx = 1.0;
            gbc.weighty = 0;
            gbc.fill = gbc.HORIZONTAL;
            gbc.insets = new Insets(MARGIN - (5), MARGIN, 0, MARGIN);
            gbc.gridx = 1; // <-- Cell
            gbc.gridy = rowPtr; // <-- Row

            scrollablePanel.add(ionSourceDropDown, gbc);
            rowPtr++;
        }

        // Instrument drop down
        {
            JLabel l = new JLabel("Instrument:");
            l.setFont(Styles.FONT_11PT);

            gbc.gridwidth = 1;
            gbc.anchor = gbc.FIRST_LINE_START;
            gbc.weightx = 0;
            gbc.weighty = 0;
            gbc.fill = gbc.NONE;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, 0);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = rowPtr; // <-- Row

            scrollablePanel.add(l, gbc);

            instrumentDropDown = new JComboBox(instrumentOptions);
            instrumentDropDown.setSelectedIndex(0);
            instrumentDropDown.setBackground(Color.WHITE);

            gbc.gridwidth = gbc.REMAINDER;
            gbc.anchor = gbc.FIRST_LINE_START;
            gbc.weightx = 1.0;
            gbc.weighty = 0;
            gbc.fill = gbc.HORIZONTAL;
            gbc.insets = new Insets(MARGIN - (5), MARGIN, 0, MARGIN);
            gbc.gridx = 1; // <-- Cell
            gbc.gridy = rowPtr; // <-- Row

            scrollablePanel.add(instrumentDropDown, gbc);
            rowPtr++;
        }

        // "Only return results with online data" check box
        {
            onlineResultsOnlyBox = new JCheckBox("Only return results with online data");
            onlineResultsOnlyBox.setSelected(true);
            onlineResultsOnlyBox.setBackground(Color.WHITE);

            gbc.gridwidth = gbc.REMAINDER;
            gbc.anchor = gbc.FIRST_LINE_START;
            gbc.weightx = 1.0;
            gbc.weighty = 0;
            gbc.fill = gbc.HORIZONTAL;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = rowPtr; // <-- Row

            scrollablePanel.add(onlineResultsOnlyBox, gbc);

            rowPtr++;
        }

        // Search button
        {
            searchButton = new JButton("Search");
            searchButton.setFont(Styles.FONT_12PT_BOLD);

            gbc.gridwidth = gbc.REMAINDER;
            gbc.anchor = gbc.FIRST_LINE_START;
            gbc.weightx = 1.0;
            gbc.weighty = 0;
            gbc.fill = gbc.HORIZONTAL;
            gbc.insets = new Insets((MARGIN * 2), MARGIN, MARGIN, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = rowPtr; // <-- Row

            scrollablePanel.add(searchButton, gbc);

            rowPtr++;
            // Set the reset pointer so consequitive searches are layed out correctly.
            RESET_ROW_PTR = rowPtr;
        }

        // Create our first ephemeral component -- a vertical strut!
        {
            Component strut = Box.createVerticalStrut(1);

            gbc.gridwidth = gbc.REMAINDER;
            gbc.anchor = gbc.FIRST_LINE_START;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.fill = gbc.BOTH;
            gbc.insets = new Insets(0, 0, 0, 0);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = rowPtr; // <-- Row

            this.addEphemeralComponent(strut, gbc);
        }

        // Add scrollable panel to this
        GridBagConstraints scrollc = new GridBagConstraints();
        scrollc.gridwidth = gbc.REMAINDER;
        scrollc.anchor = gbc.FIRST_LINE_START;
        scrollc.weightx = 1.0;
        scrollc.weighty = 1.0;
        scrollc.fill = gbc.BOTH;
        scrollc.insets = new Insets(0, 0, 0, 0);
        scrollc.gridx = 0; // <-- Cell
        scrollc.gridy = 0; // <-- Row
        this.add(new JScrollPane(scrollablePanel), scrollc);

        // Listeners
        searchButton.addActionListener(this);
        keywordText.addActionListener(this);
        researcherText.addActionListener(this);
        organismText.addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        search();
    }
//    // KeyListener
//    public void keyTyped(KeyEvent e) {
//        // If pressed enter, perform search
//        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
//            search();
//        }
//    }
//    public void keyPressed(KeyEvent e) {}
//    public void keyReleased(KeyEvent e) {}
    /**
     * Performs the search, updates the GUI
     */
    public void search() {

        Thread t = new Thread() {

            public void run() {
                // For HTTP response
                BufferedReader reader = null;
                final IndeterminateProgressBar progress = new IndeterminateProgressBar("Connecting to database...");
                try {
                    // Clear out all ephemeral components
                    resetSearchView();

                    progress.setDisposeAllowable(true);
                    progress.setLocationRelativeTo(main);

                    Thread pthread = new Thread() {

                        public void run() {
                            progress.start();
                        }
                    };
                    SwingUtilities.invokeLater(pthread);

                    // Make the query string
                    StringBuffer query = new StringBuffer();

                    // "Any" refers to default query parameter
                    String ionSelection = ionSourceDropDown.getSelectedItem().toString();
                    if (ionSelection.equalsIgnoreCase("any")) {
                        ionSelection = "";
                    }
                    String journalSelection = journalDropDown.getSelectedItem().toString();
                    if (journalSelection.equalsIgnoreCase("any")) {
                        journalSelection = "";
                    }
                    String instrumentSelection = instrumentDropDown.getSelectedItem().toString();
                    if (instrumentSelection.equalsIgnoreCase("any")) {
                        instrumentSelection = "";
                    }
                    query.append("q=" + GUIUtil.createURLEncodedString(keywordText.getText()));
                    query.append("&i=" + GUIUtil.createURLEncodedString(ionSelection));
                    query.append("&j=" + GUIUtil.createURLEncodedString(journalSelection));
                    query.append("&n=" + GUIUtil.createURLEncodedString(researcherText.getText()));
                    query.append("&o=" + GUIUtil.createURLEncodedString(organismText.getText()));
                    query.append("&m=" + GUIUtil.createURLEncodedString(instrumentSelection));
                    if (onlineResultsOnlyBox.isSelected()) {
                        query.append("&t=on");                    // HTTP request for results
                    }
                    HttpClient c = new HttpClient();
                    GetMethod pm = new GetMethod("http://www.proteomecommons.org/data/tranche-advanced-search.jsp?" + query.toString());

                    // execute the method
                    if (c.executeMethod(pm) != 200) {
                        throw new Exception("Unsuccessfully registered hash.");
                    }

                    // Get response
                    reader = new BufferedReader(new InputStreamReader(pm.getResponseBodyAsStream()));

                    String line = null;
                    // Buffer each entry separately
                    StringBuffer buffer = new StringBuffer();

                    while ((line = reader.readLine()) != null) {

                        printTracer("Reading next response line: " + line);

                        // Trim the line (to be safe)
                        line = line.trim() + " ";

                        // Skip blank lines
                        if (line.trim().equals("")) {
                            continue;
                        }
                        // If new entry
                        if (line.startsWith("TITLE")) {
                            // Add previous buffer to entry if not empty
                            if (!buffer.toString().trim().equals("")) {
                                // Add component
                                addEphemeralComponent(new SearchResult(buffer));
                                buffer = new StringBuffer();
                            }

                            buffer.append(line);
                        } else {
                            // Building up subsequent lines
                            buffer.append(line);
                        }
                    }

                    progress.stop();
                    // If no results, notify user
                    if (ephemeralComponents.size() == 0) {
                        JOptionPane.showMessageDialog(
                                main,
                                "You query didn't yield any matches.",
                                "No results",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {

                        String title = "Search results";
                        String message = "A total of ";

                        if (ephemeralComponents.size() == 1) {
                            message += "1 result was ";
                        } else {
                            message += (ephemeralComponents.size() + " results were ");
                        }
                        message += "found.";

                        JOptionPane.showMessageDialog(
                                main,
                                message,
                                title,
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    progress.stop();
                    ErrorFrame ef = new ErrorFrame();
                    ef.show(ex, main);
                } finally {
                    IOUtil.safeClose(reader);

                    Thread updateThread = new Thread() {

                        public void run() {
                            updateUI();
                            // Request focus (scroll bar to top)
                            keywordText.select(0, 1);
                            keywordText.requestFocus();
                        }
                    };
                    SwingUtilities.invokeLater(updateThread);
                }

            } // run
        };
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    /**
     * Add a component that won't remain part of layout w/ subsequent searches.
     */
    private void addEphemeralComponent(Component c) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = gbc.REMAINDER;
        gbc.anchor = gbc.FIRST_LINE_START;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = gbc.BOTH;
        gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
        gbc.gridx = 0; // <-- Cell
        gbc.gridy = rowPtr; // <-- Row
        scrollablePanel.add(c, gbc);
        rowPtr++;
        ephemeralComponents.add(c);
    }

    /**
     * Add a component that won't remain part of layout w/ subsequent searches
     */
    private void addEphemeralComponent(Component c, GridBagConstraints gbc) {
        scrollablePanel.add(c, gbc);
        rowPtr++;
        ephemeralComponents.add(c);
    }

    /**
     * On subsequent searches, handles the task of clearing out old search results
     * and prepares for new ones.
     */
    private void resetSearchView() {
        for (Component c : ephemeralComponents) {
            scrollablePanel.remove(c);
        }
        ephemeralComponents = new HashSet();
        rowPtr = RESET_ROW_PTR;
    }

    /**
     * Represents a single search result.
     */
    private class SearchResult extends JPanel {

        final String title;
        final String description;
        final BigHash projectHash;
        JButton browseProjectButton, downloadProjectButton, viewOnlineButton;

        public SearchResult(StringBuffer buffer) {

            this.setBackground(Styles.COLOR_BACKGROUND_LIGHT);
            this.setBorder(Styles.BORDER_BLACK_1);

            // Set the layout
            this.setLayout(new GridBagLayout());
            GridBagConstraints innerGbc = new GridBagConstraints();

            // Keep local copy since obj not init
            final String title = extractTitle(buffer);
            final String description = extractDescription(buffer);
            final BigHash projectHash = extractBigHash(buffer);

            this.title = title;
            this.description = description;
            this.projectHash = projectHash;

            // Add the title
            {
                JTextArea a = new JTextArea(title);
                a.setWrapStyleWord(true);
                a.setLineWrap(true);
                a.setEditable(false);
                a.setFont(Styles.FONT_12PT_BOLD);
                a.setBackground(Styles.COLOR_BACKGROUND_LIGHT);

                innerGbc.gridwidth = innerGbc.REMAINDER;
                innerGbc.weightx = 1.0;
                innerGbc.weighty = 0;
                innerGbc.fill = innerGbc.HORIZONTAL;
                innerGbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
                innerGbc.gridx = 0; // <-- Cell
                innerGbc.gridy = 0; // <-- Row

                this.add(a, innerGbc);
            }

            // Add the description
            {
                printTracer("Adding description to GUI: " + description);

                JTextArea a = new JTextArea(description);
                a.setWrapStyleWord(true);
                a.setLineWrap(true);
                a.setEditable(false);
                a.setFont(Styles.FONT_11PT);
                a.setBackground(Styles.COLOR_BACKGROUND_LIGHT);

                innerGbc.gridwidth = innerGbc.REMAINDER;
                innerGbc.weightx = 1.0;
                innerGbc.weighty = 0;
                innerGbc.fill = innerGbc.HORIZONTAL;
                innerGbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
                innerGbc.gridx = 0; // <-- Cell
                innerGbc.gridy = 1; // <-- Row

                this.add(a, innerGbc);
            }

            // Add the buttons. If no hash, button to view page entire space
            {
                int colPtr = 0;
                if (this.projectHash != null) {
                    browseProjectButton = new JButton("Browse Project");
                    browseProjectButton.setBackground(Styles.COLOR_BACKGROUND);
                    browseProjectButton.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent e) {
                            GUIUtil.getAdvancedGUI().mainPanel.projectsPanel.openProject(projectHash);
                        }
                    });

                    innerGbc.gridwidth = 1;
                    innerGbc.weightx = 0;
                    innerGbc.weighty = 0;
                    innerGbc.fill = innerGbc.NONE;
                    innerGbc.insets = new Insets((int) (MARGIN * 1.5), MARGIN, MARGIN, 0);
                    innerGbc.gridx = colPtr++; // <-- Cell
                    innerGbc.gridy = 2; // <-- Row

                    this.add(browseProjectButton, innerGbc);

                    downloadProjectButton = new JButton("Download Project");
                    downloadProjectButton.setBackground(Styles.COLOR_BACKGROUND);
                    downloadProjectButton.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent e) {
                            MainPanel m = GUIUtil.getAdvancedGUI().mainPanel;
                            DownloadPool.setDownloadSummary(new DownloadSummary(projectHash));
                            m.setPanel(m.downloadsPanel);
                            m.setTab(m.downloadsTab);
                        }
                    });

                    innerGbc.gridwidth = 1;
                    innerGbc.weightx = 0;
                    innerGbc.weighty = 0;
                    innerGbc.fill = innerGbc.NONE;
                    innerGbc.insets = new Insets((int) (MARGIN * 1.5), MARGIN, MARGIN, 0);
                    innerGbc.gridx = colPtr++; // <-- Cell
                    innerGbc.gridy = 2; // <-- Row

                    this.add(downloadProjectButton, innerGbc);

                    viewOnlineButton = new JButton("View Online Description");
                    viewOnlineButton.setBackground(Styles.COLOR_BACKGROUND);
                    viewOnlineButton.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent e) {
                            String URL = "http://www.proteomecommons.org/data/show.jsp?id=";
                            GUIUtil.displayURL(URL + GUIUtil.createURLEncodeBase16Hash(projectHash));
                        }
                    });

                    innerGbc.gridwidth = innerGbc.REMAINDER;
                    innerGbc.weightx = 0;
                    innerGbc.weighty = 0;
                    innerGbc.fill = innerGbc.HORIZONTAL;
                    innerGbc.insets = new Insets((int) (MARGIN * 1.5), MARGIN, MARGIN, MARGIN);
                    innerGbc.gridx = colPtr++; // <-- Cell
                    innerGbc.gridy = 2; // <-- Row

                    this.add(viewOnlineButton, innerGbc);
                } else {
                    // Vertical strut
                    innerGbc.gridwidth = innerGbc.REMAINDER;
                    innerGbc.weightx = 1.0;
                    innerGbc.weighty = 0;
                    innerGbc.fill = innerGbc.HORIZONTAL;
                    innerGbc.insets = new Insets((int) (MARGIN * 1.5), MARGIN, MARGIN, MARGIN);
                    innerGbc.gridx = colPtr++; // <-- Cell
                    innerGbc.gridy = 2; // <-- Row
                    this.add(Box.createVerticalStrut(1), innerGbc);
                }
            }
        }

        private String extractTitle(StringBuffer buffer) {
            Pattern p = Pattern.compile(".*TITLE: (.*?)DESCRIPTION:.*");
            Matcher m = p.matcher(buffer.toString());
            if (m.matches()) {
                return m.group(1);
            }

            // Fail
            return null;
        }

        private String extractDescription(StringBuffer buffer) {

            printTracer("Looking for description in following buffer: " + buffer.toString());

            // See if downloadable first
            Pattern p = Pattern.compile(".*DESCRIPTION: (.*?)DOWNLOADABLE:.*");
            Matcher m = p.matcher(buffer.toString());
            if (m.matches()) {
                printTracer("Found description using first match: " + m.group(1));
                return m.group(1);
            }

            // Take the rest of the line
            p = Pattern.compile(".*DESCRIPTION: (.*?)");
            m = p.matcher(buffer.toString());
            if (m.matches()) {
                printTracer("Found description using second match: " + m.group(1));
                return m.group(1);
            }

            // Fail
            printTracer("Failed to find description match.");
            return null;
        }

        private BigHash extractBigHash(StringBuffer buffer) {
            Pattern p = Pattern.compile(".*DOWNLOADABLE: (.*)");
            Matcher m = p.matcher(buffer.toString());
            if (m.matches()) {
                try {
                    return GUIUtil.createBigHashFromURLEncodedString(m.group(1).trim());
                } catch (RuntimeException ex) { /* Continue */ }
            }

            return null;
        }
    }

    /**
     *
     */
    private static void printTracer(String msg) {
        if (isDebug) {
            System.out.println("ADVANCED_SEARCH_PANEL> " + msg);
        }
    }
}
