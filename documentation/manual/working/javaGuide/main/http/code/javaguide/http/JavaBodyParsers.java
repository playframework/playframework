/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.http;

import static javaguide.testhelpers.MockJavaActionHelper.*;
import static org.junit.jupiter.api.Assertions.*;
import static play.test.Helpers.*;

import akka.stream.Materializer;
import akka.stream.javadsl.*;
import akka.util.ByteString;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import javaguide.testhelpers.MockJavaAction;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import play.Application;
import play.core.j.JavaHandlerComponents;
import play.http.HttpErrorHandler;
import play.libs.F;
import play.libs.Json;
import play.libs.streams.Accumulator;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import play.mvc.*;
import play.mvc.Http.*;
import play.test.junit5.ApplicationExtension;

public class JavaBodyParsers {

  @RegisterExtension
  static ApplicationExtension appExtension = new ApplicationExtension(fakeApplication());

  static Application app = appExtension.getApplication();
  static Materializer mat = appExtension.getMaterializer();

  @Test
  void accessRequestBody() {
    assertTrue(
        contentAsString(
                call(
                    new MockJavaAction(app.injector().instanceOf(JavaHandlerComponents.class)) {
                      // #access-json-body
                      public Result index(Http.Request request) {
                        JsonNode json = request.body().asJson();
                        return ok("Got name: " + json.get("name").asText());
                      }
                      // #access-json-body
                    },
                    fakeRequest("POST", "/")
                        .bodyJson(Json.toJson(Collections.singletonMap("name", "foo"))),
                    mat))
            .contains("foo"));
  }

  @Test
  void particularBodyParser() {
    assertTrue(
        contentAsString(
                call(
                    new MockJavaAction(app.injector().instanceOf(JavaHandlerComponents.class)) {
                      // #particular-body-parser
                      @BodyParser.Of(BodyParser.Text.class)
                      public Result index(Http.Request request) {
                        RequestBody body = request.body();
                        return ok("Got text: " + body.asText());
                      }
                      // #particular-body-parser
                    },
                    fakeRequest().bodyText("foo"),
                    mat))
            .contains("foo"));
  }

  public abstract static class BodyParserApply<A> implements BodyParser<A> {
    // Override the method with another abstract method - if the signature changes, we get a compile
    // error
    @Override
    // #body-parser-apply
    public abstract Accumulator<ByteString, F.Either<Result, A>> apply(RequestHeader request);
    // #body-parser-apply
  }

  static class User {
    public String name;
  }

  // #composing-class
  public static class UserBodyParser implements BodyParser<User> {

    private BodyParser.Json jsonParser;
    private Executor executor;

    @Inject
    public UserBodyParser(BodyParser.Json jsonParser, Executor executor) {
      this.jsonParser = jsonParser;
      this.executor = executor;
    }
    // #composing-class

    // #composing-apply
    public Accumulator<ByteString, F.Either<Result, User>> apply(RequestHeader request) {
      Accumulator<ByteString, F.Either<Result, JsonNode>> jsonAccumulator =
          jsonParser.apply(request);
      return jsonAccumulator.map(
          resultOrJson -> {
            if (resultOrJson.left.isPresent()) {
              return F.Either.Left(resultOrJson.left.get());
            } else {
              JsonNode json = resultOrJson.right.get();
              try {
                User user = play.libs.Json.fromJson(json, User.class);
                return F.Either.Right(user);
              } catch (Exception e) {
                return F.Either.Left(
                    Results.badRequest("Unable to read User from json: " + e.getMessage()));
              }
            }
          },
          executor);
    }
    // #composing-apply
  }

  @Test
  void composingBodyParser() {
    assertEquals(
        "Got: foo",
        contentAsString(
            call(
                new MockJavaAction(app.injector().instanceOf(JavaHandlerComponents.class)) {
                  // #composing-access
                  @BodyParser.Of(UserBodyParser.class)
                  public Result save(Http.Request request) {
                    RequestBody body = request.body();
                    User user = body.as(User.class);

                    return ok("Got: " + user.name);
                  }
                  // #composing-access
                },
                fakeRequest().bodyJson(Json.toJson(Collections.singletonMap("name", "foo"))),
                mat)));
  }

