/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.proteomecommons.tranche.scripts.test;

import org.tranche.TrancheServer;
import org.tranche.hash.BigHash;
import org.tranche.network.ConnectionUtil;
import org.tranche.util.IOUtil;

/**
 *
 * @author Tranche
 */
public class CheckShadowServerForChunksScript {
    
    final static Chunk[] chunksToCheck = {
        // Top-Down Aflavus: PfBBXOHKrcjh9hm6pygUhqOIXvohLKq4dS8A/c5U3Qaj5HfAhG+M9YjN3hZVIJuNmKlwfkFgXKsV4sNEYvJjQ8cLP+4AAAAAAAAD+Q==
        new Chunk(BigHash.createHashFromString("GBqD04Rv2+DKb8WfdfE2pLrxyzXr2Eg4XWq7tkpNj/eH50WCmcXKsfLCqJADLHu/ObzmJSnZpjaoZ1r67+nkNJyRBHIAAAAAF1qw/A=="), false),
        new Chunk(BigHash.createHashFromString("gVoVNL9Sd8DvXHKYpws4Z15uEhdm57yBGZ5dKlxNU+HKGKY7poyl0V3CHg6t2xevV3lzs1shDNXaanmq/JpFEFCI6KsAAAAAGWOpog=="), false),
        new Chunk(BigHash.createHashFromString("UOda7wlCvo9vWdOkzJvEnAQ9aH1LQKZJqFoEktnO4QwN7GE7bLGRGx+Sogcios5f6MPFk3xyQ3MjUgIunKfOezEqdOYAAAAASKEvpw=="), false),
        // MaxQuant: 2BmnbGCLnSj1iuz4G/uGB444cZqh5ThNyVzLEWi1BKoini2JpJidQOiDkHNFQQ0EmBRO6N1xfyRbcOmYx6esdIx703UAAAAAAAA4/Q==
        new Chunk(BigHash.createHashFromString("vTm7hh9GJe1P5EPSn6Mp66R0fcM0t7bBcMdlDPdQpWXfTBv0dPrDDzMxbafNMZMyuPw7rrS+pogTLhNEHDTVEvEyrhYAAAAADsPvbg=="), false),
        new Chunk(BigHash.createHashFromString("58jWZl/QmrxWNt5COtwo2rEiVBC40Zqfh2SDGLL+HaRj2b8CuiOyxmVdQPorRRcevPps7z57XmvXiV0sbFhfkLnAEgwAAAAAEBjFxw=="), false),
        new Chunk(BigHash.createHashFromString("rb2qUS98qV3yZujfoNLaxyXNYDVong5+NfJSu/2r1xUN80fwLsQwMKNIo7LmYOUb1eHD5Xc9aidHwQ3mJxoT/2tvwVUAAAAADfb7Bw=="), false),
        new Chunk(BigHash.createHashFromString("1bu8pNohm7WtRZh3ZUX6vXitgHmvt2etidYg+LfmJDV/18DBI/jpleJsyrTXKwSFpmXbVAE10iPjK7NAlzO4S1gplZwAAAAADxZ0nQ=="), false),
        new Chunk(BigHash.createHashFromString("7w0dKjAD3iMj+HtWrket90IbAjyg3mATu/qhzucRAjs6B0SthI4ddzpri6ipWKurvdNljtFRPoa0vJUtsRH+fFzCR1UAAAAADl/rKw=="), false),
        new Chunk(BigHash.createHashFromString("mQ9Wrg5HEYDjyQW5Hgvgp95u/1CrIu8pOOigiTBOfz1sJL7qdcObxH2m7i3bg3z0wTx3vNhYobwcUDU+kbbHy6nAGFEAAAAAFkIYNg=="), false),
    };
    
    public static void main(String[] args) throws Exception {
        final String shadowURL = "tranche://141.214.65.205:1045";
        
        try {
            TrancheServer ts = ConnectionUtil.connectURL(shadowURL, true);
            if (ts == null) {
                throw new Exception("Could not connect to server: "+shadowURL);
            }
            
            for (Chunk chunk : chunksToCheck) {
                final BigHash[] hashArr = {chunk.hash};
                if (chunk.isMetaData) {
                    if (ts.hasMetaData(hashArr)[0]) {
                        System.err.println("Server has meta data chunk: "+chunk.hash);
                    } else {
                        System.err.println("Server does not have meta data chunk: "+chunk.hash);
                    }
                } else {
                    if (ts.hasData(hashArr)[0]) {
                        System.err.println("Server has data chunk: "+chunk.hash);
                    } else {
                        System.err.println("Server does not have data chunk: "+chunk.hash);
                    }
                }
            }
            
        } finally {
            ConnectionUtil.unlockConnection(IOUtil.parseHost(shadowURL));
            ConnectionUtil.safeCloseURL(shadowURL);
        }
    }
}

/**
 * 
 * @author Tranche
 */
class Chunk {
    final BigHash hash;
    final boolean isMetaData;
    
    Chunk(BigHash hash, boolean isMetaData) {
        this.hash = hash;
        this.isMetaData = isMetaData;
    }
}