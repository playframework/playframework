/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.http;

import static javaguide.testhelpers.MockJavaActionHelper.*;
import static org.junit.jupiter.api.Assertions.*;
import static play.test.Helpers.*;

import akka.stream.Materializer;
import java.util.Collections;
import java.util.List;
import javaguide.testhelpers.MockJavaAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import play.Application;
import play.core.j.JavaHandlerComponents;
import play.libs.Json;
import play.mvc.*;
import play.test.junit5.ApplicationExtension;

public class JavaContentNegotiation {

  @RegisterExtension
  static ApplicationExtension appExtension = new ApplicationExtension(fakeApplication());

  static Application app = appExtension.getApplication();
  static Materializer mat = appExtension.getMaterializer();

  @Test
  void negotiateContent() {
    assertEquals(
        "html list of items",
        contentAsString(
            call(
                new MockJavaAction(app.injector().instanceOf(JavaHandlerComponents.class)) {
                  // #negotiate-content
                  public Result list(Http.Request request) {
                    List<Item> items = Item.find.all();
                    if (request.accepts("text/html")) {
                      return ok(views.html.Application.list.render(items));
                    } else {
                      return ok(Json.toJson(items));
                    }
                  }
                  // #negotiate-content
                },
                fakeRequest().header("Accept", "text/html"),
                mat)));
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
      return Collections.singletonList(new Item("foo"));
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
