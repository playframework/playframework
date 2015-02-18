/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.test;

import org.junit.After;
import org.junit.Before;

/**
 * Provides a server and browser to JUnit tests. Make your test class extend this class and an application, a server and a browser will be started before each test is invoked.
 * You can setup the fake application to use, the port and the browser to use by overriding the provideFakeApplication, providePort and provideBrowser methods, respectively.
 * Within a test, the running application, the TCP port and the browser are available through the app, port and browser fields, respectively.
 */
public class WithBrowser extends WithServer {
    protected TestBrowser browser;

    /**
     * Override this if you want to use a different browser
     *
     * @return a new test browser
     */
    protected TestBrowser provideBrowser(int port) {
        return Helpers.testBrowser(port);
    }

    @Before
    public void createBrowser() {
        browser = provideBrowser(port);
    }

    @After
    public void quitBrowser() {
        if (browser != null) {
            browser.quit();
            browser = null;
        }
    }
}
