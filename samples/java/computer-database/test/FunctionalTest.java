import org.junit.*;

import play.mvc.*;
import play.test.*;
import play.libs.F.*;

import static play.test.Helpers.*;
import static org.fest.assertions.Assertions.*;

public class FunctionalTest {
    
    @Test
    public void test() {
        running(testServer(9001), HTMLUNIT, new Callback<TestBrowser>() {
            public void invoke(TestBrowser browser) {
                browser.goTo("http://localhost:9001");
                assertThat(browser.$("#homeTitle").getTexts().get(0)).isEqualTo("574 computers found");
            }
        });
    }
  
}
