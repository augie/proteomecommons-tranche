/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.scripts;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import junit.framework.TestCase;
import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.tranche.commons.TextUtil;
import org.tranche.flatfile.DataBlockUtil;
import org.tranche.flatfile.DataDirectoryConfiguration;
import org.tranche.hash.BigHash;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.TestUtil;

/**
 *
 * @author bryan
 */
public class MergeDDCsScriptTest extends TestCase {

    private static final boolean isVerbose = false;

    public MergeDDCsScriptTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    private static final int BASELINE_DATA = 50,
            BASELINE_META_DATA = 5;

    /**
     * Test of main method, of class MergeDDCsScript. Approximately 140 MB for input and output directories combined.
     */
    public void testMainSmall() throws Exception {
        final int data = BASELINE_DATA * 1, meta = BASELINE_META_DATA * 1;
        final int Di = data, Do = data, Dio = data;
        final int Mi = meta, Mo = meta, Mio = meta;
        System.out.println(">>>>>>>>>>>>>>>>>>>>> testMainSmall <<<<<<<<<<<<<<<<<<<<<<");
        runDDCTest(Di, Do, Dio, Mi, Mo, Mio);
    }
    
    /**
     * Test of main method, of class MergeDDCsScript. Approximately 1.2 GB for input and output directories combined.
     */
    public void testMainMedium() throws Exception {
        final int data = BASELINE_DATA * 10, meta = BASELINE_META_DATA * 10;
        final int Di = data, Do = data, Dio = data;
        final int Mi = meta, Mo = meta, Mio = meta;
        System.out.println(">>>>>>>>>>>>>>>>>>>>> testMainMedium <<<<<<<<<<<<<<<<<<<<<<");
        runDDCTest(Di, Do, Dio, Mi, Mo, Mio);
    }
    
    /**
     * Test of main method, of class MergeDDCsScript. Approximately 12.0 GB for input and output directories combined.
     */
    public void testMainLarge() throws Exception {
        final int data = BASELINE_DATA * 100, meta = BASELINE_META_DATA * 100;
        final int Di = data, Do = data, Dio = data;
        final int Mi = meta, Mo = meta, Mio = meta;
        System.out.println(">>>>>>>>>>>>>>>>>>>>> testMainLarge <<<<<<<<<<<<<<<<<<<<<<");
        runDDCTest(Di, Do, Dio, Mi, Mo, Mio);
    }
    
    /**
     * Test of main method, of class MergeDDCsScript. Approximately ____ for input and output directories combined.
     */
    public void testMainLarger() throws Exception {
        final int data = BASELINE_DATA * 250, meta = BASELINE_META_DATA * 100;
        final int Di = data, Do = data, Dio = data;
        final int Mi = meta, Mo = meta, Mio = meta;
        System.out.println(">>>>>>>>>>>>>>>>>>>>> testMainLarge <<<<<<<<<<<<<<<<<<<<<<");
        runDDCTest(Di, Do, Dio, Mi, Mo, Mio);
    }
    
    
//    /**
//     * Test of main method, of class MergeDDCsScript. Approximately ____.
//     */
//    public void testMainHuge() throws Exception {
//        final int data = BASELINE_DATA * 100, meta = BASELINE_META_DATA * 100;
//        final int Di = data, Do = data, Dio = data;
//        final int Mi = meta, Mo = meta, Mio = meta;
//        System.out.println(">>>>>>>>>>>>>>>>>>>>> testMainLarge <<<<<<<<<<<<<<<<<<<<<<");
//        runDDCTest(Di, Do, Dio, Mi, Mo, Mio);
//    }

