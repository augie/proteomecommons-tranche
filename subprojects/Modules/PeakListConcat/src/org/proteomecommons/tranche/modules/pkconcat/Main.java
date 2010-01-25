package org.proteomecommons.tranche.modules.pkconcat;

import java.util.HashMap;
import java.util.Map;
import org.tranche.gui.GenericPopupFrame;
import org.tranche.hash.BigHash;
import org.tranche.modules.LeftMenuAnnotation;
import org.tranche.modules.PopupMenuAnnotation;
import org.tranche.modules.TrancheMethodAnnotation;
import org.tranche.modules.TrancheModuleAnnotation;

/**
 * Module for concatenating peak lists.
 * @author besmit
 */
@TrancheModuleAnnotation(name = "Peak List Concatenator Module",
description = "Concatenate multiple peak lists together without attempting to convert.")
public class Main {

    @LeftMenuAnnotation(scope = "Files")
    @PopupMenuAnnotation(scope = "Files")
    @TrancheMethodAnnotation(fileExtension = "*",
    mdAnnotation = "Tranche:Peaklist->*",
    selectionMode = "multiple",
    label = "Concatenate Peak Lists",
    description = "Concatenate multiple peak lists together without attempting to convert.")
    public static void openPeakListConcatenatorGUI(Map<String, BigHash> files) {

        // Requires Map<BigHash,String>
        Map<BigHash, String> map = new HashMap();
        for (String key : files.keySet()) {
            map.put(files.get(key), key);
        }
        GenericPopupFrame popup = PopupTaskFrame.getInstance(map);
        popup.setVisible(true);
    }
}
