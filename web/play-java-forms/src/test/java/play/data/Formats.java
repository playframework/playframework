/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import play.data.format.Formatters;

public class Formats {

    /**
     * Defines the format for a <code>BigDecimal</code> field.
     */
    @Target({FIELD})
    @Retention(RUNTIME)
    public static @interface Currency {
    }

    /**
     * Annotation formatter, triggered by the <code>@Currency</code> annotation.
     */
    public static class AnnotationCurrencyFormatter extends Formatters.AnnotationFormatter<Currency, BigDecimal> {

        /**
         * Binds the field - constructs a concrete value from submitted data.
         *
         * @param annotation the annotation that triggered this formatter
         * @param text the field text
         * @param locale the current <code>Locale</code>
         * @return a new value
         */
        @Override
        public BigDecimal parse(final Currency annotation, final String text, final Locale locale) throws java.text.ParseException {
            if(text == null || text.trim().isEmpty()) {
                return null;
            }
            final DecimalFormat format = (DecimalFormat) NumberFormat.getInstance(locale);
            format.setParseBigDecimal(true);
            return (BigDecimal)format.parseObject(text);
        }

        /**
         * Unbinds this field - converts a concrete value to plain string
         *
         * @param annotation the annotation that triggered this formatter
         * @param value the value to unbind
         * @param locale the current <code>Locale</code>
         * @return printable version of the value
         */
        @Override
        public String print(final Currency annotation, final BigDecimal value, final Locale locale) {
            if(value == null) {
                return "";
            }

            DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(locale);
            return formatter.format(value);
        }

    }
}
