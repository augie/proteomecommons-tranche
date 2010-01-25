package org.proteomecommons.tranche.modules.advancedsearch;

import javax.swing.JPanel;
import org.tranche.gui.GUIUtil;
import org.tranche.modules.TabAnnotation;
import org.tranche.modules.TrancheMethodAnnotation;
import org.tranche.modules.TrancheModuleAnnotation;

/**
 * Advanced search tab module.
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 */
@TrancheModuleAnnotation(name = "Advanced Search Module", description = "Perform an advanced search on the ProteomeCommons.org database registered of projects. There is an option to limit search results to projects uploaded to the Tranche network.")
public class Main {
    
    static JPanel searchPanel = null;
    
    @TrancheMethodAnnotation(fileExtension="*",mdAnnotation="*",selectionMode="any",label="Search",description="Perform an advanced search on the ProteomeCommons.org database registered of projects. There is an option to limit search results to projects uploaded to the Tranche network.")
    @TabAnnotation(isPlacedInFront = true)
    public static JPanel getPanel() {
        return getSearchPanel();
    }
    
    // Singleton
    private static JPanel getSearchPanel() {
        if (searchPanel == null) {
            searchPanel = new AdvancedSearchPanel(GUIUtil.getAdvancedGUI());
        }
        return searchPanel;
    }
}
