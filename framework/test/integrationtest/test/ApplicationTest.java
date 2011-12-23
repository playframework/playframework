package test;

import org.junit.*;

import play.mvc.*;
import play.test.*;
import play.libs.F.*;

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

}
