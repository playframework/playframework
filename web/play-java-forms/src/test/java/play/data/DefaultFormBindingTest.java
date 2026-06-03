/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.Test;
import play.data.validation.Constraints;
import play.data.validation.ValidationError;
import play.i18n.Lang;
import play.libs.typedmap.TypedMap;
import play.mvc.Http.MultipartFormData.FilePart;
import play.test.WithApplication;

public class DefaultFormBindingTest extends WithApplication {

  private static Form<FormData> bind(FormFactory formFactory, Map<String, String> data) {
    return formFactory.form(FormData.class).bind(Lang.defaultLang(), TypedMap.empty(), data);
  }

  private static Form<FormData> bindDirect(FormFactory formFactory, Map<String, String> data) {
    return formFactory
        .form(FormData.class)
        .withDirectFieldAccess(true)
        .bind(Lang.defaultLang(), TypedMap.empty(), data);
  }

  private static Form<FormData> bindRequestData(
      FormFactory formFactory, Map<String, String[]> data) {
    return formFactory
        .form(FormData.class)
        .bindFromRequestData(Lang.defaultLang(), TypedMap.empty(), data);
  }

  private static Form<FormData> bindRequestDataDirect(
      FormFactory formFactory, Map<String, String[]> data) {
    return formFactory
        .form(FormData.class)
        .withDirectFieldAccess(true)
        .bindFromRequestData(Lang.defaultLang(), TypedMap.empty(), data);
  }

  private static void assertInvalidError(Form<?> form, String key) {
    assertThat(form.errors(key)).hasSize(1);
    ValidationError error = form.errors(key).get(0);
    assertThat(error.key()).isEqualTo(key);
    assertThat(error.messages()).containsExactly("error.invalid");
    assertThat(error.arguments()).isEmpty();
  }

  private static void assertInvalidDateError(Form<?> form, String key) {
    // Date conversion is handled by Play's ConversionService. When that conversion fails,
    // Spring's TypeConverterDelegate keeps the original ConversionFailedException instead of
    // replacing it with a generic Spring fallback error.
    assertThat(form.errors(key)).hasSize(1);
    ValidationError error = form.errors(key).get(0);
    assertThat(error.key()).isEqualTo(key);
    assertThat(error.messages()).containsExactly("error.invalid", "error.invalid.java.util.Date");
    assertThat(error.arguments()).isEmpty();
  }

