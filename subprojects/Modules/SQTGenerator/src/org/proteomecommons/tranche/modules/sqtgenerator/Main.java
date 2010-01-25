package org.proteomecommons.tranche.modules.sqtgenerator;

import java.util.HashMap;
import java.util.Map;
import org.tranche.hash.BigHash;
import org.tranche.modules.LeftMenuAnnotation;
import org.tranche.modules.PopupMenuAnnotation;
import org.tranche.modules.TrancheMethodAnnotation;
import org.tranche.modules.TrancheModuleAnnotation;

/**
 * Generate SQT file.
 * @author besmit
 */
@TrancheModuleAnnotation(name = "SQT File Generator",
description = "Generate a SQT file from one or more Sequest output (*.out) files.")
public class Main {

    @LeftMenuAnnotation(scope = "Files")
    @PopupMenuAnnotation(scope = "Files")
    @TrancheMethodAnnotation(fileExtension = "out|tar|gz|zip|tgz|gzip",
    mdAnnotation = "*",
    selectionMode = "any",
    label = "Generate SQT File",
    description = "Generate a SQT file from one or more Sequest output (*.out) files.")
    public static void openSQTGeneratorGUI(Map<String, BigHash> files) {

        // Need Map<BigHash,String>
        Map<BigHash, String> map = new HashMap();
        String filename;
        for (String key : files.keySet()) {
            filename = key;
            if (filename.contains("/")) {
                filename = filename.substring(filename.lastIndexOf("/") + 1);
            }
            map.put(files.get(key), filename);
        }

        PopupTaskFrame.getInstance(map).setVisible(true);
    }
}
