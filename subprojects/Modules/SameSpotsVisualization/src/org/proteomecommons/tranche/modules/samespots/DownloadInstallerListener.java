package org.proteomecommons.tranche.modules.samespots;

import java.text.NumberFormat;
import org.tranche.get.GetFileTool;
import org.tranche.get.GetFileToolListener;
import org.tranche.gui.GenericProgressiveTaskPopup;
import org.tranche.hash.BigHash;
import org.tranche.project.file.ProjectFile;
import org.tranche.timeestimator.ContextualTimeEstimator;
import org.tranche.timeestimator.TimeEstimator;

/**
 *
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 */
public class DownloadInstallerListener implements GetFileToolListener {

    private GenericProgressiveTaskPopup popup;
    private NumberFormat nf;    // track bytes to download
    private long bytesToDownload = 1;
    private long bytesDownloaded = 0;    // make a work estimator
    private TimeEstimator te = new ContextualTimeEstimator();

    public DownloadInstallerListener(GenericProgressiveTaskPopup popup) {
        this.popup = popup;

        // config the instance
        nf = NumberFormat.getInstance();
        nf.setGroupingUsed(true);
        nf.setMaximumFractionDigits(3);
    }

    public void failedChunk(GetFileTool gft, BigHash fileHash, BigHash chunkHash) {
        // Do nothing, but uh-oh
    }

    public void skippedChunkDownload(GetFileTool gft, BigHash fileHash, BigHash chunkHash) {
        // Do nothing, but uh-oh
    }

    public void startedChunkDownload(GetFileTool gft, BigHash fileHash, BigHash chunkHash) {
        // Do nothing, but uh-oh
    }

    public void finishedChunkDownload(GetFileTool gft, BigHash fileHash, BigHash chunkHash) {
        // 
        bytesDownloaded += chunkHash.getLength();
        int percent = getPercentComplete(bytesDownloaded, bytesToDownload);
        this.popup.setPercentComplete(percent);

        // How much time remaining?
        te.update(bytesDownloaded, bytesToDownload);
        this.popup.setProgressBarText(nf.format(te.getPercentDone()) + "% done, time left: " + te.getHours() + "h " + te.getMinutes() + "m " + te.getSeconds() + "s");
    }

    public void failedFile(GetFileTool gft, BigHash hash) {
        // Argh
    }

    public void skippedFileDownload(GetFileTool gft, BigHash hash) {
        // Shouldn't happen
    }

    public void startedFileDownload(GetFileTool gft, BigHash hash, BigHash encodedHash, String relativeName) {
        // Only one file
        bytesToDownload = hash.getLength();
        this.popup.setPercentComplete(0);
    }

    public void finishedFileDownload(GetFileTool gft, BigHash hash) {
        // Complete!
        this.popup.setPercentComplete(100);
        this.popup.setProgressBarText("100% complete, validating...");
    }

    public void startedDirectoryDownload(GetFileTool gft, BigHash projectFileHash, ProjectFile projectFile) {
        // Skip, we are only considered w/ file
    }

    public void finishedDirectoryDownload(GetFileTool gft, BigHash projectFileHash) {
        // Irrelevant
    }

    public int getPercentComplete(long downloaded, long total) {
        float decimal = (float) downloaded / (float) total;
        decimal *= 100;
        return Math.round(decimal);
    }
}
