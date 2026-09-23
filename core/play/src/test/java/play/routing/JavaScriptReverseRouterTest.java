/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.routing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import play.api.routing.JavaScriptReverseRoute;
import play.twirl.api.JavaScript;

public class JavaScriptReverseRouterTest {

  @Test
  public void createShouldUseTheHostWithoutAddingAnAjaxHelper() {
    JavaScriptReverseRoute route =
        new JavaScriptReverseRoute(
            "controllers.FooController.foo", "function(foo) { return null; }");

    JavaScript router = JavaScriptReverseRouter.create("foobarRoutes", "foobar.com", route);

    assertTrue(router.body().contains("var foobarRoutes = "));
    assertTrue(router.body().contains("'foobar.com'"));
    assertFalse(router.body().contains("ajax:function"));
  }
}
