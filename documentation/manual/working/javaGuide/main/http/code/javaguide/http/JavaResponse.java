/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.http;

import static javaguide.testhelpers.MockJavaActionHelper.*;
import static org.junit.jupiter.api.Assertions.*;
import static play.mvc.Controller.*;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;

import akka.NotUsed;
import akka.stream.Materializer;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javaguide.testhelpers.MockJavaAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import play.Application;
import play.core.j.JavaHandlerComponents;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Http.Cookie;
import play.mvc.Http.MimeTypes;
import play.mvc.RangeResults;
import play.mvc.Result;
import play.test.Helpers;
import play.test.junit5.ApplicationExtension;

public class JavaResponse {

  @RegisterExtension
  static ApplicationExtension appExtension = new ApplicationExtension(fakeApplication());

  static Application app = appExtension.getApplication();
  static Materializer mat = appExtension.getMaterializer();

  @Test
  void textContentType() {
    // #text-content-type
    Result textResult = ok("Hello World!");
    // #text-content-type

    assertTrue(textResult.contentType().get().contains("text/plain"));
  }

  @Test
  void jsonContentType() {
    String object = "";
    // #json-content-type
    JsonNode json = Json.toJson(object);
    Result jsonResult = ok(json);
    // #json-content-type

    assertTrue(jsonResult.contentType().get().contains("application/json"));
  }

  @Test
  void customContentType() {
    // #custom-content-type
    Result htmlResult = ok("<h1>Hello World!</h1>").as("text/html");
    // #custom-content-type

    assertTrue(htmlResult.contentType().get().contains("text/html"));
  }

  @Test
  void customDefiningContentType() {
    // #content-type_defined_html
    Result htmlResult = ok("<h1>Hello World!</h1>").as(MimeTypes.HTML);
    // #content-type_defined_html

    assertTrue(htmlResult.contentType().get().contains("text/html"));
  }

  @Test
  void responseHeaders() {
    Map<String, String> headers =
        call(
                new MockJavaAction(app.injector().instanceOf(JavaHandlerComponents.class)) {
                  // #response-headers
                  public Result index() {
                    return ok("<h1>Hello World!</h1>")
                        .as(MimeTypes.HTML)
                        .withHeader(CACHE_CONTROL, "max-age=3600")
                        .withHeader(ETAG, "some-etag-calculated-value");
                  }
                  // #response-headers
                },
                fakeRequest(),
                mat)
            .headers();
    assertEquals("max-age=3600", headers.get(CACHE_CONTROL));
    assertEquals("some-etag-calculated-value", headers.get(ETAG));
  }

  @Test
  void setCookie() {
    Http.Cookies cookies =
        call(
                new MockJavaAction(app.injector().instanceOf(JavaHandlerComponents.class)) {
                  // #set-cookie
                  public Result index() {
                    return ok("<h1>Hello World!</h1>")
                        .as(MimeTypes.HTML)
                        .withCookies(Cookie.builder("theme", "blue").build());
                  }
                  // #set-cookie
                },
                fakeRequest(),
                mat)
            .cookies();

    Optional<Cookie> cookie = cookies.get("theme");
    assertTrue(cookie.isPresent());
    assertEquals("blue", cookie.get().value());
  }

  @Test
  void detailedSetCookie() {
    Http.Cookies cookies =
        call(
                new MockJavaAction(app.injector().instanceOf(JavaHandlerComponents.class)) {
                  // #detailed-set-cookie
                  public Result index() {
                    return ok("<h1>Hello World!</h1>")
                        .as(MimeTypes.HTML)
                        .withCookies(
                            Cookie.builder("theme", "blue")
                                .withMaxAge(Duration.ofSeconds(3600))
                                .withPath("/some/path")
                                .withDomain(".example.com")
                                .withSecure(false)
                                .withHttpOnly(true)
                                .withSameSite(Cookie.SameSite.STRICT)
                                .build());
                  }
                  // #detailed-set-cookie
                },
                fakeRequest(),
                mat)
            .cookies();
    Optional<Cookie> cookieOpt = cookies.get("theme");

    assertTrue(cookieOpt.isPresent());

    Cookie cookie = cookieOpt.get();
    assertEquals("theme", cookie.name());
    assertEquals("blue", cookie.value());
    assertEquals(3600, cookie.maxAge());
    assertEquals("/some/path", cookie.path());
    assertEquals(".example.com", cookie.domain());
    assertEquals(false, cookie.secure());
    assertEquals(true, cookie.httpOnly());
    assertEquals(Optional.of(Cookie.SameSite.STRICT), cookie.sameSite());
  }

