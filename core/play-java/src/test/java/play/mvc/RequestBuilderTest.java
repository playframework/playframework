/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.apache.pekko.stream.javadsl.FileIO;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.util.ByteString;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import play.api.Application;
import play.api.Play;
import play.api.inject.guice.GuiceApplicationBuilder;
import play.i18n.Lang;
import play.i18n.Messages;
import play.libs.Files;
import play.libs.Files.TemporaryFileCreator;
import play.libs.typedmap.TypedKey;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBuilder;
import play.test.Helpers;

public class RequestBuilderTest {

  @Test
  public void testUri_absolute() {
    Request request = new RequestBuilder().uri("https://www.benmccann.com/blog").build();
    assertEquals("https://www.benmccann.com/blog", request.uri());
  }

  @Test
  public void testUri_relative() {
    Request request = new RequestBuilder().uri("/blog").build();
    assertEquals("/blog", request.uri());
  }

  @Test
  public void testUri_asterisk() {
    Request request = new RequestBuilder().method("OPTIONS").uri("*").build();
    assertEquals("*", request.uri());
  }

  @Test
  public void testSecure() {
    assertFalse(new RequestBuilder().uri("http://www.benmccann.com/blog").build().secure());
    assertTrue(new RequestBuilder().uri("https://www.benmccann.com/blog").build().secure());
  }

  @Test
  public void testAttrs() {
    final TypedKey<Long> NUMBER = TypedKey.create("number");
    final TypedKey<String> COLOR = TypedKey.create("color");

    RequestBuilder builder = new RequestBuilder().uri("http://www.playframework.com/");
    assertFalse(builder.attrs().containsKey(NUMBER));
    assertFalse(builder.attrs().containsKey(COLOR));

    Request req1 = builder.build();

    builder.attr(NUMBER, 6L);
    assertTrue(builder.attrs().containsKey(NUMBER));
    assertFalse(builder.attrs().containsKey(COLOR));

    Request req2 = builder.build();

    builder.attr(NUMBER, 70L);
    assertTrue(builder.attrs().containsKey(NUMBER));
    assertFalse(builder.attrs().containsKey(COLOR));

    Request req3 = builder.build();

    builder.attrs(builder.attrs().putAll(NUMBER.bindValue(6L), COLOR.bindValue("blue")));
    assertTrue(builder.attrs().containsKey(NUMBER));
    assertTrue(builder.attrs().containsKey(COLOR));

    Request req4 = builder.build();

    builder.attrs(builder.attrs().putAll(COLOR.bindValue("red")));
    assertTrue(builder.attrs().containsKey(NUMBER));
    assertTrue(builder.attrs().containsKey(COLOR));

    Request req5 = builder.build();

    assertFalse(req1.attrs().containsKey(NUMBER));
    assertFalse(req1.attrs().containsKey(COLOR));

    assertEquals(Optional.of(6L), req2.attrs().getOptional(NUMBER));
    assertEquals((Long) 6L, req2.attrs().get(NUMBER));
    assertFalse(req2.attrs().containsKey(COLOR));

    assertEquals(Optional.of(70L), req3.attrs().getOptional(NUMBER));
    assertEquals((Long) 70L, req3.attrs().get(NUMBER));
    assertFalse(req3.attrs().containsKey(COLOR));

    assertEquals(Optional.of(6L), req4.attrs().getOptional(NUMBER));
    assertEquals((Long) 6L, req4.attrs().get(NUMBER));
    assertEquals(Optional.of("blue"), req4.attrs().getOptional(COLOR));
    assertEquals("blue", req4.attrs().get(COLOR));

    assertEquals(Optional.of(6L), req5.attrs().getOptional(NUMBER));
    assertEquals((Long) 6L, req5.attrs().get(NUMBER));
    assertEquals(Optional.of("red"), req5.attrs().getOptional(COLOR));
    assertEquals("red", req5.attrs().get(COLOR));

    Request req6 = req4.removeAttr(COLOR).removeAttr(NUMBER);

    assertFalse(req6.attrs().containsKey(NUMBER));
    assertFalse(req6.attrs().containsKey(COLOR));

    Request req7 = req4.removeAttr(COLOR);

    assertEquals(Optional.of(6L), req7.attrs().getOptional(NUMBER));
    assertEquals((Long) 6L, req7.attrs().get(NUMBER));
    assertFalse(req7.attrs().containsKey(COLOR));
  }

  @Test
  public void testNewRequestsShouldNotHaveATransientLang() {
    RequestBuilder builder = new RequestBuilder().uri("http://www.playframework.com/");

    Request request = builder.build();
    assertFalse(request.transientLang().isPresent());
    assertFalse(request.attrs().getOptional(Messages.Attrs.CurrentLang).isPresent());
  }

