package org.proteomecommons.tranche.scripts;

import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.tranche.commons.DebugUtil;
import org.tranche.get.GetFileTool;
import org.tranche.hash.BigHash;
import org.tranche.meta.MetaData;

/**
 *
 * @author Augie
 */
public class GetMetaData {

    public static final void main(String[] args) throws Exception {
        DebugUtil.setDebug(GetFileTool.class, true);
        ProteomeCommonsTrancheConfig.load();
        GetFileTool gft = new GetFileTool();
        gft.setHash(BigHash.createHashFromString("eHA9Xf7Ia45k5lWGvHjWN9U+asmvim/JAFI7073tUDOwUa98E5ce+SzHVIuWJAy8ldtYtyouxi5CHKnX/0aHx2E1giEAAAAAAAADsg=="));
        MetaData md = gft.getMetaData();
        System.out.println(md.getPublicPassphrase());
    }
}
