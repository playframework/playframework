/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.routing;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.*;
import static play.mvc.Results.internalServerError;
import static play.mvc.Results.ok;
import static play.test.Helpers.*;

import akka.util.ByteString;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import play.Application;
import play.libs.Files;
import play.libs.Json;
import play.libs.XML;
import play.mvc.Http;
import play.mvc.PathBindable;
import play.mvc.Result;
import play.mvc.Results;

/**
 * This class is in the integration tests so that we have the right helper classes to build a
 * request with to test it.
 */
public abstract class AbstractRoutingDslTest {

  abstract Application application();

  abstract RoutingDsl routingDsl();

  private Router router(Function<RoutingDsl, Router> function) {
    return function.apply(routingDsl());
  }

  @Test
  public void shouldProvideJavaRequestToActionWithoutParameters() {
    Router router =
        router(
            routingDsl ->
                routingDsl
                    .GET("/with-request")
                    .routingTo(
                        request ->
                            request.header("X-Test").map(Results::ok).orElse(Results.notFound()))
                    .build());

    String result =
        makeRequest(router, "GET", "/with-request", rb -> rb.header("X-Test", "Header value"));
    assertEquals("Header value", result);
  }

  @Test
  public void shouldProvideJavaRequestToActionWithSingleParameter() {
    Router router =
        router(
            routingDsl ->
                routingDsl
                    .GET("/with-request/:p1")
                    .routingTo(
                        (request, number) ->
                            request
                                .header("X-Test")
                                .map(header -> Results.ok(header + " - " + number))
                                .orElse(Results.notFound()))
                    .build());

    String result =
        makeRequest(router, "GET", "/with-request/10", rb -> rb.header("X-Test", "Header value"));
    assertEquals("Header value - 10", result);
  }

  @Test
  public void shouldProvideJavaRequestToActionWith2Parameters() {
    Router router =
        router(
            routingDsl ->
                routingDsl
                    .GET("/with-request/:p1/:p2")
                    .routingTo(
                        (request, n1, n2) ->
                            request
                                .header("X-Test")
                                .map(header -> Results.ok(header + " - " + n1 + " - " + n2))
                                .orElse(Results.notFound()))
                    .build());

    String result =
        makeRequest(
            router, "GET", "/with-request/10/20", rb -> rb.header("X-Test", "Header value"));
    assertEquals("Header value - 10 - 20", result);
  }

  @Test
  public void shouldProvideJavaRequestToActionWith3Parameters() {
    Router router =
        router(
            routingDsl ->
                routingDsl
                    .GET("/with-request/:p1/:p2/:p3")
                    .routingTo(
                        (request, n1, n2, n3) ->
                            request
                                .header("X-Test")
                                .map(
                                    header ->
                                        Results.ok(header + " - " + n1 + " - " + n2 + " - " + n3))
                                .orElse(Results.notFound()))
                    .build());

    String result =
        makeRequest(
            router, "GET", "/with-request/10/20/30", rb -> rb.header("X-Test", "Header value"));
    assertEquals("Header value - 10 - 20 - 30", result);
  }

  @Test
  public void shouldProvideJavaRequestToAsyncActionWithoutParameters() {
    Router router =
        router(
            routingDsl ->
                routingDsl
                    .GET("/with-request")
                    .routingAsync(
                        request ->
                            CompletableFuture.completedFuture(
                                request
                                    .header("X-Test")
                                    .map(Results::ok)
                                    .orElse(Results.notFound())))
                    .build());

    String result =
        makeRequest(router, "GET", "/with-request", rb -> rb.header("X-Test", "Header value"));
    assertEquals("Header value", result);
  }

  @Test
  public void shouldProvideJavaRequestToAsyncActionWithSingleParameter() {
    Router router =
        router(
            routingDsl ->
                routingDsl
                    .GET("/with-request/:p1")
                    .routingAsync(
                        (request, number) ->
                            CompletableFuture.completedFuture(
                                request
                                    .header("X-Test")
                                    .map(header -> Results.ok(header + " - " + number))
                                    .orElse(Results.notFound())))
                    .build());

    String result =
        makeRequest(router, "GET", "/with-request/10", rb -> rb.header("X-Test", "Header value"));
    assertEquals("Header value - 10", result);
  }

