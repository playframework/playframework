import org.junit.*;

import play.mvc.*;
import play.test.*;
import play.libs.F.*;

import controllers.Interceptor;

import static play.test.Helpers.*;
import static org.fest.assertions.Assertions.*;
import java.util.concurrent.Callable;
import java.util.*;

public class ApplicationTest extends WithApplication {
    
    @Test 
    public void compute() {
        assertThat(1 + 1).isEqualTo(2);
    }


    @Test
    public void cache() throws Exception {
        start();
        Callable<String> c = new Callable<String>() {
            public String call() {
                return "world";
            }
        };
        Callable<String> overrideC = new Callable<String>() {
            public String call() {
                return "override";
            }
        };
        assertThat(play.cache.Cache.get("key")).isEqualTo(null);
        play.cache.Cache.getOrElse("key", c, 0);
        assertThat(play.cache.Cache.get("key")).isEqualTo("world");
        String j = play.cache.Cache.getOrElse("key", overrideC, 0);
        assertThat(j).isEqualTo("world");
    }

    @Test 
    public void testAdditionPlugin() throws Exception {
        start(fakeApplication(Collections.<String, String>emptyMap(), Arrays.asList("test.DummyPlugin")));
        assertThat(play.Play.application().plugin(test.DummyPlugin.class).foo()).isEqualTo("yay");
    }

    @Test 
    public void test() {
        start();
        Result result = callAction(
            controllers.routes.ref.JavaApi.headers(),
            fakeRequest().withHeader(HOST, "playframework.com")
        );
        assertThat(contentAsString(result)).isEqualTo("playframework.com");
    }
    @Test 
    public void testCookie() {
        start();
        final Http.Cookie c = new Http.Cookie("testcookie", "value", -1, "/", "localhost", true, true);
        Result result = callAction(
            controllers.routes.ref.JavaApi.cookietest(),
            fakeRequest().withCookies(c)
        );
        assertThat(cookie("testcookie",result).name()).isEqualTo("testcookie");
        assertThat(contentAsString(result)).isEqualTo("testcookie");
    }

    @Test
    public void interceptors() {
        start();
        Interceptor.state = "";
        Result result = callAction(controllers.routes.ref.JavaApi.notIntercepted());
        assertThat(contentAsString(result)).isEqualTo("");

        Interceptor.state = "";
        result = callAction(controllers.routes.ref.JavaApi.interceptedUsingWith());
        assertThat(contentAsString(result)).isEqualTo("intercepted");

        Interceptor.state = "";
        result = callAction(controllers.routes.ref.JavaApi.intercepted());
        assertThat(contentAsString(result)).isEqualTo("intercepted");
    }
}
