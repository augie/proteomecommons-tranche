package org.proteomecommons.tranche.modules.fasta;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.HashMap;
import java.util.Map;
import org.tranche.gui.GenericPopupFrame;
import org.tranche.hash.BigHash;
import org.tranche.modules.AdvancedToolsAnnotation;
import org.tranche.modules.LeftMenuAnnotation;
import org.tranche.modules.PopupMenuAnnotation;
import org.tranche.modules.TrancheMethodAnnotation;
import org.tranche.modules.TrancheModuleAnnotation;

/**
 * Module for concatenating, reversing, and converting FASTA files.
 * @author James "Augie" Hill <augman85@gmail.com>
 */
@TrancheModuleAnnotation(name = "FASTA Module", description = "Concatenate a set of FASTA files. Optionally reverse the protein sequences and convert the FASTA format.")
public class Main {

    @LeftMenuAnnotation(scope = "Files")
    @PopupMenuAnnotation(scope = "Files")
    @TrancheMethodAnnotation(fileExtension = ".fasta|.FASTA|.FastA", mdAnnotation = "Tranche:FASTA->*", selectionMode = "any", label = "Open FASTA Module", description = "Concatenate a set of FASTA files. Optionally reverse the protein sequences and convert the FASTA format.")
    public static void openFastAReverserGUI(Map<String, BigHash> files) {
        try {
            Map<BigHash, String> map = new HashMap();
            for (String key : files.keySet()) {
                map.put(files.get(key), key);
            }

            GenericPopupFrame popup = PopupTaskFrame.getInstance(map);
            popup.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @AdvancedToolsAnnotation()
    @TrancheMethodAnnotation(fileExtension = ".fasta|.FASTA|.FastA", mdAnnotation = "Tranche:FASTA->*", selectionMode = "any", label = "Open FASTA Module", description = "Concatenate a set of FASTA files. Optionally reverse the protein sequences and convert the FASTA format.")
    public static void launch() throws Exception {
        try {
            main(new String[]{});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        // Create the GUI
        GenericPopupFrame popup = PopupTaskFrame.getInstance();
        // open in the default platform location
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension d = tk.getScreenSize();
        popup.setLocation((int) (d.getWidth() / 2 - popup.getWidth() / 2), (int) (d.getHeight() / 2 - popup.getHeight() / 2));
        // make visible
        popup.setVisible(true);

    }
}
