/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data.format;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.ParseException;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormattersTest {

    private Formatters formatters;

    @BeforeEach
    public void prepareFormatters() {
        formatters = new Formatters(null);
        formatters.register(Integer.class, new IntegerFormatter());
        formatters.register(Integer.class, new IntegerCustomFormatter());
    }

    @Test
    void testFormattersParseUsingField() throws NoSuchFieldException {
        int integerFromPlainField = formatters.parse(Bean.class.getDeclaredField("plainIntegerField"), "10");
        assertEquals(10, integerFromPlainField);
    }

    @Test
    void testFormattersParseUsingAnnotatedField() throws NoSuchFieldException {
        int integerFromAnnotatedField = formatters.parse(Bean.class.getDeclaredField("annotatedIntegerField"), "10");
        assertEquals(15, integerFromAnnotatedField);
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
