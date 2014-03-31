package javaguide.tests;

import java.util.HashMap;
import java.util.Map;

import org.junit.*;

import play.mvc.*;
import play.test.*;
import play.libs.F.*;
import play.libs.ws.*;
import play.test.Helpers;

import static play.test.Helpers.*;
import static org.fest.assertions.Assertions.*;

public class JavaFunctionalTest extends WithApplication {

    @Before
    public void setUp() throws Exception {
        start();
    }

    //#render
    @Test
    public void renderTemplate() {
      Content html = javaguide.tests.html.index.render("Coco");
      assertThat(contentType(html)).isEqualTo("text/html");
      assertThat(contentAsString(html)).contains("Hello Coco");
    }
    //#render

    //#action-ref
    @Test
    public void callIndex() {
        Result result = callAction(
            javaguide.tests.controllers.routes.ref.Application.index("Kiki")
        );
        assertThat(status(result)).isEqualTo(OK);
        assertThat(contentType(result)).isEqualTo("text/html");
        assertThat(charset(result)).isEqualTo("utf-8");
        assertThat(contentAsString(result)).contains("Hello Kiki");
    }
    //#action-ref

    //#bad-route
    @Test
    public void badRoute() {
        Result result = route(fakeRequest(GET, "/xx/Kiki"));
        assertThat(result).isNull();
    }
    //#bad-route

    int timeout = 5000;

    private TestServer testServer(int port) {
        Map<String, String> config = new HashMap<String, String>();
        config.put("application.router", "javaguide.tests.Routes");
        return Helpers.testServer(port, fakeApplication(config));
    }

    //#test-server
    @Test
    public void testInServer() {
        running(testServer(3333), new Runnable() {
            public void run() {
                assertThat(
                    WS.url("http://localhost:3333").get().get(timeout).getStatus()
                ).isEqualTo(OK);
            }
        });
    }
    //#test-server

    //#with-browser
    @Test
    public void runInBrowser() {
        running(testServer(3333), HTMLUNIT, new Callback<TestBrowser>() {
            public void invoke(TestBrowser browser) {
                browser.goTo("http://localhost:3333");
                assertThat(browser.$("#title").getText()).isEqualTo("Hello Guest");
                browser.$("a").click();
                assertThat(browser.url()).isEqualTo("http://localhost:3333/Coco");
                assertThat(browser.$("#title", 0).getText()).isEqualTo("Hello Coco");
            }
        });
    }
    //#with-browser
}
