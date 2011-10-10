package play.data.format;

import org.springframework.core.*;
import org.springframework.format.*;
import org.springframework.core.convert.*;
import org.springframework.context.i18n.*;
import org.springframework.format.support.*;
import org.springframework.core.convert.converter.*;

import java.util.*;

import java.lang.annotation.*;
import java.lang.reflect.*;

public class Formatters {

    public static <T> T parse(String text, Class<T> clazz) {
        return conversion.convert(text, clazz);
    }

    public static <T> T parse(Field field, String text, Class<T> clazz) {
        return (T)conversion.convert(text, new TypeDescriptor(field), TypeDescriptor.valueOf(clazz));
    }

    public static <T> String print(T t) {
        if(t == null) {
            return "";
        }
        if(conversion.canConvert(t.getClass(), String.class)) {
            return conversion.convert(t, String.class);
        } else {
            return t.toString();
        }
    }

    public static <T> String print(Field field, T t) {
        return print(new TypeDescriptor(field), t);
    }

    public static <T> String print(TypeDescriptor desc, T t) {
        if(t == null) {
            return "";
        }
        if(conversion.canConvert(desc, TypeDescriptor.valueOf(String.class))) {
            return (String)conversion.convert(t, desc, TypeDescriptor.valueOf(String.class));
        } else if(conversion.canConvert(t.getClass(), String.class)) {
            return conversion.convert(t, String.class);
        } else {
            return t.toString();
        }
    }

    // --

    public final static FormattingConversionService conversion = new FormattingConversionService();

    static {
        register(Date.class, new Formats.DateFormatter("yyyy-MM-dd"));
        register(Date.class, new Formats.AnnotationDateFormatter());
    }

    public static abstract class SimpleFormatter<T> {
        public abstract T parse(String text, Locale locale) throws java.text.ParseException;
        public abstract String print(T t, Locale locale);
    }

    public static abstract class AnnotationFormatter<A extends Annotation,T> {
        public abstract T parse(A annotation, String text, Locale locale) throws java.text.ParseException;
        public abstract String print(A annotation, T t, Locale locale);
    }

    public static <T> void register(final Class<T> clazz, final SimpleFormatter<T> formatter) {
        conversion.addFormatterForFieldType(clazz, new org.springframework.format.Formatter<T>() {

            public T parse(String text, Locale locale) throws java.text.ParseException {
                return formatter.parse(text, locale);
            }

            public String print(T t, Locale locale) {
                return formatter.print(t, locale);
            }

            public String toString() {
                return formatter.toString();
            }

        });
    }

    public static <A extends Annotation,T> void register(final Class<T> clazz, final AnnotationFormatter<A,T> formatter) {
        final Class<? extends Annotation> annotationType = (Class<? extends Annotation>)GenericTypeResolver.resolveTypeArguments(
            formatter.getClass(), AnnotationFormatter.class
        )[0];

        conversion.addConverter(new ConditionalGenericConverter() {
            public Set<GenericConverter.ConvertiblePair> getConvertibleTypes() {
                Set<GenericConverter.ConvertiblePair> types = new HashSet<GenericConverter.ConvertiblePair>();
                types.add(new GenericConverter.ConvertiblePair(clazz, String.class));
                return types;
            }

            public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
                return (sourceType.getAnnotation(annotationType) != null);
            }

            public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
                final A a = (A)sourceType.getAnnotation(annotationType);
                Locale locale = LocaleContextHolder.getLocale();
                try {
                    return formatter.print(a, (T)source, locale);
                } catch (Exception ex) {
                    throw new ConversionFailedException(sourceType, targetType, source, ex);
                }
            }

            public String toString() {
                return "@" + annotationType.getName() + " "
                    + clazz.getName() + " -> "
                    + String.class.getName() + ": "
                    + formatter;
            }

        });

        conversion.addConverter(new ConditionalGenericConverter() {
            public Set<GenericConverter.ConvertiblePair> getConvertibleTypes() {
                Set<GenericConverter.ConvertiblePair> types = new HashSet<GenericConverter.ConvertiblePair>();
                types.add(new GenericConverter.ConvertiblePair(String.class, clazz));
                return types;
            }

            public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
                return (targetType.getAnnotation(annotationType) != null);
            }

            public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
                final A a = (A)targetType.getAnnotation(annotationType);
                Locale locale = LocaleContextHolder.getLocale();
                try {
                    return formatter.parse(a, (String)source, locale);
                } catch (Exception ex) {
                    throw new ConversionFailedException(sourceType, targetType, source, ex);
                }
            }

            public String toString() {
                return String.class.getName() + " -> @"
                    + annotationType.getName() + " "
                    + clazz.getName() + ": "
                    + formatter;
            }
        });

    }

}