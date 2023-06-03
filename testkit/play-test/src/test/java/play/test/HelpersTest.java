/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test;

import static org.junit.jupiter.api.Assertions.*;
import static play.test.Helpers.POST;

import akka.actor.ActorSystem;
import akka.actor.Terminated;
import akka.stream.Materializer;
import akka.util.ByteString;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import play.Application;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import play.twirl.api.Content;
import play.twirl.api.Html;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class HelpersTest {

  @Test
  public void shouldCreateASimpleFakeRequest() {
    Http.RequestImpl request = Helpers.fakeRequest().build();
    assertEquals("GET", request.method());
    assertEquals("/", request.path());
  }

  @Test
  public void shouldCreateAFakeRequestWithMethodAndUri() {
    Http.RequestImpl request = Helpers.fakeRequest("POST", "/my-uri").build();
    assertEquals("POST", request.method());
    assertEquals("/my-uri", request.path());
  }

  @Test
  public void shouldAddHostHeaderToFakeRequests() {
    Http.RequestImpl request = Helpers.fakeRequest().build();
    assertEquals("localhost", request.host());
  }

  @Test
  public void shouldCreateFakeApplicationsWithAnInMemoryDatabase() {
    Application application = Helpers.fakeApplication(Helpers.inMemoryDatabase());
    assertNotNull(application.config().getString("db.default.driver"));
    assertNotNull(application.config().getString("db.default.url"));
  }

  @Test
  public void shouldCreateFakeApplicationsWithAnNamedInMemoryDatabase() {
    Application application = Helpers.fakeApplication(Helpers.inMemoryDatabase("testDb"));
    assertNotNull(application.config().getString("db.testDb.driver"));
    assertNotNull(application.config().getString("db.testDb.url"));
  }

  @Test
  public void shouldCreateFakeApplicationsWithAnNamedInMemoryDatabaseAndConnectionOptions() {
    Map<String, String> options = new HashMap<>();
    options.put("username", "testUsername");
    options.put("ttl", "10");

    Application application = Helpers.fakeApplication(Helpers.inMemoryDatabase("testDb", options));
    assertNotNull(application.config().getString("db.testDb.driver"));
    assertNotNull(application.config().getString("db.testDb.url"));
    assertTrue(application.config().getString("db.testDb.url").contains("username"));
    assertTrue(application.config().getString("db.testDb.url").contains("ttl"));
  }

  @Test
  public void shouldExtractContentAsBytesFromAResult() {
    Result result = Results.ok("Test content");
    ByteString contentAsBytes = Helpers.contentAsBytes(result);
    assertEquals(ByteString.fromString("Test content"), contentAsBytes);
  }

  @Test
  public void shouldExtractContentAsBytesFromAResultUsingAMaterializer() throws Exception {
    ActorSystem actorSystem = ActorSystem.create("TestSystem");

    try {
      Materializer mat = Materializer.matFromSystem(actorSystem);

      Result result = Results.ok("Test content");
      ByteString contentAsBytes = Helpers.contentAsBytes(result, mat);
      assertEquals(ByteString.fromString("Test content"), contentAsBytes);
    } finally {
      Future<Terminated> future = actorSystem.terminate();
      Await.result(future, Duration.create("5s"));
    }
  }

  @Test
  public void shouldExtractContentAsBytesFromTwirlContent() {
    Content content = Html.apply("Test content");
    ByteString contentAsBytes = Helpers.contentAsBytes(content);
    assertEquals(ByteString.fromString("Test content"), contentAsBytes);
  }

  @Test
  public void shouldExtractContentAsStringFromTwirlContent() {
    Content content = Html.apply("Test content");
    String contentAsString = Helpers.contentAsString(content);
    assertEquals("Test content", contentAsString);
  }

  @Test
  public void shouldExtractContentAsStringFromAResult() {
    Result result = Results.ok("Test content");
    String contentAsString = Helpers.contentAsString(result);
    assertEquals("Test content", contentAsString);
  }

  @Test
  public void shouldExtractContentAsStringFromAResultUsingAMaterializer() throws Exception {
    ActorSystem actorSystem = ActorSystem.create("TestSystem");

    try {
      Materializer mat = Materializer.matFromSystem(actorSystem);

      Result result = Results.ok("Test content");
      String contentAsString = Helpers.contentAsString(result, mat);
      assertEquals("Test content", contentAsString);
    } finally {
      Future<Terminated> future = actorSystem.terminate();
      Await.result(future, Duration.create("5s"));
    }
  }

  @Test
  public void shouldSuccessfullyExecutePostRequestWithEmptyBody() {
    Http.RequestBuilder request = Helpers.fakeRequest("POST", "/uri");
    Application app = Helpers.fakeApplication();

    Result result = Helpers.route(app, request);
    assertEquals(404, result.status());
  }

  @Test
  public void shouldSuccessfullyExecutePostRequestWithMultipartFormData() {
    Application app = Helpers.fakeApplication();
    Map<String, String[]> postParams = new java.util.HashMap<>();
    postParams.put("key", new String[] {"value"});
    Http.RequestBuilder request =
        new Http.RequestBuilder().method(POST).bodyMultipart(postParams, Collections.emptyList());
    Result result = Helpers.route(app, request);
    assertEquals(404, result.status());
  }

  @Test
  public void shouldReturnProperHasBodyValueForFakeRequest() {
    // Does not set a Content-Length and also not a Transfer-Encoding header, sets null as body
    Http.Request request = Helpers.fakeRequest("POST", "/uri").build();
    assertFalse(request.hasBody());
  }

  @Test
  public void shouldReturnProperHasBodyValueForEmptyRawBuffer() {
    // Does set a Content-Length header
    Http.Request request =
        Helpers.fakeRequest("POST", "/uri").bodyRaw(ByteString.emptyByteString()).build();
    assertFalse(request.hasBody());
  }

  @Test
  public void shouldReturnProperHasBodyValueForNonEmptyRawBuffer() {
    // Does set a Content-Length header
    Http.Request request =
        Helpers.fakeRequest("POST", "/uri").bodyRaw(ByteString.fromString("a")).build();
    assertTrue(request.hasBody());
  }
}
