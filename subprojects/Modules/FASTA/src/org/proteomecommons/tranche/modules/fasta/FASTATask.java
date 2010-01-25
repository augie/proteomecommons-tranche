package org.proteomecommons.tranche.modules.fasta;

import java.io.File;
import java.util.Map;
import org.proteomecommons.io.fasta.FASTAProtein;
import org.proteomecommons.io.fasta.FASTAReader;
import org.proteomecommons.io.fasta.FASTAReaderFactory;
import org.proteomecommons.io.fasta.FASTAWriter;
import org.proteomecommons.io.fasta.FASTAWriterFactory;
import org.tranche.tasks.TaskInterface;

/**
 *
 * @author  James "Augie" Hill <augman85@gmail.com>
 */
public class FASTATask implements TaskInterface {

    private Map<File, FASTAReaderFactory> files;
    private File output;
    private boolean reverse = false;
    private String NEWLINE,  signifyReverse = "_REVERSED";
    private FASTAWriterFactory writerFactory;    // Used to mark that a task is running
    private boolean isRunning;
    private double percent;

    /**
     * @param   files   the list of files received
     * @param   output  the output file received
     * @param   reverse whether the protein sequences should be reversed
     * @param   signifyReverse  the text that should be added to the accession of the protein sequences header line if reverse is true
     * @param   writer  the writer factory to be used
     */
    public FASTATask(Map<File, FASTAReaderFactory> files, File output, boolean reverse, String signifyReverse, FASTAWriterFactory writerFactory) {
        this.files = files;
        this.output = output;
        this.reverse = reverse;
        this.signifyReverse = signifyReverse;
        this.writerFactory = writerFactory;

        NEWLINE = System.getProperty("line.separator");
        if (NEWLINE == null || NEWLINE == "") {
            NEWLINE = "\n";
        }
        this.isRunning = false;
        this.percent = 0.0;
    }

    /**
     * @throws  Exception   if any exception occurs
     */
    public void execute() throws Exception {
        isRunning = true;

        long totalBytes = 0, finishedBytes = 0;

        for (File file : files.keySet()) {
            totalBytes += file.length();
            if (files.get(file) == null) {
                throw new RuntimeException("FASTA Reader Factory cannot be null.");
            }
        }

        if (reverse) {
            totalBytes *= 2;
        }

        FASTAWriter writer = null;
        try {
            writer = writerFactory.newInstance(output.getAbsolutePath());
            if (reverse) {
                for (File file : files.keySet()) {
                    FASTAReader reader = files.get(file).newInstance(file.getAbsolutePath());
                    writer.beginReader(reader);
                    try {
                        FASTAProtein protein = reader.next();

                        writer.write(protein);

                        // update the percent done (approximate)
                        finishedBytes += protein.numberOfCharacters();
                        percent = (double) finishedBytes / (double) totalBytes;

                        protein = reader.next();
                    } finally {
                        reader.close();
                    }
                }
            }
            for (File file : files.keySet()) {
                FASTAReader reader = files.get(file).newInstance(file.getAbsolutePath());
                writer.beginReader(reader);
                try {
                    FASTAProtein protein = reader.next();

                    if (reverse) {
                        writer.writeReverse(protein, signifyReverse);
                    } else {
                        writer.write(protein);
                    }

                    // update the percent done (approximate)
                    finishedBytes += protein.numberOfCharacters();
                    percent = (double) finishedBytes / (double) totalBytes;

                    protein = reader.next();
                } finally {
                    reader.close();
                }
            }
        } finally {
            writer.close();
            isRunning = false;
            percent = 100.0;
        }
    }

    /**
     * @return  <code>true</code> if the current task is running;
     *          <code>false</code> otherwise
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * @return  the percent of the current task completed
     */
    public double getPercentComplete() {
        return percent;
    }
}
