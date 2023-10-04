/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.ehcache;

import static javaguide.testhelpers.MockJavaActionHelper.call;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javaguide.testhelpers.MockJavaAction;
import org.apache.pekko.Done;
import org.junit.Ignore;
import org.junit.Test;
import play.Application;
import play.cache.AsyncCacheApi;
import play.cache.Cached;
import play.core.j.JavaHandlerComponents;
import play.mvc.*;
import play.test.WithApplication;

public class JavaEhCache extends WithApplication {

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
    assertThat(block(news).get(), equalTo(frontPageNews));
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
    assertThat(cache.sync().get("item.key"), equalTo(Optional.empty()));
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

  @Ignore
  @Test
  public void http() {
    AsyncCacheApi cache = app.injector().instanceOf(AsyncCacheApi.class);

    assertThat(
        contentAsString(
            call(new Controller1(instanceOf(JavaHandlerComponents.class)), fakeRequest(), mat)),
        equalTo("Hello world"));
    assertThat(cache.sync().get("homePage").get(), notNullValue());
    cache.set("homePage", Results.ok("something else"));
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
