package javaguide.tests;

//#test-controller-test
import static org.fest.assertions.Assertions.assertThat;
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
    assertThat(status(result)).isEqualTo(OK);
    assertThat(contentType(result)).isEqualTo("text/html");
    assertThat(charset(result)).isEqualTo("utf-8");
    assertThat(contentAsString(result)).contains("Welcome");
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
    assertThat(status(result)).isEqualTo(OK);
  }
  //#test-controller-routes
  
  //#test-template
  @Test
  public void renderTemplate() {
    Content html = javaguide.tests.html.index.render("Welcome to Play!");
    assertThat(contentType(html)).isEqualTo("text/html");
    assertThat(contentAsString(html)).contains("Welcome to Play!");
  }
  //#test-template

}
