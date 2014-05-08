/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.http;

import org.junit.Before;
import org.junit.Test;
import play.libs.Json;
import play.test.WithApplication;
import javaguide.testhelpers.MockJavaAction;
import play.mvc.*;

import java.util.Arrays;
import java.util.List;

import static javaguide.testhelpers.MockJavaAction.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

public class JavaContentNegotiation extends WithApplication {

    @Test
    public void negotiateContent() {
        assertThat(contentAsString(call(new Controller1(), fakeRequest().withHeader("Accept", "text/html"))),
                equalTo("html list of items"));
    }

    public static class Controller1 extends MockJavaAction {
        //#negotiate-content
        public static Result list() {
            List<Item> items = Item.find.all();
            if (request().accepts("text/html")) {
                return ok(views.html.Application.list.render(items));
            } else {
                return ok(Json.toJson(items));
            }
        }
        //#negotiate-content
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

    static Views views = new Views();
    static class Views {
        Html html = new Html();
    }
    static class Html {
        Application Application = new Application();
    }
    static class Application {
        ListTemplate list = new ListTemplate();
    }
    static class ListTemplate {
        String render(List<Item> items) {
            return "html list of items";
        }
    }
}