  @Test
  void discardCookie() {
    Http.Cookies cookies =
        call(
                new MockJavaAction(app.injector().instanceOf(JavaHandlerComponents.class)) {
                  // #discard-cookie
                  public Result index() {
                    return ok("<h1>Hello World!</h1>").as(MimeTypes.HTML).discardingCookie("theme");
                  }
                  // #discard-cookie
                },
                fakeRequest(),
                mat)
            .cookies();
    Optional<Cookie> cookie = cookies.get("theme");
    assertTrue(cookie.isPresent());
    assertEquals("theme", cookie.get().name());
    assertEquals("", cookie.get().value());
  }

  @Test
  void charset() {
    assertEquals(
        "iso-8859-1",
        call(
                new MockJavaAction(app.injector().instanceOf(JavaHandlerComponents.class)) {
                  // #charset
                  public Result index() {
                    return ok("<h1>Hello World!</h1>", "iso-8859-1")
                        .as("text/html; charset=iso-8859-1");
                  }
                  // #charset
                },
                fakeRequest(),
                mat)
            .charset()
            .get());
  }

  @Test
  void rangeResultInputStream() {
    Result result =
        call(
            new MockJavaAction(app.injector().instanceOf(JavaHandlerComponents.class)) {
              // #range-result-input-stream
              public Result index(Http.Request request) {
                String content = "This is the full content!";
                InputStream input = getInputStream(content);
                return RangeResults.ofStream(request, input, content.length());
              }
              // #range-result-input-stream

              private InputStream getInputStream(String content) {
                return new ByteArrayInputStream(content.getBytes());
              }
            },
            fakeRequest().header(RANGE, "bytes=0-3"),
            mat);

    assertEquals(PARTIAL_CONTENT, result.status());
    assertEquals("This", Helpers.contentAsString(result, mat));
  }

  @Test
  void rangeResultSource() {
    Result result =
        call(
            new MockJavaAction(app.injector().instanceOf(JavaHandlerComponents.class)) {
              // #range-result-source
              public Result index(Http.Request request) {
                String content = "This is the full content!";
                Source<ByteString, NotUsed> source = sourceFrom(content);
                return RangeResults.ofSource(
                    request, (long) content.length(), source, "file.txt", MimeTypes.TEXT);
              }
              // #range-result-source

              private Source<ByteString, NotUsed> sourceFrom(String content) {
                List<ByteString> byteStrings =
                    content
                        .chars()
                        .boxed()
                        .map(c -> ByteString.fromArray(new byte[] {c.byteValue()}))
                        .collect(Collectors.toList());
                return akka.stream.javadsl.Source.from(byteStrings);
              }
            },
            fakeRequest().header(RANGE, "bytes=0-3"),
            mat);

    assertEquals(PARTIAL_CONTENT, result.status());
    assertEquals("This", Helpers.contentAsString(result, mat));
  }

  @Test
  void rangeResultSourceOffset() {
    Result result =
        call(
            new MockJavaAction(app.injector().instanceOf(JavaHandlerComponents.class)) {
              // #range-result-source-with-offset
              public Result index(Http.Request request) {
                String content = "This is the full content!";
                return RangeResults.ofSource(
                    request,
                    (long) content.length(),
                    offset ->
                        new RangeResults.SourceAndOffset(offset, sourceFrom(content).drop(offset)),
                    "file.txt",
                    MimeTypes.TEXT);
              }
              // #range-result-source-with-offset

              private Source<ByteString, NotUsed> sourceFrom(String content) {
                List<ByteString> byteStrings =
                    content
                        .chars()
                        .boxed()
                        .map(c -> ByteString.fromArray(new byte[] {c.byteValue()}))
                        .collect(Collectors.toList());
                return akka.stream.javadsl.Source.from(byteStrings);
              }
            },
            fakeRequest().header(RANGE, "bytes=8-10"),
            mat);

    assertEquals(PARTIAL_CONTENT, result.status());
    assertEquals("the", Helpers.contentAsString(result, mat));
  }
}
