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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;

/**
 * Temporary location for SQT generator. Will eventually be absorbed by IO Framework.
 *
 * @author  Bryan Smith - bryanesmith@gmail.com
 * @version %I%, %G%
 * @since   1.1
 */
public class SQTUtil {

    /**
     * CHANGE LOG:
     * -------------------------------------------------------------------------
     * version 1.1:
     *   - Add support for multiple references in match record, per Dr. Tabb's request
     *   - Always at least 1 locus per match record
     */
    public static final String GENERATOR_NAME = "Tranche SQT Generator";
    public static final String GENERATOR_VERSION = "1.1";
    public static String NEWLINE = System.getProperty("line.separator");
    public static final String TAB = "\t";    // Turn on debugger if want to see parsing on standard out
    private static final boolean DEBUG = true;    // Hold name for current file
    private static String currentFileName = null;    // Internal flags used for identifying lines
    private static final byte EMPTY = 0,  HEADER = 1,  MATCH = 2,  LOCUS = 3,  MATCH_REFERENCE = 4;

    /**
     * <p>Converts set of Sequest output files to a SQT file.</p>
     *
     * @param   outs        the set of output files
     * @param   sqt         the destination for the SQT file
     * @throws  Exception   if any exception occurs
     * @since               1.0
     */
    public static void createSQT(Set<File> outs, File sqt) throws Exception {
        // Perchance system property not set
        if (NEWLINE == null) {
            NEWLINE = "\n";        // Used to keep track of conversion state within a single out file.
        }
        byte currentLineStatus = EMPTY;

        BufferedReader in = null;
        BufferedWriter out = null;

        // Parts that are built from data across multiple lines
        Header header = new Header();
        SpectrumRecord spectrum = new SpectrumRecord();
        header.start();

        // Use a temp file b/c header must be written first, calculated last
        File tmp = TempFileUtil.createTemporaryFile();

        // Holds string after file read
        String str;

        try {
            // First write to a temp file
            out = new BufferedWriter(new FileWriter(tmp));

            for (File next : outs) {

                currentFileName = next.getName();
                in = new BufferedReader(new FileReader(next));

                // Represents a single OUT file
                OutFile outFileObj = new OutFile();

                // Handle each line according to its purpose
                while ((str = in.readLine()) != null) {

                    // What type of line is it?
                    byte type = getCurrentLineStatus(str);

                    switch (type) {

                        case EMPTY: // Do nothing
                            break;

                        case HEADER:
                            extractHeaderInfo(header, str);

                            // May have spectrum data
                            if (containsSpectrumData(str)) {
                                extractSpectrumInfo(outFileObj.spectrumRecord, str);
                            }
                            break;

                        case MATCH:
                            outFileObj.matchRecords.add(getMatchRecord(str));
                            break;

                        case MATCH_REFERENCE:
                            outFileObj.getLastMatchRecord().references.add(getMatchRecordReference(str));
                            break;

                        case LOCUS:
                            outFileObj.locusRecords.add(getLocusRecord(str));
                            break;

                        default:
                            throw new RuntimeException("Unknown token " + type + " in switch.");
                    }
                }

                // Write out information from out file obj
                out.write(outFileObj.toString());

                // Next file
                IOUtil.safeClose(in);
            }

            // Start reading from tmp and writing to SQT.
            out.flush();
            IOUtil.safeClose(out);
            out = new BufferedWriter(new FileWriter(sqt));
            in = new BufferedReader(new FileReader(tmp));

            // Write out header and temp file
            header.stop();
            out.write(header.toString());
            while ((str = in.readLine()) != null) {
                out.write(str + NEWLINE);
            }
        } finally {
            IOUtil.safeClose(in);
            out.flush();
            IOUtil.safeClose(out);
            IOUtil.safeDelete(tmp);
        }
    }

