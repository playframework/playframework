/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.test;

import org.junit.After;
import org.junit.Before;

/**
 * Provides an application for JUnit tests. Make your test class extend this class and an application will be started before each test is invoked.
 * You can setup the fake application to use by overriding the provideFakeApplication method.
 * Within a test, the running application is available through the app field.
 */
public class WithApplication {

    protected FakeApplication app;

    /**
     * Override this method to setup the fake application to use.
     *
     * @return The fake application to use
     */
    protected FakeApplication provideFakeApplication() {
        return Helpers.fakeApplication();
    }

    @Before
    public void startPlay() {
        app = provideFakeApplication();
        Helpers.start(app);
    }

    @After
    public void stopPlay() {
        if (app != null) {
            Helpers.stop(app);
            app = null;
        }
    }

}
