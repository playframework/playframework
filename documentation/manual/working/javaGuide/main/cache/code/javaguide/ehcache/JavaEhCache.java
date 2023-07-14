/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.ehcache;

import static javaguide.testhelpers.MockJavaActionHelper.call;
import static org.junit.jupiter.api.Assertions.*;
import static play.test.Helpers.*;

import akka.Done;
import akka.stream.Materializer;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javaguide.testhelpers.MockJavaAction;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import play.Application;
import play.cache.AsyncCacheApi;
import play.cache.Cached;
import play.core.j.JavaHandlerComponents;
import play.mvc.*;
import play.test.junit5.ApplicationExtension;

public class JavaEhCache {

  static ApplicationExtension appExtension =
      new ApplicationExtension(
          fakeApplication(
              ImmutableMap.of(
                  "play.cache.bindCaches", Collections.singletonList("session-cache"))));
  static Application app = appExtension.getApplication();
  static Materializer mat = appExtension.getMaterializer();

  private class News {}

  @Test
  void inject() {
    // Check that we can instantiate it
    assertDoesNotThrow(() -> app.injector().instanceOf(javaguide.cache.inject.Application.class));
    // Check that we can instantiate the qualified one
    assertDoesNotThrow(
        () -> app.injector().instanceOf(javaguide.cache.qualified.Application.class));
  }

  @Test
  void simple() {
    AsyncCacheApi cache = app.injector().instanceOf(AsyncCacheApi.class);

    News frontPageNews = new News();
    {
      // #simple-set
      CompletionStage<Done> result = cache.set("item.key", frontPageNews);
      // #simple-set
      block(result);
    }
    {
      // #time-set
      // Cache for 15 minutes
      CompletionStage<Done> result = cache.set("item.key", frontPageNews, 60 * 15);
      // #time-set
      block(result);
    }
    // #get
    CompletionStage<Optional<News>> news = cache.get("item.key");
    // #get
    assertEquals(frontPageNews, block(news).get());
    // #get-or-else
    CompletionStage<News> maybeCached =
        cache.getOrElseUpdate("item.key", this::lookUpFrontPageNews);
    // #get-or-else
    assertEquals(frontPageNews, block(maybeCached));
    {
      // #remove
      CompletionStage<Done> result = cache.remove("item.key");
      // #remove

      // #removeAll
      CompletionStage<Done> resultAll = cache.removeAll();
      // #removeAll
      block(result);
    }
    assertEquals(Optional.empty(), cache.sync().get("item.key"));
  }

  private CompletionStage<News> lookUpFrontPageNews() {
    return CompletableFuture.completedFuture(new News());
  }

  public static class Controller1 extends MockJavaAction {

    Controller1(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    // #http
    @Cached(key = "homePage")
    public Result index() {
      return ok("Hello world");
    }
    // #http
  }

  @Disabled("Flaky EHCache test")
  @Test
  void http() {
    AsyncCacheApi cache = app.injector().instanceOf(AsyncCacheApi.class);

    assertEquals(
        "Hello world",
        contentAsString(
            call(
                new Controller1(app.injector().instanceOf(JavaHandlerComponents.class)),
                fakeRequest(),
                mat)));
    assertNotNull(cache.sync().get("homePage").get());
    cache.set("homePage", Results.ok("something else"));
    assertEquals(
        "something else",
        contentAsString(
            call(
                new Controller1(app.injector().instanceOf(JavaHandlerComponents.class)),
                fakeRequest(),
                mat)));
  }

  private static <T> T block(CompletionStage<T> stage) {
    try {
      return stage.toCompletableFuture().get();
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }
}
