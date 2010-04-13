/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.proteomecommons.tranche;

/**
 * <p>Runs org.tranche.project.ArbitraryProjectBuildingTool main method with the ProteomeCommons.org Tranche repository configuration loaded.</p>
   <p>Do not pass in a repository configuration file.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ArbitraryProjectBuildingTool {
    /**
     * <p>Runs org.tranche.project.ArbitraryProjectBuildingTool main method with the ProteomeCommons.org Tranche repository configuration loaded.</p>
     * <p>Do not pass in a repository configuration file.</p>
     * @param args
     */
    public static void main(String[] args) throws Exception {
        org.tranche.project.ArbitraryProjectBuildingTool.main(ProteomeCommonsTrancheConfig.getArgsConfigAdjusted(args));
    }
}
