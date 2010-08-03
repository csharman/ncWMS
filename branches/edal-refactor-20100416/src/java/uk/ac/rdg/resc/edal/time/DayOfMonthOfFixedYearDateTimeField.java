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

import org.joda.time.DateTimeField;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DurationField;
import org.joda.time.ReadablePartial;
import org.joda.time.field.PreciseDurationDateTimeField;

/**
 * A {@link DateTimeField} that represents the day in the month within a fixed-length
 * year.  This is a {@link PreciseDurationDateTimeField} because a day is a precise
 * duration, even though a month is not.
 * @author Jon
 */
final class DayOfMonthOfFixedYearDateTimeField extends PreciseDurationDateTimeField {
    
    private final FixedYearVariableMonthChronology chron;
    
    public DayOfMonthOfFixedYearDateTimeField(FixedYearVariableMonthChronology chron) {
        super(DateTimeFieldType.dayOfMonth(), chron.days());
        this.chron = chron;
    }

    @Override
    public int get(long instant) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getMaximumValue() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getMaximumValue(long instant) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getMaximumValue(ReadablePartial partial) {
        throw new UnsupportedOperationException("Not supported yet.");
        
    }

    @Override
    public int getMaximumValue(ReadablePartial partial, int[] values) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DurationField getRangeDurationField() {
        return this.chron.months();
    }

}
