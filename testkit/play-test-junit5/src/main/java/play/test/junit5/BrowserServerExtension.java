/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test.junit5;

import java.util.Objects;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import play.test.TestBrowser;
import play.test.TestServer;

public class BrowserServerExtension implements BeforeAllCallback, AfterAllCallback {

    private final TestBrowser testBrowser;
    private final TestServer testServer;

    public BrowserServerExtension(TestBrowser testBrowser, TestServer testServer) {
        this.testBrowser = testBrowser;
        this.testServer = testServer;
    }

    public TestBrowser getTestBrowser() {
        return testBrowser;
    }

    public TestServer getTestServer() {
        return testServer;
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (Objects.nonNull(testServer)) {
            testServer.stop();
        }
        if (Objects.nonNull(testBrowser)) {
            testBrowser.quit();
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        if (Objects.nonNull(testServer)) {
            testServer.start();
        }
    }
}