    /**
     * Escapes characters for fields that are not permitted to have spaces.
     *
     * @param   string  the string checked for spaces
     * @return          the string in which spaces are replaced with plus signs
     * @since           1.0
     */
    public static String escapeSpaces(String string) {
        if (string == null) {
            return null;
        }
        return string.replace(" ", "+");
    }

    /**
     * <p>Some header lines contain spectrum data.</p>
     *
     * @param   line    the line checked for spectrum data
     * @return          <code>true</code> if the line contains spectrum data;
     *                  <code>false</code> otherwise
     * @since           1.0
     */
    private static boolean containsSpectrumData(String line) {
        line = line.trim();

        // Contains the server name
        if (line.contains(" on ")) {
            return true;
        // Mass and fragment line
        }
        if (line.contains("mass") || line.contains("fragment tol")) {
            return true;
        // Intensity line
        }
        if (line.contains("inten") && line.contains("peptides")) {
            return true;
        // Database line
        }
        if (line.contains("amino acids") && line.contains("proteins")) {
            return true;
        // TODO What is the scan?
        }
        if (line.contains("CODE") || line.matches(".*\\(\\d\\d\\d\\d\\d?\\).*")) {
            return true;
        }
        return false;
    }

    /**
     * Adds to existing header object with info extracted from line.
     *
     * @param   header  the header to which information is added
     * @param   line    the line from which information is extracted
     * @since           1.0
     */
    private static void extractHeaderInfo(Header header, String line) {
        line = line.trim();

        // Get database info (num amino acids/SeqLength, num proteins/LocusCount, path)
        if (line.contains("# amino acids")) {
            String[] split1 = line.split(",");
            String dbPath = split1[2].trim();
            if (!header.databases.contains(dbPath) && isFilePath(dbPath)) {
                header.databases.add(dbPath);
            }
            header.DBSeqLength = Long.parseLong(split1[0].split("=")[1].trim());
            header.DBLocusCount = Long.parseLong(split1[1].split("=")[1].trim());
        } // Get two masses (AVG, MONO, etc.), Alg-FragMassTotal
        else if (line.contains("(M+H)+ mass") || line.contains("fragment tol")) {
            String[] split1 = line.split(",");

            // Mine entries for different values, variable length num entries
            for (String next : split1) {

                next = next.trim();

                if (next.contains("mass tol")) {
                    if (!header.algs.containsKey("Alg-PreMassTotal")) {
                        header.algs.put("Alg-PreMassTotal", next.split("=")[1].trim());
                    }
                } else if (next.contains("mass")) {
                    if (!header.algs.containsKey("")) {
                        // TODO Handled elsewhere? In Spectrum line, yes, should be in header, too?
                    }
                } else if (next.contains("fragment tol")) {
                    if (!header.algs.containsKey("Alg-FragMassTotal")) {
                        header.algs.put("Alg-FragMassTotal", next.split("=")[1].trim());
                    }
                }
            }

            // Maybe form "MASS/MASS". MASS should be the last field in line.
            if (split1[2].contains("/")) {
                header.precursorMasses = split1[split1.length - 1].split("/")[0].trim();
                header.fragmentMasses = split1[split1.length - 1].split("/")[1].trim();
            } // TODO If only one field, is it both?
            else {
                header.precursorMasses = split1[split1.length - 1].trim();
                header.fragmentMasses = split1[split1.length - 1].trim();
            }
        } // Is there a static mod (c=)? Add. Also grab any other mods in parentheses,
        // including DiffMod (ST), and EnzymeSpec
        else if (line.contains("Enzyme")) {
            Pattern p = Pattern.compile("Enzyme:(.*?)\\(");
            Matcher m = p.matcher(line);

            if (m.find()) {
                header.enzyme = m.group(1);
            }
            String[] split1 = line.split("\\s");

            for (String string : split1) {
                string = string.trim();

                if (string.startsWith("C=")) {
                    String staticMod = string.split("=")[1].trim();
                    if (!header.staticMod.contains(staticMod)) {
                        header.staticMod.add(staticMod);
                    }
                } else if (string.startsWith("(") && string.endsWith(")")) {
                    String diffMod = string.substring(1, string.length() - 1).replace(' ', '=').trim();
                    if (!header.diffMod.contains(diffMod)) {
                        header.diffMod.add(diffMod);
                    }
                }
            }
        } // Is/are there [a] dynamic mod? Add.
        // TODO More information needed!
        // If ion field, Add a Alg-IonSeries
        else if (line.contains("ion series")) {
            String[] split1 = line.split(":");
            String series = split1[1].trim();
            if (!header.algs.containsKey("Alg-IonSeries")) {
                header.algs.put("Alg-IonSeries", series);
            }
        } // Add other Alg-
        // TODO Wow, a definitive list maybe?
        // Unknown line, may contain some random fields
        else {
            String[] split1 = line.split(",");

            for (String next : split1) {
                next = next.trim();

                // Is it a database path?
                if (isFilePath(next)) {
                    if (!header.databases.contains(next)) {
                        header.databases.add(next);
                    }
                }
            }
        }
    }