  @Test
  public void testAddATransientLangToRequest() {
    RequestBuilder builder = new RequestBuilder().uri("http://www.playframework.com/");

    Lang lang = new Lang(Locale.GERMAN);
    Request request = builder.build().withTransientLang(lang);

    assertTrue(request.transientLang().isPresent());
    assertEquals(lang, request.attrs().get(Messages.Attrs.CurrentLang));
  }

  @Test
  public void testAddATransientLangByCodeToRequest() {
    RequestBuilder builder = new RequestBuilder().uri("http://www.playframework.com/");

    String lang = "de";
    Request request = builder.build().withTransientLang(Lang.forCode(lang));

    assertTrue(request.transientLang().isPresent());
    assertEquals(Lang.forCode(lang), request.attrs().get(Messages.Attrs.CurrentLang));
  }

  @Test
  public void testAddATransientLangByLocaleToRequest() {
    RequestBuilder builder = new RequestBuilder().uri("http://www.playframework.com/");

    Locale locale = Locale.GERMAN;
    Request request = builder.build().withTransientLang(locale);

    assertTrue(request.transientLang().isPresent());
    assertEquals(new Lang(locale), request.attrs().get(Messages.Attrs.CurrentLang));
  }

  @Test
  public void testClearRequestTransientLang() {
    RequestBuilder builder = new RequestBuilder().uri("http://www.playframework.com/");

    Lang lang = new Lang(Locale.GERMAN);
    Request request = builder.build().withTransientLang(lang);
    assertTrue(request.transientLang().isPresent());

    // Language attr should be removed
    assertFalse(request.withoutTransientLang().transientLang().isPresent());
  }

  @Test
  public void testAddATransientLangToRequestBuilder() {
    RequestBuilder builder = new RequestBuilder().uri("http://www.playframework.com/");

    Lang lang = new Lang(Locale.GERMAN);
    Request request = builder.transientLang(lang).build();

    assertTrue(request.transientLang().isPresent());
    assertEquals(lang, request.attrs().get(Messages.Attrs.CurrentLang));
  }

  @Test
  public void testAddATransientLangByCodeToRequestBuilder() {
    RequestBuilder builder = new RequestBuilder().uri("http://www.playframework.com/");

    String lang = "de";
    Request request = builder.transientLang(Lang.forCode(lang)).build();

    assertTrue(request.transientLang().isPresent());
    assertEquals(Lang.forCode(lang), request.attrs().get(Messages.Attrs.CurrentLang));
  }

  @Test
  public void testAddATransientLangByLocaleToRequestBuilder() {
    RequestBuilder builder = new RequestBuilder().uri("http://www.playframework.com/");

    Locale locale = Locale.GERMAN;
    Request request = builder.transientLang(locale).build();

    assertTrue(request.transientLang().isPresent());
    assertEquals(new Lang(locale), request.attrs().get(Messages.Attrs.CurrentLang));
  }

  @Test
  public void testClearRequestBuilderTransientLang() {
    Lang lang = new Lang(Locale.GERMAN);
    RequestBuilder builder =
        new RequestBuilder().uri("http://www.playframework.com/").transientLang(lang);

    assertTrue(builder.build().transientLang().isPresent());
    assertEquals(Optional.of(lang), builder.transientLang());

    // Language attr should be removed
    assertFalse(builder.withoutTransientLang().build().transientLang().isPresent());
  }

  @Test
  public void testNewRequestsShouldNotHaveALangCookie() {
    RequestBuilder builder = new RequestBuilder().uri("http://www.playframework.com/");

    Request request = builder.build();
    assertFalse(request.cookie(Helpers.stubMessagesApi().langCookieName()).isPresent());
    assertFalse(request.transientLang().isPresent());
    assertFalse(request.attrs().getOptional(Messages.Attrs.CurrentLang).isPresent());
  }

  @Test
  public void testAddALangCookieToRequestBuilder() {
    RequestBuilder builder = new RequestBuilder().uri("http://www.playframework.com/");

    Lang lang = new Lang(Locale.GERMAN);
    Request request = builder.langCookie(lang, Helpers.stubMessagesApi()).build();

    assertEquals(
        Optional.of(lang.code()),
        request.cookie(Helpers.stubMessagesApi().langCookieName()).map(Http.Cookie::value));
    assertFalse(request.transientLang().isPresent());
    assertFalse(request.attrs().getOptional(Messages.Attrs.CurrentLang).isPresent());
  }

