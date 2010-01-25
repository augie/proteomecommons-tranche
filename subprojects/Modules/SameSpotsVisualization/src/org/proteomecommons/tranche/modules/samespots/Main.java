package org.proteomecommons.tranche.modules.samespots;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Map;
import javax.swing.JOptionPane;
import org.tranche.get.GetFileTool;
import org.tranche.get.GetFileToolListener;
import org.tranche.gui.ErrorFrame;
import org.tranche.gui.GUIUtil;
import org.tranche.gui.GenericProgressiveTaskPopup;
import org.tranche.hash.BigHash;
import org.tranche.modules.AdvancedToolsAnnotation;
import org.tranche.modules.LeftMenuAnnotation;
import org.tranche.modules.PopupMenuAnnotation;
import org.tranche.modules.RunInstallable;
import org.tranche.modules.TrancheMethodAnnotation;
import org.tranche.modules.TrancheModuleAnnotation;
import org.tranche.util.IOUtil;
import org.tranche.util.OperatingSystem;
import org.tranche.util.OperatingSystemUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.Text;

/**
 * Module for installing, if necessary, SameSpots and visualizing data.
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 */
@TrancheModuleAnnotation(name = "Nonlinear SameSpots Module",description = "Run SameSpots visualization in MS Windows. Downloads and installs software if necessary.")
public class Main implements RunInstallable {
    
    /**
     * The default executable location. Note this may be different than the user's actual location.
     */
    public static final File DEFAULT_EXECUTABLE_LOCATION = new File("/Program Files/Nonlinear Dynamics/Progenesis SameSpots/ProgenesisDiscovery.exe");
    
    /**
     * The hash for the install package (it's on the network).
     */
    public final static BigHash installableHash = BigHash.createHashFromString("LGYt49L2bINjKXH7Wjr30y6UlCTSnuJHcWi75yd5NptVC4ZakikbcqvlH0SoiCFDUvZ/xlCxOeE2gZ/wtBWcwK3+LRUAAAAAC8phYA==");
    
    /**
     * The user's executable. May be different.
     */
    private static File executable = DEFAULT_EXECUTABLE_LOCATION;
    
    /**
     * Files stores the location of the SameSpots executable file perchance non-standard.
     */
    private static final File configFile = new File(GUIUtil.getGUIDirectory(),"samespots-visualization-module.conf");
    
