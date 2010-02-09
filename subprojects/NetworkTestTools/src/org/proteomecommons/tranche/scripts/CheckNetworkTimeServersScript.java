package org.proteomecommons.tranche.scripts;

import java.util.List;
import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.tranche.ConfigureTranche;
import org.tranche.time.TimeUtil;
import org.tranche.util.DebugUtil;

/**
 *
 * @author James A Hill
 */
public class CheckNetworkTimeServersScript {

    public static void main(String args[]) throws Exception {
        ProteomeCommonsTrancheConfig.load();
        DebugUtil.setDebug(true);
        TimeUtil.setDebug(true);
        List<String> servers = ConfigureTranche.getNetworkTimeServers();
        for (String server : servers) {
            System.out.println(server);
            try {
                TimeUtil.getOffset(server);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
