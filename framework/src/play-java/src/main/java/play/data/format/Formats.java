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
     * Formatter for <code>java.util.Date</code> values.
     */
    public static class DateFormatter extends Formatters.SimpleFormatter<Date> {
        
        private final String pattern;
        
        /**
         * Creates a date formatter.
         *
         * @param pattern date pattern, as specified for {@link SimpleDateFormat}.
         */
        public DateFormatter(String pattern) {
            this.pattern = pattern;
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
            SimpleDateFormat sdf = new SimpleDateFormat(pattern, locale);
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
            return new SimpleDateFormat(pattern, locale).format(value);
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
         */
        String pattern();
    }
    
    /**
     * Annotation formatter, triggered by the <code>@DateTime</code> annotation.
     */
    public static class AnnotationDateFormatter extends Formatters.AnnotationFormatter<DateTime,Date> {
        
        /**
         * Binds the field - constructs a concrete value from submitted data.
         *
         * @param annotation the annotation that trigerred this formatter
         * @param text the field text
         * @param locale the current <code>Locale</code>
         * @return a new value
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
         * Unbinds this field - converts a concrete value to plain string
         *
         * @param annotation the annotation that trigerred this formatter
         * @param value the value to unbind
         * @param locale the current <code>Locale</code>
         * @return printable version of the value
         */
        public String print(DateTime annotation, Date value, Locale locale) {
            if(value == null) {
                return "";
            }
            return new SimpleDateFormat(annotation.pattern(), locale).format(value);
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
         * @param annotation the annotation that trigerred this formatter
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
         * @param annotation the annotation that trigerred this formatter
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