  @Test
  public void testAddALangCookieByLocaleToRequestBuilder() {
    RequestBuilder builder = new RequestBuilder().uri("http://www.playframework.com/");

    Locale locale = Locale.GERMAN;
    Request request = builder.langCookie(locale, Helpers.stubMessagesApi()).build();

    assertEquals(
        Optional.of(locale.toLanguageTag()),
        request.cookie(Helpers.stubMessagesApi().langCookieName()).map(Http.Cookie::value));
    assertFalse(request.transientLang().isPresent());
    assertFalse(request.attrs().getOptional(Messages.Attrs.CurrentLang).isPresent());
  }

  @Test
  public void testFlash() {
    final Request req =
        new RequestBuilder().flash("a", "1").flash("b", "1").flash("b", "2").build();
    assertEquals(Optional.of("1"), req.flash().get("a"));
    assertEquals(Optional.of("2"), req.flash().get("b"));
  }

  @Test
  public void testSession() {
    final Request req =
        new RequestBuilder().session("a", "1").session("b", "1").session("b", "2").build();
    assertEquals(Optional.of("1"), req.session().get("a"));
    assertEquals(Optional.of("2"), req.session().get("b"));
  }

  @Test
  public void testUsername() {
    final Request req1 = new RequestBuilder().uri("http://playframework.com/").build();
    final Request req2 = req1.addAttr(Security.USERNAME, "user2");
    final Request req3 = req1.addAttr(Security.USERNAME, "user3");
    final Request req4 =
        new RequestBuilder()
            .uri("http://playframework.com/")
            .attr(Security.USERNAME, "user4")
            .build();

    assertFalse(req1.attrs().containsKey(Security.USERNAME));

    assertTrue(req2.attrs().containsKey(Security.USERNAME));
    assertEquals("user2", req2.attrs().get(Security.USERNAME));

    assertTrue(req3.attrs().containsKey(Security.USERNAME));
    assertEquals("user3", req3.attrs().get(Security.USERNAME));

    assertTrue(req4.attrs().containsKey(Security.USERNAME));
    assertEquals("user4", req4.attrs().get(Security.USERNAME));
  }

  @Test
  public void testGetQuery_doubleEncoding() {
    final Optional<String> query =
        new Http.RequestBuilder().uri("path?query=x%2By").build().queryString("query");
    assertEquals(Optional.of("x+y"), query);
  }

  @Test
  public void testQuery_doubleEncoding() {
    final Optional<String> query =
        new Http.RequestBuilder().uri("path?query=x%2By").build().queryString("query");
    assertEquals(Optional.of("x+y"), query);
  }

  @Test
  public void testGetQuery_multipleParams() {
    final Request req = new Http.RequestBuilder().uri("/path?one=1&two=a+b&").build();
    assertEquals(Optional.of("1"), req.queryString("one"));
    assertEquals(Optional.of("a b"), req.queryString("two"));
  }

  @Test
  public void testQuery_multipleParams() {
    final Request req = new Http.RequestBuilder().uri("/path?one=1&two=a+b&").build();
    assertEquals(Optional.of("1"), req.queryString("one"));
    assertEquals(Optional.of("a b"), req.queryString("two"));
  }

  @Test
  public void testGetQuery_emptyParam() {
    final Request req = new Http.RequestBuilder().uri("/path?one=&two=a+b&").build();
    assertEquals(Optional.empty(), req.queryString("one"));
    assertEquals(Optional.of("a b"), req.queryString("two"));
  }

  @Test
  public void testQuery_emptyParam() {
    final Request req = new Http.RequestBuilder().uri("/path?one=&two=a+b&").build();
    assertEquals(Optional.empty(), req.queryString("one"));
    assertEquals(Optional.of("a b"), req.queryString("two"));
  }

  @Test
  public void testGetUri_badEncoding() {
    final Request req =
        new Http.RequestBuilder().uri("/test.html?one=hello=world&two=false").build();
    assertEquals(Optional.of("hello=world"), req.queryString("one"));
    assertEquals(Optional.of("false"), req.queryString("two"));
  }

  @Test
  public void testUri_badEncoding() {
    final Request req =
        new Http.RequestBuilder().uri("/test.html?one=hello=world&two=false").build();
    assertEquals(Optional.of("hello=world"), req.queryString("one"));
    assertEquals(Optional.of("false"), req.queryString("two"));
  }

  @Test
  public void multipartForm() throws ExecutionException, InterruptedException {
    Application app = new GuiceApplicationBuilder().build();
    Play.start(app);
    TemporaryFileCreator temporaryFileCreator =
        app.injector().instanceOf(TemporaryFileCreator.class);
    Http.MultipartFormData.DataPart dp = new Http.MultipartFormData.DataPart("hello", "world");
    final Request request =
        new RequestBuilder()
            .uri("http://playframework.com/")
            .bodyRaw(Collections.singletonList(dp), temporaryFileCreator, app.materializer())
            .build();

    Optional<Http.MultipartFormData<Files.TemporaryFile>> parts =
        app.injector()
            .instanceOf(BodyParser.MultipartFormData.class)
            .apply(request)
            .run(Source.single(request.body().asBytes()), app.materializer())
            .toCompletableFuture()
            .get()
            .right;
    assertTrue(parts.isPresent());
    assertArrayEquals(new String[] {"world"}, parts.get().asFormUrlEncoded().get("hello"));

    Play.stop(app);
  }

