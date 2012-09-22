package play.test;

import org.junit.After;

/**
 * Provides an application for JUnit tests.
 */
public class WithApplication {

    protected FakeApplication app;

    protected void start() {
        start(Helpers.fakeApplication());
    }

    protected void start(FakeApplication app) {
        this.app = app;
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
