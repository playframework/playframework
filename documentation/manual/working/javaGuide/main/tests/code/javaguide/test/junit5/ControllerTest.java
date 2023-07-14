/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.test.junit5;

// #test-controller-test
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.*;

import javaguide.test.junit5.controllers.HomeController;
import org.junit.jupiter.api.Test;
import play.mvc.Result;
import play.twirl.api.Content;

class ControllerTest {
  @Test
  void testIndex() {
    Result result = new HomeController().index();
    assertEquals(OK, result.status());
    assertEquals("text/html", result.contentType().get());
    assertEquals("utf-8", result.charset().get());
    assertTrue(contentAsString(result).contains("Welcome"));
  }

  // ###replace: }
  // #test-controller-test

  // #test-template
  @Test
  void renderTemplate() {
    // ###replace:     Content html = views.html.index.render("Welcome to Play!");
    Content html = javaguide.test.junit5.html.index.render("Welcome to Play!");
    assertEquals("text/html", html.contentType());
    assertTrue(contentAsString(html).contains("Welcome to Play!"));
  }
  // #test-template

}
