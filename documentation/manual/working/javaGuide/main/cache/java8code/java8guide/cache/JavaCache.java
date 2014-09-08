/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package java8guide.cache;

import org.junit.Test;
import play.cache.CacheApi;
import play.test.WithApplication;

import static org.junit.Assert.*;

public class JavaCache extends WithApplication {

    public class News {}

    private News lookUpFrontPageNews() {
        return new News();
    }

    @Test
    public void getOrElse() {
        CacheApi cache = app.getWrappedApplication().injector().instanceOf(CacheApi.class);

        //#get-or-else
        News maybeCached = cache.getOrElse("item.key", () -> lookUpFrontPageNews());
        //#get-or-else

        assertNotNull(maybeCached);
    }
}