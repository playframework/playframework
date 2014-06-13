/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package detailedtopics.configuration.threadpools;

import org.junit.Test;
import play.Play;
import play.test.FakeApplication;
import javaguide.testhelpers.MockJavaAction;
import play.libs.F.Promise;

import play.mvc.*;
import play.mvc.Http.*;
import static javaguide.testhelpers.MockJavaAction.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

public class ThreadPoolsJava {

    @Test
    public void usingAppClassLoader() throws Exception {
        running(fakeApplication(), new Runnable() {
            public void run() {
                String myClassName = "java.lang.String";
                try {
                    //#using-app-classloader
                    Class myClass = Play.application().classloader().loadClass(myClassName);
                    //#using-app-classloader
                    assertThat(myClass, notNullValue());
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Test
    public void context1() {
        assertThat(header("Content-Type", call(new Controller1(), fakeRequest())), containsString("text/html"));
    }

    public static class Controller1 extends MockJavaAction {
        //#context-content-type
        public static Result index() {
            // Access the context's response
            response().setContentType("text/html");
            return ok("<h1>Hello World!</h1>");
        }
        //#context-content-type
    }

}