  @Test
  public void shouldProvideJavaRequestToAsyncActionWith2Parameters() {
    Router router =
        router(
            routingDsl ->
                routingDsl
                    .GET("/with-request/:p1/:p2")
                    .routingAsync(
                        (request, n1, n2) ->
                            CompletableFuture.completedFuture(
                                request
                                    .header("X-Test")
                                    .map(header -> Results.ok(header + " - " + n1 + " - " + n2))
                                    .orElse(Results.notFound())))
                    .build());

    String result =
        makeRequest(
            router, "GET", "/with-request/10/20", rb -> rb.header("X-Test", "Header value"));
    assertEquals("Header value - 10 - 20", result);
  }

  @Test
  public void shouldProvideJavaRequestToAsyncActionWith3Parameters() {
    Router router =
        router(
            routingDsl ->
                routingDsl
                    .GET("/with-request/:p1/:p2/:p3")
                    .routingAsync(
                        (request, n1, n2, n3) ->
                            CompletableFuture.completedFuture(
                                request
                                    .header("X-Test")
                                    .map(
                                        header ->
                                            Results.ok(
                                                header + " - " + n1 + " - " + n2 + " - " + n3))
                                    .orElse(Results.notFound())))
                    .build());

    String result =
        makeRequest(
            router, "GET", "/with-request/10/20/30", rb -> rb.header("X-Test", "Header value"));
    assertEquals("Header value - 10 - 20 - 30", result);
  }

  @Test
  public void shouldPreserveRequestBodyAsText() {
    Router router =
        router(
            routingDsl ->
                routingDsl
                    .POST("/with-body")
                    .routingTo(request -> Results.ok(request.body().asText()))
                    .build());

    String result = makeRequest(router, "POST", "/with-body", rb -> rb.bodyText("The Body"));
    assertEquals("The Body", result);
  }

  @Test
  public void shouldPreserveRequestBodyAsJson() {
    Router router =
        router(
            routingDsl ->
                routingDsl
                    .POST("/with-body")
                    .routingTo(request -> Results.ok(request.body().asJson()))
                    .build());

    String result =
        makeRequest(
            router,
            "POST",
            "/with-body",
            requestBuilder -> requestBuilder.bodyJson(Json.parse("{ \"a\": \"b\" }")));
    assertEquals("{\"a\":\"b\"}", result);
  }

  @Test
  public void shouldPreserveRequestBodyAsXml() {
    Router router =
        router(
            routingDsl ->
                routingDsl
                    .POST("/with-body")
                    .routingTo(request -> ok(XML.toBytes(request.body().asXml()).utf8String()))
                    .build());

    String result =
        makeRequest(
            router,
            "POST",
            "/with-body",
            requestBuilder ->
                requestBuilder.bodyXml(
                    XML.fromString("<?xml version=\"1.0\" encoding=\"UTF-8\"?><a>b</a>")));
    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><a>b</a>", result);
  }

  @Test
  public void shouldPreserveRequestBodyAsRawBuffer() {
    Router router =
        router(
            routingDsl ->
                routingDsl
                    .POST("/with-body")
                    .routingTo(request -> ok(request.body().asRaw().asBytes().utf8String()))
                    .build());

    String result =
        makeRequest(
            router,
            "POST",
            "/with-body",
            requestBuilder -> requestBuilder.bodyRaw(ByteString.fromString("The Raw Body")));
    assertEquals("The Raw Body", result);
  }

  @Test
  public void shouldAcceptMultipartFormData() throws IOException {
    Router router =
        router(
            routingDsl ->
                routingDsl
                    .POST("/with-body")
                    .routingTo(
                        request -> {
                          Http.MultipartFormData<Object> data =
                              request.body().asMultipartFormData();
                          Files.TemporaryFile ref =
                              (Files.TemporaryFile) data.getFile("document").getRef();
                          try {
                            String contents = java.nio.file.Files.readString(ref.path());
                            return ok(
                                "author: "
                                    + data.asFormUrlEncoded().get("author")[0]
                                    + "\n"
                                    + "filename: "
                                    + data.getFile("document").getFilename()
                                    + "\n"
                                    + "contentType: "
                                    + data.getFile("document").getContentType()
                                    + "\n"
                                    + "contents: "
                                    + contents
                                    + "\n");

                          } catch (IOException e) {
                            return internalServerError(e.getMessage());
                          }
                        })
                    .build());

    Files.TemporaryFile tempFile = Files.singletonTemporaryFileCreator().create("temp", "txt");
    java.nio.file.Files.write(tempFile.path(), "Twas brillig and the slithy Toves...".getBytes());
    String result =
        makeRequest(
            router,
            "POST",
            "/with-body",
            requestBuilder ->
                requestBuilder.bodyMultipart(
                    Map.of("author", new String[] {"Lewis Carrol"}),
                    List.of(
                        new Http.MultipartFormData.FilePart<>(
                            "document", "jabberwocky.txt", "text/plain", tempFile))));
    assertEquals(
        "author: Lewis Carrol\n"
            + "filename: jabberwocky.txt\n"
            + "contentType: text/plain\n"
            + "contents: Twas brillig and the slithy Toves...\n",
        result);
  }

