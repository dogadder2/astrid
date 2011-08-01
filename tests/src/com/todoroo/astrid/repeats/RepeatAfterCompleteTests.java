package com.todoroo.astrid.repeats;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.google.ical.values.Weekday;
import com.google.ical.values.WeekdayNum;
import com.todoroo.andlib.test.TodorooTestCase;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;

public class RepeatAfterCompleteTests extends TodorooTestCase {

    private Task task;
    private long nextDueDate;
    private RRule rrule;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        task = new Task();
        rrule = new RRule();
    }

    // --- date with time tests

    public void testSubDailyFreqs() throws ParseException {
        task.setValue(Task.DUE_DATE, DateUtilities.now() - DateUtilities.ONE_WEEK);
        task.setFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION, true);

        for(Frequency freq : Frequency.values()) {
            long interval = -1;
            switch(freq) {
            case MINUTELY:
                interval = DateUtilities.ONE_MINUTE; break;
            case HOURLY:
                interval = DateUtilities.ONE_HOUR; break;
            default:
                continue;
            }

            buildRRule(1, freq);
            nextDueDate = RepeatTaskCompleteListener.computeNextDueDate(task, rrule.toIcal());
            assertDateTimeEquals(freq.toString() + "x1", DateUtilities.now() + interval, nextDueDate);

            buildRRule(6, freq);
            nextDueDate = RepeatTaskCompleteListener.computeNextDueDate(task, rrule.toIcal());
            assertDateTimeEquals(freq.toString() + "x6", DateUtilities.now() + 6 * interval, nextDueDate);
        }
    }

    public void testDailyAndGreaterFreqs() throws ParseException {
        task.setValue(Task.DUE_DATE, DateUtilities.now() - DateUtilities.ONE_WEEK);
        task.setFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION, true);

        for(int interval = 1; interval < 7; interval++) {
            for(Frequency freq : Frequency.values()) {
                long next = DateUtilities.now();
                switch(freq) {
                case DAILY:
                    next += interval * DateUtilities.ONE_DAY; break;
                case WEEKLY:
                    next += interval * DateUtilities.ONE_WEEK; break;
                case MONTHLY:
                    next = DateUtilities.addCalendarMonthsToUnixtime(next, interval); break;
                case YEARLY:
                    next = DateUtilities.addCalendarMonthsToUnixtime(next, interval * 12); break;
                default:
                    continue;
                }

                buildRRule(interval, freq);
                nextDueDate = RepeatTaskCompleteListener.computeNextDueDate(task, rrule.toIcal());
                assertDateEquals(freq.toString() + "x" + interval, next, nextDueDate);
            }
        }
    }


    // --- helpers

    private void buildRRule(int interval, Frequency freq, Weekday... weekdays) {
        rrule.setInterval(interval);
        rrule.setFreq(freq);
        setRRuleDays(rrule, weekdays);
    }

    public static void assertDateTimeEquals(String message, long expected, long actual) {
        expected = expected / 1000L * 1000;
        actual = actual / 1000L * 1000;
        assertEquals(message + ": Expected: " + new Date(expected) + ", Actual: " + new Date(actual),
                expected, actual);
    }

    private void assertDateEquals(String message, long expected, long actual) {
        expected = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, expected);
        actual = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, actual);
        assertEquals("Due Date is '" + DateUtilities.getDateStringWithWeekday(getContext(), new Date(actual))
                + "', expected '" + DateUtilities.getDateStringWithWeekday(getContext(), new Date(expected)) + "'",
                expected, actual);
    }

    private void setRRuleDays(RRule rrule, Weekday... weekdays) {
        ArrayList<WeekdayNum> days = new ArrayList<WeekdayNum>();
        for(Weekday wd : weekdays)
            days.add(new WeekdayNum(0, wd));
        rrule.setByDay(days);
    }

}