    /**
     * <p>Best-effort attempt to determine whether a file path.</p>
     *
     * @param   string  string checked to determine whether it is a file path
     * @return          <code>true</code> if the string is a file path;
     *                  <code>false</code> otherwise
     * @since           1.0
     */
    private static boolean isFilePath(String string) {
        string = string.trim();

        // Unix-styel
        if (string.startsWith("/")) {
            return true;
        // Win-style
        } else if (string.contains(":\\")) {
            return true;
        }
        return false;
    }

    /**
     * Parses out additional references to matches found on subsequent line underneath a match record.
     */
    private static String getMatchRecordReference(String line) {
        String[] tokens = line.trim().split("\\s+");
        return tokens[1];
    }

    /**
     * Adds to existing spectrum object with info extracted from line.
     *
     * @param   record  the record to which information is added
     * @param   line    the line from which information is extracted
     * @since           1.0
     */
    private static void extractSpectrumInfo(SpectrumRecord record, String line) {
        line = line.trim();

        // Get server and process time
        if (line.contains(" on ")) {
            String[] split1 = line.split(" on ");
            record.server = split1[1].trim();
            String[] split2 = line.split(",");

            // The word "on" can appear anywhere, make sure actually is a spectrum
            if (split2.length != 2 || !split2[2].matches(".*?(\\d+).*?")) {
                return;
            }

            long sec = 0;

            // Secs
            Pattern p = Pattern.compile("(\\d+) sec.");
            Matcher m = p.matcher(split2[2]);
            if (m.find()) {
                sec += Long.parseLong(m.group(1));            // Mins
            }
            p = Pattern.compile("(\\d+) min.");
            m = p.matcher(split2[2]);
            if (m.find()) {
                sec += 60 * (Long.parseLong(m.group(1)));            // Hours
            }
            p = Pattern.compile("(\\d+) hr.");
            m = p.matcher(split2[2]);
            if (m.find()) {
                sec += 60 * 60 * (Long.parseLong(m.group(1)));
            }
            record.processTime = sec;
        } // Get mass and charge
        else if (line.contains("mass") || line.contains("fragment")) {

            // Get the observed mass
            String[] split1 = line.split("=");
            Pattern p = Pattern.compile("(\\d+\\.?\\d*)");
            Matcher m = p.matcher(split1[1]);

            if (m.find()) {
                record.obsMass = Double.parseDouble(m.group(1));
            }

            // Get the charge, something similar to (+1), need the number
            boolean isPositive = true;

            if (split1[1].contains("(-")) {
                isPositive = false;
            }
            p = Pattern.compile("\\([+-]?(\\d+)\\)");
            m = p.matcher(split1[1]);

            if (m.find()) {
                long charge = Long.parseLong(m.group(1));

                // If there was a '-' character, make value negative
                if (!isPositive) {
                    charge = 0 - charge;
                }
                record.charge = charge;
            }
        } // Get total intensity, lowest sp
        else if (line.contains("total inten")) {
            String[] split1 = line.split(",");

            for (String entry : split1) {

                entry = entry.trim();

                if (entry.contains("total inten")) {
                    record.totalIntensity = Double.parseDouble(entry.split("=")[1].trim());
                } else if (entry.contains("lowest")) {
                    record.lowestSp = Double.parseDouble(entry.split("=")[1].trim());
                } // TODO Is this really the # sequences matched?
                else if (entry.contains("peptides")) {
                    record.sequencesMatch = Integer.parseInt(entry.split("=")[1].trim());
                }

            }
        } // Get scan values
        // TODO Is this really the high scan and low scan?
        else if (line.contains("CODE")) {
            String[] split1 = line.split("CODE =");
            record.highScan = split1[1];
            record.lowScan = split1[1];
        }

        // TODO Is this really the high scan and low scan?
        if (line.matches(".*\\(\\d\\d\\d\\d\\d\\).*")) {
            Pattern p = Pattern.compile(".*\\((\\d\\d\\d\\d\\d?)\\).*");
            Matcher m = p.matcher(line);
            if (m.find()) {
                record.highScan = m.group(1);
                record.lowScan = m.group(1);
            }
        }
    }

