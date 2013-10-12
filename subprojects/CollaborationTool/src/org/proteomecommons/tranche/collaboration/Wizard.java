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

import java.io.File;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import org.tranche.gui.ImagePanel;
import org.tranche.gui.LazyLoadAllSlowStuffAfterGUIRenders;
import org.tranche.gui.Styles;
import org.tranche.gui.add.wizard.AddFileToolWizard;
import org.tranche.gui.util.GUIUtil;

/**
 *
 * @author James "Augie" Hill - augie@umich.edu
 */
public class Wizard {

    // create the wizard
    private static AddFileToolWizard aftw = null;

    public static AddFileToolWizard getAFTWizard() {
        return aftw;
    }

    public static void main(String[] args) throws Exception {
        // set the parameters as given by the user
        for (int i = 0; i < args.length - 1; i += 2) {
            // look and feel file location
            if (args[i].equals("--config")) {
                ParametersUtil.setMasterConfig(args[i + 1]);
            } else if (args[i].equals("--laf")) {
                ParametersUtil.loadParameters(args[i + 1]);
            } else if (args[i].equals("--usage")) {
                ParametersUtil.loadParameters(args[i + 1]);
            } else if (args[i].equals("--structure")) {
                ParametersUtil.setUploadStructureConfig(args[i + 1]);
            } else if (args[i].equals("--tags")) {
                ParametersUtil.setTagsConfig(args[i + 1]);
            }
        }

        // set the look and feel
        try {
            Styles.COLOR_TRIM = ParametersUtil.getColorParameter(ParametersUtil.PARAM_TRIM_COLOR);
        } catch (Exception e) {
            Styles.COLOR_TRIM = ParametersUtil.DEFAULT_TRIM_COLOR;
        }
        try {
            Styles.INT_TRANCHE_COLORS_TO_USE = Integer.valueOf(ParametersUtil.getParameter(ParametersUtil.PARAM_TRANCHE_COLORS_TO_USE));
        } catch (Exception e) {
            Styles.INT_TRANCHE_COLORS_TO_USE = ParametersUtil.DEFAULT_TRANCHE_COLORS_TO_USE;
        }
        try {
            Styles.COLOR_TRANCHE_LOGO = ParametersUtil.getColorParameter(ParametersUtil.PARAM_TRANCHE_LOGO_COLOR);
        } catch (Exception e) {
            Styles.COLOR_TRANCHE_LOGO = ParametersUtil.DEFAULT_TRANCHE_LOGO_COLOR;
        }
        try {
            Styles.COLOR_TRANCHE_LOGO2 = ParametersUtil.getColorParameter(ParametersUtil.PARAM_TRANCHE_LOGO_COLOR2);
        } catch (Exception e) {
            Styles.COLOR_TRANCHE_LOGO2 = ParametersUtil.DEFAULT_TRANCHE_LOGO_COLOR2;
        }
        try {
            Styles.COLOR_MENU_BAR_BACKGROUND = ParametersUtil.getColorParameter(ParametersUtil.PARAM_MENU_BG_COLOR);
        } catch (Exception e) {
            Styles.COLOR_MENU_BAR_BACKGROUND = ParametersUtil.DEFAULT_MENU_BACKGROUND_COLOR;
        }
        try {
            Styles.COLOR_MENU_BAR_FOREGROUND = ParametersUtil.getColorParameter(ParametersUtil.PARAM_MENU_TEXT_COLOR);
        } catch (Exception e) {
            Styles.COLOR_MENU_BAR_FOREGROUND = ParametersUtil.DEFAULT_MENU_TEXT_COLOR;
        }
        try {
            Styles.COLOR_MENU_BAR_SELECTION_FOREGROUND = ParametersUtil.getColorParameter(ParametersUtil.PARAM_MENU_SELECTION_TEXT_COLOR);
        } catch (Exception e) {
            Styles.COLOR_MENU_BAR_SELECTION_FOREGROUND = ParametersUtil.DEFAULT_MENU_SELECTION_TEXT_COLOR;
        }
        try {
            Styles.COLOR_MENU_BAR_SELECTION_BACKGROUND = ParametersUtil.getColorParameter(ParametersUtil.PARAM_MENU_SELECTION_BG_COLOR);
        } catch (Exception e) {
            Styles.COLOR_MENU_BAR_SELECTION_BACKGROUND = ParametersUtil.DEFAULT_MENU_SELECTION_BG_COLOR;
        }
        try {
            Styles.COLOR_PANEL_BACKGROUND = ParametersUtil.getColorParameter(ParametersUtil.PARAM_MAIN_BG_COLOR);
        } catch (Exception e) {
            Styles.COLOR_PANEL_BACKGROUND = ParametersUtil.DEFAULT_MAIN_BACKGROUND_COLOR;
        }
        // set the icon
        try {
            Styles.IMAGE_FRAME_ICON = ImageIO.read(new URL(ParametersUtil.getParameter(ParametersUtil.PARAM_ICON)).openStream());
        } catch (Exception e) {
            try {
                Styles.IMAGE_FRAME_ICON = ImageIO.read(new File(ParametersUtil.getParameter(ParametersUtil.PARAM_ICON)).toURL().openStream());
            } catch (Exception ee) {
                try {
                    Styles.IMAGE_FRAME_ICON = ImageIO.read(Wizard.class.getResourceAsStream(ParametersUtil.getParameter(ParametersUtil.PARAM_ICON)));
                } catch (Exception eee) {
                }
            }
        }

        // try-catch around all setting of parameters in case somebody messed up
        try {
            // servers
            aftw = new AddFileToolWizard();

            // try to get the logo for the wizard
            try {
                aftw.setLogo(new ImagePanel(ImageIO.read(new URL(ParametersUtil.getParameter(ParametersUtil.PARAM_LOGO)).openStream())));
            } catch (Exception e) {
                try {
                    aftw.setLogo(new ImagePanel(ImageIO.read(new File(ParametersUtil.getParameter(ParametersUtil.PARAM_LOGO)).toURL().openStream())));
                } catch (Exception ee) {
                    try {
                        aftw.setLogo(new ImagePanel(ImageIO.read(Wizard.class.getResourceAsStream(ParametersUtil.getParameter(ParametersUtil.PARAM_LOGO)))));
                    } catch (Exception eee) {
                    }
                }
            }

            // whether to show the home button
            try {
                aftw.getAddFileToolWizardMenuBar().homeButton.setVisible(ParametersUtil.getBooleanParameter(ParametersUtil.PARAM_SHOW_HOME_BUTTON));
            } catch (Exception e) {
            }

            // make visible the appropriate options
            try {
                aftw.getAddFileToolWizardMenuBar().serversMenuItem.setVisible(ParametersUtil.getBooleanParameter(ParametersUtil.PARAM_SHOW_SERVERS));
            } catch (Exception e) {
            }
            try {
                aftw.getAddFileToolWizardMenuBar().annotationsMenuItem.setVisible(ParametersUtil.getBooleanParameter(ParametersUtil.PARAM_SHOW_TAGS));
            } catch (Exception e) {
            }

            // open the wizard
            GUIUtil.centerOnScreen(aftw);
            aftw.setVisible(true);

            // lazy load the lazyloadable stuff
            LazyLoadAllSlowStuffAfterGUIRenders.lazyLoad();

            // load the tags
            try {
                for (String name : ParametersUtil.tags.keySet()) {
                    for (String value : ParametersUtil.tags.get(name)) {
                        aftw.getAnnotationFrame().getPanel().add(name, value);
                    }
                }
            } catch (Exception e) {
            }

            // set the license info
            try {
                if (ParametersUtil.getParameter(ParametersUtil.PARAM_DEFAULT_LICENSE).equals("public now")) {
                    aftw.getStep2Panel().cc0Button.doClick();
                } else if (ParametersUtil.getParameter(ParametersUtil.PARAM_DEFAULT_LICENSE).equals("public later")) {
                    aftw.getStep1Panel().encryptionPanel.encryptBox.doClick();
                    aftw.getStep1Panel().encryptionPanel.random.doClick();
                    aftw.getStep2Panel().cc0Button.doClick();
                } else if (ParametersUtil.getParameter(ParametersUtil.PARAM_DEFAULT_LICENSE).equals("custom")) {
                    aftw.getStep2Panel().customButton.doClick();
                    if (ParametersUtil.getBooleanParameter(ParametersUtil.PARAM_CUSTOM_LICENSE_ENCRYPTED)) {
                        aftw.getStep1Panel().encryptionPanel.encryptBox.doClick();
                        aftw.getStep1Panel().encryptionPanel.random.doClick();
                    }
                    if (ParametersUtil.getParameter("custom license text") != null) {
                        aftw.getStep3Panel().customLicensePanel.customLicenseTextArea.setText(ParametersUtil.getParameter("custom license text"));
                    }
                }
            } catch (Exception e) {
            }

            // set the logo url
            try {
                aftw.setLogoUrl(ParametersUtil.getParameter(ParametersUtil.PARAM_COLLAB_URL));
            } catch (Exception e) {
            }
            try {
                aftw.setLogoToolTipText(ParametersUtil.getParameter(ParametersUtil.PARAM_COLLAB_NAME));
            } catch (Exception e) {
            }

            // set up the upload structure
            try {
                aftw.getStep1Panel().setUploadStructures(ParametersUtil.uploadStructures);
            } catch (Exception e) {
            }

            // show the intro popup
            try {
                if (ParametersUtil.getParameter(ParametersUtil.PARAM_INTRO_MESSAGE) != null) {
                    JOptionPane.showMessageDialog(aftw, ParametersUtil.getParameter(ParametersUtil.PARAM_INTRO_MESSAGE), "Introduction", JOptionPane.PLAIN_MESSAGE);
                }
            } catch (Exception e) {
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
