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
package org.proteomecommons.tranche.modules.pkconcat;

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
import org.proteomecommons.fourg.annotations.ModuleIsSatisfied;
import org.proteomecommons.fourg.annotations.Order;
import org.proteomecommons.fourg.annotations.Panel;
import org.proteomecommons.fourg.annotations.PrerequisiteCallbackMethod;
import org.proteomecommons.fourg.annotations.SaveFile;
import org.proteomecommons.fourg.prerequisites.Prerequisite;
import org.tranche.add.AddFileTool;
import org.tranche.get.GetFileTool;
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
@Module(name = "Peak List Concatatenator", description = "Concatenate multiple peak lists together without attempting to convert.")
public class PKConcatPanel extends AbstractPeakListPanel implements DropTargetListener {

    // necessary for 4g drag and drop
    private List<DropTargetListener> dropTargetListeners = new ArrayList<DropTargetListener>();
    private UserZipFile userZipFile = null;

    public PKConcatPanel() {
        this("Concatenate multiple peak lists together without attempting to convert.", new HashMap<BigHash, String>());
    }

    public PKConcatPanel(String description, Map<BigHash, String> files) {
        super(OPTION_DISABLE_OUTPUT_FORMAT_DROPDOWN, description, files);

        // Overwrite tailored components
        this.description = description;
        this.filesToDownload = files;
        this.PREFERRED_DIMENSION = new Dimension(425, 280);
        dropTargetListeners.add(this);
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
            if (extension == null || extension.equals("") || !extension.contains(".")) {
                extension = ".txt";
            } else {
                extension = extension.substring(extension.lastIndexOf('.'));
            }

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
                    if (frame != null) {
                        ErrorFrame er = new ErrorFrame();
                        er.show(ex, frame);
                    }
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
        final PKConcatTask ctask = new PKConcatTask(inputFiles, super.outputFile);

        final Thread cthread = new Thread("Concatention thread") {

            public void run() {

                try {
                    progressBar.updateStatus(50, "Concatenating files.");
                    ctask.execute();

                    if (frame != null) {
                        JOptionPane.showMessageDialog(
                                frame,
                                "Success, the file is located at " + outputFile.getAbsolutePath(),
                                "Peak lists concatenated.",
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
                                addFileToTrancheAndPanel(file);
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
                addFileToDownload(hash, file.getName());
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

    @Panel(name = "Peak List Concat", width = 425, height = 250)
    public JPanel getPeakListConcatPanel() {
        return this;
    }

    @CurrentPanel
    public JPanel getCurrentPanel() {
        return this;
    }

    @IsSatisfied(panelName = "Peak List Concat")
    public boolean isSatisfied() {
        return isComplete();
    }

    @ModuleIsSatisfied()
    public boolean moduleisSatisfied() {
        return isComplete();
    }

    @SaveFile
    public String moduleSaveFile() {
        return "Hello world!";
    }

    @Order
    public LinkedList<String> getOrder() {
        LinkedList ll = new LinkedList<String>();
        ll.add("Peak List Concat");
        return ll;
    }

    @PrerequisiteCallbackMethod(type = Prerequisite.PREREQ_BIGHASH, title = "Input File", inputTitle = "Tranche Hash", blocking = false, maxRecursions = 0)
    public void addFile(BigHash hash) {
        try {
            ServerUtil.waitForStartup();
            GetFileTool gft = new GetFileTool();
            gft.setHash(hash);
            addFileToDownload(hash, gft.getMetaData().getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PrerequisiteCallbackMethod(type = Prerequisite.PREREQ_USERZIPFILE, title = "User Zip File", inputTitle = "User Zip File")
    public void getUserZipFile(UserZipFile userZipFile) {
        try {
            this.userZipFile = userZipFile;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PrerequisiteCallbackMethod(type = Prerequisite.PREREQ_FILE, title = "Save To", inputTitle = "Location")
    public void getFile(File file) {
        try {
            System.out.println("File: " + file.getAbsolutePath());
            outputFile = file;
            baseFile = file;
            outputButton.setText(file.getAbsolutePath());
            outputButton.setToolTipText(file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
