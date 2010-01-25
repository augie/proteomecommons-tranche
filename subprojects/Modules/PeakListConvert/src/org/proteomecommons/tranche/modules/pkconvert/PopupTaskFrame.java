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
package org.proteomecommons.tranche.modules.pkconvert;

import java.util.Map;
import javax.swing.JFrame;
import org.tranche.gui.GenericPopupFrame;
import org.tranche.hash.BigHash;

/**
 * <p>A popup frame for peaklist (and SQT) tasks.</p>
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
public class PopupTaskFrame extends GenericPopupFrame {

    private PopupTaskPanelWrapper wrapper;

    /**
     * <p>Returns a popup task frame.</p>
     * @param flag A byte flag specified in the class.
     * @param inputs Collection of files to download. Must be 1 or more. The values (String) are the file names.
     * @return A PopupTaskFrame instance.
     */
    public static PopupTaskFrame getInstance(Map<BigHash, String> inputs) {

        return new PopupTaskFrame(new PopupTaskPanelWrapper(inputs));
    }

    /**
     * <p>Better to not invoke directly, use getInstance(...)</p>
     */
    public PopupTaskFrame(PopupTaskPanelWrapper wrapper) {

        // Create the generic popup frame instance
        super(wrapper.title, wrapper.panel);

        super.setSizeAndPosition(wrapper.panel.PREFERRED_DIMENSION);

        // Call-backs
        this.wrapper = wrapper;
        this.wrapper.setFrame(PopupTaskFrame.this);

    } // Constructor
} // PopupTaskFrame

// =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
/**
 * A panel that encapsulates all panels used by popup task frame.
 */
class PopupTaskPanelWrapper {

    public PopupTaskPanel panel;
    public String title;
    public String description;
    public byte flag;
    public Map<BigHash, String> inputs;
    private JFrame frame;

    public PopupTaskPanelWrapper(Map<BigHash, String> inputs) {
        this.flag = flag;
        this.inputs = inputs;
        this.title = "Convert Peak List";
        this.description = "Convert a single peak list from one format to another.";
        this.panel = new PKConvertPanel(description, inputs);
    }

    public void setFrame(JFrame frame) {
        this.frame = frame;
        this.panel.setFrame(frame);
    }
}
