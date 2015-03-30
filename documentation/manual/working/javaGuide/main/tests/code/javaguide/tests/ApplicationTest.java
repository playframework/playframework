package javaguide.tests;

//#test-controller-test
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.GET;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.route;
import javaguide.tests.controllers.Application;

import java.util.ArrayList;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import play.mvc.Result;
import play.test.FakeApplication;
import play.test.Helpers;
import play.test.WithApplication;
import play.twirl.api.Content;

public class ApplicationTest extends WithApplication {
  
  @Override
  protected FakeApplication provideFakeApplication() {
    return new FakeApplication(new java.io.File("."), Helpers.class.getClassLoader(),
        ImmutableMap.of("play.http.router", "javaguide.tests.Routes"), new ArrayList<String>(), null);
  }

  @Test
  public void testIndex() {
    Result result = new Application().index();
    assertEquals(OK, result.status());
    assertEquals("text/html", result.contentType());
    assertEquals("utf-8", result.charset());
    assertTrue(contentAsString(result).contains("Welcome"));
  }

  //###replace: }
//#test-controller-test
  
  //#test-controller-routes
  @Test
  public void testCallIndex() {
    Result result = route(
      //###replace:     controllers.routes.Application.index(),
      javaguide.tests.controllers.routes.Application.index()
    );
    assertEquals(OK, result.status());
  }
  //#test-controller-routes
  
  //#test-template
  @Test
  public void renderTemplate() {
    Content html = javaguide.tests.html.index.render("Welcome to Play!");
    assertEquals("text/html", html.contentType());
    assertTrue(contentAsString(html).contains("Welcome to Play!"));
  }
  //#test-template

}
