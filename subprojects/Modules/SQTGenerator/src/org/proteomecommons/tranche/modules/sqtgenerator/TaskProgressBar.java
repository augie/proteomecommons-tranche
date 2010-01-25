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

package org.proteomecommons.tranche.modules.sqtgenerator;

import javax.swing.JProgressBar;

/**
 * <p>Progress bar for any task. Does not use Task, but simply reports percent complete with a status message.</p>
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
public class TaskProgressBar extends JProgressBar {
    
    public TaskProgressBar() {
        
        // No task model, just use 0 to 100 (to represent percent completion)
        super(0,100);
        
        this.setValue(0);
        this.setString("Starting");
        this.setStringPainted(true);
    }
    
    /**
     * <p>Convenient way to update progress bar.</p>
     * @param percent Number b/w 0 and 100 representing percent complete.
     * @param msg String message, should be relevant to stage of progress.
     */
    public void updateStatus(int percent, String msg) {
        this.setValue(percent);
        this.setString(msg);
        this.repaint();
    }
    
    /**
     * <p>Convenient way to update progress bar.</p>
     * @param percent Double between 0.0 and 1.0.
     * @param msg String message, should be relevant to stage of progress.
     */
    public void updateStatus(double percent, String msg) {
        
        int adjustedPercent = (int)(percent * 100);
        
        this.setValue(adjustedPercent);
        this.setString(msg);
        this.repaint();
    }
}
