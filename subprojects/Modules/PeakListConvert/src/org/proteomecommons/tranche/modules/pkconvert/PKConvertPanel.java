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

import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.proteomecommons.fourg.annotations.CurrentPanel;
import org.proteomecommons.fourg.annotations.Dispose;
import org.proteomecommons.fourg.annotations.DropTargetListeners;
import org.proteomecommons.fourg.annotations.Init;
import org.proteomecommons.fourg.annotations.IsSatisfied;
import org.proteomecommons.fourg.annotations.Module;
import org.proteomecommons.fourg.annotations.Order;
import org.proteomecommons.fourg.annotations.Panel;
import org.proteomecommons.fourg.annotations.PrerequisiteCallbackMethod;
import org.proteomecommons.fourg.prerequisites.Prerequisite;
import org.tranche.add.AddFileTool;
import org.tranche.gui.ErrorFrame;
import org.tranche.gui.GUIUtil;
import org.tranche.hash.BigHash;
import org.tranche.servers.ServerUtil;
import org.tranche.tasks.DownloadsTask;
import org.tranche.users.UserZipFile;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;

/**
 * <p>Used by PopupTaskFrame, created by factory method.</p>
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
@Module(name = "Peak List Converter", description = "Convert a peak list from one format to another using the IO Framework.")
public class PKConvertPanel extends AbstractPeakListPanel implements DropTargetListener {

    // necessary for 4g drag and drop
    private List<DropTargetListener> dropTargetListeners = new ArrayList<DropTargetListener>();

    public PKConvertPanel() {
        this("Peak List Converter", new HashMap());
    }

    public PKConvertPanel(String description, Map<BigHash, String> files) {
        super(OPTION_NONE, description, files);

        // Overwrite tailored components
        this.description = description;
        this.filesToDownload = files;
        this.PREFERRED_DIMENSION = new Dimension(425, 310);

        // for 4g
        dropTargetListeners.add(this);
    }

    public void execute() throws Exception {

        // Used to arbitrarily associate a temp file for each hash for download task
        final Map<BigHash, File> toDownload = new HashMap();

        // Prepare collections for two tasks
        File tmp = null;
        String extension;

        // Should only be one hash
        for (BigHash hash : this.filesToDownload.keySet()) {

            // Change file extension so IO framework can read
            extension = this.filesToDownload.get(hash);
            extension = extension.substring(extension.lastIndexOf('.'));

            tmp = TempFileUtil.createTemporaryFile(extension);

            // Associate a temp file with each hash is a map for download task
            toDownload.put(hash, tmp);
        }

        // Used to simply hold a single file that will be converted
        final File inputFile = tmp;

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
                    File toDelete = GUIUtil.getTopmostSubdirectoryContaining(new File(TempFileUtil.getTemporaryDirectory()), inputFile);
                    IOUtil.recursiveDelete(toDelete);
                    return;
                }
            }
        };

        dthread.start();

        // Give the download thread a chance to start
        Thread.yield();

        // Second, convert the files
        final PKConvertTask ctask = new PKConvertTask(inputFile, super.outputFile);

        final Thread cthread = new Thread("Convert conversion thread") {

            public void run() {

                try {
                    progressBar.updateStatus(50, "Converting peak list...");
                    ctask.execute();

                    if (frame != null) {
                        JOptionPane.showMessageDialog(
                                frame,
                                "Success, the file is located at " + outputFile.getAbsolutePath(),
                                "Peak lists converted.",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception e) {
                    if (frame != null) {
                        ErrorFrame er = new ErrorFrame();
                        er.show(e, frame);
                    } else {
                        e.printStackTrace();
                    }
                } finally {
                    redrawForStop();
                    File toDelete = GUIUtil.getTopmostSubdirectoryContaining(new File(TempFileUtil.getTemporaryDirectory()), inputFile);
                    IOUtil.recursiveDelete(toDelete);
                }
            }
        };

        Thread progressThread = new Thread("Progress thread") {

            public void run() {
                while (dtask.isRunning()) {
                    // Arbitrarily choose that download task is half the job, adjust percentage accordingly
                    progressBar.updateStatus(dtask.getPercentComplete() / 2, "Downloading peak list");
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                        // No thanks
                    }
                }

                // Now that it's done, start the conversion thread
                cthread.start();
            }
        };

        progressThread.start();
    }

    public void dragEnter(DropTargetDragEvent dtde) {
    }

    public void dragOver(DropTargetDragEvent dtde) {
    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    public void dragExit(DropTargetEvent dte) {
    }

    public void drop(final DropTargetDropEvent dtde) {
        try {
            if (userZipFile != null) {
                // accept the drop
                dtde.acceptDrop(dtde.getDropAction());
                final List<File> fileList = (List) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

                Thread t = new Thread("Drop File") {

                    public void run() {
                        try {
                            // for all of the files
                            for (File file : fileList) {
                                // add file to tranche and this panel
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            // finish
                            dtde.dropComplete(true);
                        }
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addFileToTrancheAndPanel(File file) {
        try {
            // recursively add to tranche and panel
            if (file.isDirectory()) {
                for (File subFile : file.listFiles()) {
                    addFileToTrancheAndPanel(subFile);
                }
                return;
            }

            // only files fall through
            if (userZipFile != null) {
                ServerUtil.waitForStartup();
                // add to tranche
                AddFileTool aft = new AddFileTool(userZipFile.getCertificate(), userZipFile.getPrivateKey());
                aft.setServersToUploadTo(1);
                aft.setSingleFileUpload(true);
                aft.setUseRemoteReplication(true);
                BigHash hash = aft.addFile(file);
                // add file to be concatenated
                PKConvertPanel.this.addFile(hash);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @DropTargetListeners
    public List<DropTargetListener> getDropTargetListeners() {
        return dropTargetListeners;
    }

    @Init
    public void init4G() {
    }

    @Dispose
    public void dispose4G() {
    }

    @Panel(name = "Peak List Convert", width = 425, height = 310)
    public JPanel getPeakListConvertPanel() {
        return this;
    }

    @CurrentPanel
    public JPanel getCurrentPanel() {
        return this;
    }

    @IsSatisfied(panelName = "Peak List Convert")
    public boolean isSatisfied() {
        return false;
    }

    @Order
    public LinkedList<String> getOrder() {
        LinkedList ll = new LinkedList<String>();
        ll.add("Peak List Convert");
        return ll;
    }

    @PrerequisiteCallbackMethod(type = Prerequisite.PREREQ_BIGHASH, title = "Input File", inputTitle = "Tranche Hash")
    public void addFile(BigHash hash) {
        try {
            addFile(hash);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private UserZipFile userZipFile = null;

    @PrerequisiteCallbackMethod(type = Prerequisite.PREREQ_USERZIPFILE, title = "User Zip File", inputTitle = "User Zip File")
    public void getUserZipFile(UserZipFile userZipFile) {
        try {
            this.userZipFile = userZipFile;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
