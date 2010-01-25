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

import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import org.proteomecommons.io.GenericPeakListWriter;
import org.proteomecommons.io.PeakListWriterFactory;

/**
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
public class PKFormatDropDown extends JComboBox {

    private List<String> fileExtensions;
    private List<String> fileTypes;

    public PKFormatDropDown(String[] options, List<String> fileExtensions, List<String> fileTypes) {
        super(options);
        setSelectedIndex(0);

        this.fileExtensions = fileExtensions;
        this.fileTypes = fileTypes;
    }

    public String getSelectedFileExtension() {
        return this.fileExtensions.get(this.getSelectedIndex());
    }

    public String getSelectedFileType() {
        return this.fileTypes.get(this.getSelectedIndex());
    }

    /**
     * <p>Returns a drop down menu with known peak list file types.</p>
     */
    public static PKFormatDropDown getInstance() {

        List<String> formats = new ArrayList();
        List<String> fileExtensions = new ArrayList();
        List<String> fileTypes = new ArrayList();

        // Build the lists
        for (PeakListWriterFactory next : GenericPeakListWriter.getRegisteredWriters()) {
            formats.add(next.getName() + " (" + next.getFileExtension() + ")");
            fileExtensions.add(next.getFileExtension());
            fileTypes.add(next.getName());
        }

        return new PKFormatDropDown(formats.toArray(new String[0]), fileExtensions, fileTypes);
    }
}
