/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test;

import akka.stream.Materializer;
import org.junit.After;
import org.junit.Before;
import play.Application;

/**
 * Provides an application for JUnit tests. Make your test class extend this class and an application will be started before each test is invoked.
 * You can setup the application to use by overriding the provideApplication method.
 * Within a test, the running application is available through the app field.
 */
public class WithApplication {

    protected Application app;

    /**
     * The application's Akka streams Materializer.
     */
    protected Materializer mat;

    /**
     * Override this method to setup the application to use.
     *
     * @return The application to use
     */
    protected Application provideApplication() {
        return Helpers.fakeApplication();
    }

    /**
     * Provides an instance from the application.
     *
     * @param clazz the type's class.
     * @param <T> the type to return, using `app.injector.instanceOf`
     * @return an instance of type T.
     */
    protected <T> T instanceOf(Class<T> clazz) {
        return app.injector().instanceOf(clazz);
    }

    @Before
    public void startPlay() {
        app = provideApplication();
        Helpers.start(app);
        mat = app.asScala().materializer();
    }

    @After
    public void stopPlay() {
        if (app != null) {
            Helpers.stop(app);
            app = null;
        }
    }

}