    /**
     * Builds a match record from a line.
     *
     * @param   line    the line from which the match record is built
     * @return          the match record built
     * @since           1.0
     */
    private static MatchRecord getMatchRecord(String line) {
        MatchRecord record = new MatchRecord();

        // Split on white space
        line = line.trim();

        // Make sure there is space between all entries
        line = line.replaceAll("/", " / ");

        final byte VERSION = MatchRecord.getMatchRecordVersion(line);

        // Split on spaces
        String[] fields = line.split("\\s+");

        // Extraction depends on type
        if (VERSION == MatchRecord.VERSION_A) {

            record.rankXcorr = Integer.parseInt(fields[1]);
            record.rankSp = Integer.parseInt(fields[3]);
            record.calcMass = Double.parseDouble(fields[5]);
            record.deltCN = Double.parseDouble(fields[6]);
            record.Xcorr = Double.parseDouble(fields[7]);
            record.Sp = Double.parseDouble(fields[8]);
            record.matchedIons = Integer.parseInt(fields[9]);
            record.expectedIons = Integer.parseInt(fields[11]);
            record.references.add(fields[12]);

            // Another trick... optional entry before sequence, so just assign last item
            record.sequence = fields[fields.length - 1];
        } // Second scenario: there isn't an id
        else if (VERSION == MatchRecord.VERSION_B) {

            record.rankXcorr = Integer.parseInt(fields[1]);
            record.rankSp = Integer.parseInt(fields[3]);
            record.calcMass = Double.parseDouble(fields[4]);
            record.deltCN = Double.parseDouble(fields[5]);
            record.Xcorr = Double.parseDouble(fields[6]);
            record.Sp = Double.parseDouble(fields[7]);
            record.matchedIons = Integer.parseInt(fields[8]);
            record.expectedIons = Integer.parseInt(fields[10]);
            record.references.add(fields[11]);

            // Another trick... optional entry before sequence, so just assign last item
            record.sequence = fields[fields.length - 1];
        } else if (VERSION == MatchRecord.VERSION_C) {
            record.rankXcorr = Integer.parseInt(fields[1]);
            record.rankSp = Integer.parseInt(fields[3]);
            record.calcMass = Double.parseDouble(fields[4]);
            record.deltCN = Double.parseDouble(fields[6]);
            record.Xcorr = Double.parseDouble(fields[7]);
            record.Sp = Double.parseDouble(fields[8]);
            record.matchedIons = Integer.parseInt(fields[9]);
            record.expectedIons = Integer.parseInt(fields[11]);
            record.references.add(fields[12]);

            // Another trick... optional entry before sequence, so just assign last item
            record.sequence = fields[fields.length - 1];
        } else {
            throw new RuntimeException("The following match record could not be parsed: " + line);
        }

        // No way to calculate validation status, leave as U

        return record;
    }

