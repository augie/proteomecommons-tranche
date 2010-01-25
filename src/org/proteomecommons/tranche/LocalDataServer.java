/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.proteomecommons.tranche;

/**
 * <p>Command-line client to start a local data server.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class LocalDataServer {
    public static void main(String[] args) throws Exception {
        org.tranche.LocalDataServer.main(ProteomeCommonsTrancheConfig.getArgsConfigAdjusted(args));
    }
}
