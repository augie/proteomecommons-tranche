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
package org.proteomecommons.tranche.modules.fasta;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.proteomecommons.io.fasta.FASTAReaderFactory;
import org.tranche.gui.ErrorFrame;
import org.tranche.gui.GUIUtil;
import org.tranche.gui.GenericButton;
import org.tranche.gui.PreferencesUtil;
import org.tranche.gui.Styles;
import org.tranche.hash.BigHash;
import org.tranche.tasks.DownloadsTask;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;

/**
 * <p>Used by PopupTaskFrame, created by factory method.</p>
 * @author Bryan Smith <bryanesmith at gmail.com>
 * @author James "Augie" Hill <augman85@gmail.com>
 */
public class FASTAPanel extends PopupTaskPanel {

    private String LOCATION_OUTPUT_PREFERENCE = "FASTA MODULE OUTPUT LOCATION";
    private File outputFile,  baseFile;
    private FASTAInputFilePanel inputPanel = new FASTAInputFilePanel();
    private JCheckBox reverse = new JCheckBox("Reverse protein sequences");
    private FASTAWriterFormatDropDown outputFormat = FASTAWriterFormatDropDown.getInstance();
    private JTextField signifyReverseTextField = new JTextField();
    private TaskProgressBar progressBar = new TaskProgressBar();

