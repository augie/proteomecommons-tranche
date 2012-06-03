/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.scripts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import junit.framework.TestCase;
import org.tranche.commons.RandomUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;

/**
 * <p>Useful utilities for use when testing.</p>
 *
 * @author Brian Maso
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public class Utils {

    /**
     * <p>
     * Convenience method to read all objects from the input stream. Assumes
     * there is a <i>read()</i> method on this input object. Whatever the read()
     * method returns, this object collects and stores in a List.
     * </p>
     **/
    public static List<Object> typedInputStreamAsList(Object objStream) throws Exception {
        Class cls = objStream.getClass();
        Method mtdRead = cls.getMethod("read", new Class[]{});

        List<Object> list = new LinkedList<Object>();
        for (Object obj; null != (obj = mtdRead.invoke(objStream, new Object[]{}));) {
            list.add(obj);
        }
        return list;
    }

    /**
     *Recursively deletes all the files in the directory.
     */
    public static File recursiveDelete(File dir) {
        // only delete if it exists
        if (dir.exists()) {
            if (dir.isDirectory()) {
                for (String fname : dir.list()) {
                    File ff = new File(dir, fname);
                    recursiveDelete(ff);
                }
            }

            TestCase.assertTrue("Unable to delete " + dir, dir.delete());
        }

        return dir;
    }

    /**
     *<p>Recursively deletes all the files in the directory. If the deletion can't completely finish, show a warning.</p>
     */
    public static File recursiveDeleteWithWarning(File dir) {
        try {
            return recursiveDelete(dir);
        } catch (Throwable e) {
            try {
                System.out.println("Warning! Can't fully delete " + dir.getCanonicalPath());
            } catch (Throwable ex) {
                // noop
            }
        }
        return dir;
    }

    public static byte[] makeRandomData(int length) {
        byte[] data = new byte[length];
        RandomUtil.getBytes(data);
        return data;
    }

    public static File makeTempFile(byte[] data) throws IOException {
        File tempFile = TempFileUtil.createTemporaryFile();
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(tempFile);
            fos.write(data);
        } finally {
            IOUtil.safeClose(fos);
        }
        return tempFile;
    }

    public static void makeTempFile(byte[] data, File file) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(data);
        } finally {
            IOUtil.safeClose(fos);
        }
    }

    public static byte[] makeTempFile(File tempFile) throws IOException {
        // make random data
        byte[] data = makeRandomData(1000);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(tempFile);
            fos.write(data);
        } finally {
            IOUtil.safeClose(fos);
        }
        // return the contents of the file
        return data;
    }

    /**
     * Helper method to make a temporary file with completely random data of a given size.
     */
    public static File makeTempFile(long fileSize) throws IOException {
        File tempFile = File.createTempFile("temp", "data");
        makeTempFile(tempFile, fileSize);
        return tempFile;
    }

    public static void makeTempFile(File file, long fileSize) throws IOException {
        // make the buffer large enough
        int bufferSize = 100000;
        // adjust if it is larger than the file
        if (bufferSize > fileSize) {
            bufferSize = (int) fileSize;
        }
        // make the buffer
        byte[] buf = new byte[bufferSize];

        // make sure the directory exists
        file.getParentFile().mkdirs();

        // write the results to a file
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            for (int i = 0; i < fileSize / buf.length; i++) {
                RandomUtil.getBytes(buf);
                out.write(buf);
            }

            // write out any odd bytes
            buf = new byte[(int) (fileSize % buf.length)];
            RandomUtil.getBytes(buf);
            out.write(buf);

        } finally {
            IOUtil.safeClose(out);
        }
    }
}