  @Test
  public void multipartForm_bodyRaw_correctEscapedParams() throws URISyntaxException, IOException {
    Application app = new GuiceApplicationBuilder().build();
    Play.start(app);

    File file = new File(this.getClass().getResource("/testassets/foo.txt").toURI());
    Http.MultipartFormData.Part<Source<ByteString, ?>> filePart =
        new Http.MultipartFormData.FilePart<>(
            "f\"i\rl\nef\"ie\nld\r1",
            "f\rir\"s\ntf\ril\"e\n.txt",
            "text/plain",
            FileIO.fromPath(file.toPath()),
            java.nio.file.Files.size(file.toPath()));

    Http.MultipartFormData.DataPart dataPart =
        new Http.MultipartFormData.DataPart("f\ni\re\"l\nd1", "value1");

    TemporaryFileCreator temporaryFileCreator =
        app.injector().instanceOf(TemporaryFileCreator.class);
    final Request request =
        new RequestBuilder()
            .uri("http://playframework.com/")
            // bodyRaw, as its name tells us, saves the body in raw bytes.
            // To do that it needs to render the body, so bodyRaw(...) goes through
            // play.mvc.MultipartFormatter#transform(...), so eventually
            // play.core.formatters.Multipart, which renders the multipart/form-data elements and
            // escapes params, will be used
            .bodyRaw(List.of(dataPart, filePart), temporaryFileCreator, app.materializer())
            .build();

    String body =
        request.body().asBytes().utf8String(); // Let's get the text representation of the bytes
    assertThat(
        body,
        CoreMatchers.containsString("Content-Disposition: form-data; name=\"f%0Ai%0De%22l%0Ad1\""));
    assertThat(
        body,
        CoreMatchers.containsString(
            "Content-Disposition: form-data; name=\"f%22i%0Dl%0Aef%22ie%0Ald%0D1\"; filename=\"f%0Dir%22s%0Atf%0Dil%22e%0A.txt\""));

    Play.stop(app);
  }

  @Test
  public void multipartFormContentLength() {
    final Map<String, String[]> dataParts = new HashMap<>();
    dataParts.put("f\ni\re\"l\nd1", new String[] {"value1"});
    dataParts.put("field2", new String[] {"value2-1", "value2.2"});

    final List<Http.MultipartFormData.FilePart> fileParts = new ArrayList<>();
    fileParts.add(
        new Http.MultipartFormData.FilePart<>(
            "f\"i\rl\nef\"ie\nld\r1", "f\rir\"s\ntf\ril\"e\n.txt", "text/plain", "abc", 3));
    fileParts.add(
        new Http.MultipartFormData.FilePart<>(
            "file_field_2", "secondfile.txt", "text/plain", "hello world", 11));

    final Request request =
        new RequestBuilder()
            .uri("http://playframework.com/")
            .bodyMultipart(dataParts, fileParts)
            .build();

    assertNotNull(request.body().asMultipartFormData());
    assertEquals(dataParts, request.body().asMultipartFormData().asFormUrlEncoded());
    assertEquals(fileParts, request.body().asMultipartFormData().getFiles());

    // Now let's check the calculated Content-Length. The request body should look like this when
    // stringified:
    // (You can copy the lines, save it with an editor with UTF-8 encoding and Windows line endings
    // (\r\n) and the file size should be 590 bytes
    /*
    --somerandomboundary
    Content-Disposition: form-data; name="f%0Ai%0De%22l%0Ad1"

    value1
    --somerandomboundary
    Content-Disposition: form-data; name="field2[]"

    value2-1
    --somerandomboundary
    Content-Disposition: form-data; name="field2[]"

    value2.2
    --somerandomboundary
    Content-Disposition: form-data; name="f%22i%0Dl%0Aef%22ie%0Ald%0D1"; filename="f%0Dir%22s%0Atf%0Dil%22e%0A.txt"
    Content-Type: text/plain

    abc
    --somerandomboundary
    Content-Disposition: form-data; name="file_field_2"; filename="secondfile.txt"
    Content-Type: text/plain

    hello world
    --somerandomboundary--
    */
    assertEquals(request.header(Http.HeaderNames.CONTENT_LENGTH).get(), "590");
  }
}
