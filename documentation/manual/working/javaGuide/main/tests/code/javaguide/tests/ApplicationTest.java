package javaguide.tests;

//#test-controller-test
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.GET;
import static play.test.Helpers.callAction;
import static play.test.Helpers.charset;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;
import static play.test.Helpers.status;
import javaguide.tests.controllers.Application;

import org.junit.Test;

import play.mvc.Result;
import play.test.FakeRequest;
import play.twirl.api.Content;

public class ApplicationTest {
  
  @Test
  public void testIndex() {
    Result result = Application.index();
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
    Result result = callAction(
      //###replace:     controllers.routes.ref.Application.index(),
      javaguide.tests.controllers.routes.ref.Application.index(),
      new FakeRequest(GET, "/")
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
