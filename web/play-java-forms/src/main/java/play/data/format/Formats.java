/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data.format;

import java.text.*;
import java.util.*;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import play.i18n.Lang;
import play.i18n.MessagesApi;

/** Defines several default formatters. */
public class Formats {

  // -- DATE

  /** Formatter for <code>java.util.Date</code> values. */
  public static class DateFormatter extends Formatters.SimpleFormatter<Date> {

    private final MessagesApi messagesApi;

    private final String pattern;

    private final String patternNoApp;

    /**
     * Creates a date formatter. The value defined for the message file key "formats.date" will be
     * used as the default pattern.
     *
     * @param messagesApi messages to look up the pattern
     */
    public DateFormatter(MessagesApi messagesApi) {
      this(messagesApi, "formats.date");
    }

    /**
     * Creates a date formatter.
     *
     * @param messagesApi messages to look up the pattern
     * @param pattern date pattern, as specified for {@link SimpleDateFormat}. Can be a message file
     *     key.
     */
    public DateFormatter(MessagesApi messagesApi, String pattern) {
      this(messagesApi, pattern, "yyyy-MM-dd");
    }

    /**
     * Creates a date formatter.
     *
     * @param messagesApi messages to look up the pattern
     * @param pattern date pattern, as specified for {@link SimpleDateFormat}. Can be a message file
     *     key.
     * @param patternNoApp date pattern to use as fallback when no app is started.
     */
    public DateFormatter(MessagesApi messagesApi, String pattern, String patternNoApp) {
      this.messagesApi = messagesApi;
      this.pattern = pattern;
      this.patternNoApp = patternNoApp;
    }

    /**
     * Binds the field - constructs a concrete value from submitted data.
     *
     * @param text the field text
     * @param locale the current <code>Locale</code>
     * @return a new value
     */
    public Date parse(String text, Locale locale) throws java.text.ParseException {
      if (text == null || text.trim().isEmpty()) {
        return null;
      }
      Lang lang = new Lang(locale);
      SimpleDateFormat sdf =
          new SimpleDateFormat(
              Optional.ofNullable(this.messagesApi)
                  .map(messages -> messages.get(lang, pattern))
                  .orElse(patternNoApp),
              locale);
      sdf.setLenient(false);
      return sdf.parse(text);
    }

    /**
     * Unbinds this fields - converts a concrete value to a plain string.
     *
     * @param value the value to unbind
     * @param locale the current <code>Locale</code>
     * @return printable version of the value
     */
    public String print(Date value, Locale locale) {
      if (value == null) {
        return "";
      }
      Lang lang = new Lang(locale);
      return new SimpleDateFormat(
              Optional.ofNullable(this.messagesApi)
                  .map(messages -> messages.get(lang, pattern))
                  .orElse(patternNoApp),
              locale)
          .format(value);
    }
  }

  /**
   * Formatter for <code>java.time.LocalDate</code> values. {@link LocalDate} toString method, to
   * which the LocalDateFormatter print method directly defers, results in a date complying with
   * ISO-8601 format {@code uuuu-MM-dd}. For this reason, this is used as the expected format of the
   * input argument to the parse method. Dates before 0000, or after 9999 are not supported in this
   * implementation.
   */
  public static class LocalDateFormatter extends Formatters.SimpleFormatter<LocalDate> {
    /**
     * As per LocalDate.toString, the expected format for LocalDate is the ISO-8601 format {@code
     * uuuu-MM-dd}. Dates before 0000, or after 9999 are not supported in this implementation.
     */
    private static final Pattern LOCAL_DATE_PATTERN =
        Pattern.compile("(\\d\\d\\d\\d)-(\\d\\d)-(\\d\\d)");

    /**
     * Binds the field - constructs a concrete value from submitted data. The expected format for
     * LocalDate is the ISO-8601 format {@code uuuu-MM-dd}. Dates before 0000, or after 9999 are not
     * supported in this implementation.
     *
     * @param text the field text
     * @param locale the current <code>Locale</code>
     * @return a new value
     */
    @Override
    public LocalDate parse(String text, Locale locale) throws ParseException {
      Matcher m = LOCAL_DATE_PATTERN.matcher(text);
      if (!m.matches()) throw new ParseException("No valid Input for date text: " + text, 0);
      return parse(m.group(1), m.group(2), m.group(3));
    }

