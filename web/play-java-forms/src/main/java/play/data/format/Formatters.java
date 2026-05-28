/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data.format;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import play.data.internal.binding.core.GenericTypeResolver;
import play.data.internal.binding.core.convert.ConversionFailedException;
import play.data.internal.binding.core.convert.TypeDescriptor;
import play.data.internal.binding.core.convert.converter.ConditionalGenericConverter;
import play.data.internal.binding.core.convert.converter.GenericConverter;
import play.data.internal.binding.format.Formatter;
import play.data.internal.binding.format.support.FormattingConversionService;
import play.i18n.MessagesApi;

/** Formatters helper. */
@Singleton
public class Formatters {

  @Inject
  public Formatters(MessagesApi messagesApi) {
    // By default, we always register some common and useful Formatters
    register(new Formats.DefaultStringConverter());
    register(Date.class, new Formats.DateFormatter(messagesApi));
    register(Date.class, new Formats.AnnotationDateFormatter(messagesApi));
    register(String.class, new Formats.AnnotationNonEmptyFormatter());
    registerOptional();
  }

  /**
   * Parses this string as instance of the given class.
   *
   * @param text the text to parse
   * @param clazz class representing the required type
   * @param <T> the type to parse out of the text
   * @return the parsed value
   */
  public <T> T parse(String text, Class<T> clazz, Locale locale) {
    return conversion.convert(text, clazz, locale);
  }

  /**
   * Parses this string as instance of a specific field
   *
   * @param field the related field (custom formatters are extracted from this field annotation)
   * @param text the text to parse
   * @param <T> the type to parse out of the text
   * @return the parsed value
   */
  @SuppressWarnings("unchecked")
  public <T> T parse(Field field, String text, Locale locale) {
    return (T) conversion.convert(text, new TypeDescriptor(field), locale);
  }

  /**
   * Computes the display string for any value.
   *
   * @param t the value to print
   * @param <T> the type to print
   * @return the formatted string
   */
  public <T> String print(T t, Locale locale) {
    if (t == null) {
      return "";
    }
    if (conversion.canConvert(t.getClass(), String.class)) {
      return conversion.convert(t, String.class, locale);
    } else {
      return t.toString();
    }
  }

  /**
   * Computes the display string for any value, for a specific field.
   *
   * @param field the related field - custom formatters are extracted from this field annotation
   * @param t the value to print
   * @param <T> the type to print
   * @return the formatted string
   */
  public <T> String print(Field field, T t, Locale locale) {
    return print(new TypeDescriptor(field), t, locale);
  }

  /**
   * Computes the display string for any value, for a specific type.
   *
   * @param desc the field descriptor - custom formatters are extracted from this descriptor.
   * @param t the value to print
   * @param <T> the type to print
   * @return the formatted string
   */
  <T> String print(TypeDescriptor desc, T t, Locale locale) {
    if (t == null) {
      return "";
    }
    if (desc != null && conversion.canConvert(desc, TypeDescriptor.valueOf(String.class))) {
      return (String) conversion.convert(t, desc, TypeDescriptor.valueOf(String.class), locale);
    } else if (conversion.canConvert(t.getClass(), String.class)) {
      return conversion.convert(t, String.class, locale);
    } else {
      return t.toString();
    }
  }

  // --

  /** The underlying conversion service. */
  final FormattingConversionService conversion = new FormattingConversionService();

  /**
   * Super-type for custom simple formatters.
   *
   * @param <T> the type that this formatter will parse and print
   */
  public abstract static class SimpleFormatter<T> {

    /**
     * Binds the field - constructs a concrete value from submitted data.
     *
     * @param text the field text
     * @param locale the current Locale
     * @throws java.text.ParseException if the text could not be parsed into T
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
   *
   * @param <A> the type of the annotation
   * @param <T> the type that this formatter will parse and print
   */
  public abstract static class AnnotationFormatter<A extends Annotation, T> {

    /**
     * Binds the field - constructs a concrete value from submitted data.
     *
     * @param annotation the annotation that triggered this formatter
     * @param text the field text
     * @param locale the current <code>Locale</code>
     * @throws java.text.ParseException when the text could not be parsed
     * @return a new value
     */
    public abstract T parse(A annotation, String text, Locale locale)
        throws java.text.ParseException;

    /**
     * Unbind this field (ie. transform a concrete value to plain string)
     *
     * @param annotation the annotation that triggered this formatter.
     * @param value the value to unbind
     * @param locale the current <code>Locale</code>
     * @return printable version of the value
     */
    public abstract String print(A annotation, T value, Locale locale);
  }

  /**
   * Super-type for converters that parse strings into values and print values as strings.
   *
   * <p>This is useful for converters that support multiple target types and therefore do not fit
   * the one-type {@link SimpleFormatter} API.
   */
  public interface StringFormatConverter {

    /**
     * Returns the value types this converter can parse from strings and print to strings.
     *
     * @return supported value types
     */
    Set<Class<?>> supportedTypes();

    /**
     * Parses the given text into the requested target type.
     *
     * @param text the field text
     * @param targetType the requested target type
     * @return the parsed value
     * @throws Exception if the text could not be parsed into the requested type
     */
    Object parse(String text, Class<?> targetType) throws Exception;

    /**
     * Prints the given value as a string.
     *
     * @param value the value to print
     * @return printable version of the value
     * @throws Exception if the value could not be printed
     */
    String print(Object value) throws Exception;
  }

