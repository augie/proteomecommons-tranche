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

package org.proteomecommons.tranche.modules.pkmerge;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.proteomecommons.io.util.ConvertPeakList;

/**
 * <p>Merges multiple peak lists to a single peak list of chosen format.</p>
 *
 * @author  Bryan Smith - bryanesmith@gmail.com
 * @version %I%, %G%
 * @since   1.0
 */
public class PKMergeTask {
    
    private List<File> files;
    private File output;
    
    // Used to mark that a task is running
    private boolean isRunning;
    private double percent;
    
    /**
     * @param   files   the list of files received 
     * @param   output  the output file received
     * @since           1.0 
     */
    public PKMergeTask(List<File> files, File output) {
        this.files = files;
        this.output = output;
        this.isRunning = false;
        this.percent = 0.0;
    }
    
    /**
     * @throws  Exception   if any exception occurs
     * @since               1.0 
     */
    public void execute() throws Exception {
        this.isRunning = true;
        
        try {
            List<String> filenames = new ArrayList();
            for (File file : files)
                filenames.add(file.getAbsolutePath());
            
            ConvertPeakList cpl = new ConvertPeakList();
            cpl.mergePeakLists(filenames.toArray(new String[0]), this.output.getAbsolutePath());
        }
        
        // Catch any errors, rethrow as exception
        catch (Error t) {
            throw new Exception(t.getMessage());
        }
        
        finally {
            this.isRunning = false;
            this.percent = 100.0;
        }
    }
    
    /**
     * @return  <code>true</code> if the current task is running;
     *          <code>false</code> otherwise
     * @since   1.0 
     */
    public boolean isRunning() {
        return this.isRunning;
    }
    
    /**
     * @return  the percent of the current task completed
     * @since   1.0 
     */
    public double getPercentComplete() {
        return this.percent;
    }
}
