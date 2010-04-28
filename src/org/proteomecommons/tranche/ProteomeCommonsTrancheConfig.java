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
package org.proteomecommons.tranche;

import java.io.IOException;
import java.util.ArrayList;
import org.tranche.commons.DebugUtil;
import org.tranche.gui.ConfigureTrancheGUI;

/**
 * <p>Used to explicitly load the configuration file for the ProteomeCommons.org Tranche Network in Apache Tomcat.</p>
 * <p>Any time you want to use Tranche client-server code, you must first load your network's configuration.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ProteomeCommonsTrancheConfig {

    /**
     * 
     */
    public static final String CONFIG_FILE_LOCATION = "/org/proteomecommons/tranche/pc-tranche.conf";
    private static boolean loaded = false;

    /**
     * <p>Loads the configuration file for the ProteomeCommons.org Tranche repository.</p>
     */
    public synchronized static void load() throws IOException {
        // only load once
        if (loaded) {
            return;
        }
        loaded = true;
        DebugUtil.debugOut(ProteomeCommonsTrancheConfig.class, "Loading configuration from " + CONFIG_FILE_LOCATION);
        ConfigureTrancheGUI.load(CONFIG_FILE_LOCATION);
    }

    /**
     * <p>Places the configuration file location at the front of an array of arguments.</p>
     * @param args
     * @return
     */
    public static String[] getArgsConfigAdjusted(String[] args) {
        ArrayList<String> argList = new ArrayList<String>();
        argList.add(CONFIG_FILE_LOCATION);
        try {
            for (String arg : args) {
                try {
                    argList.add(arg);
                } catch (Exception e) {
                    DebugUtil.debugErr(ProteomeCommonsTrancheConfig.class, e);
                }
            }
        } catch (Exception e) {
            DebugUtil.debugErr(ProteomeCommonsTrancheConfig.class, e);
        }
        return argList.toArray(new String[0]);
    }
}