  @Test
  void maxLength() {
    StringBuilder body = new StringBuilder();
    for (int i = 0; i < 1100; i++) {
      body.append("1234567890");
    }
    assertEquals(
        413,
        callWithStringBody(
                new MaxLengthAction(app.injector().instanceOf(JavaHandlerComponents.class)),
                fakeRequest(),
                body.toString(),
                mat)
            .status());
  }

  public static class MaxLengthAction extends MockJavaAction {

    MaxLengthAction(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    // #max-length
    // Accept only 10KB of data.
    public static class Text10Kb extends BodyParser.Text {
      @Inject
      public Text10Kb(HttpErrorHandler errorHandler) {
        super(10 * 1024, errorHandler);
      }
    }

    @BodyParser.Of(Text10Kb.class)
    public Result index(Http.Request request) {
      return ok("Got body: " + request.body().asText());
    }
    // #max-length
  }

  // #forward-body
  public static class ForwardingBodyParser implements BodyParser<WSResponse> {
    private WSClient ws;
    private Executor executor;

    @Inject
    public ForwardingBodyParser(WSClient ws, Executor executor) {
      this.ws = ws;
      this.executor = executor;
    }

    String url = "http://example.com";

    public Accumulator<ByteString, F.Either<Result, WSResponse>> apply(RequestHeader request) {
      Accumulator<ByteString, Source<ByteString, ?>> forwarder = Accumulator.source();

      return forwarder.mapFuture(
          source -> {
            // TODO: when streaming upload has been implemented, pass the source as the body
            return ws.url(url)
                .setMethod("POST")
                // .setBody(source)
                .execute()
                .thenApply(F.Either::Right);
          },
          executor);
    }
  }
  // #forward-body
  // no test for forwarding yet because it doesn't actually work yet

  // #csv
  public static class CsvBodyParser implements BodyParser<List<List<String>>> {
    private Executor executor;

    @Inject
    public CsvBodyParser(Executor executor) {
      this.executor = executor;
    }

    @Override
    public Accumulator<ByteString, F.Either<Result, List<List<String>>>> apply(
        RequestHeader request) {
      // A flow that splits the stream into CSV lines
      Sink<ByteString, CompletionStage<List<List<String>>>> sink =
          Flow.<ByteString>create()
              // We split by the new line character, allowing a maximum of 1000 characters per line
              .via(Framing.delimiter(ByteString.fromString("\n"), 1000, FramingTruncation.ALLOW))
              // Turn each line to a String and split it by commas
              .map(
                  bytes -> {
                    String[] values = bytes.utf8String().trim().split(",");
                    return Arrays.asList(values);
                  })
              // Now we fold it into a list
              .toMat(
                  Sink.<List<List<String>>, List<String>>fold(
                      new ArrayList<>(),
                      (list, values) -> {
                        list.add(values);
                        return list;
                      }),
                  Keep.right());

      // Convert the body to a Right either
      return Accumulator.fromSink(sink).map(F.Either::Right, executor);
    }
  }
  // #csv

  @Test
  @SuppressWarnings("unchecked")
  void testCsv() {
    assertEquals(
        "Got: foo",
        contentAsString(
            call(
                new MockJavaAction(app.injector().instanceOf(JavaHandlerComponents.class)) {
                  @BodyParser.Of(CsvBodyParser.class)
                  public Result uploadCsv(Http.Request request) {
                    String value =
                        ((List<List<String>>) request.body().as(List.class)).get(1).get(2);
                    return ok("Got: " + value);
                  }
                },
                fakeRequest().bodyText("1,2\n3,4,foo\n5,6"),
                mat)));
  }
}