    /**
     * - Di is the number of data chunks to add to just the input directories
     * - Do is the number of data chunks to add to just the output directories
     * - Dio is the number of data chunks to add to both the input and output directories.
     * 
     * So the input directories will start with Di + Dio chunks, and the output directories
     * will start with Do + Dio chunks.
     * 
     * After the merge, the input directories should still have Di + Dio chunks, but the
     * output directories will now have Di + Dio + Do chunks.
     * 
     * (Mi, Mo & Mio are the same, but for meta data chunks.)
     */
    private void runDDCTest(final int Di, final int Do, final int Dio, final int Mi, final int Mo, final int Mio) throws Exception {

        // The specific network shouldn't matter (since no network I/O),
        // but include perchance some aspect of code assumes load was
        // invokes (i.e., avoid npe)
        ProteomeCommonsTrancheConfig.load();

        /**
         * Input directories will hold data to merge, and output directories will
         * be where the data is merged. Note there will be pre-existing data in 
         * the output directories.
         */
        final Set<File> inputDirs = new HashSet(),
                outputDirs = new HashSet();

        final Random random = new Random();

        final int inputDirCount = random.nextInt(3) + 1, outputDirCount = random.nextInt(3) + 1;

        assertTrue("Should be between 1 & 3 input directories, instead found " + inputDirCount, inputDirCount >= 1 && inputDirCount <= 3);
        assertTrue("Should be between 1 & 3 output directories, instead found " + outputDirCount, outputDirCount >= 1 && outputDirCount <= 3);

        final Set<BigHash> DiHashes = new HashSet(), DoHashes = new HashSet(), DioHashes = new HashSet(), DallHashes = new HashSet();
        final Set<BigHash> MiHashes = new HashSet(), MoHashes = new HashSet(), MioHashes = new HashSet(), MallHashes = new HashSet();

        DataBlockUtil inputDBU = null, outputDBU = null;
        final Set<DataBlockUtil> bothDBUs = new HashSet();

        try {

            // ===== Step: create the directories
            for (int i = 0; i < inputDirCount; i++) {
                inputDirs.add(TempFileUtil.createTemporaryDirectory());
            }

            for (int i = 0; i < outputDirCount; i++) {
                outputDirs.add(TempFileUtil.createTemporaryDirectory());
            }

            assertEquals("Expecting certain number of input directories", inputDirCount, inputDirs.size());
            assertEquals("Expecting certain number of output directories", outputDirCount, outputDirs.size());

            say("Input directories:");
            for (File f : inputDirs) {
                say("    - " + f.getAbsolutePath());
            }
            say("Output directories:");
            for (File f : outputDirs) {
                say("    - " + f.getAbsolutePath());
            }

            // ===== STEP: Create the DataBlockUtils
            inputDBU = new DataBlockUtil();
            outputDBU = new DataBlockUtil();

            bothDBUs.add(inputDBU);
            bothDBUs.add(outputDBU);

            for (File dir : inputDirs) {
                DataDirectoryConfiguration ddc = new DataDirectoryConfiguration(dir.getAbsolutePath(), Long.MAX_VALUE);
                inputDBU.add(ddc);
            }
            for (File dir : outputDirs) {
                DataDirectoryConfiguration ddc = new DataDirectoryConfiguration(dir.getAbsolutePath(), Long.MAX_VALUE);
                outputDBU.add(ddc);
            }

            assertEquals("Expecting certain number of input directories", inputDirCount, inputDBU.getDataDirectoryConfigurations().size());
            assertEquals("Expecting certain number of output directories", outputDirCount, outputDBU.getDataDirectoryConfigurations().size());

            // ===== Step: create and add data
            say("Adding " + Di + " data chunks to input directory set.");
            DiHashes.addAll(addDataChunks(Di, inputDBU, DallHashes));

            say("Adding " + Do + " data chunks to output directory set.");
            DoHashes.addAll(addDataChunks(Do, outputDBU, DallHashes));

            say("Adding " + Dio + " data chunks to input & output directory sets.");
            DioHashes.addAll(addDataChunks(Dio, bothDBUs, DallHashes));

            say("Done adding data chunks.");

            // Assert have correct number of hashes
            assertEquals("Expecting certain number of hashes.", Di, DiHashes.size());
            assertEquals("Expecting certain number of hashes.", Do, DoHashes.size());
            assertEquals("Expecting certain number of hashes.", Dio, DioHashes.size());

            // Assert have correct number of chunks in directories
            assertEquals("Expecting certain number of chunks.", Di + Dio, inputDBU.dataHashes.size());
            assertEquals("Expecting certain number of chunks.", Do + Dio, outputDBU.dataHashes.size());

            // ===== Step: create and add meta data
            say("Adding " + Mi + " meta data chunks to input directory set.");
            MiHashes.addAll(addMetaDataChunks(Mi, inputDBU, MallHashes));

            say("Adding " + Mo + " meta data chunks to output directory set.");
            MoHashes.addAll(addMetaDataChunks(Mo, outputDBU, MallHashes));

            say("Adding " + Mio + " meta data chunks to output directory set.");
            MioHashes.addAll(addMetaDataChunks(Mio, bothDBUs, MallHashes));

            say("Done adding meta data chunks.");

            // Assert have correct number of hashes
            assertEquals("Expecting certain number of hashes.", Mi, MiHashes.size());
            assertEquals("Expecting certain number of hashes.", Mo, MoHashes.size());
            assertEquals("Expecting certain number of hashes.", Mio, MioHashes.size());

            // Assert have correct number of chunks in directories
            assertEquals("Expecting certain number of chunks.", Mi + Mio, inputDBU.metaDataHashes.size());
            assertEquals("Expecting certain number of chunks.", Mo + Mio, outputDBU.metaDataHashes.size());

            // Done adding chunks - let's see the data
            if (isVerbose && false) {
                say("Input directories:");
                for (File f : inputDirs) {
                    TestUtil.printRecursiveDirectoryStructure(f);
                }
                say("Output directories:");
                for (File f : outputDirs) {
                    TestUtil.printRecursiveDirectoryStructure(f);
                }
            }

            // ===== Step: verify the directories have the correct chunks
            for (BigHash h : DiHashes) {
                assertTrue("Expecting has hash.", inputDBU.hasData(h));
                assertFalse("Should not have hash.", outputDBU.hasData(h));
            }

            for (BigHash h : DoHashes) {
                assertFalse("Should not have hash.", inputDBU.hasData(h));
                assertTrue("Expecting has hash.", outputDBU.hasData(h));
            }

            for (BigHash h : DioHashes) {
                assertTrue("Expecting has hash.", inputDBU.hasData(h));
                assertTrue("Expecting has hash.", outputDBU.hasData(h));
            }

            for (BigHash h : MiHashes) {
                assertTrue("Expecting has hash.", inputDBU.hasMetaData(h));
                assertFalse("Should not have hash.", outputDBU.hasData(h));
            }

            for (BigHash h : MoHashes) {
                assertFalse("Should not have hash.", inputDBU.hasData(h));
                assertTrue("Expecting has hash.", outputDBU.hasMetaData(h));
            }

            for (BigHash h : MioHashes) {
                assertTrue("Expecting has hash.", inputDBU.hasMetaData(h));
                assertTrue("Expecting has hash.", outputDBU.hasMetaData(h));
            }
            
            // ===== Print sizes of directories
            Map<File, Long> directorySizes = new HashMap();
            long input = 0, output = 0;
            for (File f : inputDirs) {
                long size = getSizeInBytes(f);
                directorySizes.put(f, size);
                input += size;
            }
            for (File f : outputDirs) {
                long size = getSizeInBytes(f);
                directorySizes.put(f, size);
                output += size;
            }
            
            System.out.println("Total size of directories: "+TextUtil.formatBytes(input+output));
            System.out.println("Input directories ["+TextUtil.formatBytes(input)+"]: ");
            for ( File f : inputDirs) {
                long size = directorySizes.get(f);
                System.out.println("    - "+f.getAbsolutePath()+" ["+TextUtil.formatBytes(size)+"]");
            }
            
            System.out.println("Output directories ["+TextUtil.formatBytes(output)+"]: ");
            for ( File f : outputDirs) {
                long size = directorySizes.get(f);
                System.out.println("    - "+f.getAbsolutePath()+" ["+TextUtil.formatBytes(size)+"]");
            }
            
            // ===== Step: run tool
            final String[] args = new String[2 * (inputDirCount + outputDirCount)];

            {
                int index = 0;
                for (File d : inputDirs) {
                    args[index++] = "-i";
                    args[index++] = d.getAbsolutePath();
                }
                for (File d : outputDirs) {
                    args[index++] = "-o";
                    args[index++] = d.getAbsolutePath();
                }

                String argsStr = "";
                for (String a : args) {
                    argsStr += a + ' ';
                }
                say("Running merge tool with following args: " + argsStr);
            }

            for (int i = 0; i < args.length; i++) {
                assertNotNull("arg[" + i + "] should not be null", args[i]);
            }

            MergeDDCsScript.main(args);

            // ===== Reload the DBU since changed by another process
            
            /**
             * For a normal Tranche server, on startup, the ProjectFindingThread locates all
             * data and meta data chunks, and adds to dbu.dataHashes and dbu.metaDataHashes,
             * respectively. As chunks are added or deleted, these collections are modified 
             * accordingly.
             * 
             * However, we updated the disk in a different "process" (same process, but
             * different DBU, so might as well be different process). As a result, we need
             * to find which chunks have been added, and add them to the respective 
             * collections.
             * 
             * Ideally, the DataBlockUtil would have a member function that did this for
             * us. 
             */
            for (BigHash h : DallHashes) {
                for (DataBlockUtil dbu : bothDBUs) {
                    if ( dbu.hasData(h) && !dbu.dataHashes.contains(h)) {
                        dbu.dataHashes.add(h);
                    }
                }
            }
            
            for (BigHash h : MallHashes) {
                for (DataBlockUtil dbu : bothDBUs) {
                    if ( dbu.hasMetaData(h) && !dbu.metaDataHashes.contains(h)) {
                        dbu.metaDataHashes.add(h);
                    }
                }
            }
            
            // ===== Step: verify the directories have the correct chunks
            assertEquals("Expecting certain number of chunks.", Di + Dio, inputDBU.dataHashes.size());
            assertEquals("Expecting certain number of chunks.", Di + Do + Dio, outputDBU.dataHashes.size());

            assertEquals("Expecting certain number of chunks.", Mi + Mio, inputDBU.metaDataHashes.size());
            assertEquals("Expecting certain number of chunks.", Mi + Mo + Mio, outputDBU.metaDataHashes.size());

            for (BigHash h : DiHashes) {
                assertTrue("Expecting has hash.", inputDBU.hasData(h));
                assertTrue("Expecting has hash.", outputDBU.hasData(h));
            }

            for (BigHash h : DoHashes) {
                assertFalse("Should not have hash.", inputDBU.hasData(h));
                assertTrue("Expecting has hash.", outputDBU.hasData(h));
            }

            for (BigHash h : DioHashes) {
                assertTrue("Expecting has hash.", inputDBU.hasData(h));
                assertTrue("Expecting has hash.", outputDBU.hasData(h));
            }

            for (BigHash h : MiHashes) {
                assertTrue("Expecting has hash.", inputDBU.hasMetaData(h));
                assertTrue("Expecting has hash.", outputDBU.hasMetaData(h));
            }

            for (BigHash h : MoHashes) {
                assertFalse("Should not have hash.", inputDBU.hasMetaData(h));
                assertTrue("Expecting has hash.", outputDBU.hasMetaData(h));
            }

            for (BigHash h : MioHashes) {
                assertTrue("Expecting has hash.", inputDBU.hasMetaData(h));
                assertTrue("Expecting has hash.", outputDBU.hasMetaData(h));
            }

        } finally {

            IOUtil.safeClose(inputDBU);
            IOUtil.safeClose(outputDBU);

            Set<File> allDirs = new HashSet();
            allDirs.addAll(inputDirs);
            allDirs.addAll(outputDirs);
            for (File f : allDirs) {
                IOUtil.recursiveDeleteWithWarning(f);
            }
        }
    }

