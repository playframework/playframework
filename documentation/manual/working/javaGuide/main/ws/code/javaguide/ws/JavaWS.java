/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import javaguide.testhelpers.MockJavaAction;

// #ws-imports
import org.slf4j.Logger;
import play.api.Configuration;
import play.core.j.JavaHandlerComponents;
import play.libs.concurrent.Futures;
import play.libs.ws.*;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.concurrent.*;
// #ws-imports

// #json-imports
import com.fasterxml.jackson.databind.JsonNode;
import play.libs.Json;
// #json-imports

// #multipart-imports
import play.mvc.Http.MultipartFormData.*;
// #multipart-imports

import java.io.*;
import java.util.Optional;
import java.util.stream.*;

import org.w3c.dom.Document;
import play.mvc.Result;

import javax.inject.Inject;

import play.http.HttpEntity;
import play.mvc.Http.Status;

// #ws-client-imports
import akka.stream.Materializer;
import akka.stream.javadsl.*;
import akka.util.ByteString;
import play.mvc.Results;
// #ws-client-imports

public class JavaWS {
  private static final String feedUrl = "http://localhost:3333/feed";

  public static class Controller0 extends MockJavaAction
      implements WSBodyReadables, WSBodyWritables {

    private final WSClient ws;
    private final Materializer materializer;

    @Inject
    Controller0(
        JavaHandlerComponents javaHandlerComponents, WSClient ws, Materializer materializer) {
      super(javaHandlerComponents);
      this.ws = ws;
      this.materializer = materializer;
    }

    public void requestExamples() {
      // #ws-holder
      WSRequest request = ws.url("http://example.com");
      // #ws-holder

      // #ws-complex-holder
      WSRequest complexRequest =
          request
              .addHeader("headerKey", "headerValue")
              .setRequestTimeout(Duration.of(1000, ChronoUnit.MILLIS))
              .addQueryParameter("paramKey", "paramValue");
      // #ws-complex-holder

      // #ws-get
      CompletionStage<? extends WSResponse> responsePromise = complexRequest.get();
      // #ws-get

      String url = "http://example.com";
      // #ws-auth
      ws.url(url).setAuth("user", "password", WSAuthScheme.BASIC).get();
      // #ws-auth

      // #ws-follow-redirects
      ws.url(url).setFollowRedirects(true).get();
      // #ws-follow-redirects

      // #ws-query-parameter
      ws.url(url).addQueryParameter("paramKey", "paramValue");
      // #ws-query-parameter

      // #ws-header
      ws.url(url).addHeader("headerKey", "headerValue").get();
      // #ws-header

      // #ws-cookie
      ws.url(url)
          .addCookies(new WSCookieBuilder().setName("headerKey").setValue("headerValue").build())
          .get();
      // #ws-cookie

      String jsonString = "{\"key1\":\"value1\"}";
      // #ws-header-content-type
      ws.url(url).addHeader("Content-Type", "application/json").post(jsonString);
      // OR
      ws.url(url).setContentType("application/json").post(jsonString);
      // #ws-header-content-type

      // #ws-timeout
      ws.url(url).setRequestTimeout(Duration.of(1000, ChronoUnit.MILLIS)).get();
      // #ws-timeout

      // #ws-post-form-data
      ws.url(url)
          .setContentType("application/x-www-form-urlencoded")
          .post("key1=value1&key2=value2");
      // #ws-post-form-data

      // #ws-post-json
      JsonNode json = Json.newObject().put("key1", "value1").put("key2", "value2");

      ws.url(url).post(json);
      // #ws-post-json

      // #ws-post-json-objectmapper
      ObjectMapper objectMapper = play.libs.Json.newDefaultMapper();
      ws.url(url).post(body(json, objectMapper));
      // #ws-post-json-objectmapper

      // #ws-post-xml
      Document xml = play.libs.XML.fromString("<document></document>");
      ws.url(url).post(xml);
      // #ws-post-xml

      // #ws-post-multipart
      ws.url(url).post(Source.single(new DataPart("hello", "world")));
      // #ws-post-multipart

      // #ws-post-multipart2
      Source<ByteString, ?> file = FileIO.fromFile(new File("hello.txt"));
      FilePart<Source<ByteString, ?>> fp = new FilePart<>("hello", "hello.txt", "text/plain", file);
      DataPart dp = new DataPart("key", "value");

      ws.url(url).post(Source.from(Arrays.asList(fp, dp)));
      // #ws-post-multipart2

      String value =
          IntStream.range(0, 100).boxed().map(i -> "abcdefghij").reduce("", (a, b) -> a + b);
      ByteString seedValue = ByteString.fromString(value);
      Stream<ByteString> largeSource = IntStream.range(0, 10).boxed().map(i -> seedValue);
      Source<ByteString, ?> largeImage = Source.from(largeSource.collect(Collectors.toList()));
      // #ws-stream-request
      CompletionStage<WSResponse> wsResponse = ws.url(url).setBody(body(largeImage)).execute("PUT");
      // #ws-stream-request
    }

    public void responseExamples() {

      String url = "http://example.com";

      // #ws-response-json
      // implements WSBodyReadables or use WSBodyReadables.instance.json()
      CompletionStage<JsonNode> jsonPromise = ws.url(url).get().thenApply(r -> r.getBody(json()));
      // #ws-response-json

      // #ws-response-xml
      // implements WSBodyReadables or use WSBodyReadables.instance.xml()
      CompletionStage<Document> documentPromise =
          ws.url(url).get().thenApply(r -> r.getBody(xml()));
      // #ws-response-xml
    }

    public void streamSimpleRequest() {
      String url = "http://example.com";
      // #stream-count-bytes
      // Make the request
      CompletionStage<WSResponse> futureResponse = ws.url(url).setMethod("GET").stream();

      CompletionStage<Long> bytesReturned =
          futureResponse.thenCompose(
              res -> {
                Source<ByteString, ?> responseBody = res.getBodyAsSource();

                // Count the number of bytes returned
                Sink<ByteString, CompletionStage<Long>> bytesSum =
                    Sink.fold(0L, (total, bytes) -> total + bytes.length());

                return responseBody.runWith(bytesSum, materializer);
              });
      // #stream-count-bytes
    }

    public void streamFile()
        throws IOException, FileNotFoundException, InterruptedException, ExecutionException {
      String url = "http://example.com";
      // #stream-to-file
      File file = File.createTempFile("stream-to-file-", ".txt");
      OutputStream outputStream = java.nio.file.Files.newOutputStream(file.toPath());

      // Make the request
      CompletionStage<WSResponse> futureResponse = ws.url(url).setMethod("GET").stream();

      CompletionStage<File> downloadedFile =
          futureResponse.thenCompose(
              res -> {
                Source<ByteString, ?> responseBody = res.getBodyAsSource();

                // The sink that writes to the output stream
                Sink<ByteString, CompletionStage<akka.Done>> outputWriter =
                    Sink.<ByteString>foreach(bytes -> outputStream.write(bytes.toArray()));

                // materialize and run the stream
                CompletionStage<File> result =
                    responseBody
                        .runWith(outputWriter, materializer)
                        .whenComplete(
                            (value, error) -> {
                              // Close the output stream whether there was an error or not
                              try {
                                outputStream.close();
                              } catch (IOException e) {
                              }
                            })
                        .thenApply(v -> file);
                return result;
              });
      // #stream-to-file
      downloadedFile.toCompletableFuture().get();
      file.delete();
    }

    public void streamResponse() {
      String url = "http://example.com";
      // #stream-to-result
      // Make the request
      CompletionStage<WSResponse> futureResponse = ws.url(url).setMethod("GET").stream();

      CompletionStage<Result> result =
          futureResponse.thenApply(
              response -> {
                Source<ByteString, ?> body = response.getBodyAsSource();
                // Check that the response was successful
                if (response.getStatus() == 200) {
                  // Get the content type
                  String contentType =
                      Optional.ofNullable(response.getHeaders().get("Content-Type"))
                          .map(contentTypes -> contentTypes.get(0))
                          .orElse("application/octet-stream");

                  // If there's a content length, send that, otherwise return the body chunked
                  Optional<String> contentLength =
                      Optional.ofNullable(response.getHeaders().get("Content-Length"))
                          .map(contentLengths -> contentLengths.get(0));
                  if (contentLength.isPresent()) {
                    return ok().sendEntity(
                            new HttpEntity.Streamed(
                                body,
                                Optional.of(Long.parseLong(contentLength.get())),
                                Optional.of(contentType)));
                  } else {
                    return ok().chunked(body).as(contentType);
                  }
                } else {
                  return new Result(Status.BAD_GATEWAY);
                }
              });
      // #stream-to-result
    }

    public void streamPut() {
      String url = "http://example.com";
      // #stream-put
      CompletionStage<WSResponse> futureResponse =
          ws.url(url).setMethod("PUT").setBody(body("some body")).stream();
      // #stream-put
    }

    public void patternExamples() {
      String urlOne = "http://localhost:3333/one";
      // #ws-composition
      final CompletionStage<WSResponse> responseThreePromise =
          ws.url(urlOne)
              .get()
              .thenCompose(responseOne -> ws.url(responseOne.getBody()).get())
              .thenCompose(responseTwo -> ws.url(responseTwo.getBody()).get());
      // #ws-composition

      // #ws-recover
      CompletionStage<WSResponse> responsePromise = ws.url("http://example.com").get();
      responsePromise.handle(
          (result, error) -> {
            if (error != null) {
              return ws.url("http://backup.example.com").get();
            } else {
              return CompletableFuture.completedFuture(result);
            }
          });
      // #ws-recover
    }

    public void clientExamples() {
      play.api.Configuration configuration = Configuration.reference();
      play.Environment environment = play.Environment.simple();

      // #ws-client
      // Set up the client config (you can also use a parser here):
      // play.api.Configuration configuration = ... // injection
      // play.Environment environment = ... // injection

      WSClient customWSClient =
          play.libs.ws.ahc.AhcWSClient.create(
              play.libs.ws.ahc.AhcWSClientConfigFactory.forConfig(
                  configuration.underlying(), environment.classLoader()),
              null, // no HTTP caching
              materializer);
      // #ws-client

      org.slf4j.Logger logger = play.Logger.underlying();
      // #ws-close-client
      try {
        customWSClient.close();
      } catch (IOException e) {
        logger.error(e.getMessage(), e);
      }
      // #ws-close-client

      // #ws-underlying-client
      play.shaded.ahc.org.asynchttpclient.AsyncHttpClient underlyingClient =
          (play.shaded.ahc.org.asynchttpclient.AsyncHttpClient) ws.getUnderlying();
      // #ws-underlying-client

    }
  }