  @Test
  public void shouldBindSimpleDefaultTypes() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("byteArray", "bytes");
    data.put("charArray", "chars");
    data.put("primitiveChar", "\\u0078");
    data.put("character", "\\u0041");
    data.put("charset", "UTF-8");
    data.put("currency", "EUR");
    data.put("locale", "de_AT_POSIX");
    data.put("localeLanguageOnly", "fr");
    data.put("localeWithMultiPartVariant", "en_US_POSIX_EXTRA");
    data.put("pattern", "a+");
    data.put("timeZone", "Europe/Vienna");
    data.put("timeZoneGmtOffset", "GMT+02:00");
    data.put("uri", "https://example.com/some path");
    data.put("uriMailto", "mailto:forms@example.test");
    data.put("uriCustomScheme", "widget:/catalog/items/42");
    data.put("uriRelative", "docs/releases/notes.txt");
    data.put("uriClasspath", "classpath:play/forms/sample.txt");
    data.put("uriWithFragment", "https://example.net/docs#chapter-7");
    data.put("uriWithEncodedFragment", "https://example.net/docs path#chapter 7");
    data.put("uriWithNonAscii", "https://example.com/café and £");
    data.put("uriAlreadyEncoded", "https://example.com/a%20path%20and%20%C2%A3");
    data.put("url", "https://example.com/index.html");
    data.put("urlMailto", "mailto:forms@example.test");
    data.put("uuid", "5801519c-a3e7-4cb2-b9eb-872c68586042");
    data.put("zoneId", "Europe/Vienna");
    data.put("zoneIdOffset", "+02:00");
    data.put("objectValue", "opaque text");
    data.put("staticValue", "ALPHA");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(new String(bean.getByteArray())).isEqualTo("bytes");
    assertThat(bean.getCharArray()).containsExactly('c', 'h', 'a', 'r', 's');
    assertThat(bean.getPrimitiveChar()).isEqualTo('x');
    assertThat(bean.getCharacter()).isEqualTo('A');
    assertThat(bean.getCharset()).isEqualTo(StandardCharsets.UTF_8);
    assertThat(bean.getCurrency()).isEqualTo(Currency.getInstance("EUR"));
    assertThat(bean.getLocale()).isEqualTo(new Locale("de", "AT", "POSIX"));
    assertThat(bean.getLocale().getLanguage()).isEqualTo("de");
    assertThat(bean.getLocale().getCountry()).isEqualTo("AT");
    assertThat(bean.getLocale().getVariant()).isEqualTo("POSIX");
    assertThat(bean.getLocaleLanguageOnly()).isEqualTo(Locale.FRENCH);
    assertThat(bean.getLocaleWithMultiPartVariant())
        .isEqualTo(new Locale("en", "US", "POSIX_EXTRA"));
    assertThat(bean.getPattern().pattern()).isEqualTo("a+");
    assertThat(bean.getTimeZone()).isEqualTo(TimeZone.getTimeZone("Europe/Vienna"));
    assertThat(bean.getTimeZoneGmtOffset()).isEqualTo(TimeZone.getTimeZone("GMT+02:00"));
    assertThat(bean.getUri()).isEqualTo(URI.create("https://example.com/some%20path"));
    assertThat(bean.getUriMailto()).isEqualTo(URI.create("mailto:forms@example.test"));
    assertThat(bean.getUriCustomScheme()).isEqualTo(URI.create("widget:/catalog/items/42"));
    assertThat(bean.getUriRelative()).isEqualTo(URI.create("docs/releases/notes.txt"));
    assertThat(bean.getUriClasspath()).isEqualTo(URI.create("classpath:play/forms/sample.txt"));
    assertThat(bean.getUriWithFragment())
        .isEqualTo(URI.create("https://example.net/docs#chapter-7"));
    assertThat(bean.getUriWithEncodedFragment())
        .isEqualTo(URI.create("https://example.net/docs%20path#chapter%207"));
    assertThat(bean.getUriWithNonAscii().toASCIIString())
        .isEqualTo("https://example.com/caf%C3%A9%20and%20%C2%A3");
    assertThat(bean.getUriAlreadyEncoded().toASCIIString())
        .isEqualTo("https://example.com/a%2520path%2520and%2520%25C2%25A3");
    assertThat(bean.getUrl().toExternalForm()).isEqualTo("https://example.com/index.html");
    assertThat(bean.getUrlMailto().toExternalForm()).isEqualTo("mailto:forms@example.test");
    assertThat(bean.getUuid()).isEqualTo(UUID.fromString("5801519c-a3e7-4cb2-b9eb-872c68586042"));
    assertThat(bean.getZoneId()).isEqualTo(ZoneId.of("Europe/Vienna"));
    assertThat(bean.getZoneIdOffset()).isEqualTo(ZoneId.of("+02:00"));
    assertThat(bean.getObjectValue()).isEqualTo("opaque text");
    assertThat(bean.getStaticValue()).isSameAs(StaticValue.ALPHA);
  }

  @Test
  public void shouldBindSimpleDefaultTypesWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("byteArray", "bytes");
    data.put("charArray", "chars");
    data.put("primitiveChar", "\\u0078");
    data.put("character", "\\u0041");
    data.put("charset", "UTF-8");
    data.put("currency", "EUR");
    data.put("locale", "de_AT_POSIX");
    data.put("localeLanguageOnly", "fr");
    data.put("localeWithMultiPartVariant", "en_US_POSIX_EXTRA");
    data.put("pattern", "a+");
    data.put("timeZone", "Europe/Vienna");
    data.put("timeZoneGmtOffset", "GMT+02:00");
    data.put("uri", "https://example.com/some path");
    data.put("uriMailto", "mailto:forms@example.test");
    data.put("uriCustomScheme", "widget:/catalog/items/42");
    data.put("uriRelative", "docs/releases/notes.txt");
    data.put("uriClasspath", "classpath:play/forms/sample.txt");
    data.put("uriWithFragment", "https://example.net/docs#chapter-7");
    data.put("uriWithEncodedFragment", "https://example.net/docs path#chapter 7");
    data.put("uriWithNonAscii", "https://example.com/café and £");
    data.put("uriAlreadyEncoded", "https://example.com/a%20path%20and%20%C2%A3");
    data.put("url", "https://example.com/index.html");
    data.put("urlMailto", "mailto:forms@example.test");
    data.put("uuid", "5801519c-a3e7-4cb2-b9eb-872c68586042");
    data.put("zoneId", "Europe/Vienna");
    data.put("zoneIdOffset", "+02:00");
    data.put("objectValue", "opaque text");
    data.put("staticValue", "ALPHA");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(new String(bean.byteArray)).isEqualTo("bytes");
    assertThat(bean.charArray).containsExactly('c', 'h', 'a', 'r', 's');
    assertThat(bean.primitiveChar).isEqualTo('x');
    assertThat(bean.character).isEqualTo('A');
    assertThat(bean.charset).isEqualTo(StandardCharsets.UTF_8);
    assertThat(bean.currency).isEqualTo(Currency.getInstance("EUR"));
    assertThat(bean.locale).isEqualTo(new Locale("de", "AT", "POSIX"));
    assertThat(bean.locale.getLanguage()).isEqualTo("de");
    assertThat(bean.locale.getCountry()).isEqualTo("AT");
    assertThat(bean.locale.getVariant()).isEqualTo("POSIX");
    assertThat(bean.localeLanguageOnly).isEqualTo(Locale.FRENCH);
    assertThat(bean.localeWithMultiPartVariant).isEqualTo(new Locale("en", "US", "POSIX_EXTRA"));
    assertThat(bean.pattern.pattern()).isEqualTo("a+");
    assertThat(bean.timeZone).isEqualTo(TimeZone.getTimeZone("Europe/Vienna"));
    assertThat(bean.timeZoneGmtOffset).isEqualTo(TimeZone.getTimeZone("GMT+02:00"));
    assertThat(bean.uri).isEqualTo(URI.create("https://example.com/some%20path"));
    assertThat(bean.uriMailto).isEqualTo(URI.create("mailto:forms@example.test"));
    assertThat(bean.uriCustomScheme).isEqualTo(URI.create("widget:/catalog/items/42"));
    assertThat(bean.uriRelative).isEqualTo(URI.create("docs/releases/notes.txt"));
    assertThat(bean.uriClasspath).isEqualTo(URI.create("classpath:play/forms/sample.txt"));
    assertThat(bean.uriWithFragment).isEqualTo(URI.create("https://example.net/docs#chapter-7"));
    assertThat(bean.uriWithEncodedFragment)
        .isEqualTo(URI.create("https://example.net/docs%20path#chapter%207"));
    assertThat(bean.uriWithNonAscii.toASCIIString())
        .isEqualTo("https://example.com/caf%C3%A9%20and%20%C2%A3");
    assertThat(bean.uriAlreadyEncoded.toASCIIString())
        .isEqualTo("https://example.com/a%2520path%2520and%2520%25C2%25A3");
    assertThat(bean.url.toExternalForm()).isEqualTo("https://example.com/index.html");
    assertThat(bean.urlMailto.toExternalForm()).isEqualTo("mailto:forms@example.test");
    assertThat(bean.uuid).isEqualTo(UUID.fromString("5801519c-a3e7-4cb2-b9eb-872c68586042"));
    assertThat(bean.zoneId).isEqualTo(ZoneId.of("Europe/Vienna"));
    assertThat(bean.zoneIdOffset).isEqualTo(ZoneId.of("+02:00"));
    assertThat(bean.objectValue).isEqualTo("opaque text");
    assertThat(bean.staticValue).isSameAs(StaticValue.ALPHA);
  }

  @Test
  public void shouldBindDefaultBooleanValues() {
    FormFactory formFactory = instanceOf(FormFactory.class);

    assertDefaultBooleanValue(formFactory, "true", true);
    assertDefaultBooleanValue(formFactory, "on", true);
    assertDefaultBooleanValue(formFactory, "yes", true);
    assertDefaultBooleanValue(formFactory, "1", true);
    assertDefaultBooleanValue(formFactory, "TRUE", true);
    assertDefaultBooleanValue(formFactory, "True", true);
    assertDefaultBooleanValue(formFactory, "ON", true);
    assertDefaultBooleanValue(formFactory, "On", true);
    assertDefaultBooleanValue(formFactory, "YES", true);
    assertDefaultBooleanValue(formFactory, "Yes", true);
    assertDefaultBooleanValue(formFactory, " true ", true);
    assertDefaultBooleanValue(formFactory, " on ", true);
    assertDefaultBooleanValue(formFactory, " yes ", true);
    assertDefaultBooleanValue(formFactory, " 1 ", true);
    assertDefaultBooleanValue(formFactory, "false", false);
    assertDefaultBooleanValue(formFactory, "off", false);
    assertDefaultBooleanValue(formFactory, "no", false);
    assertDefaultBooleanValue(formFactory, "0", false);
    assertDefaultBooleanValue(formFactory, "FALSE", false);
    assertDefaultBooleanValue(formFactory, "False", false);
    assertDefaultBooleanValue(formFactory, "OFF", false);
    assertDefaultBooleanValue(formFactory, "Off", false);
    assertDefaultBooleanValue(formFactory, "NO", false);
    assertDefaultBooleanValue(formFactory, "No", false);
    assertDefaultBooleanValue(formFactory, " false ", false);
    assertDefaultBooleanValue(formFactory, " off ", false);
    assertDefaultBooleanValue(formFactory, " no ", false);
    assertDefaultBooleanValue(formFactory, " 0 ", false);
  }

  @Test
  public void shouldBindDefaultBooleanValuesWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);

    assertDefaultDirectBooleanValue(formFactory, "true", true);
    assertDefaultDirectBooleanValue(formFactory, "on", true);
    assertDefaultDirectBooleanValue(formFactory, "yes", true);
    assertDefaultDirectBooleanValue(formFactory, "1", true);
    assertDefaultDirectBooleanValue(formFactory, "TRUE", true);
    assertDefaultDirectBooleanValue(formFactory, "True", true);
    assertDefaultDirectBooleanValue(formFactory, "ON", true);
    assertDefaultDirectBooleanValue(formFactory, "On", true);
    assertDefaultDirectBooleanValue(formFactory, "YES", true);
    assertDefaultDirectBooleanValue(formFactory, "Yes", true);
    assertDefaultDirectBooleanValue(formFactory, " true ", true);
    assertDefaultDirectBooleanValue(formFactory, " on ", true);
    assertDefaultDirectBooleanValue(formFactory, " yes ", true);
    assertDefaultDirectBooleanValue(formFactory, " 1 ", true);
    assertDefaultDirectBooleanValue(formFactory, "false", false);
    assertDefaultDirectBooleanValue(formFactory, "off", false);
    assertDefaultDirectBooleanValue(formFactory, "no", false);
    assertDefaultDirectBooleanValue(formFactory, "0", false);
    assertDefaultDirectBooleanValue(formFactory, "FALSE", false);
    assertDefaultDirectBooleanValue(formFactory, "False", false);
    assertDefaultDirectBooleanValue(formFactory, "OFF", false);
    assertDefaultDirectBooleanValue(formFactory, "Off", false);
    assertDefaultDirectBooleanValue(formFactory, "NO", false);
    assertDefaultDirectBooleanValue(formFactory, "No", false);
    assertDefaultDirectBooleanValue(formFactory, " false ", false);
    assertDefaultDirectBooleanValue(formFactory, " off ", false);
    assertDefaultDirectBooleanValue(formFactory, " no ", false);
    assertDefaultDirectBooleanValue(formFactory, " 0 ", false);
  }

  private static void assertDefaultBooleanValue(
      FormFactory formFactory, String value, boolean expected) {
    Form<FormData> form =
        bind(formFactory, Map.of("primitiveBoolean", value, "booleanValue", value));

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.isPrimitiveBoolean()).isEqualTo(expected);
    assertThat(bean.getBooleanValue()).isEqualTo(expected);
  }

  private static void assertDefaultDirectBooleanValue(
      FormFactory formFactory, String value, boolean expected) {
    Form<FormData> form =
        bindDirect(formFactory, Map.of("primitiveBoolean", value, "booleanValue", value));

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.primitiveBoolean).isEqualTo(expected);
    assertThat(bean.booleanValue).isEqualTo(expected);
  }

  @Test
  public void shouldRejectInvalidDefaultBooleanValues() {
    FormFactory formFactory = instanceOf(FormFactory.class);

    assertInvalidBooleanValue(formFactory, "maybe");
    assertInvalidBooleanValue(formFactory, "2");
    assertInvalidBooleanValue(formFactory, "truthy");
  }

  @Test
  public void shouldRejectInvalidDefaultBooleanValuesWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);

    assertInvalidDirectBooleanValue(formFactory, "maybe");
    assertInvalidDirectBooleanValue(formFactory, "2");
    assertInvalidDirectBooleanValue(formFactory, "truthy");
  }

  private static void assertInvalidBooleanValue(FormFactory formFactory, String value) {
    Form<FormData> form =
        bind(formFactory, Map.of("primitiveBoolean", value, "booleanValue", value));

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "primitiveBoolean");
    assertInvalidError(form, "booleanValue");
  }

  private static void assertInvalidDirectBooleanValue(FormFactory formFactory, String value) {
    Form<FormData> form =
        bindDirect(formFactory, Map.of("primitiveBoolean", value, "booleanValue", value));

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "primitiveBoolean");
    assertInvalidError(form, "booleanValue");
  }

  @Test
  public void shouldHandleWhitespaceOnlyDefaultBooleanValues() {
    FormFactory formFactory = instanceOf(FormFactory.class);

    Form<FormData> wrapperForm = bind(formFactory, Map.of("booleanValue", "   "));

    assertThat(wrapperForm.errors()).isEmpty();
    assertThat(wrapperForm.get().getBooleanValue()).isNull();

    Form<FormData> primitiveForm = bind(formFactory, Map.of("primitiveBoolean", "   "));

    assertThat(primitiveForm.hasErrors()).isTrue();
    assertInvalidError(primitiveForm, "primitiveBoolean");
  }

  @Test
  public void shouldHandleWhitespaceOnlyDefaultBooleanValuesWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);

    Form<FormData> wrapperForm = bindDirect(formFactory, Map.of("booleanValue", "   "));

    assertThat(wrapperForm.errors()).isEmpty();
    assertThat(wrapperForm.get().booleanValue).isNull();

    Form<FormData> primitiveForm = bindDirect(formFactory, Map.of("primitiveBoolean", "   "));

    assertThat(primitiveForm.hasErrors()).isTrue();
    assertInvalidError(primitiveForm, "primitiveBoolean");
  }

  @Test
  public void shouldBindDefaultNumberValues() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("primitiveByte", "111");
    data.put("byteValue", "112");
    data.put("primitiveShort", "113");
    data.put("shortValue", "114");
    data.put("primitiveInt", "115");
    data.put("integerValue", "116");
    data.put("primitiveLong", "117");
    data.put("longValue", "118");
    data.put("primitiveFloat", "119.5");
    data.put("floatValue", "120.5");
    data.put("primitiveDouble", "121.5");
    data.put("doubleValue", "122.5");
    data.put("bigDecimal", "12345.67");
    data.put("bigInteger", "123456789");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.getPrimitiveByte()).isEqualTo((byte) 111);
    assertThat(bean.getByteValue()).isEqualTo((byte) 112);
    assertThat(bean.getPrimitiveShort()).isEqualTo((short) 113);
    assertThat(bean.getShortValue()).isEqualTo((short) 114);
    assertThat(bean.getPrimitiveInt()).isEqualTo(115);
    assertThat(bean.getIntegerValue()).isEqualTo(116);
    assertThat(bean.getPrimitiveLong()).isEqualTo(117L);
    assertThat(bean.getLongValue()).isEqualTo(118L);
    assertThat(bean.getPrimitiveFloat()).isEqualTo(119.5f);
    assertThat(bean.getFloatValue()).isEqualTo(120.5f);
    assertThat(bean.getPrimitiveDouble()).isEqualTo(121.5d);
    assertThat(bean.getDoubleValue()).isEqualTo(122.5d);
    assertThat(bean.getBigDecimal()).isEqualByComparingTo(new BigDecimal("12345.67"));
    assertThat(bean.getBigInteger()).isEqualTo(new BigInteger("123456789"));
  }

  @Test
  public void shouldBindDefaultNumberValuesWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("primitiveByte", "111");
    data.put("byteValue", "112");
    data.put("primitiveShort", "113");
    data.put("shortValue", "114");
    data.put("primitiveInt", "115");
    data.put("integerValue", "116");
    data.put("primitiveLong", "117");
    data.put("longValue", "118");
    data.put("primitiveFloat", "119.5");
    data.put("floatValue", "120.5");
    data.put("primitiveDouble", "121.5");
    data.put("doubleValue", "122.5");
    data.put("bigDecimal", "12345.67");
    data.put("bigInteger", "123456789");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.primitiveByte).isEqualTo((byte) 111);
    assertThat(bean.byteValue).isEqualTo((byte) 112);
    assertThat(bean.primitiveShort).isEqualTo((short) 113);
    assertThat(bean.shortValue).isEqualTo((short) 114);
    assertThat(bean.primitiveInt).isEqualTo(115);
    assertThat(bean.integerValue).isEqualTo(116);
    assertThat(bean.primitiveLong).isEqualTo(117L);
    assertThat(bean.longValue).isEqualTo(118L);
    assertThat(bean.primitiveFloat).isEqualTo(119.5f);
    assertThat(bean.floatValue).isEqualTo(120.5f);
    assertThat(bean.primitiveDouble).isEqualTo(121.5d);
    assertThat(bean.doubleValue).isEqualTo(122.5d);
    assertThat(bean.bigDecimal).isEqualByComparingTo(new BigDecimal("12345.67"));
    assertThat(bean.bigInteger).isEqualTo(new BigInteger("123456789"));
  }

  @Test
  public void shouldBindDefaultNumberValuesWithHexAndWhitespace() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("primitiveByte", " 0x7 ");
    data.put("byteValue", " #8 ");
    data.put("primitiveShort", " 0x71 ");
    data.put("shortValue", " #72 ");
    data.put("primitiveInt", " 0x73 ");
    data.put("integerValue", " #74 ");
    data.put("integerValueFromHexPrefix", "0x40");
    data.put("integerValueFromUppercaseHexPrefix", "0X41");
    data.put("integerValueWithLeadingZero", "0102");
    data.put("primitiveLong", " 0x75 ");
    data.put("longValue", " #76 ");
    data.put("longValueFromNegativeHexPrefix", "-0x4D");
    data.put("primitiveFloat", " 77.5 ");
    data.put("floatValue", " 78.5 ");
    data.put("primitiveDouble", " 79.5 ");
    data.put("doubleValue", " 80.5 ");
    data.put("bigDecimal", " 81.81 ");
    data.put("bigInteger", " 0x52 ");
    data.put("bigIntegerFromUppercaseHexPrefix", "0X53");
    data.put("bigIntegerFromHashHexPrefix", "#54");
    data.put("bigIntegerFromNegativeHexPrefix", "-0x55");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.getPrimitiveByte()).isEqualTo((byte) 7);
    assertThat(bean.getByteValue()).isEqualTo((byte) 8);
    assertThat(bean.getPrimitiveShort()).isEqualTo((short) 113);
    assertThat(bean.getShortValue()).isEqualTo((short) 114);
    assertThat(bean.getPrimitiveInt()).isEqualTo(115);
    assertThat(bean.getIntegerValue()).isEqualTo(116);
    assertThat(bean.getIntegerValueFromHexPrefix()).isEqualTo(64);
    assertThat(bean.getIntegerValueFromUppercaseHexPrefix()).isEqualTo(65);
    assertThat(bean.getIntegerValueWithLeadingZero()).isEqualTo(102);
    assertThat(bean.getPrimitiveLong()).isEqualTo(117L);
    assertThat(bean.getLongValue()).isEqualTo(118L);
    assertThat(bean.getLongValueFromNegativeHexPrefix()).isEqualTo(-77L);
    assertThat(bean.getPrimitiveFloat()).isEqualTo(77.5f);
    assertThat(bean.getFloatValue()).isEqualTo(78.5f);
    assertThat(bean.getPrimitiveDouble()).isEqualTo(79.5d);
    assertThat(bean.getDoubleValue()).isEqualTo(80.5d);
    assertThat(bean.getBigDecimal()).isEqualByComparingTo(new BigDecimal("81.81"));
    assertThat(bean.getBigInteger()).isEqualTo(new BigInteger("82"));
    assertThat(bean.getBigIntegerFromUppercaseHexPrefix()).isEqualTo(new BigInteger("83"));
    assertThat(bean.getBigIntegerFromHashHexPrefix()).isEqualTo(new BigInteger("84"));
    assertThat(bean.getBigIntegerFromNegativeHexPrefix()).isEqualTo(new BigInteger("-85"));
  }

  @Test
  public void shouldBindDefaultNumberValuesWithHexAndWhitespaceWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("primitiveByte", " 0x7 ");
    data.put("byteValue", " #8 ");
    data.put("primitiveShort", " 0x71 ");
    data.put("shortValue", " #72 ");
    data.put("primitiveInt", " 0x73 ");
    data.put("integerValue", " #74 ");
    data.put("integerValueFromHexPrefix", "0x40");
    data.put("integerValueFromUppercaseHexPrefix", "0X41");
    data.put("integerValueWithLeadingZero", "0102");
    data.put("primitiveLong", " 0x75 ");
    data.put("longValue", " #76 ");
    data.put("longValueFromNegativeHexPrefix", "-0x4D");
    data.put("primitiveFloat", " 77.5 ");
    data.put("floatValue", " 78.5 ");
    data.put("primitiveDouble", " 79.5 ");
    data.put("doubleValue", " 80.5 ");
    data.put("bigDecimal", " 81.81 ");
    data.put("bigInteger", " 0x52 ");
    data.put("bigIntegerFromUppercaseHexPrefix", "0X53");
    data.put("bigIntegerFromHashHexPrefix", "#54");
    data.put("bigIntegerFromNegativeHexPrefix", "-0x55");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.primitiveByte).isEqualTo((byte) 7);
    assertThat(bean.byteValue).isEqualTo((byte) 8);
    assertThat(bean.primitiveShort).isEqualTo((short) 113);
    assertThat(bean.shortValue).isEqualTo((short) 114);
    assertThat(bean.primitiveInt).isEqualTo(115);
    assertThat(bean.integerValue).isEqualTo(116);
    assertThat(bean.integerValueFromHexPrefix).isEqualTo(64);
    assertThat(bean.integerValueFromUppercaseHexPrefix).isEqualTo(65);
    assertThat(bean.integerValueWithLeadingZero).isEqualTo(102);
    assertThat(bean.primitiveLong).isEqualTo(117L);
    assertThat(bean.longValue).isEqualTo(118L);
    assertThat(bean.longValueFromNegativeHexPrefix).isEqualTo(-77L);
    assertThat(bean.primitiveFloat).isEqualTo(77.5f);
    assertThat(bean.floatValue).isEqualTo(78.5f);
    assertThat(bean.primitiveDouble).isEqualTo(79.5d);
    assertThat(bean.doubleValue).isEqualTo(80.5d);
    assertThat(bean.bigDecimal).isEqualByComparingTo(new BigDecimal("81.81"));
    assertThat(bean.bigInteger).isEqualTo(new BigInteger("82"));
    assertThat(bean.bigIntegerFromUppercaseHexPrefix).isEqualTo(new BigInteger("83"));
    assertThat(bean.bigIntegerFromHashHexPrefix).isEqualTo(new BigInteger("84"));
    assertThat(bean.bigIntegerFromNegativeHexPrefix).isEqualTo(new BigInteger("-85"));
  }

  @Test
  public void shouldBindDefaultNumberValuesWithInternalWhitespace() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("primitiveByte", "1 2");
    data.put("byteValue", "1 3");
    data.put("primitiveShort", "1 4");
    data.put("shortValue", "1 5");
    data.put("primitiveInt", "1 6");
    data.put("integerValue", "1 7");
    data.put("primitiveLong", "1 8");
    data.put("longValue", "1 9");
    data.put("primitiveFloat", "2 0.5");
    data.put("floatValue", "2 1.5");
    data.put("primitiveDouble", "2 2.5");
    data.put("doubleValue", "2 3.5");
    data.put("bigDecimal", "2 4.25");
    data.put("bigInteger", "2 5");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.getPrimitiveByte()).isEqualTo((byte) 12);
    assertThat(bean.getByteValue()).isEqualTo((byte) 13);
    assertThat(bean.getPrimitiveShort()).isEqualTo((short) 14);
    assertThat(bean.getShortValue()).isEqualTo((short) 15);
    assertThat(bean.getPrimitiveInt()).isEqualTo(16);
    assertThat(bean.getIntegerValue()).isEqualTo(17);
    assertThat(bean.getPrimitiveLong()).isEqualTo(18L);
    assertThat(bean.getLongValue()).isEqualTo(19L);
    assertThat(bean.getPrimitiveFloat()).isEqualTo(20.5f);
    assertThat(bean.getFloatValue()).isEqualTo(21.5f);
    assertThat(bean.getPrimitiveDouble()).isEqualTo(22.5d);
    assertThat(bean.getDoubleValue()).isEqualTo(23.5d);
    assertThat(bean.getBigDecimal()).isEqualByComparingTo(new BigDecimal("24.25"));
    assertThat(bean.getBigInteger()).isEqualTo(new BigInteger("25"));
  }

  @Test
  public void shouldBindDefaultNumberValuesWithInternalWhitespaceWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("primitiveByte", "1 2");
    data.put("byteValue", "1 3");
    data.put("primitiveShort", "1 4");
    data.put("shortValue", "1 5");
    data.put("primitiveInt", "1 6");
    data.put("integerValue", "1 7");
    data.put("primitiveLong", "1 8");
    data.put("longValue", "1 9");
    data.put("primitiveFloat", "2 0.5");
    data.put("floatValue", "2 1.5");
    data.put("primitiveDouble", "2 2.5");
    data.put("doubleValue", "2 3.5");
    data.put("bigDecimal", "2 4.25");
    data.put("bigInteger", "2 5");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.primitiveByte).isEqualTo((byte) 12);
    assertThat(bean.byteValue).isEqualTo((byte) 13);
    assertThat(bean.primitiveShort).isEqualTo((short) 14);
    assertThat(bean.shortValue).isEqualTo((short) 15);
    assertThat(bean.primitiveInt).isEqualTo(16);
    assertThat(bean.integerValue).isEqualTo(17);
    assertThat(bean.primitiveLong).isEqualTo(18L);
    assertThat(bean.longValue).isEqualTo(19L);
    assertThat(bean.primitiveFloat).isEqualTo(20.5f);
    assertThat(bean.floatValue).isEqualTo(21.5f);
    assertThat(bean.primitiveDouble).isEqualTo(22.5d);
    assertThat(bean.doubleValue).isEqualTo(23.5d);
    assertThat(bean.bigDecimal).isEqualByComparingTo(new BigDecimal("24.25"));
    assertThat(bean.bigInteger).isEqualTo(new BigInteger("25"));
  }

  @Test
  public void shouldAllowEmptyValuesForDefaultWrappers() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("character", "");
    data.put("booleanValue", "");
    data.put("byteValue", "");
    data.put("shortValue", "");
    data.put("integerValue", "");
    data.put("longValue", "");
    data.put("floatValue", "");
    data.put("doubleValue", "");
    data.put("bigDecimal", "");
    data.put("bigInteger", "");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.getCharacter()).isNull();
    assertThat(bean.getBooleanValue()).isNull();
    assertThat(bean.getByteValue()).isNull();
    assertThat(bean.getShortValue()).isNull();
    assertThat(bean.getIntegerValue()).isNull();
    assertThat(bean.getLongValue()).isNull();
    assertThat(bean.getFloatValue()).isNull();
    assertThat(bean.getDoubleValue()).isNull();
    assertThat(bean.getBigDecimal()).isNull();
    assertThat(bean.getBigInteger()).isNull();
  }

  @Test
  public void shouldAllowEmptyValuesForDefaultWrappersWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("character", "");
    data.put("booleanValue", "");
    data.put("byteValue", "");
    data.put("shortValue", "");
    data.put("integerValue", "");
    data.put("longValue", "");
    data.put("floatValue", "");
    data.put("doubleValue", "");
    data.put("bigDecimal", "");
    data.put("bigInteger", "");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.character).isNull();
    assertThat(bean.booleanValue).isNull();
    assertThat(bean.byteValue).isNull();
    assertThat(bean.shortValue).isNull();
    assertThat(bean.integerValue).isNull();
    assertThat(bean.longValue).isNull();
    assertThat(bean.floatValue).isNull();
    assertThat(bean.doubleValue).isNull();
    assertThat(bean.bigDecimal).isNull();
    assertThat(bean.bigInteger).isNull();
  }

  @Test
  public void shouldAllowWhitespaceOnlyValuesForDefaultNumberWrappers() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("byteValue", "   ");
    data.put("shortValue", "   ");
    data.put("integerValue", "   ");
    data.put("longValue", "   ");
    data.put("floatValue", "   ");
    data.put("doubleValue", "   ");
    data.put("bigDecimal", "   ");
    data.put("bigInteger", "   ");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.getByteValue()).isNull();
    assertThat(bean.getShortValue()).isNull();
    assertThat(bean.getIntegerValue()).isNull();
    assertThat(bean.getLongValue()).isNull();
    assertThat(bean.getFloatValue()).isNull();
    assertThat(bean.getDoubleValue()).isNull();
    assertThat(bean.getBigDecimal()).isNull();
    assertThat(bean.getBigInteger()).isNull();
  }

  @Test
  public void shouldAllowWhitespaceOnlyValuesForDefaultNumberWrappersWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("byteValue", "   ");
    data.put("shortValue", "   ");
    data.put("integerValue", "   ");
    data.put("longValue", "   ");
    data.put("floatValue", "   ");
    data.put("doubleValue", "   ");
    data.put("bigDecimal", "   ");
    data.put("bigInteger", "   ");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.byteValue).isNull();
    assertThat(bean.shortValue).isNull();
    assertThat(bean.integerValue).isNull();
    assertThat(bean.longValue).isNull();
    assertThat(bean.floatValue).isNull();
    assertThat(bean.doubleValue).isNull();
    assertThat(bean.bigDecimal).isNull();
    assertThat(bean.bigInteger).isNull();
  }

  @Test
  public void shouldRejectEmptyValuesForDefaultPrimitives() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("primitiveChar", "");
    data.put("primitiveBoolean", "");
    data.put("primitiveByte", "");
    data.put("primitiveShort", "");
    data.put("primitiveInt", "");
    data.put("primitiveLong", "");
    data.put("primitiveFloat", "");
    data.put("primitiveDouble", "");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "primitiveChar");
    assertInvalidError(form, "primitiveBoolean");
    assertInvalidError(form, "primitiveByte");
    assertInvalidError(form, "primitiveShort");
    assertInvalidError(form, "primitiveInt");
    assertInvalidError(form, "primitiveLong");
    assertInvalidError(form, "primitiveFloat");
    assertInvalidError(form, "primitiveDouble");
  }

  @Test
  public void shouldRejectEmptyValuesForDefaultPrimitivesWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("primitiveChar", "");
    data.put("primitiveBoolean", "");
    data.put("primitiveByte", "");
    data.put("primitiveShort", "");
    data.put("primitiveInt", "");
    data.put("primitiveLong", "");
    data.put("primitiveFloat", "");
    data.put("primitiveDouble", "");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "primitiveChar");
    assertInvalidError(form, "primitiveBoolean");
    assertInvalidError(form, "primitiveByte");
    assertInvalidError(form, "primitiveShort");
    assertInvalidError(form, "primitiveInt");
    assertInvalidError(form, "primitiveLong");
    assertInvalidError(form, "primitiveFloat");
    assertInvalidError(form, "primitiveDouble");
  }

  @Test
  public void shouldHandleNullValuesForDefaultTypes() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("primitiveChar", null);
    data.put("character", null);
    data.put("primitiveBoolean", null);
    data.put("booleanValue", null);
    data.put("primitiveByte", null);
    data.put("byteValue", null);
    data.put("primitiveShort", null);
    data.put("shortValue", null);
    data.put("primitiveInt", null);
    data.put("integerValue", null);
    data.put("primitiveLong", null);
    data.put("longValue", null);
    data.put("primitiveFloat", null);
    data.put("floatValue", null);
    data.put("primitiveDouble", null);
    data.put("doubleValue", null);
    data.put("bigDecimal", null);
    data.put("bigInteger", null);
    data.put("byteArray", null);
    data.put("charArray", null);
    data.put("charset", null);
    data.put("currency", null);
    data.put("locale", null);
    data.put("pattern", null);
    data.put("timeZone", null);
    data.put("uri", null);
    data.put("url", null);
    data.put("uuid", null);
    data.put("zoneId", null);
    data.put("optionalValue", null);

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "primitiveChar");
    assertInvalidError(form, "primitiveBoolean");
    assertInvalidError(form, "primitiveByte");
    assertInvalidError(form, "primitiveShort");
    assertInvalidError(form, "primitiveInt");
    assertInvalidError(form, "primitiveLong");
    assertInvalidError(form, "primitiveFloat");
    assertInvalidError(form, "primitiveDouble");
    FormData bean = form.discardingErrors().get();
    assertThat(bean.getCharacter()).isNull();
    assertThat(bean.getBooleanValue()).isNull();
    assertThat(bean.getByteValue()).isNull();
    assertThat(bean.getShortValue()).isNull();
    assertThat(bean.getIntegerValue()).isNull();
    assertThat(bean.getLongValue()).isNull();
    assertThat(bean.getFloatValue()).isNull();
    assertThat(bean.getDoubleValue()).isNull();
    assertThat(bean.getBigDecimal()).isNull();
    assertThat(bean.getBigInteger()).isNull();
    assertThat(bean.getByteArray()).isNull();
    assertThat(bean.getCharArray()).isNull();
    assertThat(bean.getCharset()).isNull();
    assertThat(bean.getCurrency()).isNull();
    assertThat(bean.getLocale()).isNull();
    assertThat(bean.getPattern()).isNull();
    assertThat(bean.getTimeZone()).isNull();
    assertThat(bean.getUri()).isNull();
    assertThat(bean.getUrl()).isNull();
    assertThat(bean.getUuid()).isNull();
    assertThat(bean.getZoneId()).isNull();
    assertThat(bean.getOptionalValue()).isEmpty();
  }

  @Test
  public void shouldHandleNullValuesForDefaultTypesWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("primitiveChar", null);
    data.put("character", null);
    data.put("primitiveBoolean", null);
    data.put("booleanValue", null);
    data.put("primitiveByte", null);
    data.put("byteValue", null);
    data.put("primitiveShort", null);
    data.put("shortValue", null);
    data.put("primitiveInt", null);
    data.put("integerValue", null);
    data.put("primitiveLong", null);
    data.put("longValue", null);
    data.put("primitiveFloat", null);
    data.put("floatValue", null);
    data.put("primitiveDouble", null);
    data.put("doubleValue", null);
    data.put("bigDecimal", null);
    data.put("bigInteger", null);
    data.put("byteArray", null);
    data.put("charArray", null);
    data.put("charset", null);
    data.put("currency", null);
    data.put("locale", null);
    data.put("pattern", null);
    data.put("timeZone", null);
    data.put("uri", null);
    data.put("url", null);
    data.put("uuid", null);
    data.put("zoneId", null);
    data.put("optionalValue", null);

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "primitiveChar");
    assertInvalidError(form, "primitiveBoolean");
    assertInvalidError(form, "primitiveByte");
    assertInvalidError(form, "primitiveShort");
    assertInvalidError(form, "primitiveInt");
    assertInvalidError(form, "primitiveLong");
    assertInvalidError(form, "primitiveFloat");
    assertInvalidError(form, "primitiveDouble");
    FormData bean = form.discardingErrors().get();
    assertThat(bean.character).isNull();
    assertThat(bean.booleanValue).isNull();
    assertThat(bean.byteValue).isNull();
    assertThat(bean.shortValue).isNull();
    assertThat(bean.integerValue).isNull();
    assertThat(bean.longValue).isNull();
    assertThat(bean.floatValue).isNull();
    assertThat(bean.doubleValue).isNull();
    assertThat(bean.bigDecimal).isNull();
    assertThat(bean.bigInteger).isNull();
    assertThat(bean.byteArray).isNull();
    assertThat(bean.charArray).isNull();
    assertThat(bean.charset).isNull();
    assertThat(bean.currency).isNull();
    assertThat(bean.locale).isNull();
    assertThat(bean.pattern).isNull();
    assertThat(bean.timeZone).isNull();
    assertThat(bean.uri).isNull();
    assertThat(bean.url).isNull();
    assertThat(bean.uuid).isNull();
    assertThat(bean.zoneId).isNull();
    assertThat(bean.optionalValue).isEmpty();
  }

  @Test
  public void shouldRejectInvalidDefaultCharacterValues() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("primitiveChar", "ab");
    data.put("character", "\\uZZZZ");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "primitiveChar");
    assertInvalidError(form, "character");
  }

  @Test
  public void shouldRejectInvalidDefaultCharacterValuesWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("primitiveChar", "ab");
    data.put("character", "\\uZZZZ");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "primitiveChar");
    assertInvalidError(form, "character");
  }

  @Test
  public void shouldBindDefaultCharacterValues() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    assertDefaultCharacterValue(formFactory, "c", "d", 'c', 'd');
    assertDefaultCharacterValue(formFactory, "\u0041", "\u0042", 'A', 'B');
    assertDefaultCharacterValue(formFactory, "\\u0022", " ", '"', ' ');
  }

  @Test
  public void shouldBindDefaultCharacterValuesWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    assertDefaultDirectCharacterValue(formFactory, "c", "d", 'c', 'd');
    assertDefaultDirectCharacterValue(formFactory, "\u0041", "\u0042", 'A', 'B');
    assertDefaultDirectCharacterValue(formFactory, "\\u0022", " ", '"', ' ');
  }

  private static void assertDefaultCharacterValue(
      FormFactory formFactory,
      String primitiveValue,
      String wrapperValue,
      char expectedPrimitiveValue,
      Character expectedWrapperValue) {
    Map<String, String> data = new HashMap<>();
    data.put("primitiveChar", primitiveValue);
    data.put("character", wrapperValue);

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.getPrimitiveChar()).isEqualTo(expectedPrimitiveValue);
    assertThat(bean.getCharacter()).isEqualTo(expectedWrapperValue);
  }

  private static void assertDefaultDirectCharacterValue(
      FormFactory formFactory,
      String primitiveValue,
      String wrapperValue,
      char expectedPrimitiveValue,
      Character expectedWrapperValue) {
    Map<String, String> data = new HashMap<>();
    data.put("primitiveChar", primitiveValue);
    data.put("character", wrapperValue);

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.primitiveChar).isEqualTo(expectedPrimitiveValue);
    assertThat(bean.character).isEqualTo(expectedWrapperValue);
  }

  @Test
  public void shouldRejectDefaultNumberValuesOutsideTargetRange() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("primitiveShort", String.valueOf(Short.MAX_VALUE + 1));
    data.put("shortValue", String.valueOf(Short.MAX_VALUE + 2));

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "primitiveShort");
    assertInvalidError(form, "shortValue");
  }

  @Test
  public void shouldRejectDefaultNumberValuesOutsideTargetRangeWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("primitiveShort", String.valueOf(Short.MAX_VALUE + 1));
    data.put("shortValue", String.valueOf(Short.MAX_VALUE + 2));

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "primitiveShort");
    assertInvalidError(form, "shortValue");
  }

  @Test
  public void shouldRejectWhitespaceOnlyValuesForDefaultNumberPrimitives() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("primitiveByte", "   ");
    data.put("primitiveShort", "   ");
    data.put("primitiveInt", "   ");
    data.put("primitiveLong", "   ");
    data.put("primitiveFloat", "   ");
    data.put("primitiveDouble", "   ");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "primitiveByte");
    assertInvalidError(form, "primitiveShort");
    assertInvalidError(form, "primitiveInt");
    assertInvalidError(form, "primitiveLong");
    assertInvalidError(form, "primitiveFloat");
    assertInvalidError(form, "primitiveDouble");
  }

  @Test
  public void shouldRejectWhitespaceOnlyValuesForDefaultNumberPrimitivesWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("primitiveByte", "   ");
    data.put("primitiveShort", "   ");
    data.put("primitiveInt", "   ");
    data.put("primitiveLong", "   ");
    data.put("primitiveFloat", "   ");
    data.put("primitiveDouble", "   ");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "primitiveByte");
    assertInvalidError(form, "primitiveShort");
    assertInvalidError(form, "primitiveInt");
    assertInvalidError(form, "primitiveLong");
    assertInvalidError(form, "primitiveFloat");
    assertInvalidError(form, "primitiveDouble");
  }

  @Test
  public void shouldTrimDefaultScalarValues() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("charset", " UTF-8 ");
    data.put("currency", " EUR ");
    data.put("timeZone", " Europe/Vienna ");
    data.put("uri", " https://example.com/some path ");
    data.put("url", " https://example.com/index.html ");
    data.put("uuid", " 5801519c-a3e7-4cb2-b9eb-872c68586042 ");
    data.put("zoneId", " Europe/Vienna ");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.getCharset()).isEqualTo(StandardCharsets.UTF_8);
    assertThat(bean.getCurrency()).isEqualTo(Currency.getInstance("EUR"));
    assertThat(bean.getTimeZone()).isEqualTo(TimeZone.getTimeZone("Europe/Vienna"));
    assertThat(bean.getUri()).isEqualTo(URI.create("https://example.com/some%20path"));
    assertThat(bean.getUrl().toExternalForm()).isEqualTo("https://example.com/index.html");
    assertThat(bean.getUuid()).isEqualTo(UUID.fromString("5801519c-a3e7-4cb2-b9eb-872c68586042"));
    assertThat(bean.getZoneId()).isEqualTo(ZoneId.of("Europe/Vienna"));
  }

  @Test
  public void shouldTrimDefaultScalarValuesWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("charset", " UTF-8 ");
    data.put("currency", " EUR ");
    data.put("timeZone", " Europe/Vienna ");
    data.put("uri", " https://example.com/some path ");
    data.put("url", " https://example.com/index.html ");
    data.put("uuid", " 5801519c-a3e7-4cb2-b9eb-872c68586042 ");
    data.put("zoneId", " Europe/Vienna ");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.charset).isEqualTo(StandardCharsets.UTF_8);
    assertThat(bean.currency).isEqualTo(Currency.getInstance("EUR"));
    assertThat(bean.timeZone).isEqualTo(TimeZone.getTimeZone("Europe/Vienna"));
    assertThat(bean.uri).isEqualTo(URI.create("https://example.com/some%20path"));
    assertThat(bean.url.toExternalForm()).isEqualTo("https://example.com/index.html");
    assertThat(bean.uuid).isEqualTo(UUID.fromString("5801519c-a3e7-4cb2-b9eb-872c68586042"));
    assertThat(bean.zoneId).isEqualTo(ZoneId.of("Europe/Vienna"));
  }

  @Test
  public void shouldPreserveWhitespaceForStringValues() {
    FormFactory formFactory = instanceOf(FormFactory.class);

    Form<FormData> form = bind(formFactory, Map.of("stringValue", " test "));

    assertThat(form.errors()).isEmpty();
    assertThat(form.get().getStringValue()).isEqualTo(" test ");
  }

  @Test
  public void shouldPreserveWhitespaceForStringValuesWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);

    Form<FormData> form = bindDirect(formFactory, Map.of("stringValue", " test "));

    assertThat(form.errors()).isEmpty();
    assertThat(form.get().stringValue).isEqualTo(" test ");
  }

  @Test
  public void shouldPreserveWhitespaceForStringArrayValues() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("stringArray[]", new String[] {" red ", " green "});

    Form<FormData> form = bindRequestData(formFactory, data);

    assertThat(form.errors()).isEmpty();
    assertThat(form.get().getStringArray()).containsExactly(" red ", " green ");
  }

  @Test
  public void shouldBindSingleStringValueToStringArray() {
    FormFactory formFactory = instanceOf(FormFactory.class);

    Form<FormData> form = bind(formFactory, Map.of("stringArray", "0,1,2"));

    assertThat(form.errors()).isEmpty();
    assertThat(form.get().getStringArray()).containsExactly("0,1,2");
  }

  @Test
  public void shouldBindSingleStringValueToNumberArrays() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("integerArray", "123");
    data.put("primitiveIntArray", "124");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    assertThat(form.get().getIntegerArray()).containsExactly(123);
    assertThat(form.get().getPrimitiveIntArray()).containsExactly(124);
  }

  @Test
  public void shouldPreserveCommaSeparatedSingleStringValueForStringArray() {
    FormFactory formFactory = instanceOf(FormFactory.class);

    Form<FormData> form = bind(formFactory, Map.of("stringArray", " 0,1 , 2 "));

    assertThat(form.errors()).isEmpty();
    assertThat(form.get().getStringArray()).containsExactly(" 0,1 , 2 ");
  }

  @Test
  public void shouldBindEmptySingleStringValueToStringArray() {
    FormFactory formFactory = instanceOf(FormFactory.class);

    Form<FormData> form = bind(formFactory, Map.of("stringArray", ""));

    assertThat(form.errors()).isEmpty();
    assertThat(form.get().getStringArray()).containsExactly("");
  }

  @Test
  public void shouldPreserveWhitespaceForStringArrayValuesWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("stringArray[]", new String[] {" red ", " green "});

    Form<FormData> form = bindRequestDataDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    assertThat(form.get().stringArray).containsExactly(" red ", " green ");
  }

  @Test
  public void shouldBindSingleStringValueToStringArrayWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);

    Form<FormData> form = bindDirect(formFactory, Map.of("stringArray", "0,1,2"));

    assertThat(form.errors()).isEmpty();
    assertThat(form.get().stringArray).containsExactly("0,1,2");
  }

  @Test
  public void shouldBindSingleStringValueToNumberArraysWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("integerArray", "123");
    data.put("primitiveIntArray", "124");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    assertThat(form.get().integerArray).containsExactly(123);
    assertThat(form.get().primitiveIntArray).containsExactly(124);
  }

  @Test
  public void shouldPreserveCommaSeparatedSingleStringValueForStringArrayWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);

    Form<FormData> form = bindDirect(formFactory, Map.of("stringArray", " 0,1 , 2 "));

    assertThat(form.errors()).isEmpty();
    assertThat(form.get().stringArray).containsExactly(" 0,1 , 2 ");
  }

  @Test
  public void shouldBindEmptySingleStringValueToStringArrayWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);

    Form<FormData> form = bindDirect(formFactory, Map.of("stringArray", ""));

    assertThat(form.errors()).isEmpty();
    assertThat(form.get().stringArray).containsExactly("");
  }

  @Test
  public void shouldBindLocaleValuesWithSpaceSeparators() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Form<FormData> form = bind(formFactory, Map.of("locale", "de AT POSIX"));

    assertThat(form.errors()).isEmpty();
    assertThat(form.get().getLocale()).isEqualTo(new Locale("de", "AT", "POSIX"));
  }

  @Test
  public void shouldBindLocaleValuesWithSpaceSeparatorsWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Form<FormData> form = bindDirect(formFactory, Map.of("locale", "de AT POSIX"));

    assertThat(form.errors()).isEmpty();
    assertThat(form.get().locale).isEqualTo(new Locale("de", "AT", "POSIX"));
  }

  @Test
  public void shouldBindLocaleValuesWithLanguageTags() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Form<FormData> form = bind(formFactory, Map.of("locale", "de-AT-POSIX"));

    assertThat(form.errors()).isEmpty();
    assertThat(form.get().getLocale()).isEqualTo(new Locale("de", "AT", "POSIX"));
    assertThat(form.get().getLocale().getLanguage()).isEqualTo("de");
    assertThat(form.get().getLocale().getCountry()).isEqualTo("AT");
    assertThat(form.get().getLocale().getVariant()).isEqualTo("POSIX");

    Form<FormData> scriptForm = bind(formFactory, Map.of("locale", "zh-Hans"));

    assertThat(scriptForm.errors()).isEmpty();
    assertThat(scriptForm.get().getLocale()).isEqualTo(Locale.forLanguageTag("zh-Hans"));
    assertThat(scriptForm.get().getLocale().getLanguage()).isEqualTo("zh");
    assertThat(scriptForm.get().getLocale().getScript()).isEqualTo("Hans");
  }

  @Test
  public void shouldBindLocaleValuesWithLanguageTagsWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Form<FormData> form = bindDirect(formFactory, Map.of("locale", "de-AT-POSIX"));

    assertThat(form.errors()).isEmpty();
    assertThat(form.get().locale).isEqualTo(new Locale("de", "AT", "POSIX"));
    assertThat(form.get().locale.getLanguage()).isEqualTo("de");
    assertThat(form.get().locale.getCountry()).isEqualTo("AT");
    assertThat(form.get().locale.getVariant()).isEqualTo("POSIX");

    Form<FormData> scriptForm = bindDirect(formFactory, Map.of("locale", "zh-Hans"));

    assertThat(scriptForm.errors()).isEmpty();
    assertThat(scriptForm.get().locale).isEqualTo(Locale.forLanguageTag("zh-Hans"));
    assertThat(scriptForm.get().locale.getLanguage()).isEqualTo("zh");
    assertThat(scriptForm.get().locale.getScript()).isEqualTo("Hans");
  }

  @Test
  public void shouldBindDefaultEnumValues() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("sampleEnum", " SECOND ");
    data.put("sampleEnumAlias", "ALIAS");
    data.put("sampleEnumMap[FIRST]", "THIRD");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.getSampleEnum()).isEqualTo(SampleEnum.SECOND);
    assertThat(bean.getSampleEnumAlias()).isEqualTo(SampleEnum.SECOND);
    assertThat(bean.getSampleEnumMap()).containsEntry(SampleEnum.FIRST, SampleEnum.THIRD);
  }

  @Test
  public void shouldBindDefaultEnumValuesWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("sampleEnum", " SECOND ");
    data.put("sampleEnumAlias", "ALIAS");
    data.put("sampleEnumMap[FIRST]", "THIRD");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.sampleEnum).isEqualTo(SampleEnum.SECOND);
    assertThat(bean.sampleEnumAlias).isEqualTo(SampleEnum.SECOND);
    assertThat(bean.sampleEnumMap).containsEntry(SampleEnum.FIRST, SampleEnum.THIRD);
  }

  @Test
  public void shouldBindCommaSeparatedSingleStringToEnumArrayAndCollection() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("sampleEnumArray", "FIRST,SECOND");
    data.put("sampleEnumList", "SECOND,THIRD");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.getSampleEnumArray()).containsExactly(SampleEnum.FIRST, SampleEnum.SECOND);
    assertThat(bean.getSampleEnumList()).containsExactly(SampleEnum.SECOND, SampleEnum.THIRD);
  }

  @Test
  public void shouldBindCommaSeparatedSingleStringToEnumArrayAndCollectionWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("sampleEnumArray", "FIRST,SECOND");
    data.put("sampleEnumList", "SECOND,THIRD");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.sampleEnumArray).containsExactly(SampleEnum.FIRST, SampleEnum.SECOND);
    assertThat(bean.sampleEnumList).containsExactly(SampleEnum.SECOND, SampleEnum.THIRD);
  }

  @Test
  public void shouldHandleEmptyDefaultEnumValues() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("sampleEnum", "");
    data.put("sampleEnumArray[0]", "");
    data.put("sampleEnumList[0]", "");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.discardingErrors().get();
    assertThat(bean.getSampleEnum()).isNull();
    assertThat(bean.getSampleEnumArray()).containsNull();
    assertThat(bean.getSampleEnumList()).containsNull();
  }

  @Test
  public void shouldHandleEmptyDefaultEnumValuesWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("sampleEnum", "");
    data.put("sampleEnumArray[0]", "");
    data.put("sampleEnumList[0]", "");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.discardingErrors().get();
    assertThat(bean.sampleEnum).isNull();
    assertThat(bean.sampleEnumArray).containsNull();
    assertThat(bean.sampleEnumList).containsNull();
  }

  @Test
  public void shouldRejectInvalidDefaultEnumValues() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("sampleEnum", "NO_SUCH_ENUM");
    data.put("sampleEnumArray[0]", "NO_SUCH_ENUM");
    data.put("sampleEnumList[0]", "NO_SUCH_ENUM");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "sampleEnum");
    assertInvalidError(form, "sampleEnumArray[0]");
    assertInvalidError(form, "sampleEnumList[0]");
  }

  @Test
  public void shouldRejectInvalidDefaultEnumValuesWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("sampleEnum", "NO_SUCH_ENUM");
    data.put("sampleEnumArray[0]", "NO_SUCH_ENUM");
    data.put("sampleEnumList[0]", "NO_SUCH_ENUM");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "sampleEnum");
    assertInvalidError(form, "sampleEnumArray[0]");
    assertInvalidError(form, "sampleEnumList[0]");
  }

  @Test
  public void shouldHandleEmptyDefaultScalarValues() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("byteArray", "");
    data.put("charArray", "");
    data.put("charset", "");
    data.put("locale", "");
    data.put("pattern", "");
    data.put("uri", "");
    data.put("url", "");
    data.put("uuid", "");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.getByteArray()).isEmpty();
    assertThat(bean.getCharArray()).isEmpty();
    assertThat(bean.getCharset()).isNull();
    assertThat(bean.getLocale()).isNull();
    assertThat(bean.getPattern().pattern()).isEmpty();
    assertThat(bean.getUri()).isNull();
    assertThat(bean.getUrl()).isNull();
    assertThat(bean.getUuid()).isNull();
  }

  @Test
  public void shouldHandleEmptyDefaultScalarValuesWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("byteArray", "");
    data.put("charArray", "");
    data.put("charset", "");
    data.put("locale", "");
    data.put("pattern", "");
    data.put("uri", "");
    data.put("url", "");
    data.put("uuid", "");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.byteArray).isEmpty();
    assertThat(bean.charArray).isEmpty();
    assertThat(bean.charset).isNull();
    assertThat(bean.locale).isNull();
    assertThat(bean.pattern.pattern()).isEmpty();
    assertThat(bean.uri).isNull();
    assertThat(bean.url).isNull();
    assertThat(bean.uuid).isNull();
  }

  @Test
  public void shouldHandleWhitespaceOnlyDefaultScalarValues() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("byteArray", "   ");
    data.put("charArray", "   ");
    data.put("charset", "   ");
    data.put("locale", "   ");
    data.put("pattern", "   ");
    data.put("uri", "   ");
    data.put("url", "   ");
    data.put("uuid", "   ");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(new String(bean.getByteArray())).isEqualTo("   ");
    assertThat(bean.getCharArray()).containsExactly(' ', ' ', ' ');
    assertThat(bean.getCharset()).isNull();
    assertThat(bean.getLocale().toString()).isEmpty();
    assertThat(bean.getLocale().getLanguage()).isEmpty();
    assertThat(bean.getLocale().getCountry()).isEmpty();
    assertThat(bean.getLocale().getVariant()).isEqualTo(" ");
    assertThat(bean.getPattern().pattern()).isEqualTo("   ");
    assertThat(bean.getUri()).isNull();
    assertThat(bean.getUrl()).isNull();
    assertThat(bean.getUuid()).isNull();
  }

  @Test
  public void shouldHandleWhitespaceOnlyDefaultScalarValuesWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("byteArray", "   ");
    data.put("charArray", "   ");
    data.put("charset", "   ");
    data.put("locale", "   ");
    data.put("pattern", "   ");
    data.put("uri", "   ");
    data.put("url", "   ");
    data.put("uuid", "   ");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(new String(bean.byteArray)).isEqualTo("   ");
    assertThat(bean.charArray).containsExactly(' ', ' ', ' ');
    assertThat(bean.charset).isNull();
    assertThat(bean.locale.toString()).isEmpty();
    assertThat(bean.locale.getLanguage()).isEmpty();
    assertThat(bean.locale.getCountry()).isEmpty();
    assertThat(bean.locale.getVariant()).isEqualTo(" ");
    assertThat(bean.pattern.pattern()).isEqualTo("   ");
    assertThat(bean.uri).isNull();
    assertThat(bean.url).isNull();
    assertThat(bean.uuid).isNull();
  }

  @Test
  public void shouldRejectEmptyDefaultScalarValuesWithoutEmptyHandling() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("currency", "");
    data.put("timeZone", "");
    data.put("zoneId", "");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "currency");
    assertInvalidError(form, "timeZone");
    assertInvalidError(form, "zoneId");
  }

  @Test
  public void shouldRejectEmptyDefaultScalarValuesWithoutEmptyHandlingWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("currency", "");
    data.put("timeZone", "");
    data.put("zoneId", "");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "currency");
    assertInvalidError(form, "timeZone");
    assertInvalidError(form, "zoneId");
  }

  @Test
  public void shouldRejectWhitespaceOnlyDefaultScalarValuesWithoutEmptyHandling() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("currency", "   ");
    data.put("timeZone", "   ");
    data.put("zoneId", "   ");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "currency");
    assertInvalidError(form, "timeZone");
    assertInvalidError(form, "zoneId");
  }

  @Test
  public void
      shouldRejectWhitespaceOnlyDefaultScalarValuesWithoutEmptyHandlingWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("currency", "   ");
    data.put("timeZone", "   ");
    data.put("zoneId", "   ");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "currency");
    assertInvalidError(form, "timeZone");
    assertInvalidError(form, "zoneId");
  }

  @Test
  public void shouldRejectInvalidDefaultScalarValues() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("integerValue", "not-a-number");
    data.put("charset", "NO_SUCH_CHARSET");
    data.put("currency", "INVALID");
    data.put("locale", "de@AT");
    data.put("pattern", "[");
    data.put("timeZone", "No/SuchZone");
    data.put("uri", "http://[invalid");
    data.put("uriRelative", "docs/releases with spaces.txt");
    data.put("url", "http://[invalid");
    data.put("urlClasspath", "classpath:play/data/DefaultFormBindingTest.class");
    data.put("urlUnknownProtocol", "unknown-protocol:/missing/resource");
    data.put("uuid", "not-a-uuid");
    data.put("zoneId", "No/SuchZone");
    data.put("dateValue", "not-a-date");
    data.put("staticValueWithWrongFieldType", "WRONG_FIELD_TYPE");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "integerValue");
    assertInvalidError(form, "charset");
    assertInvalidError(form, "currency");
    assertInvalidError(form, "locale");
    assertInvalidError(form, "pattern");
    assertInvalidError(form, "timeZone");
    assertInvalidError(form, "uri");
    assertInvalidError(form, "uriRelative");
    assertInvalidError(form, "url");
    assertInvalidError(form, "urlClasspath");
    assertInvalidError(form, "urlUnknownProtocol");
    assertInvalidError(form, "uuid");
    assertInvalidError(form, "zoneId");
    assertInvalidDateError(form, "dateValue");
    assertInvalidError(form, "staticValueWithWrongFieldType");
  }

  @Test
  public void shouldRejectInvalidDefaultScalarValuesWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("integerValue", "not-a-number");
    data.put("charset", "NO_SUCH_CHARSET");
    data.put("currency", "INVALID");
    data.put("locale", "de@AT");
    data.put("pattern", "[");
    data.put("timeZone", "No/SuchZone");
    data.put("uri", "http://[invalid");
    data.put("uriRelative", "docs/releases with spaces.txt");
    data.put("url", "http://[invalid");
    data.put("urlClasspath", "classpath:play/data/DefaultFormBindingTest.class");
    data.put("urlUnknownProtocol", "unknown-protocol:/missing/resource");
    data.put("uuid", "not-a-uuid");
    data.put("zoneId", "No/SuchZone");
    data.put("dateValue", "not-a-date");
    data.put("staticValueWithWrongFieldType", "WRONG_FIELD_TYPE");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "integerValue");
    assertInvalidError(form, "charset");
    assertInvalidError(form, "currency");
    assertInvalidError(form, "locale");
    assertInvalidError(form, "pattern");
    assertInvalidError(form, "timeZone");
    assertInvalidError(form, "uri");
    assertInvalidError(form, "uriRelative");
    assertInvalidError(form, "url");
    assertInvalidError(form, "urlClasspath");
    assertInvalidError(form, "urlUnknownProtocol");
    assertInvalidError(form, "uuid");
    assertInvalidError(form, "zoneId");
    assertInvalidDateError(form, "dateValue");
    assertInvalidError(form, "staticValueWithWrongFieldType");
  }

  @Test
  public void shouldBindSimpleArrayValuesFromRequestData() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("stringArray[]", new String[] {"red", "green"});
    data.put("characterArray[]", new String[] {"a", "b"});
    data.put("booleanArray[]", new String[] {"true", "off"});
    data.put("byteArrayElements[]", new String[] {"9", "10"});
    data.put("primitiveByteArrayElements[]", new String[] {"11", "12"});
    data.put("shortArray[]", new String[] {"13", "14"});
    data.put("primitiveShortArray[]", new String[] {"15", "16"});
    data.put("integerArray[]", new String[] {"17", "18"});
    data.put("primitiveIntArray[]", new String[] {"19", "20"});
    data.put("longArray[]", new String[] {"21", "22"});
    data.put("primitiveLongArray[]", new String[] {"23", "24"});
    data.put("floatArray[]", new String[] {"25.25", "26.25"});
    data.put("primitiveFloatArray[]", new String[] {"27.25", "28.25"});
    data.put("doubleArray[]", new String[] {"29.25", "30.25"});
    data.put("primitiveDoubleArray[]", new String[] {"31.25", "32.25"});
    data.put("bigDecimalArray[]", new String[] {"33.33", "34.34"});
    data.put("bigIntegerArray[]", new String[] {"351", "352"});
    data.put("charsetArray[]", new String[] {"UTF-8", "ISO-8859-1"});
    data.put("currencyArray[]", new String[] {"EUR", "USD"});
    data.put("localeArray[]", new String[] {"de_AT", "en_US"});
    data.put("patternArray[]", new String[] {"a+", "b+"});
    data.put("timeZoneArray[]", new String[] {"Europe/Vienna", "UTC"});
    data.put(
        "uriArray[]", new String[] {"https://example.com/some path", "urn:isbn:9780140328721"});
    data.put("urlArray[]", new String[] {"https://example.com/one", "https://example.com/two"});
    data.put(
        "uuidArray[]",
        new String[] {
          "5a68095f-3f88-4c82-9b2f-345ef6017d92", "954279b9-1bc0-4be5-8ca9-f30d27316564"
        });
    data.put("zoneIdArray[]", new String[] {"Europe/Vienna", "UTC"});

    Form<FormData> form = bindRequestData(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertRequestArrayValues(bean);
  }

  @Test
  public void shouldBindSimpleArrayValuesFromRequestDataWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("stringArray[]", new String[] {"red", "green"});
    data.put("characterArray[]", new String[] {"a", "b"});
    data.put("booleanArray[]", new String[] {"true", "off"});
    data.put("byteArrayElements[]", new String[] {"9", "10"});
    data.put("primitiveByteArrayElements[]", new String[] {"11", "12"});
    data.put("shortArray[]", new String[] {"13", "14"});
    data.put("primitiveShortArray[]", new String[] {"15", "16"});
    data.put("integerArray[]", new String[] {"17", "18"});
    data.put("primitiveIntArray[]", new String[] {"19", "20"});
    data.put("longArray[]", new String[] {"21", "22"});
    data.put("primitiveLongArray[]", new String[] {"23", "24"});
    data.put("floatArray[]", new String[] {"25.25", "26.25"});
    data.put("primitiveFloatArray[]", new String[] {"27.25", "28.25"});
    data.put("doubleArray[]", new String[] {"29.25", "30.25"});
    data.put("primitiveDoubleArray[]", new String[] {"31.25", "32.25"});
    data.put("bigDecimalArray[]", new String[] {"33.33", "34.34"});
    data.put("bigIntegerArray[]", new String[] {"351", "352"});
    data.put("charsetArray[]", new String[] {"UTF-8", "ISO-8859-1"});
    data.put("currencyArray[]", new String[] {"EUR", "USD"});
    data.put("localeArray[]", new String[] {"de_AT", "en_US"});
    data.put("patternArray[]", new String[] {"a+", "b+"});
    data.put("timeZoneArray[]", new String[] {"Europe/Vienna", "UTC"});
    data.put(
        "uriArray[]", new String[] {"https://example.com/some path", "urn:isbn:9780140328721"});
    data.put("urlArray[]", new String[] {"https://example.com/one", "https://example.com/two"});
    data.put(
        "uuidArray[]",
        new String[] {
          "5a68095f-3f88-4c82-9b2f-345ef6017d92", "954279b9-1bc0-4be5-8ca9-f30d27316564"
        });
    data.put("zoneIdArray[]", new String[] {"Europe/Vienna", "UTC"});

    Form<FormData> form = bindRequestDataDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertRequestArrayValues(bean);
  }

  @Test
  public void shouldRejectInvalidArrayValuesFromRequestData() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("integerArray[]", new String[] {"not-a-number"});
    data.put("booleanArray[]", new String[] {"maybe"});
    data.put("characterArray[]", new String[] {"ab"});
    data.put("charsetArray[]", new String[] {"NO_SUCH_CHARSET"});
    data.put("currencyArray[]", new String[] {"INVALID"});
    data.put("localeArray[]", new String[] {"de@AT"});
    data.put("patternArray[]", new String[] {"["});
    data.put("timeZoneArray[]", new String[] {"No/SuchZone"});
    data.put("uriArray[]", new String[] {"http://[invalid"});
    data.put("urlArray[]", new String[] {"http://[invalid"});
    data.put("uuidArray[]", new String[] {"not-a-uuid"});
    data.put("zoneIdArray[]", new String[] {"No/SuchZone"});

    Form<FormData> form = bindRequestData(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "integerArray[0]");
    assertInvalidError(form, "booleanArray[0]");
    assertInvalidError(form, "characterArray[0]");
    assertInvalidError(form, "charsetArray[0]");
    assertInvalidError(form, "currencyArray[0]");
    assertInvalidError(form, "localeArray[0]");
    assertInvalidError(form, "patternArray[0]");
    assertInvalidError(form, "timeZoneArray[0]");
    assertInvalidError(form, "uriArray[0]");
    assertInvalidError(form, "urlArray[0]");
    assertInvalidError(form, "uuidArray[0]");
    assertInvalidError(form, "zoneIdArray[0]");
  }

  @Test
  public void shouldRejectInvalidArrayValuesFromRequestDataWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("integerArray[]", new String[] {"not-a-number"});
    data.put("booleanArray[]", new String[] {"maybe"});
    data.put("characterArray[]", new String[] {"ab"});
    data.put("charsetArray[]", new String[] {"NO_SUCH_CHARSET"});
    data.put("currencyArray[]", new String[] {"INVALID"});
    data.put("localeArray[]", new String[] {"de@AT"});
    data.put("patternArray[]", new String[] {"["});
    data.put("timeZoneArray[]", new String[] {"No/SuchZone"});
    data.put("uriArray[]", new String[] {"http://[invalid"});
    data.put("urlArray[]", new String[] {"http://[invalid"});
    data.put("uuidArray[]", new String[] {"not-a-uuid"});
    data.put("zoneIdArray[]", new String[] {"No/SuchZone"});

    Form<FormData> form = bindRequestDataDirect(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "integerArray[0]");
    assertInvalidError(form, "booleanArray[0]");
    assertInvalidError(form, "characterArray[0]");
    assertInvalidError(form, "charsetArray[0]");
    assertInvalidError(form, "currencyArray[0]");
    assertInvalidError(form, "localeArray[0]");
    assertInvalidError(form, "patternArray[0]");
    assertInvalidError(form, "timeZoneArray[0]");
    assertInvalidError(form, "uriArray[0]");
    assertInvalidError(form, "urlArray[0]");
    assertInvalidError(form, "uuidArray[0]");
    assertInvalidError(form, "zoneIdArray[0]");
  }

  @Test
  public void shouldRejectInvalidArrayValuesFromIndexedData() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("integerArray[0]", "not-a-number");
    data.put("booleanArray[0]", "maybe");
    data.put("characterArray[0]", "ab");
    data.put("charsetArray[0]", "NO_SUCH_CHARSET");
    data.put("currencyArray[0]", "INVALID");
    data.put("localeArray[0]", "de@AT");
    data.put("patternArray[0]", "[");
    data.put("timeZoneArray[0]", "No/SuchZone");
    data.put("uriArray[0]", "http://[invalid");
    data.put("urlArray[0]", "http://[invalid");
    data.put("uuidArray[0]", "not-a-uuid");
    data.put("zoneIdArray[0]", "No/SuchZone");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "integerArray[0]");
    assertInvalidError(form, "booleanArray[0]");
    assertInvalidError(form, "characterArray[0]");
    assertInvalidError(form, "charsetArray[0]");
    assertInvalidError(form, "currencyArray[0]");
    assertInvalidError(form, "localeArray[0]");
    assertInvalidError(form, "patternArray[0]");
    assertInvalidError(form, "timeZoneArray[0]");
    assertInvalidError(form, "uriArray[0]");
    assertInvalidError(form, "urlArray[0]");
    assertInvalidError(form, "uuidArray[0]");
    assertInvalidError(form, "zoneIdArray[0]");
  }

  @Test
  public void shouldRejectInvalidArrayValuesFromIndexedDataWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("integerArray[0]", "not-a-number");
    data.put("booleanArray[0]", "maybe");
    data.put("characterArray[0]", "ab");
    data.put("charsetArray[0]", "NO_SUCH_CHARSET");
    data.put("currencyArray[0]", "INVALID");
    data.put("localeArray[0]", "de@AT");
    data.put("patternArray[0]", "[");
    data.put("timeZoneArray[0]", "No/SuchZone");
    data.put("uriArray[0]", "http://[invalid");
    data.put("urlArray[0]", "http://[invalid");
    data.put("uuidArray[0]", "not-a-uuid");
    data.put("zoneIdArray[0]", "No/SuchZone");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "integerArray[0]");
    assertInvalidError(form, "booleanArray[0]");
    assertInvalidError(form, "characterArray[0]");
    assertInvalidError(form, "charsetArray[0]");
    assertInvalidError(form, "currencyArray[0]");
    assertInvalidError(form, "localeArray[0]");
    assertInvalidError(form, "patternArray[0]");
    assertInvalidError(form, "timeZoneArray[0]");
    assertInvalidError(form, "uriArray[0]");
    assertInvalidError(form, "urlArray[0]");
    assertInvalidError(form, "uuidArray[0]");
    assertInvalidError(form, "zoneIdArray[0]");
  }

  @Test
  public void shouldBindEmptyArrayValuesFromRequestData() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("stringArray[]", new String[] {""});
    data.put("characterArray[]", new String[] {""});
    data.put("booleanArray[]", new String[] {""});
    data.put("byteArrayElements[]", new String[] {""});
    data.put("shortArray[]", new String[] {""});
    data.put("integerArray[]", new String[] {""});
    data.put("longArray[]", new String[] {""});
    data.put("floatArray[]", new String[] {""});
    data.put("doubleArray[]", new String[] {""});
    data.put("bigDecimalArray[]", new String[] {""});
    data.put("bigIntegerArray[]", new String[] {""});
    data.put("charsetArray[]", new String[] {""});
    data.put("localeArray[]", new String[] {""});
    data.put("patternArray[]", new String[] {""});
    data.put("uriArray[]", new String[] {""});
    data.put("urlArray[]", new String[] {""});
    data.put("uuidArray[]", new String[] {""});

    Form<FormData> form = bindRequestData(formFactory, data);

    assertThat(form.errors()).isEmpty();
    assertEmptyArrayValues(form.get());
  }

  @Test
  public void shouldBindEmptyArrayValuesFromRequestDataWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("stringArray[]", new String[] {""});
    data.put("characterArray[]", new String[] {""});
    data.put("booleanArray[]", new String[] {""});
    data.put("byteArrayElements[]", new String[] {""});
    data.put("shortArray[]", new String[] {""});
    data.put("integerArray[]", new String[] {""});
    data.put("longArray[]", new String[] {""});
    data.put("floatArray[]", new String[] {""});
    data.put("doubleArray[]", new String[] {""});
    data.put("bigDecimalArray[]", new String[] {""});
    data.put("bigIntegerArray[]", new String[] {""});
    data.put("charsetArray[]", new String[] {""});
    data.put("localeArray[]", new String[] {""});
    data.put("patternArray[]", new String[] {""});
    data.put("uriArray[]", new String[] {""});
    data.put("urlArray[]", new String[] {""});
    data.put("uuidArray[]", new String[] {""});

    Form<FormData> form = bindRequestDataDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    assertEmptyArrayValues(form.get());
  }

  @Test
  public void shouldBindEmptyArrayValuesFromIndexedData() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringArray[0]", "");
    data.put("characterArray[0]", "");
    data.put("booleanArray[0]", "");
    data.put("byteArrayElements[0]", "");
    data.put("shortArray[0]", "");
    data.put("integerArray[0]", "");
    data.put("longArray[0]", "");
    data.put("floatArray[0]", "");
    data.put("doubleArray[0]", "");
    data.put("bigDecimalArray[0]", "");
    data.put("bigIntegerArray[0]", "");
    data.put("charsetArray[0]", "");
    data.put("localeArray[0]", "");
    data.put("patternArray[0]", "");
    data.put("uriArray[0]", "");
    data.put("urlArray[0]", "");
    data.put("uuidArray[0]", "");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    assertEmptyArrayValues(form.get());
  }

  @Test
  public void shouldBindEmptyArrayValuesFromIndexedDataWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringArray[0]", "");
    data.put("characterArray[0]", "");
    data.put("booleanArray[0]", "");
    data.put("byteArrayElements[0]", "");
    data.put("shortArray[0]", "");
    data.put("integerArray[0]", "");
    data.put("longArray[0]", "");
    data.put("floatArray[0]", "");
    data.put("doubleArray[0]", "");
    data.put("bigDecimalArray[0]", "");
    data.put("bigIntegerArray[0]", "");
    data.put("charsetArray[0]", "");
    data.put("localeArray[0]", "");
    data.put("patternArray[0]", "");
    data.put("uriArray[0]", "");
    data.put("urlArray[0]", "");
    data.put("uuidArray[0]", "");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    assertEmptyArrayValues(form.get());
  }

  @Test
  public void shouldBindWhitespaceOnlyArrayValuesFromRequestData() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("stringArray[]", new String[] {"   "});
    data.put("characterArray[]", new String[] {" "});
    data.put("booleanArray[]", new String[] {"   "});
    data.put("byteArrayElements[]", new String[] {"   "});
    data.put("shortArray[]", new String[] {"   "});
    data.put("integerArray[]", new String[] {"   "});
    data.put("longArray[]", new String[] {"   "});
    data.put("floatArray[]", new String[] {"   "});
    data.put("doubleArray[]", new String[] {"   "});
    data.put("bigDecimalArray[]", new String[] {"   "});
    data.put("bigIntegerArray[]", new String[] {"   "});
    data.put("charsetArray[]", new String[] {"   "});
    data.put("localeArray[]", new String[] {"   "});
    data.put("patternArray[]", new String[] {"   "});
    data.put("uriArray[]", new String[] {"   "});
    data.put("urlArray[]", new String[] {"   "});
    data.put("uuidArray[]", new String[] {"   "});

    Form<FormData> form = bindRequestData(formFactory, data);

    assertThat(form.errors()).isEmpty();
    assertWhitespaceOnlyArrayValues(form.get());
  }

  @Test
  public void shouldBindWhitespaceOnlyArrayValuesFromRequestDataWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("stringArray[]", new String[] {"   "});
    data.put("characterArray[]", new String[] {" "});
    data.put("booleanArray[]", new String[] {"   "});
    data.put("byteArrayElements[]", new String[] {"   "});
    data.put("shortArray[]", new String[] {"   "});
    data.put("integerArray[]", new String[] {"   "});
    data.put("longArray[]", new String[] {"   "});
    data.put("floatArray[]", new String[] {"   "});
    data.put("doubleArray[]", new String[] {"   "});
    data.put("bigDecimalArray[]", new String[] {"   "});
    data.put("bigIntegerArray[]", new String[] {"   "});
    data.put("charsetArray[]", new String[] {"   "});
    data.put("localeArray[]", new String[] {"   "});
    data.put("patternArray[]", new String[] {"   "});
    data.put("uriArray[]", new String[] {"   "});
    data.put("urlArray[]", new String[] {"   "});
    data.put("uuidArray[]", new String[] {"   "});

    Form<FormData> form = bindRequestDataDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    assertWhitespaceOnlyArrayValues(form.get());
  }

  @Test
  public void shouldBindWhitespaceOnlyArrayValuesFromIndexedData() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringArray[0]", "   ");
    data.put("characterArray[0]", " ");
    data.put("booleanArray[0]", "   ");
    data.put("byteArrayElements[0]", "   ");
    data.put("shortArray[0]", "   ");
    data.put("integerArray[0]", "   ");
    data.put("longArray[0]", "   ");
    data.put("floatArray[0]", "   ");
    data.put("doubleArray[0]", "   ");
    data.put("bigDecimalArray[0]", "   ");
    data.put("bigIntegerArray[0]", "   ");
    data.put("charsetArray[0]", "   ");
    data.put("localeArray[0]", "   ");
    data.put("patternArray[0]", "   ");
    data.put("uriArray[0]", "   ");
    data.put("urlArray[0]", "   ");
    data.put("uuidArray[0]", "   ");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    assertWhitespaceOnlyArrayValues(form.get());
  }

  @Test
  public void shouldBindWhitespaceOnlyArrayValuesFromIndexedDataWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringArray[0]", "   ");
    data.put("characterArray[0]", " ");
    data.put("booleanArray[0]", "   ");
    data.put("byteArrayElements[0]", "   ");
    data.put("shortArray[0]", "   ");
    data.put("integerArray[0]", "   ");
    data.put("longArray[0]", "   ");
    data.put("floatArray[0]", "   ");
    data.put("doubleArray[0]", "   ");
    data.put("bigDecimalArray[0]", "   ");
    data.put("bigIntegerArray[0]", "   ");
    data.put("charsetArray[0]", "   ");
    data.put("localeArray[0]", "   ");
    data.put("patternArray[0]", "   ");
    data.put("uriArray[0]", "   ");
    data.put("urlArray[0]", "   ");
    data.put("uuidArray[0]", "   ");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    assertWhitespaceOnlyArrayValues(form.get());
  }

  @Test
  public void shouldBindNullArrayValuesFromRequestData() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("stringArray[]", new String[] {null});
    data.put("characterArray[]", new String[] {null});
    data.put("booleanArray[]", new String[] {null});
    data.put("byteArrayElements[]", new String[] {null});
    data.put("shortArray[]", new String[] {null});
    data.put("integerArray[]", new String[] {null});
    data.put("longArray[]", new String[] {null});
    data.put("floatArray[]", new String[] {null});
    data.put("doubleArray[]", new String[] {null});
    data.put("bigDecimalArray[]", new String[] {null});
    data.put("bigIntegerArray[]", new String[] {null});
    data.put("charsetArray[]", new String[] {null});
    data.put("currencyArray[]", new String[] {null});
    data.put("localeArray[]", new String[] {null});
    data.put("patternArray[]", new String[] {null});
    data.put("timeZoneArray[]", new String[] {null});
    data.put("uriArray[]", new String[] {null});
    data.put("urlArray[]", new String[] {null});
    data.put("uuidArray[]", new String[] {null});
    data.put("zoneIdArray[]", new String[] {null});

    Form<FormData> form = bindRequestData(formFactory, data);

    assertThat(form.errors()).isEmpty();
    assertNullArrayValues(form.get());
  }

  @Test
  public void shouldBindNullArrayValuesFromRequestDataWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("stringArray[]", new String[] {null});
    data.put("characterArray[]", new String[] {null});
    data.put("booleanArray[]", new String[] {null});
    data.put("byteArrayElements[]", new String[] {null});
    data.put("shortArray[]", new String[] {null});
    data.put("integerArray[]", new String[] {null});
    data.put("longArray[]", new String[] {null});
    data.put("floatArray[]", new String[] {null});
    data.put("doubleArray[]", new String[] {null});
    data.put("bigDecimalArray[]", new String[] {null});
    data.put("bigIntegerArray[]", new String[] {null});
    data.put("charsetArray[]", new String[] {null});
    data.put("currencyArray[]", new String[] {null});
    data.put("localeArray[]", new String[] {null});
    data.put("patternArray[]", new String[] {null});
    data.put("timeZoneArray[]", new String[] {null});
    data.put("uriArray[]", new String[] {null});
    data.put("urlArray[]", new String[] {null});
    data.put("uuidArray[]", new String[] {null});
    data.put("zoneIdArray[]", new String[] {null});

    Form<FormData> form = bindRequestDataDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    assertNullArrayValues(form.get());
  }

  @Test
  public void shouldBindNullArrayValuesFromIndexedData() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringArray[0]", null);
    data.put("characterArray[0]", null);
    data.put("booleanArray[0]", null);
    data.put("byteArrayElements[0]", null);
    data.put("shortArray[0]", null);
    data.put("integerArray[0]", null);
    data.put("longArray[0]", null);
    data.put("floatArray[0]", null);
    data.put("doubleArray[0]", null);
    data.put("bigDecimalArray[0]", null);
    data.put("bigIntegerArray[0]", null);
    data.put("charsetArray[0]", null);
    data.put("currencyArray[0]", null);
    data.put("localeArray[0]", null);
    data.put("patternArray[0]", null);
    data.put("timeZoneArray[0]", null);
    data.put("uriArray[0]", null);
    data.put("urlArray[0]", null);
    data.put("uuidArray[0]", null);
    data.put("zoneIdArray[0]", null);

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    assertNullArrayValues(form.get());
  }

  @Test
  public void shouldBindNullArrayValuesFromIndexedDataWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringArray[0]", null);
    data.put("characterArray[0]", null);
    data.put("booleanArray[0]", null);
    data.put("byteArrayElements[0]", null);
    data.put("shortArray[0]", null);
    data.put("integerArray[0]", null);
    data.put("longArray[0]", null);
    data.put("floatArray[0]", null);
    data.put("doubleArray[0]", null);
    data.put("bigDecimalArray[0]", null);
    data.put("bigIntegerArray[0]", null);
    data.put("charsetArray[0]", null);
    data.put("currencyArray[0]", null);
    data.put("localeArray[0]", null);
    data.put("patternArray[0]", null);
    data.put("timeZoneArray[0]", null);
    data.put("uriArray[0]", null);
    data.put("urlArray[0]", null);
    data.put("uuidArray[0]", null);
    data.put("zoneIdArray[0]", null);

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    assertNullArrayValues(form.get());
  }

  private static void assertEmptyArrayValues(FormData bean) {
    assertThat(bean.getStringArray()).containsExactly("");
    assertThat(bean.getCharacterArray()).containsNull();
    assertThat(bean.getBooleanArray()).containsNull();
    assertThat(bean.getByteArrayElements()).containsNull();
    assertThat(bean.getShortArray()).containsNull();
    assertThat(bean.getIntegerArray()).containsNull();
    assertThat(bean.getLongArray()).containsNull();
    assertThat(bean.getFloatArray()).containsNull();
    assertThat(bean.getDoubleArray()).containsNull();
    assertThat(bean.getBigDecimalArray()).containsNull();
    assertThat(bean.getBigIntegerArray()).containsNull();
    assertThat(bean.getCharsetArray()).containsNull();
    assertThat(bean.getLocaleArray()).containsNull();
    assertThat(bean.getPatternArray()).extracting(Pattern::pattern).containsExactly("");
    assertThat(bean.getUriArray()).containsNull();
    assertThat(bean.getUrlArray()).containsNull();
    assertThat(bean.getUuidArray()).containsNull();
  }

  private static void assertNullArrayValues(FormData bean) {
    assertThat(bean.getStringArray()).containsNull();
    assertThat(bean.getCharacterArray()).containsNull();
    assertThat(bean.getBooleanArray()).containsNull();
    assertThat(bean.getByteArrayElements()).containsNull();
    assertThat(bean.getShortArray()).containsNull();
    assertThat(bean.getIntegerArray()).containsNull();
    assertThat(bean.getLongArray()).containsNull();
    assertThat(bean.getFloatArray()).containsNull();
    assertThat(bean.getDoubleArray()).containsNull();
    assertThat(bean.getBigDecimalArray()).containsNull();
    assertThat(bean.getBigIntegerArray()).containsNull();
    assertThat(bean.getCharsetArray()).containsNull();
    assertThat(bean.getCurrencyArray()).containsNull();
    assertThat(bean.getLocaleArray()).containsNull();
    assertThat(bean.getPatternArray()).containsNull();
    assertThat(bean.getTimeZoneArray()).containsNull();
    assertThat(bean.getUriArray()).containsNull();
    assertThat(bean.getUrlArray()).containsNull();
    assertThat(bean.getUuidArray()).containsNull();
    assertThat(bean.getZoneIdArray()).containsNull();
  }

  private static void assertWhitespaceOnlyArrayValues(FormData bean) {
    assertThat(bean.getStringArray()).containsExactly("   ");
    assertThat(bean.getCharacterArray()).containsExactly(' ');
    assertThat(bean.getBooleanArray()).containsNull();
    assertThat(bean.getByteArrayElements()).containsNull();
    assertThat(bean.getShortArray()).containsNull();
    assertThat(bean.getIntegerArray()).containsNull();
    assertThat(bean.getLongArray()).containsNull();
    assertThat(bean.getFloatArray()).containsNull();
    assertThat(bean.getDoubleArray()).containsNull();
    assertThat(bean.getBigDecimalArray()).containsNull();
    assertThat(bean.getBigIntegerArray()).containsNull();
    assertThat(bean.getCharsetArray()).containsNull();
    assertThat(bean.getLocaleArray()).extracting(Locale::getVariant).containsExactly(" ");
    assertThat(bean.getPatternArray()).extracting(Pattern::pattern).containsExactly("   ");
    assertThat(bean.getUriArray()).containsNull();
    assertThat(bean.getUrlArray()).containsNull();
    assertThat(bean.getUuidArray()).containsNull();
  }

  @Test
  public void shouldRejectEmptyArrayValuesWithoutEmptyHandlingFromRequestData() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("primitiveByteArrayElements[]", new String[] {""});
    data.put("primitiveShortArray[]", new String[] {""});
    data.put("primitiveIntArray[]", new String[] {""});
    data.put("primitiveLongArray[]", new String[] {""});
    data.put("primitiveFloatArray[]", new String[] {""});
    data.put("primitiveDoubleArray[]", new String[] {""});
    data.put("currencyArray[]", new String[] {""});
    data.put("timeZoneArray[]", new String[] {""});
    data.put("zoneIdArray[]", new String[] {""});

    Form<FormData> form = bindRequestData(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "primitiveByteArrayElements[0]");
    assertInvalidError(form, "primitiveShortArray[0]");
    assertInvalidError(form, "primitiveIntArray[0]");
    assertInvalidError(form, "primitiveLongArray[0]");
    assertInvalidError(form, "primitiveFloatArray[0]");
    assertInvalidError(form, "primitiveDoubleArray[0]");
    assertInvalidError(form, "currencyArray[0]");
    assertInvalidError(form, "timeZoneArray[0]");
    assertInvalidError(form, "zoneIdArray[0]");
  }

  @Test
  public void
      shouldRejectEmptyArrayValuesWithoutEmptyHandlingFromRequestDataWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("primitiveByteArrayElements[]", new String[] {""});
    data.put("primitiveShortArray[]", new String[] {""});
    data.put("primitiveIntArray[]", new String[] {""});
    data.put("primitiveLongArray[]", new String[] {""});
    data.put("primitiveFloatArray[]", new String[] {""});
    data.put("primitiveDoubleArray[]", new String[] {""});
    data.put("currencyArray[]", new String[] {""});
    data.put("timeZoneArray[]", new String[] {""});
    data.put("zoneIdArray[]", new String[] {""});

    Form<FormData> form = bindRequestDataDirect(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "primitiveByteArrayElements[0]");
    assertInvalidError(form, "primitiveShortArray[0]");
    assertInvalidError(form, "primitiveIntArray[0]");
    assertInvalidError(form, "primitiveLongArray[0]");
    assertInvalidError(form, "primitiveFloatArray[0]");
    assertInvalidError(form, "primitiveDoubleArray[0]");
    assertInvalidError(form, "currencyArray[0]");
    assertInvalidError(form, "timeZoneArray[0]");
    assertInvalidError(form, "zoneIdArray[0]");
  }

  @Test
  public void shouldRejectEmptyArrayValuesWithoutEmptyHandlingFromIndexedData() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("primitiveByteArrayElements[0]", "");
    data.put("primitiveShortArray[0]", "");
    data.put("primitiveIntArray[0]", "");
    data.put("primitiveLongArray[0]", "");
    data.put("primitiveFloatArray[0]", "");
    data.put("primitiveDoubleArray[0]", "");
    data.put("currencyArray[0]", "");
    data.put("timeZoneArray[0]", "");
    data.put("zoneIdArray[0]", "");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "primitiveByteArrayElements[0]");
    assertInvalidError(form, "primitiveShortArray[0]");
    assertInvalidError(form, "primitiveIntArray[0]");
    assertInvalidError(form, "primitiveLongArray[0]");
    assertInvalidError(form, "primitiveFloatArray[0]");
    assertInvalidError(form, "primitiveDoubleArray[0]");
    assertInvalidError(form, "currencyArray[0]");
    assertInvalidError(form, "timeZoneArray[0]");
    assertInvalidError(form, "zoneIdArray[0]");
  }

  @Test
  public void
      shouldRejectEmptyArrayValuesWithoutEmptyHandlingFromIndexedDataWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("primitiveByteArrayElements[0]", "");
    data.put("primitiveShortArray[0]", "");
    data.put("primitiveIntArray[0]", "");
    data.put("primitiveLongArray[0]", "");
    data.put("primitiveFloatArray[0]", "");
    data.put("primitiveDoubleArray[0]", "");
    data.put("currencyArray[0]", "");
    data.put("timeZoneArray[0]", "");
    data.put("zoneIdArray[0]", "");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "primitiveByteArrayElements[0]");
    assertInvalidError(form, "primitiveShortArray[0]");
    assertInvalidError(form, "primitiveIntArray[0]");
    assertInvalidError(form, "primitiveLongArray[0]");
    assertInvalidError(form, "primitiveFloatArray[0]");
    assertInvalidError(form, "primitiveDoubleArray[0]");
    assertInvalidError(form, "currencyArray[0]");
    assertInvalidError(form, "timeZoneArray[0]");
    assertInvalidError(form, "zoneIdArray[0]");
  }

  @Test
  public void shouldBindSimpleArrayValuesFromIndexedData() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringArray[0]", "red");
    data.put("stringArray[1]", "green");
    data.put("characterArray[0]", "a");
    data.put("characterArray[1]", "b");
    data.put("booleanArray[0]", "true");
    data.put("booleanArray[1]", "off");
    data.put("byteArrayElements[0]", "41");
    data.put("byteArrayElements[1]", "42");
    data.put("primitiveByteArrayElements[0]", "43");
    data.put("primitiveByteArrayElements[1]", "44");
    data.put("shortArray[0]", "45");
    data.put("shortArray[1]", "46");
    data.put("primitiveShortArray[0]", "47");
    data.put("primitiveShortArray[1]", "48");
    data.put("integerArray[0]", "49");
    data.put("integerArray[1]", "50");
    data.put("primitiveIntArray[0]", "51");
    data.put("primitiveIntArray[1]", "52");
    data.put("longArray[0]", "53");
    data.put("longArray[1]", "54");
    data.put("primitiveLongArray[0]", "55");
    data.put("primitiveLongArray[1]", "56");
    data.put("floatArray[0]", "57.25");
    data.put("floatArray[1]", "58.25");
    data.put("primitiveFloatArray[0]", "59.25");
    data.put("primitiveFloatArray[1]", "60.25");
    data.put("doubleArray[0]", "61.25");
    data.put("doubleArray[1]", "62.25");
    data.put("primitiveDoubleArray[0]", "63.25");
    data.put("primitiveDoubleArray[1]", "64.25");
    data.put("bigDecimalArray[0]", "65.65");
    data.put("bigDecimalArray[1]", "66.66");
    data.put("bigIntegerArray[0]", "671");
    data.put("bigIntegerArray[1]", "672");
    data.put("charsetArray[0]", "UTF-8");
    data.put("charsetArray[1]", "ISO-8859-1");
    data.put("currencyArray[0]", "EUR");
    data.put("currencyArray[1]", "USD");
    data.put("localeArray[0]", "de_AT");
    data.put("localeArray[1]", "en_US");
    data.put("patternArray[0]", "a+");
    data.put("patternArray[1]", "b+");
    data.put("timeZoneArray[0]", "Europe/Vienna");
    data.put("timeZoneArray[1]", "UTC");
    data.put("uriArray[0]", "https://example.com/some path");
    data.put("uriArray[1]", "urn:isbn:9780140328721");
    data.put("urlArray[0]", "https://example.com/one");
    data.put("urlArray[1]", "https://example.com/two");
    data.put("uuidArray[0]", "b8c1244a-066a-4099-bfa1-95fc31475849");
    data.put("uuidArray[1]", "b22c3fa2-e35f-4761-be4f-5a9b7971d20d");
    data.put("zoneIdArray[0]", "Europe/Vienna");
    data.put("zoneIdArray[1]", "UTC");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertIndexedArrayValues(bean);
  }

  @Test
  public void shouldBindSimpleArrayValuesFromIndexedDataWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringArray[0]", "red");
    data.put("stringArray[1]", "green");
    data.put("characterArray[0]", "a");
    data.put("characterArray[1]", "b");
    data.put("booleanArray[0]", "true");
    data.put("booleanArray[1]", "off");
    data.put("byteArrayElements[0]", "41");
    data.put("byteArrayElements[1]", "42");
    data.put("primitiveByteArrayElements[0]", "43");
    data.put("primitiveByteArrayElements[1]", "44");
    data.put("shortArray[0]", "45");
    data.put("shortArray[1]", "46");
    data.put("primitiveShortArray[0]", "47");
    data.put("primitiveShortArray[1]", "48");
    data.put("integerArray[0]", "49");
    data.put("integerArray[1]", "50");
    data.put("primitiveIntArray[0]", "51");
    data.put("primitiveIntArray[1]", "52");
    data.put("longArray[0]", "53");
    data.put("longArray[1]", "54");
    data.put("primitiveLongArray[0]", "55");
    data.put("primitiveLongArray[1]", "56");
    data.put("floatArray[0]", "57.25");
    data.put("floatArray[1]", "58.25");
    data.put("primitiveFloatArray[0]", "59.25");
    data.put("primitiveFloatArray[1]", "60.25");
    data.put("doubleArray[0]", "61.25");
    data.put("doubleArray[1]", "62.25");
    data.put("primitiveDoubleArray[0]", "63.25");
    data.put("primitiveDoubleArray[1]", "64.25");
    data.put("bigDecimalArray[0]", "65.65");
    data.put("bigDecimalArray[1]", "66.66");
    data.put("bigIntegerArray[0]", "671");
    data.put("bigIntegerArray[1]", "672");
    data.put("charsetArray[0]", "UTF-8");
    data.put("charsetArray[1]", "ISO-8859-1");
    data.put("currencyArray[0]", "EUR");
    data.put("currencyArray[1]", "USD");
    data.put("localeArray[0]", "de_AT");
    data.put("localeArray[1]", "en_US");
    data.put("patternArray[0]", "a+");
    data.put("patternArray[1]", "b+");
    data.put("timeZoneArray[0]", "Europe/Vienna");
    data.put("timeZoneArray[1]", "UTC");
    data.put("uriArray[0]", "https://example.com/some path");
    data.put("uriArray[1]", "urn:isbn:9780140328721");
    data.put("urlArray[0]", "https://example.com/one");
    data.put("urlArray[1]", "https://example.com/two");
    data.put("uuidArray[0]", "b8c1244a-066a-4099-bfa1-95fc31475849");
    data.put("uuidArray[1]", "b22c3fa2-e35f-4761-be4f-5a9b7971d20d");
    data.put("zoneIdArray[0]", "Europe/Vienna");
    data.put("zoneIdArray[1]", "UTC");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertIndexedArrayValues(bean);
  }

  @Test
  public void shouldBindMultipleRequestValuesToStringValue() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("stringValue", new String[] {"alpha", "beta"});

    Form<FormData> form = bindRequestData(formFactory, data);

    assertThat(form.errors()).isEmpty();
    assertThat(form.get().getStringValue()).isEqualTo("alpha");
  }

  @Test
  public void shouldBindMultipleRequestValuesToStringValueWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("stringValue", new String[] {"alpha", "beta"});

    Form<FormData> form = bindRequestDataDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    assertThat(form.get().stringValue).isEqualTo("alpha");
  }

  @Test
  public void shouldBindFirstRequestValueToByteArrayAndCharArray() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("byteArray", new String[] {"bytes-one", "bytes-two"});
    data.put("charArray", new String[] {"chars-one", "chars-two"});

    Form<FormData> form = bindRequestData(formFactory, data);

    assertThat(form.errors()).isEmpty();
    assertThat(new String(form.get().getByteArray())).isEqualTo("bytes-one");
    assertThat(form.get().getCharArray())
        .containsExactly('c', 'h', 'a', 'r', 's', '-', 'o', 'n', 'e');
  }

  @Test
  public void shouldBindFirstRequestValueToByteArrayAndCharArrayWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("byteArray", new String[] {"bytes-one", "bytes-two"});
    data.put("charArray", new String[] {"chars-one", "chars-two"});

    Form<FormData> form = bindRequestDataDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    assertThat(new String(form.get().byteArray)).isEqualTo("bytes-one");
    assertThat(form.get().charArray).containsExactly('c', 'h', 'a', 'r', 's', '-', 'o', 'n', 'e');
  }

  @Test
  public void shouldBindFirstRequestValueToCollectionTypes() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("stringCollection", new String[] {"red", "green"});
    data.put("integerList", new String[] {"81", "82"});
    data.put("stringSet", new String[] {"blue", "yellow"});
    data.put("sortedStringSet", new String[] {"orange", "purple"});

    Form<FormData> form = bindRequestData(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.getStringCollection()).containsExactly("red");
    assertThat(bean.getIntegerList()).containsExactly(81);
    assertThat(bean.getStringSet()).containsExactly("blue");
    assertThat(bean.getSortedStringSet()).containsExactly("orange");
  }

  @Test
  public void shouldBindFirstRequestValueToCollectionTypesWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("stringCollection", new String[] {"red", "green"});
    data.put("integerList", new String[] {"81", "82"});
    data.put("stringSet", new String[] {"blue", "yellow"});
    data.put("sortedStringSet", new String[] {"orange", "purple"});

    Form<FormData> form = bindRequestDataDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.stringCollection).containsExactly("red");
    assertThat(bean.integerList).containsExactly(81);
    assertThat(bean.stringSet).containsExactly("blue");
    assertThat(bean.sortedStringSet).containsExactly("orange");
  }

  @Test
  public void shouldRejectSingleRequestValueForMapTypes() {
    FormFactory formFactory = instanceOf(FormFactory.class);

    Form<FormData> form =
        bindRequestData(formFactory, Map.of("stringIntegerMap", new String[] {"not-a-map"}));

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "stringIntegerMap");
  }

  @Test
  public void shouldRejectSingleRequestValueForMapTypesWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);

    Form<FormData> form =
        bindRequestDataDirect(formFactory, Map.of("stringIntegerMap", new String[] {"not-a-map"}));

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "stringIntegerMap");
  }

  @Test
  public void shouldRejectEmptyAndWhitespaceSingleValuesForMapTypes() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringIntegerMap", "");
    data.put("sortedStringIntegerMap", "   ");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "stringIntegerMap");
    assertInvalidError(form, "sortedStringIntegerMap");
  }

  @Test
  public void shouldRejectEmptyAndWhitespaceSingleValuesForMapTypesWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringIntegerMap", "");
    data.put("sortedStringIntegerMap", "   ");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "stringIntegerMap");
    assertInvalidError(form, "sortedStringIntegerMap");
  }

  @Test
  public void shouldKeepNullCollectionAndMapValuesAsNull() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringCollection", null);
    data.put("integerList", null);
    data.put("stringSet", null);
    data.put("sortedStringSet", null);
    data.put("stringIntegerMap", null);
    data.put("sortedStringIntegerMap", null);

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.getStringCollection()).isNull();
    assertThat(bean.getIntegerList()).isNull();
    assertThat(bean.getStringSet()).isNull();
    assertThat(bean.getSortedStringSet()).isNull();
    assertThat(bean.getStringIntegerMap()).isNull();
    assertThat(bean.getSortedStringIntegerMap()).isNull();
  }

  @Test
  public void shouldKeepNullCollectionAndMapValuesAsNullWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringCollection", null);
    data.put("integerList", null);
    data.put("stringSet", null);
    data.put("sortedStringSet", null);
    data.put("stringIntegerMap", null);
    data.put("sortedStringIntegerMap", null);

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.stringCollection).isNull();
    assertThat(bean.integerList).isNull();
    assertThat(bean.stringSet).isNull();
    assertThat(bean.sortedStringSet).isNull();
    assertThat(bean.stringIntegerMap).isNull();
    assertThat(bean.sortedStringIntegerMap).isNull();
  }

  private static void assertRequestArrayValues(FormData bean) {
    assertCommonArrayValues(bean);
    assertThat(bean.getByteArrayElements()).containsExactly((byte) 9, (byte) 10);
    assertThat(bean.getPrimitiveByteArrayElements()).containsExactly((byte) 11, (byte) 12);
    assertThat(bean.getShortArray()).containsExactly((short) 13, (short) 14);
    assertThat(bean.getPrimitiveShortArray()).containsExactly((short) 15, (short) 16);
    assertThat(bean.getIntegerArray()).containsExactly(17, 18);
    assertThat(bean.getPrimitiveIntArray()).containsExactly(19, 20);
    assertThat(bean.getLongArray()).containsExactly(21L, 22L);
    assertThat(bean.getPrimitiveLongArray()).containsExactly(23L, 24L);
    assertThat(bean.getFloatArray()).containsExactly(25.25f, 26.25f);
    assertThat(bean.getPrimitiveFloatArray()).containsExactly(27.25f, 28.25f);
    assertThat(bean.getDoubleArray()).containsExactly(29.25d, 30.25d);
    assertThat(bean.getPrimitiveDoubleArray()).containsExactly(31.25d, 32.25d);
    assertThat(bean.getBigDecimalArray())
        .containsExactly(new BigDecimal("33.33"), new BigDecimal("34.34"));
    assertThat(bean.getBigIntegerArray())
        .containsExactly(new BigInteger("351"), new BigInteger("352"));
    assertThat(bean.getUuidArray())
        .containsExactly(
            UUID.fromString("5a68095f-3f88-4c82-9b2f-345ef6017d92"),
            UUID.fromString("954279b9-1bc0-4be5-8ca9-f30d27316564"));
  }

  private static void assertIndexedArrayValues(FormData bean) {
    assertCommonArrayValues(bean);
    assertThat(bean.getByteArrayElements()).containsExactly((byte) 41, (byte) 42);
    assertThat(bean.getPrimitiveByteArrayElements()).containsExactly((byte) 43, (byte) 44);
    assertThat(bean.getShortArray()).containsExactly((short) 45, (short) 46);
    assertThat(bean.getPrimitiveShortArray()).containsExactly((short) 47, (short) 48);
    assertThat(bean.getIntegerArray()).containsExactly(49, 50);
    assertThat(bean.getPrimitiveIntArray()).containsExactly(51, 52);
    assertThat(bean.getLongArray()).containsExactly(53L, 54L);
    assertThat(bean.getPrimitiveLongArray()).containsExactly(55L, 56L);
    assertThat(bean.getFloatArray()).containsExactly(57.25f, 58.25f);
    assertThat(bean.getPrimitiveFloatArray()).containsExactly(59.25f, 60.25f);
    assertThat(bean.getDoubleArray()).containsExactly(61.25d, 62.25d);
    assertThat(bean.getPrimitiveDoubleArray()).containsExactly(63.25d, 64.25d);
    assertThat(bean.getBigDecimalArray())
        .containsExactly(new BigDecimal("65.65"), new BigDecimal("66.66"));
    assertThat(bean.getBigIntegerArray())
        .containsExactly(new BigInteger("671"), new BigInteger("672"));
    assertThat(bean.getUuidArray())
        .containsExactly(
            UUID.fromString("b8c1244a-066a-4099-bfa1-95fc31475849"),
            UUID.fromString("b22c3fa2-e35f-4761-be4f-5a9b7971d20d"));
  }

  private static void assertCommonArrayValues(FormData bean) {
    assertThat(bean.getStringArray()).containsExactly("red", "green");
    assertThat(bean.getCharacterArray()).containsExactly('a', 'b');
    assertThat(bean.getBooleanArray()).containsExactly(true, false);
    assertThat(bean.getCharsetArray())
        .containsExactly(StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1);
    assertThat(bean.getCurrencyArray())
        .containsExactly(Currency.getInstance("EUR"), Currency.getInstance("USD"));
    assertThat(bean.getLocaleArray()).containsExactly(new Locale("de", "AT"), Locale.US);
    assertThat(bean.getPatternArray()).extracting(Pattern::pattern).containsExactly("a+", "b+");
    assertThat(bean.getTimeZoneArray())
        .containsExactly(TimeZone.getTimeZone("Europe/Vienna"), TimeZone.getTimeZone("UTC"));
    assertThat(bean.getUriArray())
        .containsExactly(
            URI.create("https://example.com/some%20path"), URI.create("urn:isbn:9780140328721"));
    assertThat(bean.getUrlArray())
        .extracting(URL::toExternalForm)
        .containsExactly("https://example.com/one", "https://example.com/two");
    assertThat(bean.getZoneIdArray()).containsExactly(ZoneId.of("Europe/Vienna"), ZoneId.of("UTC"));
  }

  @Test
  public void shouldBindSingleValuesToCollectionTypes() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringCollection", "red, green");
    data.put("integerList", "71");
    data.put("stringSet", "green");
    data.put("sortedStringSet", "blue");
    data.put("rawCollection", "raw-teal");
    data.put("uuidCollection", "73eef03b-86e0-456f-b0eb-78a5541aaee2");
    data.put("uuidList", "18b396f2-066f-4c26-9629-2c5d963040ef");
    data.put("uuidSet", "a92afee8-60d0-4e74-82ab-9ea43c849160");
    data.put("sortedUuidSet", "94c1c8f5-8c71-4912-b513-195c71cfb453");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.getStringCollection()).containsExactly("red, green");
    assertThat(bean.getIntegerList()).containsExactly(71);
    assertThat(bean.getStringSet()).containsExactly("green");
    assertThat(bean.getSortedStringSet()).containsExactly("blue");
    assertThat(bean.getRawCollection()).containsExactly("raw-teal");
    assertThat(bean.getUuidCollection())
        .containsExactly(UUID.fromString("73eef03b-86e0-456f-b0eb-78a5541aaee2"));
    assertThat(bean.getUuidList())
        .containsExactly(UUID.fromString("18b396f2-066f-4c26-9629-2c5d963040ef"));
    assertThat(bean.getUuidSet())
        .containsExactly(UUID.fromString("a92afee8-60d0-4e74-82ab-9ea43c849160"));
    assertThat(bean.getSortedUuidSet())
        .containsExactly(UUID.fromString("94c1c8f5-8c71-4912-b513-195c71cfb453"));
  }

  @Test
  public void shouldBindSingleValuesToCollectionTypesWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringCollection", "red, green");
    data.put("integerList", "71");
    data.put("stringSet", "green");
    data.put("sortedStringSet", "blue");
    data.put("rawCollection", "raw-cyan");
    data.put("uuidCollection", "73eef03b-86e0-456f-b0eb-78a5541aaee2");
    data.put("uuidList", "18b396f2-066f-4c26-9629-2c5d963040ef");
    data.put("uuidSet", "a92afee8-60d0-4e74-82ab-9ea43c849160");
    data.put("sortedUuidSet", "94c1c8f5-8c71-4912-b513-195c71cfb453");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.stringCollection).containsExactly("red, green");
    assertThat(bean.integerList).containsExactly(71);
    assertThat(bean.stringSet).containsExactly("green");
    assertThat(bean.sortedStringSet).containsExactly("blue");
    assertThat(bean.rawCollection).containsExactly("raw-cyan");
    assertThat(bean.uuidCollection)
        .containsExactly(UUID.fromString("73eef03b-86e0-456f-b0eb-78a5541aaee2"));
    assertThat(bean.uuidList)
        .containsExactly(UUID.fromString("18b396f2-066f-4c26-9629-2c5d963040ef"));
    assertThat(bean.uuidSet)
        .containsExactly(UUID.fromString("a92afee8-60d0-4e74-82ab-9ea43c849160"));
    assertThat(bean.sortedUuidSet)
        .containsExactly(UUID.fromString("94c1c8f5-8c71-4912-b513-195c71cfb453"));
  }

  @Test
  public void shouldBindEmptySingleValuesToCollectionTypes() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringCollection", "");
    data.put("integerList", "");
    data.put("uuidList", "");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.getStringCollection()).containsExactly("");
    assertThat(bean.getIntegerList()).containsNull();
    assertThat(bean.getUuidList()).containsNull();
  }

  @Test
  public void shouldBindEmptySingleValuesToCollectionTypesWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringCollection", "");
    data.put("integerList", "");
    data.put("uuidList", "");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.stringCollection).containsExactly("");
    assertThat(bean.integerList).containsNull();
    assertThat(bean.uuidList).containsNull();
  }

  @Test
  public void shouldBindWhitespaceOnlySingleValuesToCollectionTypes() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringCollection", "   ");
    data.put("integerList", "   ");
    data.put("uuidList", "   ");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.getStringCollection()).containsExactly("   ");
    assertThat(bean.getIntegerList()).containsNull();
    assertThat(bean.getUuidList()).containsNull();
  }

  @Test
  public void shouldBindWhitespaceOnlySingleValuesToCollectionTypesWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringCollection", "   ");
    data.put("integerList", "   ");
    data.put("uuidList", "   ");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.stringCollection).containsExactly("   ");
    assertThat(bean.integerList).containsNull();
    assertThat(bean.uuidList).containsNull();
  }

  @Test
  public void shouldPreserveWhitespaceForStringCollectionValues() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("stringList[]", new String[] {" red ", " green "});
    data.put("stringArrayList[]", new String[] {" blue ", " yellow "});

    Form<FormData> form = bindRequestData(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.getStringList()).containsExactly(" red ", " green ");
    assertThat(bean.getStringArrayList()).containsExactly(" blue ", " yellow ");
  }

  @Test
  public void shouldPreserveWhitespaceForStringCollectionValuesWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("stringList[]", new String[] {" red ", " green "});
    data.put("stringArrayList[]", new String[] {" blue ", " yellow "});

    Form<FormData> form = bindRequestDataDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.stringList).containsExactly(" red ", " green ");
    assertThat(bean.stringArrayList).containsExactly(" blue ", " yellow ");
  }

  @Test
  public void shouldBindCollectionValuesFromRequestData() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("integerList[]", new String[] {"81", "82"});
    data.put("stringArrayList[]", new String[] {"red", "green"});
    data.put(
        "uuidList[]",
        new String[] {
          "2a32ce60-2a1d-4e10-a22e-58474baaa478", "dc5021dc-6a04-4cae-8caa-44a3590bc086"
        });
    data.put(
        "uuidArrayList[]",
        new String[] {
          "9f341f5f-180e-4b6c-8db7-0af6f6153509", "7c836183-e277-4804-af9c-4240c756ea2b"
        });

    Form<FormData> form = bindRequestData(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertCollectionValuesFromRequestData(bean);
  }

  @Test
  public void shouldBindCollectionValuesFromRequestDataWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("integerList[]", new String[] {"81", "82"});
    data.put("stringArrayList[]", new String[] {"red", "green"});
    data.put(
        "uuidList[]",
        new String[] {
          "2a32ce60-2a1d-4e10-a22e-58474baaa478", "dc5021dc-6a04-4cae-8caa-44a3590bc086"
        });
    data.put(
        "uuidArrayList[]",
        new String[] {
          "9f341f5f-180e-4b6c-8db7-0af6f6153509", "7c836183-e277-4804-af9c-4240c756ea2b"
        });

    Form<FormData> form = bindRequestDataDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertCollectionValuesFromRequestData(bean);
  }

  @Test
  public void shouldBindEmptyCollectionElementsFromRequestData() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("integerList[]", new String[] {"", "85"});
    data.put("uuidList[]", new String[] {"", "f1c1acef-8c9b-4669-9a94-7d8ac687d650"});

    Form<FormData> form = bindRequestData(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.getIntegerList()).containsExactly(null, 85);
    assertThat(bean.getUuidList())
        .containsExactly(null, UUID.fromString("f1c1acef-8c9b-4669-9a94-7d8ac687d650"));
  }

  @Test
  public void shouldBindEmptyCollectionElementsFromRequestDataWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("integerList[]", new String[] {"", "85"});
    data.put("uuidList[]", new String[] {"", "f1c1acef-8c9b-4669-9a94-7d8ac687d650"});

    Form<FormData> form = bindRequestDataDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.integerList).containsExactly(null, 85);
    assertThat(bean.uuidList)
        .containsExactly(null, UUID.fromString("f1c1acef-8c9b-4669-9a94-7d8ac687d650"));
  }

  @Test
  public void shouldBindWhitespaceOnlyCollectionElementsFromRequestData() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("integerList[]", new String[] {"   ", "86"});
    data.put("uuidList[]", new String[] {"   ", "e2ef0d70-7a5f-4086-88b7-fae75d1aa46d"});

    Form<FormData> form = bindRequestData(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.getIntegerList()).containsExactly(null, 86);
    assertThat(bean.getUuidList())
        .containsExactly(null, UUID.fromString("e2ef0d70-7a5f-4086-88b7-fae75d1aa46d"));
  }

  @Test
  public void shouldBindWhitespaceOnlyCollectionElementsFromRequestDataWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("integerList[]", new String[] {"   ", "86"});
    data.put("uuidList[]", new String[] {"   ", "e2ef0d70-7a5f-4086-88b7-fae75d1aa46d"});

    Form<FormData> form = bindRequestDataDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.integerList).containsExactly(null, 86);
    assertThat(bean.uuidList)
        .containsExactly(null, UUID.fromString("e2ef0d70-7a5f-4086-88b7-fae75d1aa46d"));
  }

  @Test
  public void shouldBindNullCollectionElementsFromRequestData() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("integerList[]", new String[] {null, "87"});
    data.put("uuidList[]", new String[] {null, "3b04d2a2-36b9-449a-9c0a-7dd1a0ea7d78"});

    Form<FormData> form = bindRequestData(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.getIntegerList()).containsExactly(null, 87);
    assertThat(bean.getUuidList())
        .containsExactly(null, UUID.fromString("3b04d2a2-36b9-449a-9c0a-7dd1a0ea7d78"));
  }

  @Test
  public void shouldBindNullCollectionElementsFromRequestDataWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String[]> data = new HashMap<>();
    data.put("integerList[]", new String[] {null, "87"});
    data.put("uuidList[]", new String[] {null, "3b04d2a2-36b9-449a-9c0a-7dd1a0ea7d78"});

    Form<FormData> form = bindRequestDataDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.integerList).containsExactly(null, 87);
    assertThat(bean.uuidList)
        .containsExactly(null, UUID.fromString("3b04d2a2-36b9-449a-9c0a-7dd1a0ea7d78"));
  }

  @Test
  public void shouldBindEmptyCollectionElementsFromIndexedData() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("integerList[0]", "");
    data.put("integerList[1]", "88");
    data.put("uuidList[0]", "");
    data.put("uuidList[1]", "4438e915-dd1a-4c7e-b459-f9e09ceee826");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.getIntegerList()).containsExactly(null, 88);
    assertThat(bean.getUuidList())
        .containsExactly(null, UUID.fromString("4438e915-dd1a-4c7e-b459-f9e09ceee826"));
  }

  @Test
  public void shouldBindEmptyCollectionElementsFromIndexedDataWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("integerList[0]", "");
    data.put("integerList[1]", "88");
    data.put("uuidList[0]", "");
    data.put("uuidList[1]", "4438e915-dd1a-4c7e-b459-f9e09ceee826");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.integerList).containsExactly(null, 88);
    assertThat(bean.uuidList)
        .containsExactly(null, UUID.fromString("4438e915-dd1a-4c7e-b459-f9e09ceee826"));
  }

  @Test
  public void shouldBindWhitespaceOnlyCollectionElementsFromIndexedData() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("integerList[0]", "   ");
    data.put("integerList[1]", "89");
    data.put("uuidList[0]", "   ");
    data.put("uuidList[1]", "d8c2e2e6-cd2a-436d-9abb-bb9809e5661f");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.getIntegerList()).containsExactly(null, 89);
    assertThat(bean.getUuidList())
        .containsExactly(null, UUID.fromString("d8c2e2e6-cd2a-436d-9abb-bb9809e5661f"));
  }

  @Test
  public void shouldBindWhitespaceOnlyCollectionElementsFromIndexedDataWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("integerList[0]", "   ");
    data.put("integerList[1]", "89");
    data.put("uuidList[0]", "   ");
    data.put("uuidList[1]", "d8c2e2e6-cd2a-436d-9abb-bb9809e5661f");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.integerList).containsExactly(null, 89);
    assertThat(bean.uuidList)
        .containsExactly(null, UUID.fromString("d8c2e2e6-cd2a-436d-9abb-bb9809e5661f"));
  }

  @Test
  public void shouldBindNullCollectionElementsFromIndexedData() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("integerList[0]", null);
    data.put("integerList[1]", "90");
    data.put("uuidList[0]", null);
    data.put("uuidList[1]", "b7892e0b-20ed-4829-b030-f44c9db8d1f7");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.getIntegerList()).containsExactly(null, 90);
    assertThat(bean.getUuidList())
        .containsExactly(null, UUID.fromString("b7892e0b-20ed-4829-b030-f44c9db8d1f7"));
  }

  @Test
  public void shouldBindNullCollectionElementsFromIndexedDataWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("integerList[0]", null);
    data.put("integerList[1]", "90");
    data.put("uuidList[0]", null);
    data.put("uuidList[1]", "b7892e0b-20ed-4829-b030-f44c9db8d1f7");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.integerList).containsExactly(null, 90);
    assertThat(bean.uuidList)
        .containsExactly(null, UUID.fromString("b7892e0b-20ed-4829-b030-f44c9db8d1f7"));
  }

  @Test
  public void shouldBindCollectionValuesFromIndexedData() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("integerList[0]", "83");
    data.put("integerList[1]", "84");
    data.put("stringArrayList[0]", "red");
    data.put("stringArrayList[1]", "green");
    data.put("uuidList[0]", "bb6c0da2-2b34-44dc-9f32-15f2e5a9dd11");
    data.put("uuidList[1]", "65748d5a-a2d5-46ae-b1fd-e759ac9e7328");
    data.put("uuidArrayList[0]", "3071586f-5f0e-4fa3-8756-d09d477d4780");
    data.put("uuidArrayList[1]", "17dab658-4ab5-42bf-95f2-98f6c1de9b65");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertCollectionValuesFromIndexedData(bean);
  }

  @Test
  public void shouldBindCollectionValuesFromIndexedDataWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("integerList[0]", "83");
    data.put("integerList[1]", "84");
    data.put("stringArrayList[0]", "red");
    data.put("stringArrayList[1]", "green");
    data.put("uuidList[0]", "bb6c0da2-2b34-44dc-9f32-15f2e5a9dd11");
    data.put("uuidList[1]", "65748d5a-a2d5-46ae-b1fd-e759ac9e7328");
    data.put("uuidArrayList[0]", "3071586f-5f0e-4fa3-8756-d09d477d4780");
    data.put("uuidArrayList[1]", "17dab658-4ab5-42bf-95f2-98f6c1de9b65");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertCollectionValuesFromIndexedData(bean);
  }

  private static void assertCollectionValuesFromRequestData(FormData bean) {
    assertThat(bean.getIntegerList()).containsExactly(81, 82);
    assertThat(bean.getStringArrayList()).containsExactly("red", "green");
    assertThat(bean.getUuidList())
        .containsExactly(
            UUID.fromString("2a32ce60-2a1d-4e10-a22e-58474baaa478"),
            UUID.fromString("dc5021dc-6a04-4cae-8caa-44a3590bc086"));
    assertThat(bean.getUuidArrayList())
        .containsExactly(
            UUID.fromString("9f341f5f-180e-4b6c-8db7-0af6f6153509"),
            UUID.fromString("7c836183-e277-4804-af9c-4240c756ea2b"));
  }

  private static void assertCollectionValuesFromIndexedData(FormData bean) {
    assertThat(bean.getIntegerList()).containsExactly(83, 84);
    assertThat(bean.getStringArrayList()).containsExactly("red", "green");
    assertThat(bean.getUuidList())
        .containsExactly(
            UUID.fromString("bb6c0da2-2b34-44dc-9f32-15f2e5a9dd11"),
            UUID.fromString("65748d5a-a2d5-46ae-b1fd-e759ac9e7328"));
    assertThat(bean.getUuidArrayList())
        .containsExactly(
            UUID.fromString("3071586f-5f0e-4fa3-8756-d09d477d4780"),
            UUID.fromString("17dab658-4ab5-42bf-95f2-98f6c1de9b65"));
  }

  @Test
  public void shouldBindMapValuesFromIndexedData() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringIntegerMap[red]", "91");
    data.put("stringIntegerMap[green]", "92");
    data.put("sortedStringIntegerMap[green]", "94");
    data.put("sortedStringIntegerMap[red]", "93");
    data.put("stringIntegerTreeMap[green]", "96");
    data.put("stringIntegerTreeMap[red]", "95");
    data.put("stringIntegerLinkedHashMap[red]", "97");
    data.put("stringIntegerLinkedHashMap[green]", "98");
    data.put("uuidIntegerMap[637a3d4e-3f92-4636-aa06-e923168c4c69]", "101");
    data.put("uuidIntegerMap[669033af-e0d4-4fc9-80f9-8d4bfa73ddc8]", "102");
    data.put("sortedUuidIntegerMap[5ad1b139-d07e-4495-a25b-da66a54bb698]", "104");
    data.put("sortedUuidIntegerMap[88e333e0-43d3-4f34-b66e-356cd7bbc1a8]", "103");
    data.put("uuidIntegerTreeMap[af46cdd3-f9dd-4850-adef-f6f154fd888a]", "106");
    data.put("uuidIntegerTreeMap[f554b329-316f-48fe-bc96-f1fef3bd9d3a]", "105");
    data.put("uuidIntegerLinkedHashMap[f1a306b1-4a33-409e-926e-277af05c3762]", "107");
    data.put("uuidIntegerLinkedHashMap[0de9fb0a-695e-427b-a9ad-7c737ae933dd]", "108");
    data.put("rawMap[red]", "raw-91");
    data.put("rawMap[green]", "raw-92");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertMapValuesFromIndexedData(bean);
  }

  @Test
  public void shouldBindMapValuesFromIndexedDataWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringIntegerMap[red]", "91");
    data.put("stringIntegerMap[green]", "92");
    data.put("sortedStringIntegerMap[green]", "94");
    data.put("sortedStringIntegerMap[red]", "93");
    data.put("stringIntegerTreeMap[green]", "96");
    data.put("stringIntegerTreeMap[red]", "95");
    data.put("stringIntegerLinkedHashMap[red]", "97");
    data.put("stringIntegerLinkedHashMap[green]", "98");
    data.put("uuidIntegerMap[637a3d4e-3f92-4636-aa06-e923168c4c69]", "101");
    data.put("uuidIntegerMap[669033af-e0d4-4fc9-80f9-8d4bfa73ddc8]", "102");
    data.put("sortedUuidIntegerMap[5ad1b139-d07e-4495-a25b-da66a54bb698]", "104");
    data.put("sortedUuidIntegerMap[88e333e0-43d3-4f34-b66e-356cd7bbc1a8]", "103");
    data.put("uuidIntegerTreeMap[af46cdd3-f9dd-4850-adef-f6f154fd888a]", "106");
    data.put("uuidIntegerTreeMap[f554b329-316f-48fe-bc96-f1fef3bd9d3a]", "105");
    data.put("uuidIntegerLinkedHashMap[f1a306b1-4a33-409e-926e-277af05c3762]", "107");
    data.put("uuidIntegerLinkedHashMap[0de9fb0a-695e-427b-a9ad-7c737ae933dd]", "108");
    data.put("rawMap[red]", "raw-91");
    data.put("rawMap[green]", "raw-92");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertMapValuesFromIndexedData(bean);
  }

  private static void assertMapValuesFromIndexedData(FormData bean) {
    assertThat(bean.getStringIntegerMap()).containsEntry("red", 91).containsEntry("green", 92);
    assertThat(bean.getSortedStringIntegerMap())
        .containsExactly(Map.entry("green", 94), Map.entry("red", 93));
    assertThat(bean.getStringIntegerTreeMap())
        .containsExactly(Map.entry("green", 96), Map.entry("red", 95));
    assertThat(bean.getStringIntegerLinkedHashMap())
        .containsExactly(Map.entry("red", 97), Map.entry("green", 98));
    assertThat(bean.getUuidIntegerMap())
        .containsEntry(UUID.fromString("637a3d4e-3f92-4636-aa06-e923168c4c69"), 101)
        .containsEntry(UUID.fromString("669033af-e0d4-4fc9-80f9-8d4bfa73ddc8"), 102);
    assertThat(bean.getSortedUuidIntegerMap())
        .containsExactly(
            Map.entry(UUID.fromString("88e333e0-43d3-4f34-b66e-356cd7bbc1a8"), 103),
            Map.entry(UUID.fromString("5ad1b139-d07e-4495-a25b-da66a54bb698"), 104));
    assertThat(bean.getUuidIntegerTreeMap())
        .containsExactly(
            Map.entry(UUID.fromString("af46cdd3-f9dd-4850-adef-f6f154fd888a"), 106),
            Map.entry(UUID.fromString("f554b329-316f-48fe-bc96-f1fef3bd9d3a"), 105));
    assertThat(bean.getUuidIntegerLinkedHashMap())
        .containsExactly(
            Map.entry(UUID.fromString("f1a306b1-4a33-409e-926e-277af05c3762"), 107),
            Map.entry(UUID.fromString("0de9fb0a-695e-427b-a9ad-7c737ae933dd"), 108));
    assertThat(bean.getRawMap().get("red")).isEqualTo("raw-91");
    assertThat(bean.getRawMap().get("green")).isEqualTo("raw-92");
  }

  @Test
  public void shouldRejectRawCollectionValuesFromIndexedData() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("rawCollection[0]", "raw-red");
    data.put("rawCollection[1]", "raw-green");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "rawCollection[0]");
    assertInvalidError(form, "rawCollection[1]");
  }

  @Test
  public void shouldRejectRawCollectionValuesFromIndexedDataWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("rawCollection[0]", "raw-red");
    data.put("rawCollection[1]", "raw-green");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "rawCollection[0]");
    assertInvalidError(form, "rawCollection[1]");
  }

  @Test
  public void shouldBindEmptyMapKeysAndValuesFromIndexedData() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringIntegerMap[red]", "");
    data.put("uuidIntegerMap['']", "115");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    assertThat(form.get().getStringIntegerMap()).containsEntry("red", null);
    assertThat(form.get().getUuidIntegerMap()).containsEntry(null, 115);
  }

  @Test
  public void shouldBindEmptyMapKeysAndValuesFromIndexedDataWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringIntegerMap[red]", "");
    data.put("uuidIntegerMap['']", "115");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    assertThat(form.get().stringIntegerMap).containsEntry("red", null);
    assertThat(form.get().uuidIntegerMap).containsEntry(null, 115);
  }

  @Test
  public void shouldBindWhitespaceOnlyMapKeysAndValuesFromIndexedData() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringIntegerMap[red]", "   ");
    data.put("uuidIntegerMap['   ']", "116");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    assertThat(form.get().getStringIntegerMap()).containsEntry("red", null);
    assertThat(form.get().getUuidIntegerMap()).containsEntry(null, 116);
  }

  @Test
  public void shouldBindWhitespaceOnlyMapKeysAndValuesFromIndexedDataWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringIntegerMap[red]", "   ");
    data.put("uuidIntegerMap['   ']", "116");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    assertThat(form.get().stringIntegerMap).containsEntry("red", null);
    assertThat(form.get().uuidIntegerMap).containsEntry(null, 116);
  }

  @Test
  public void shouldBindNullMapValuesFromIndexedData() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringIntegerMap[red]", null);
    data.put("uuidIntegerMap['']", null);

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    assertThat(form.get().getStringIntegerMap()).containsEntry("red", null);
    assertThat(form.get().getUuidIntegerMap()).containsEntry(null, null);
  }

  @Test
  public void shouldBindNullMapValuesFromIndexedDataWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringIntegerMap[red]", null);
    data.put("uuidIntegerMap['']", null);

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    assertThat(form.get().stringIntegerMap).containsEntry("red", null);
    assertThat(form.get().uuidIntegerMap).containsEntry(null, null);
  }

  @Test
  public void shouldBindMapValuesFromQuotedIndexedData() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringIntegerMap['red']", "111");
    data.put("stringIntegerMap[\"green\"]", "112");
    data.put("stringIntegerLinkedHashMap['red']", "113");
    data.put("stringIntegerLinkedHashMap[\"green\"]", "114");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.getStringIntegerMap()).containsEntry("red", 111).containsEntry("green", 112);
    assertThat(bean.getStringIntegerLinkedHashMap())
        .containsEntry("red", 113)
        .containsEntry("green", 114);
  }

  @Test
  public void shouldBindMapValuesFromQuotedIndexedDataWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringIntegerMap['red']", "111");
    data.put("stringIntegerMap[\"green\"]", "112");
    data.put("stringIntegerLinkedHashMap['red']", "113");
    data.put("stringIntegerLinkedHashMap[\"green\"]", "114");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.stringIntegerMap).containsEntry("red", 111).containsEntry("green", 112);
    assertThat(bean.stringIntegerLinkedHashMap)
        .containsEntry("red", 113)
        .containsEntry("green", 114);
  }

  @Test
  public void shouldRejectInvalidCollectionAndMapValuesFromIndexedData() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("uuidList[0]", "not-a-uuid");
    data.put("uuidIntegerMap[not-a-uuid]", "121");
    data.put("stringIntegerMap[red]", "not-a-number");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertThat(form.errors()).hasSize(3);
    assertInvalidError(form, "uuidList[0]");
    assertInvalidError(form, "stringIntegerMap[red]");
    assertInvalidError(form, "uuidIntegerMap[not-a-uuid]");
  }

  @Test
  public void shouldRejectInvalidCollectionAndMapValuesFromIndexedDataWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("uuidList[0]", "not-a-uuid");
    data.put("uuidIntegerMap[not-a-uuid]", "121");
    data.put("stringIntegerMap[red]", "not-a-number");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertThat(form.errors()).hasSize(3);
    assertInvalidError(form, "uuidList[0]");
    assertInvalidError(form, "stringIntegerMap[red]");
    assertInvalidError(form, "uuidIntegerMap[not-a-uuid]");
  }

  @Test
  public void shouldPreserveWhitespaceForStringMapKeysAndValues() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringStringMap[ red ]", " blue ");
    data.put("stringStringMap[' green ']", " yellow ");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.errors()).isEmpty();
    assertThat(form.get().getStringStringMap())
        .containsEntry(" red ", " blue ")
        .containsEntry(" green ", " yellow ");
  }

  @Test
  public void shouldPreserveWhitespaceForStringMapKeysAndValuesWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringStringMap[ red ]", " blue ");
    data.put("stringStringMap[' green ']", " yellow ");

    Form<FormData> form = bindDirect(formFactory, data);

    assertThat(form.errors()).isEmpty();
    assertThat(form.get().stringStringMap)
        .containsEntry(" red ", " blue ")
        .containsEntry(" green ", " yellow ");
  }

  @Test
  public void shouldBindIndexedNestedBeanProperties() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("array[0].name", "array-zero");
    data.put("array[1].name", "array-one");
    data.put("list[0].name", "list-zero");
    data.put("list[1].name", "list-one");
    data.put("map[key1].name", "map-one");
    data.put("map['key2'].name", "map-two");

    Form<IndexedFormData> form =
        formFactory.form(IndexedFormData.class).bind(Lang.defaultLang(), TypedMap.empty(), data);

    assertThat(form.errors()).isEmpty();
    IndexedFormData bean = form.get();
    assertThat(bean.getArray()[0].getName()).isEqualTo("array-zero");
    assertThat(bean.getArray()[1].getName()).isEqualTo("array-one");
    assertThat(bean.getList().get(0).getName()).isEqualTo("list-zero");
    assertThat(bean.getList().get(1).getName()).isEqualTo("list-one");
    assertThat(bean.getMap().get("key1").getName()).isEqualTo("map-one");
    assertThat(bean.getMap().get("key2").getName()).isEqualTo("map-two");
  }

  @Test
  public void shouldBindDirectFieldIndexedNestedBeanProperties() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("array[0].name", "array-zero");
    data.put("array[1].name", "array-one");
    data.put("list[0].name", "list-zero");
    data.put("list[1].name", "list-one");
    data.put("map[key1].name", "map-one");
    data.put("map['key2'].name", "map-two");

    Form<IndexedFormData> form =
        formFactory
            .form(IndexedFormData.class)
            .withDirectFieldAccess(true)
            .bind(Lang.defaultLang(), TypedMap.empty(), data);

    assertThat(form.errors()).isEmpty();
    IndexedFormData bean = form.get();
    assertThat(bean.array[0].name).isEqualTo("array-zero");
    assertThat(bean.array[1].name).isEqualTo("array-one");
    assertThat(bean.list.get(0).name).isEqualTo("list-zero");
    assertThat(bean.list.get(1).name).isEqualTo("list-one");
    assertThat(bean.map.get("key1").name).isEqualTo("map-one");
    assertThat(bean.map.get("key2").name).isEqualTo("map-two");
  }

  @Test
  public void shouldBindDeeplyNestedIndexedBeanProperties() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("array[0].nested.array[0].name", "nested-array-zero");
    data.put("array[1].nested.array[1].name", "nested-array-one");
    data.put("list[0].nested.list[0].name", "nested-list-zero");
    data.put("list[1].nested.list[1].name", "nested-list-one");
    data.put("map[key1].nested.map[\"key1\"].name", "nested-map-one");
    data.put("map['key2'].nested.map[key2].name", "nested-map-two");

    Form<IndexedFormData> form =
        formFactory.form(IndexedFormData.class).bind(Lang.defaultLang(), TypedMap.empty(), data);

    assertThat(form.errors()).isEmpty();
    IndexedFormData bean = form.get();
    assertThat(bean.getArray()[0].getNested().getArray()[0].getName())
        .isEqualTo("nested-array-zero");
    assertThat(bean.getArray()[1].getNested().getArray()[1].getName())
        .isEqualTo("nested-array-one");
    assertThat(bean.getList().get(0).getNested().getList().get(0).getName())
        .isEqualTo("nested-list-zero");
    assertThat(bean.getList().get(1).getNested().getList().get(1).getName())
        .isEqualTo("nested-list-one");
    assertThat(bean.getMap().get("key1").getNested().getMap().get("key1").getName())
        .isEqualTo("nested-map-one");
    assertThat(bean.getMap().get("key2").getNested().getMap().get("key2").getName())
        .isEqualTo("nested-map-two");
  }

  @Test
  public void shouldBindDirectFieldDeeplyNestedIndexedBeanProperties() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("array[0].nested.array[0].name", "nested-array-zero");
    data.put("array[1].nested.array[1].name", "nested-array-one");
    data.put("list[0].nested.list[0].name", "nested-list-zero");
    data.put("list[1].nested.list[1].name", "nested-list-one");
    data.put("map[key1].nested.map[\"key1\"].name", "nested-map-one");
    data.put("map['key2'].nested.map[key2].name", "nested-map-two");

    Form<IndexedFormData> form =
        formFactory
            .form(IndexedFormData.class)
            .withDirectFieldAccess(true)
            .bind(Lang.defaultLang(), TypedMap.empty(), data);

    assertThat(form.errors()).isEmpty();
    IndexedFormData bean = form.get();
    assertThat(bean.array[0].nested.array[0].name).isEqualTo("nested-array-zero");
    assertThat(bean.array[1].nested.array[1].name).isEqualTo("nested-array-one");
    assertThat(bean.list.get(0).nested.list.get(0).name).isEqualTo("nested-list-zero");
    assertThat(bean.list.get(1).nested.list.get(1).name).isEqualTo("nested-list-one");
    assertThat(bean.map.get("key1").nested.map.get("key1").name).isEqualTo("nested-map-one");
    assertThat(bean.map.get("key2").nested.map.get("key2").name).isEqualTo("nested-map-two");
  }

  @Test
  public void shouldRejectIndexedBindingBeyondDefaultAutoGrowCollectionLimit() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("list[256].name", "too-far");
    data.put("array[256].name", "too-far");

    Form<IndexedFormData> form =
        formFactory.form(IndexedFormData.class).bind(Lang.defaultLang(), TypedMap.empty(), data);

    assertThat(form.hasErrors()).isTrue();
    assertThat(form.errors("list[256]")).hasSize(1);
    assertThat(form.errors("array[256]")).hasSize(1);
  }

  @Test
  public void shouldRejectDirectFieldIndexedBindingBeyondDefaultAutoGrowCollectionLimit() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("list[256].name", "too-far");
    data.put("array[256].name", "too-far");

    Form<IndexedFormData> form =
        formFactory
            .form(IndexedFormData.class)
            .withDirectFieldAccess(true)
            .bind(Lang.defaultLang(), TypedMap.empty(), data);

    assertThat(form.hasErrors()).isTrue();
    assertThat(form.errors("list[256]")).hasSize(1);
    assertThat(form.errors("array[256]")).hasSize(1);
  }

  @Test
  public void shouldRejectRootNamedIndexedBindingBeyondDefaultAutoGrowCollectionLimit() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("indexed.list[256].name", "too-far");
    data.put("indexed.array[256].name", "too-far");

    Form<IndexedFormData> form =
        formFactory
            .form("indexed", IndexedFormData.class)
            .bind(Lang.defaultLang(), TypedMap.empty(), data);

    assertThat(form.hasErrors()).isTrue();
    assertThat(form.errors("indexed.list[256]")).hasSize(1);
    assertThat(form.errors("indexed.array[256]")).hasSize(1);
  }

  @Test
  public void shouldBindIndexedBindingAtDefaultAutoGrowCollectionLimitBoundary() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("list[255].name", "last-allowed-list");
    data.put("array[255].name", "last-allowed-array");

    Form<IndexedFormData> form =
        formFactory.form(IndexedFormData.class).bind(Lang.defaultLang(), TypedMap.empty(), data);

    assertThat(form.errors()).isEmpty();
    IndexedFormData bean = form.get();
    assertThat(bean.getList()).hasSize(256);
    assertThat(bean.getList().get(255).getName()).isEqualTo("last-allowed-list");
    assertThat(bean.getArray()).hasSize(256);
    assertThat(bean.getArray()[255].getName()).isEqualTo("last-allowed-array");
  }

  @Test
  public void shouldBindDirectFieldIndexedBindingAtDefaultAutoGrowCollectionLimitBoundary() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("list[255].name", "last-allowed-list");
    data.put("array[255].name", "last-allowed-array");

    Form<IndexedFormData> form =
        formFactory
            .form(IndexedFormData.class)
            .withDirectFieldAccess(true)
            .bind(Lang.defaultLang(), TypedMap.empty(), data);

    assertThat(form.errors()).isEmpty();
    IndexedFormData bean = form.get();
    assertThat(bean.list).hasSize(256);
    assertThat(bean.list.get(255).name).isEqualTo("last-allowed-list");
    assertThat(bean.array).hasSize(256);
    assertThat(bean.array[255].name).isEqualTo("last-allowed-array");
    assertThat(bean.getList()).hasSize(256);
    assertThat(bean.getList().get(255).getName()).isEqualTo("last-allowed-list");
    assertThat(bean.getArray()).hasSize(256);
    assertThat(bean.getArray()[255].getName()).isEqualTo("last-allowed-array");
  }

  @Test
  public void shouldContinueBindingAfterIndexedBindingBeyondDefaultAutoGrowCollectionLimit() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("list[256].name", "too-far");
    data.put("map[key1].name", "still-bound");

    Form<IndexedFormData> form =
        formFactory.form(IndexedFormData.class).bind(Lang.defaultLang(), TypedMap.empty(), data);

    assertThat(form.hasErrors()).isTrue();
    assertThat(form.errors("list[256]")).hasSize(1);
    assertThat(form.discardingErrors().get().getMap().get("key1").getName())
        .isEqualTo("still-bound");
  }

  @Test
  public void
      shouldContinueDirectFieldBindingAfterIndexedBindingBeyondDefaultAutoGrowCollectionLimit() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("list[256].name", "too-far");
    data.put("map[key1].name", "still-bound");

    Form<IndexedFormData> form =
        formFactory
            .form(IndexedFormData.class)
            .withDirectFieldAccess(true)
            .bind(Lang.defaultLang(), TypedMap.empty(), data);

    assertThat(form.hasErrors()).isTrue();
    assertThat(form.errors("list[256]")).hasSize(1);
    assertThat(form.discardingErrors().get().map.get("key1").name).isEqualTo("still-bound");
    assertThat(form.discardingErrors().get().getMap().get("key1").getName())
        .isEqualTo("still-bound");
  }

  @Test
  public void shouldIgnoreUnknownFieldsDuringBinding() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("doesNotExist", "ignored");
    data.put("map[key1].name", "known");

    Form<IndexedFormData> form =
        formFactory.form(IndexedFormData.class).bind(Lang.defaultLang(), TypedMap.empty(), data);

    assertThat(form.errors()).isEmpty();
    assertThat(form.get().getMap().get("key1").getName()).isEqualTo("known");
  }

  @Test
  public void shouldIgnoreUnknownFieldsWithDirectFieldAccess() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("doesNotExist", "ignored");
    data.put("map[key1].name", "known");

    Form<IndexedFormData> form =
        formFactory
            .form(IndexedFormData.class)
            .withDirectFieldAccess(true)
            .bind(Lang.defaultLang(), TypedMap.empty(), data);

    assertThat(form.errors()).isEmpty();
    assertThat(form.get().map.get("key1").name).isEqualTo("known");
    assertThat(form.get().getMap().get("key1").getName()).isEqualTo("known");
  }

  @Test
  public void shouldRejectNormalTypeMismatchDuringBinding() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("integerValue", "not-a-number");
    data.put("stringValue", "still-bound");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "integerValue");
    assertThat(form.discardingErrors().get().getStringValue()).isEqualTo("still-bound");
  }

  @Test
  public void shouldRejectDirectFieldTypeMismatchDuringBinding() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("integerValue", "not-a-number");
    data.put("stringValue", "still-bound");

    Form<FormData> form =
        formFactory
            .form(FormData.class)
            .withDirectFieldAccess(true)
            .bind(Lang.defaultLang(), TypedMap.empty(), data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "integerValue");
    assertThat(form.discardingErrors().get().stringValue).isEqualTo("still-bound");
  }

  @Test
  public void shouldHonorAllowedFieldsDuringBinding() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("stringValue", "allowed");
    data.put("integerValue", "123");

    Form<FormData> form =
        formFactory
            .form(FormData.class)
            .bind(Lang.defaultLang(), TypedMap.empty(), data, "stringValue");

    assertThat(form.errors()).isEmpty();
    FormData bean = form.get();
    assertThat(bean.getStringValue()).isEqualTo("allowed");
    assertThat(bean.getIntegerValue()).isNull();
  }

  @Test
  public void shouldReportMultipleBindingFailuresDuringBinding() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("integerValue", "not-a-number");
    data.put("longValue", "not-a-long");
    data.put("uuid", "not-a-uuid");
    data.put("stringValue", "still-bound");

    Form<FormData> form = bind(formFactory, data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "integerValue");
    assertInvalidError(form, "longValue");
    assertInvalidError(form, "uuid");
    assertThat(form.discardingErrors().get().getStringValue()).isEqualTo("still-bound");
  }

  @Test
  public void shouldIgnoreUnknownInvalidNestedFieldsDuringBinding() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("doesNotExist[256].name", "ignored");
    data.put("map[key1].name", "known");

    Form<IndexedFormData> form =
        formFactory.form(IndexedFormData.class).bind(Lang.defaultLang(), TypedMap.empty(), data);

    assertThat(form.errors()).isEmpty();
    assertThat(form.get().getMap().get("key1").getName()).isEqualTo("known");
  }

  @Test
  public void shouldRunBeanValidationAfterBindingFailures() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("integerValue", "not-a-number");
    data.put("stringValue", "still-bound");

    Form<ValidatedFormData> form =
        formFactory.form(ValidatedFormData.class).bind(Lang.defaultLang(), TypedMap.empty(), data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "integerValue");
    assertThat(form.errors("requiredValue")).hasSize(1);
    assertThat(form.errors("requiredValue").get(0).messages()).containsExactly("error.required");
    assertThat(form.discardingErrors().get().getStringValue()).isEqualTo("still-bound");
  }

  @Test
  public void shouldRunBeanValidationAfterDirectFieldBindingFailures() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("integerValue", "not-a-number");
    data.put("stringValue", "still-bound");

    Form<ValidatedFormData> form =
        formFactory
            .form(ValidatedFormData.class)
            .withDirectFieldAccess(true)
            .bind(Lang.defaultLang(), TypedMap.empty(), data);

    assertThat(form.hasErrors()).isTrue();
    assertInvalidError(form, "integerValue");
    assertThat(form.errors("requiredValue")).hasSize(1);
    assertThat(form.errors("requiredValue").get(0).messages()).containsExactly("error.required");
    assertThat(form.discardingErrors().get().stringValue).isEqualTo("still-bound");
  }

  @Test
  public void shouldBindFileAfterIndexedBindingBeyondDefaultAutoGrowCollectionLimit() {
    FormFactory formFactory = instanceOf(FormFactory.class);
    Map<String, String> data = new HashMap<>();
    data.put("list[256].name", "too-far");
    Map<String, FilePart<?>> files = new HashMap<>();
    FilePart<String> upload = new FilePart<>("upload", "test.txt", "text/plain", "content");
    files.put("upload", upload);

    Form<FileBindingFormData> form =
        formFactory
            .form(FileBindingFormData.class)
            .bind(Lang.defaultLang(), TypedMap.empty(), data, files);

    assertThat(form.hasErrors()).isTrue();
    assertThat(form.errors("list[256]")).hasSize(1);
    assertThat(form.discardingErrors().get().getUpload()).isSameAs(upload);
  }

  public static class FormData {

    private byte[] byteArray;
    private char[] charArray;
    private char primitiveChar;
    private Character character;
    private boolean primitiveBoolean;
    private Boolean booleanValue;
    private byte primitiveByte;
    private Byte byteValue;
    private short primitiveShort;
    private Short shortValue;
    private int primitiveInt;
    private Integer integerValue;
    private Integer integerValueFromHexPrefix;
    private Integer integerValueFromUppercaseHexPrefix;
    private Integer integerValueWithLeadingZero;
    private long primitiveLong;
    private Long longValue;
    private Long longValueFromNegativeHexPrefix;
    private float primitiveFloat;
    private Float floatValue;
    private double primitiveDouble;
    private Double doubleValue;
    private BigDecimal bigDecimal;
    private BigInteger bigInteger;
    private BigInteger bigIntegerFromUppercaseHexPrefix;
    private BigInteger bigIntegerFromHashHexPrefix;
    private BigInteger bigIntegerFromNegativeHexPrefix;
    private Charset charset;
    private Currency currency;
    private Locale locale;
    private Locale localeLanguageOnly;
    private Locale localeWithMultiPartVariant;
    private Pattern pattern;
    private TimeZone timeZone;
    private TimeZone timeZoneGmtOffset;
    private URI uri;
    private URI uriMailto;
    private URI uriCustomScheme;
    private URI uriRelative;
    private URI uriClasspath;
    private URI uriWithFragment;
    private URI uriWithEncodedFragment;
    private URI uriWithNonAscii;
    private URI uriAlreadyEncoded;
    private URL url;
    private URL urlMailto;
    private URL urlClasspath;
    private URL urlUnknownProtocol;
    private UUID uuid;
    private ZoneId zoneId;
    private ZoneId zoneIdOffset;
    private Optional<String> optionalValue;
    private Date dateValue;
    private Object objectValue;
    private StaticValue staticValue;
    private StaticValue staticValueWithWrongFieldType;
    private SampleEnum sampleEnum;
    private SampleEnum sampleEnumAlias;
    private String stringValue;
    private SampleEnum[] sampleEnumArray;
    private List<SampleEnum> sampleEnumList;
    private Map<SampleEnum, SampleEnum> sampleEnumMap;
    private String[] stringArray;
    private Character[] characterArray;
    private Boolean[] booleanArray;
    private Byte[] byteArrayElements;
    private byte[] primitiveByteArrayElements;
    private Short[] shortArray;
    private short[] primitiveShortArray;
    private Integer[] integerArray;
    private int[] primitiveIntArray;
    private Long[] longArray;
    private long[] primitiveLongArray;
    private Float[] floatArray;
    private float[] primitiveFloatArray;
    private Double[] doubleArray;
    private double[] primitiveDoubleArray;
    private BigDecimal[] bigDecimalArray;
    private BigInteger[] bigIntegerArray;
    private Charset[] charsetArray;
    private Currency[] currencyArray;
    private Locale[] localeArray;
    private Pattern[] patternArray;
    private TimeZone[] timeZoneArray;
    private URI[] uriArray;
    private URL[] urlArray;
    private UUID[] uuidArray;
    private ZoneId[] zoneIdArray;
    private Collection<String> stringCollection;
    private List<String> stringList;
    private List<Integer> integerList;
    private Set<String> stringSet;
    private SortedSet<String> sortedStringSet;
    private ArrayList<String> stringArrayList;
    private Collection rawCollection;
    private Map<String, String> stringStringMap;
    private Map<String, Integer> stringIntegerMap;
    private SortedMap<String, Integer> sortedStringIntegerMap;
    private TreeMap<String, Integer> stringIntegerTreeMap;
    private LinkedHashMap<String, Integer> stringIntegerLinkedHashMap;
    private Map rawMap;
    private Collection<UUID> uuidCollection;
    private List<UUID> uuidList;
    private Set<UUID> uuidSet;
    private SortedSet<UUID> sortedUuidSet;
    private ArrayList<UUID> uuidArrayList;
    private Map<UUID, Integer> uuidIntegerMap;
    private SortedMap<UUID, Integer> sortedUuidIntegerMap;
    private TreeMap<UUID, Integer> uuidIntegerTreeMap;
    private LinkedHashMap<UUID, Integer> uuidIntegerLinkedHashMap;

    public byte[] getByteArray() {
      return byteArray;
    }

    public void setByteArray(byte[] byteArray) {
      this.byteArray = byteArray;
    }

    public char[] getCharArray() {
      return charArray;
    }

    public void setCharArray(char[] charArray) {
      this.charArray = charArray;
    }

    public char getPrimitiveChar() {
      return primitiveChar;
    }

    public void setPrimitiveChar(char primitiveChar) {
      this.primitiveChar = primitiveChar;
    }

    public Character getCharacter() {
      return character;
    }

    public void setCharacter(Character character) {
      this.character = character;
    }

    public boolean isPrimitiveBoolean() {
      return primitiveBoolean;
    }

    public void setPrimitiveBoolean(boolean primitiveBoolean) {
      this.primitiveBoolean = primitiveBoolean;
    }

    public Boolean getBooleanValue() {
      return booleanValue;
    }

    public void setBooleanValue(Boolean booleanValue) {
      this.booleanValue = booleanValue;
    }

    public byte getPrimitiveByte() {
      return primitiveByte;
    }

    public void setPrimitiveByte(byte primitiveByte) {
      this.primitiveByte = primitiveByte;
    }

    public Byte getByteValue() {
      return byteValue;
    }

    public void setByteValue(Byte byteValue) {
      this.byteValue = byteValue;
    }

    public short getPrimitiveShort() {
      return primitiveShort;
    }

    public void setPrimitiveShort(short primitiveShort) {
      this.primitiveShort = primitiveShort;
    }

    public Short getShortValue() {
      return shortValue;
    }

    public void setShortValue(Short shortValue) {
      this.shortValue = shortValue;
    }

    public int getPrimitiveInt() {
      return primitiveInt;
    }

    public void setPrimitiveInt(int primitiveInt) {
      this.primitiveInt = primitiveInt;
    }

    public Integer getIntegerValue() {
      return integerValue;
    }

    public void setIntegerValue(Integer integerValue) {
      this.integerValue = integerValue;
    }

    public Integer getIntegerValueFromHexPrefix() {
      return integerValueFromHexPrefix;
    }

    public void setIntegerValueFromHexPrefix(Integer integerValueFromHexPrefix) {
      this.integerValueFromHexPrefix = integerValueFromHexPrefix;
    }

    public Integer getIntegerValueFromUppercaseHexPrefix() {
      return integerValueFromUppercaseHexPrefix;
    }

    public void setIntegerValueFromUppercaseHexPrefix(Integer integerValueFromUppercaseHexPrefix) {
      this.integerValueFromUppercaseHexPrefix = integerValueFromUppercaseHexPrefix;
    }

    public Integer getIntegerValueWithLeadingZero() {
      return integerValueWithLeadingZero;
    }

    public void setIntegerValueWithLeadingZero(Integer integerValueWithLeadingZero) {
      this.integerValueWithLeadingZero = integerValueWithLeadingZero;
    }

    public long getPrimitiveLong() {
      return primitiveLong;
    }

    public void setPrimitiveLong(long primitiveLong) {
      this.primitiveLong = primitiveLong;
    }

    public Long getLongValue() {
      return longValue;
    }

    public void setLongValue(Long longValue) {
      this.longValue = longValue;
    }

    public Long getLongValueFromNegativeHexPrefix() {
      return longValueFromNegativeHexPrefix;
    }

    public void setLongValueFromNegativeHexPrefix(Long longValueFromNegativeHexPrefix) {
      this.longValueFromNegativeHexPrefix = longValueFromNegativeHexPrefix;
    }

    public float getPrimitiveFloat() {
      return primitiveFloat;
    }

    public void setPrimitiveFloat(float primitiveFloat) {
      this.primitiveFloat = primitiveFloat;
    }

    public Float getFloatValue() {
      return floatValue;
    }

    public void setFloatValue(Float floatValue) {
      this.floatValue = floatValue;
    }

    public double getPrimitiveDouble() {
      return primitiveDouble;
    }

    public void setPrimitiveDouble(double primitiveDouble) {
      this.primitiveDouble = primitiveDouble;
    }

    public Double getDoubleValue() {
      return doubleValue;
    }

    public void setDoubleValue(Double doubleValue) {
      this.doubleValue = doubleValue;
    }

    public BigDecimal getBigDecimal() {
      return bigDecimal;
    }

    public void setBigDecimal(BigDecimal bigDecimal) {
      this.bigDecimal = bigDecimal;
    }

    public BigInteger getBigInteger() {
      return bigInteger;
    }

    public void setBigInteger(BigInteger bigInteger) {
      this.bigInteger = bigInteger;
    }

    public BigInteger getBigIntegerFromUppercaseHexPrefix() {
      return bigIntegerFromUppercaseHexPrefix;
    }

    public void setBigIntegerFromUppercaseHexPrefix(BigInteger bigIntegerFromUppercaseHexPrefix) {
      this.bigIntegerFromUppercaseHexPrefix = bigIntegerFromUppercaseHexPrefix;
    }

    public BigInteger getBigIntegerFromHashHexPrefix() {
      return bigIntegerFromHashHexPrefix;
    }

    public void setBigIntegerFromHashHexPrefix(BigInteger bigIntegerFromHashHexPrefix) {
      this.bigIntegerFromHashHexPrefix = bigIntegerFromHashHexPrefix;
    }

    public BigInteger getBigIntegerFromNegativeHexPrefix() {
      return bigIntegerFromNegativeHexPrefix;
    }

    public void setBigIntegerFromNegativeHexPrefix(BigInteger bigIntegerFromNegativeHexPrefix) {
      this.bigIntegerFromNegativeHexPrefix = bigIntegerFromNegativeHexPrefix;
    }

    public Charset getCharset() {
      return charset;
    }

    public void setCharset(Charset charset) {
      this.charset = charset;
    }

    public Currency getCurrency() {
      return currency;
    }

    public void setCurrency(Currency currency) {
      this.currency = currency;
    }

    public Locale getLocale() {
      return locale;
    }

    public void setLocale(Locale locale) {
      this.locale = locale;
    }

    public Locale getLocaleLanguageOnly() {
      return localeLanguageOnly;
    }

    public void setLocaleLanguageOnly(Locale localeLanguageOnly) {
      this.localeLanguageOnly = localeLanguageOnly;
    }

    public Locale getLocaleWithMultiPartVariant() {
      return localeWithMultiPartVariant;
    }

    public void setLocaleWithMultiPartVariant(Locale localeWithMultiPartVariant) {
      this.localeWithMultiPartVariant = localeWithMultiPartVariant;
    }

    public Pattern getPattern() {
      return pattern;
    }

    public void setPattern(Pattern pattern) {
      this.pattern = pattern;
    }

    public TimeZone getTimeZone() {
      return timeZone;
    }

    public void setTimeZone(TimeZone timeZone) {
      this.timeZone = timeZone;
    }

    public TimeZone getTimeZoneGmtOffset() {
      return timeZoneGmtOffset;
    }

    public void setTimeZoneGmtOffset(TimeZone timeZoneGmtOffset) {
      this.timeZoneGmtOffset = timeZoneGmtOffset;
    }

    public URI getUri() {
      return uri;
    }

    public void setUri(URI uri) {
      this.uri = uri;
    }

    public URI getUriMailto() {
      return uriMailto;
    }

    public void setUriMailto(URI uriMailto) {
      this.uriMailto = uriMailto;
    }

    public URI getUriCustomScheme() {
      return uriCustomScheme;
    }

    public void setUriCustomScheme(URI uriCustomScheme) {
      this.uriCustomScheme = uriCustomScheme;
    }

    public URI getUriRelative() {
      return uriRelative;
    }

    public void setUriRelative(URI uriRelative) {
      this.uriRelative = uriRelative;
    }

    public URI getUriClasspath() {
      return uriClasspath;
    }

    public void setUriClasspath(URI uriClasspath) {
      this.uriClasspath = uriClasspath;
    }

    public URI getUriWithFragment() {
      return uriWithFragment;
    }

    public void setUriWithFragment(URI uriWithFragment) {
      this.uriWithFragment = uriWithFragment;
    }

    public URI getUriWithEncodedFragment() {
      return uriWithEncodedFragment;
    }

    public void setUriWithEncodedFragment(URI uriWithEncodedFragment) {
      this.uriWithEncodedFragment = uriWithEncodedFragment;
    }

    public URI getUriWithNonAscii() {
      return uriWithNonAscii;
    }

    public void setUriWithNonAscii(URI uriWithNonAscii) {
      this.uriWithNonAscii = uriWithNonAscii;
    }

    public URI getUriAlreadyEncoded() {
      return uriAlreadyEncoded;
    }

    public void setUriAlreadyEncoded(URI uriAlreadyEncoded) {
      this.uriAlreadyEncoded = uriAlreadyEncoded;
    }

    public URL getUrl() {
      return url;
    }

    public void setUrl(URL url) {
      this.url = url;
    }

    public URL getUrlMailto() {
      return urlMailto;
    }

    public void setUrlMailto(URL urlMailto) {
      this.urlMailto = urlMailto;
    }

    public URL getUrlClasspath() {
      return urlClasspath;
    }

    public void setUrlClasspath(URL urlClasspath) {
      this.urlClasspath = urlClasspath;
    }

    public URL getUrlUnknownProtocol() {
      return urlUnknownProtocol;
    }

    public void setUrlUnknownProtocol(URL urlUnknownProtocol) {
      this.urlUnknownProtocol = urlUnknownProtocol;
    }

    public UUID getUuid() {
      return uuid;
    }

    public void setUuid(UUID uuid) {
      this.uuid = uuid;
    }

    public ZoneId getZoneId() {
      return zoneId;
    }

    public void setZoneId(ZoneId zoneId) {
      this.zoneId = zoneId;
    }

    public ZoneId getZoneIdOffset() {
      return zoneIdOffset;
    }

    public void setZoneIdOffset(ZoneId zoneIdOffset) {
      this.zoneIdOffset = zoneIdOffset;
    }

    public Optional<String> getOptionalValue() {
      return optionalValue;
    }

    public void setOptionalValue(Optional<String> optionalValue) {
      this.optionalValue = optionalValue;
    }

    public Date getDateValue() {
      return dateValue;
    }

    public void setDateValue(Date dateValue) {
      this.dateValue = dateValue;
    }

    public Object getObjectValue() {
      return objectValue;
    }

    public void setObjectValue(Object objectValue) {
      this.objectValue = objectValue;
    }

    public StaticValue getStaticValue() {
      return staticValue;
    }

    public void setStaticValue(StaticValue staticValue) {
      this.staticValue = staticValue;
    }

    public StaticValue getStaticValueWithWrongFieldType() {
      return staticValueWithWrongFieldType;
    }

    public void setStaticValueWithWrongFieldType(StaticValue staticValueWithWrongFieldType) {
      this.staticValueWithWrongFieldType = staticValueWithWrongFieldType;
    }

    public SampleEnum getSampleEnum() {
      return sampleEnum;
    }

    public void setSampleEnum(SampleEnum sampleEnum) {
      this.sampleEnum = sampleEnum;
    }

    public SampleEnum getSampleEnumAlias() {
      return sampleEnumAlias;
    }

    public void setSampleEnumAlias(SampleEnum sampleEnumAlias) {
      this.sampleEnumAlias = sampleEnumAlias;
    }

    public String getStringValue() {
      return stringValue;
    }

    public void setStringValue(String stringValue) {
      this.stringValue = stringValue;
    }

    public SampleEnum[] getSampleEnumArray() {
      return sampleEnumArray;
    }

    public void setSampleEnumArray(SampleEnum[] sampleEnumArray) {
      this.sampleEnumArray = sampleEnumArray;
    }

    public List<SampleEnum> getSampleEnumList() {
      return sampleEnumList;
    }

    public void setSampleEnumList(List<SampleEnum> sampleEnumList) {
      this.sampleEnumList = sampleEnumList;
    }

    public Map<SampleEnum, SampleEnum> getSampleEnumMap() {
      return sampleEnumMap;
    }

    public void setSampleEnumMap(Map<SampleEnum, SampleEnum> sampleEnumMap) {
      this.sampleEnumMap = sampleEnumMap;
    }

    public String[] getStringArray() {
      return stringArray;
    }

    public void setStringArray(String[] stringArray) {
      this.stringArray = stringArray;
    }

    public Character[] getCharacterArray() {
      return characterArray;
    }

    public void setCharacterArray(Character[] characterArray) {
      this.characterArray = characterArray;
    }

    public Boolean[] getBooleanArray() {
      return booleanArray;
    }

    public void setBooleanArray(Boolean[] booleanArray) {
      this.booleanArray = booleanArray;
    }

    public Byte[] getByteArrayElements() {
      return byteArrayElements;
    }

    public void setByteArrayElements(Byte[] byteArrayElements) {
      this.byteArrayElements = byteArrayElements;
    }

    public byte[] getPrimitiveByteArrayElements() {
      return primitiveByteArrayElements;
    }

    public void setPrimitiveByteArrayElements(byte[] primitiveByteArrayElements) {
      this.primitiveByteArrayElements = primitiveByteArrayElements;
    }

    public Short[] getShortArray() {
      return shortArray;
    }

    public void setShortArray(Short[] shortArray) {
      this.shortArray = shortArray;
    }

    public short[] getPrimitiveShortArray() {
      return primitiveShortArray;
    }

    public void setPrimitiveShortArray(short[] primitiveShortArray) {
      this.primitiveShortArray = primitiveShortArray;
    }

    public Integer[] getIntegerArray() {
      return integerArray;
    }

    public void setIntegerArray(Integer[] integerArray) {
      this.integerArray = integerArray;
    }

    public int[] getPrimitiveIntArray() {
      return primitiveIntArray;
    }

    public void setPrimitiveIntArray(int[] primitiveIntArray) {
      this.primitiveIntArray = primitiveIntArray;
    }

    public Long[] getLongArray() {
      return longArray;
    }

    public void setLongArray(Long[] longArray) {
      this.longArray = longArray;
    }

    public long[] getPrimitiveLongArray() {
      return primitiveLongArray;
    }

    public void setPrimitiveLongArray(long[] primitiveLongArray) {
      this.primitiveLongArray = primitiveLongArray;
    }

    public Float[] getFloatArray() {
      return floatArray;
    }

    public void setFloatArray(Float[] floatArray) {
      this.floatArray = floatArray;
    }

    public float[] getPrimitiveFloatArray() {
      return primitiveFloatArray;
    }

    public void setPrimitiveFloatArray(float[] primitiveFloatArray) {
      this.primitiveFloatArray = primitiveFloatArray;
    }

    public Double[] getDoubleArray() {
      return doubleArray;
    }

    public void setDoubleArray(Double[] doubleArray) {
      this.doubleArray = doubleArray;
    }

    public double[] getPrimitiveDoubleArray() {
      return primitiveDoubleArray;
    }

    public void setPrimitiveDoubleArray(double[] primitiveDoubleArray) {
      this.primitiveDoubleArray = primitiveDoubleArray;
    }

    public BigDecimal[] getBigDecimalArray() {
      return bigDecimalArray;
    }

    public void setBigDecimalArray(BigDecimal[] bigDecimalArray) {
      this.bigDecimalArray = bigDecimalArray;
    }

    public BigInteger[] getBigIntegerArray() {
      return bigIntegerArray;
    }

    public void setBigIntegerArray(BigInteger[] bigIntegerArray) {
      this.bigIntegerArray = bigIntegerArray;
    }

    public Charset[] getCharsetArray() {
      return charsetArray;
    }

    public void setCharsetArray(Charset[] charsetArray) {
      this.charsetArray = charsetArray;
    }

    public Currency[] getCurrencyArray() {
      return currencyArray;
    }

    public void setCurrencyArray(Currency[] currencyArray) {
      this.currencyArray = currencyArray;
    }

    public Locale[] getLocaleArray() {
      return localeArray;
    }

    public void setLocaleArray(Locale[] localeArray) {
      this.localeArray = localeArray;
    }

    public Pattern[] getPatternArray() {
      return patternArray;
    }

    public void setPatternArray(Pattern[] patternArray) {
      this.patternArray = patternArray;
    }

    public TimeZone[] getTimeZoneArray() {
      return timeZoneArray;
    }

    public void setTimeZoneArray(TimeZone[] timeZoneArray) {
      this.timeZoneArray = timeZoneArray;
    }

    public URI[] getUriArray() {
      return uriArray;
    }

    public void setUriArray(URI[] uriArray) {
      this.uriArray = uriArray;
    }

    public URL[] getUrlArray() {
      return urlArray;
    }

    public void setUrlArray(URL[] urlArray) {
      this.urlArray = urlArray;
    }

    public UUID[] getUuidArray() {
      return uuidArray;
    }

    public void setUuidArray(UUID[] uuidArray) {
      this.uuidArray = uuidArray;
    }

    public ZoneId[] getZoneIdArray() {
      return zoneIdArray;
    }

    public void setZoneIdArray(ZoneId[] zoneIdArray) {
      this.zoneIdArray = zoneIdArray;
    }

    public Collection<String> getStringCollection() {
      return stringCollection;
    }

    public void setStringCollection(Collection<String> stringCollection) {
      this.stringCollection = stringCollection;
    }

    public List<String> getStringList() {
      return stringList;
    }

    public void setStringList(List<String> stringList) {
      this.stringList = stringList;
    }

    public List<Integer> getIntegerList() {
      return integerList;
    }

    public void setIntegerList(List<Integer> integerList) {
      this.integerList = integerList;
    }

    public Set<String> getStringSet() {
      return stringSet;
    }

    public void setStringSet(Set<String> stringSet) {
      this.stringSet = stringSet;
    }

    public SortedSet<String> getSortedStringSet() {
      return sortedStringSet;
    }

    public void setSortedStringSet(SortedSet<String> sortedStringSet) {
      this.sortedStringSet = sortedStringSet;
    }

    public ArrayList<String> getStringArrayList() {
      return stringArrayList;
    }

    public void setStringArrayList(ArrayList<String> stringArrayList) {
      this.stringArrayList = stringArrayList;
    }

    public Collection getRawCollection() {
      return rawCollection;
    }

    public void setRawCollection(Collection rawCollection) {
      this.rawCollection = rawCollection;
    }

    public Map<String, String> getStringStringMap() {
      return stringStringMap;
    }

    public void setStringStringMap(Map<String, String> stringStringMap) {
      this.stringStringMap = stringStringMap;
    }

    public Map<String, Integer> getStringIntegerMap() {
      return stringIntegerMap;
    }

    public void setStringIntegerMap(Map<String, Integer> stringIntegerMap) {
      this.stringIntegerMap = stringIntegerMap;
    }

    public SortedMap<String, Integer> getSortedStringIntegerMap() {
      return sortedStringIntegerMap;
    }

    public void setSortedStringIntegerMap(SortedMap<String, Integer> sortedStringIntegerMap) {
      this.sortedStringIntegerMap = sortedStringIntegerMap;
    }

    public TreeMap<String, Integer> getStringIntegerTreeMap() {
      return stringIntegerTreeMap;
    }

    public void setStringIntegerTreeMap(TreeMap<String, Integer> stringIntegerTreeMap) {
      this.stringIntegerTreeMap = stringIntegerTreeMap;
    }

    public LinkedHashMap<String, Integer> getStringIntegerLinkedHashMap() {
      return stringIntegerLinkedHashMap;
    }

    public void setStringIntegerLinkedHashMap(
        LinkedHashMap<String, Integer> stringIntegerLinkedHashMap) {
      this.stringIntegerLinkedHashMap = stringIntegerLinkedHashMap;
    }

    public Map getRawMap() {
      return rawMap;
    }

    public void setRawMap(Map rawMap) {
      this.rawMap = rawMap;
    }

    public Collection<UUID> getUuidCollection() {
      return uuidCollection;
    }

    public void setUuidCollection(Collection<UUID> uuidCollection) {
      this.uuidCollection = uuidCollection;
    }

    public List<UUID> getUuidList() {
      return uuidList;
    }

    public void setUuidList(List<UUID> uuidList) {
      this.uuidList = uuidList;
    }

    public Set<UUID> getUuidSet() {
      return uuidSet;
    }

    public void setUuidSet(Set<UUID> uuidSet) {
      this.uuidSet = uuidSet;
    }

    public SortedSet<UUID> getSortedUuidSet() {
      return sortedUuidSet;
    }

    public void setSortedUuidSet(SortedSet<UUID> sortedUuidSet) {
      this.sortedUuidSet = sortedUuidSet;
    }

    public ArrayList<UUID> getUuidArrayList() {
      return uuidArrayList;
    }

    public void setUuidArrayList(ArrayList<UUID> uuidArrayList) {
      this.uuidArrayList = uuidArrayList;
    }

    public Map<UUID, Integer> getUuidIntegerMap() {
      return uuidIntegerMap;
    }

    public void setUuidIntegerMap(Map<UUID, Integer> uuidIntegerMap) {
      this.uuidIntegerMap = uuidIntegerMap;
    }

    public SortedMap<UUID, Integer> getSortedUuidIntegerMap() {
      return sortedUuidIntegerMap;
    }

    public void setSortedUuidIntegerMap(SortedMap<UUID, Integer> sortedUuidIntegerMap) {
      this.sortedUuidIntegerMap = sortedUuidIntegerMap;
    }

    public TreeMap<UUID, Integer> getUuidIntegerTreeMap() {
      return uuidIntegerTreeMap;
    }

    public void setUuidIntegerTreeMap(TreeMap<UUID, Integer> uuidIntegerTreeMap) {
      this.uuidIntegerTreeMap = uuidIntegerTreeMap;
    }

    public LinkedHashMap<UUID, Integer> getUuidIntegerLinkedHashMap() {
      return uuidIntegerLinkedHashMap;
    }

    public void setUuidIntegerLinkedHashMap(LinkedHashMap<UUID, Integer> uuidIntegerLinkedHashMap) {
      this.uuidIntegerLinkedHashMap = uuidIntegerLinkedHashMap;
    }
  }

  public enum SampleEnum {
    FIRST,
    SECOND,
    THIRD;

    public static final SampleEnum ALIAS = SECOND;
  }

  public static class StaticValue {
    public static final StaticValue ALPHA = new StaticValue("alpha");
    public static final String WRONG_FIELD_TYPE = "wrong";

    private final String value;

    private StaticValue(String value) {
      this.value = value;
    }

    public String value() {
      return value;
    }
  }

  public static class IndexedFormData {

    private IndexedValue[] array;
    private List<IndexedValue> list;
    private Map<String, IndexedValue> map;

    public IndexedValue[] getArray() {
      return array;
    }

    public void setArray(IndexedValue[] array) {
      this.array = array;
    }

    public List<IndexedValue> getList() {
      return list;
    }

    public void setList(List<IndexedValue> list) {
      this.list = list;
    }

    public Map<String, IndexedValue> getMap() {
      return map;
    }

    public void setMap(Map<String, IndexedValue> map) {
      this.map = map;
    }
  }

  public static class IndexedValue {

    private String name;
    private IndexedFormData nested;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public IndexedFormData getNested() {
      return nested;
    }

    public void setNested(IndexedFormData nested) {
      this.nested = nested;
    }
  }

  public static class ValidatedFormData {

    private Integer integerValue;
    private String stringValue;

    @Constraints.Required private String requiredValue;

    public Integer getIntegerValue() {
      return integerValue;
    }

    public void setIntegerValue(Integer integerValue) {
      this.integerValue = integerValue;
    }

    public String getStringValue() {
      return stringValue;
    }

    public void setStringValue(String stringValue) {
      this.stringValue = stringValue;
    }

    public String getRequiredValue() {
      return requiredValue;
    }

    public void setRequiredValue(String requiredValue) {
      this.requiredValue = requiredValue;
    }
  }

  public static class FileBindingFormData {

    private List<IndexedValue> list;
    private FilePart<?> upload;

    public List<IndexedValue> getList() {
      return list;
    }

    public void setList(List<IndexedValue> list) {
      this.list = list;
    }

    public FilePart<?> getUpload() {
      return upload;
    }

    public void setUpload(FilePart<?> upload) {
      this.upload = upload;
    }
  }
}