  @Test
  public void shouldPreserveRequestBodyAsTextWhenUsingHttpRequest() {
    Router router =
        router(
            routingDsl ->
                routingDsl.POST("/with-body").routingTo(req -> ok(req.body().asText())).build());

    String result = makeRequest(router, "POST", "/with-body", rb -> rb.bodyText("The Body"));
    assertEquals("The Body", result);
  }

  @Test
  public void shouldPreserveRequestBodyAsJsonWhenUsingHttpRequest() {
    Router router =
        router(
            routingDsl ->
                routingDsl.POST("/with-body").routingTo(req -> ok(req.body().asJson())).build());

    String result =
        makeRequest(
            router,
            "POST",
            "/with-body",
            requestBuilder -> requestBuilder.bodyJson(Json.parse("{ \"a\": \"b\" }")));
    assertEquals("{\"a\":\"b\"}", result);
  }

  @Test
  public void shouldPreserveRequestBodyAsXmlWhenUsingHttpRequest() {
    Router router =
        router(
            routingDsl ->
                routingDsl
                    .POST("/with-body")
                    .routingTo(req -> ok(XML.toBytes(req.body().asXml()).utf8String()))
                    .build());

    String result =
        makeRequest(
            router,
            "POST",
            "/with-body",
            requestBuilder ->
                requestBuilder.bodyXml(
                    XML.fromString("<?xml version=\"1.0\" encoding=\"UTF-8\"?><a>b</a>")));
    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><a>b</a>", result);
  }

  @Test
  public void shouldPreserveRequestBodyAsRawBufferWhenUsingHttpRequest() {
    Router router =
        router(
            routingDsl ->
                routingDsl
                    .POST("/with-body")
                    .routingTo(req -> ok(req.body().asRaw().asBytes().utf8String()))
                    .build());

    String result =
        makeRequest(
            router,
            "POST",
            "/with-body",
            requestBuilder -> requestBuilder.bodyRaw(ByteString.fromString("The Raw Body")));
    assertEquals("The Raw Body", result);
  }

  @Test
  public void noParameters() {
    Router router =
        router(
            routingDsl ->
                routingDsl.GET("/hello/world").routingTo(req -> ok("Hello world")).build());

    assertEquals("Hello world", makeRequest(router, "GET", "/hello/world"));
    assertNull(makeRequest(router, "GET", "/foo/bar"));
  }

  @Test
  public void oneParameter() {
    Router router =
        router(
            routingDsl ->
                routingDsl.GET("/hello/:to").routingTo((req, to) -> ok("Hello " + to)).build());

    assertEquals("Hello world", makeRequest(router, "GET", "/hello/world"));
    assertNull(makeRequest(router, "GET", "/foo/bar"));
  }

  @Test
  public void twoParameters() {
    Router router =
        router(
            routingDsl ->
                routingDsl
                    .GET("/:say/:to")
                    .routingTo((req, say, to) -> ok(say + " " + to))
                    .build());

    assertEquals("Hello world", makeRequest(router, "GET", "/Hello/world"));
    assertNull(makeRequest(router, "GET", "/foo"));
  }

  @Test
  public void threeParameters() {
    Router router =
        router(
            routingDsl ->
                routingDsl
                    .GET("/:say/:to/:extra")
                    .routingTo((req, say, to, extra) -> ok(say + " " + to + extra))
                    .build());

    assertEquals("Hello world!", makeRequest(router, "GET", "/Hello/world/!"));
    assertNull(makeRequest(router, "GET", "/foo/bar"));
  }

  @Test
  public void noParametersAsync() {
    Router router =
        router(
            routingDsl ->
                routingDsl
                    .GET("/hello/world")
                    .routingAsync(req -> completedFuture(ok("Hello world")))
                    .build());

    assertEquals("Hello world", makeRequest(router, "GET", "/hello/world"));
    assertNull(makeRequest(router, "GET", "/foo/bar"));
  }