  public static class Controller1 extends MockJavaAction {

    private final WSClient ws;

    @Inject
    public Controller1(JavaHandlerComponents javaHandlerComponents, WSClient client) {
      super(javaHandlerComponents);
      this.ws = client;
    }

    // #ws-action
    public CompletionStage<Result> index() {
      return ws.url(feedUrl)
          .get()
          .thenApply(response -> ok("Feed title: " + response.asJson().findPath("title").asText()));
    }
    // #ws-action
  }

  public static class Controller2 extends MockJavaAction
      implements WSBodyWritables, WSBodyReadables {

    private final WSClient ws;

    @Inject
    public Controller2(JavaHandlerComponents javaHandlerComponents, WSClient ws) {
      super(javaHandlerComponents);
      this.ws = ws;
    }

    // #composed-call
    public CompletionStage<Result> index() {
      return ws.url(feedUrl)
          .get()
          .thenCompose(response -> ws.url(response.asJson().findPath("commentsUrl").asText()).get())
          .thenApply(
              response -> ok("Number of comments: " + response.asJson().findPath("count").asInt()));
    }
    // #composed-call
  }

  public static class Controller3 extends MockJavaAction
      implements WSBodyWritables, WSBodyReadables {

    private final WSClient ws;
    private Logger logger;

    @Inject
    public Controller3(JavaHandlerComponents javaHandlerComponents, WSClient ws) {
      super(javaHandlerComponents);
      this.ws = ws;
      this.logger = org.slf4j.LoggerFactory.getLogger("testLogger");
    }

    public void setLogger(Logger logger) {
      this.logger = logger;
    }

    // #ws-request-filter
    public CompletionStage<Result> index() {
      WSRequestFilter filter =
          executor ->
              request -> {
                logger.debug("url = " + request.getUrl());
                return executor.apply(request);
              };

      return ws.url(feedUrl)
          .setRequestFilter(filter)
          .get()
          .thenApply(
              (WSResponse r) -> {
                String title = r.getBody(json()).findPath("title").asText();
                return ok("Feed title: " + title);
              });
    }
    // #ws-request-filter
  }

