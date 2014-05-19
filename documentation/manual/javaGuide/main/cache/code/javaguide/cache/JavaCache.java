/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.cache;

import org.junit.Before;
import org.junit.Test;
import play.cache.Cache;
import play.cache.Cached;
import play.mvc.*;
import play.test.WithApplication;

import javaguide.testhelpers.MockJavaAction;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

public class JavaCache extends WithApplication {

    public class News {}

    @Test
    public void simple() {
        News frontPageNews = new News();
        //#simple-set
        Cache.set("item.key", frontPageNews);
        //#simple-set
        //#time-set
        // Cache for 15 minutes
        Cache.set("item.key", frontPageNews, 60 * 15);
        //#time-set
        //#get
        News news = (News) Cache.get("item.key");
        //#get
        assertThat(news, equalTo(frontPageNews));
        //#remove
        Cache.remove("item.key");
        //#remove
        assertThat(Cache.get("item.key"), nullValue());
    }

    public static class Controller1 extends MockJavaAction {
        //#http
        @Cached(key = "homePage")
        public static Result index() {
            return ok("Hello world");
        }
        //#http
    }

    @Test
    public void http() {
        assertThat(contentAsString(MockJavaAction.call(new Controller1(), fakeRequest())), equalTo("Hello world"));
        assertThat(Cache.get("homePage"), notNullValue());
        Cache.set("homePage", Results.ok("something else"));
        assertThat(contentAsString(MockJavaAction.call(new Controller1(), fakeRequest())), equalTo("something else"));
    }
}
