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
package org.proteomecommons.tranche.cacheupdater;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.LinkedList;

/**
 * @author James "Augie" Hill
 */
public class FileTypeLogParseScript {

    public static void runOn(final File fileTypeLogFile, final File logFile) {
        Thread t = new Thread() {

            public void run() {
                // variables and structures needed in the output of the final log
                BigInteger totalSize = BigInteger.ZERO;
                LinkedList<FileTypeSummary> fileTypeSummaries = new LinkedList<FileTypeSummary>();

                //
                PrintStream ps = null;
                BufferedReader br = null;
                FileReader fr = null;
                try {
                    fr = new FileReader(fileTypeLogFile);
                    br = new BufferedReader(fr);
                    ps = new PrintStream(logFile);

                    // throw away the first line
                    if (br.ready()) {
                        br.readLine();
                    }

                    // read the file type info
                    while (br.ready()) {
                        try {
                            String line = br.readLine();
                            FileTypeSummary fts = new FileTypeSummary();

                            // read the file type
                            fts.fileType = line.substring(0, line.indexOf("\t")).trim();
                            line = line.substring(line.indexOf("\t")).trim();

                            // read the # of files
                            fts.files = new BigInteger(line.substring(0, line.indexOf("\t")).trim());
                            line = line.substring(line.indexOf("\t")).trim();

                            // read the size
                            fts.size = new BigInteger(line.substring(0, line.indexOf("\t")).trim());
                            line = line.substring(line.indexOf("\t")).trim();

                            // read the human readable # files
                            fts.filesHR = line.substring(0, line.indexOf("\t")).trim();
                            line = line.substring(line.indexOf("\t")).trim();

                            // read the human readable size
                            fts.sizeHR = line.trim();

                            fileTypeSummaries.add(fts);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    // calculate the total size of the data
                    for (FileTypeSummary fts : fileTypeSummaries) {
                        totalSize = fts.size.add(totalSize);
                    }

                    // set the percentage for each of the file type summaries
                    for (FileTypeSummary fts : fileTypeSummaries) {
                        fts.percentage = ((float) fts.size.longValue() / totalSize.longValue()) * 100.0f;
                    }

                    // delete the percentages lower than 1
                    LinkedList<FileTypeSummary> newList = new LinkedList<FileTypeSummary>();
                    for (FileTypeSummary fts : fileTypeSummaries) {
                        if (fts.percentage >= 1.0f) {
                            newList.add(fts);
                        }
                    }
                    fileTypeSummaries = newList;

                    File outputFile = new File(fileTypeLogFile.getParentFile(), "filetype.output");
                    if (!outputFile.exists()) {
                        outputFile.createNewFile();
                    }
                    FileWriter fw = null;
                    try {
                        fw = new FileWriter(outputFile);
                        NumberFormat nf = NumberFormat.getInstance();
                        nf.setParseIntegerOnly(false);
                        nf.setMaximumFractionDigits(1);
                        nf.setMinimumFractionDigits(1);
                        fw.write("filetype\tpercentage\tfileshr\tsizehr\n");
                        for (FileTypeSummary fts : fileTypeSummaries) {
                            fw.write(fts.fileType + "\t" + nf.format((double) fts.percentage) + "\t" + fts.filesHR + "\t" + fts.sizeHR + "\n");
                        }
                    } finally {
                        fw.flush();
                        fw.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        ps.println("Finished");
                        ps.flush();
                        ps.close();
                        br.close();
                        fr.close();
                    } catch (Exception e) {
                    }
                }
            }
        };
        t.start();
    }

    private static class FileTypeSummary {

        public float percentage = 0.0f;
        public BigInteger size = BigInteger.ZERO,  files = BigInteger.ZERO;
        public String fileType = "",  filesHR = "",  sizeHR = "";
    }
}
