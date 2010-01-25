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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;
import org.tranche.util.IOUtil;

/**
 * <p>Literal concatenation of multiple peak lists into single file.</p>
 *
 * @author  Bryan Smith - bryanesmith@gmail.com
 * @version %I%, %G%
 * @since   1.0
 */
public class PKConcatTask {

    private List<File> files;
    private File output;
    private String NEWLINE;    // Used to mark that a task is running
    private boolean isRunning;
    private double percent;

    /**
     * @param   files   the list of files received 
     * @param   output  the output file received
     * @since           1.0
     */
    public PKConcatTask(List<File> files, File output) {
        this.files = files;
        this.output = output;

        NEWLINE = System.getProperty("line.separator");
        if (NEWLINE == null || NEWLINE == "") {
            NEWLINE = "\n";
        }
        this.isRunning = false;
        this.percent = 0.0;
    }

    /**
     * @throws  Exception   if any exception occurs
     * @since               1.0 
     */
    public void execute() throws Exception {

        this.isRunning = true;

        BufferedWriter out = null;
        BufferedReader in = null;

        // Used to track percent done
        long totalBytes = 0;
        long finishedBytes = 0;

        for (File file : files) {
            totalBytes += file.length();
        }

        try {
            output.createNewFile();
            out = new BufferedWriter(new FileWriter(this.output));
            String str;

            for (File file : files) {

                in = new BufferedReader(new FileReader(file));

                while ((str = in.readLine()) != null) {
                    out.write(str + NEWLINE);
                }

                out.write(NEWLINE);
                out.flush();

                IOUtil.safeClose(in);

                finishedBytes += file.length();
                this.percent = (double) finishedBytes / (double) totalBytes;
            }
        } finally {
            IOUtil.safeClose(out);
            IOUtil.safeClose(in);
            this.isRunning = false;
            this.percent = 100.0;
        }
    }

    /**
     * @return  <code>true</code> if the current task is running;
     *          <code>false</code> otherwise
     * @since   1.0 
     */
    public boolean isRunning() {
        return this.isRunning;
    }

    /**
     * @return  the percent of the current task completed
     * @since   1.0 
     */
    public double getPercentComplete() {
        return this.percent;
    }
}
