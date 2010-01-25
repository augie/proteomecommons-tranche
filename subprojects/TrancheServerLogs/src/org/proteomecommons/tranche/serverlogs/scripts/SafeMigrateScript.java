/*
 * MigrateScript.java
 *
 * Created on January 23, 2008, 4:01 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.proteomecommons.tranche.serverlogs.scripts;

/**
 * <p>Runs all necessary scripts in order! Safe -- only migrates if finds need.</p>
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 */
public class SafeMigrateScript {
    
    /** Creates a new instance of MigrateScript */
    public static void main(String[] args) {
//        System.out.println("Finding oldest timestamp...");
//        FindOldestTimestampScript.main(new String[0]);
        
        System.out.println("Running ip file script...");
        PopulateLogDirsWithIPFilesScript.main(new String[0]);
        
        System.out.println("Running rename script...");
        RenameOldServerLogsScript.main(new String[0]);
        
        System.out.println("Running rebuild cache script...");
        RebuildCacheFilesScript.main(new String[0]);
    }
    
}
