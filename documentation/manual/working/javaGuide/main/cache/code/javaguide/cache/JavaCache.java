/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.cache;

import akka.Done;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import play.Application;
import play.cache.AsyncCacheApi;
import play.cache.Cached;
import play.core.j.JavaHandlerComponents;
import play.mvc.*;
import play.test.WithApplication;

import javaguide.testhelpers.MockJavaAction;

import java.lang.Throwable;
import java.util.Collections;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletableFuture;

import static javaguide.testhelpers.MockJavaActionHelper.call;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

public class JavaCache extends WithApplication {

  @Override
  protected Application provideApplication() {
    return fakeApplication(
        ImmutableMap.of("play.cache.bindCaches", Collections.singletonList("session-cache")));
  }

  private class News {}

  @Test
  public void inject() {
    // Check that we can instantiate it
    app.injector().instanceOf(javaguide.cache.inject.Application.class);
    // Check that we can instantiate the qualified one
    app.injector().instanceOf(javaguide.cache.qualified.Application.class);
  }

  @Test
  public void simple() {
    AsyncCacheApi cache = app.injector().instanceOf(AsyncCacheApi.class);

    News frontPageNews = new News();
    {
      // #simple-set
      CompletionStage<Done> result = cache.set("item.key", frontPageNews);
      // #simple-set
      block(result);
      // #time-set
    }
    {
      // Cache for 15 minutes
      CompletionStage<Done> result = cache.set("item.key", frontPageNews, 60 * 15);
      // #time-set
      block(result);
    }
    // #get
    CompletionStage<News> news = cache.get("item.key");
    // #get
    assertThat(block(news), equalTo(frontPageNews));
    // #get-or-else
    CompletionStage<News> maybeCached =
        cache.getOrElseUpdate("item.key", this::lookUpFrontPageNews);
    // #get-or-else
    assertThat(block(maybeCached), equalTo(frontPageNews));
    {
      // #remove
      CompletionStage<Done> result = cache.remove("item.key");
      // #remove

      // #removeAll
      CompletionStage<Done> resultAll = cache.removeAll();
      // #removeAll
      block(result);
    }
    assertThat(cache.sync().get("item.key"), nullValue());
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

  @Test
  public void http() {
    AsyncCacheApi cache = app.injector().instanceOf(AsyncCacheApi.class);

    assertThat(
        contentAsString(
            call(new Controller1(instanceOf(JavaHandlerComponents.class)), fakeRequest(), mat)),
        equalTo("Hello world"));
    assertThat(cache.sync().get("homePage"), notNullValue());

    // Use sync API to ensure the cache is populated before executing the request.
    cache.sync().set("homePage", Results.ok("something else"));
    assertThat(
        contentAsString(
            call(new Controller1(instanceOf(JavaHandlerComponents.class)), fakeRequest(), mat)),
        equalTo("something else"));
  }

  private static <T> T block(CompletionStage<T> stage) {
    try {
      return stage.toCompletableFuture().get();
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }
}
