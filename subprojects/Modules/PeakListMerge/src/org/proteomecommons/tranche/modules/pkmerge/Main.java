package org.proteomecommons.tranche.modules.pkmerge;

import java.util.HashMap;
import java.util.Map;
import org.tranche.gui.GenericPopupFrame;
import org.tranche.hash.BigHash;
import org.tranche.modules.LeftMenuAnnotation;
import org.tranche.modules.PopupMenuAnnotation;
import org.tranche.modules.TrancheMethodAnnotation;
import org.tranche.modules.TrancheModuleAnnotation;

/**
 * Merges peaklists together using the IO Framework
 * @author besmit
 */
@TrancheModuleAnnotation(name = "Peak List Merger Module",
description = "Merges peaklists together using the IO Framework.")
public class Main {

    @LeftMenuAnnotation(scope = "Files")
    @PopupMenuAnnotation(scope = "Files")
    @TrancheMethodAnnotation(fileExtension = "*",
    mdAnnotation = "Tranche:Peaklist->*",
    selectionMode = "multiple",
    label = "Merge Peak Lists",
    description = "Merges peaklists together using the IO Framework. Select output format.")
    public static void openPeakListMergerGUI(Map<String, BigHash> files) {

        // Convert -> Map<BigHash,String>
        Map<BigHash, String> map = new HashMap();
        for (String key : files.keySet()) {
            map.put(files.get(key), key);
        }
        GenericPopupFrame popup = PopupTaskFrame.getInstance(map);
        popup.setVisible(true);
    }
}
