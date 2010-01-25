/*
 * ViewUtil.java
 *
 * Created on August 18, 2007, 8:03 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package autocert.view;

import autocert.model.UserRecord;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Some utility methods to aid in presentation
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 */
public class ViewUtil {
    
    /**
     * Convert newlines to break tags.
     */
    public static String convertNewlinesToBreakTags(String str) {
        return str.replaceAll("\r","").replaceAll("\n","<br />");
    }
    
    /**
     * Convert break tags to new lines.
     */
    public static String convertBreakTagsToNewlines(String str) {
        return str.replaceAll("<br\\s*/?>","\n");
    }
    
    /**
     * A method that grows as certain thing make HTTP queries break.
     */
    public static String escapeForSafeGetRequest(String str) {
        if (str == null) return str;
        return str.replaceAll("#","%23").replaceAll("%","%25");
    }
    
    /**
     * Return original message from safe HTTP request.
     */
    public static String unescapeFromSafeGetRequest(String str) {
        if (str == null) return str;
        return str.replaceAll("%25","%").replaceAll("%23","#");
    }
    
    /**
     * Returns true if param is set, false otherwise.
     */
    public static boolean isParamSet(String param) {
        return param != null && !param.trim().equals("");
    }
    
    /**
     * A flash message is passed as a param to a page to signify something happened.
     */
    public static String getFormattedFlashMessage(String message) {
        return "<p id=\"flash\">"+message+"</p>";
    }
    
    /**
     * Returns a pretty printed time from a timestamp
     */
    public static String getFormattedTime(long timestamp) {
        
        if (timestamp == -1)
            return "&mdash;";
        if (timestamp == 0)
            return "Past due";
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy");
        return dateFormat.format(new Date(timestamp));
    }
    
    /**
     * Returns a pretty printed exception
     */
    public static String getFormattedException(Exception e, boolean includeStackTrace) {
        StringBuffer buffer = new StringBuffer();
        
        buffer.append("<p class=\"exception\">An exception occurred with the following message: "+e.getMessage()+"</p>");
        
        if (includeStackTrace) {
            buffer.append("Stack trace:");
            buffer.append("<ul>");
            for (int i=0; i<e.getStackTrace().length; i++) {
                buffer.append("<li>"+e.getStackTrace()[i].toString()+"</li>");
            }
            buffer.append("</ul>");
        }
        
        return buffer.toString();
    }
    
    /**
     * Returns a formatted logout link
     * @param subdirLevel: 0 if in root, 1 if in child of root, etc. Increment for each generation.
     */
    public static String getFormattedLogoutLink(int subdirLevel) {
        StringBuffer dir = new StringBuffer();
        for (int i=0; i<subdirLevel; i++) dir.append("../");
        return "<a id=\"logout\" href=\""+dir.toString()+"logout.jsp\">Logout</a>";
    }
    
    /**
     * Returns abbreviated content if too long
     */
    final static int CONTENT_MAX = 60;
    public static String createAbbreviatedMessage(String msg) {
        if (msg.length() < CONTENT_MAX) return msg;
        return msg.substring(0,CONTENT_MAX-3)+"...";
    }
    
    public static String capitalize(boolean b) {
        return String.valueOf(b).substring(0,1).toUpperCase() + String.valueOf(b).substring(1);
    }
    
    public static List sortUsersByName(Set userRecords) {
        
        List sortedUsers = new ArrayList();
        
        // Copy set: maybe caller will want to use set w/o modification
        Set usersToSort = new HashSet();
        Iterator it = userRecords.iterator();
        while(it.hasNext())
            usersToSort.add(it.next());
        
        UserRecord nextUser, lowestUser;
        
        while(!usersToSort.isEmpty()) {
            it = usersToSort.iterator();
            nextUser = null;
            lowestUser = null;
            
            while(it.hasNext()) {
                if (lowestUser == null) {
                    lowestUser = (UserRecord)it.next();
                } else {
                    nextUser = (UserRecord)it.next();
                    if (nextUser.name.compareTo(lowestUser.name) < 0) {
                        lowestUser = nextUser;
                    }
                }
            }
            sortedUsers.add(lowestUser);
            usersToSort.remove(lowestUser);
        }
        
        return sortedUsers;
    }
}