    /**
     * This method is hooked when the JAR is loaded since the class implements RunInstallable.
     */
    public boolean install() throws Exception {
        OperatingSystem os = OperatingSystem.getCurrentOS();
        if (!os.isMSWindows()) {
            JOptionPane.showMessageDialog(
                    GUIUtil.getAdvancedGUI(),
                    "SameSpots is currently only available for Windows. Sorry for the inconvenience.",
                    "MS Windows Required",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        // Attempt to read in. If just installed, will fail...
        try {
            readInCurrentExecutableLocation();
        } catch(Exception ex) { /* expected */ }
        
        // See if already installed
        if (!executable.exists()) {
            
            // Loop until user explicitly bails or install completes
            WHILE_NOT_INSTALL_OR_BAIL: while(true) {
                
                Object[] options = {
                    "Cancel Installation",
                    "Find on Hard Drive",
                    "Download and Install"};
                
                int n = JOptionPane.showOptionDialog(
                        GUIUtil.getAdvancedGUI(),
                        "SameSpots was not found on your hard drive.\nChoose \""+options[2]+"\" to locate "+executable.getName()+"\nNormal installation location is "+executable.getAbsolutePath(),
                        "Download and Install SameSpots?",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[2]);
                
                // I put my custom options in a different order so that install is the far
                // right option.
                
                // User bails
                if (n == JOptionPane.YES_OPTION) {
                    return false;
                }
                
                // User wants to find
                else if (n == JOptionPane.NO_OPTION) {
                    executable = GUIUtil.userSelectFileForRead("Please locate "+executable.getName(),GUIUtil.getAdvancedGUI());
                    if (executable != null) {
                        // Exit loop, user located!
                        break WHILE_NOT_INSTALL_OR_BAIL;
                    }
                    // If null, take back to parent JOptionPane
                }
                
                // User wants to download and install
                else if (n == JOptionPane.CANCEL_OPTION) {
                    File installer = TempFileUtil.createTemporaryFile(".exe");
                    
                    String[] message = {
                        "Downloading intstaller, which is of size "+Text.getFormattedBytes(installableHash.getLength()),
                        "Once the installer is downloaded, the installer will execute and assist you through the installation process.",
                        "It is recommended, though not required, that you accept the default installation location."
                    };
                    
                    GenericProgressiveTaskPopup popup = new GenericProgressiveTaskPopup(
                            "Downloading SameSpots Installer",
                            message);
                    try {
                        
                        // Show task popup and associate w/ listener for progress bar
                        popup.setSizeAndPosition(new Dimension(500,500));
                        popup.setVisible(true);
                        GetFileToolListener l = new DownloadInstallerListener(popup);
                        
                        // Download installer using GFT.
                        GetFileTool gft = new GetFileTool();
                        gft.setHash(Main.installableHash);
                        gft.setValidate(false);
                        gft.addListener(l);
                        gft.getFile(installer);
                        
                        // Close down the task popup
                        popup.setVisible(false);
                        popup.dispose();
                        
                        // Install.
                        int returnCode = OperatingSystemUtil.executeExternalCommand("\""+installer.getAbsolutePath()+"\"");
                        if (returnCode > 0) {
                            throw new Exception("Installer exitted with a return status of "+returnCode+". If this is an error, please let us know.");
                        }
                        
                        // Install succeeded. If in non-standard location, ask user to find.
                        if (!Main.DEFAULT_EXECUTABLE_LOCATION.exists()) {
                            executable = GUIUtil.userSelectFileForRead("Cannot find the installed executable. Please locate "+executable.getName(),GUIUtil.getAdvancedGUI());
                            if (executable != null) {
                                // Exit loop, user located!
                                break WHILE_NOT_INSTALL_OR_BAIL;
                            }
                            // If null, take back to parent JOptionPane
                        }
                        
                    } catch (Exception ex) {
                        ErrorFrame ef = new ErrorFrame();
                        ef.show(ex,GUIUtil.getAdvancedGUI());
                        
                        return false;
                    } finally {
                        IOUtil.safeDelete(installer);
                        
                        // Close down the task popup
                        popup.setVisible(false);
                        popup.dispose();
                    }
                } // Install executable
            } // While not installed or user bails
        } // If SameSpots executable was not already installed
        
        // Save the executable location
        writeOutCurrentExecutableLocation();
        
        return true;
    }
    
    /**
     * Does nothing for now.
     */
    public void uninstall() throws Exception {
        // If there is a config file for module, remove
        if (configFile.exists()) {
            configFile.delete();
        }
    }
    
    /**
     * Writes out executable path to config file.
     */
    private static void writeOutCurrentExecutableLocation() throws Exception {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(Main.configFile));
            out.write(executable.getAbsolutePath());
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }
    
    /**
     * Reads in executable path from config file.
     */
    private static void readInCurrentExecutableLocation() throws Exception {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(Main.configFile));
            String path = in.readLine();
            executable = new File(path);
            System.out.println("SameSpots executable located at: "+executable.getAbsolutePath());
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
    
    @AdvancedToolsAnnotation()
    @LeftMenuAnnotation(scope="Projects|Files")
    @PopupMenuAnnotation(scope="Projects|Files")
    @TrancheMethodAnnotation(fileExtension="*",mdAnnotation="*",selectionMode="none",label="Launch Nonlinear SameSpots",description="Only launches the SameSpot viewer. Does not accept any parameters.")
    public static void launchSameSpotsWithoutArguments() {
        runCommand("\""+executable.getAbsolutePath()+"\"");
    }
    
    @LeftMenuAnnotation(scope="Projects|Files")
    @PopupMenuAnnotation(scope="Projects|Files")
    @TrancheMethodAnnotation(fileExtension="*",mdAnnotation="*",selectionMode="any",label="Open Project in SameSpots",description="Launches the SameSpot viewer with any selected files or projects.")
    public static void launchSameSpots(Map<String,BigHash> files) {
        if (files.size() == 0) {
            launchSameSpotsWithoutArguments();
            return;
        }
        
        JOptionPane.showMessageDialog(
                GUIUtil.getAdvancedGUI(),
                "Currently, SameSpots will not accept any arguments.\nWe hope to support arguments soon.",
                "Option not available",
                JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * May run SameSpots with or without arguments.
     */
    private static void runCommand(String command) {
        
        // Read in current executable location
        try {
            readInCurrentExecutableLocation();
        } catch (Exception ex) {
            System.err.println("Trouble reading in configuration file. Not a fatal exception, but note: "+ex.getMessage());
            ex.printStackTrace(System.err);
        }
        
        // See if file exists
        if (executable == null || !executable.exists()) {
            // Check whether standard location exists
            if (Main.DEFAULT_EXECUTABLE_LOCATION.exists()) {
                executable = Main.DEFAULT_EXECUTABLE_LOCATION;
            }
            
            // Not found, ask user
            else {
                executable = GUIUtil.userSelectFileForRead("Please locate "+executable.getName(),GUIUtil.getAdvancedGUI());
                
                // User bails
                if (executable == null) {
                    JOptionPane.showMessageDialog(
                            GUIUtil.getAdvancedGUI(),
                            "If SameSpots is no longer installed, you have two options:\n1. You can uninstall this Tranche module in the module manager, and reinstall it to be guided through installation\n2. You can install SameSpots yourself and rerun the Tranche tool.",
                            "SameSpots not installed?",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
            }
            
            // Write so doesn't happen again
            try {
                writeOutCurrentExecutableLocation();
            } catch (Exception ex) {
                System.err.println("Trouble writing out configuration file. Not a fatal exception, but note: "+ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }
        
        System.out.println("Launching SameSpots... "+executable.getAbsolutePath());
        try {
            int returnCode = OperatingSystemUtil.executeExternalCommand(command);
            System.out.println("Command returned "+returnCode+" as an exit code.");
        } catch (Exception ex) {
            ErrorFrame ef = new ErrorFrame();
            ef.show(ex,GUIUtil.getAdvancedGUI());
        }
    }
}