  /** Converter for String -> Optional and Optional -> String */
  private Formatters registerOptional() {
    conversion.addConverter(
        new GenericConverter() {

          public Object convert(
              Object source, TypeDescriptor sourceType, TypeDescriptor targetType, Locale locale) {
            if (sourceType.getObjectType().equals(String.class)) {
              // From String to Optional
              Object element =
                  conversion.convert(
                      source, sourceType, targetType.elementTypeDescriptor(source), locale);
              return Optional.ofNullable(element);
            } else if (targetType.getObjectType().equals(String.class)) {
              // From Optional to String
              if (source == null) return "";

              Optional<?> opt = (Optional) source;
              return opt.map(
                      o ->
                          conversion.convert(
                              source, sourceType.getElementTypeDescriptor(), targetType, locale))
                  .orElse("");
            }
            return null;
          }

          public Set<GenericConverter.ConvertiblePair> getConvertibleTypes() {
            Set<ConvertiblePair> result = new HashSet<>();
            result.add(new ConvertiblePair(Optional.class, String.class));
            result.add(new ConvertiblePair(String.class, Optional.class));
            return result;
          }
        });

    return this;
  }

  /**
   * Registers a simple formatter.
   *
   * @param clazz class handled by this formatter
   * @param <T> the type that this formatter will parse and print
   * @param formatter the formatter to register
   * @return the modified Formatters object.
   */
  public <T> Formatters register(final Class<T> clazz, final SimpleFormatter<T> formatter) {
    conversion.addFormatterForFieldType(
        clazz,
        new Formatter<T>() {

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

    return this;
  }

  /**
   * Registers a converter that can parse strings into values and print values as strings.
   *
   * @param converter the converter to register
   * @return the modified Formatters object.
   */
  public Formatters register(final StringFormatConverter converter) {
    conversion.addConverter(new StringFormatConverterAdapter(converter));
    return this;
  }

  /**
   * Unregisters all formatters for the given class.
   *
   * <p>This removes both directions used by Play formatters: parsing from {@link String} to the
   * given class and printing from the given class to {@link String}.
   *
   * @param clazz class handled by the formatters to unregister
   * @return the modified Formatters object.
   */
  public Formatters unregisterAll(final Class<?> clazz) {
    conversion.removeConvertible(clazz, String.class);
    conversion.removeConvertible(String.class, clazz);
    return this;
  }

  private static class StringFormatConverterAdapter implements GenericConverter {

    private final StringFormatConverter converter;

    StringFormatConverterAdapter(StringFormatConverter converter) {
      this.converter = converter;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
      Set<ConvertiblePair> types = new HashSet<>();
      for (Class<?> supportedType : converter.supportedTypes()) {
        types.add(new ConvertiblePair(String.class, supportedType));
        types.add(new ConvertiblePair(supportedType, String.class));
      }
      return types;
    }

    @Override
    public Object convert(
        Object source, TypeDescriptor sourceType, TypeDescriptor targetType, Locale locale) {
      try {
        if (sourceType.getType() == String.class) {
          return converter.parse((String) source, targetType.getType());
        }
        if (targetType.getType() == String.class) {
          return converter.print(source);
        }
        return null;
      } catch (Exception ex) {
        throw new ConversionFailedException(sourceType, targetType, source, ex);
      }
    }
  }

  /**
   * Registers an annotation-based formatter.
   *
   * @param clazz class handled by this formatter
   * @param formatter the formatter to register
   * @param <A> the annotation type
   * @param <T> the type that will be parsed or printed
   * @return the modified Formatters object.
   */
  @SuppressWarnings("unchecked")
  public <A extends Annotation, T> Formatters register(
      final Class<T> clazz, final AnnotationFormatter<A, T> formatter) {
    final Class<? extends Annotation> annotationType =
        (Class<? extends Annotation>)
            GenericTypeResolver.resolveTypeArguments(
                formatter.getClass(), AnnotationFormatter.class)[0];

    conversion.addConverter(
        new ConditionalGenericConverter() {
          public Set<GenericConverter.ConvertiblePair> getConvertibleTypes() {
            Set<GenericConverter.ConvertiblePair> types = new HashSet<>();
            types.add(new GenericConverter.ConvertiblePair(clazz, String.class));
            return types;
          }

          public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
            return (sourceType.getAnnotation(annotationType) != null);
          }

          public Object convert(
              Object source, TypeDescriptor sourceType, TypeDescriptor targetType, Locale locale) {
            final A a = (A) sourceType.getAnnotation(annotationType);
            try {
              return formatter.print(a, (T) source, locale);
            } catch (Exception ex) {
              throw new ConversionFailedException(sourceType, targetType, source, ex);
            }
          }

          public String toString() {
            return "@"
                + annotationType.getName()
                + " "
                + clazz.getName()
                + " -> "
                + String.class.getName()
                + ": "
                + formatter;
          }
        });

    conversion.addConverter(
        new ConditionalGenericConverter() {
          public Set<GenericConverter.ConvertiblePair> getConvertibleTypes() {
            Set<GenericConverter.ConvertiblePair> types = new HashSet<>();
            types.add(new GenericConverter.ConvertiblePair(String.class, clazz));
            return types;
          }

          public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
            return (targetType.getAnnotation(annotationType) != null);
          }

          public Object convert(
              Object source, TypeDescriptor sourceType, TypeDescriptor targetType, Locale locale) {
            final A a = (A) targetType.getAnnotation(annotationType);
            try {
              return formatter.parse(a, (String) source, locale);
            } catch (Exception ex) {
              throw new ConversionFailedException(sourceType, targetType, source, ex);
            }
          }

          public String toString() {
            return String.class.getName()
                + " -> @"
                + annotationType.getName()
                + " "
                + clazz.getName()
                + ": "
                + formatter;
          }
        });

    return this;
  }
}
