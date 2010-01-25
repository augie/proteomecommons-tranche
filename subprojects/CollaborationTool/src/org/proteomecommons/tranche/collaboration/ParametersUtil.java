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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.tranche.gui.Styles;
/**
 *
 * @author James "Augie" Hill - augie@828productions.com
 */
public class ParametersUtil {

    // gets the master config file and subsequent config files
    public static void setMasterConfig(String location) {
        if (location.startsWith("http://")) {
            // treat the location as a URL
            try {
                HttpClient hc = new HttpClient();
                PostMethod gm = new PostMethod(location);

                // execute the method
                int returnStatus = hc.executeMethod(gm);
                if (returnStatus != 200) {
                    return;
                }

                // get the body as a string
                String file = gm.getResponseBodyAsString();
                gm.releaseConnection();

                // break into an array of lines
                String lines[] = file.split("\n");
                for (String line : lines) {
                    try {
                        if (!line.trim().startsWith("#")) {
                            String name = line.substring(0, line.indexOf('=')).trim();
                            String url = line.substring(line.indexOf('=') + 1).trim();
                            if (name.equals("laf")) {
                                setLookAndFeelLocation(url);
                            } else if (name.equals("usage")) {
                                setUsageLocation(url);
                            } else if (name.equals("structure")) {
                                setUploadStructureLocation(url);
                            } else if (name.equals("servers")) {
                                setServersLocation(url);
                            } else if (name.equals("tags")) {
                                setTagsLocation(url);
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            } catch (IOException e) {
                // do nothing - use default params
            }
        }
    }
    // holds all parameters for how this tool will look and work
    private static Map<String, String> parameters = new HashMap<String, String>();

    public static String getParameter(String name) {
        return parameters.get(name);
    }

    public static Boolean getBooleanParameter(String name) {
        return Boolean.valueOf(parameters.get(name));
    }

    public static Color getColorParameter(String name) {
        return getColor(parameters.get(name));
    }
    // for the look and feel parameters
    public static String lookAndFeelFileLocation = "http://www.proteomecommons.org/dev/dfs/examples/tool/lookAndFeel.conf";
    // standard look and feel parameter names
    public static final String PARAM_WIDTH = "width";
    public static final String PARAM_HEIGHT = "height";
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
    public static final int DEFAULT_WIDTH = 750;
    public static final int DEFAULT_HEIGHT = 600;
    public static final String DEFAULT_ICON = "icon.jpg";
    public static final String DEFAULT_LOGO = "logo.jpg";
    public static final Color DEFAULT_MENU_BACKGROUND_COLOR = Styles.COLOR_MENU_BAR_BACKGROUND;
    public static final Color DEFAULT_MENU_TEXT_COLOR = Styles.COLOR_MENU_BAR_FOREGROUND;
    public static final Color DEFAULT_MENU_SELECTION_BG_COLOR = Styles.COLOR_MENU_BAR_SELECTION_BACKGROUND;
    public static final Color DEFAULT_MENU_SELECTION_TEXT_COLOR = Styles.COLOR_MENU_BAR_SELECTION_FOREGROUND;
    public static final Color DEFAULT_TRIM_COLOR = Styles.COLOR_TRIM;
    public static final Color DEFAULT_MAIN_BACKGROUND_COLOR = Styles.COLOR_PANEL_BACKGROUND;
    public static final int DEFAULT_TRANCHE_COLORS_TO_USE = Styles.INT_TRANCHE_COLORS_TO_USE;
    public static final Color DEFAULT_TRANCHE_LOGO_COLOR = Styles.COLOR_TRANCHE_LOGO;
    public static final Color DEFAULT_TRANCHE_LOGO_COLOR2 = Styles.COLOR_TRANCHE_LOGO2;    // load the default look and feel parameters
    

    static {
        parameters.put(PARAM_WIDTH, String.valueOf(DEFAULT_WIDTH));
        parameters.put(PARAM_HEIGHT, String.valueOf(DEFAULT_HEIGHT));
        parameters.put(PARAM_ICON, DEFAULT_ICON);
        parameters.put(PARAM_LOGO, DEFAULT_LOGO);
        parameters.put(PARAM_MENU_BG_COLOR, Integer.toString(DEFAULT_MENU_BACKGROUND_COLOR.getRGB(), 16));
        parameters.put(PARAM_MENU_TEXT_COLOR, Integer.toString(DEFAULT_MENU_TEXT_COLOR.getRGB(), 16));
        parameters.put(PARAM_MENU_SELECTION_BG_COLOR, Integer.toString(DEFAULT_MENU_SELECTION_BG_COLOR.getRGB(), 16));
        parameters.put(PARAM_MENU_SELECTION_TEXT_COLOR, Integer.toString(DEFAULT_MENU_SELECTION_TEXT_COLOR.getRGB(), 16));
        parameters.put(PARAM_TRIM_COLOR, Integer.toString(DEFAULT_TRIM_COLOR.getRGB(), 16));
        parameters.put(PARAM_MAIN_BG_COLOR, Integer.toString(DEFAULT_MAIN_BACKGROUND_COLOR.getRGB(), 16));
        parameters.put(PARAM_TRANCHE_COLORS_TO_USE, Integer.toString(DEFAULT_TRANCHE_COLORS_TO_USE));
        parameters.put(PARAM_TRANCHE_LOGO_COLOR, Integer.toString(DEFAULT_TRANCHE_LOGO_COLOR.getRGB(), 16));
        parameters.put(PARAM_TRANCHE_LOGO_COLOR2, Integer.toString(DEFAULT_TRANCHE_LOGO_COLOR2.getRGB(), 16));
    }

    public static void setLookAndFeelLocation(String location) {
        try {
            loadParameters(location);
            lookAndFeelFileLocation = location;
        } catch (Exception e) {
        }
    }
    // for the usage parameters
    public static String usageFileLocation = "http://www.proteomecommons.org/dev/dfs/examples/tool/usage.conf";
    // standard usage parameter names
    public static final String PARAM_COLLAB_NAME = "collaboration name";
    public static final String PARAM_COLLAB_URL = "collaboration url";
    public static final String PARAM_HELP_URL = "help url";
    public static final String PARAM_SHOW_HOME_BUTTON = "show home button";
    public static final String PARAM_SHOW_SERVERS = "show servers";
    public static final String PARAM_USE_CORE_SERVERS = "use core servers";
    public static final String PARAM_SHOW_TAGS = "show tags";
    public static final String PARAM_DEFAULT_LICENSE = "default license";
    public static final String PARAM_CUSTOM_LICENSE_TEXT = "custom license text";
    public static final String PARAM_CUSTOM_LICENSE_ENCRYPTED = "custom license encrypted";
    public static final String PARAM_LOG = "log upload";
    public static final String PARAM_REGISTER = "register";
    public static final String PARAM_SHOW_REGISTER = "show register";
    public static final String PARAM_SKIP_CHUNKS = "skip chunks";
    public static final String PARAM_SHOW_SKIP_CHUNKS = "show skip chunks";
    public static final String PARAM_SKIP_FILES = "skip files";
    public static final String PARAM_SHOW_SKIP_FILES = "show skip files";
    public static final String PARAM_REMOTE_REP = "remote replication";
    public static final String PARAM_SHOW_REMOTE_REP = "show remote replication";
    public static final String PARAM_INTRO_MESSAGE = "intro message";
    // default usage parameters
    public static final String DEFAULT_COLLAB_NAME = "ProteomeCommons.org";
    public static final String DEFAULT_COLLAB_URL = "http://tranche.proteomecommons.org";
    public static final String DEFAULT_HELP_URL = "http://www.proteomecommons.org/dev/dfs/users/howto-add-data.html";
    public static final boolean DEFAULT_SHOW_HOME_BUTTON = true;
    public static final boolean DEFAULT_SHOW_SERVERS = true;
    public static final boolean DEFAULT_USE_CORE_SERVERS = true;
    public static final boolean DEFAULT_SHOW_TAGS = true;
    public static final String DEFAULT_DEFAULT_LICENSE = "";
    public static final String DEFAULT_CUSTOM_LICENSE_TEXT = "";
    public static final boolean DEFAULT_CUSTOM_LICENSE_ENCRYPTED = false;
    public static final boolean DEFAULT_LOG = true;
    public static final boolean DEFAULT_REGISTER = true;
    public static final boolean DEFAULT_SHOW_REGISTER = true;
    public static final boolean DEFAULT_SKIP_CHUNKS = true;
    public static final boolean DEFAULT_SHOW_SKIP_CHUNKS = true;
    public static final boolean DEFAULT_SKIP_FILES = false;
    public static final boolean DEFAULT_SHOW_SKIP_FILES = true;
    public static final boolean DEFAULT_REMOTE_REP = false;
    public static final boolean DEFAULT_SHOW_REMOTE_REP = true;
    public static final String DEFAULT_INTRO_MESSAGE = null;    // load the default usage parameters
    

    static {
        parameters.put(PARAM_COLLAB_NAME, DEFAULT_COLLAB_NAME);
        parameters.put(PARAM_COLLAB_URL, DEFAULT_COLLAB_URL);
        parameters.put(PARAM_HELP_URL, DEFAULT_HELP_URL);
        parameters.put(PARAM_SHOW_HOME_BUTTON, String.valueOf(DEFAULT_SHOW_HOME_BUTTON));
        parameters.put(PARAM_SHOW_SERVERS, String.valueOf(DEFAULT_SHOW_SERVERS));
        parameters.put(PARAM_SHOW_TAGS, String.valueOf(DEFAULT_SHOW_TAGS));
        parameters.put(PARAM_DEFAULT_LICENSE, DEFAULT_DEFAULT_LICENSE);
        parameters.put(PARAM_CUSTOM_LICENSE_TEXT, DEFAULT_CUSTOM_LICENSE_TEXT);
        parameters.put(PARAM_CUSTOM_LICENSE_ENCRYPTED, String.valueOf(DEFAULT_CUSTOM_LICENSE_ENCRYPTED));
        parameters.put(PARAM_LOG, String.valueOf(DEFAULT_LOG));
        parameters.put(PARAM_REGISTER, String.valueOf(DEFAULT_REGISTER));
        parameters.put(PARAM_REMOTE_REP, String.valueOf(DEFAULT_REMOTE_REP));
        parameters.put(PARAM_SKIP_CHUNKS, String.valueOf(DEFAULT_SKIP_CHUNKS));
        parameters.put(PARAM_SKIP_FILES, String.valueOf(DEFAULT_SKIP_FILES));
        parameters.put(PARAM_SHOW_REGISTER, String.valueOf(DEFAULT_SHOW_REGISTER));
        parameters.put(PARAM_SHOW_REMOTE_REP, String.valueOf(DEFAULT_SHOW_REMOTE_REP));
        parameters.put(PARAM_SHOW_SKIP_CHUNKS, String.valueOf(DEFAULT_SHOW_SKIP_CHUNKS));
        parameters.put(PARAM_SHOW_SKIP_FILES, String.valueOf(DEFAULT_SHOW_SKIP_FILES));
        parameters.put(PARAM_USE_CORE_SERVERS, String.valueOf(DEFAULT_USE_CORE_SERVERS));
        parameters.put(PARAM_INTRO_MESSAGE, DEFAULT_INTRO_MESSAGE);
    }

    public static void setUsageLocation(String location) {
        try {
            loadParameters(location);
            usageFileLocation = location;
        } catch (Exception e) {
        }
    }
    // for the upload configuration
    public static String uploadStructureFileLocation = "http://www.proteomecommons.org/dev/dfs/examples/tool/structure.conf";
    // upload location configuration parameters
    public static Set<String> uploadStructures = new HashSet<String>();

    public static void setUploadStructureLocation(String location) {
        if (location.startsWith("http://")) {
            // treat the location as a URL
            try {
                HttpClient hc = new HttpClient();
                PostMethod gm = new PostMethod(location);

                // execute the method
                int returnStatus = hc.executeMethod(gm);
                if (returnStatus != 200) {
                    return;
                }

                // get the body as a string
                String file = gm.getResponseBodyAsString();
                gm.releaseConnection();

                // make a new structure
                uploadStructureFileLocation = location;
                uploadStructures.clear();

                // break into an array of lines
                String lines[] = file.split("\n");
                for (String line : lines) {
                    try {
                        if (!line.trim().startsWith("#")) {
                            if (!line.trim().startsWith("\\")) {
                                uploadStructures.add("\\" + line.replace("\\\\", "\\").replace("/", "\\").trim());
                            } else {
                                uploadStructures.add(line.replace("\\\\", "\\").replace("/", "\\").trim());
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            } catch (IOException e) {
                // do nothing - use default params
            }
        } else {
            // load the file
            FileReader fileReader = null;
            try {
                fileReader = new FileReader(new File(location));
                // make a new structure
                uploadStructureFileLocation = location;
                uploadStructures.clear();
                for (String line = readLine(fileReader); line != null; line = readLine(fileReader)) {
                    try {
                        while (line.contains("/")) {
                            line = line.substring(0, line.indexOf('/')) + "\\" + line.substring(line.indexOf('/') + 1);
                        }
                        if (!line.trim().startsWith("#")) {
                            if (!line.trim().startsWith("\\")) {
                                uploadStructures.add("\\" + line.trim());
                            } else {
                                uploadStructures.add(line.trim());
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            } catch (Exception e) {
                // do nothing - default parameters will be used
            } finally {
                try {
                    fileReader.close();
                } catch (Exception e) {
                }
            }
        }
    }    // for the servers to use
    public static String serversFileLocation = "http://www.proteomecommons.org/dev/dfs/examples/tool/servers.conf";
    // grouped servers
    public static List<String> servers = new ArrayList<String>();

    public static void setServersLocation(String location) {
        if (location.startsWith("http://")) {
            // treat the location as a URL
            try {
                HttpClient hc = new HttpClient();
                PostMethod gm = new PostMethod(location);

                // execute the method
                int returnStatus = hc.executeMethod(gm);
                if (returnStatus != 200) {
                    return;
                }

                // get the body as a string
                String file = gm.getResponseBodyAsString();
                gm.releaseConnection();

                // make a new structure
                serversFileLocation = location;
                servers.clear();

                // break into an array of lines
                String lines[] = file.split("\n");
                for (String line : lines) {
                    try {
                        if (!line.trim().startsWith("#")) {
                            // add the servers to the group
                            servers.add(line.trim());
                        }
                    } catch (Exception e) {
                    }
                }
            } catch (IOException e) {
            }
        } else {
            // load the file
            FileReader fileReader = null;
            try {
                fileReader = new FileReader(new File(location));
                // make a new structure
                serversFileLocation = location;
                servers.clear();
                for (String line = readLine(fileReader); line != null; line = readLine(fileReader)) {
                    try {
                        // the line was commented
                        if (!line.trim().startsWith("#")) {
                            // add the servers to the group
                            servers.add(line.trim());
                        }
                    } catch (Exception e) {
                    }
                }
            } catch (Exception e) {
                // do nothing
            } finally {
                try {
                    fileReader.close();
                } catch (Exception e) {
                }
            }
        }
    }
    // for the tags to start with
    public static String tagsFileLocation = "http://www.proteomecommons.org/dev/dfs/examples/tool/tags.conf";
    // tags
    public static Map<String, List<String>> tags = new HashMap<String, List<String>>();

    public static void setTagsLocation(String location) {
        if (location.startsWith("http://")) {
            // treat the location as a URL
            try {
                HttpClient hc = new HttpClient();
                PostMethod gm = new PostMethod(location);

                // execute the method
                int returnStatus = hc.executeMethod(gm);
                if (returnStatus != 200) {
                    return;
                }

                // get the body as a string
                String file = gm.getResponseBodyAsString();
                gm.releaseConnection();

                // make a new structure
                tagsFileLocation = location;
                tags.clear();

                // break into an array of lines
                String lines[] = file.split("\n");
                for (String line : lines) {
                    try {
                        // the line was commented
                        if (line.trim().startsWith("#")) {
                            continue;
                        }
                        String name = line.substring(0, line.indexOf('=')).trim();
                        String value = line.substring(line.indexOf('=') + 1).trim();
                        if (!tags.containsKey(name)) {
                            tags.put(name, new ArrayList<String>());
                        }
                        tags.get(name).add(value);
                    } catch (Exception e) {
                    }
                }
            } catch (IOException e) {
            }
        } else {
            // load the file
            FileReader fileReader = null;
            try {
                fileReader = new FileReader(new File(location));
                // make a new structure
                tagsFileLocation = location;
                tags.clear();
                for (String line = readLine(fileReader); line != null; line = readLine(fileReader)) {
                    try {
                        // the line was commented
                        if (line.trim().startsWith("#")) {
                            continue;
                        }
                        String name = line.substring(0, line.indexOf('=')).trim();
                        String value = line.substring(line.indexOf('=') + 1).trim();
                        if (!tags.containsKey(name)) {
                            tags.put(name, new ArrayList<String>());
                        }
                        tags.get(name).add(value);
                    } catch (Exception e) {
                    }
                }
            } catch (Exception e) {
                // do nothing
            } finally {
                try {
                    fileReader.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private static void loadParameters(String location) throws Exception {
        if (location.startsWith("http://")) {
            // treat the location as a URL
            HttpClient hc = new HttpClient();
            PostMethod gm = new PostMethod(location);

            // execute the method
            int returnStatus = hc.executeMethod(gm);
            if (returnStatus != 200) {
                throw new Exception("File not found");
            }

            // get the body as a string
            String file = gm.getResponseBodyAsString();
            gm.releaseConnection();

            // break into an array of lines
            String lines[] = file.split("\n");
            for (String line : lines) {
                try {
                    if (!line.trim().startsWith("#")) {
                        parameters.put(line.substring(0, line.indexOf('=')).trim(), line.substring(line.indexOf('=') + 1).trim());
                    }
                } catch (Exception e) {
                }
            }
        } else {
            // treat the location as a local file
            FileReader fileReader = null;
            try {
                fileReader = new FileReader(new File(location));
                for (String line = readLine(fileReader); line != null; line = readLine(fileReader)) {
                    try {
                        if (!line.trim().startsWith("#")) {
                            parameters.put(line.substring(0, line.indexOf('=')).trim(), line.substring(line.indexOf('=') + 1).trim());
                        }
                    } catch (Exception e) {
                    }
                }
            } finally {
                try {
                    fileReader.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public static String readLine(FileReader fileReader) throws Exception {
        if (!fileReader.ready()) {
            return null;
        }
        String line = "";
        try {
            while (fileReader.ready()) {
                char c = (char) fileReader.read();
                if (c == '\n') {
                    break;
                }
                line = line + c;
            }
        } catch (Exception e) {
        }
        return line;
    }

    public static Color getColor(String color) {
        try {
            return (Color) Color.class.getDeclaredField(color.toUpperCase().replaceAll(" ", "_")).get(null);
        } catch (Exception e) {
            return Color.decode(String.valueOf(Integer.parseInt(color, 16)));
        }
    }
}
