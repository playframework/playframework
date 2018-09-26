/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test;

import akka.actor.ActorSystem;
import akka.actor.Terminated;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.util.ByteString;
import org.hamcrest.CoreMatchers;
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

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class HelpersTest {

    @Test
    public void shouldCreateASimpleFakeRequest() {
        Http.RequestImpl request = Helpers.fakeRequest().build();
        assertThat(request.method(), equalTo("GET"));
        assertThat(request.path(), equalTo("/"));
    }

    @Test
    public void shouldCreateAFakeRequestWithMethodAndUri() {
        Http.RequestImpl request = Helpers.fakeRequest("POST", "/my-uri").build();
        assertThat(request.method(), equalTo("POST"));
        assertThat(request.path(), equalTo("/my-uri"));
    }

    @Test
    public void shouldAddHostHeaderToFakeRequests() {
        Http.RequestImpl request = Helpers.fakeRequest().build();
        assertThat(request.host(), equalTo("localhost"));
    }

    @Test
    public void shouldCreateFakeApplicationsWithAnInMemoryDatabase() {
        Application application = Helpers.fakeApplication(Helpers.inMemoryDatabase());
        assertThat(application.config().getString("db.default.driver"), CoreMatchers.notNullValue());
        assertThat(application.config().getString("db.default.url"), CoreMatchers.notNullValue());
    }

    @Test
    public void shouldCreateFakeApplicationsWithAnNamedInMemoryDatabase() {
        Application application = Helpers.fakeApplication(Helpers.inMemoryDatabase("testDb"));
        assertThat(application.config().getString("db.testDb.driver"), CoreMatchers.notNullValue());
        assertThat(application.config().getString("db.testDb.url"), CoreMatchers.notNullValue());
    }

    @Test
    public void shouldCreateFakeApplicationsWithAnNamedInMemoryDatabaseAndConnectionOptions() {
        Map<String, String> options = new HashMap<>();
        options.put("username", "testUsername");
        options.put("ttl", "10");

        Application application = Helpers.fakeApplication(Helpers.inMemoryDatabase("testDb", options));
        assertThat(application.config().getString("db.testDb.driver"), CoreMatchers.notNullValue());
        assertThat(application.config().getString("db.testDb.url"), CoreMatchers.notNullValue());
        assertThat(application.config().getString("db.testDb.url"), CoreMatchers.containsString("username"));
        assertThat(application.config().getString("db.testDb.url"), CoreMatchers.containsString("ttl"));
    }

    @Test
    public void shouldExtractContentAsBytesFromAResult() {
        Result result = Results.ok("Test content");
        ByteString contentAsBytes = Helpers.contentAsBytes(result);
        assertThat(contentAsBytes, equalTo(ByteString.fromString("Test content")));
    }

    @Test
    public void shouldExtractContentAsBytesFromAResultUsingAMaterializer() throws Exception {
        ActorSystem actorSystem = ActorSystem.create("TestSystem");

        try {
            Materializer mat = ActorMaterializer.create(actorSystem);

            Result result = Results.ok("Test content");
            ByteString contentAsBytes = Helpers.contentAsBytes(result, mat);
            assertThat(contentAsBytes, equalTo(ByteString.fromString("Test content")));
        } finally {
            Future<Terminated> future = actorSystem.terminate();
            Await.result(future, Duration.create("5s"));
        }

    }

    @Test
    public void shouldExtractContentAsBytesFromTwirlContent() {
        Content content = Html.apply("Test content");
        ByteString contentAsBytes = Helpers.contentAsBytes(content);
        assertThat(contentAsBytes, equalTo(ByteString.fromString("Test content")));
    }

    @Test
    public void shouldExtractContentAsStringFromTwirlContent() {
        Content content = Html.apply("Test content");
        String contentAsString = Helpers.contentAsString(content);
        assertThat(contentAsString, equalTo("Test content"));
    }

    @Test
    public void shouldExtractContentAsStringFromAResult() {
        Result result = Results.ok("Test content");
        String contentAsString = Helpers.contentAsString(result);
        assertThat(contentAsString, equalTo("Test content"));
    }

    @Test
    public void shouldExtractContentAsStringFromAResultUsingAMaterializer() throws Exception {
        ActorSystem actorSystem = ActorSystem.create("TestSystem");

        try {
            Materializer mat = ActorMaterializer.create(actorSystem);

            Result result = Results.ok("Test content");
            String contentAsString = Helpers.contentAsString(result, mat);
            assertThat(contentAsString, equalTo("Test content"));
        } finally {
            Future<Terminated> future = actorSystem.terminate();
            Await.result(future, Duration.create("5s"));
        }

    }
}
