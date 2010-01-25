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

package org.proteomecommons.tranche.modules.fasta;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import org.proteomecommons.io.fasta.AbstractFASTAWriter;
import org.proteomecommons.io.fasta.FASTAWriterFactory;

/**
 *
 * @author James "Augie" Hill<augman85@gmail.com>
 */
public class FASTAWriterFormatDropDown extends JComboBox {
    
    private List<String> fileExtensions, fileTypes;
    private List<FASTAWriterFactory> writerFactories;
    
    public FASTAWriterFormatDropDown(String[] options, List<String> fileExtensions, List<String> fileTypes, List<FASTAWriterFactory> writerFactories) {
        super(options);
        this.fileExtensions = fileExtensions;
        this.fileTypes = fileTypes;
        this.writerFactories = writerFactories;
    }
    
    public String getSelectedFileExtension() {
        return fileExtensions.get(getSelectedIndex());
    }
    
    public String getSelectedFileType() {
        return fileTypes.get(getSelectedIndex());
    }
    
    public String getSelectedFormat() {
        return (String) getSelectedItem();
    }
    
    public FASTAWriterFactory getSelectedWriterFactory() {
        return writerFactories.get(getSelectedIndex());
    }
    
    /**
     * <p>Returns a drop down menu with known FASTA file types.</p>
     */
    public static FASTAWriterFormatDropDown getInstance() {
        
        List<String> formats = new ArrayList();
        List<String> fileExtensions = new ArrayList();
        List<String> fileTypes = new ArrayList();
        List<FASTAWriterFactory> writerFactories = new ArrayList();
        
        // Build the lists
        for (FASTAWriterFactory next : AbstractFASTAWriter.getRegisteredWriters()) {
            formats.add(next.getName() + " (" + next.getFileExtension() + ")");
            fileExtensions.add(next.getFileExtension());
            fileTypes.add(next.getName());
            writerFactories.add(next);
        }
        
        return new FASTAWriterFormatDropDown(formats.toArray(new String[0]), fileExtensions, fileTypes, writerFactories);
    }
}
