package play.data.format;

import java.text.*;
import java.util.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Defines several default formatters.
 */
public class Formats {
    
    // -- DATE
    
    /**
     * Formatter for java.util.Date type.
     */
    public static class DateFormatter extends Formatters.SimpleFormatter<Date> {
        
        private final String pattern;
        
        /**
         * Create a date formatter.
         *
         * @param pattern Date pattern as specified in Java SimpleDateFormat.
         */
        public DateFormatter(String pattern) {
            this.pattern = pattern;
        }
        
        /**
         * Bind the field (ie. construct a concrete value from submitted data).
         *
         * @param text The field text.
         * @param locale The current Locale.
         * @return A new value.
         */
        public Date parse(String text, Locale locale) throws java.text.ParseException {
            if(text == null || text.trim().isEmpty()) {
                return null;
            }
            SimpleDateFormat sdf = new SimpleDateFormat(pattern, locale);
            sdf.setLenient(false);  
            return sdf.parse(text);
        }
        
        /**
         * Unbind this field (ie. transform a concrete value to plain string)
         *
         * @param value The value to unbind.
         * @param locale The current Locale.
         * @return Printable version of the value.
         */
        public String print(Date value, Locale locale) {
            if(value == null) {
                return "";
            }
            return new SimpleDateFormat(pattern, locale).format(value);
        }
        
    }
    
    /**
     * Defines the format for a Date field.
     */
    @Target({FIELD})
    @Retention(RUNTIME)
    @play.data.Form.Display(name="format.date", attributes={"pattern"})
    public static @interface DateTime {
        
        /**
         * Date pattern as specified in Java SimpleDateFormat.
         */
        String pattern();
    }
    
    /**
     * Annotation formatter, triggered by the @DateTime annotation.
     */
    public static class AnnotationDateFormatter extends Formatters.AnnotationFormatter<DateTime,Date> {
        
        /**
         * Bind the field (ie. construct a concrete value from submitted data).
         *
         * @param annotation The annotation which has trigerred this formatter.
         * @param text The field text.
         * @param locale The current Locale.
         * @return A new value.
         */
        public Date parse(DateTime annotation, String text, Locale locale) throws java.text.ParseException {
            if(text == null || text.trim().isEmpty()) {
                return null;
            }
            SimpleDateFormat sdf = new SimpleDateFormat(annotation.pattern(), locale);
            sdf.setLenient(false);  
            return sdf.parse(text);
        }
        
        /**
         * Unbind this field (ie. transform a concrete value to plain string)
         *
         * @param annotation The annotation which has trigerred this formatter.
         * @param value The value to unbind.
         * @param locale The current Locale.
         * @return Printable version of the value.
         */
        public String print(DateTime annotation, Date value, Locale locale) {
            if(value == null) {
                return "";
            }
            return new SimpleDateFormat(annotation.pattern(), locale).format(value);
        }
        
    }
    
}
