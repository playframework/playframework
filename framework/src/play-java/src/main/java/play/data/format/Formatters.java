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

import play.libs.F.*;

/**
 * Formatters helper.
 */
public class Formatters {

    /**
     * Parses this string as instance of the given class.
     *
     * @param text the text to parse
     * @param clazz class representing the required type
     * @return the parsed value
     */
    public static <T> T parse(String text, Class<T> clazz) {
        return conversion.convert(text, clazz);
    }

    /**
     * Parses this string as instance of a specific field in the given class
     *
     * @param field the related field (custom formatters are extracted from this field annotation)
     * @param text the text to parse
     * @param clazz class representing the required type
     * @return the parsed value
     */
    public static <T> T parse(Field field, String text, Class<T> clazz) {
        return (T)conversion.convert(text, new TypeDescriptor(field), TypeDescriptor.valueOf(clazz));
    }

    /**
     * Computes the display string for any value.
     *
     * @param t the value to print
     * @return the formatted string
     */
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

    /**
     * Computes the display string for any value, for a specific field.
     *
     * @param field the related field - custom formatters are extracted from this field annotation
     * @param t the value to print
     * @return the formatted string
     */
    public static <T> String print(Field field, T t) {
        return print(new TypeDescriptor(field), t);
    }

    /**
     * Computes the display string for any value, for a specific type.
     *
     * @param desc the field descriptor - custom formatters are extracted from this descriptor.
     * @param t the value to print
     * @return the formatted string
     */
    public static <T> String print(TypeDescriptor desc, T t) {
        if(t == null) {
            return "";
        }
        if(desc != null && conversion.canConvert(desc, TypeDescriptor.valueOf(String.class))) {
            return (String)conversion.convert(t, desc, TypeDescriptor.valueOf(String.class));
        } else if(conversion.canConvert(t.getClass(), String.class)) {
            return conversion.convert(t, String.class);
        } else {
            return t.toString();
        }
    }

    // --

    /**
     * The underlying conversion service.
     */
    public final static FormattingConversionService conversion = new FormattingConversionService();

    static {
        register(Date.class, new Formats.DateFormatter("yyyy-MM-dd"));
        register(Date.class, new Formats.AnnotationDateFormatter());
        register(String.class, new Formats.AnnotationNonEmptyFormatter());
        registerOption();
    }

    /**
     * Super-type for custom simple formatters.
     */
    public static abstract class SimpleFormatter<T> {

        /**
         * Binds the field - constructs a concrete value from submitted data.
         *
         * @param text the field text
         * @param locale the current Locale
         * @return a new value
         */
        public abstract T parse(String text, Locale locale) throws java.text.ParseException;

        /**
         * Unbinds this field - transforms a concrete value to plain string.
         *
         * @param t the value to unbind
         * @param locale the current <code>Locale</code>
         * @return printable version of the value
         */
        public abstract String print(T t, Locale locale);

    }

    /**
     * Super-type for annotation-based formatters.
     */
    public static abstract class AnnotationFormatter<A extends Annotation,T> {

        /**
         * Binds the field - constructs a concrete value from submitted data.
         *
         * @param annotation the annotation that trigerred this formatter
         * @param text the field text
         * @param locale the current <code>Locale</code>
         * @return a new value
         */
        public abstract T parse(A annotation, String text, Locale locale) throws java.text.ParseException;

        /**
         * Unbind this field (ie. transform a concrete value to plain string)
         *
         * @param annotation the annotation that trigerred this formatter.
         * @param value the value to unbind
         * @param locale the current <code>Locale</code>
         * @return printable version of the value
         */
        public abstract String print(A annotation, T value, Locale locale);
    }

    /**
     * Converter for String -> Option and Option -> String
     */
    private static <T> void registerOption() {
        conversion.addConverter(new GenericConverter() {

            public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
                if (sourceType.getObjectType().equals(String.class)) {
                    // From String to Option
                    Object element = conversion.convert(source, sourceType, targetType.getElementTypeDescriptor());
                    if (element == null) {
                        return new None();
                    } else {
                        return new Some(element);
                    }
                } else if (targetType.getObjectType().equals(String.class)) {
                    // Fromt Option to String
                    if (source == null) return "";

                    Option<?> opt = (Option) source;
                    if (!opt.isDefined()) {
                        return "";
                    } else {
                        return conversion.convert(source, sourceType.getElementTypeDescriptor(), targetType);
                    }
                }
                return null;
            }

            public Set<GenericConverter.ConvertiblePair> getConvertibleTypes() {
                Set<ConvertiblePair> result = new HashSet<ConvertiblePair>();
                result.add(new ConvertiblePair(Option.class, String.class));
                result.add(new ConvertiblePair(String.class, Option.class));
                return result;
            }

        });
    }

    /**
     * Registers a simple formatter.
     *
     * @param clazz class handled by this formatter
     * @param formatter the formatter to register
     */
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

    /**
     * Registers an annotation-based formatter.
     *
     * @param clazz class handled by this formatter
     * @param formatter the formatter to register
     */
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