  // #ws-custom-body-readable
  public interface URLBodyReadables {
    default BodyReadable<java.net.URL> url() {
      return response -> {
        try {
          String s = response.getBody();
          return java.net.URI.create(s).toURL();
        } catch (MalformedURLException e) {
          throw new RuntimeException(e);
        }
      };
    }
  }
  // #ws-custom-body-readable

  // #ws-custom-body-writable
  public interface URLBodyWritables {
    default InMemoryBodyWritable body(java.net.URL url) {
      try {
        String s = url.toURI().toString();
        ByteString byteString = ByteString.fromString(s);
        return new InMemoryBodyWritable(byteString, "text/plain");
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    }
  }
  // #ws-custom-body-writable

  public static class Controller4 extends MockJavaAction {
    private final WSClient ws;
    private final Futures futures;
    private Logger logger;
    Executor customExecutionContext = ForkJoinPool.commonPool();

    @Inject
    public Controller4(JavaHandlerComponents javaHandlerComponents, WSClient ws, Futures futures) {
      super(javaHandlerComponents);
      this.ws = ws;
      this.futures = futures;
      this.logger = org.slf4j.LoggerFactory.getLogger("testLogger");
    }

    // #ws-futures-timeout
    public CompletionStage<Result> index() {
      CompletionStage<Result> f =
          futures.timeout(
              ws.url("http://playframework.com")
                  .get()
                  .thenApplyAsync(
                      result -> {
                        try {
                          Thread.sleep(10000L);
                          return Results.ok();
                        } catch (InterruptedException e) {
                          return Results.status(SERVICE_UNAVAILABLE);
                        }
                      },
                      customExecutionContext),
              1L,
              TimeUnit.SECONDS);

      return f.handleAsync(
          (result, e) -> {
            if (e != null) {
              if (e instanceof CompletionException) {
                Throwable completionException = e.getCause();
                if (completionException instanceof TimeoutException) {
                  return Results.status(SERVICE_UNAVAILABLE, "Service has timed out");
                } else {
                  return internalServerError(e.getMessage());
                }
              } else {
                logger.error("Unknown exception " + e.getMessage(), e);
                return internalServerError(e.getMessage());
              }
            } else {
              return result;
            }
          });
    }
    // #ws-futures-timeout
  }
}
