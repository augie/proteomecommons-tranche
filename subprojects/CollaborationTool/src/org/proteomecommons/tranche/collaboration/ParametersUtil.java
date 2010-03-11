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
package org.proteomecommons.tranche.collaboration;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.tranche.ConfigureTranche;
import org.tranche.gui.Styles;
import org.tranche.util.IOUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ParametersUtil {

    private static Map<String, String> parameters = new HashMap<String, String>();
    public static Set<String> uploadStructures = new HashSet<String>();
    public static Map<String, List<String>> tags = new HashMap<String, List<String>>();
    // standard look and feel parameter names
    public static final String PARAM_ICON = "icon";
    public static final String PARAM_LOGO = "logo";
    public static final String PARAM_MENU_BG_COLOR = "menu background color";
    public static final String PARAM_MENU_TEXT_COLOR = "menu text color";
    public static final String PARAM_MENU_SELECTION_BG_COLOR = "menu selection background color";
    public static final String PARAM_MENU_SELECTION_TEXT_COLOR = "menu selection text color";
    public static final String PARAM_TRIM_COLOR = "trim color";
    public static final String PARAM_MAIN_BG_COLOR = "main background color";
    public static final String PARAM_TRANCHE_LOGO_COLOR = "tranche logo color";
    public static final String PARAM_TRANCHE_LOGO_COLOR2 = "tranche logo color 2";
    public static final String PARAM_TRANCHE_COLORS_TO_USE = "tranche logo colors to use";
    // default look and feel parameters
    public static final Color DEFAULT_MENU_BACKGROUND_COLOR = Styles.COLOR_MENU_BAR_BACKGROUND;
    public static final Color DEFAULT_MENU_TEXT_COLOR = Styles.COLOR_MENU_BAR_FOREGROUND;
    public static final Color DEFAULT_MENU_SELECTION_BG_COLOR = Styles.COLOR_MENU_BAR_SELECTION_BACKGROUND;
    public static final Color DEFAULT_MENU_SELECTION_TEXT_COLOR = Styles.COLOR_MENU_BAR_SELECTION_FOREGROUND;
    public static final Color DEFAULT_TRIM_COLOR = Styles.COLOR_TRIM;
    public static final Color DEFAULT_MAIN_BACKGROUND_COLOR = Styles.COLOR_PANEL_BACKGROUND;
    public static final int DEFAULT_TRANCHE_COLORS_TO_USE = Styles.INT_TRANCHE_COLORS_TO_USE;
    public static final Color DEFAULT_TRANCHE_LOGO_COLOR = Styles.COLOR_TRANCHE_LOGO;
    public static final Color DEFAULT_TRANCHE_LOGO_COLOR2 = Styles.COLOR_TRANCHE_LOGO2;
    // standard usage parameter names
    public static final String PARAM_COLLAB_NAME = "collaboration name";
    public static final String PARAM_COLLAB_URL = "collaboration url";
    public static final String PARAM_SHOW_HOME_BUTTON = "show home button";
    public static final String PARAM_SHOW_SERVERS = "show servers";
    public static final String PARAM_SHOW_TAGS = "show tags";
    public static final String PARAM_DEFAULT_LICENSE = "default license";
    public static final String PARAM_CUSTOM_LICENSE_TEXT = "custom license text";
    public static final String PARAM_CUSTOM_LICENSE_ENCRYPTED = "custom license encrypted";
    public static final String PARAM_LOG = "log upload";
    public static final String PARAM_INTRO_MESSAGE = "intro message";
    // default usage parameters
    public static final String DEFAULT_COLLAB_NAME = "ProteomeCommons.org";
    public static final String DEFAULT_COLLAB_URL = "http://tranche.proteomecommons.org";
    public static final boolean DEFAULT_SHOW_HOME_BUTTON = true;
    public static final boolean DEFAULT_SHOW_SERVERS = true;
    public static final boolean DEFAULT_SHOW_TAGS = true;
    public static final String DEFAULT_DEFAULT_LICENSE = "";
    public static final String DEFAULT_CUSTOM_LICENSE_TEXT = "";
    public static final boolean DEFAULT_CUSTOM_LICENSE_ENCRYPTED = false;
    public static final boolean DEFAULT_LOG = true;
    public static final String DEFAULT_INTRO_MESSAGE = null;

    static {
        parameters.put(PARAM_MENU_BG_COLOR, Integer.toString(DEFAULT_MENU_BACKGROUND_COLOR.getRGB(), 16));
        parameters.put(PARAM_MENU_TEXT_COLOR, Integer.toString(DEFAULT_MENU_TEXT_COLOR.getRGB(), 16));
        parameters.put(PARAM_MENU_SELECTION_BG_COLOR, Integer.toString(DEFAULT_MENU_SELECTION_BG_COLOR.getRGB(), 16));
        parameters.put(PARAM_MENU_SELECTION_TEXT_COLOR, Integer.toString(DEFAULT_MENU_SELECTION_TEXT_COLOR.getRGB(), 16));
        parameters.put(PARAM_TRIM_COLOR, Integer.toString(DEFAULT_TRIM_COLOR.getRGB(), 16));
        parameters.put(PARAM_MAIN_BG_COLOR, Integer.toString(DEFAULT_MAIN_BACKGROUND_COLOR.getRGB(), 16));
        parameters.put(PARAM_TRANCHE_COLORS_TO_USE, Integer.toString(DEFAULT_TRANCHE_COLORS_TO_USE));
        parameters.put(PARAM_TRANCHE_LOGO_COLOR, Integer.toString(DEFAULT_TRANCHE_LOGO_COLOR.getRGB(), 16));
        parameters.put(PARAM_TRANCHE_LOGO_COLOR2, Integer.toString(DEFAULT_TRANCHE_LOGO_COLOR2.getRGB(), 16));
        parameters.put(PARAM_COLLAB_NAME, DEFAULT_COLLAB_NAME);
        parameters.put(PARAM_COLLAB_URL, DEFAULT_COLLAB_URL);
        parameters.put(PARAM_SHOW_HOME_BUTTON, String.valueOf(DEFAULT_SHOW_HOME_BUTTON));
        parameters.put(PARAM_SHOW_SERVERS, String.valueOf(DEFAULT_SHOW_SERVERS));
        parameters.put(PARAM_SHOW_TAGS, String.valueOf(DEFAULT_SHOW_TAGS));
        parameters.put(PARAM_DEFAULT_LICENSE, DEFAULT_DEFAULT_LICENSE);
        parameters.put(PARAM_CUSTOM_LICENSE_TEXT, DEFAULT_CUSTOM_LICENSE_TEXT);
        parameters.put(PARAM_CUSTOM_LICENSE_ENCRYPTED, String.valueOf(DEFAULT_CUSTOM_LICENSE_ENCRYPTED));
        parameters.put(PARAM_LOG, String.valueOf(DEFAULT_LOG));
        parameters.put(PARAM_INTRO_MESSAGE, DEFAULT_INTRO_MESSAGE);
    }

    public static void setMasterConfig(String location) throws IOException {
        InputStream in = null;
        try {
            in = ConfigureTranche.openStreamToFile(location);
            for (String line = ConfigureTranche.readLineIgnoreComments(in); line != null; line = ConfigureTranche.readLineIgnoreComments(in)) {
                String name = line.substring(0, line.indexOf('=')).trim().toLowerCase();
                String url = line.substring(line.indexOf('=') + 1).trim();
                if (name.equals("laf")) {
                    loadParameters(url);
                } else if (name.equals("usage")) {
                    loadParameters(url);
                } else if (name.equals("structure")) {
                    setUploadStructureConfig(url);
                } else if (name.equals("tags")) {
                    setTagsConfig(url);
                }
            }
        } finally {
            IOUtil.safeClose(in);
        }
    }

    public static void setUploadStructureConfig(String location) throws IOException {
        InputStream in = null;
        try {
            in = ConfigureTranche.openStreamToFile(location);
            uploadStructures.clear();
            for (String line = ConfigureTranche.readLineIgnoreComments(in); line != null; line = ConfigureTranche.readLineIgnoreComments(in)) {
                while (line.contains("/")) {
                    line = line.substring(0, line.indexOf('/')) + "\\" + line.substring(line.indexOf('/') + 1);
                }
                if (!line.trim().startsWith("\\")) {
                    uploadStructures.add("\\" + line.trim());
                } else {
                    uploadStructures.add(line.trim());
                }
            }
        } finally {
            IOUtil.safeClose(in);
        }
    }

    public static void setTagsConfig(String location) throws IOException {
        InputStream in = null;
        try {
            in = ConfigureTranche.openStreamToFile(location);
            tags.clear();
            for (String line = ConfigureTranche.readLineIgnoreComments(in); line != null; line = ConfigureTranche.readLineIgnoreComments(in)) {
                String name = line.substring(0, line.indexOf('=')).trim();
                String value = line.substring(line.indexOf('=') + 1).trim();
                if (!tags.containsKey(name)) {
                    tags.put(name, new ArrayList<String>());
                }
                tags.get(name).add(value);
            }
        } finally {
            IOUtil.safeClose(in);
        }
    }

    public static void loadParameters(String location) throws IOException {
        InputStream in = null;
        try {
            in = ConfigureTranche.openStreamToFile(location);
            for (String line = ConfigureTranche.readLineIgnoreComments(in); line != null; line = ConfigureTranche.readLineIgnoreComments(in)) {
                parameters.put(line.substring(0, line.indexOf('=')).trim(), line.substring(line.indexOf('=') + 1).trim());
            }
        } finally {
            IOUtil.safeClose(in);
        }
    }

    public static String getParameter(String name) {
        return parameters.get(name);
    }

    public static Boolean getBooleanParameter(String name) {
        return Boolean.valueOf(parameters.get(name));
    }

    public static Color getColorParameter(String name) {
        return getColor(parameters.get(name));
    }

    public static Color getColor(String color) {
        try {
            return (Color) Color.class.getDeclaredField(color.toUpperCase().replaceAll(" ", "_")).get(null);
        } catch (Exception e) {
            return Color.decode(String.valueOf(Integer.parseInt(color, 16)));
        }
    }
}
