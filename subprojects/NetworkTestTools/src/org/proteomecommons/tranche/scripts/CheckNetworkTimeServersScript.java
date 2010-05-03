package org.proteomecommons.tranche.scripts;

import java.util.List;
import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.tranche.ConfigureTranche;
import org.tranche.commons.DebugUtil;
import org.tranche.time.TimeUtil;

/**
 *
 * @author James A Hill
 */
public class CheckNetworkTimeServersScript {

    public static void main(String args[]) throws Exception {

        ProteomeCommonsTrancheConfig.load();
        DebugUtil.setDebug(TimeUtil.class, true);
        List<String> servers = ConfigureTranche.getList(ConfigureTranche.CATEGORY_NETWORK_TIME_SERVERS);
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
