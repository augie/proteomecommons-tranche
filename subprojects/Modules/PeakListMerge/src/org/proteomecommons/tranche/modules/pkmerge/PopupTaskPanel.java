/*
 * PopupTaskPanel.java
 *
 * Created on June 27, 2007, 4:04 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.proteomecommons.tranche.modules.pkmerge;

import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * <p>Parent class for all popup task panels.</p>
 * @author besmit
 */
public abstract class PopupTaskPanel extends JPanel {
    
    public JFrame frame;
    
    /**
     * <p>Preferred dimension. If there is a need, subclass should override.</p>
     */
    public Dimension PREFERRED_DIMENSION = new Dimension(500,500);
    
    /**
     * <p>Set the frame for callbacks.</p>
     */
    public void setFrame(JFrame frame) {
        this.frame = frame;
    }
    
    public abstract void execute() throws Exception;
}
