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

package org.proteomecommons.tranche.modules.pkviewer;

import javax.swing.JPanel;
import org.tranche.gui.GUIUtil;
import org.tranche.gui.GenericPopupFrame;
import org.tranche.modules.LeftMenuAnnotation;
import org.tranche.modules.PopupMenuAnnotation;
import org.tranche.modules.TrancheMethodAnnotation;
import org.tranche.modules.TrancheModuleAnnotation;

/**
 *
 * @author James "Augie" Hill - augie@umich.edu
 */
@TrancheModuleAnnotation(name = "Peak List Viewer Module", description = "")
public class Main {
    
    @LeftMenuAnnotation (
      scope="Files"
    )
    @PopupMenuAnnotation (
      scope="Files"
    )
    @TrancheMethodAnnotation( fileExtension="*", mdAnnotation="Tranche:Peaklist->*", selectionMode="single", label="View Peak List", description="" )
    public static void openPeakListViewer() {
        GenericPopupFrame popup = new GenericPopupFrame("Peak List Viewer", new JPanel());
        popup.setLocationRelativeTo(GUIUtil.getAdvancedGUI());
        popup.setVisible(true);
    }
    
}