package test;

import org.junit.*;

import play.mvc.*;
import play.test.*;
import play.libs.F.*;

import static play.test.Helpers.*;
import static org.fest.assertions.Assertions.*;

public class ApplicationTest {

    @Test 
    public void test() {
        running(fakeApplication(), new Runnable() {
           public void run() {
               Result result = routeAndCall(fakeRequest(GET, "/computers"));

               assertThat(status(result)).isEqualTo(OK);
               assertThat(contentAsString(result)).contains("574 computers found");
           }
        });
    }
    
}
