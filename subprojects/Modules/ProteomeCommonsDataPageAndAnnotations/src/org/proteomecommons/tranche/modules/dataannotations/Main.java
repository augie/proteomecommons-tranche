/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.proteomecommons.tranche.modules.dataannotations;

import java.lang.reflect.Method;
import org.tranche.gui.GUIUtil;
import org.tranche.hash.BigHash;
import org.tranche.modules.LeftMenuAnnotation;
import org.tranche.modules.PopupMenuAnnotation;
import org.tranche.modules.TrancheMethodAnnotation;
import org.tranche.modules.TrancheModuleAnnotation;

/**
 * <p>Functionality to tie Tranche GUI to ProteomeCommons.org</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
@TrancheModuleAnnotation(name = "ProteomeCommons.org Data and Annotations Module", description = "Allows user to quickly find data page for data, or annotate data. Launches page from browser.")
public class Main {
    
    @LeftMenuAnnotation(scope = "Projects")
    @PopupMenuAnnotation(scope = "Projects")
    @TrancheMethodAnnotation(fileExtension = "*",
    mdAnnotation = "*",
    selectionMode = "single",
    label = "View Project Page & Annotations",
    description = "View ProteomeCommons.org data page for project, including any existing annotations.")
    public static void viewDataPage(BigHash projectHash) {
        String URL = "https://proteomecommons.org/dataset.jsp?id="+GUIUtil.createURLEncodeBase16Hash(projectHash);
        displayURL(URL);
    }
    
    @LeftMenuAnnotation(scope = "Projects")
    @PopupMenuAnnotation(scope = "Projects")
    @TrancheMethodAnnotation(fileExtension = "*",
    mdAnnotation = "*",
    selectionMode = "single",
    label = "Edit Annotations",
    description = "Visit ProteomeCommons.org data page to annotate data set.")
    public static void editAnnotations(BigHash projectHash) {
        String URL = "https://proteomecommons.org/dataset.jsp?id="+GUIUtil.createURLEncodeBase16Hash(projectHash)+"&action=annotate";
        displayURL(URL);
    }
    
    /**
     * Display a file in the system browser.  If you want to display a
     * file, you must include the absolute path name.
     *
     * @param url the file's url (the url must start with either "http://" or "file://").
     */
    static public void displayURL(String url) {

        // make sure the URL is safe
        url = GUIUtil.createSafeURL(url);

        String osName = null;

        try {

            osName = System.getProperty("os.name");

            System.out.println("Going to launch browser: " + url);

            if (osName.startsWith("Mac OS")) {
                Class fileMgr = Class.forName("com.apple.eio.FileManager");
                Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[]{String.class});
                openURL.invoke(null, new Object[]{url});
            } else if (osName.startsWith("Windows")) {
                // if the url starts with "file://", make sure it actually starts with "file:///"
                if (url.toLowerCase().startsWith("file://") && !url.toLowerCase().startsWith("file:///")) {
                    url.replaceFirst("file://", "file:///");
                }
                Process process = Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
                int returnVal = process.waitFor();
                if (returnVal != 0) {
                    throw new Exception("non-zero runtime return value: "+returnVal);
                }
            } else { //assume Unix or Linux
                String[] browsers = {"firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape"};
                String browser = null;
                for (int count = 0; count < browsers.length && browser == null; count++) {
                    if (Runtime.getRuntime().exec(new String[]{"which", browsers[count]}).waitFor() == 0) {
                        browser = browsers[count];
                    }
                }
                if (browser == null) {
                    throw new Exception("Could not find web browser");
                } else {
                    Process process = Runtime.getRuntime().exec(new String[]{browser, url});
                    int returnVal = process.waitFor();
                    if (returnVal != 0) {
                        throw new Exception("non-zero runtime return value: "+returnVal);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println(e.getClass().getSimpleName()+" occurred while opening URL in browser: "+e.getMessage());
            e.printStackTrace(System.err);
        }
    }
}
