/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.cache;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import play.Application;
import play.cache.CacheApi;
import play.cache.Cached;
import play.mvc.*;
import play.test.WithApplication;

import javaguide.testhelpers.MockJavaAction;
import javaguide.testhelpers.MockJavaActionHelper;

import java.util.Arrays;
import java.util.concurrent.Callable;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

public class JavaCache extends WithApplication {

    @Override
    protected Application provideApplication() {
        return fakeApplication(ImmutableMap.of("play.cache.bindCaches", Arrays.asList("session-cache")));
    }

    public class News {}

    @Test
    public void inject() {
        // Check that we can instantiate it
        app.injector().instanceOf(javaguide.cache.inject.Application.class);
        // Check that we can instantiate the qualified one
        app.injector().instanceOf(javaguide.cache.qualified.Application.class);
    }

    @Test
    public void simple() {
        CacheApi cache = app.injector().instanceOf(CacheApi.class);

        News frontPageNews = new News();
        //#simple-set
        cache.set("item.key", frontPageNews);
        //#simple-set
        //#time-set
        // Cache for 15 minutes
        cache.set("item.key", frontPageNews, 60 * 15);
        //#time-set
        //#get
        News news = cache.get("item.key");
        //#get
        assertThat(news, equalTo(frontPageNews));
        //#get-or-else
        News maybeCached = cache.getOrElse("item.key", () -> lookUpFrontPageNews());
        //#get-or-else
        //#remove
        cache.remove("item.key");
        //#remove
        assertThat(cache.get("item.key"), nullValue());
    }

    private News lookUpFrontPageNews() {
        return new News();
    }

    public static class Controller1 extends MockJavaAction {
        //#http
        @Cached(key = "homePage")
        public Result index() {
            return ok("Hello world");
        }
        //#http
    }

    @Test
    public void http() {
        CacheApi cache = app.injector().instanceOf(CacheApi.class);

        assertThat(contentAsString(MockJavaActionHelper.call(new Controller1(), fakeRequest())), equalTo("Hello world"));
        assertThat(cache.get("homePage"), notNullValue());
        cache.set("homePage", Results.ok("something else"));
        assertThat(contentAsString(MockJavaActionHelper.call(new Controller1(), fakeRequest())), equalTo("something else"));
    }
}