    /**
     * Builds a locus record from a line.
     *
     * @param   line    the line from which the locus record is built
     * @return          the locus record built
     * @since           1.0
     */
    private static LocusRecord getLocusRecord(String line) {
        LocusRecord record = new LocusRecord();

        line = line.trim();
        String[] entries = line.split("\\s+");

        int index = -1;

        // Find the locus... might be 2nd or third
        if (entries[1].trim().matches("\\d+")) {
            record.locus = entries[2].trim();
            index = 2;
        } else {
            record.locus = entries[1].trim();
            index = 1;
        }

        // Grab comments if present
        if (entries.length > index + 1) {
            record.optionalDesc = entries[index + 1].trim();
        }

        return record;
    }

    /**
     * Returns the state/purpose of the next line. Decision assisted by state of previous line.
     *
     * @param   line    the line to parse
     * @return          the state/purpose of the next line
     * @since           1.0
     */
    private static byte getCurrentLineStatus(String line) {
        line = line.trim();

        byte status = EMPTY;

        // if just an id and a reference, it's a match record reference (see Tabb example)
        String[] entries = line.split("\\s+");
        if (entries.length == 2 && !entries[0].contains(".")) {
            status = MATCH_REFERENCE;
        } // The header starts with the path to file name
        else if (line.contains(currentFileName)) {
            status = HEADER;
        } /**
         * Find throw-away lines
         */
        else if (line.equals("")) {
            status = EMPTY;
        } // Don't need the legend header
        else if (line.trim().contains("#" + TAB + "Rank/Sp" + TAB + "Id#")) {
            status = EMPTY;
        } // Don't need the legend header
        else if (line.contains("------")) {
            status = EMPTY;
        } /**
         * Find match lines
         */
        // Numbered lines with a / separating two digits
        else if (line.matches("\\d+\\.\\s+\\d+\\s*/\\s*\\d+.*")) {
            status = MATCH;
        } /**
         * Find locus lines
         */
        // Other numbered lines. Don't want matching any file names (.out)
        else if (line.matches("\\d+\\..*") && !line.contains(".out")) {

            // Only if at least two entries in  record
            if (line.matches(".*\\w.*?\\s+\\w.*?.*")) {
                status = LOCUS;
            } else {
                status = HEADER;
            }
        } // Catch all. Headers can be hard to programmatically parse.
        else {
            status = HEADER;
        }

        /**
         * Update previous line status
         */
        previousLineStatus = status;

        if (DEBUG) {

            String statusStr = null;

            switch (status) {
                case EMPTY:
                    statusStr = "E";
                    break;
                case HEADER:
                    statusStr = "H";
                    break;
                case LOCUS:
                    statusStr = "L";
                    break;
                case MATCH:
                    statusStr = "M";
                    break;
                default:
                    statusStr = "ERROR";
                    break;
            }

            System.out.println(statusStr + ": " + line);
        }

        return status;
    }    // Used within getCurrentLineStatus to store previous line status
    private static byte previousLineStatus = EMPTY;

