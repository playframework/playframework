/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data.format;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Pattern;
import play.i18n.Lang;
import play.i18n.MessagesApi;

/** Defines several default formatters. */
public class Formats {

  // -- DEFAULT SCALARS

  public static class DefaultStringConverter implements Formatters.StringFormatConverter {

    private static final Set<Class<?>> SUPPORTED_TYPES =
        Set.of(
            byte[].class,
            char[].class,
            Character.class,
            Boolean.class,
            Byte.class,
            Short.class,
            Integer.class,
            Long.class,
            Float.class,
            Double.class,
            BigDecimal.class,
            BigInteger.class,
            Charset.class,
            Currency.class,
            Locale.class,
            Pattern.class,
            TimeZone.class,
            URI.class,
            URL.class,
            UUID.class,
            ZoneId.class);

    @Override
    public Set<Class<?>> supportedTypes() {
      return SUPPORTED_TYPES;
    }

    @Override
    public Object parse(String text, Class<?> targetType) {
      return Formats.parse(text, targetType);
    }

    @Override
    public String print(Object value) {
      return Formats.print(value);
    }
  }

  private static Object parse(String text, Class<?> targetType) {
    if (targetType == byte[].class) {
      return (text != null ? text.getBytes() : null);
    }
    if (targetType == char[].class) {
      return (text != null ? text.toCharArray() : null);
    }

    Class<?> type = wrapperType(targetType);
    if (type == Character.class) {
      return parseCharacter(text);
    }
    if (type == Boolean.class) {
      return parseBoolean(text);
    }
    if (Number.class.isAssignableFrom(type)) {
      return parseNumber(text, type.asSubclass(Number.class));
    }
    if (type == Charset.class) {
      return hasText(text) ? Charset.forName(text.trim()) : null;
    }
    if (type == Currency.class) {
      return Currency.getInstance(hasText(text) ? text.trim() : text);
    }
    if (type == Locale.class) {
      return parseLocale(text);
    }
    if (type == Pattern.class) {
      return (text != null ? Pattern.compile(text) : null);
    }
    if (type == TimeZone.class) {
      return parseTimeZone(text);
    }
    if (type == URI.class) {
      return parseURI(text);
    }
    if (type == URL.class) {
      return parseURL(text);
    }
    if (type == UUID.class) {
      return hasText(text) ? UUID.fromString(text.trim()) : null;
    }
    if (type == ZoneId.class) {
      return ZoneId.of(hasText(text) ? text.trim() : text);
    }
    throw new IllegalArgumentException("Unsupported default form conversion target: " + targetType);
  }

