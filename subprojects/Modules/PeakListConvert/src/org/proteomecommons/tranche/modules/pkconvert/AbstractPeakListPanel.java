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
package org.proteomecommons.tranche.modules.pkconvert;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import org.tranche.gui.ErrorFrame;
import org.tranche.gui.Styles;
import org.tranche.hash.BigHash;
import org.tranche.tasks.TaskUtil;

/**
 * <p>An abstract panel for certain tast panels.</p>
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
public abstract class AbstractPeakListPanel extends PopupTaskPanel implements MouseListener {

    /**
     * <p>If don't want any options.</p>
     */
    public final static int OPTION_NONE = 0;
    /**
     * <p>If a particular panel doesn't need this component, disable.</p>
     */
    public final static int OPTION_DISABLE_OUTPUT_FORMAT_DROPDOWN = 1 << 1;
    private int options = 0;
    /**
     * <p>Overwritten by subclass.</p>
     */
    public String description = "Task Title";
    public Map<BigHash, String> filesToDownload;
    public static final int MARGIN_LEFT = 10,  MARGIN_RIGHT = 10;
    public static final int MARGIN_TOP_BIG = 22,  MARGIN_TOP_MEDIUM = 14,  MARGIN_TOP_SMALL = 5;
    // Components
    public PKFormatDropDown formatDropDown = PKFormatDropDown.getInstance();
    public JButton outputButton,  startButton;
    private JTextArea inputBox = new JTextArea();
    public TaskProgressBar progressBar = new TaskProgressBar();
    public File outputFile = null;
    // Used for toggling names
    protected File baseFile = null;

    /**
     * <p>Uses options from this class to enable/disable certain option.</p>
     */
    public AbstractPeakListPanel(int options, String description, Map<BigHash, String> filesToDownload) {

        this.description = description;
        this.filesToDownload = filesToDownload;
        this.options = options;

        // Layout and constraints
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        this.setBackground(Styles.COLOR_BACKGROUND);

        // Want to auto-increment row
        int row = 0;

        // Set the description
        {

            JLabel descLabel = new JLabel("Description:");
            descLabel.setFont(Styles.FONT_14PT_BOLD);
            descLabel.setBorder(Styles.UNDERLINE_BLACK);

            gbc.gridwidth = gbc.REMAINDER;
            gbc.anchor = gbc.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = gbc.HORIZONTAL;
            gbc.insets = new Insets(MARGIN_TOP_MEDIUM, MARGIN_LEFT, 0, MARGIN_RIGHT);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(descLabel, gbc);

            JTextArea descBox = new JTextArea(this.description);
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
            gbc.fill = gbc.HORIZONTAL;
            gbc.insets = new Insets(MARGIN_TOP_SMALL, MARGIN_LEFT, 0, MARGIN_RIGHT);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(descBox, gbc);
        }

        // Set the inputs (read-only). Show filename if one file, else show file count.
        {
            JLabel inputLabel = new JLabel("Input:");
            inputLabel.setFont(Styles.FONT_12PT_BOLD);

            gbc.gridwidth = 1;
            gbc.anchor = gbc.PAGE_START;
            gbc.weightx = .5;
            gbc.weighty = 0;
            gbc.fill = gbc.HORIZONTAL;
            gbc.insets = new Insets(MARGIN_TOP_BIG, MARGIN_LEFT, 0, 0);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row; // <-- Row

            this.add(inputLabel, gbc);

            String input = null;
            if (this.filesToDownload.size() == 1) {
                input = this.filesToDownload.values().toArray(new String[0])[0];
            } else {
                input = this.filesToDownload.size() + " files.";
            }

            inputBox.setText(input);
            inputBox.setFont(Styles.FONT_12PT);
            inputBox.setEditable(false);
            inputBox.setOpaque(false);
            inputBox.setBorder(Styles.BORDER_NONE);
            inputBox.setWrapStyleWord(true);
            inputBox.setLineWrap(true);

            gbc.gridwidth = gbc.REMAINDER;
            gbc.anchor = gbc.PAGE_START;
            gbc.weightx = .5;
            gbc.weighty = 0;
            gbc.fill = gbc.HORIZONTAL;
            gbc.insets = new Insets(MARGIN_TOP_BIG, MARGIN_LEFT, 0, MARGIN_RIGHT);
            gbc.gridx = 1; // <-- Cell
            gbc.gridy = row; // <-- Row

            this.add(inputBox, gbc);

            // All on same row, now increment
            row++;
        }

        // Show dropdown (if not disabled)
        boolean disabled = (options & OPTION_DISABLE_OUTPUT_FORMAT_DROPDOWN) == OPTION_DISABLE_OUTPUT_FORMAT_DROPDOWN;
        if (!disabled) {

            JLabel formatLabel = new JLabel("Output format:");
            formatLabel.setFont(Styles.FONT_12PT_BOLD);

            gbc.gridwidth = 1;
            gbc.anchor = gbc.PAGE_START;
            gbc.weightx = .5;
            gbc.weighty = 0;
            gbc.fill = gbc.HORIZONTAL;
            gbc.insets = new Insets(MARGIN_TOP_MEDIUM, MARGIN_LEFT, 0, 0);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row; // <-- Row

            this.add(formatLabel, gbc);

            this.formatDropDown.setFont(Styles.FONT_12PT);

            gbc.gridwidth = gbc.REMAINDER;
            gbc.anchor = gbc.PAGE_START;
            gbc.weightx = .5;
            gbc.weighty = 0;
            gbc.fill = gbc.HORIZONTAL;
            gbc.insets = new Insets(MARGIN_TOP_MEDIUM - 8, MARGIN_LEFT, 0, MARGIN_RIGHT);
            gbc.gridx = 1; // <-- Cell
            gbc.gridy = row; // <-- Row

            this.add(this.formatDropDown, gbc);

            // All on same row, now increment
            row++;
        }

        // Save file button
        {
            JLabel locationLabel = new JLabel("Output location:");
            locationLabel.setFont(Styles.FONT_12PT_BOLD);

            gbc.gridwidth = 1;
            gbc.anchor = gbc.PAGE_START;
            gbc.weightx = .5;
            gbc.weighty = 0;
            gbc.fill = gbc.HORIZONTAL;
            gbc.insets = new Insets(MARGIN_TOP_MEDIUM, MARGIN_LEFT, 0, 0);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row; // <-- Row

            this.add(locationLabel, gbc);

            this.outputButton = new JButton("Select save file...");
            this.outputButton.setFont(Styles.FONT_11PT_BOLD);

            gbc.gridwidth = gbc.REMAINDER;
            gbc.anchor = gbc.PAGE_START;
            gbc.weightx = .5;
            gbc.weighty = 0;
            gbc.fill = gbc.HORIZONTAL;
            gbc.insets = new Insets(MARGIN_TOP_MEDIUM - 10, MARGIN_LEFT, 0, MARGIN_RIGHT);
            gbc.gridx = 1; // <-- Cell
            gbc.gridy = row; // <-- Row

            this.add(this.outputButton, gbc);

            // All on same row, now increment
            row++;
        }

        // "Start" button
        {
            this.startButton = new JButton("Start");
            this.startButton.setFont(Styles.FONT_12PT_BOLD);

            gbc.gridwidth = gbc.REMAINDER;
            gbc.anchor = gbc.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = gbc.HORIZONTAL;
            gbc.insets = new Insets((int) (MARGIN_TOP_BIG * 1.5), MARGIN_LEFT, 0, MARGIN_RIGHT);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(this.startButton, gbc);
        }

        // Progress bar. Invisible by default.
        {
            this.progressBar.setVisible(false);

            gbc.gridwidth = gbc.REMAINDER;
            gbc.anchor = gbc.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = gbc.HORIZONTAL;
            gbc.insets = new Insets(MARGIN_TOP_MEDIUM, MARGIN_LEFT, 0, MARGIN_RIGHT);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(this.progressBar, gbc);
        }

        // Listeners
        this.outputButton.addMouseListener(this);
        this.startButton.addMouseListener(this);
    }

    public void addFileToDownload(BigHash hash, String name) {
        filesToDownload.put(hash, name);
        inputBox.setText(filesToDownload.size() + " files.");
        validate();
        repaint();
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseClicked(MouseEvent e) {

        // Choose output file
        if (e.getSource().equals(this.outputButton)) {

            this.outputFile = TaskUtil.getFile("Select output file.", this.frame);

            if (this.outputFile != null) {
                this.outputButton.setText(this.outputFile.getAbsolutePath());
                this.baseFile = this.outputFile;
            }

            return;

        } // Choose output file

        // Choose start
        if (e.getSource().equals(this.startButton)) {

            if (this.outputFile == null) {
                JOptionPane.showMessageDialog(this.frame,
                        "You must select an output file before running the tool.",
                        "Please select output file",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // If not ending with selected file extension, change file output.
            // However, only if output format wasn't disabled.
            boolean disabled = (options & OPTION_DISABLE_OUTPUT_FORMAT_DROPDOWN) == OPTION_DISABLE_OUTPUT_FORMAT_DROPDOWN;

            // If output file is not the same as the file user chose, revert. This way, multiple conversions using this instance won't chain file extensions
            this.outputFile = this.baseFile;

            if (!disabled && !this.outputFile.getAbsolutePath().endsWith(this.formatDropDown.getSelectedFileExtension())) {
                this.outputFile = new File(this.outputFile.getAbsolutePath() + this.formatDropDown.getSelectedFileExtension());
            }

            try {
                redrawForStart();

                // Perform task
                this.execute();
            } catch (Exception ex) {
                if (this.frame != null) {
                    ErrorFrame er = new ErrorFrame();
                    er.show(ex, this.frame);
                } else {
                    ex.printStackTrace();
                }
            }

        } // Choose start

    } // mouseClicked

    /**
     * <p>Changes components so user experiences the tool running.</p>
     */
    public void redrawForStart() {
        progressBar.setVisible(true);
        startButton.setText("Running...");
        startButton.setEnabled(false);
    }

    /**
     * <p>Changes components so user experiences the tool stopping.</p>
     */
    public void redrawForStop() {
        progressBar.setVisible(false);
        startButton.setText("Start");
        startButton.setEnabled(true);
    }
}