    public FASTAPanel(Map<BigHash, String> filesToDownload) {

        // layout and constraints
        this.PREFERRED_DIMENSION = new Dimension(550, 430);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        setBackground(Styles.COLOR_BACKGROUND);

        // Set the description
        {
            JLabel descLabel = new JLabel("Description:");
            descLabel.setFont(Styles.FONT_14PT_BOLD);
            descLabel.setBorder(Styles.UNDERLINE_BLACK);

            gbc.anchor = gbc.NORTHWEST;
            gbc.fill = gbc.HORIZONTAL;
            gbc.gridwidth = gbc.REMAINDER;
            gbc.insets = new Insets(5, 5, 0, 5);
            gbc.weightx = 1;
            add(descLabel, gbc);

            JTextArea descBox = new JTextArea("Concatenate a set of FASTA files. Optionally reverse the protein sequences and convert the FASTA format.");
            descBox.setFont(Styles.FONT_12PT);
            descBox.setEditable(false);
            descBox.setOpaque(false);
            descBox.setBorder(Styles.BORDER_NONE);
            descBox.setWrapStyleWord(true);
            descBox.setLineWrap(true);

            gbc.insets = new Insets(5, 10, 0, 0);
            add(descBox, gbc);
        }

        // inputs
        {
            JLabel inputLabel = new JLabel("Input Files:");
            inputLabel.setFont(Styles.FONT_12PT_BOLD);

            gbc.gridwidth = gbc.RELATIVE;
            gbc.insets = new Insets(10, 15, 0, 0);
            gbc.weightx = 0;
            add(inputLabel, gbc);

            // add the files to download to the input panel
            for (BigHash hash : filesToDownload.keySet()) {
                inputPanel.addFileToDownload(hash, filesToDownload.get(hash));
            }

            gbc.fill = gbc.BOTH;
            gbc.gridwidth = gbc.REMAINDER;
            gbc.insets = new Insets(10, 10, 0, 10);
            gbc.weightx = 2;
            gbc.weighty = 1;
            add(inputPanel, gbc);
        }

        // Save file button
        {
            JLabel locationLabel = new JLabel("Output Location:");
            locationLabel.setFont(Styles.FONT_12PT_BOLD);

            gbc.anchor = gbc.CENTER;
            gbc.fill = gbc.HORIZONTAL;
            gbc.gridwidth = gbc.RELATIVE;
            gbc.insets = new Insets(5, 15, 0, 0);
            gbc.weightx = 0;
            gbc.weighty = 0;
            add(locationLabel, gbc);

            final JButton outputButton = new GenericButton("Select save file...");
            outputButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    final JFileChooser jfc = GUIUtil.makeNewFileChooser();
                    
                    Thread t = new Thread() {

                        public void run() {
                            // get the users last known input file
                            String lastLocation = PreferencesUtil.getPreference(LOCATION_OUTPUT_PREFERENCE);
                            if (lastLocation != null) {
                                File file = new File(lastLocation);
                                jfc.setSelectedFile(file);
                            }

                            jfc.setFileSelectionMode(jfc.FILES_ONLY);
                            int returnButton = jfc.showDialog(FASTAPanel.this, "Set Output Location");

                            // check for a submitted file
                            outputFile = jfc.getSelectedFile();

                            // the user cancelled
                            if (outputFile == null || returnButton == jfc.CANCEL_OPTION) {
                                return;
                            }

                            baseFile = outputFile;

                            // save the location
                            PreferencesUtil.setPreference(LOCATION_OUTPUT_PREFERENCE, outputFile.getAbsolutePath());
                            try {
                                PreferencesUtil.save();
                            } catch (Exception e) {
                            }

                            // set the appropriate string
                            outputButton.setText(outputFile.toString());
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            outputButton.setFont(Styles.FONT_11PT_BOLD);

            gbc.gridwidth = gbc.REMAINDER;
            gbc.insets = new Insets(5, 10, 0, 10);
            gbc.weightx = 2;
            add(outputButton, gbc);
        }

        // convert to which format?
        {
            gbc.anchor = gbc.WEST;
            gbc.gridwidth = gbc.RELATIVE;
            gbc.insets = new Insets(5, 15, 0, 0);
            gbc.weightx = 0;
            add(new JLabel("Output Format:"), gbc);

            gbc.anchor = gbc.EAST;
            gbc.gridwidth = gbc.REMAINDER;
            gbc.insets = new Insets(5, 10, 0, 10);
            gbc.weightx = 1;
            add(outputFormat, gbc);
        }

        // reverse?
        {
            reverse.setFocusable(false);
            reverse.setBackground(getBackground());
            reverse.setSelected(true);
            reverse.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        public void run() {
                            signifyReverseTextField.setEnabled(reverse.isSelected());
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            gbc.anchor = gbc.NORTH;
            gbc.insets = new Insets(5, 10, 0, 10);
            add(reverse, gbc);
        }

        // how to signify reversed
        {
            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());
            panel.add(new JLabel("    Signify reverse by adding to accession:  "), BorderLayout.WEST);
            signifyReverseTextField.setText("_REVERSED");
            panel.add(signifyReverseTextField, BorderLayout.CENTER);

            gbc.gridwidth = gbc.REMAINDER;
            gbc.insets = new Insets(0, 10, 3, 10);
            gbc.weightx = 1;
            add(panel, gbc);
        }

        // start button
        {
            JButton startButton = new GenericButton("Concatenate FASTA Files");
            startButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        public void run() {
                            if (outputFile == null) {
                                JOptionPane.showMessageDialog(frame, "You must select an output file before running the tool.", "Please select output file", JOptionPane.INFORMATION_MESSAGE);
                                return;
                            }

                            progressBar.setVisible(true);

                            // If output file is not the same as the file user chose, revert. This way, multiple conversions using this instance won't chain file extensions
                            outputFile = baseFile;

                            try {
                                execute();
                            } catch (Exception ex) {
                                ErrorFrame er = new ErrorFrame();
                                er.show(ex, frame);
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            startButton.setFont(Styles.FONT_TITLE);
            startButton.setBorder(Styles.BORDER_BUTTON_TOP);

            gbc.anchor = gbc.NORTH;
            gbc.insets = new Insets(15, 0, 0, 0);
            gbc.weightx = 1;
            add(startButton, gbc);
        }

        // Progress bar. Invisible by default.
        {
            progressBar.setVisible(false);
            progressBar.setStringPainted(true);
            progressBar.setFont(Styles.FONT_10PT_BOLD);
            progressBar.setBorder(Styles.BORDER_GRAY_1);

            gbc.fill = gbc.HORIZONTAL;
            gbc.insets = new Insets(0, 0, 0, 0);
            gbc.weighty = 0;
            add(progressBar, gbc);
        }
    }

    public void execute() throws Exception {

        // Used to simply hold a collection of files for merge task
        final Map<File, FASTAReaderFactory> inputFiles = new HashMap<File, FASTAReaderFactory>();

        // Used to arbitrarily associate a temp file for each hash for download task
        final Map<BigHash, File> toDownload = new HashMap();

        // Prepare collections for two tasks
        for (FASTAInputFile file : inputPanel.getFilesToDownload()) {
            // set up the temporary file with a correct extension
            File tmp = TempFileUtil.createTemporaryFile(file.getFormatMenu().getSelectedFileExtension());

            // Create temporary files for downloads
            inputFiles.put(tmp, file.getFormatMenu().getSelectedReaderFactory());

            // Associate a temp file with each hash is a map for download task
            toDownload.put(BigHash.createHashFromString(file.getLocation()), tmp);
        }
        for (FASTAInputFile file : inputPanel.getLocalFiles()) {
            inputFiles.put(new File(file.getLocation()), file.getFormatMenu().getSelectedReaderFactory());
        }

        // First, download the files
        final DownloadsTask dtask = new DownloadsTask(toDownload);

        final Thread dthread = new Thread("Download thread") {

            public void run() {
                progressBar.updateStatus(0, "Downloading files...");
                try {
                    dtask.execute();
                } catch (Exception ex) {
                    ErrorFrame er = new ErrorFrame();
                    er.show(ex, frame);

                    // Won't be needed temp files
                    File toDelete = null;
                    for (File file : inputFiles.keySet()) {
                        toDelete = GUIUtil.getTopmostSubdirectoryContaining(new File(TempFileUtil.getTemporaryDirectory()), file);
                        if (toDelete != null) {
                            break;
                        }
                    }
                    IOUtil.recursiveDelete(toDelete);
                    return;
                }
            }
        };
        dthread.setDaemon(true);
        dthread.start();

        // Second, convert the files
        final FASTATask task = new FASTATask(inputFiles, outputFile, reverse.isSelected(), signifyReverseTextField.getText(), outputFormat.getSelectedWriterFactory());

        final Thread cthread = new Thread("Execution Thread") {

            public void run() {
                try {
                    progressBar.updateStatus(50, "Performing tasks.");
                    task.execute();
                    JOptionPane.showMessageDialog(frame, "Success, the file is located at " + outputFile.getAbsolutePath(), "FASTA file generated.", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    ErrorFrame er = new ErrorFrame();
                    er.show(e, frame);
                } finally {
                    File toDelete = null;
                    for (File file : inputFiles.keySet()) {
                        toDelete = GUIUtil.getTopmostSubdirectoryContaining(new File(TempFileUtil.getTemporaryDirectory()), file);
                        if (toDelete != null) {
                            break;
                        }
                    }
                    IOUtil.recursiveDelete(toDelete);
                }
            }
        };
        cthread.setDaemon(true);

        Thread progressThread = new Thread("Progress thread") {

            public void run() {
                while (dtask.isRunning()) {
                    // Arbitrarily choose that download task is half the job, adjust percentage accordingly
                    progressBar.updateStatus(dtask.getPercentComplete() / 2, "Downloading file #" + (dtask.getDownloadCount() + 1));
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                        // No thanks
                    }
                }

                // wait for the dtask thread to stop executing
                try {
                    dthread.join();
                } catch (Exception e) {
                    // do nothing
                }

                if (!dtask.isComplete()) {
                    JOptionPane.showMessageDialog(FASTAPanel.this, "One or more of the files could not be downloaded.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Now that it's done, start the conversion thread
                cthread.start();
                while (cthread.isAlive()) {
                    progressBar.updateStatus(0.5 + task.getPercentComplete() / 2.0, "Performing tasks.");
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException ex) {
                        // No thanks
                    }
                }
                progressBar.setVisible(false);
            }
        };
        progressThread.setDaemon(true);
        progressThread.start();
    }
}
