/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.http;

import static javaguide.testhelpers.MockJavaActionHelper.*;
import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.*;

import java.util.Collections;
import java.util.List;
import javaguide.testhelpers.MockJavaAction;
import org.junit.Test;
import play.core.j.JavaHandlerComponents;
import play.libs.Json;
import play.mvc.*;
import play.test.WithApplication;

public class JavaContentNegotiation extends WithApplication {

  @Test
  public void negotiateContent() {
    assertThat(
            contentAsString(
                call(
                    new MockJavaAction(instanceOf(JavaHandlerComponents.class)) {
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
                    mat)))
        .isEqualTo("html list of items");
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
