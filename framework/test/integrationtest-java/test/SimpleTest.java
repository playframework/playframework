package test;

import org.junit.*;

import play.mvc.*;
import play.test.*;
import play.libs.F.*;

import static play.test.Helpers.*;
import static org.fest.assertions.Assertions.*;

public class SimpleTest {

    @Test 
    public void simpleCheck() {
        int a = 1 + 1;
        assertThat(a).isEqualTo(2);
    }
    
    @Test
    public void renderTemplate() {
        Content html = views.html.index.render("Coco");
        assertThat(contentType(html)).isEqualTo("text/html");
        assertThat(contentAsString(html)).contains("Coco");
    }
  
    @Test
    public void callIndex() {
        Result result = callAction(controllers.routes.ref.Application.index("Kiki"));   
        assertThat(status(result)).isEqualTo(OK);
        assertThat(contentType(result)).isEqualTo("text/html");
        assertThat(charset(result)).isEqualTo("utf-8");
        assertThat(contentAsString(result)).contains("Hello Kiki");
    }
    
    @Test
    public void badRoute() {
        Result result = routeAndCall(fakeRequest(GET, "/xx/Kiki"));
        assertThat(result).isNull();
    }
    
    @Test
    public void routeIndex() {
        Result result = routeAndCall(fakeRequest(GET, "/Kiki"));
        assertThat(status(result)).isEqualTo(OK);
        assertThat(contentType(result)).isEqualTo("text/html");
        assertThat(charset(result)).isEqualTo("utf-8");
        assertThat(contentAsString(result)).contains("Hello Kiki");
    }
    
    @Test
    public void inApp() {  
        running(fakeApplication(), new Runnable() {
            public void run() {
                Result result = routeAndCall(fakeRequest(GET, "/key"));
                assertThat(status(result)).isEqualTo(OK);
                assertThat(contentType(result)).isEqualTo("text/plain");
                assertThat(charset(result)).isEqualTo("utf-8");
                assertThat(contentAsString(result)).contains("secret");
            }
        });
    }
    
    @Test
    public void inServer() {
        running(testServer(3333), HTMLUNIT, new Callback<TestBrowser>() {
            public void invoke(TestBrowser browser) {
               browser.goTo("http://localhost:3333"); 
               assertThat(browser.$("#title").getTexts().get(0)).isEqualTo("Hello Guest");
               browser.$("a").click();
               assertThat(browser.url()).isEqualTo("http://localhost:3333/Coco");
               assertThat(browser.$("#title", 0).getText()).isEqualTo("Hello Coco");
            }
        });
    }
  
}
