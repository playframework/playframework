/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.tests;

// #test-controller-test
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.*;

import javaguide.tests.controllers.HomeController;

import org.junit.Test;

import play.mvc.Result;
import play.twirl.api.Content;

public class ControllerTest {

  @Test
  public void testIndex() {
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
  public void renderTemplate() {
    // ###replace:     Content html = views.html.index.render("Welcome to Play!");
    Content html = javaguide.tests.html.index.render("Welcome to Play!");
    assertEquals("text/html", html.contentType());
    assertTrue(contentAsString(html).contains("Welcome to Play!"));
  }
  // #test-template

}
