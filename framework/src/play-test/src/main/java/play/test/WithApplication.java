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

    /**
     * @deprecated The application is automatically started before each test, you don’t need to call this method explicitly
     */
    @Deprecated
    protected void start() {
        start(provideFakeApplication());
    }

    @Deprecated
    protected void start(FakeApplication app) {
        this.app = app;
        Helpers.start(app);
    }

    @Before
    public void startPlay() {
        start(provideFakeApplication());
    }

    @After
    public void stopPlay() {
        if (app != null) {
            Helpers.stop(app);
            app = null;
        }
    }

}