  private static String print(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof byte[] bytes) {
      return new String(bytes);
    }
    if (value instanceof char[] chars) {
      return new String(chars);
    }
    if (value instanceof Charset charset) {
      return charset.name();
    }
    if (value instanceof Currency currency) {
      return currency.getCurrencyCode();
    }
    if (value instanceof Pattern pattern) {
      return pattern.pattern();
    }
    if (value instanceof TimeZone timeZone) {
      return timeZone.getID();
    }
    if (value instanceof URL url) {
      return url.toExternalForm();
    }
    if (value instanceof ZoneId zoneId) {
      return zoneId.getId();
    }
    return value.toString();
  }

  private static Class<?> wrapperType(Class<?> type) {
    if (!type.isPrimitive()) {
      return type;
    }
    if (type == char.class) {
      return Character.class;
    }
    if (type == boolean.class) {
      return Boolean.class;
    }
    if (type == byte.class) {
      return Byte.class;
    }
    if (type == short.class) {
      return Short.class;
    }
    if (type == int.class) {
      return Integer.class;
    }
    if (type == long.class) {
      return Long.class;
    }
    if (type == float.class) {
      return Float.class;
    }
    if (type == double.class) {
      return Double.class;
    }
    return type;
  }

  private static Character parseCharacter(String text) {
    if (text == null || text.isEmpty()) {
      return null;
    }
    if (text.startsWith("\\u") && text.length() == 6) {
      return (char) Integer.parseInt(text.substring(2), 16);
    }
    if (text.length() == 1) {
      return text.charAt(0);
    }
    throw new IllegalArgumentException("String [" + text + "] with length " + text.length());
  }

  private static Boolean parseBoolean(String text) {
    String input = (text != null ? text.trim() : null);
    if (input == null || input.isEmpty()) {
      return null;
    }
    if ("true".equalsIgnoreCase(input)
        || "on".equalsIgnoreCase(input)
        || "yes".equalsIgnoreCase(input)
        || "1".equals(input)) {
      return Boolean.TRUE;
    }
    if ("false".equalsIgnoreCase(input)
        || "off".equalsIgnoreCase(input)
        || "no".equalsIgnoreCase(input)
        || "0".equals(input)) {
      return Boolean.FALSE;
    }
    throw new IllegalArgumentException("Invalid boolean value [" + text + "]");
  }

  private static Number parseNumber(String text, Class<? extends Number> targetType) {
    if (!hasText(text)) {
      return null;
    }
    String trimmed = trimAllWhitespace(text);
    if (targetType == Byte.class) {
      return isHexNumber(trimmed) ? Byte.decode(trimmed) : Byte.valueOf(trimmed);
    }
    if (targetType == Short.class) {
      return isHexNumber(trimmed) ? Short.decode(trimmed) : Short.valueOf(trimmed);
    }
    if (targetType == Integer.class) {
      return isHexNumber(trimmed) ? Integer.decode(trimmed) : Integer.valueOf(trimmed);
    }
    if (targetType == Long.class) {
      return isHexNumber(trimmed) ? Long.decode(trimmed) : Long.valueOf(trimmed);
    }
    if (targetType == Float.class) {
      return Float.valueOf(trimmed);
    }
    if (targetType == Double.class) {
      return Double.valueOf(trimmed);
    }
    if (targetType == BigDecimal.class) {
      return new BigDecimal(trimmed);
    }
    if (targetType == BigInteger.class) {
      return isHexNumber(trimmed) ? decodeBigInteger(trimmed) : new BigInteger(trimmed);
    }
    throw new IllegalArgumentException("Unsupported number type [" + targetType.getName() + "]");
  }

  private static boolean isHexNumber(String value) {
    int index = (value.startsWith("-") ? 1 : 0);
    return value.startsWith("0x", index)
        || value.startsWith("0X", index)
        || value.startsWith("#", index);
  }

  private static BigInteger decodeBigInteger(String value) {
    int radix = 10;
    int index = 0;
    boolean negative = false;
    if (value.startsWith("-")) {
      negative = true;
      index++;
    }
    if (value.startsWith("0x", index) || value.startsWith("0X", index)) {
      index += 2;
      radix = 16;
    } else if (value.startsWith("#", index)) {
      index++;
      radix = 16;
    } else if (value.startsWith("0", index) && value.length() > 1 + index) {
      index++;
      radix = 8;
    }
    BigInteger result = new BigInteger(value.substring(index), radix);
    return negative ? result.negate() : result;
  }

  private static Locale parseLocale(String localeValue) {
    if (localeValue == null || localeValue.isEmpty()) {
      return null;
    }
    if (!localeValue.contains("_") && !localeValue.contains(" ")) {
      validateLocalePart(localeValue);
      Locale resolved = Locale.forLanguageTag(localeValue);
      if (!resolved.getLanguage().isEmpty()) {
        return resolved;
      }
    }
    return parseLocaleString(localeValue);
  }

  @SuppressWarnings("deprecation")
  private static Locale parseLocaleString(String localeString) {
    String delimiter = "_";
    if (!localeString.contains("_") && localeString.contains(" ")) {
      delimiter = " ";
    }

    String[] tokens = localeString.split(delimiter, -1);
    if (tokens.length == 1) {
      String language = tokens[0];
      validateLocalePart(language);
      return new Locale(language);
    } else if (tokens.length == 2) {
      String language = tokens[0];
      validateLocalePart(language);
      String country = tokens[1];
      validateLocalePart(country);
      return new Locale(language, country);
    } else if (tokens.length > 2) {
      String language = tokens[0];
      validateLocalePart(language);
      String country = tokens[1];
      validateLocalePart(country);
      String variant = String.join(delimiter, Arrays.copyOfRange(tokens, 2, tokens.length));
      return new Locale(language, country, variant);
    }
    throw new IllegalArgumentException("Invalid locale format: '" + localeString + "'");
  }

  private static void validateLocalePart(String localePart) {
    for (int i = 0; i < localePart.length(); i++) {
      char ch = localePart.charAt(i);
      if (ch != ' ' && ch != '_' && ch != '-' && ch != '#' && !Character.isLetterOrDigit(ch)) {
        throw new IllegalArgumentException(
            "Locale part \"" + localePart + "\" contains invalid characters");
      }
    }
  }

  private static TimeZone parseTimeZone(String timeZoneString) {
    String value = hasText(timeZoneString) ? timeZoneString.trim() : timeZoneString;
    TimeZone timeZone = TimeZone.getTimeZone(value);
    if ("GMT".equals(timeZone.getID()) && (value == null || !value.startsWith("GMT"))) {
      throw new IllegalArgumentException(
          "Invalid time zone specification '" + timeZoneString + "'");
    }
    return timeZone;
  }

  private static URI parseURI(String text) {
    if (!hasText(text)) {
      return null;
    }
    String value = text.trim();
    try {
      int colonIndex = value.indexOf(':');
      int fragmentIndex = value.indexOf('#');
      if (colonIndex >= 0 && (fragmentIndex < 0 || colonIndex < fragmentIndex)) {
        String scheme = value.substring(0, colonIndex);
        String ssp =
            value.substring(colonIndex + 1, fragmentIndex > 0 ? fragmentIndex : value.length());
        String fragment = (fragmentIndex > 0 ? value.substring(fragmentIndex + 1) : null);
        return new URI(scheme, ssp, fragment);
      }
      return new URI(value);
    } catch (URISyntaxException ex) {
      throw new IllegalArgumentException("Invalid URI value [" + text + "]", ex);
    }
  }

  private static URL parseURL(String text) {
    if (!hasText(text)) {
      return null;
    }
    try {
      return new URL(text.trim());
    } catch (MalformedURLException ex) {
      throw new IllegalArgumentException("Invalid URL value [" + text + "]", ex);
    }
  }

  private static String trimAllWhitespace(String text) {
    StringBuilder result = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); i++) {
      char ch = text.charAt(i);
      if (!Character.isWhitespace(ch)) {
        result.append(ch);
      }
    }
    return result.toString();
  }

  private static boolean hasText(String text) {
    return text != null && !text.isBlank();
  }

  // -- DATE

  /** Formatter for <code>java.util.Date</code> values. */
  public static class DateFormatter extends Formatters.SimpleFormatter<Date> {

    private final MessagesApi messagesApi;

    private final String pattern;

    private final String patternNoApp;

    /**
     * Creates a date formatter. The value defined for the message file key "formats.date" will be
     * used as the default pattern.
     *
     * @param messagesApi messages to look up the pattern
     */
    public DateFormatter(MessagesApi messagesApi) {
      this(messagesApi, "formats.date");
    }

    /**
     * Creates a date formatter.
     *
     * @param messagesApi messages to look up the pattern
     * @param pattern date pattern, as specified for {@link SimpleDateFormat}. Can be a message file
     *     key.
     */
    public DateFormatter(MessagesApi messagesApi, String pattern) {
      this(messagesApi, pattern, "yyyy-MM-dd");
    }

    /**
     * Creates a date formatter.
     *
     * @param messagesApi messages to look up the pattern
     * @param pattern date pattern, as specified for {@link SimpleDateFormat}. Can be a message file
     *     key.
     * @param patternNoApp date pattern to use as fallback when no app is started.
     */
    public DateFormatter(MessagesApi messagesApi, String pattern, String patternNoApp) {
      this.messagesApi = messagesApi;
      this.pattern = pattern;
      this.patternNoApp = patternNoApp;
    }

    /**
     * Binds the field - constructs a concrete value from submitted data.
     *
     * @param text the field text
     * @param locale the current <code>Locale</code>
     * @return a new value
     */
    public Date parse(String text, Locale locale) throws java.text.ParseException {
      if (text == null || text.trim().isEmpty()) {
        return null;
      }
      Lang lang = new Lang(locale);
      SimpleDateFormat sdf =
          new SimpleDateFormat(
              Optional.ofNullable(this.messagesApi)
                  .map(messages -> messages.get(lang, pattern))
                  .orElse(patternNoApp),
              locale);
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
      if (value == null) {
        return "";
      }
      Lang lang = new Lang(locale);
      return new SimpleDateFormat(
              Optional.ofNullable(this.messagesApi)
                  .map(messages -> messages.get(lang, pattern))
                  .orElse(patternNoApp),
              locale)
          .format(value);
    }
  }

  /** Defines the format for a <code>Date</code> field. */
  @Target({FIELD})
  @Retention(RUNTIME)
  @play.data.Form.Display(
      name = "format.date",
      attributes = {"pattern"})
  public static @interface DateTime {

    /**
     * Date pattern, as specified for {@link SimpleDateFormat}.
     *
     * @return the date pattern
     */
    String pattern();
  }

  /** Annotation formatter, triggered by the <code>@DateTime</code> annotation. */
  public static class AnnotationDateFormatter
      extends Formatters.AnnotationFormatter<DateTime, Date> {

    private final MessagesApi messagesApi;

    /**
     * Creates an annotation date formatter.
     *
     * @param messagesApi messages to look up the pattern
     */
    public AnnotationDateFormatter(MessagesApi messagesApi) {
      this.messagesApi = messagesApi;
    }

    /**
     * Binds the field - constructs a concrete value from submitted data.
     *
     * @param annotation the annotation that triggered this formatter
     * @param text the field text
     * @param locale the current <code>Locale</code>
     * @return a new value
     */
    public Date parse(DateTime annotation, String text, Locale locale)
        throws java.text.ParseException {
      if (text == null || text.trim().isEmpty()) {
        return null;
      }
      Lang lang = new Lang(locale);
      SimpleDateFormat sdf =
          new SimpleDateFormat(
              Optional.ofNullable(this.messagesApi)
                  .map(messages -> messages.get(lang, annotation.pattern()))
                  .orElse(annotation.pattern()),
              locale);
      sdf.setLenient(false);
      return sdf.parse(text);
    }

    /**
     * Unbinds this field - converts a concrete value to plain string
     *
     * @param annotation the annotation that triggered this formatter
     * @param value the value to unbind
     * @param locale the current <code>Locale</code>
     * @return printable version of the value
     */
    public String print(DateTime annotation, Date value, Locale locale) {
      if (value == null) {
        return "";
      }
      Lang lang = new Lang(locale);
      return new SimpleDateFormat(
              Optional.ofNullable(this.messagesApi)
                  .map(messages -> messages.get(lang, annotation.pattern()))
                  .orElse(annotation.pattern()),
              locale)
          .format(value);
    }
  }

  // -- STRING

  /** Defines the format for a <code>String</code> field that cannot be empty. */
  @Target({FIELD})
  @Retention(RUNTIME)
  public static @interface NonEmpty {}

  /** Annotation formatter, triggered by the <code>@NonEmpty</code> annotation. */
  public static class AnnotationNonEmptyFormatter
      extends Formatters.AnnotationFormatter<NonEmpty, String> {

    /**
     * Binds the field - constructs a concrete value from submitted data.
     *
     * @param annotation the annotation that triggered this formatter
     * @param text the field text
     * @param locale the current <code>Locale</code>
     * @return a new value
     */
    public String parse(NonEmpty annotation, String text, Locale locale)
        throws java.text.ParseException {
      if (text == null || text.trim().isEmpty()) {
        return null;
      }
      return text;
    }

    /**
     * Unbinds this field - converts a concrete value to plain string
     *
     * @param annotation the annotation that triggered this formatter
     * @param value the value to unbind
     * @param locale the current <code>Locale</code>
     * @return printable version of the value
     */
    public String print(NonEmpty annotation, String value, Locale locale) {
      if (value == null) {
        return "";
      }
      return value;
    }
  }
}