    /**
     * Helper method; Builds a LocalDate object based on the provided input. Wraps the
     * DateTimeException with the ParseException thrown by the caller.
     *
     * @param year the year component of the date (0000-9999)
     * @param month the month component of the date (1-12)
     * @param dayOfMonth the day component of the date (1-31)
     * @return a LocalDate instance
     */
    private static LocalDate parse(String year, String month, String dayOfMonth)
        throws ParseException {
      try {
        return LocalDate.of(
            Integer.valueOf(year), Integer.valueOf(month), Integer.valueOf(dayOfMonth));
      } catch (DateTimeException e) {
        throw new ParseException(e.getMessage(), 0);
      }
    }

    /**
     * Unbinds this fields - converts a concrete value to a plain string.
     *
     * @param value the value to unbind
     * @param locale the current <code>Locale</code>
     * @return printable version of the value
     */
    @Override
    public String print(LocalDate value, Locale locale) {
      return value
          .toString(); // Local can be ignored in the case of LocalDate ; format is always ISO-8601.
                       // i.e., uuuu-MM-dd
    }
  }

  /** Defines the format for a <code>Date</code> field. */
  @Target({FIELD})
  @Retention(RUNTIME)
  @play.data.Form.Display(
      name = "format.date",
      attributes = {"pattern"})
  public static @interface DateTime {

    /**
     * Date pattern, as specified for {@link SimpleDateFormat}.
     *
     * @return the date pattern
     */
    String pattern();
  }

  /** Annotation formatter, triggered by the <code>@DateTime</code> annotation. */
  public static class AnnotationDateFormatter
      extends Formatters.AnnotationFormatter<DateTime, Date> {

    private final MessagesApi messagesApi;

    /**
     * Creates an annotation date formatter.
     *
     * @param messagesApi messages to look up the pattern
     */
    public AnnotationDateFormatter(MessagesApi messagesApi) {
      this.messagesApi = messagesApi;
    }

    /**
     * Binds the field - constructs a concrete value from submitted data.
     *
     * @param annotation the annotation that triggered this formatter
     * @param text the field text
     * @param locale the current <code>Locale</code>
     * @return a new value
     */
    public Date parse(DateTime annotation, String text, Locale locale)
        throws java.text.ParseException {
      if (text == null || text.trim().isEmpty()) {
        return null;
      }
      Lang lang = new Lang(locale);
      SimpleDateFormat sdf =
          new SimpleDateFormat(
              Optional.ofNullable(this.messagesApi)
                  .map(messages -> messages.get(lang, annotation.pattern()))
                  .orElse(annotation.pattern()),
              locale);
      sdf.setLenient(false);
      return sdf.parse(text);
    }

    /**
     * Unbinds this field - converts a concrete value to plain string
     *
     * @param annotation the annotation that triggered this formatter
     * @param value the value to unbind
     * @param locale the current <code>Locale</code>
     * @return printable version of the value
     */
    public String print(DateTime annotation, Date value, Locale locale) {
      if (value == null) {
        return "";
      }
      Lang lang = new Lang(locale);
      return new SimpleDateFormat(
              Optional.ofNullable(this.messagesApi)
                  .map(messages -> messages.get(lang, annotation.pattern()))
                  .orElse(annotation.pattern()),
              locale)
          .format(value);
    }
  }

  // -- STRING

  /** Defines the format for a <code>String</code> field that cannot be empty. */
  @Target({FIELD})
  @Retention(RUNTIME)
  public static @interface NonEmpty {}

  /** Annotation formatter, triggered by the <code>@NonEmpty</code> annotation. */
  public static class AnnotationNonEmptyFormatter
      extends Formatters.AnnotationFormatter<NonEmpty, String> {

    /**
     * Binds the field - constructs a concrete value from submitted data.
     *
     * @param annotation the annotation that triggered this formatter
     * @param text the field text
     * @param locale the current <code>Locale</code>
     * @return a new value
     */
    public String parse(NonEmpty annotation, String text, Locale locale)
        throws java.text.ParseException {
      if (text == null || text.trim().isEmpty()) {
        return null;
      }
      return text;
    }

    /**
     * Unbinds this field - converts a concrete value to plain string
     *
     * @param annotation the annotation that triggered this formatter
     * @param value the value to unbind
     * @param locale the current <code>Locale</code>
     * @return printable version of the value
     */
    public String print(NonEmpty annotation, String value, Locale locale) {
      if (value == null) {
        return "";
      }
      return value;
    }
  }
}