    /**
     * 
     * @param count
     * @param dbu 
     */
    private static Set<BigHash> addDataChunks(int count, DataBlockUtil dbu, Set<BigHash> allHashes) throws Exception {
        Set<DataBlockUtil> dbus = new HashSet();
        dbus.add(dbu);
        return addDataChunks(count, dbus, allHashes);
    }

    /**
     * 
     * @param count
     * @param dbus 
     */
    private static Set<BigHash> addDataChunks(int count, Set<DataBlockUtil> dbus, Set<BigHash> allHashes) throws Exception {
        return addChunks(count, dbus, false, allHashes);
    }

    /**
     * 
     * @param count
     * @param dbu 
     */
    private static Set<BigHash> addMetaDataChunks(int count, DataBlockUtil dbu, Set<BigHash> allHashes) throws Exception {
        Set<DataBlockUtil> dbus = new HashSet();
        dbus.add(dbu);
        return addMetaDataChunks(count, dbus, allHashes);
    }

    /**
     * 
     * @param count
     * @param dbus 
     */
    private static Set<BigHash> addMetaDataChunks(int count, Set<DataBlockUtil> dbus, Set<BigHash> allHashes) throws Exception {
        return addChunks(count, dbus, true, allHashes);
    }

    /**
     * 
     * @param count
     * @param dbus
     * @param isMetaData
     * @return
     * @throws Exception 
     */
    private static Set<BigHash> addChunks(int count, Set<DataBlockUtil> dbus, boolean isMetaData, Set<BigHash> allHashes) throws Exception {
        Set<BigHash> hashes = new HashSet();
        for (int i = 0; i < count; i++) {

            byte[] chunk = null;

            if (isMetaData) {
                chunk = DevUtil.createRandomBigMetaDataChunk();
            } else {
                chunk = DevUtil.createRandomDataChunkVariableSize();
            }

            final BigHash hash = new BigHash(chunk);

            if (allHashes.contains(hash)) { // Don't add if ever added
                i--;
                continue;
            }

            for (DataBlockUtil dbu : dbus) {
                if (isMetaData) {
                    dbu.addMetaData(hash, chunk);
                } else {
                    dbu.addData(hash, chunk);
                }
            }

            hashes.add(hash);
            allHashes.add(hash); // Add to collection of all hashes

            if (i > 0 && i % 100 == 0) { // Status report
                say("    ... added " + i + " " + (isMetaData ? "meta data" : "data")
                        + " chunks.");
            }
        }
        assertEquals("Expecting add certain number of chunks.", count, hashes.size());
        return hashes; // Return hashes just added
    }

    private static void say(String msg) {
        if (isVerbose) {
            System.out.println(msg);
        }
    }
    
    /**
     * 
     * @param f
     * @return 
     */
    private static long getSizeInBytes(File f) {
        if (f.isDirectory()) {
            long size = 0;
            for (File n : f.listFiles()) {
                size += getSizeInBytes(n);
            }
            return size;
        } else {
            return f.length();
        }
    }
}
