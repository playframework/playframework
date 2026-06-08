/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data.format;

import org.junit.Before;
import org.junit.Test;
import play.data.internal.binding.core.convert.ConverterNotFoundException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.ParseException;
import java.util.Locale;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
    public void testUnregisterAllRemovesParseAndPrintConverters() throws NoSuchFieldException {
        formatters.register(Value.class, new ValueFormatter());
        Value value = new Value("10");

        assertEquals(new Value("10"), formatters.parse(Bean.class.getDeclaredField("valueField"), "10"));
        assertEquals("formatted-10", formatters.print(Bean.class.getDeclaredField("valueField"), value));

        assertEquals(formatters, formatters.unregisterAll(Value.class));

        try {
            formatters.parse(Bean.class.getDeclaredField("valueField"), "10");
            fail("Expected parsing converter to be removed");
        } catch (ConverterNotFoundException expected) {
        }

        assertEquals("value-10", formatters.print(Bean.class.getDeclaredField("valueField"), value));
    }

    @SuppressWarnings("unused")
    private static class Bean {
        private Integer plainIntegerField;
        @CustomInteger
        private Integer annotatedIntegerField;
        private Value valueField;
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

    private static class Value {
        private final String text;

        private Value(String text) {
            this.text = text;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Value)) {
                return false;
            }
            Value value = (Value) o;
            return Objects.equals(text, value.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text);
        }

        @Override
        public String toString() {
            return "value-" + text;
        }
    }

    public static class ValueFormatter extends Formatters.SimpleFormatter<Value> {
        @Override
        public Value parse(String text, Locale locale) {
            return new Value(text);
        }

        @Override
        public String print(Value value, Locale locale) {
            return "formatted-" + value.text;
        }
    }
}
