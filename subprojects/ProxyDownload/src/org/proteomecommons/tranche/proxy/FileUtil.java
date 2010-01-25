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

package org.proteomecommons.tranche.proxy;

import java.io.File;

/**
 *
 * @author James "Augie" Hill - augie@828productions.com
 */
public class FileUtil {
    
    public static File recursiveDelete(File dir) {
        try {
            // only delete if it exists
            if(dir.exists()) {
                if(dir.isDirectory()) {
                    for(String fname : dir.list()) {
                        File ff = new File(dir, fname);
                        recursiveDelete(ff);
                    }
                }
                
                if (!dir.delete()) {
                    if (dir.exists()) {
                        throw new RuntimeException("Can't delete "+dir);
                    }
                }
            }
        } catch (Exception e) {
            // do nothing
        }
        
        return dir;
    }
    
}
