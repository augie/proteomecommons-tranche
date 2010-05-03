/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.scripts.test;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.proteomecommons.tranche.scripts.TrancheScript;
import org.tranche.TrancheServer;
import org.tranche.commons.TextUtil;
import org.tranche.get.GetFileTool;
import org.tranche.hash.*;
import org.tranche.meta.*;
import org.tranche.network.*;
import org.tranche.project.*;
import org.tranche.time.*;

/**
 * <p>Used to identify projects with bad chunks.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class FindProjectsWithBadChunksScript implements TrancheScript {

    /**
     * 
     */
    final static BigHash[] dataChunksThatDoNotVerify = {
        BigHash.createHashFromString("tcPGbBTnp4DnOBrrpXDWowz2mKX8KGs2QbJ19h/Pt5cIMdj0gP+Qc28lPJPCDDRU5Zh/NCNdGNVXUeSmhbEaVQEVH00AAAAAAAAIfQ=="),
        BigHash.createHashFromString("tcPtWaNbXfATxqe/qt1+8AWNNvbbOBG1ZRj5qaF04K+e09qlyztBEiNz3zoA69YacpFnlzygC3dcOeDbBLKEWlUOo9IAAAAAAAAVGA=="),
        BigHash.createHashFromString("tcNNMv0PbW3BesKkQ7n+XD6dpeveI4oHV/zf+OzUralEqW61IFM0c9mN7qVwEx5JtO52wNyQwKNc3bTZQFSaq1aJTrsAAAAAAAAIcw=="),
        BigHash.createHashFromString("tcNhrk46VZ+akY6RzBi4r8wjxv5JO4QbtyjpJnTvD9GzV+OuZIi2j3yXY57pKS4wp1HbVNpW7XtdYWJYR36Hjk2hG38AAAAAAAAIOg=="),
        BigHash.createHashFromString("tc2frSXkN+oMwa3HgoeSf99o/uPKqSM2voQdWJYksydum/biTpsNu89s8XBOfwPeuDhQGn6xZtk2Is7ZbUz+p60GkuYAAAAAAAAIxQ=="),
        BigHash.createHashFromString("tdwMyEI6vC/6UukjXiU6wTjaZlAs650k++ghHCrqHogs/slXTOAxAKWyrUGcgA3+L3JlgaFCippCHAV5L/2C77/44/YAAAAAAAAs7g=="),
        BigHash.createHashFromString("tdxc0C0tD2MrMEwjbYw7RsaABR6eIye14D3Wy4EMuojeH0/4Sy2KLNDzDXw4SQyvLBbfVMoKrmPKKn4+ROWuWwMiBycAAAAAAAAIUg=="),
        BigHash.createHashFromString("temOPYnFnBC66J00E6Ec4kMqZ8D0iD4RZ373YxgT3whyR5Ju49Rxm13CxwtkxxfucXas56kBOIKYkpp1rOjQW6f1jekAAAAAAAAlqg=="),
        BigHash.createHashFromString("tekDmMh4ANUBS/70jd9Tr8mHhGY0LwiBRyJhH4I+EGFQIX2ROHsDpwIxEDaeDrLrj4C7T9i74KvCVFbEjrdpdvQK74kAAAAAAAAI/A=="),
        BigHash.createHashFromString("tfECXvkXXLY+Z8Gd7mA2jpeOPjXAYwCCEhD62W2dPmuue8mdP5UxEeoaVRBbamfrD4W++vdhQuJEmVkmDEpCRoRRcvAAAAAAAAAC/Q=="),
        BigHash.createHashFromString("tfYBpzpnfqhb5T1ssYQbjYSgUdYixY3D2AHehbzm0Fk+ZXEN0YxRWGGeowu7yhmz0Ws0d7GuUbfwL9DtT782cm4Q0qsAAAAAAAAI6Q=="),
        BigHash.createHashFromString("toGHAmEo5DXu4g6ma5BQQkVVAy/bWrEHlsssn5qP1AGYgJCwGbzcRdzNsnic5hQxJ1Cpnw/tqXi3BTBvfCuHrPGc0gkAAAAAAAAg6g=="),
        BigHash.createHashFromString("toEjFxogKR6XmVwkwtsRG1/Kn9rfmwOiEI/5AufgYozXe7ZWaD6GoSoUuyRbW8I1uUMTjuvMCjvfjgPqBURtVwuyBocAAAAAAAApEg=="),
        BigHash.createHashFromString("toSwQ0dml/lR0jGHOgKnQaqFetrpSgmhGByk0FMimUuqI+8KZ80Y4m2Wber/oWWSlnE2EGaqI7CKjp4le9CUlq9TkSUAAAAAAAAIcg=="),
        BigHash.createHashFromString("toR2Z889zbmdGx78lHAzmFCoKrYQVhWvrVv/ExDc8lIPNSxduy/rJOjay5EFdcrDg2zxoT8XisK1gYOlC319UHuAg88AAAAAAAAIQQ=="),
        BigHash.createHashFromString("tocq9xv9qR1qMLChjovio9oa9Lf2xNY1v84XWc3ll0HTvUV0wB++Bc9csXaEovCS7tTXfn4Xwp0zIEB15MNrWNrHi4AAAAAAAAAH/w=="),
        BigHash.createHashFromString("totjGAwJv9pTqpkfB3+DcU0p7Wh2SUDF0ESfF6HbvNA8CDiFS5frPyxur8lpt4u/Ky/92lslnEIB5KsvkGRq3nDv3asAAAAAAAAn2g=="),
        BigHash.createHashFromString("tpJCXJY0cdGW8vL6mIjlJC0TKgpUuUjf9RwW2sUioVGzne4gUI3r5XS/hQx6Ca1FaQzVcScQYQCcL7zmbwcwMmD+G3gAAAAAAAAIqA=="),
        BigHash.createHashFromString("tpu1efjRNmqIPBVvd5O4z9B6aE1AQl2CCRpClo9wHUwkAjhbz2k2Z2XImaUKMb9zoPwS1/D7OY3j6oW9YRsWlpJtSq8AAAAAAAAYHw=="),
        BigHash.createHashFromString("tqCX59poYMXi2u5vBTYWknuyNEtEZmmQwTNkaYPVkNb+3d1udiMWMHtLY7iC7YX6vePGQ3ab6lNrstg53j120jMminQAAAAAAAAUlg=="),
        BigHash.createHashFromString("tqasuuBqvS9R4oCQSM6Miv7TE3PjpGdw4U00V6BWx78EWdFQEaTQwEHdZRRnT6cPaS0HW+xKje86FVz2aUICgsFEG4IAAAAAAAAbzw=="),
        BigHash.createHashFromString("tqbw7+ykuMAp3d3n4R7R0EhyfcxYGG6lT2OF8V2RWUG29JJGYlecWAolGql1g7tzN3IPe4sVgSeV0jgLaAQ2tIv8vpoAAAAAAAAhdg=="),
        BigHash.createHashFromString("tquy1WAI1qVudiguAayGBw3/5StZkeBL7D1UzkaGH32hO5lEwCrg/ENFokvG8Ctz3gDX/MD7Q5YWnAa+Z7c4iwvDjCYAAAAAAAAItQ=="),
        BigHash.createHashFromString("tqvKKaJL8A26dL1Gyg0dmeSkhR6gqZ7nfBGv2ObMkLdnNP+9mdZ03tJEyCbuklLWF5lphjteIoZIybKZfnVrDZdT6M8AAAAAAAAbnA=="),
        BigHash.createHashFromString("tq8rhhxEj3hv8C6Nkrt3C8rbd/qIoL6QsxsB6c+iEtqYHJRgqHtI7e+WXH3nL9x0Ux7gilQiSzFkilVd1r0owvZ5+KgAAAAAAAABAw=="),
        BigHash.createHashFromString("trMGjNKFpxNRgNjcP97p5c3lSTcPJ8vLJAUxvwIiMQkKIsEgJvFNtg70LEJSqlZDISdV4ZBm6/Rp9HAzZq+fxeUXVWQAAAAAAAAIKQ=="),
        BigHash.createHashFromString("trMvaxBeUOXd5kV5cxVz7uqxxBn5f+ulPiU7zLT8YeACVnA1duOTyi0Hxwyv+gr1wdQ8HAmBqsWfWiiMFn6v6poech0AAAAAAAAWLg=="),
        BigHash.createHashFromString("trNcJRuH9U173+Tls/ZnZcl1BVvJJnBY8+ODGgMiDStJvhdhkGF4YS3CsWPauix0t1S0NK/23qRx71rxEAMOTbXFf8EAAAAAAAAIow=="),
        BigHash.createHashFromString("tru6bGp1hOdgyQDRKlgmWXT9hotwYlTOOgbo8Dcu+QKDkQ3K7Btmpm33769itZI5hnzrxZNGTRgPM5tn7YeJXbsKkM4AAAAAAAAIHw=="),
        BigHash.createHashFromString("trvVpBbU0noSecJTlKYTqjdT0HaIVek9h7R1WPNizYOjQYJqTtb5nBqEjH8W+QeG7Yl69CrJsXpYF3QdJwYlSRCNL0MAAAAAAAABiw=="),
        BigHash.createHashFromString("tr+ghStRPnSJlkVf6xRMYBTmCOSyTZjMWcJuy8mDkB9N28M0G3/Nn2qUpHlLb/vihVhwpElVOLBdxUy+3POAL4gV1/AAAAAAAAAITg=="),
        BigHash.createHashFromString("tr/M8uHErsCIAFeIxMJAowvCa39v7vB7u5Yax1Ve5tFmleGOTbEFWBKX33Er3hnMi2gu1Ih7CVYbTXacOdT5tau9sSQAAAAAAAALig=="),
        BigHash.createHashFromString("tsKAPRtHXHgyDrBR9XYcXDUwJeXwwvc2m6OihbBoDO5zBL0/2f0sS5jTtJDAvEpUgUVJ0C/ze6vkoq/Su8JdbxdToqoAAAAAAAAVxw=="),
        BigHash.createHashFromString("tsI13nAfy2lKMCUY4z8BKzV6jPtedZoVqNF591yuTqqB4S8IaMOXiVPg1EQ2g0r2VK09516wKYyb5VukNpjNRZDzLsEAAAAAAAABgQ=="),
        BigHash.createHashFromString("tsooOr2t5OchhP9mLjNjqUfQUH6FFvJwI9ol/9lrgHDQIgHv38FpPmjNzlQyv7QJtPAxuytuiMNPJ6idYsy9Bkbn+NoAAAAAAAAH8g=="),
        BigHash.createHashFromString("ttOBA3V5rlKl+/01h0kAn9wmaat79Sdcs1n82M1OoyIGHT6Eh0RdqKghmRXHOFcrvnxHPObYKNgFZvto5zCuwD/D0IwAAAAAAAAIRQ=="),
        BigHash.createHashFromString("ttNk4ty2/hCaNJ+ooF6keHtF48fUAQCXd49TJtkK9Qup0xhRBJus152mEet/cE+472+5/qnXtsR62CnfGrYHwS/c/rsAAAAAAAAj3Q=="),
        BigHash.createHashFromString("tt78wMsHQwXm+E3b4q6gl28CTd/mBZQ/bPZbiKQVBgCMrRcy8VBPrWpk4BGq00vw00VfFNg45QlmuZSFtX4z5hzwRJcAAAAAAAAYhg=="),
        BigHash.createHashFromString("tuHFF1Qz+sGlFQ3b+iGGtyLAQJOFca/9hZifqgcsWj4+cXN7X23JJkgSrR+kCxjehEAkXuYqUCab2wMzuQuLfjz22ysAAAAAAAAINg=="),
        BigHash.createHashFromString("tuHbe0c/AAfMT+0u52dtb8EMv8grPUx8mJ7iWQqkI8Hw3/t3ISBr+eNxUO8JLaG5JtaNrCEBJNgjM+4/8we6Q3hxHRQAAAAAAAAYVQ=="),
        BigHash.createHashFromString("tuvF4sm+pSwlz8UAEXkzSgNgwVFWRlElGzLiqyVriK2QvLaIsMUmGoxNtTsrNhKtoWK0OSCYUEViWsKk4cmPGgnTo3QAAAAAAAAIVw=="),
        BigHash.createHashFromString("tu3cxV3Q3i+RPTUfDlRMjyggUQNgCZAn0OJC4azi+EA+84TuDviqWSN0QKdCdZxPvIYePLqmmU1PxDZMcSLKksyLyUQAAAAAAAAIJQ=="),
        BigHash.createHashFromString("tu0LVHF8QroSEtfb034kIjTxyqDQ7F6TLnlTMTzqQQvBysAPE1AS2m4wbJI3HHjc3P3uvc8vgmtMSS31iwdUDzsriLwAAAAAAAAjTQ=="),
        BigHash.createHashFromString("tvExQFrmZ+m1S77cL6FIkyL78quevkXC8lbPWIzTZoK3BlhXorOAXSgVIJD0AZeFgkhOzUEYH3cPBQrAQeahWsrS5FUAAAAAAAAhcw=="),
        BigHash.createHashFromString("tvUkbwSZIGOLx6zHXhnh0c14r2yCGcv+y3wHQHUwzeF9GTWWGwizK0unT8JNgwJpFrmRm+gMGvfpxNa5ChIQbo3x9SQAAAAAAAAIug=="),
        BigHash.createHashFromString("thCaiZvFiWlHewklUfz+BB1Br5bptII7rb8d5kzQ+UxHNqas85Wwai78qc9kMDUK/jyvfs0iAgQz4D51zse5K/snuSIAAAAAAAAIYA=="),
        BigHash.createHashFromString("thvR8uClixkyfQxTUw1NVwsNUY9h6jlNNKl9cjA4xJLLe9mt7cjF2zX+OtvESISQr5QYdEY0M/SJP0S2oLsy0b7f/AMAAAAAAAAILA=="),
        BigHash.createHashFromString("tjXq+T8EEG/52UlP3q7wU0Ygkyuo1HNBwkK+f1/1DmDAugskc7NPUyN6mAYScEOgAk1LKgVyirY93xuZdzgVjPRraNYAAAAAAAAI8Q=="),
        BigHash.createHashFromString("tkjI6BFKg6jYr/SPEjg2GPJPmjTdWhpmBRxyTZ8vKBTpXcywiogNZNSl2U2opaYpeKL/vYDz6OFM/d2MzMPRwdnfGGcAAAAAAAAIeA=="),
        BigHash.createHashFromString("tkxFAhgwmIt2eL72VaqUzFbCHWYBv5JWdt0yisKEXw1Ie7Hg3Zai4e/Ouh52FwsMtkqm8Dml4uxeFLnf1wImFtoLZOQAAAAAAAASnQ=="),
        BigHash.createHashFromString("tlA3oUiGkD8BGMu2sGMFqTM+SdgNEmMF5Ltuh+Z0v0IsczH0z+YXK7AcEfVHG6hGbzc31hOvNecFnwedtWJUrfsYYYUAAAAAAAAbKw=="),
        BigHash.createHashFromString("tlRNKBiF5fXZafGQ80d2wlr35ZBS0zmTnqfhtgnngbNvqZb5qC5J1vx+bnbnTnLan/623+kQApePIcVeQNpe4JCeSzsAAAAAAAAJAA=="),
        BigHash.createHashFromString("tlzFoRxXoz1RpfNzCV3f4Wb1ByWCbL4Tibvy945P/fU/ffl7zkHPwyGyLyjX+hApJLii3ZtuGV5WUees/1hGkSO1IzgAAAAAAAAXrw=="),
        BigHash.createHashFromString("tmA6CamrSIwwNrBXJw1cZQZh9RxCCTglr7y7Em321NODIx5FTM/fzwSaRLNPc0sq6dP5LTyBryMRpSQkvpNlci9McBcAAAAAAAAlnw=="),
        BigHash.createHashFromString("tnAsurF6N91oXSS2icGFLsoCP7RNVpDPahx4Ay7dfLxuyLD/U5J13biq9HLsThN4pKg5lB/ddgo2zQynta7N8+lZ/z4AAAAAAAAWAg=="),
        BigHash.createHashFromString("t4NeiJIHyflO3NI4jkAnqCyP6JO90ureN4BLmVbrsuN/xbih7sGcicrbxy6Gx/6dfgomgwFHw8WUUhqfWCD6Y9uaHEIAAAAAAAAK3w=="),
        BigHash.createHashFromString("t5w/B7lpgYyw9uwL5buykULn6mEz4Q15pxCuT58S6QDjycEH5rpIxlmhyv5VkgdigF877Q9bIizV9kHsHkYSemJ9vJAAAAAAAAAR+Q=="),
        BigHash.createHashFromString("t+y1FhiAddT7daysQAOKOKw8ehOEyALgPmzhWQrPXmuS2QvOdoqpAO23GAwa8rGMcrr1uHNmbE+99VNody8oo//Pg9gAAAAAAAAH/w=="),
        BigHash.createHashFromString("twKLMHo2cGQf9VWe8gwabicoE7YHQztPmUDnS6NtLsvBmrQGFz6GVbYcpLVhXJ43e4fEyDYPZSEf+C/UVCFkeH3NzogAAAAAAAAILg=="),
        BigHash.createHashFromString("txBNuRrijWy1b8+N9+yqNY76eWTrdifVlNQGCoH56sgQSP5QDCEIIm4kKmSKIwE9SF7jjZoIbAgOVWejdyuZc5O0bx0AAAAAAAAf1g=="),
        BigHash.createHashFromString("t2u8u5Qf9TJT8qsmuQPbXkSp9CcxtMTaK1H9/teoIRsGPgy97luBX4oOKBWnsJIW5Vv/Xe5maiCCxGQhl+CnVgN9vwEAAAAAAAAIXA=="),
        BigHash.createHashFromString("t2vIfffFwkasUMVos5sI4+c0fVvD6tGtSOq52CeSNrSNJmLaopNuwTLSh+AfjX6SZFa/EKxxzLmphI397kBGh6rBvcEAAAAAAAAXGA=="),
        BigHash.createHashFromString("uIaEb9qdccPvJXhi/1DAihoRJtrWTys5Y2mvxcMBtHsXTqBJRh1AO6s/lZXpBsHc6zpibCRwQoxuX4O6wqthsDTST/cAAAAAAAAIDw=="),
        BigHash.createHashFromString("uIadKIt2bXZ9/jB4XhjfCD3RPqXtDLkk6oaQSPwVCB9yJvusKpHO5ieTtuE9f5hEs5FtS1tiv226dHQEXtMSZglruJ4AAAAAAAAV0A=="),
        BigHash.createHashFromString("uJkQJCSm/f90lVwTIplSOAIvAVnOjpEQvtd2W63oNpNszGgNvzUv0ZRoEqRU8E6BqdfGgeN9aYws28I6RuKuMuTv6dIAAAAAAAAIUg=="),
        BigHash.createHashFromString("uMi+lygA2DJeR6cBoxfXZvmxB+JAZ5wn/oMHw74futXK9WQM37vDQHIvc/CSyd70IABfOeL5DKp/5XOc9b/qC9t9Le8AAAAAAAAj5A=="),
        BigHash.createHashFromString("uPMxDqE/sJaFBxaj2wKnZxjeBsL7yAbrAqDcWLNGyTsXDIzOQtjG93119HE5t7Qg9H4EjaB+uPyPaCooYIHo7rktE+4AAAAAAAAIjg=="),
        BigHash.createHashFromString("uBum0DEDtOA4TiMFdpHpxyHylCHa6k4fI4S5oYHu6TXY0+2kyu9i621N9VeQYSxkUGfNJjyp1W6u62K+L/I7MEdrWUQAAAAAAAAZrA=="),
        BigHash.createHashFromString("uBvP7SsfpDNuA1DtHtP88i3rAD4EScNOtq5DOuMFakT3isX37AODZteQYOye4zuVVgQ6y0zs5IqHkUnOwxWOpsPzEGMAAAAAAAAH/w=="),
        BigHash.createHashFromString("uDPiK0vYI41TxOO03ltqIDUs8vEtTHESeCWfHTRUw85RZag04+pHBCpr/y/Vbp9bJpOB0o4/tp395IoHsq6+dp8KzMEAAAAAAAAImg=="),
        BigHash.createHashFromString("uEaVMgnE6e6awIZx0Vo/bcqL3IqHvIg1xnFZ/iw9DElWitsM2/N2qmn8ZAoYxt9NOWt7bViGp+hzmEbmEXoNAu8+UmkAAAAAAAAIOQ=="),
        BigHash.createHashFromString("uYmIRp8R3+fzDsLLq3iMtjzqI05QxgMKRhzFOT79LYQuP9xOIujVt6Suj/rxJis0WjCXp4d7cozuKSOCx6FpktA7JbcAAAAAAAAbiw=="),
        BigHash.createHashFromString("uYlIMsGI056V7sYPOLcVhuxlSDAZER4d7QYLMii/b2zwU+SrvEUN5Ns1p2wWv7a8aNEdYPDXPKojWjBzguuuF+Su7GgAAAAAAAAbFw=="),
        BigHash.createHashFromString("uY2uILhHo1G9bJS+EkmJydUhF94OElHPKftrPHGEatFlXHd95zpUYOYNigNxUKXoQS/YG34L53qdtiCldwBBuDXWyt0AAAAAAAAXsg=="),
        BigHash.createHashFromString("uZGSdfmsib6lf8LU3JyzYQRBn/pZlkGydrBPvuy4+CHnVnqHQvCCaW/ACRSCW4xCo0UIcSHivk23HkaV5XI0Wb9oqgUAAAAAAAAI3A=="),
        BigHash.createHashFromString("uZ0bGuZFEEFYRk4FHvQl5qQqHxtRC6O/LdwxVdAc2iiWCDeBa+y+z2Jdj00JJI/LBes93PTvrSeJHr9ug3bN6n2nsRgAAAAAAAAmAg=="),
        BigHash.createHashFromString("ubPvjRI7m0FKhXauq0ET1QvkmRXNieeok+tbmY+JClKyXvcULwkdJLx1COnBWu7H9JSICZKb65akdYGRgMgLc4PS3CgAAAAAAAAIvg=="),
        BigHash.createHashFromString("ubMM4orGM1PvvgQCJa2lWGFpFgnUHSbGNbbupYN6aJfGRvwkcEmn4PKp4bunaMzxxX9xhXJ6r19nw2A/wLU6cM6GPqkAAAAAAAAIeg=="),
        BigHash.createHashFromString("ubeIAG+JDFyRNZU8u1FRUfrphuhfmvgloG/hL+vomvHF6vmS/0J1m3QaCyLlRNUY5L6yqw6o1jBaIx9QX1Fvxo0fJbcAAAAAAAAcmA=="),
        BigHash.createHashFromString("ubewJBasN55QKBF03JUIjgv9UdOXg4iWkHdvI27O0ubW/1AUQWljQkqBaLdDs/D8ak+GRBFdJiA8Q9kteCA94z6YzLUAAAAAAAAfyA=="),
        BigHash.createHashFromString("ubdCO+z6JEc/ZNVkF2mMrQ3GBxchEDFMmQUTU8sdfRjvY8a5V/viWP1NjMAbBNVVxm8i12tp896K0ESrbNORstrEupIAAAAAAAAfWA=="),
        BigHash.createHashFromString("ub4GsMIMUMs0PQUIQqcOyL233RD9BYa0PhOYEaL4LDyGIOXQv0GwAp5odat20N4lmu8lWheNPlpx3oWtNdcQVGYKWFoAAAAAAAAIIg=="),
        BigHash.createHashFromString("ucfiZKFfe2HIB28uOoZWosLl07RHN3c24p433KZ1teSKCxRAkRpIQnGTWT+bTmQ8rXeiH4OpHPAFHDIjdv9fLulbV48AAAAAAAAH+w=="),
        BigHash.createHashFromString("ucd4pSjq8Gxkka/DGk2BuboQn9gnZO+f3+P89E1tGZHYJr9Q/yGEYgAlHG1v9Lau1lub/rBqon90UPVTrLjvinOZ3bAAAAAAAAAdrg=="),
        BigHash.createHashFromString("udy7ZAEwRMtRHWvZzqciKdTLtbSKO+/0H/WX2FLxU2kjyH91jfiDiAYQfUEJQsLWAt/IP7FCWWdS6HnoEjxAaVU4cOwAAAAAAAAW1Q=="),
        BigHash.createHashFromString("ueBv1NKEn2WBS2Ws76xy6eFFg6gDSUcd/Vt1PwTCUYLIlC4HEfd8UgdQnT27DSBGaCzWC6Ud9IbXGG+Rvoilk0QOb2YAAAAAAAAd4g=="),
        BigHash.createHashFromString("ueRijN4ggi0BKvH61AW4SeDcrMUExXcx1i6EO3KxB5tAC2STZslDn8aeIQ1BIYIDcOKlGGPoiY4KUFXjcBU7rq0b3jgAAAAAAAAIUQ=="),
        BigHash.createHashFromString("ueujzODkwrKTnlwF3OwoOR8koe/00ulu5rdllyN2hZ59eBJqaZQVmcSIjZS9/pDcG27ZUoSSL4ifDAndg7fBol50lrUAAAAAAAApzA=="),
        BigHash.createHashFromString("ufKBTa/7c8ZXrrgQhpAXFZrKeyTNOme+dzaauT9l+b80wKOEmUwJpPdkDIjbjRgTHBq+XBR+3sVzAUVmt7UU2scn/U8AAAAAAAAIjw=="),
        BigHash.createHashFromString("ufqtD5AOHJ61FiQoGIKt8E9mE8lFIbbpSBBVLI2ZbFD6jUp5L7AIdkYchvwum7MZmT/tzS1ZvTQlO4DHvGBY9wwB58AAAAAAAAAVoQ=="),
        BigHash.createHashFromString("ufr9wX9aLeJONBNw+j3B8SosZ4FobjU5YCLxDFte3mjcSGytFUSSo1Tvm6KhmJZsRfIdZ80wBCemlrXQLDTkAyLukq8AAAAAAAAIAQ=="),
        BigHash.createHashFromString("uQQ3cmPg0k+x9vyjAfGvfoweahhdFW4AhlkTNzLyzbPJEnAw1tLRm3ngiHGDki8Xv6Zh/rpH6edi1kYt07NSLRjRHaMAAAAAAAAfNA=="),
        BigHash.createHashFromString("uQgs5BmvMVTha4fDV6/TL+hlPbC+OIIGj7gFKe3ggugYrMigYCh/6r9zMh4+6X44qGoVtEOlPUNmCzgp2quQxxYAIZ4AAAAAAAAOvA=="),
        BigHash.createHashFromString("uRG/MMkzWMwV8vSIdjLASRnh2fOSCoHW7mBRRvlzdgqMCd/WB3bnQa9FcwnL9zA78KQkD9Xx83L3GsTRpGi/197bqN4AAAAAAAArcA=="),
        BigHash.createHashFromString("uRF3fJsTmdCeSAmyK2/BW+yoPhTKxHnWR/iMNwFTbkwDMcyVIGsnl8E1lPa+a+eUiCatPZsnT56nuXSI9RfN4Asj2/YAAAAAAAAUPg=="),
        BigHash.createHashFromString("uRUNs3NTNRHVb+J5y3pdGDUT16pL0MOS35RXr1R3xDOhuGAq1sY+OKDRNy50QdIRBcY3cprzW65h8sjomE3vPeUtU9oAAAAAAAAaVA=="),
        BigHash.createHashFromString("uRVPAhm2zYPb2887lukCQ7sMwaI40avwUMlDTYu9/OG+2EnTHGBgvNLybQsLZvuLlMkPhnimsULwJKoSnilQeJkS+6YAAAAAAAAj2A=="),
        BigHash.createHashFromString("uR7T0ChDMBwJZgKievV+3+VfdtrE1A2wXsJ1mEpKiJ1WIcwRXyFgqkFlennQdr8TUPAFZU3mtgSS0hTcnngvqRTMDLIAAAAAAAAcqA=="),
        BigHash.createHashFromString("uR7q19eyPg6L+I8Z9/sPdHlhDVJeoEwdyWFWop8Jp7cBglPY/5HzJJU/W/W8Dd34tteOllpD5ZwVzirmUt+cnKchKMEAAAAAAAApeQ=="),
        BigHash.createHashFromString("uSUAL4O+mnVNgvhTWCrP5eY/q2MUQvnKG5EpdJNreN2hLF7mjRQxzRAt94SNABYvtNq02pj1gnxotTHYDKdkTKwnrIsAAAAAAAAXOg=="),
        BigHash.createHashFromString("uTz2xE+DImIZ4Ylvtf5dZsdOhPtchnuZ1+1DkDfzunY+pAALDXP8KzRilzpTWlZ+y5aPxTE7UWS8VEyXfRvnCrXZLc4AAAAAAAAIxw=="),
        BigHash.createHashFromString("uUCv3eUFaQSxqQggE+rY1bboscTbugzfeHMZgyYGLtjMNo4cuHBGNXgbK9qYxYyxZW+uN63s1VREIKbGRy9B12gh4TMAAAAAAAAIbg=="),
        BigHash.createHashFromString("uUA3VhYlxe6LOBKWxWm4ZtMehKg8vQogIJueSfuzyyyvpaz3gTibKcw/1KsjE7CJzS071T6KuaRwuIBKEv3fNHvljRUAAAAAAAAcGw=="),
        BigHash.createHashFromString("uUxsQCJSbLlARJIbUh0K9vA3inenJwINqpYnGuq65huPftUQCla4kZQU7cWHdHn0DUepv75FA84bDK8FFBSSGcXXu94AAAAAAAAIoQ=="),
        BigHash.createHashFromString("uVQlnqMdowyFfy1rbHPxeoJ1SwS7I+A+BIrybY8IzI5xOF4BYDAHTVEsKFC8FlFmciMwwhjVq1+D+ltmImPf2a/TAEgAAAAAAAAIxw=="),
        BigHash.createHashFromString("uWOfLA4rb40XFhB1UM6AuiGDR0paDJmehoxsAfFsiYfIPKYYnW3OkkuBPfHGUGMAGBbNVtZ1Z0rQaAe/Z0mGHk7eZSgAAAAAAAAIJg=="),
        BigHash.createHashFromString("uWeeH0iK3cMWeQS0qhj1wSA44YOfIxKdkthwYk+yyaspDTF6I0mwrQrFavi7KAHDKh7r+pEUuR+j/su5IxuywzrTUMwAAAAAAAAIeQ=="),
        BigHash.createHashFromString("uXtdOugQUXNiipKwekwTYlwqY2kFHlOWtL9JZuLHekk49uteGt4ohGNwGQ6hF/v+niI4+f9QrmrrjviDTfpjfTB0utUAAAAAAAAZuQ=="),
        BigHash.createHashFromString("uX9WGebSSkycWdLyBd2dOGQP5WVFXYPtH+nr7npnrHxGEE//fy5d9Y3U4iDQ6oa4f+B3ZXnACCApwwvdOthDKhJABSgAAAAAAAAItQ=="),
        BigHash.createHashFromString("uppY8b/V5ip14WuP4kTailMNeVbp1PU5vvY566pifSKgY6m2WsX/ssiPIKmUp29iE6cCQSEfwDHNtxGBS8PAMdY7d1EAAAAAAAAhSg=="),
        BigHash.createHashFromString("up2FpsvDQRROQl5DTbg6QAOlpSPoLJiYhAh4Nj+zHku6hZwgpu3CLaoeOzFXv4cZp/hDChtB/v4CqHbD72jeeN2yk+UAAAAAAAAayA=="),
        BigHash.createHashFromString("utPPY/MfWxQk9OpOG3eJyRK24hBQ1kk+u8UFW1oH76ZbyzAq+ncHEP0uzX9wKg3hMklIFNtP1YR3xTiqUug5i7+X898AAAAAAAAJqQ=="),
        BigHash.createHashFromString("uu5hkbmVqo4vwcHW8er2F6u4DZKt6RbbxCrqz7BrWLDLq1/IG944RooZQiOw/5zZUSmK077U8WNboVUo/2xoAm3WRGkAAAAAAAAXWA=="),
        BigHash.createHashFromString("uhcvBQyCOoqpt6jy+XHszHX1eLqLL1/0ED7IFfe3WuKHydCUHTxuZfD/saI0Gj1EQmoHr8sfYoQulk/WvtsSzkPaXxYAAAAAAAAZIA=="),
        BigHash.createHashFromString("uj/+0gm/5GKL3YiCUYqZ4H67r39qZAtGjz0d7tounWVLzeTAdHK6mA6YVEKNu7CcHYtd8Ko3RsXSWT1vxsCgRtqdk1UAAAAAAAAItw=="),
        BigHash.createHashFromString("u4IWKJHru5R3tzFlLmFMgO/I43Vcfzuf2R5c4H4rIOhZMkHG688wC6+ZVKJBeOMej1UnV0wIGTz0bynTSziwaODMwfkAAAAAAAAnsQ=="),
        BigHash.createHashFromString("u4Z4P+A9M+Y52DGTFxsoNbSezdWoAFDt+OandzuNXba0m+WzH07epTEeu0jrQR1t2fycZwsujkq2O3gB63h5LXd4OlwAAAAAAAAkKg=="),
        BigHash.createHashFromString("u4qeaBX7PqPT0QE7FyC83UYIy+Ynzu9kx93V4CLvX0IhVa5d8DH5BD2Ssxw8clT0XxXoAmDctysFplUyHN432YPIsgcAAAAAAAAIeg=="),
        BigHash.createHashFromString("u42qjmiCIjbXVjLX/9QGGSZv7NNGiS3uFWGvx5/4RyxeSEipNgQKZrUUzn5zQCTF7DcWgXscVajPcbApFPTqZnCf5twAAAAAAAAaQQ=="),
        BigHash.createHashFromString("u433dKjwohc95XmGNTaHQMyLjbzZ0zMuAu1oWFOY3gFZBU7+jTrP4BteB7Y4eVZ64cXUf3ATMvMzqVKoNJacSU3z+tcAAAAAAAAFMw=="),
        BigHash.createHashFromString("u5o+3JI4bqcgffdY6qALzJTxURm0QMyMsNTupmnJlOYNkWfPb/LEjf0WIR6lB8zyQ6mX9bBYM70o9W5owh0N32rPWU8AAAAAAAAI1A=="),
        BigHash.createHashFromString("u6nMCQ6RQgp5DXi5iv/CnEWwHaV/rKuzgEhD80jkEMQ68G4cnTYZavmKEppJvioC0GSNJLTxzAuXksBuYiVRw6LUSRoAAAAAAAAVfg=="),
        BigHash.createHashFromString("u7S4Yt1w8iCuKUTffp+nPQsR3blfAy5zhcPFyR04IYZGSTqLJWh8VVtMR1dKuR/7TfD11C0168ABleymA45WnnWXr6AAAAAAAAAR/w=="),
        BigHash.createHashFromString("u7eLueIdc9uiXj5qcOTbRrqrgsaJebciRsr9Bwz7sXBTIvV4J16wx4st/x6RByBGgzFdpdY0r53HZ76muWEb5JNigvYAAAAAAAAI/g=="),
        BigHash.createHashFromString("u7c91vIBpxZ+uiNyIbOBkbBISbDEyB3znS0+RKNrQ7Sq5oFasSyj4fozKT3bytqCaTpv2TLaobkkFRZmDlZCgKhGKW4AAAAAAAAafQ=="),
        BigHash.createHashFromString("u7rUdIO7/ybYgtH3+C09a52HjuXBiK7odCGMt3jDPguBA+kd3yro5dZ9yDJTSxrLaqcuoHmt2J5THTBjBbHfMDQyEL0AAAAAAAAIjw=="),
        BigHash.createHashFromString("u75BQ92JMzDDDNXVu3hrpiBamaq014JOu79DVt6GLafRyKs+RbjRWA9pTJuxBpJn809wMj4Af3jEk6SFKZcakCf0gzkAAAAAAAAIVQ=="),
        BigHash.createHashFromString("u8LNCR4Fy/Tp0BBo593rVHRZfgPzm9V4xb6s3shvCmXw/c75MWvc085X384gLR0FXWnnc53CGWRjBmiZK8XDOKrN/RkAAAAAAAAjtQ=="),
        BigHash.createHashFromString("u9D8l/0k2XyrNMw3wysF3eDWF5lsWjexf9ITuH0Udu590iaqh3NRKO2R3YOUm38buKOzUvpny5rX9NvEzqHAYvctc8UAAAAAAAAlsA=="),
        BigHash.createHashFromString("u9h4CAKZ2ALWykjP71VEv1u6oLwqWHpElrsjeqqls/+84eEynzFCaba2+p4zMbgtcDqSS2hdlnTWjKTgdUYJJd/dRVsAAAAAAAAkAw=="),
        BigHash.createHashFromString("u9uHAvlqF4hdhK9Zw5cbrwcFXO/iBoW/ZT6wZEYlvliXRiAkGFZu6SIV+PaIykOTcdClkm9G9TYywrIywEgGGCevifMAAAAAAAAdfQ=="),
        BigHash.createHashFromString("u9vrZnGC5fqECtR3U7gdYAXTobMgcfG8paTWDRv67c46lI1pwB3tUaolcGjbwfIDovpS9K31jxrbL0BXUlbL+nFdjSoAAAAAAAAI6Q=="),
        BigHash.createHashFromString("u9srQ+H46hDuSxl0WbzcPouY3vqjPzhx33pJSPLa6MTm+wmOSIpsKEC4kBNoVb11KYNnCfBroWEvDyfJNPtNiwxqWfAAAAAAAAAI9w=="),
        BigHash.createHashFromString("u+TSl1A4IX+jjZ07sDr1jM2I27e/gh098B0Ocib4anNC4apXZylRAngy+x/cVIW6Q9QRA8pUn11wQplxGOpxtj3ljUYAAAAAAAACJA=="),
        BigHash.createHashFromString("u+RtZNUPHEQFLUEdrdFEfZ/deZvoodk1w6hCNyGz/Z5mg6wgBIKi4gSeWa0dccUkeDih2ClVTS8bpFaaXvVKKvF/MHoAAAAAAAAJAg=="),
        BigHash.createHashFromString("u+ePySfr1JbloYfnJ0ET+18OEC2PB8R9Y0aCYFE5zD+ONKWyW3kGoVHJr+HpUX/1AVje8vFjZvnPq6W32Xu+bPGOcQgAAAAAAAAIQQ=="),
        BigHash.createHashFromString("u+cmqRlPuopNX/stx+c01eZHBEel5Xlp8Ku+sncR6YZEKPmInWrUgWkbg1thUN3tBd2TUeUnFTtf2YZXyIN3SheRzD4AAAAAAAAI3w=="),
        BigHash.createHashFromString("u+t2nAm5oqlTJsiOKEv3D9sr6TjLnWANbxMTYfNTSTi7pOO+TxuyEq+zSAsSsh5j6bl5tnvNI/rYweSD2uEKt6zlnYEAAAAAAAAJoA=="),
        BigHash.createHashFromString("u/d53D4rmfga/32pBIR0lVLr4VurovWRUXUl0FQV78Kjda6QXcViYaCJyTPYBbXF6KYzfBbSiCzVvqY4gJTOoxbpAq0AAAAAAAAIJA=="),
        BigHash.createHashFromString("u/8Uvu6Rtmbk8GxTl0qP10v91dzyKnxjWGrQiJxKpSdB2qs6+Gn/Zq8/ige+xYoj+3D+hxP6Ghu62uZBTRPTUqqxMv0AAAAAAAAIbQ=="),
        BigHash.createHashFromString("uwaMFuNSw7py0Vd6t8PC7piJ4DNdjmtpfWqz4u00kXThSGpg7P7ignaT7osWm8RzfCQJWmWOSZdpzg7M9nXMRHI8SQ8AAAAAAAAZ9A=="),
        BigHash.createHashFromString("uwZONhHj0EMfXMVN4Fe7jsEcUW7Karxyxsdw7YXHxFPeAkrfAfhuWxi4/9sDLG6q2EAbGrjr6wlTPbrRQcPd1itTqkwAAAAAAAAd7g=="),
        BigHash.createHashFromString("uw6ItUU19eBYtUcIupkRAAGF1ncTTy/oXWPWDykgjYHnFDn/7d/CFbBwxSoGEqJiYyqG+IjZ1/zg3aPHdN64dMenw7wAAAAAAAAXFA=="),
        BigHash.createHashFromString("uw5uzKoiUDnAT+9thiQ/TYeH0tJrrzOZQTqkqSYs/8Er8oPhbeMTXyYzuAytjYjhaHDT+iWU9paRTf/os+girfcc3JIAAAAAAAAjhw=="),
        BigHash.createHashFromString("uxGTQsAKbPio4x62eoq+8xDz0h2DpyTY6rBnt5hiSj5LzefrIRTve1BRcQQAKSIDFo5HR06RTHsCPPB4GQTisFIWkDkAAAAAAAAIDw=="),
        BigHash.createHashFromString("uxZAhvoTP6wKvSr0EbjINka/VeC0JS6b1/YUy0a0UcxNdqZ7V0a78NAaEbeeqTsHVSEsnbv7ihDovuLT+F/3KmvIwUwAAAAAAAAINg=="),
        BigHash.createHashFromString("uxoDc4okfdQWj+AMAIHyPzWjFsHwxFJA5DZZoEMNjY3RdlmghMdZvQD4f/fQMVGbcko6hxuDG22Pmga8ZWpbiW3SfYYAAAAAAAAIvA=="),
        BigHash.createHashFromString("uyEjspYuFI1Zi4JgOLA3+zkAhP2Zfi0T7PQcDAX5iIIj+gkyuI6nRS+GfURCsjG4p2qgga9KN4r9i0EBatk6aVv0T4AAAAAAAAAWgQ=="),
        BigHash.createHashFromString("uzh1rLNrsszI1Pl7/uTueS1mYwevtVWRDPzO9fSGLbO7amc7f69PUwdr9rIDV13hCLh83rMInq5TyzbaSVt0khYs/XUAAAAAAAAIPQ=="),
        BigHash.createHashFromString("uzzV2Ti+415DgFQAkPaOrl7wBu5Wsnu+Sm6Y2UTKsy+2AzVV/YXRWrvQdagB/dwXpGW6w0ypHQbnMdSwmXyEInuVJgQAAAAAAAAIRQ=="),
        BigHash.createHashFromString("u0D1Q0QSw2GH4fSfn6g1kSGDBDCv3RHzQOYLlt0NbSa8TfVUYBhL2Jvb2ft1+5BkOBWTAQZYzJMV27JgK/ZczR6E7UwAAAAAAAAgGA=="),
        BigHash.createHashFromString("u0ABiVMRMPAntJU3nNdA7Zt1bDTnTxvkY8EwWThu4SmMIQVncAtwXrZL9odJcVVBbd7k9gpbhWRAhFAbZ1fbQq547goAAAAAAAAH5A=="),
        BigHash.createHashFromString("u0TMmZ6U6i2rDszsGDsH5G7327RZMsq2LVt5K+QzOCubWQ+4GsCsAX9LJAC/hb4TYfF04eGjDyI45fRB8dyPAH9F0jYAAAAAAAAYuw=="),
        BigHash.createHashFromString("u047iKlbzzchHOxhYdTGnOo2//a/7J/X5d1wsiyIDA6Mc+4EATGo3Nvfowr4Cf+699JkxLdwJJ6N64pA7w0xlj6Z2ZAAAAAAAAAIVQ=="),
        BigHash.createHashFromString("u1GGL90E+a+JD7w0luOSSU4jmsHGXzrUrbCRR+EOrmaOVgXiizxKUsNelC2Jnsdqwgbbwsi7/eVJ7sghuMJvwLg5pAoAAAAAAAASbQ=="),
        BigHash.createHashFromString("u1kvbc3vYNU1IuilOYWrAl8QGLz/voto34/GR4Eqy9G84C12eXKEX5dIFR4xtS/Ku2k36QD6I+r58NRBSSBxFMdDoigAAAAAAAAIWw=="),
        BigHash.createHashFromString("u2GYjA4W28DgQYtHYmW1722n9tU3PLKrt7DRJ+CpV9PZQ65vd6F2NGROEbVqOkcLfc5HJzlF4xELyWc0eBFfh/YKp7AAAAAAAAAhig=="),
        BigHash.createHashFromString("u2oMhJcERBK231OI6cGyt4LG3a53bunRsZl7/tImE4xvY+5y28C8oeA2Ba8H4WaWwAgExQ9OtEOtwWKwq4/h4+hcxxwAAAAAAAAIZw=="),
        BigHash.createHashFromString("u3U4CD9pqVxRzGXGbdROs7Axis8d5WeIR8fHoe+RExutHERsszm4A8tbIMNpXa8xT5mY5dGgEycbYG7prOtJL40emKkAAAAAAAAI0w=="),
        BigHash.createHashFromString("u30r0yaIjkiQENZ6R9rEr9jku3Wqz/0g1PfkAQrVShy5cNBd7C/JTUWDghDCG28NQHgUAxGniZa44NoGWaoHBfoDzmIAAAAAAAAI7Q=="),
        BigHash.createHashFromString("vJCWRUPkTXKVSHJqPc5fQzvRxOBhh28JPP6NP3lrCH9dtPfRm2ynSTuj2DyuFTJ4aps1hr5X/myNf9oOWVrtpw1MiJIAAAAAAAAXCg=="),
        BigHash.createHashFromString("vJDd5L0kWKs4WEnXURm2bnoH/pcdgLQOyF/Pg8bmaR48NJqD6fDByP1a4cBygtHqB6LZ6mkMe4zSAlJ/0pkfgkX9pq4AAAAAAAAIXg=="),
        BigHash.createHashFromString("vKjUYkn5JCtpUmlNVJyWuQ/yGkImuaePChD0nfGLtPbND2SENl+pKvjpNR0ENWfNsMxRUkQPM1D3SxfUP1s116P7MyUAAAAAAAAEFA=="),
        BigHash.createHashFromString("vKgot5BTTSCOg27gpXLpvr7OsSV+fTN7SUU29Yk9MkyylG8IaF4wZyaB3aM49GITfmhTz9TSfTDVasvSPwXCn6zxy8UAAAAAAAAC/A=="),
        BigHash.createHashFromString("vKhNYoGQEniMi9c5QC6quGUiTb16tzGrsXMTUtZFCIlLUhmjJrRbmJS6T3zqkasdTxsVL/N6Snix5+hcYLcw+aecKGMAAAAAAAAZaA=="),
        BigHash.createHashFromString("vLCeH9mkM7KNwlce5I0g9TMuvFHMt8epGhogcvyk7PNw4zSVwomtSF4ovv48OKnrv756QGqJImSh4o6qEmIqSQgMn/QAAAAAAAAI8Q=="),
        BigHash.createHashFromString("vNMgNWRV39066qdTK/Moujy33+Pi0v0PQwtZPqbvWboqx6QTWWDexGU+b7Qd+oU+8zBulb+fbLJ1bzE/JT6S7h1teWAAAAAAAAAmPw=="),
        BigHash.createHashFromString("vDStnoJvQdGUAPWu/JJsSsFYzxi0gsFmb3ZT6kZ6Gvi9y0dV8M6tDhN97bOX/ojy3O0pK3+CXxsVP8tbXMTbcvL1RqIAAAAAAAAIyg=="),
        BigHash.createHashFromString("vEi1FzfhaL2zSHZORjMvKJSqWKwe6gOGKgk7z6hFbk+v56r2lP5PfJS4qQGN2U2HTqHCfoUXsCVM5QztsU4xNFXUfMYAAAAAAAAITQ=="),
        BigHash.createHashFromString("vEjUGZBNBI57Bm3M28jlltqVqpZSwN9YOMTLcva8FsjFyWT1Mr1hSt+iaqurWzfq7eXi+LyWH9v3m0hR3PzhluArn/YAAAAAAAAIHg=="),
        BigHash.createHashFromString("vEhCLn14odWqYU//KmPZJHqKKr16507iBu5MmFLzChkTLjoEEyLXJxnwXOaaCY39TQ6Ml+bZxy28zdqfI8C27vqUNicAAAAAAAAIWA=="),
        BigHash.createHashFromString("vHeGB2QTXvuYvBxH9xCC+KIWaZT3cvlxi4hkBXOn7UERM8/iI8QJYls0C+kIV5TOWRb/+9P31BeqH27oioJfAsOKRZoAAAAAAAAikQ=="),
        BigHash.createHashFromString("vaTE1bNq5ycRvydXmcOfLnTXbV9Kbc7mvpTngvHDkMUyFAFbLNE9sR0FjnSQff2Z4Cdj4W+efjd8dea7Fyrcx4j726sAAAAAAAAICA=="),
        BigHash.createHashFromString("vakxtPwPsn3r1ArWkUgLqJlo+sqlw1/ZeNR8JU6wU5XADxzGHCG/15XusMXYrr9e4XsjGy/QZuSnvZc557p7F9rboYsAAAAAAAAeWQ=="),
        BigHash.createHashFromString("vbyzIc/Yq2iBZOt9P2Mn2EPGCQDuNj7gaWL6hi8saJdyUyRO92hMtt3K6/svDPGL5vtu1yluIVc8lIbhgGPz/TlD/HYAAAAAAAAIGw=="),
        BigHash.createHashFromString("vcZhPA1WbtB504tPzeDrbmFbxCNiPDgQBRY6QgrpeQ/S1vZWVL3WMXuw1L1HuvL/g4o824QilPqpLKZcLwlmyNCFJ3cAAAAAAAAd5Q=="),
        BigHash.createHashFromString("vfE5N+fNQeQfiOnknKEkwitFdamU21UypRqgLFGR/M3B+E0apWiTJ6vTyFXHH0RjlKuL5tTSHLCTiCpINqGjYj/uVroAAAAAAAAIeg=="),
        BigHash.createHashFromString("vQ1ZfkirKRAVIZKtrQGgggHEr0qvP5b8C5ddVZkzVz3Rt80AnPckTV/WXbd2UZFQYDzr74QCU0XaL8kBylmm+KYL7ugAAAAAAAApWw=="),
        BigHash.createHashFromString("vRm4T2elQ1Jsm947MiYsq7PFE49nhtkZCxokDgs91hgvYxYUb4hlUJsfXKeI2mC54DHqGytpR+91IRozn6uiaIJWF0kAAAAAAAAP+w=="),
        BigHash.createHashFromString("vTBnUffqRKDmQNVkLcyc7C/7xdtLCY6+1aIKIZOqdFsVYZwB5jmokdE/lEN7cmg1Av2axH0/FMNAlyo3DXVGaAC0KHYAAAAAAAApYg=="),
        BigHash.createHashFromString("vToFb3cbMFAGw2HcNDccosHnShh8Qx+jDjqdTSjDugOEu2wmharnFv5+/ODnbdXUF+XvbXHVcF74vmjT9LsIxAoLV6MAAAAAAAAr4A=="),
        BigHash.createHashFromString("vX7dEp6ZfUSZuaJbpDh3LANvOiLyyEzrh5Q0odMqb3oRAxSmAD771IpDKTs01Keqo0bVbzvjYF4ufE83+e2BYwF/+hgAAAAAAAAhow=="),
        BigHash.createHashFromString("vom/NTpfA7pJ1r3wDqyxA23ncqM57z9nEEW80kTTd9dDBSaH4+kLKgPUjUnde3D2Zyx7CDphhg2mJsQY22j8lQ6hzW8AAAAAAAADow=="),
        BigHash.createHashFromString("vo2F5Svg3kAiWEpUScxAWfQLOGUpyxLx/ajpJjJNePi2/Ayk0ZkIZIxRSBminNVfG0FPdY6iJBHKopjlsQoTCycahEEAAAAAAAAC/Q=="),
        BigHash.createHashFromString("vo2eFm4O6US6hvhesqRU1Th1m+M5C9PuRGPBnJo7e5UwmmocJexn0VXfjv78rNnudlsrkK0V2kaVamOiCCWnNPUxiZ8AAAAAAAAH9A=="),
        BigHash.createHashFromString("vqM4ksT+daCjoCkmhk5Iw+EavUkmH7hHwIBdRxH0V2SVzz9l7ec/o0G44vRtVZSPpVD3Z1lJC0Vt7PdWzEmice/LhRMAAAAAAAAlmA=="),
        BigHash.createHashFromString("vqsPcYHc4YfPsFp0elTwbT4RFe/cRvw7BQYtxTTtuhoj0BPk762bWh5z+w91epuTO/Ixrvik7AWP+hpInzDefXFqnVEAAAAAAAAIEw=="),
        BigHash.createHashFromString("vqskTu0WtRxmyc8FkpWamKiDrD+nJstYCo7DgC9QYtpkg3RM2yaBbF0fhzfOAubQsJdB9Bkim4cS11EoNgGAwYiVCL0AAAAAAAAdLw=="),
        BigHash.createHashFromString("vq48UHnGAbJWp3avwzGz0++N+sW/YZDJNDs4mzxohYk0xJj07nT4Z+y8uxMnCL7yeHiMvmnoWl+eqcWN/072NJK8lBcAAAAAAAAWEg=="),
        BigHash.createHashFromString("vraYcuNf8OaeRGYscTLjClo2/vOc/356tvEbnCCJUdfABvjOTNShAAEqM9D8ZPsfX/lXAElC/WpbOvaHBKpioQFW5ogAAAAAAAArQg=="),
        BigHash.createHashFromString("vrk1lJ99p4FkkfXdLbfkD+gNOBIgttXE+1Clf69uJ39Kdifl/LRjbSjViUwKQba8MxyBVQcbmlpjNZqigmMKViBGfOAAAAAAAAAISQ=="),
        BigHash.createHashFromString("vr2m6cfI7Gb9pcO8XPNU0+Q8OeTlbEUayqq6BjJXZJpleoJRqRAdfNcROihoJ4AJcel5A3yk3Puy7F1zguF5ZSLDoWgAAAAAAAAIww=="),
        BigHash.createHashFromString("vr056kr17bt59vRKSBWsie1g5Hc8vIpMhIbtkRqk87u0TNA3VnmNDpYizynXjzPOJDJamIQbN1NlbE2+VON+9+lvQUcAAAAAAAAKmQ=="),
        BigHash.createHashFromString("vsGIAi9tkQ+bKLn1pKH84yfmY4fGjzMHJXau4zIa4BQ3MgS3WPI9RJUMMKR8sgOIcAVAYe6kwHlYZuJdCX5dCey/naIAAAAAAAAIYQ=="),
        BigHash.createHashFromString("vsHX4S7a6B20LP1YRnCnFDNNizLSlJO7a7fdEHY/r/+bmYm9bJxSJfj5Ikvxzv9me/umPY3c6nOurt4bI96JegI3PdcAAAAAAAAIFQ=="),
        BigHash.createHashFromString("vsEDmQwrwo9E8EkMW5uYJEg3UcWbxTxXinDbR/JTwiimlX1Yg6sJpHgmECLsaFkxu7tn8FtCW3USly9sosmPM4aLv50AAAAAAAAiVA=="),
        BigHash.createHashFromString("vsF0nP//3LURFDi7Xd3qTJQm3Nxw5y9HmTRLOltyChTOfCsNCEgUa5NMJ2zA/xOaHiAPnfyJ2yA4ee0fU0mG9RgD6O0AAAAAAAAmfg=="),
        BigHash.createHashFromString("vsU9jY8aHHv6u8ZiFfmULPl6R9t5af6kr4qkGy0fQt57WFjIFq9v3cRqhMKiSU3h8DeBaobzmknipHgBPUVjID0QJoEAAAAAAAAIkg=="),
        BigHash.createHashFromString("vtDe8j3eQtiFvrlbonAhRRq2XwAROtmRu35yQxmjljo0b64nZvwMpVTjOZSn22Qb9SuF/ANyudq8ivAV7cGvlaWZ8MoAAAAAAAAqtA=="),
        BigHash.createHashFromString("vtQx35nEN0gM5ZXUdqkrEj8jVeASyviTA4jmhMDWGM1ahA7YbAn5VyUit/0grWGZvhGwfC0VpycDw9H3EpA3oVAdkX4AAAAAAAAg7A=="),
        BigHash.createHashFromString("vtjLo1XJ5Mb6m38XTpJjuVFDLZazHyh1FqTHa01hl9drcucNnMP24v+rzapPpXNE1IE3VM/xKPd2gVAY258Y1bSCeZEAAAAAAAAILw=="),
        BigHash.createHashFromString("vth/vga4vHN/lDSs8FYf82s05xbK0KkeFLRUFtT61HE1pfiYpMohVJmTKzrCHjz0duMYkYWXlE/4241q8N1JOYezRq4AAAAAAAAcLQ=="),
        BigHash.createHashFromString("vvq7T13Z1FkAx+waun3nnLfI3vwY/GxiFijQDmNGYY4JrYpG225kYu3RTokn1KYqa7n4TNJVbwa1X/Tdaa/e0j2ot5MAAAAAAAAIJw=="),
        BigHash.createHashFromString("vgKLitYdphuo44o4KtY/JIYnbQSp1KMalEkJIq/G8TRHHAHe3TKHhJDj9ewP95gEjgJI7UG/O2BFXKyR5iOjKfmYO1oAAAAAAAAjvg=="),
        BigHash.createHashFromString("vgIUdYH2fqUw/L9PBvyE/y+3K7eEA7FHhH0ebflxmvyvUBScpc2S+9ryPxW9DlFp2Uw1TjKvZj7CP0UCYU8imsdzylMAAAAAAAAIdA=="),
        BigHash.createHashFromString("vgJOyPybd/qFYrzjALsv3lXDiA5Slg1lQEbilDMh3YMIexKkog4i2iXUu9s9BUiMnwLHaBvJFif1pEdY8KGg5HAZorYAAAAAAAAIPg=="),
        BigHash.createHashFromString("vhBTtBvV+ruzJoORAg8PsGU0CECUtoN4cJS8I+7q8f2b1v75LNMvh7Ar0GQ6eFRx274JIOPKK5UsCBSJasGaJ8yBAsIAAAAAAAAiQg=="),
        BigHash.createHashFromString("vhyghb/DlH3be9XDseYmMAGa3dYWxehiDhfGVAdwXBApmGK9++XgSxmt/iqd8h075RuexVjxHzYdhs3iAhPO1Y7q2K4AAAAAAAAIHQ=="),
        BigHash.createHashFromString("vhzzYfv7TR2Fxc1hSqPtS3ldQq0XHLRX9dFdgW9vmk9wbHuo1xwvkwPG7J2sJOhSbV3WgjgmRG4WWbk70rwO8fFHD1QAAAAAAAARvA=="),
        BigHash.createHashFromString("vhwWVtIamfhKXWQc/9IOmeco/b+s4NUKWgllFj6ygeMPE9nnT4/ZhbRiPfMC+udYALbb/gg5cRc01xF9l5frwFxfcnAAAAAAAAAafw=="),
        BigHash.createHashFromString("vhxCmjlkGIAAmavMjemtP5KIdJ0dVNXnsS+wOGk3FsSlKGpDKzgoVHrVB5cuYO7AyiAUojFnzFKPogfLZx+Qkek7OPcAAAAAAAAhXg=="),
        BigHash.createHashFromString("vjKDk/R/g0q8tNo+JVdcAv8/JR1Wp2Zj6gW/a9kNfq2ArJDRPTWAS8Z2525cY/ZWFXWJQ8R3UcwDcdseI1f/0jgdoTwAAAAAAAAIkw=="),
        BigHash.createHashFromString("vjI5/pY5bqE6Gf/upBx7CoEmyPW3fYD4NayIgwUhE9GHqvULLLtjYoGaiELzYKWI4DV8CpE0NIAg9dmFTMoFQkUmqgoAAAAAAAAIvw=="),
        BigHash.createHashFromString("vjzRgMiRMbIJq21WEppZzzkD4iPOQ8FG5Qbh7VUaFD/KLzmr7oXuolQT6ghVhx1H4uQ79/IKOi4F4iX28lgSlr4CRHgAAAAAAAAOoA=="),
        BigHash.createHashFromString("vjwTfeHdQ3EDA4tHfQqPcGSqRudROXQwqKufR60jBCC7VTA/JSEsF481fHfMtsnW1V1KlprL6phLILI3uY4rLpbcgqkAAAAAAAAIhw=="),
        BigHash.createHashFromString("vk9V4vApYxiHV3/MPB3kDT2ccTwY/tI1fecuduim4F7BuqZLSFLY252Za/Uz4Xio1itTQAPX079FEN4EgJEHvKFZhFEAAAAAAAAOvA=="),
        BigHash.createHashFromString("vlfzW95NfXCcl1PDPIJQnarRCXUO4GJPDTcUs/YQqUzxduCHXSPXDkdKGCn0umjw2LNGRZbegrQ9rkK//eAVHMuLuGsAAAAAAAAIZQ=="),
        BigHash.createHashFromString("vl96BcAVLDIPcrbhi0TEnZscEZc7w4AMlaHVZWLAykya0sFFyLhkRLYnmQhrDgqtWeEENfmTkCva/ooQiFlR6WK9U6MAAAAAAAAoQA=="),
        BigHash.createHashFromString("vmOQajnH5cBvpbYJXe/DW+KD4eyL1oWtCQ1UvlVzQ0Ppz3CR/OQrNZprZVPxj+71VMRT4hxZK6ujnJG5mawpkung7/cAAAAAAAAIuw=="),
        BigHash.createHashFromString("vmepfo+PXfEtT+t0tvBUZDFVpT0tA65IDsTzStDkMtoHbUs0CupGNc8rrgvmMNpUQzlqLLaQWPcQYlXq9J8pummSVu8AAAAAAAAIcg=="),
        BigHash.createHashFromString("vmzFl4AAd2h73csURLBGsQcpJzwa54zQFFnahGxNMvSOWtpPrAhY24a8cVIh2Lvq+ERm3o2t6vJ1iB9i7p5GQLLzATQAAAAAAAAI5A=="),
        BigHash.createHashFromString("vnDhCboeuPavHFrYu7NwT1/mqBjuHnqZG7ut3L3mA7FFHUzKBLl2Z81+nMy0CCv19+aM9ZLJEyChQJSNtEKohM419ecAAAAAAAAp0Q=="),
        BigHash.createHashFromString("vnAE5Exc9rfHOqY/j7J1y3zTWPlIXwq1zWXmJ1nOkB2S7E2TLX6xTl7OFhtFhmQ9o/609wgSfK62Gzpfv1ORpLRJLygAAAAAAAAI0g=="),
        BigHash.createHashFromString("vngKomH0393b8JoJZQv3M5lmVV/aMdALj24NRKwjPVzoL34Ou35V0RBtQEQFdEowPmZWZlyd6AHL3a1AtDu9Df0UpMgAAAAAAAAb6g=="),
        BigHash.createHashFromString("vnvSg2HVr9+cgQYBGKzPlIqCOCxzYZl75SZZ3NyReOB0NiOPDSViDrknXsn3R9o2ySAPbiQ5e24v1GGRiq0/rQrnxFQAAAAAAAAIXw=="),
        BigHash.createHashFromString("vnvjgXX6i9N6A884F8ZpZZzuZaW0wbv4N4aV5zm7UGOxSJUhjYyQDBPJDKS+Gl99p0MwjHljIl4T2w6XUR/orZbO+9MAAAAAAAAIuQ=="),
        BigHash.createHashFromString("v4bXhoiYRUjTxyyCSyv/8iA0SoAZoXIUF2sdkLJMeElPuv8UHRBgEUbPDNJOCIVgDBpQG36MnYgrBqs/FTFtyzp9XWQAAAAAAAARLw=="),
        BigHash.createHashFromString("v7r+AlHIHxc9NW41IXBTLaK383A/hm0EjwrPas4hEzAmjQR3yZp19S19SC607cKeHg6oz/pcqCKJG861g22etSr/HLUAAAAAAAAd0g=="),
        BigHash.createHashFromString("v9Vp1IL+hb81EF+f9zCbsalqG1cGuMOWsNGavgrb1ojHBYUkY/rAp1suGjYKAd8dqVjFuJC5gFAlV03aClStBMLN+48AAAAAAAAIZg=="),
        BigHash.createHashFromString("v974JpOHDtTDWd3anlds7NKk7AYD+wjbxhj6JCg8alvlsu5qoNX8Ba+9Er5WZmlKFlW/lWgBv7gZynNdXwjKnTVWTIYAAAAAAAAuHw=="),
        BigHash.createHashFromString("vwG0fOEc9a3URzIr0us1uOP8gD08cuBjRIKSp4lg2FdSD3EtTRahRSS2x8FO8q5sRtxkSBV9dO1SWDnDCN8u3hNGp0AAAAAAAAAc7w=="),
        BigHash.createHashFromString("vxyv0d8squNMImbBA+/WY6mQhXn0imCMwD5WpbhuMB7UUTxjM0SgqJgBa+9z8zqzNgpN8rXidbFBrtS3EpZNgcjD5PMAAAAAAAAaqg=="),
        BigHash.createHashFromString("v2NldEdIPBWyLMToploe2/qi5BxXBoxKw/nELUA+NOEJOzUz5LBmt0cWkEE30fR9a1cRf2AUwHVXhpKybP2NL3f44E0AAAAAAAAIFQ=="),
        BigHash.createHashFromString("v3ThFHSFIIILsH/Nw7SxqrbXrCCfLxwOSZyE3FZaxJ00KkY1VwQkL8IU+scx3KVqPcIzdke1RC2JM7xxmuF53JHU8oAAAAAAAAAkJg==")
    };
    /**
     * <p>Put all encrypted project info here. Duh, don't commit!</p>
     */
    final static EncryptedProject[] encryptedProjects = {
        // Just put the following for all the relevant projects here:
        //new EncryptedProject(BigHash.createHashFromString(""), 
    };

    /**
     * 
     * @param args
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {

        try {
            ProteomeCommonsTrancheConfig.load();
            NetworkUtil.waitForStartup();

            int onlineCoreCount = 0;

            for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {
                if (row.isOnline()) {
                    onlineCoreCount++;
                }
            }

            System.out.println("Total of " + onlineCoreCount + " online, core servers.");

            // Step 1: collect all project hashes from servers
            final Set<BigHash> projectHashes = getAllProjectHashes();
            System.out.println("Found a total of " + projectHashes.size() + " projects.");

            // Step 2: one project at a time, get all the meta data and try to identify any data chunks that match
            int count = 0;
            final int maxAttempts = 3;
            final String tab = "    ";
            for (BigHash projectHash : projectHashes) {
                count++;
                System.out.println("Checking project #" + count + " of " + projectHashes.size() + ": " + projectHash);
                System.out.println(tab + "Started: " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()));
                ATTEMPT:
                for (int attempt = 0; attempt < maxAttempts; attempt++) {
                    try {

                        String passphrase = getPassphraseForProject(projectHash);

                        // Get project meta data to see whether any chunks in project file
                        MetaData projectMD = getMetaData(projectHash);

                        lookForDataChunks(projectHash, projectHash, projectMD);
                        
                        if (passphrase == null) {
                            System.out.println(tab+"Not using passphrase; not found.");
                        } else {
                            System.out.println(tab+"Using passphrase (len: "+passphrase.length()+")");
                        }

                        if (projectMD.isEncrypted() && !projectMD.isPublicPassphraseSet()) {
                            System.err.println(tab + "Skipping project. Passphrase protected, but no public passphrase set.");
                            break ATTEMPT;
                        }

                        ProjectFile pf = getProjectFile(projectHash, passphrase);

                        for (ProjectFilePart pfp : pf.getParts()) {
                            MetaData fileMD = getMetaData(pfp.getHash());
                            lookForDataChunks(projectHash, pfp.getHash(), fileMD);
                        }

                        break ATTEMPT;
                    } catch (Exception e) {
                        System.err.println(tab + "Attempt #" + (attempt + 1) + " of " + maxAttempts + " -- " + e.getClass().getSimpleName() + ": " + e.getMessage());
//                        e.printStackTrace(System.err);
                    }
                }
            } // For each project file
        } finally {
        }
    }

    /**
     * 
     * @param hash
     * @return
     */
    private static String getPassphraseForProject(BigHash hash) {

        for (EncryptedProject ep : encryptedProjects) {
            if (ep.hash.equals(hash)) {
                return ep.passphrase;
            }
        }
        
        return null;
    }

    /**
     * 
     * @param projectHash
     * @param md
     */
    private static void lookForDataChunks(BigHash projectHash, BigHash fileHash, MetaData md) {
        META_DATA:
        for (BigHash chunkHash : md.getParts()) {
            for (BigHash failedHash : dataChunksThatDoNotVerify) {
                if (chunkHash.equals(failedHash)) {
                    System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
                    System.out.println(" Found chunk that fails verification:");
                    System.out.println("    - Project...... " + projectHash);
                    System.out.println("    - File......... " + fileHash);
                    System.out.println("    - Data chunk... " + failedHash);
                    System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
//                    System.out.println("Project <" + projectHash + ">, file <" + fileHash + "> contains data chunk that fails verification: " + failedHash);
                    continue META_DATA;
                }
            }
        }
    }

    /**
     * 
     * @param hash
     * @return
     * @throws java.lang.Exception
     */
    private static ProjectFile getProjectFile(BigHash hash, String passphrase) throws Exception {

        try {
            GetFileTool gft = new GetFileTool();
            gft.setHash(hash);
            if (passphrase != null) {
                gft.setPassphrase(passphrase);
            }

            return gft.getProjectFile();
        } finally {
        }
    }

    /**
     * 
     * @param hash
     * @return
     * @throws java.lang.Exception
     */
    private static MetaData getMetaData(BigHash hash) throws Exception {
        try {
            GetFileTool gft = new GetFileTool();
            gft.setHash(hash);

            return gft.getMetaData();
        } finally {
        }
    }

    /**
     * 
     * @return
     */
    private static Set<BigHash> getAllProjectHashes() {
        Set<BigHash> projectHashes = new HashSet();

        for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {
            final boolean isConnected = ConnectionUtil.isConnected(row.getHost());
            if (row.isOnline()) {
                ATTEMPT:
                for (int attempt = 0; attempt < 3; attempt++) {
                    TrancheServer ts = null;
                    try {
                        if (!isConnected) {
                            ts = ConnectionUtil.connectHost(row.getHost(), true);
                        } else {
                            ts = ConnectionUtil.getHost(row.getHost());
                        }

                        BigInteger offset = BigInteger.ZERO;
                        BigInteger batch = BigInteger.valueOf(100);

                        while (true) {
                            BigHash[] hashes = ts.getProjectHashes(offset, batch);

                            if (hashes.length == 0) {
                                break;
                            }

                            for (BigHash h : hashes) {
                                projectHashes.add(h);
                            }
                            offset = offset.add(batch);
                        }

                        break ATTEMPT;
                    } catch (Exception e) {
                        System.err.println(e.getClass().getSimpleName() + " occurred while injecting chunk to " + row.getHost() + ": " + e.getMessage());
                        e.printStackTrace(System.err);
                    } finally {
                        if (!isConnected) {
                            ConnectionUtil.unlockConnection(row.getHost());
                            ConnectionUtil.safeCloseHost(row.getHost());
                        }
                    }
                } // Attempt loop
            } // If online
        } // For each server

        return projectHashes;
    } // getAllProjectHashes
}

/**
 * <p>Simple way to associate hashes and passphrases.</p>
 * @author Tranche
 */
class EncryptedProject {

    public final BigHash hash;
    public final String passphrase;

    public EncryptedProject(BigHash hash, String passphrase) {
        this.hash = hash;
        this.passphrase = passphrase;
    }
}