    /**
     * Command-line conversion utility for generating SQT files.
     *
     * @param   args    the command line arguments
     * @since           1.0
     */
    public static void main(String[] args) {

        final String USAGE = NEWLINE + "USAGE: SQTUtil -input [<Sequest out files>] -sqt <Path for output file>" + NEWLINE;

        if (args.length < 3) {
            System.out.println(USAGE);
            return;
        }

        Set<File> outs = new HashSet();
        File sqt = null;

        // Parse args
        for (int i = 0; i < args.length; i++) {

            if (args[i].trim().equalsIgnoreCase("-input")) {

                i++;
                while (i < args.length) {

                    outs.add(new File(args[i].trim()));
                    i++;

                    if (i < args.length && args[i].trim().equalsIgnoreCase("-sqt")) {
                        i--;
                        break;
                    }
                }
            } else if (args[i].trim().equalsIgnoreCase("-sqt")) {
                i++;
                sqt = new File(args[i].trim());
            } else {
                System.err.println(NEWLINE + "ERROR: Unknown input " + args[i]);
                System.out.println(USAGE);
                return;
            }
        }

        if (sqt == null) {
            System.err.println(NEWLINE + "ERROR: Must specify exactly 1 SQT file for output.");
            System.out.println(USAGE);
            return;
        }

        if (outs.size() < 1) {
            System.err.println(NEWLINE + "ERROR: Must specify at least 1 Sequest out file.");
            System.out.println(USAGE);
            return;
        }
        try {
            SQTUtil.createSQT(outs, sqt);
        } catch (Exception ex) {
            System.err.println(NEWLINE + "ERROR: " + ex.getMessage());
            ex.printStackTrace(System.err);
            System.out.println(USAGE);
            return;
        }

        System.out.println(NEWLINE + "Success. SQT file at " + sqt.getAbsolutePath() + NEWLINE);
    }
}

// =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
/**
 * Abstract class representing single record (line) in a SQT file.
 *
 * @since   1.0
 */
abstract class SQTRecord {

    char TYPE;
    public static String TAB = SQTUtil.TAB;
    public static String NEWLINE = SQTUtil.NEWLINE;
} // SQTRecord

// =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
/**
 * Represents a single spectrum record in the SQT file.
 *
 * @since   1.0
 */
class SpectrumRecord extends SQTRecord {

    public String lowScan;
    public String highScan;
    public long charge;
    public long processTime;
    public String server;
    public double obsMass;
    public double totalIntensity;
    public double lowestSp;
    public long sequencesMatch;

    /**
     * @since   1.0
     */
    public SpectrumRecord() {
        TYPE = 'S';
        this.lowScan = null;
        this.highScan = null;
        this.charge = 0;
        this.processTime = 0;
        this.server = null;
        this.obsMass = 0.0;
        this.totalIntensity = 0.0;
        this.lowestSp = 0.0;
        this.sequencesMatch = 0;
    }

    /**
     * Returns true when all required information is gathered. Information is multi-line in out file.
     *
     * @return  <code>true</code> if the required information is gathered;
     *          <code>false</code> otherwise
     * @since   1.0
     */
    public boolean isComplete() {
        return this.server != null && this.sequencesMatch != 0;
    }

    /**
     * @return  the buffer created as a string
     * @since   1.0
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(TYPE + TAB);
        buffer.append(this.lowScan + TAB);
        buffer.append(this.highScan + TAB);
        buffer.append(this.charge + TAB);
        buffer.append(this.processTime + TAB);
        buffer.append(SQTUtil.escapeSpaces(this.server) + TAB);
        buffer.append(this.obsMass + TAB);
        buffer.append(this.totalIntensity + TAB);
        buffer.append(this.lowestSp + TAB);
        buffer.append(this.sequencesMatch + NEWLINE);

        return buffer.toString();
    }
} // SpectrumRecord

// =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
/**
 * Represents a single match record in the SQT file.
 *
 * @since   1.0
 */
class MatchRecord extends SQTRecord {

    public int rankXcorr;
    public int rankSp;
    public double calcMass;
    public double deltCN;
    public double Xcorr;
    public double Sp;
    public int matchedIons;
    public int expectedIons;
    public String sequence;
    public char validationStatus;
    public List<String> references;
    /**
     * Flags used to determine match record type
     */
    public static final byte VERSION_A = 0,  VERSION_B = 1,  VERSION_C = 2;

    /**
     * @since   1.0
     */
    public MatchRecord() {
        TYPE = 'M';
        this.rankXcorr = 0;
        this.rankSp = 0;
        this.calcMass = 0.0;
        this.deltCN = 0.0;
        this.Xcorr = 0.0;
        this.Sp = 0.0;
        this.matchedIons = 0;
        this.expectedIons = 0;
        this.references = new ArrayList();
        this.sequence = null;
        this.validationStatus = 'U';
    }

