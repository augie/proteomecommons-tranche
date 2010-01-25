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
package org.proteomecommons.tranche.proxy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author James "Augie" Hill - augie@828productions.com
 */
public class ZipUtil {

    private static ZipOutputStream zos = null;
    private static String source = "",  target = "";

    public static void zip(String source, String target) throws Exception {
        ZipUtil.source = source;
        ZipUtil.target = target;

        File sourceFile = new File(source);

        // the file was not found
        if (!sourceFile.isFile() && !sourceFile.isDirectory()) {
            return;
        }

        FileOutputStream fos = new FileOutputStream(target);
        zos = new ZipOutputStream(fos);
        zos.setLevel(9);

        zipFile(sourceFile);

        zos.finish();
        zos.close();
    }

    private static void zipFile(File file) {
        if (file.isDirectory()) {
            if (file.getName().equalsIgnoreCase(".metadata") || file.getName().equalsIgnoreCase("Libraries") || file.getName().equalsIgnoreCase("RepositoryBuild") || file.getName().equalsIgnoreCase("images")) {
                return;
            }
            File list[] = file.listFiles();
            for (int i = 0; i < list.length; i++) {
                zipFile(list[i]);
            }
        } else {
            try {
                String strAbsPath = file.getPath();
                String strZipEntryName = strAbsPath.substring(source.length() + 1, strAbsPath.length());
                byte[] b = new byte[(int) (file.length())];
                ZipEntry cpZipEntry = new ZipEntry(strZipEntryName);
                zos.putNextEntry(cpZipEntry);
                zos.write(b, 0, (int) file.length());
                zos.closeEntry();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void unzip(String sourceZip, String targetDirectory) throws Exception {

        // read the ZIP
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        ZipInputStream zis = null;

        try {
            // make the streams
            fis = new FileInputStream(new File(sourceZip));
            bis = new BufferedInputStream(fis);
            zis = new ZipInputStream(bis);

            // read the entries
            for (ZipEntry ze = zis.getNextEntry(); ze != null; ze = zis.getNextEntry()) {

                // make the file
                File f = new File(targetDirectory + File.separator + ze.getName());
                if (ze.isDirectory()) {
                    f.mkdirs();
                    continue;
                } else {
                    if (f.getParentFile() != null) {
                        f.getParentFile().mkdirs();
                    }
                    f.createNewFile();
                }

                FileOutputStream fos = null;
                BufferedOutputStream bos = null;

                try {
                    fos = new FileOutputStream(f);
                    bos = new BufferedOutputStream(fos);

                    while (zis.available() > 0) {
                        bos.write(zis.read());
                    }
                } finally {
                    try {
                        bos.close();
                        fos.close();
                    } catch (Exception e) {
                    }
                }
            }
        } finally {
            try {
                zis.close();
                bis.close();
                fis.close();
            } catch (Exception e) {
            }
        }
    }
}