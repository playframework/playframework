package javaguide.tests;

import java.util.HashMap;
import java.util.Map;

import org.junit.*;

import play.mvc.*;
import play.test.*;
import play.libs.F.*;
import play.libs.ws.*;

import static play.test.Helpers.*;
import static org.fest.assertions.Assertions.*;

//#test-withapp
public class FunctionalTest extends WithApplication {
//#test-withapp

    //#bad-route
    @Test
    public void testBadRoute() {
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
                assertThat(browser.$("#title").getText()).isEqualTo("Welcome to Play!");
                browser.$("a").click();
                assertThat(browser.url()).isEqualTo("http://localhost:3333/login");
            }
        });
    }
    //#with-browser
}
