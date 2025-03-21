/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.http;

import static javaguide.testhelpers.MockJavaActionHelper.*;
import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Controller.*;
import static play.test.Helpers.fakeRequest;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javaguide.testhelpers.MockJavaAction;
import org.apache.pekko.NotUsed;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.util.ByteString;
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

    assertThat(textResult.contentType())
        .hasValueSatisfying(__ -> assertThat(__).contains("text/plain"));
  }

  @Test
  public void jsonContentType() {
    String object = "";
    // #json-content-type
    JsonNode json = Json.toJson(object);
    Result jsonResult = ok(json);
    // #json-content-type

    assertThat(jsonResult.contentType())
        .hasValueSatisfying(__ -> assertThat(__).contains("application/json"));
  }

  @Test
  public void customContentType() {
    // #custom-content-type
    Result htmlResult = ok("<h1>Hello World!</h1>").as("text/html");
    // #custom-content-type

    assertThat(htmlResult.contentType())
        .hasValueSatisfying(__ -> assertThat(__).contains("text/html"));
  }

  @Test
  public void customDefiningContentType() {
    // #content-type_defined_html
    Result htmlResult = ok("<h1>Hello World!</h1>").as(MimeTypes.HTML);
    // #content-type_defined_html

    assertThat(htmlResult.contentType())
        .hasValueSatisfying(__ -> assertThat(__).contains("text/html"));
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
    assertThat(headers.get(CACHE_CONTROL)).isEqualTo("max-age=3600");
    assertThat(headers.get(ETAG)).isEqualTo("some-etag-calculated-value");
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
    assertThat(cookie)
        .isPresent()
        .hasValueSatisfying(__ -> assertThat(__.value()).isEqualTo("blue"));
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
                                .withPartitioned(false)
                                .build());
                  }
                  // #detailed-set-cookie
                },
                fakeRequest(),
                mat)
            .cookies();
    Optional<Cookie> cookieOpt = cookies.get("theme");

    assertThat(cookieOpt)
        .isPresent()
        .hasValueSatisfying(
            cookie -> {
              assertThat(cookie.name()).isEqualTo("theme");
              assertThat(cookie.value()).isEqualTo("blue");
              assertThat(cookie.maxAge()).isEqualTo(3600);
              assertThat(cookie.path()).isEqualTo("/some/path");
              assertThat(cookie.domain()).isEqualTo(".example.com");
              assertThat(cookie.secure()).isFalse();
              assertThat(cookie.httpOnly()).isTrue();
              assertThat(cookie.sameSite()).hasValue(Cookie.SameSite.STRICT);
              assertThat(cookie.partitioned()).isFalse();
            });
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
    assertThat(cookie)
        .isPresent()
        .hasValueSatisfying(
            c -> {
              assertThat(c.name()).isEqualTo("theme");
              assertThat(c.value()).isEmpty();
            });
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
                .charset())
        .hasValue("iso-8859-1");
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

    assertThat(result.status()).isEqualTo(PARTIAL_CONTENT);
    assertThat(Helpers.contentAsString(result, mat)).isEqualTo("This");
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
                return org.apache.pekko.stream.javadsl.Source.from(byteStrings);
              }
            },
            fakeRequest().header(RANGE, "bytes=0-3"),
            mat);

    assertThat(result.status()).isEqualTo(PARTIAL_CONTENT);
    assertThat(Helpers.contentAsString(result, mat)).isEqualTo("This");
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
                return org.apache.pekko.stream.javadsl.Source.from(byteStrings);
              }
            },
            fakeRequest().header(RANGE, "bytes=8-10"),
            mat);

    assertThat(result.status()).isEqualTo(PARTIAL_CONTENT);
    assertThat(Helpers.contentAsString(result, mat)).isEqualTo("the");
  }
}
