/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.POST;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Terminated;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.util.ByteString;
import org.junit.Test;
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
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.path()).isEqualTo("/");
  }

  @Test
  public void shouldCreateAFakeRequestWithMethodAndUri() {
    Http.RequestImpl request = Helpers.fakeRequest("POST", "/my-uri").build();
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.path()).isEqualTo("/my-uri");
  }

  @Test
  public void shouldAddHostHeaderToFakeRequests() {
    Http.RequestImpl request = Helpers.fakeRequest().build();
    assertThat(request.host()).isEqualTo("localhost");
  }

  @Test
  public void shouldCreateFakeApplicationsWithAnInMemoryDatabase() {
    Application application = Helpers.fakeApplication(Helpers.inMemoryDatabase());
    assertThat(application.config().getString("db.default.driver")).isNotNull();
    assertThat(application.config().getString("db.default.url")).isNotNull();
  }

  @Test
  public void shouldCreateFakeApplicationsWithAnNamedInMemoryDatabase() {
    Application application = Helpers.fakeApplication(Helpers.inMemoryDatabase("testDb"));
    assertThat(application.config().getString("db.testDb.driver")).isNotNull();
    assertThat(application.config().getString("db.testDb.url")).isNotNull();
  }

  @Test
  public void shouldCreateFakeApplicationsWithAnNamedInMemoryDatabaseAndConnectionOptions() {
    Map<String, String> options = new HashMap<>();
    options.put("username", "testUsername");
    options.put("ttl", "10");

    Application application = Helpers.fakeApplication(Helpers.inMemoryDatabase("testDb", options));
    assertThat(application.config().getString("db.testDb.driver")).isNotNull();
    assertThat(application.config().getString("db.testDb.url")).isNotNull();
    assertThat(application.config().getString("db.testDb.url")).contains("username");
    assertThat(application.config().getString("db.testDb.url")).contains("ttl");
  }

  @Test
  public void shouldExtractContentAsBytesFromAResult() {
    Result result = Results.ok("Test content");
    ByteString contentAsBytes = Helpers.contentAsBytes(result);
    assertThat(contentAsBytes).isEqualTo(ByteString.fromString("Test content"));
  }

  @Test
  public void shouldExtractContentAsBytesFromAResultUsingAMaterializer() throws Exception {
    ActorSystem actorSystem = ActorSystem.create("TestSystem");

    try {
      Materializer mat = Materializer.matFromSystem(actorSystem);

      Result result = Results.ok("Test content");
      ByteString contentAsBytes = Helpers.contentAsBytes(result, mat);
      assertThat(contentAsBytes).isEqualTo(ByteString.fromString("Test content"));
    } finally {
      Future<Terminated> future = actorSystem.terminate();
      Await.result(future, Duration.create("5s"));
    }
  }

  @Test
  public void shouldExtractContentAsBytesFromTwirlContent() {
    Content content = Html.apply("Test content");
    ByteString contentAsBytes = Helpers.contentAsBytes(content);
    assertThat(contentAsBytes).isEqualTo(ByteString.fromString("Test content"));
  }

  @Test
  public void shouldExtractContentAsStringFromTwirlContent() {
    Content content = Html.apply("Test content");
    String contentAsString = Helpers.contentAsString(content);
    assertThat(contentAsString).isEqualTo("Test content");
  }

  @Test
  public void shouldExtractContentAsStringFromAResult() {
    Result result = Results.ok("Test content");
    String contentAsString = Helpers.contentAsString(result);
    assertThat(contentAsString).isEqualTo("Test content");
  }

  @Test
  public void shouldExtractContentAsStringFromAResultUsingAMaterializer() throws Exception {
    ActorSystem actorSystem = ActorSystem.create("TestSystem");

    try {
      Materializer mat = Materializer.matFromSystem(actorSystem);

      Result result = Results.ok("Test content");
      String contentAsString = Helpers.contentAsString(result, mat);
      assertThat(contentAsString).isEqualTo("Test content");
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
    assertThat(result.status()).isEqualTo(404);
  }

  @Test
  public void shouldSuccessfullyExecutePostRequestWithMultipartFormData() {
    Application app = Helpers.fakeApplication();
    Map<String, String[]> postParams = new java.util.HashMap<>();
    postParams.put("key", new String[] {"value"});
    Http.RequestBuilder request =
        new Http.RequestBuilder().method(POST).bodyMultipart(postParams, Collections.emptyList());
    Result result = Helpers.route(app, request);
    assertThat(result.status()).isEqualTo(404);
  }

  @Test
  public void shouldReturnProperHasBodyValueForFakeRequest() {
    // Does not set a Content-Length and also not a Transfer-Encoding header, sets null as body
    Http.Request request = Helpers.fakeRequest("POST", "/uri").build();
    assertThat(request.hasBody()).isFalse();
  }

  @Test
  public void shouldReturnProperHasBodyValueForEmptyRawBuffer() {
    // Does set a Content-Length header
    Http.Request request =
        Helpers.fakeRequest("POST", "/uri").bodyRaw(ByteString.emptyByteString()).build();
    assertThat(request.hasBody()).isFalse();
  }

  @Test
  public void shouldReturnProperHasBodyValueForNonEmptyRawBuffer() {
    // Does set a Content-Length header
    Http.Request request =
        Helpers.fakeRequest("POST", "/uri").bodyRaw(ByteString.fromString("a")).build();
    assertThat(request.hasBody()).isTrue();
  }
}
