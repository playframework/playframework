/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */
package play.data.format;

import org.junit.Test;
import play.data.format.Formats.LocalDateFormatter;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

/** Junit test of various formats handled in the Formats class. */
public class FormatsTest {
  static final String DATE_TOO_OLD = "-0023-08-08";
  static final String DATE_TOO_FUTURISTIC = "10000-01-01";
  static final LocalDate OLD = LocalDate.of(0001, 02, 10);
  static final String DATE_OLD = "0001-02-10";
  static final LocalDate RECENT_PAST = LocalDate.of(1975, 10, 25);
  static final String DATE_RECENT_PAST = "1975-10-25";
  static final LocalDate NOW = LocalDate.now();
  static final String DATE_TODAY = NOW.toString();
  static final LocalDate FUTURE = LocalDate.of(2100, 12, 12);
  static final String DATE_FUTURE = "2100-12-12";

  static final String DATE_RANGE_ERROR_MONTH = "2018-13-20";
  static final String DATE_RANGE_ERROR_DAY = "2018-09-31";

  static final Locale LOCAL = Locale.getDefault();

  @Test
  public void testLocalDateFormatterValid() throws ParseException {
    final LocalDateFormatter formatter = new LocalDateFormatter();

    assertEquals(OLD, formatter.parse(DATE_OLD, LOCAL));
    assertEquals(RECENT_PAST, formatter.parse(DATE_RECENT_PAST, LOCAL));
    assertEquals(NOW, formatter.parse(DATE_TODAY, LOCAL));
    assertEquals(FUTURE, formatter.parse(DATE_FUTURE, LOCAL));
  }

  @Test(expected = ParseException.class)
  public void testInvalidMonthForLocalDate() throws ParseException {
    final LocalDateFormatter formatter = new LocalDateFormatter();
    LocalDate result = formatter.parse(DATE_RANGE_ERROR_MONTH, LOCAL);
    System.out.println("The result sys-out, " + result + ", should never be reached");
  }

  @Test(expected = ParseException.class)
  public void testInvalidDayForLocalDate() throws ParseException {
    final LocalDateFormatter formatter = new LocalDateFormatter();
    LocalDate result = formatter.parse(DATE_RANGE_ERROR_DAY, LOCAL);
    System.out.println("The result sys-out, " + result + ", should never be reached");
  }

  @Test(expected = ParseException.class)
  public void testTooOldDateForLocalDate() throws ParseException {
    final LocalDateFormatter formatter = new LocalDateFormatter();
    LocalDate result = formatter.parse(DATE_TOO_OLD, LOCAL);
    System.out.println("The result sys-out, " + result + ", should never be reached");
  }

  @Test(expected = ParseException.class)
  public void testTooFuturisticDateForLocalDate() throws ParseException {
    final LocalDateFormatter formatter = new LocalDateFormatter();
    LocalDate result = formatter.parse(DATE_TOO_FUTURISTIC, LOCAL);
    System.out.println("The result sys-out, " + result + ", should never be reached");
  }
}
