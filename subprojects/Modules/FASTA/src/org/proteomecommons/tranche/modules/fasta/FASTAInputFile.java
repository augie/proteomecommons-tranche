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

/**
 *
 * @author James "Augie" Hill<augman85@gmail.com>
 */
public class FASTAInputFile {
    
    private String location;
    private boolean isTrancheHash = false;
    private FASTAReaderFormatDropDown formatMenu;
    
    public FASTAInputFile(boolean isTrancheHash, String location, FASTAReaderFormatDropDown formatMenu) {
        this.isTrancheHash = isTrancheHash;
        this.location = location;
        this.formatMenu = formatMenu;
    }
    
    public String getLocation() {
        return location;
    }
    
    public FASTAReaderFormatDropDown getFormatMenu() {
        return formatMenu;
    }
    
}
