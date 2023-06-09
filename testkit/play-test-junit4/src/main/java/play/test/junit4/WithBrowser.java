/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test.junit4;

import org.junit.After;
import org.junit.Before;
import play.test.Helpers;
import play.test.TestBrowser;

/**
 * Provides a server and browser to JUnit tests. Make your test class extend this class and an
 * application, a server and a browser will be started before each test is invoked. You can setup
 * the fake application to use, the port and the browser to use by overriding the
 * provideApplication, providePort and provideBrowser methods, respectively. Within a test, the
 * running application, the TCP port and the browser are available through the app, port and browser
 * fields, respectively.
 */
public class WithBrowser extends WithServer {
  protected TestBrowser browser;

  /**
   * Override this if you want to use a different browser
   *
   * @param port the port to run the browser against.
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