    /**
     * Different out files have different formats.
     */
    public static byte getMatchRecordVersion(String line) {

        String[] fields = line.split("\\s+");

        // May or may not have an id, which is discarded. If this field is not a
        // decimal point, it is an id.
        if (!fields[4].contains(".")) {
            return VERSION_A;
        } // There is an id. If the eighth field has a decimal, then there is an additional
        // field.
        else if (!fields[8].contains(".")) {
            return VERSION_B;
        } // Has an id, Tabb version
        else {
            return VERSION_C;
        }
    }

    /**
     * @return  the buffer created as a string
     * @since   1.0
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(TYPE + TAB);
        buffer.append(this.rankXcorr + TAB);
        buffer.append(this.rankSp + TAB);
        buffer.append(this.calcMass + TAB);
        buffer.append(this.deltCN + TAB);
        buffer.append(this.Xcorr + TAB);
        buffer.append(this.Sp + TAB);
        buffer.append(this.matchedIons + TAB);
        buffer.append(this.expectedIons + TAB);
        buffer.append(this.sequence + TAB);
        buffer.append(this.validationStatus + NEWLINE);

        return buffer.toString();
    }
} // MatchRecord

// =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
/**
 * Represents a single locus record in the SQT file.
 *
 * @since   1.0
 */
class LocusRecord extends SQTRecord {

    String locus;
    String optionalDesc;

    /**
     * @since   1.0
     */
    public LocusRecord() {
        TYPE = 'L';
        locus = null;
        optionalDesc = null;
    }

    /**
     * @return  the buffer created as a string
     * @since   1.0
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(TYPE + TAB);
        buffer.append(this.locus);

        if (this.optionalDesc != null) {
            buffer.append(TAB + this.optionalDesc);
        }
        buffer.append(NEWLINE);

        return buffer.toString();
    }
} // LocusRecord

// =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
/**
 * Represents entire header in the SQT file.
 *
 * @since   1.0
 */
class Header {

    public static String TAB = SQTUtil.TAB;
    public static String NEWLINE = SQTUtil.NEWLINE;    // Required header fields
    final public String generatorName = SQTUtil.GENERATOR_NAME;
    final public String generatorVersion = SQTUtil.GENERATOR_VERSION;
    public List<String> databases = new ArrayList();
    public Date start,  end;
    public List<String> staticMod = new ArrayList();
    public List<String> dynamicMod = new ArrayList();
    public List<String> diffMod = new ArrayList();
    public String fragmentMasses;
    public String precursorMasses;
    public String enzyme;    // Optional header fields
    public List<String> comments = new ArrayList();
    public long DBSeqLength;
    public long DBLocusCount;
    public String DBMD5Sum; // Only possible if database is present
    public String sortedBy;
    public Map<String, String> algs = new HashMap();

    /**
     * @since   1.0
     */
    public Header() {
        this.start = null;
        this.end = null;
        this.DBSeqLength = 0;
        this.DBLocusCount = 0;
        this.DBMD5Sum = null;
        this.sortedBy = null;
        this.fragmentMasses = null;
        this.precursorMasses = null;
        this.enzyme = null;
    }

    /**
     * @since   1.0
     */
    public void start() {
        this.start = new Date(System.currentTimeMillis());
    }

    /**
     * @since   1.0
     */
    public void stop() {
        this.end = new Date(System.currentTimeMillis());
    }

