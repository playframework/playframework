/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.advanced.routing;

import org.junit.Test;

//#imports
import play.api.routing.Router;
import play.routing.RoutingDsl;
import play.libs.F;
import static play.mvc.Controller.*;
//#imports

import play.mvc.Result;
import play.test.WithApplication;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

public class JavaRoutingDsl extends WithApplication {
    @Test
    public void simple() {
        //#simple
        Router router = new RoutingDsl()
            .GET("/hello/:to").routeTo(to ->
                    ok("Hello " + to)
            )
            .build();
        //#simple

        assertThat(makeRequest(router, "GET", "/hello/world"), equalTo("Hello world"));
    }

    @Test
    public void fullPath() {
        //#full-path
        Router router = new RoutingDsl()
            .GET("/assets/*file").routeTo(file ->
                ok("Serving " + file)
            )
            .build();
        //#full-path

        assertThat(makeRequest(router, "GET", "/assets/javascripts/main.js"), equalTo("Serving javascripts/main.js"));
    }

    @Test
    public void regexp() {
        //#regexp
        Router router = new RoutingDsl()
            .GET("/api/items/$id<[0-9]+>").routeTo(id ->
                ok("Getting item " + id)
            )
            .build();
        //#regexp

        assertThat(makeRequest(router, "GET", "/api/items/23"), equalTo("Getting item 23"));
    }

    @Test
    public void integer() {
        //#integer
        Router router = new RoutingDsl()
            .GET("/api/items/:id").routeTo((Integer id) ->
                ok("Getting item " + id)
            )
            .build();
        //#integer

        assertThat(makeRequest(router, "GET", "/api/items/23"), equalTo("Getting item 23"));
    }

    @Test
    public void async() {
        //#async
        Router router = new RoutingDsl()
            .GET("/api/items/:id").routeAsync((Integer id) ->
                F.Promise.pure(ok("Getting item " + id))
            )
            .build();
        //#async

        assertThat(makeRequest(router, "GET", "/api/items/23"), equalTo("Getting item 23"));
    }

    private String makeRequest(Router router, String method, String path) {
        Result result = routeAndCall(router, fakeRequest(method, path));
        if (result == null) {
            return null;
        } else {
            return contentAsString(result);
        }
    }
}