  @Test
  public void oneParameterAsync() {
    Router router =
        router(
            routingDsl ->
                routingDsl
                    .GET("/hello/:to")
                    .routingAsync((req, to) -> completedFuture(ok("Hello " + to)))
                    .build());

    assertEquals("Hello world", makeRequest(router, "GET", "/hello/world"));
    assertNull(makeRequest(router, "GET", "/foo/bar"));
  }

  @Test
  public void twoParametersAsync() {
    Router router =
        router(
            routingDsl ->
                routingDsl
                    .GET("/:say/:to")
                    .routingAsync((req, say, to) -> completedFuture(ok(say + " " + to)))
                    .build());

    assertEquals("Hello world", makeRequest(router, "GET", "/Hello/world"));
    assertNull(makeRequest(router, "GET", "/foo"));
  }

  @Test
  public void threeParametersAsync() {
    Router router =
        router(
            routingDsl ->
                routingDsl
                    .GET("/:say/:to/:extra")
                    .routingAsync(
                        (req, say, to, extra) -> completedFuture(ok(say + " " + to + extra)))
                    .build());

    assertEquals("Hello world!", makeRequest(router, "GET", "/Hello/world/!"));
    assertNull(makeRequest(router, "GET", "/foo/bar"));
  }

  @Test
  public void get() {
    Router router =
        router(
            routingDsl ->
                routingDsl.GET("/hello/world").routingTo(req -> ok("Hello world")).build());

    assertEquals("Hello world", makeRequest(router, "GET", "/hello/world"));
    assertNull(makeRequest(router, "POST", "/hello/world"));
  }

  @Test
  public void head() {
    Router router =
        router(
            routingDsl ->
                routingDsl.HEAD("/hello/world").routingTo(req -> ok("Hello world")).build());

    assertEquals("Hello world", makeRequest(router, "HEAD", "/hello/world"));
    assertNull(makeRequest(router, "POST", "/hello/world"));
  }

  @Test
  public void post() {
    Router router =
        router(
            routingDsl ->
                routingDsl.POST("/hello/world").routingTo(req -> ok("Hello world")).build());

    assertEquals("Hello world", makeRequest(router, "POST", "/hello/world"));
    assertNull(makeRequest(router, "GET", "/hello/world"));
  }

  @Test
  public void put() {
    Router router =
        router(
            routingDsl ->
                routingDsl.PUT("/hello/world").routingTo(req -> ok("Hello world")).build());

    assertEquals("Hello world", makeRequest(router, "PUT", "/hello/world"));
    assertNull(makeRequest(router, "POST", "/hello/world"));
  }

  @Test
  public void delete() {
    Router router =
        router(
            routingDsl ->
                routingDsl.DELETE("/hello/world").routingTo(req -> ok("Hello world")).build());

    assertEquals("Hello world", makeRequest(router, "DELETE", "/hello/world"));
    assertNull(makeRequest(router, "POST", "/hello/world"));
  }

  @Test
  public void patch() {
    Router router =
        router(
            routingDsl ->
                routingDsl.PATCH("/hello/world").routingTo(req -> ok("Hello world")).build());

    assertEquals("Hello world", makeRequest(router, "PATCH", "/hello/world"));
    assertNull(makeRequest(router, "POST", "/hello/world"));
  }

  @Test
  public void options() {
    Router router =
        router(
            routingDsl ->
                routingDsl.OPTIONS("/hello/world").routingTo(req -> ok("Hello world")).build());

    assertEquals("Hello world", makeRequest(router, "OPTIONS", "/hello/world"));
    assertNull(makeRequest(router, "POST", "/hello/world"));
  }

  @Test
  public void withSessionAndHeader() {
    Router router =
        router(
            routingDsl ->
                routingDsl
                    .GET("/hello/world")
                    .routingTo(
                        req ->
                            ok("Hello world")
                                .addingToSession(req, "foo", "bar")
                                .withHeader("Foo", "Bar"))
                    .build());

    Result result = routeAndCall(application(), router, fakeRequest("GET", "/hello/world"));
    assertEquals(Optional.of("bar"), result.session().get("foo"));
    assertEquals("Bar", result.headers().get("Foo"));
  }

  @Test
  public void starMatcher() {
    Router router =
        router(
            routingDsl ->
                routingDsl.GET("/hello/*to").routingTo((req, to) -> ok("Hello " + to)).build());

    assertEquals("Hello blah/world", makeRequest(router, "GET", "/hello/blah/world"));
    assertNull(makeRequest(router, "GET", "/foo/bar"));
  }

