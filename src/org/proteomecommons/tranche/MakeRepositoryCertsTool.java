/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.proteomecommons.tranche;

/**
 * <p>Runs org.tranche.users.MakeRepositoryCertsTool.main main method with the ProteomeCommons.org Tranche repository configuration loaded.</p>
 * <p>Do not pass in a repository configuration file.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class MakeRepositoryCertsTool {

    /**
     *
     * @param args
     * @throws Exception
     */
    public static void main(String args[]) throws Exception {
        org.tranche.users.MakeRepositoryCertsTool.main(ProteomeCommonsTrancheConfig.getArgsConfigAdjusted(args));
    }
}
