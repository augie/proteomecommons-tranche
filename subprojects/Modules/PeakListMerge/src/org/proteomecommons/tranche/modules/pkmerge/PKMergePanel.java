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
package org.proteomecommons.tranche.modules.pkmerge;

import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import org.tranche.gui.ErrorFrame;
import org.tranche.gui.GUIUtil;
import org.tranche.hash.BigHash;
import org.tranche.tasks.DownloadsTask;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;

/**
 * <p>Used by PopupTaskFrame, created by factory method.</p>
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
public class PKMergePanel extends AbstractPeakListPanel {

    public PKMergePanel(String description, Map<BigHash, String> files) {
        super(OPTION_NONE, description, files);

        // Overwrite tailored components
        this.description = description;
        this.filesToDownload = files;
        this.PREFERRED_DIMENSION = new Dimension(425, 310);
    }

    public void execute() throws Exception {

        // Used to simply hold a collection of files for merge task
        final List<File> inputFiles = new ArrayList();

        // Used to arbitrarily associate a temp file for each hash for download task
        final Map<BigHash, File> toDownload = new HashMap();

        // Prepare collections for two tasks
        File tmp;
        String extension;
        for (BigHash hash : this.filesToDownload.keySet()) {

            // Change file extension so IO framework can read
            extension = this.filesToDownload.get(hash);
            extension = extension.substring(extension.lastIndexOf('.'));

            tmp = TempFileUtil.createTemporaryFile(extension);

            // Create temporary files for downloads
            inputFiles.add(tmp);

            // Associate a temp file with each hash is a map for download task
            toDownload.put(hash, tmp);
        }

        // First, download the files
        final DownloadsTask dtask = new DownloadsTask(toDownload);

        Thread dthread = new Thread("Download thread") {

            public void run() {
                progressBar.updateStatus(0, "Downloading files...");
                try {
                    dtask.execute();

                } catch (Exception ex) {
                    ErrorFrame er = new ErrorFrame();
                    er.show(ex, frame);
                    redrawForStop();
                    // Won't be needed temp files
                    File toDelete = null;
                    for (File file : inputFiles) {
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

        dthread.start();

        // Give the download thread a chance to start
        Thread.yield();

        // Second, convert the files
        final PKMergeTask mtask = new PKMergeTask(inputFiles, super.outputFile);

        final Thread mthread = new Thread("Merge conversion thread") {

            public void run() {

                try {
                    progressBar.updateStatus(50, "Merging files.");
                    mtask.execute();

                    JOptionPane.showMessageDialog(
                            frame,
                            "Success, the file is located at " + outputFile.getAbsolutePath(),
                            "Peak lists merged.",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    ErrorFrame er = new ErrorFrame();
                    er.show(e, frame);
                } finally {
                    redrawForStop();
                    File toDelete = null;
                    for (File file : inputFiles) {
                        toDelete = GUIUtil.getTopmostSubdirectoryContaining(new File(TempFileUtil.getTemporaryDirectory()), file);
                        if (toDelete != null) {
                            break;
                        }
                    }
                    IOUtil.recursiveDelete(toDelete);
                }
            }
        };

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

                // Now that it's done, start the conversion thread
                mthread.start();
            }
        };

        progressThread.start();
    }
}
