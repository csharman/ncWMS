/*
 * Copyright (c) 2010 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.edal.time;

import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.IllegalFieldValueException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test for the {@link ThreeSixtyDayChronology}.
 * @author Jon
 */
public final class ThreeSixtyDayChronologyTest extends AbstractFixedYearChronologyTest {

    private static Chronology CHRON_360 = ThreeSixtyDayChronology.getInstanceUTC();

    private static final DateTime SAMPLE = new DateTime(2000, 1, 2, 3, 4, 5, 6, CHRON_360);

    private static final long MONTH  = 30 * DAY;
    private static final long YEAR   = 12 * MONTH;

    public ThreeSixtyDayChronologyTest() {
        super(CHRON_360);
    }

    @Test
    public void testFeb30_2000() {
        System.out.println("Feb 30, 2000");
        testDateTime(2000, 2, 30, 0, 0, 0, 0);
    }

    @Test
    public void testNegativeYear() {
        System.out.println("Feb 30, -1");
        testDateTime(-1, 2, 30, 0, 0, 0, 0);
    }

    @Test
    public void testVeryNegativeYear() {
        System.out.println("Feb 30, -2000");
        testDateTime(-2000, 2, 30, 0, 0, 0, 0);
    }

    @Test
    public void testDateTime() {
        System.out.println("Feb 30, 2000, 13:45:56.789");
        testDateTime(2000, 2, 30, 13, 45, 56, 789);
    }

    @Test(expected=IllegalFieldValueException.class)
    public void testMillisOverflow() {
        SAMPLE.withMillisOfSecond(1000);
    }

    @Test(expected=IllegalFieldValueException.class)
    public void testMillisUnderflow() {
        SAMPLE.withMillisOfSecond(-1);
    }

    @Test(expected=IllegalFieldValueException.class)
    public void testSecondsOverflow() {
        SAMPLE.withSecondOfMinute(60);
    }

    @Test(expected=IllegalFieldValueException.class)
    public void testSecondsUnderflow() {
        SAMPLE.withSecondOfMinute(-1);
    }

    @Test(expected=IllegalFieldValueException.class)
    public void testMinutesOverflow() {
        SAMPLE.withMinuteOfHour(60);
    }

    @Test(expected=IllegalFieldValueException.class)
    public void testMinutesUnderflow() {
        SAMPLE.withMinuteOfHour(-1);
    }

    @Test(expected=IllegalFieldValueException.class)
    public void testHoursOverflow() {
        SAMPLE.withHourOfDay(24);
    }

    @Test(expected=IllegalFieldValueException.class)
    public void testHoursUnderflow() {
        SAMPLE.withHourOfDay(-1);
    }

    @Test(expected=IllegalFieldValueException.class)
    public void testDayOfMonthOverflow() {
        SAMPLE.withMonthOfYear(2).withDayOfMonth(31);
    }

    @Test(expected=IllegalFieldValueException.class)
    public void testDayOfMonthOverflow2() {
        // Pick a month that usually has 31 days
        SAMPLE.withMonthOfYear(1).withDayOfMonth(31);
    }

    @Test(expected=IllegalFieldValueException.class)
    public void testDayOfMonthUnderflow() {
        SAMPLE.withDayOfMonth(0);
    }

    @Test(expected=IllegalFieldValueException.class)
    public void testMonthOfYearOverflow() {
        SAMPLE.withMonthOfYear(13);
    }

    @Test(expected=IllegalFieldValueException.class)
    public void testMonthOfYearUnderflow() {
        SAMPLE.withMonthOfYear(0);
    }

    @Test
    public void setFields() {
        int year = 1;
        int month = 12;
        int day = 30;
        int hour = 23;
        int minute = 59;
        int second = 58;
        int millis = 999;
        assertEquals(year, SAMPLE.withYear(year).getYear());
        assertEquals(month, SAMPLE.withMonthOfYear(month).getMonthOfYear());
        assertEquals(day, SAMPLE.withDayOfMonth(day).getDayOfMonth());
        assertEquals(hour, SAMPLE.withHourOfDay(hour).getHourOfDay());
        assertEquals(minute, SAMPLE.withMinuteOfHour(minute).getMinuteOfHour());
        assertEquals(second, SAMPLE.withSecondOfMinute(second).getSecondOfMinute());
        assertEquals(millis, SAMPLE.withMillisOfSecond(millis).getMillisOfSecond());
        assertEquals(millis, SAMPLE.withMillisOfDay(millis).getMillisOfDay());
    }

    @Test
    public void testArithmetic() {
        long millis = SAMPLE.getMillis();
        assertEquals(millis + 4 * YEAR, SAMPLE.year().addToCopy(4).getMillis());
        assertEquals(millis + 4 * MONTH, SAMPLE.monthOfYear().addToCopy(4).getMillis());
        assertEquals(millis + 4 * DAY, SAMPLE.dayOfMonth().addToCopy(4).getMillis());
        assertEquals(millis + 4 * HOUR, SAMPLE.hourOfDay().addToCopy(4).getMillis());
        assertEquals(millis + 4 * MINUTE, SAMPLE.minuteOfHour().addToCopy(4).getMillis());
        assertEquals(millis + 4 * SECOND, SAMPLE.secondOfMinute().addToCopy(4).getMillis());
        assertEquals(millis + 4 , SAMPLE.millisOfSecond().addToCopy(4).getMillis());

        DateTime yearOne = SAMPLE.withYear(1);
        millis = yearOne.getMillis();
        assertEquals(millis - 4 * YEAR, yearOne.year().addToCopy(-4).getMillis());
        System.out.println(yearOne);

    }

    @Override
    protected int getNumDaysInYear() { return 360; }

    @Override
    protected int getDayOfYear(int monthOfYear, int dayOfMonth) {
        return (monthOfYear - 1) * 30 + dayOfMonth;
    }

}