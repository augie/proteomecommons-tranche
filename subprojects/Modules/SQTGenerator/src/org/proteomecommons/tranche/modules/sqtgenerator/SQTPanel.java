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
package org.proteomecommons.tranche.modules.sqtgenerator;

import java.awt.Dimension;
import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.JOptionPane;
import org.tranche.gui.ErrorFrame;
import org.tranche.gui.GUIUtil;
import org.tranche.hash.BigHash;
import org.tranche.tasks.DownloadAndExplodeTask;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;

/**
 * <p>Used by PopupTaskFrame, created by factory method.</p>
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
public class SQTPanel extends AbstractPeakListPanel {

    public SQTPanel(String description, Map<BigHash, String> files) {
        super(OPTION_DISABLE_OUTPUT_FORMAT_DROPDOWN, description, files);

        // Overwrite tailored components
        this.description = description;
        this.filesToDownload = files;
        this.PREFERRED_DIMENSION = new Dimension(425, 280);
    }

    public void execute() throws Exception {

        // Used to simply hold a collection of files for SQT task. Not built until download and explode is complete.
        final Set<File> inputFiles = new HashSet();

        // First, download the files
        final File tempDir = new File(TempFileUtil.getTemporaryDirectory() + File.separator + "sqt-temp");
        tempDir.mkdirs();

        final DownloadAndExplodeTask dtask = new DownloadAndExplodeTask(filesToDownload, tempDir);

        // Keep reference to all files that need deleted.
        final Set<File> temporaryFiles = new HashSet();

        // Converter thread. Ran last, but need reference.
        final Thread sthread = new Thread("SQT conversion thread") {

            public void run() {

                try {

                    SQTTask stask = new SQTTask(inputFiles, outputFile);

                    progressBar.updateStatus(50, "Converting out files.");

                    stask.execute();

                    JOptionPane.showMessageDialog(
                            frame,
                            "Success, the file is located at " + outputFile.getAbsolutePath(),
                            "SQT file created.",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    ErrorFrame er = new ErrorFrame();
                    er.show(e, frame);
                } finally {
                    redrawForStop();
                    File toDelete = null;
                    for (File file : temporaryFiles) {
                        toDelete = GUIUtil.getTopmostSubdirectoryContaining(new File(TempFileUtil.getTemporaryDirectory()), file);
                        if (toDelete != null) {
                            break;
                        }
                    }
                    if (toDelete != null) {
                        IOUtil.recursiveDelete(toDelete);
                    }
                }
            }
        };

        // Download thread. Ran first.
        Thread dthread = new Thread("Download thread") {

            public void run() {
                progressBar.updateStatus(0, "Downloading files...");

                try {
                    dtask.execute();

                    // Build list of files.
                    for (File file : dtask.getAllFiles()) {

                        temporaryFiles.add(file);

                        if (file.getName().endsWith(".out")) {
                            inputFiles.add(file);
                        }
                    }
                } catch (Exception ex) {
                    ErrorFrame er = new ErrorFrame();
                    er.show(ex, frame);

                } finally {

                    // Run the conversion thread.
                    sthread.start();
                }
            }
        };

        dthread.start();

        // Give the download thread a chance to start
        Thread.yield();

        Thread progressThread = new Thread("Progress thread") {

            public void run() {

                // Make sure download therad got a chance to run. This isn't the more important thread.
                Thread.yield();

                while (dtask.isRunning()) {
                    // Arbitrarily choose that download task is half the job, adjust percentage accordingly
                    progressBar.updateStatus(dtask.getPercentComplete() / 2, dtask.getCurrentTaskDescription());
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) { /* nope */ }
                }
            }
        };

        progressThread.start();
    }
}
