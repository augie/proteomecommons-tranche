/*
 * ServerCacheUtilTest.java
 * JUnit based test
 *
 * Created on January 21, 2008, 4:52 PM
 */
package org.proteomecommons.tranche.serverlogs;

import junit.framework.*;
import org.tranche.util.Text;

/**
 *
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 */
public class ServerCacheUtilTest extends TestCase {

    public ServerCacheUtilTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public void testconvertIPToSafeName() throws Exception {
        // Test two valid IPs
        String safe1 = ServerCacheUtil.convertIPToSafeName("127.0.0.1:1500");
        assertEquals("Expecting match.", "127-0-0-1-1500", safe1);

        String safe2 = ServerCacheUtil.convertIPToSafeName("211.45.117.41:443");
        assertEquals("Expecting match.", "211-45-117-41-443", safe2);

        // Test two URLS
        String safe3 = ServerCacheUtil.convertIPToSafeName("tranche://127.0.0.1:1500");
        assertEquals("Expecting match.", "127-0-0-1-1500", safe3);

        String safe4 = ServerCacheUtil.convertIPToSafeName("ssl+tranche://211.45.117.41:443");
        assertEquals("Expecting match.", "211-45-117-41-443", safe4);


        // Test utterly wrong until IP starts
        String safe5 = ServerCacheUtil.convertIPToSafeName("bogusbogus+bogus!!!!127.0.0.1:1");
        assertEquals("Expecting match.", "127-0-0-1-1", safe5);
    }

    /**
     *
     */
    public void testParseMonthsAndYear() throws Exception {

        final long current = System.currentTimeMillis();
        final long deltaThreeYears = -((long) 1000 * 60 * 60 * 24 * 365 * 3);
        final long threeYearsAgoToday = current + deltaThreeYears;

        System.out.println("Today... " + Text.getFormattedDate(current));
        System.out.println("Delta (3 years)... " + deltaThreeYears);
        System.out.println("Three years ago... " + Text.getFormattedDate(threeYearsAgoToday));

        long testTime = threeYearsAgoToday;

        // Used for debugging to make sure terminates.
        long count = 0;

        int month;
        int year;
        while (testTime < current && count < 50) {

            month = ServerCacheUtil.extractMonthFromTimestamp(testTime);
            year = ServerCacheUtil.extractYearFromTimestamp(testTime);

            System.out.println(Text.getFormattedDate(testTime) + " => Month: " + month + ", Year: " + year);

            // Add 25 days
            testTime += ((long) 1000 * 60 * 60 * 24 * 25);
            count++;
        }
        System.out.println("Quit on count " + count);
    }
}
