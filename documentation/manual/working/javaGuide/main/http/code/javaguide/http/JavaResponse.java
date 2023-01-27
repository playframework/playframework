/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.http;

import static javaguide.testhelpers.MockJavaActionHelper.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static play.mvc.Controller.*;
import static play.test.Helpers.fakeRequest;

import akka.NotUsed;
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
import org.junit.Test;
import play.core.j.JavaHandlerComponents;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Http.Cookie;
import play.mvc.Http.MimeTypes;
import play.mvc.RangeResults;
import play.mvc.Result;
import play.test.Helpers;
import play.test.WithApplication;

public class JavaResponse extends WithApplication {

  @Test
  public void textContentType() {
    // #text-content-type
    Result textResult = ok("Hello World!");
    // #text-content-type

    assertThat(textResult.contentType().get(), containsString("text/plain"));
  }

  @Test
  public void jsonContentType() {
    String object = "";
    // #json-content-type
    JsonNode json = Json.toJson(object);
    Result jsonResult = ok(json);
    // #json-content-type

    assertThat(jsonResult.contentType().get(), containsString("application/json"));
  }

  @Test
  public void customContentType() {
    // #custom-content-type
    Result htmlResult = ok("<h1>Hello World!</h1>").as("text/html");
    // #custom-content-type

    assertThat(htmlResult.contentType().get(), containsString("text/html"));
  }

  @Test
  public void customDefiningContentType() {
    // #content-type_defined_html
    Result htmlResult = ok("<h1>Hello World!</h1>").as(MimeTypes.HTML);
    // #content-type_defined_html

    assertThat(htmlResult.contentType().get(), containsString("text/html"));
  }

  @Test
  public void responseHeaders() {
    Map<String, String> headers =
        call(
                new MockJavaAction(instanceOf(JavaHandlerComponents.class)) {
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
    assertThat(headers.get(CACHE_CONTROL), equalTo("max-age=3600"));
    assertThat(headers.get(ETAG), equalTo("some-etag-calculated-value"));
  }

  @Test
  public void setCookie() {
    Http.Cookies cookies =
        call(
                new MockJavaAction(instanceOf(JavaHandlerComponents.class)) {
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
    assertThat(cookie.get().value(), equalTo("blue"));
  }

  @Test
  public void detailedSetCookie() {
    Http.Cookies cookies =
        call(
                new MockJavaAction(instanceOf(JavaHandlerComponents.class)) {
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
    assertThat(cookie.name(), equalTo("theme"));
    assertThat(cookie.value(), equalTo("blue"));
    assertThat(cookie.maxAge(), equalTo(3600));
    assertThat(cookie.path(), equalTo("/some/path"));
    assertThat(cookie.domain(), equalTo(".example.com"));
    assertThat(cookie.secure(), equalTo(false));
    assertThat(cookie.httpOnly(), equalTo(true));
    assertThat(cookie.sameSite(), equalTo(Optional.of(Cookie.SameSite.STRICT)));
  }

  @Test
  public void discardCookie() {
    Http.Cookies cookies =
        call(
                new MockJavaAction(instanceOf(JavaHandlerComponents.class)) {
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
    assertThat(cookie.get().name(), equalTo("theme"));
    assertThat(cookie.get().value(), equalTo(""));
  }

  @Test
  public void charset() {
    assertThat(
        call(
                new MockJavaAction(instanceOf(JavaHandlerComponents.class)) {
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
            .get(),
        equalTo("iso-8859-1"));
  }

  @Test
  public void rangeResultInputStream() {
    Result result =
        call(
            new MockJavaAction(instanceOf(JavaHandlerComponents.class)) {
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

    assertThat(result.status(), equalTo(PARTIAL_CONTENT));
    assertThat(Helpers.contentAsString(result, mat), equalTo("This"));
  }

  @Test
  public void rangeResultSource() {
    Result result =
        call(
            new MockJavaAction(instanceOf(JavaHandlerComponents.class)) {
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

    assertThat(result.status(), equalTo(PARTIAL_CONTENT));
    assertThat(Helpers.contentAsString(result, mat), equalTo("This"));
  }

  @Test
  public void rangeResultSourceOffset() {
    Result result =
        call(
            new MockJavaAction(instanceOf(JavaHandlerComponents.class)) {
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

    assertThat(result.status(), equalTo(PARTIAL_CONTENT));
    assertThat(Helpers.contentAsString(result, mat), equalTo("the"));
  }
}
