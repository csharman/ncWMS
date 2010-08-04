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
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Abstract superclass for testing {@link FixedYearLengthChronology}s.
 * @author Jon
 */
public abstract class AbstractFixedYearChronologyTest
{ 
    private final Chronology chron;
    private final DateTimeFormatter formatter;

    /** Number of milliseconds in a second */
    protected static final long SECOND = 1000;
    /** Number of milliseconds in a minute */
    protected static final long MINUTE = 60 * SECOND;
    /** Number of milliseconds in an hour */
    protected static final long HOUR   = 60 * MINUTE;
    /** Number of milliseconds in a day */
    protected static final long DAY    = 24 * HOUR;

    public AbstractFixedYearChronologyTest(Chronology chron)
    {
        this.chron = chron;
        this.formatter = ISODateTimeFormat.dateTime()
            .withChronology(chron)
            .withZone(DateTimeZone.UTC);
    }

    /**
     * Test of zero millisecond offset (1970-01-01)
     */
    @Test
    public void test1970() {
        System.out.println("1970");
        testDateTime(1970, 1, 1, 0, 0, 0, 0);
    }

    /**
     * Test of one year's worth of millisecond offset (1971-01-01)
     */
    @Test
    public void test1971() {
        System.out.println("1971");
        testDateTime(1971, 1, 1, 0, 0, 0, 0);
    }

    @Test
    public void test1969() {
        System.out.println("1969");
        testDateTime(1969, 1, 1, 0, 0, 0, 0);
    }

    @Test
    public void test1969AndAMillisecond() {
        System.out.println("1969 + 1ms");
        testDateTime(1969, 1, 1, 0, 0, 0, 1);
    }

    @Test
    public void testZeroYear() {
        System.out.println("1st Jan, 0000");
        testDateTime(0, 1, 1, 0, 0, 0, 0);
    }

    /**
     * Creates a DateTime from the given fields and checks that the field values
     * are preserved.  Independently checks the calculation of the millisecond
     * offset.  Formats the resulting DateTime as an ISO String, then
     * parses the string, checking that the result matches the original DateTime.
     */
    protected final void testDateTime(int year, int monthOfYear, int dayOfMonth,
        int hourOfDay, int minuteOfHour, int secondOfMinute, int millisOfSecond)
    {
        // Create a DateTime from the given fields
        DateTime dt = new DateTime(year, monthOfYear, dayOfMonth, hourOfDay,
            minuteOfHour, secondOfMinute, millisOfSecond, this.chron);
        long millis = this.getMillis(year, monthOfYear, dayOfMonth, hourOfDay,
            minuteOfHour, secondOfMinute, millisOfSecond);
        System.out.println("millis = " + millis);
        assertEquals(millis, dt.getMillis());

        // Check that all the fields are the same
        assertEquals(year,           dt.getYear());
        assertEquals(monthOfYear,    dt.getMonthOfYear());
        assertEquals(dayOfMonth,     dt.getDayOfMonth());
        assertEquals(hourOfDay,      dt.getHourOfDay());
        assertEquals(minuteOfHour,   dt.getMinuteOfHour());
        assertEquals(secondOfMinute, dt.getSecondOfMinute());
        assertEquals(millisOfSecond, dt.getMillisOfSecond());

        // Do a round-trip format and parse
        String isoString = this.formatter.print(dt);
        System.out.println(isoString);
        long parsedMillis = this.formatter.parseMillis(isoString);
        assertEquals(dt.getMillis(), parsedMillis);
    }

    /**
     * Provides an independent calculation of the number of milliseconds since
     * the datum given by these fields.
     */
    protected final long getMillis(int year, int monthOfYear, int dayOfMonth,
        int hourOfDay, int minuteOfHour, int secondOfMinute, int millisOfSecond)
    {
        int dayOfYear = this.getDayOfYear(monthOfYear, dayOfMonth);
        return (year - 1970)     * this.getMillisInYear()   +
               (dayOfYear - 1)   * DAY    +
               hourOfDay         * HOUR   +
               minuteOfHour      * MINUTE +
               secondOfMinute    * SECOND +
               millisOfSecond;
    }

    protected final long getMillisInYear()
    {
        return this.getNumDaysInYear() * DAY;
    }

    /**
     * Returns the number of days in the year in the chronology under test.
     * This must not use the Chronology itself to provide a figure: it must be
     * independent.
     */
    protected abstract int getNumDaysInYear();

    /**
     * Calculates the (one-based) day of the year from the given month of the
     * year and the day of the month (both one-based)
     */
    protected abstract int getDayOfYear(int monthOfYear, int dayOfMonth);

}
