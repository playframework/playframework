package java8guide.tests;

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

public class FunctionalTest extends WithApplication {

    int timeout = 5000;

    private TestServer testServer(int port) {
        Map<String, String> config = new HashMap<String, String>();
        config.put("application.router", "javaguide.tests.Routes");
        return Helpers.testServer(port, fakeApplication(config));
    }

    //#test-server
    @Test
    public void testInServer() {
        running(testServer(3333), () -> {
            assertThat(
                WS.url("http://localhost:3333").get().get(timeout).getStatus()
            ).isEqualTo(OK);
        });
    }
    //#test-server

    //#with-browser
    @Test
    public void runInBrowser() {
        running(testServer(3333), HTMLUNIT, browser -> {
            browser.goTo("http://localhost:3333");
            assertThat(browser.$("#title").getText()).isEqualTo("Hello Guest");
            browser.$("a").click();
            assertThat(browser.url()).isEqualTo("http://localhost:3333/login");
        });
    }
    //#with-browser
}
