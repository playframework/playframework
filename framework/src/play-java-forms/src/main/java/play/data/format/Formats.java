/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data.format;

import java.text.*;
import java.util.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import play.i18n.Lang;
import play.i18n.MessagesApi;

/**
 * Defines several default formatters.
 */
public class Formats {

    // -- DATE

    /**
     * Formatter for <code>java.util.Date</code> values.
     */
    public static class DateFormatter extends Formatters.SimpleFormatter<Date> {

        private final MessagesApi messagesApi;

        private final String pattern;

        private final String patternNoApp;

        /**
         * Creates a date formatter.
         * The value defined for the message file key "formats.date" will be used as the default pattern.
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
         * @param pattern date pattern, as specified for {@link SimpleDateFormat}. Can be a message file key.
         */
        public DateFormatter(MessagesApi messagesApi, String pattern) {
            this(messagesApi, pattern, "yyyy-MM-dd");
        }

        /**
         * Creates a date formatter.
         *
         * @param messagesApi messages to look up the pattern
         * @param pattern date pattern, as specified for {@link SimpleDateFormat}. Can be a message file key.
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
            if(text == null || text.trim().isEmpty()) {
                return null;
            }
            Lang lang = new Lang(locale);
            SimpleDateFormat sdf = new SimpleDateFormat(Optional.ofNullable(this.messagesApi)
                .map(messages -> messages.get(lang, pattern))
                .orElse(patternNoApp), locale);
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
            if(value == null) {
                return "";
            }
            Lang lang = new Lang(locale);
            return new SimpleDateFormat(Optional.ofNullable(this.messagesApi)
                .map(messages -> messages.get(lang, pattern))
                .orElse(patternNoApp), locale).format(value);
        }

    }

    /**
     * Defines the format for a <code>Date</code> field.
     */
    @Target({FIELD})
    @Retention(RUNTIME)
    @play.data.Form.Display(name="format.date", attributes={"pattern"})
    public static @interface DateTime {

        /**
         * Date pattern, as specified for {@link SimpleDateFormat}.
         *
         * @return the date pattern
         */
        String pattern();
    }

    /**
     * Annotation formatter, triggered by the <code>@DateTime</code> annotation.
     */
    public static class AnnotationDateFormatter extends Formatters.AnnotationFormatter<DateTime,Date> {

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
        public Date parse(DateTime annotation, String text, Locale locale) throws java.text.ParseException {
            if(text == null || text.trim().isEmpty()) {
                return null;
            }
            Lang lang = new Lang(locale);
            SimpleDateFormat sdf = new SimpleDateFormat(Optional.ofNullable(this.messagesApi)
                .map(messages -> messages.get(lang, annotation.pattern()))
                .orElse(annotation.pattern()), locale);
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
            if(value == null) {
                return "";
            }
            Lang lang = new Lang(locale);
            return new SimpleDateFormat(Optional.ofNullable(this.messagesApi)
                .map(messages -> messages.get(lang, annotation.pattern()))
                .orElse(annotation.pattern()), locale).format(value);
        }

    }

    // -- STRING

    /**
     * Defines the format for a <code>String</code> field that cannot be empty.
     */
    @Target({FIELD})
    @Retention(RUNTIME)
    public static @interface NonEmpty {}

    /**
     * Annotation formatter, triggered by the <code>@NonEmpty</code> annotation.
     */
    public static class AnnotationNonEmptyFormatter extends Formatters.AnnotationFormatter<NonEmpty,String> {

        /**
         * Binds the field - constructs a concrete value from submitted data.
         *
         * @param annotation the annotation that triggered this formatter
         * @param text the field text
         * @param locale the current <code>Locale</code>
         * @return a new value
         */
        public String parse(NonEmpty annotation, String text, Locale locale) throws java.text.ParseException {
            if(text == null || text.trim().isEmpty()) {
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
            if(value == null) {
                return "";
            }
            return value;
        }

    }


}
