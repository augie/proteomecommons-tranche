/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.scripts.utils;

import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTableRow;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ScriptsUtil {

    /**
     * <p>Given host_name, returns: server_name (host_name)</p>
     * @param host The host name. Cannot be null.
     * @return
     */
    public static String getServerNameAndHostName(String host) {
        return getServerNameAndHostName(host, null);
    }

    /**
     * <p>Given host_name, returns: server_name (host_name)</p>
     * @param host The host name. Cannot be null.
     * @param name The server name. If null, will try to determine using status table.
     * @return
     */
    public static String getServerNameAndHostName(String host, String name) {

        NetworkUtil.waitForStartup();

        if (host == null) {
            throw new NullPointerException("Host must not be null.");
        }

        if (name == null || name.trim().equals("")) {
            for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {
                if (row.getHost().equals(host)) {
                    if (row.getName() != null && !row.getName().equals("")) {
                        name = row.getName();
                    }
                    break;
                }
            }
        }

        if (name != null && !name.trim().equals("")) {
            return name + " (" + host + ")";
        }

        return host;
    }
}
