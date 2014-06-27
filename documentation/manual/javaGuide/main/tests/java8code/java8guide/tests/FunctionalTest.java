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
import static org.junit.Assert.*;

public class FunctionalTest extends WithApplication {

    int timeout = 5000;

    private TestServer testServer() {
        Map<String, String> config = new HashMap<String, String>();
        config.put("application.router", "javaguide.tests.Routes");
        return Helpers.testServer(fakeApplication(config));
    }

    private TestServer testServer(int port) {
        Map<String, String> config = new HashMap<String, String>();
        config.put("application.router", "javaguide.tests.Routes");
        return Helpers.testServer(port, fakeApplication(config));
    }

    //#test-server
    @Test
    public void testInServer() {
        running(testServer(3333), () -> {
            assertEquals(OK, WS.url("http://localhost:3333").get().get(timeout).getStatus());
        });
    }
    //#test-server

    //#with-browser
    @Test
    public void runInBrowser() {
        running(testServer(), HTMLUNIT, browser -> {
            browser.goTo("/");
            assertEquals("Welcome to Play!", browser.$("#title").getText());
            browser.$("a").click();
            assertEquals("/login", browser.url());
        });
    }
    //#with-browser
}
