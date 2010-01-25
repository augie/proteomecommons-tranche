package org.proteomecommons.tranche.modules.pkconvert;

import java.util.HashMap;
import java.util.Map;
import org.tranche.gui.GenericPopupFrame;
import org.tranche.hash.BigHash;
import org.tranche.modules.LeftMenuAnnotation;
import org.tranche.modules.PopupMenuAnnotation;
import org.tranche.modules.TrancheMethodAnnotation;
import org.tranche.modules.TrancheModuleAnnotation;

/**
 * Convert peak list from one format to another using IOFramework
 * @author besmit
 */
@TrancheModuleAnnotation(name = "Peak List Converter Module",
description = "Convert a peak list from one format to another using the IO Framework.")
public class Main {
    // for the tranche gui module
    @LeftMenuAnnotation(scope = "Files")
    @PopupMenuAnnotation(scope = "Files")
    @TrancheMethodAnnotation(fileExtension = "*",
    mdAnnotation = "Tranche:Peaklist->*",
    selectionMode = "single",
    label = "Convert Peak List",
    description = "Convert a peak list from one format to another using the IO Framework.")
    public static void openPKConverterGUI(Map<String, BigHash> files) {
        // Convert -> Map<BigHash,String>
        Map<BigHash, String> map = new HashMap();
        for (String key : files.keySet()) {
            map.put(files.get(key), key);
        }
        GenericPopupFrame popup = PopupTaskFrame.getInstance(map);
        popup.setVisible(true);
    }
}
