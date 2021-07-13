/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.data.format;

import org.junit.Before;
import org.junit.Test;
import play.data.format.Formats.LocalDate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.ParseException;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class FormattersTest {

    private Formatters formatters;

    @Before
    public void prepareFormatters() {
        formatters = new Formatters(null);
        formatters.register(Integer.class, new IntegerFormatter());
        formatters.register(Integer.class, new IntegerCustomFormatter());
    }

    @Test
    public void testFormattersParseUsingField() throws NoSuchFieldException {
        int integerFromPlainField = formatters.parse(Bean.class.getDeclaredField("plainIntegerField"), "10");
        assertEquals(10, integerFromPlainField);
    }

    @Test
    public void testFormattersParseUsingAnnotatedField() throws NoSuchFieldException {
        int integerFromAnnotatedField = formatters.parse(Bean.class.getDeclaredField("annotatedIntegerField"), "10");
        assertEquals(15, integerFromAnnotatedField);
    }

    @Test
    public void testFormattersParseLocalDateUsingField() throws NoSuchFieldException {
        java.time.LocalDate dateFromPlainField = formatters.parse(LocalDateBean.class.getDeclaredField("plainLocalDateField"), "1989-01-13");
        assertEquals(java.time.LocalDate.of(1989, 1, 13), dateFromPlainField);

        java.time.LocalDate veryOldDateFromPlainField = formatters.parse(LocalDateBean.class.getDeclaredField("plainLocalDateField"), "-0023-08-08");
        assertEquals(java.time.LocalDate.of(-23, 8, 8), veryOldDateFromPlainField);

        java.time.LocalDate oldDateFromPlainField = formatters.parse(LocalDateBean.class.getDeclaredField("plainLocalDateField"), "0001-02-10");
        assertEquals(java.time.LocalDate.of(0001, 02, 10), oldDateFromPlainField);

        java.time.LocalDate recentPastDateFromPlainField = formatters.parse(LocalDateBean.class.getDeclaredField("plainLocalDateField"), "1975-10-25");
        assertEquals(java.time.LocalDate.of(1975, 10, 25), recentPastDateFromPlainField);

        final java.time.LocalDate NOW = java.time.LocalDate.now();
        java.time.LocalDate dateNowFromPlainField = formatters.parse(LocalDateBean.class.getDeclaredField("plainLocalDateField"), NOW.toString());
        assertEquals(NOW, dateNowFromPlainField);

        java.time.LocalDate futureDateFromPlainField = formatters.parse(LocalDateBean.class.getDeclaredField("plainLocalDateField"), "2100-12-12");
        assertEquals(java.time.LocalDate.of(2100, 12, 12), futureDateFromPlainField);
    }

    @Test
    public void testFormattersParseLocalDateUsingAnnotatedField() throws NoSuchFieldException {
        java.time.LocalDate dateFromAnnotatedField = formatters.parse(LocalDateBean.class.getDeclaredField("annotatedLocalDateField"), "1987-12-22");
        assertEquals(java.time.LocalDate.of(1987, 12, 22), dateFromAnnotatedField);

        java.time.LocalDate veryOldDateFromAnnotatedField = formatters.parse(LocalDateBean.class.getDeclaredField("annotatedLocalDateField"), "-0023-08-08");
        assertEquals(java.time.LocalDate.of(-23, 8, 8), veryOldDateFromAnnotatedField);

        java.time.LocalDate oldDateFromAnnotatedField = formatters.parse(LocalDateBean.class.getDeclaredField("annotatedLocalDateField"), "0001-02-10");
        assertEquals(java.time.LocalDate.of(0001, 02, 10), oldDateFromAnnotatedField);

        java.time.LocalDate recentPastDateFromAnnotatedField = formatters.parse(LocalDateBean.class.getDeclaredField("annotatedLocalDateField"), "1975-10-25");
        assertEquals(java.time.LocalDate.of(1975, 10, 25), recentPastDateFromAnnotatedField);

        final java.time.LocalDate NOW = java.time.LocalDate.now();
        java.time.LocalDate dateNowFromAnnotatedField = formatters.parse(LocalDateBean.class.getDeclaredField("annotatedLocalDateField"), NOW.toString());
        assertEquals(NOW, dateNowFromAnnotatedField);

        java.time.LocalDate futureDateFromAnnotatedField = formatters.parse(LocalDateBean.class.getDeclaredField("annotatedLocalDateField"), "2100-12-12");
        assertEquals(java.time.LocalDate.of(2100, 12, 12), futureDateFromAnnotatedField);
    }

    @Test(expected = org.springframework.core.convert.ConversionFailedException.class)
    public void testFormattersParseInvalidDayOfMonth() throws NoSuchFieldException {
        formatters.parse(LocalDateBean.class.getDeclaredField("plainLocalDateField"), "2018-09-31"); // invalid dateOfMonth
    }

    @Test(expected = org.springframework.core.convert.ConversionFailedException.class)
    public void testFormattersParseInvalidMonthOfYear() throws NoSuchFieldException {
        formatters.parse(LocalDateBean.class.getDeclaredField("plainLocalDateField"), "2018-13-20"); // invalid month
    }

    @Test(expected = org.springframework.core.convert.ConversionFailedException.class)
    public void testFormattersParseYearNotMatchesPattern() throws NoSuchFieldException {
        formatters.parse(LocalDateBean.class.getDeclaredField("plainLocalDateField"), "10000-01-01"); // invalid year-to-pattern
    }

    @Test(expected = org.springframework.core.convert.ConversionFailedException.class)
    public void testAnnotationFormattersParseInvalidDayOfMonth() throws NoSuchFieldException {
        formatters.parse(LocalDateBean.class.getDeclaredField("annotatedLocalDateField"), "2018-09-31"); // invalid dateOfMonth
    }

    @Test(expected = org.springframework.core.convert.ConversionFailedException.class)
    public void testAnnotationFormattersParseInvalidMonthOfYear() throws NoSuchFieldException {
        formatters.parse(LocalDateBean.class.getDeclaredField("annotatedLocalDateField"), "2018-13-20"); // invalid month
    }

    @Test(expected = org.springframework.core.convert.ConversionFailedException.class)
    public void testAnnotationFormattersParseYearNotMatchesPattern() throws NoSuchFieldException {
        formatters.parse(LocalDateBean.class.getDeclaredField("annotatedLocalDateField"), "10000-01-01"); // invalid year-to-pattern
    }

    @SuppressWarnings("unused")
    private static class LocalDateBean {
        private java.time.LocalDate plainLocalDateField;
        @LocalDate(pattern="uuuu-MM-dd")
        private java.time.LocalDate annotatedLocalDateField;
    }

    @SuppressWarnings("unused")
    private static class Bean {
        private Integer plainIntegerField;
        @CustomInteger
        private Integer annotatedIntegerField;
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface CustomInteger {
    }

    public static class IntegerCustomFormatter extends Formatters.AnnotationFormatter<CustomInteger, Integer> {

        @Override
        public Integer parse(CustomInteger a, String text, Locale locale) throws ParseException {
            try {
                return Integer.parseInt(text) + 5;
            } catch (NumberFormatException e) {
                throw new ParseException("Invalid integer (" + text + ")", 0);
            }
        }

        @Override
        public String print(CustomInteger annotation, Integer value, Locale locale) {
            return value == null ? "" : value.toString() + "L";
        }
    }

    public static class IntegerFormatter extends Formatters.SimpleFormatter<Integer> {
        @Override
        public Integer parse(String text, Locale locale) throws ParseException {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                throw new ParseException("Invalid integer (" + text + ")", 0);
            }
        }

        @Override
        public String print(Integer t, Locale locale) {
            return t == null ? null : t.toString();
        }
    }
}