  @Test
  public void regexMatcher() {
    Router router =
        router(
            routingDsl ->
                routingDsl
                    .GET("/hello/$to<[a-z]+>")
                    .routingTo((req, to) -> ok("Hello " + to))
                    .build());

    assertEquals("Hello world", makeRequest(router, "GET", "/hello/world"));
    assertNull(makeRequest(router, "GET", "/hello/10"));
  }

  @Test
  public void multipleRoutes() {
    Router router =
        router(
            routingDsl ->
                routingDsl
                    .GET("/hello/:to")
                    .routingTo((req, to) -> ok("Hello " + to))
                    .GET("/foo/bar")
                    .routingTo(req -> ok("foo bar"))
                    .POST("/hello/:to")
                    .routingTo((req, to) -> ok("Post " + to))
                    .GET("/*path")
                    .routingTo((req, path) -> ok("Path " + path))
                    .build());

    assertEquals("Hello world", makeRequest(router, "GET", "/hello/world"));
    assertEquals("foo bar", makeRequest(router, "GET", "/foo/bar"));
    assertEquals("Post world", makeRequest(router, "POST", "/hello/world"));
    assertEquals("Path something/else", makeRequest(router, "GET", "/something/else"));
  }

  @Test
  public void encoding() {
    Router router =
        router(
            routingDsl ->
                routingDsl
                    .GET("/simple/:to")
                    .routingTo((req, to) -> ok("Simple " + to))
                    .GET("/path/*to")
                    .routingTo((req, to) -> ok("Path " + to))
                    .GET("/regex/$to<.*>")
                    .routingTo((req, to) -> ok("Regex " + to))
                    .build());

    assertEquals("Simple dollar$", makeRequest(router, "GET", "/simple/dollar%24"));
    assertEquals("Path dollar%24", makeRequest(router, "GET", "/path/dollar%24"));
    assertEquals("Regex dollar%24", makeRequest(router, "GET", "/regex/dollar%24"));
  }

  @Test
  public void typed() {
    Router router =
        router(
            routingDsl ->
                routingDsl
                    .GET("/:a/:b/:c")
                    .routingTo(
                        (Http.Request req, Integer a, Boolean b, String c) ->
                            ok("int " + a + " boolean " + b + " string " + c))
                    .build());

    assertEquals("int 20 boolean true string foo", makeRequest(router, "GET", "/20/true/foo"));
  }

  @Test
  public void wrongNumberOfParameters() {
    assertThrowsExactly(
        IllegalArgumentException.class,
        () -> routingDsl().GET("/:a/:b").routingTo((req, foo) -> ok(foo.toString())));
  }

  @Test
  public void badParameterType() {
    assertThrowsExactly(
        IllegalArgumentException.class,
        () -> routingDsl().GET("/:a").routingTo((Http.Request req, InputStream is) -> ok()));
  }

  @Test
  public void bindError() {
    Router router =
        router(
            routingDsl ->
                routingDsl
                    .GET("/:a")
                    .routingTo((Http.Request req, Integer a) -> ok("int " + a))
                    .build());

    assertEquals(
        "Cannot parse parameter a as Int: For input string: \"foo\"",
        makeRequest(router, "GET", "/foo"));
  }

  @Test
  public void customPathBindable() {
    Router router =
        router(
            routingDsl ->
                routingDsl
                    .GET("/:a")
                    .routingTo((Http.Request req, MyString myString) -> ok(myString.value))
                    .build());

    assertEquals("a:foo", makeRequest(router, "GET", "/foo"));
  }

  public static class MyString implements PathBindable<MyString> {
    final String value;

    public MyString() {
      this.value = null;
    }

    public MyString(String value) {
      this.value = value;
    }

    public MyString bind(String key, String txt) {
      return new MyString(key + ":" + txt);
    }

    public String unbind(String key) {
      return null;
    }

    public String javascriptUnbind() {
      return null;
    }
  }

  private String makeRequest(Router router, String method, String path) {
    return makeRequest(router, method, path, Function.identity());
  }

  private String makeRequest(
      Router router,
      String method,
      String path,
      Function<Http.RequestBuilder, Http.RequestBuilder> bodySetter) {
    Http.RequestBuilder request = bodySetter.apply(fakeRequest(method, path));
    Result result = routeAndCall(application(), router, request);
    if (result == null) {
      return null;
    } else {
      return contentAsString(result);
    }
  }
}
