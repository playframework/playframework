package test;

import org.junit.*;

import play.mvc.*;
import play.test.*;
import play.libs.F.*;

import controllers.Interceptor;

import static play.test.Helpers.*;
import static org.fest.assertions.Assertions.*;

public class ApplicationTest {
    
    @Test 
    public void compute() {
        assertThat(1 + 1).isEqualTo(2);
    }

    @Test 
    public void test() {
        running(fakeApplication(), new Runnable() {
            public void run() {
                Result result = callAction(
                    controllers.routes.ref.JavaApi.headers(),
                    fakeRequest().withHeader(HOST, "playframework.org")
                );
                assertThat(contentAsString(result)).isEqualTo("playframework.org");
            }
        });
    }

    @Test
    public void interceptors() {
        running(fakeApplication(), new Runnable() {
            public void run() {
                Interceptor.state = "";
                Result result = callAction(controllers.routes.ref.JavaApi.notIntercepted());
                assertThat(contentAsString(result)).isEqualTo("");
                
                Interceptor.state = "";
                result = callAction(controllers.routes.ref.JavaApi.interceptedUsingWith());
                assertThat(contentAsString(result)).isEqualTo("intercepted");
                
                Interceptor.state = "";
                result = callAction(controllers.routes.ref.JavaApi.intercepted());
                assertThat(contentAsString(result)).isEqualTo("intercepted");
      }});
    
    }
}