    /**
     * @return  the buffer created as a string
     * @since   1.0
     */
    public String toString() {

        StringBuffer buffer = new StringBuffer();

        // Generator info
        buffer.append(newLine("SQTGenerator", this.generatorName));
        buffer.append(newLine("SQTGeneratorVersion", this.generatorVersion));

        // Comments
        for (String comment : this.comments) {
            buffer.append(newLine("Comment", comment));        // Timestamps
        }
        if (this.start == null) {
            this.start = new Date(System.currentTimeMillis());
        }
        if (this.end == null) {
            this.end = new Date(System.currentTimeMillis());        // If there's a bad format, just use sql.Date.toString()
        }
        try {

            SimpleDateFormat dformat = new SimpleDateFormat("MM/dd/yyyy, hh:mm aa");

            buffer.append(newLine("StartTime", dformat.format(this.start)));
            buffer.append(newLine("EndTime", dformat.format(this.end)));
        } catch (Exception ex) {
            buffer.append(newLine("StartTime", this.start.toString()));
            buffer.append(newLine("EndTime", this.end.toString()));
        }

        // Database info
        for (String db : this.databases) {
            buffer.append(newLine("Database", SQTUtil.escapeSpaces(db)));
        }
        if (this.DBSeqLength != 0) {
            buffer.append(newLine("DBSeqLength", Long.toString(this.DBSeqLength)));
        }
        if (this.DBLocusCount != 0) {
            buffer.append(newLine("DBLocusCount", Long.toString(this.DBLocusCount)));
        }
        if (this.DBMD5Sum != null) {
            buffer.append(newLine("DBMD5Sum", this.DBMD5Sum));        // Masses
        }
        buffer.append(newLine("PrecursorMasses", this.precursorMasses));
        buffer.append(newLine("FragmentMasses", this.fragmentMasses));

        // Alg-s, which uses map between alg field name and value
        for (String alg : this.algs.keySet()) {
            buffer.append(newLine(alg, this.algs.get(alg)));        // Modifications
        }
        for (String mod : this.staticMod) {
            buffer.append(newLine("StaticMod", mod));
        }
        for (String mod : this.dynamicMod) {
            buffer.append(newLine("DynamicMod", mod));
        }
        for (String mod : this.diffMod) {
            buffer.append(newLine("DiffMod", mod));        // Sorts
        }
        if (this.sortedBy != null) {
            buffer.append(newLine("SortedBy", this.sortedBy));
        }
        if (this.enzyme != null) {
            buffer.append(newLine("EnzymeSpec", this.enzyme));
        }
        return buffer.toString();
    }

    /**
     * @param   name    the name
     * @param   value   the value
     * @return          the new line
     * @since           1.0
     */
    private String newLine(String name, String value) {
        return "H" + TAB + name + TAB + value + NEWLINE;
    }
} // Header

// =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
/**
 * Represents output file. Necessary b/c not input different order than output.
 *
 * @since   1.0
 */
class OutFile {

    public List<MatchRecord> matchRecords;
    public List<LocusRecord> locusRecords;
    public SpectrumRecord spectrumRecord;

    /**
     * @since   1.0
     */
    public OutFile() {
        matchRecords = new ArrayList();
        locusRecords = new ArrayList();
        spectrumRecord = new SpectrumRecord();
    }

    /**
     * Returns last added match record
     */
    public MatchRecord getLastMatchRecord() {
        return this.matchRecords.get(this.matchRecords.size() - 1);
    }

    /**
     * Associates match record with locus records in correct format.
     *
     * @return  the buffer created as a string
     * @since   1.0
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(spectrumRecord.toString());

        for (MatchRecord nextMatch : matchRecords) {

            buffer.append(nextMatch.toString());

            // For each reference, find associated locus records
            for (String reference : nextMatch.references) {

                boolean found = false;

                // Try to find associated locus record
                for (LocusRecord nextLocus : locusRecords) {

                    if (nextLocus.locus.equals(reference)) {
                        buffer.append(nextLocus.toString());
                        found = true;
                        break;
                    }
                }

                // If not found, create a locus
                if (!found) {
                    LocusRecord l = new LocusRecord();
                    l.locus = reference;
                    buffer.append(l.toString());
                }
            }
        }

        return buffer.toString();
    }
} // OutFile
