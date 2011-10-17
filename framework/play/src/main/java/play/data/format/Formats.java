package play.data.format;

import java.text.*;
import java.util.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

public class Formats {
    
    // -- DATE
    
    public static class DateFormatter extends Formatters.SimpleFormatter<Date> {
        
        final String pattern;
        
        public DateFormatter(String pattern) {
            this.pattern = pattern;
        }
        
        public Date parse(String text, Locale locale) throws java.text.ParseException {
            if(text == null || text.trim().isEmpty()) {
                return null;
            }
            return new SimpleDateFormat(pattern, locale).parse(text);
        }
        
        public String print(Date date, Locale locale) {
            if(date == null) {
                return "";
            }
            return new SimpleDateFormat(pattern, locale).format(date);
        }
        
    }
    
    @Target({FIELD})
    @Retention(RUNTIME)
    @play.data.Form.Display(name="format.date", attributes={"pattern"})
    public static @interface DateTime {
        String pattern();
    }
    
    public static class AnnotationDateFormatter extends Formatters.AnnotationFormatter<DateTime,Date> {
        
        public Date parse(DateTime a, String text, Locale locale) throws java.text.ParseException {
            if(text == null || text.trim().isEmpty()) {
                return null;
            }
            return new SimpleDateFormat(a.pattern(), locale).parse(text);
        }
        
        public String print(DateTime a, Date date, Locale locale) {
            if(date == null) {
                return "";
            }
            return new SimpleDateFormat(a.pattern(), locale).format(date);
        }
        
    }
    
}