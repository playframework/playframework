/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.async;

import javaguide.testhelpers.MockJavaAction;
import javaguide.testhelpers.MockJavaActionHelper;
import org.junit.Before;
import org.junit.Test;
import play.libs.Comet;
import play.mvc.Result;
import play.test.WithApplication;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

public class JavaComet extends WithApplication {

    @Test
    public void manual() {
        String content = contentAsString(MockJavaActionHelper.call(new Controller1(), fakeRequest()));
        assertThat(content, containsString("<script>console.log('kiki')</script>"));
        assertThat(content, containsString("<script>console.log('foo')</script>"));
        assertThat(content, containsString("<script>console.log('bar')</script>"));
    }

    public static class Controller1 extends MockJavaAction {
        //#manual
        public static Result index() {
            // Prepare a chunked text stream
            Chunks<String> chunks = StringChunks.whenReady(out -> {
                out.write("<script>console.log('kiki')</script>");
                out.write("<script>console.log('foo')</script>");
                out.write("<script>console.log('bar')</script>");
                out.close();
            });

            response().setContentType("text/html");
            return ok(chunks);
        }
        //#manual
    }

    @Test
    public void comet() {
        String content = contentAsString(MockJavaActionHelper.call(new Controller2(), fakeRequest()));
        assertThat(content, containsString("<script type=\"text/javascript\">console.log('kiki');</script>"));
        assertThat(content, containsString("<script type=\"text/javascript\">console.log('foo');</script>"));
        assertThat(content, containsString("<script type=\"text/javascript\">console.log('bar');</script>"));
    }

    public static class Controller2 extends MockJavaAction {
        //#comet
        public Result index() {
            return ok(Comet.whenConnected("console.log", comet -> {
                comet.sendMessage("kiki");
                comet.sendMessage("foo");
                comet.sendMessage("bar");
                comet.close();
            }));
        }
        //#comet
    }

    @Test
    public void foreverIframe() {
        String content = contentAsString(MockJavaActionHelper.call(new Controller3(), fakeRequest()));
        assertThat(content, containsString("<script type=\"text/javascript\">parent.cometMessage('kiki');</script>"));
        assertThat(content, containsString("<script type=\"text/javascript\">parent.cometMessage('foo');</script>"));
        assertThat(content, containsString("<script type=\"text/javascript\">parent.cometMessage('bar');</script>"));
    }

    public static class Controller3 extends MockJavaAction {
        //#forever-iframe
        public Result index() {
            return ok(Comet.whenConnected("parent.cometMessage", comet -> {
                comet.sendMessage("kiki");
                comet.sendMessage("foo");
                comet.sendMessage("bar");
                comet.close();
            }));
        }
        //#forever-iframe
    }
}
