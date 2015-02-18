/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package detailedtopics.configuration.threadpools;

import org.junit.Test;
import play.Play;
import play.test.FakeApplication;

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

}
