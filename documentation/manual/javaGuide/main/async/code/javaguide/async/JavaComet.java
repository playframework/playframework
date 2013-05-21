package javaguide.async;

import javaguide.testhelpers.MockJavaAction;
import org.junit.Before;
import org.junit.Test;
import play.libs.Comet;
import play.mvc.Result;
import play.test.WithApplication;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

public class JavaComet extends WithApplication {
    @Before
    public void setUp() {
        start();
    }

    @Test
    public void manual() {
        String content = contentAsString(MockJavaAction.call(new Controller1(), fakeRequest()));
        // Wait until results refactoring is merged, then this will work
        // assertThat(content, containsString("<script>console.log('kiki')</script>"));
    }

    public static class Controller1 extends MockJavaAction {
        //#manual
        public static Result index() {
            // Prepare a chunked text stream
            Chunks<String> chunks = new StringChunks() {

                // Called when the stream is ready
                public void onReady(Chunks.Out<String> out) {
                    out.write("<script>console.log('kiki')</script>");
                    out.write("<script>console.log('foo')</script>");
                    out.write("<script>console.log('bar')</script>");
                    out.close();
                }

            };

            response().setContentType("text/html");
            return ok(chunks);
        }
        //#manual
    }

    @Test
    public void comet() {
        String content = contentAsString(MockJavaAction.call(new Controller2(), fakeRequest()));
        // Wait until results refactoring is merged, then this will work
        // assertThat(content, containsString("<script type=\"text/javascript\">console.log(\"kiki\");</script>"));
    }

    public static class Controller2 extends MockJavaAction {
        //#comet
        public static Result index() {
            Comet comet = new Comet("console.log") {
                public void onConnected() {
                    sendMessage("kiki");
                    sendMessage("foo");
                    sendMessage("bar");
                    close();
                }
            };

            return ok(comet);
        }
        //#comet
    }

    @Test
    public void foreverIframe() {
        String content = contentAsString(MockJavaAction.call(new Controller3(), fakeRequest()));
        // Wait until results refactoring is merged, then this will work
        // assertThat(content, containsString("<script type=\"text/javascript\">parent.cometMessage(\"kiki\");</script>"));
    }

    public static class Controller3 extends MockJavaAction {
        //#forever-iframe
        public static Result index() {
            Comet comet = new Comet("parent.cometMessage") {
                public void onConnected() {
                    sendMessage("kiki");
                    sendMessage("foo");
                    sendMessage("bar");
                    close();
                }
            };

            return ok(comet);
        }
        //#forever-iframe
    }

}