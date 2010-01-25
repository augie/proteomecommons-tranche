package org.proteomecommons.tranche.modules.xarsummary;

import java.io.File;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.proteomecommons.tranche.GetFileTool;
import org.tranche.gui.ErrorFrame;
import org.tranche.gui.GUIUtil;
import org.tranche.gui.GenericPopupFrame;
import org.tranche.gui.IndeterminateProgressBar;
import org.tranche.gui.LocalServerUtil;
import org.tranche.hash.BigHash;
import org.tranche.modules.LeftMenuAnnotation;
import org.tranche.modules.PopupMenuAnnotation;
import org.tranche.modules.TrancheMethodAnnotation;
import org.tranche.modules.TrancheModuleAnnotation;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;

/**
 * Opens up a XAR summary.
 * @author besmit
 */
@TrancheModuleAnnotation(name = "XAR Summary Module",
description = "View summary of XAR file.")
public class Main {

    @LeftMenuAnnotation(scope = "Files")
    @PopupMenuAnnotation(scope = "Files")
    @TrancheMethodAnnotation(fileExtension = "xml|bioxml",
    mdAnnotation = "*",
    selectionMode = "single",
    label = "View XAR Summary",
    description = "View summary of XAR file.")
    public static void openXARSummary(BigHash hash) {
        File xarFile = null;
        try {
            xarFile = downloadFileWithProgress(hash);
            XARSummary xar = XARUtil.createXARSummary(xarFile);
            GenericPopupFrame popup = new GenericPopupFrame("XAR Summary", new XARSummaryPanel(xar));
            popup.setSizeAndPosition(XARSummaryPanel.RECOMMENDED_DIMENSION);
            popup.setVisible(true);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace(System.err);
            ErrorFrame ef = new ErrorFrame();
            ef.show(ex, GUIUtil.getAdvancedGUI());
        } finally {
            IOUtil.safeDelete(xarFile);
        }
    }

    public static void main(String args[]) {
        File xarFile = null;
        try {
            xarFile = new File(args[0]);
            XARSummary xar = XARUtil.createXARSummary(xarFile);
            GenericPopupFrame popup = new GenericPopupFrame("XAR Summary", new XARSummaryPanel(xar));
            popup.setSizeAndPosition(XARSummaryPanel.RECOMMENDED_DIMENSION);
            popup.setVisible(true);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }
    
    public static File downloadFileWithProgress(BigHash hash, String filename, final JFrame frame) throws Exception {

        String extension = null;

        if (filename == null) {
            extension = ".tmp";
        } else {

            int position = filename.lastIndexOf(".");

            if (position == -1) {
                extension = ".tmp";
            } else {
                extension = filename.substring(position);
            }
        }

        final File tmp = TempFileUtil.createTemporaryFile(extension);

        final IndeterminateProgressBar progress = new IndeterminateProgressBar("Downloading file...");

        Thread t = new Thread() {

            public void run() {
                progress.setLocationRelativeTo(frame);
                progress.start();
            }
        };
        SwingUtilities.invokeLater(t);

        GetFileTool gft = new GetFileTool();
        gft.setValidate(false);
        gft.setHash(hash);

        if (LocalServerUtil.isServerRunning()) {
            gft.setServersToUse(LocalServerUtil.getLocalAndCoreServers());
        }

        try {
            gft.getFile(tmp);
        } finally {
            progress.stop();
        }

        return tmp;
    }
}
