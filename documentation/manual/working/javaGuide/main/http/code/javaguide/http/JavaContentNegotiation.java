/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.http;

import org.junit.Test;
import play.libs.Json;
import play.test.WithApplication;
import javaguide.testhelpers.MockJavaAction;
import play.mvc.*;

import java.util.Arrays;
import java.util.List;

import static javaguide.testhelpers.MockJavaActionHelper.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

public class JavaContentNegotiation extends WithApplication {

    @Test
    public void negotiateContent() {
        assertThat(contentAsString(call(new MockJavaAction() {
                    //#negotiate-content
                    public Result list() {
                        List<Item> items = Item.find.all();
                        if (request().accepts("text/html")) {
                            return ok(views.html.Application.list.render(items));
                        } else {
                            return ok(Json.toJson(items));
                        }
                    }
                    //#negotiate-content
                }, fakeRequest().header("Accept", "text/html"))),
                equalTo("html list of items"));
    }

    public static class Item {
        static Find find = new Find();

        Item(String id) {
            this.id = id;
        }

        public String id;
    }

    static class Find {
        List<Item> all() {
            return Arrays.asList(new Item("foo"));
        }
    }

    static class views {
        static class html {
            static class Application {
                static class list {
                    static String render(List<Item> items) {
                        return "html list of items";
                    }
                }
            }
        }
    }
}
