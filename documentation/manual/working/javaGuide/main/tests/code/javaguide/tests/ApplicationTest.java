package javaguide.tests;

//#test-controller-test
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.GET;
import static play.test.Helpers.charset;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;
import static play.test.Helpers.route;
import static play.test.Helpers.status;
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
        ImmutableMap.of("application.router", "javaguide.tests.Routes"), new ArrayList<String>(), null);
  }

  @Test
  public void testIndex() {
    Result result = new Application().index();
    assertEquals(OK, status(result));
    assertEquals("text/html", contentType(result));
    assertEquals("utf-8", charset(result));
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
    assertEquals(OK, status(result));
  }
  //#test-controller-routes
  
  //#test-template
  @Test
  public void renderTemplate() {
    Content html = javaguide.tests.html.index.render("Welcome to Play!");
    assertEquals("text/html", contentType(html));
    assertTrue(contentAsString(html).contains("Welcome to Play!"));
  }
  //#test-template

}
