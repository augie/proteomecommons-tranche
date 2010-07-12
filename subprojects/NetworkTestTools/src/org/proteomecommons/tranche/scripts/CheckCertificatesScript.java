/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.proteomecommons.tranche.scripts;

import java.security.cert.X509Certificate;
import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.tranche.security.SecurityUtil;

/**
 *
 * @author bryan
 */
public class CheckCertificatesScript {

    /**
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        System.out.println();
        System.out.println("-----------------------------------------------------------------------------");
        System.out.println(" THIS SCRIPT LOADS ALL CERTIFICATES AND THE RANGE OF DATES IN WHICH THEY ARE");
        System.out.println(" VALID. IF CERT IS NULL, SCRIPT WILL SUGGEST THAT IT MIGHT BE EXPIRED.");
        System.out.println("-----------------------------------------------------------------------------");
        System.out.println();
        ProteomeCommonsTrancheConfig.load();
        printCertificateInformation("SecurityUtil.getAdminCertificate()", SecurityUtil.getAdminCertificate());
        printCertificateInformation("SecurityUtil.getAnonymousCertificate()", SecurityUtil.getAnonymousCertificate());
        printCertificateInformation("SecurityUtil.getAutoCertCertificate()", SecurityUtil.getAutoCertCertificate());
        printCertificateInformation("SecurityUtil.getDefaultCertificate()", SecurityUtil.getDefaultCertificate());
        printCertificateInformation("SecurityUtil.getEmailCertificate()", SecurityUtil.getEmailCertificate());
        printCertificateInformation("SecurityUtil.getReadOnlyCertificate()", SecurityUtil.getReadOnlyCertificate());
        printCertificateInformation("SecurityUtil.getUserCertificate()", SecurityUtil.getUserCertificate());
        printCertificateInformation("SecurityUtil.getWriteOnlyCertificate()", SecurityUtil.getWriteOnlyCertificate());
    }

    private static void printCertificateInformation(String certLabel, X509Certificate cert) {
        if (cert != null) {
            System.out.println(certLabel+": "+cert.getNotAfter()+" -> "+cert.getNotAfter());
        } else {
            System.out.println(certLabel+": null (expired?)");
        }
        System.out.println();
        System.out.println("-~-~- -~-~- -~-~- -~-~- -~-~- -~-~- -~-~- -~-~- ");
        System.out.println();
    }
}
