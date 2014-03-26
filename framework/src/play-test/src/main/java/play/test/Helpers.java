/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.test;

import org.openqa.selenium.WebDriver;
import play.*;

import play.core.j.JavaResultExtractor;
import play.mvc.*;
import play.api.test.Helpers$;
import play.libs.*;
import play.libs.F.*;

import org.openqa.selenium.firefox.*;
import org.openqa.selenium.htmlunit.*;


import java.util.*;

import static play.core.Router.Routes;
import static play.mvc.Http.*;

/**
 * Helper functions to run tests.
 */
public class Helpers implements play.mvc.Http.Status, play.mvc.Http.HeaderNames {

    public static String GET = "GET";
    public static String POST = "POST";
    public static String PUT = "PUT";
    public static String DELETE = "DELETE";
    public static String HEAD = "HEAD";

    // --

    public static Class<? extends WebDriver> HTMLUNIT = HtmlUnitDriver.class;
    public static Class<? extends WebDriver> FIREFOX = FirefoxDriver.class;

    // --
    @SuppressWarnings(value = "unchecked")
    private static Result invokeHandler(play.api.mvc.Handler handler, FakeRequest fakeRequest, long timeout) {
        if(handler instanceof play.core.j.JavaAction) {
            play.api.mvc.Action action = (play.api.mvc.Action) handler;
            return wrapScalaResult(action.apply(fakeRequest.getWrappedRequest()), timeout);
        } else {
            throw new RuntimeException("This is not a JavaAction and can't be invoked this way.");
        }
    }

    /** Default Timeout (milliseconds) for fake requests issued by these Helpers.
     * This value is determined from System property <b>test.timeout</b>.
     * The default value is <b>30000</b> (30 seconds).
     */
    public static final long DEFAULT_TIMEOUT = Long.getLong("test.timeout", 30000L);

    private static Result wrapScalaResult(scala.concurrent.Future<play.api.mvc.Result> result, long timeout) {
        if (result == null) {
            return null;
        } else {
            final play.api.mvc.Result scalaResult = new Promise<play.api.mvc.Result>(result).get(timeout);
            return new Result() {
                public play.api.mvc.Result toScala() {
                    return scalaResult;
                }
            };
        }
    }

    // --

    /**
     * Call an action method while decorating it with the right @With interceptors.
     */
    public static Result callAction(HandlerRef actionReference) {
        return callAction(actionReference, DEFAULT_TIMEOUT);
    }

    public static Result callAction(HandlerRef actionReference, long timeout) {
        return callAction(actionReference, fakeRequest(), timeout);
    }

    /**
     * Call an action method while decorating it with the right @With interceptors.
     */
    public static Result callAction(HandlerRef actionReference, FakeRequest fakeRequest) {
        return callAction(actionReference, fakeRequest, DEFAULT_TIMEOUT);
    }

    public static Result callAction(HandlerRef actionReference, FakeRequest fakeRequest, long timeout) {
        play.api.mvc.HandlerRef handlerRef = (play.api.mvc.HandlerRef)actionReference;
        return invokeHandler(handlerRef.handler(), fakeRequest, timeout);
    }

    /**
     * Build a new GET / fake request.
     */
    public static FakeRequest fakeRequest() {
        return new FakeRequest();
    }

    /**
     * Build a new fake request.
     */
    public static FakeRequest fakeRequest(String method, String uri) {
        return new FakeRequest(method, uri);
    }

    /**
     * Build a new fake application.
     */
    public static FakeApplication fakeApplication() {
        return new FakeApplication(new java.io.File("."), Helpers.class.getClassLoader(), new HashMap<String,Object>(), new ArrayList<String>(), null);
    }

    /**
     * Build a new fake application.
     */
    public static FakeApplication fakeApplication(GlobalSettings global) {
        return new FakeApplication(new java.io.File("."), Helpers.class.getClassLoader(), new HashMap<String,Object>(), new ArrayList<String>(), global);
    }

    /**
     * A fake Global
     */
    public static GlobalSettings fakeGlobal() {
        return new GlobalSettings();
    }

    /**
     * Constructs a in-memory (h2) database configuration to add to a FakeApplication.
     */
    public static Map<String,String> inMemoryDatabase() {
        return inMemoryDatabase("default");
    }

    /**
     * Constructs a in-memory (h2) database configuration to add to a FakeApplication.
     */
    public static Map<String,String> inMemoryDatabase(String name) {
        return inMemoryDatabase(name, Collections.<String, String>emptyMap());
    }

    /**
     * Constructs a in-memory (h2) database configuration to add to a FakeApplication.
     */
    public static Map<String,String> inMemoryDatabase(String name, Map<String, String> options) {
        return Scala.asJava(play.api.test.Helpers.inMemoryDatabase(name, Scala.asScala(options)));
    }

    /**
     * Build a new fake application.
     */
    public static FakeApplication fakeApplication(Map<String, ? extends Object> additionalConfiguration) {
        return new FakeApplication(new java.io.File("."), Helpers.class.getClassLoader(), additionalConfiguration, new ArrayList<String>(), null);
    }

    /**
     * Build a new fake application.
     */
    public static FakeApplication fakeApplication(Map<String, ? extends Object> additionalConfiguration, GlobalSettings global) {
        return new FakeApplication(new java.io.File("."), Helpers.class.getClassLoader(), additionalConfiguration, new ArrayList<String>(), global);
    }

    /**
     * Build a new fake application.
     */
    public static FakeApplication fakeApplication(Map<String, ? extends Object> additionalConfiguration, List<String> additionalPlugin) {
        return new FakeApplication(new java.io.File("."), Helpers.class.getClassLoader(), additionalConfiguration, additionalPlugin, null);
    }


    /**
     * Build a new fake application.
     */
    public static FakeApplication fakeApplication(Map<String, ? extends Object> additionalConfiguration, List<String> additionalPlugin, GlobalSettings global) {
        return new FakeApplication(new java.io.File("."), Helpers.class.getClassLoader(), additionalConfiguration, additionalPlugin, global);
    }

    /**
     * Build a new fake application.
     */
    public static FakeApplication fakeApplication(Map<String, ? extends Object> additionalConfiguration, List<String> additionalPlugins, List<String> withoutPlugins) {
        return new FakeApplication(new java.io.File("."), Helpers.class.getClassLoader(), additionalConfiguration, additionalPlugins, withoutPlugins, null);
    }

    /**
     * Build a new fake application.
     */
    public static FakeApplication fakeApplication(Map<String, ? extends Object> additionalConfiguration, List<String> additionalPlugins, List<String> withoutPlugins, GlobalSettings global) {
        return new FakeApplication(new java.io.File("."), Helpers.class.getClassLoader(), additionalConfiguration, additionalPlugins, withoutPlugins, global);
    }

    /**
     * Extracts the Status code of this Result value.
     */
    public static int status(Result result) {
        return result.toScala().header().status();
    }

    @Deprecated
    public static int status(Result result, long timeout) {
        return status(result);
    }

    /**
     * Extracts the Location header of this Result value if this Result is a Redirect.
     */
    public static String redirectLocation(Result result) {
        return header(LOCATION, result);
    }

    @Deprecated
    public static String redirectLocation(Result result, long timeout) {
        return redirectLocation(result);
    }

    /**
     * Extracts the Flash values of this Result value.
     */
    public static Flash flash(Result result) {
        return JavaResultExtractor.getFlash(result);
    }

    @Deprecated
    public static Flash flash(Result result, long timeout) {
        return flash(result);
    }

    /**
     * Extracts the Session of this Result value.
     */
    public static Session session(Result result) {
        return JavaResultExtractor.getSession(result);
    }

    @Deprecated
    public static Session session(Result result, long timeout) {
        return session(result);
    }

    /**
     * Extracts a Cookie value from this Result value
     */
    public static Cookie cookie(String name, Result result) {
        return JavaResultExtractor.getCookies(result).get(name);
    }

    @Deprecated
    public static Cookie cookie(String name, Result result, long timeout) {
        return cookie(name, result);
    }

    /**
     * Extracts the Cookies (an iterator) from this result value.
     */
    public static Cookies cookies(Result result) {
        return play.core.j.JavaResultExtractor.getCookies(result);
    }

    @Deprecated
    public static Cookies cookies(Result result, long timeout) {
        return cookies(result);
    }

    /**
     * Extracts an Header value of this Result value.
     */
    public static String header(String header, Result result) {
        return JavaResultExtractor.getHeaders(result).get(header);
    }

    @Deprecated
    public static String header(String header, Result result, long timeout) {
        return header(header, result);
    }

    /**
     * Extracts all Headers of this Result value.
     */
    public static Map<String, String> headers(Result result) {
        return JavaResultExtractor.getHeaders(result);
    }

    @Deprecated
    public static Map<String, String> headers(Result result, long timeout) {
        return headers(result);
    }

    /**
     * Extracts the Content-Type of this Content value.
     */
    public static String contentType(Content content) {
        return content.contentType();
    }

    /**
     * Extracts the Content-Type of this Result value.
     */
    public static String contentType(Result result) {
        String h = header(CONTENT_TYPE, result);
        if(h == null) return null;
        if(h.contains(";")) {
            return h.substring(0, h.indexOf(";")).trim();
        } else {
            return h.trim();
        }
    }

    @Deprecated
    public static String contentType(Result result, long timeout) {
        return contentType(result);
    }

    /**
     * Extracts the Charset of this Result value.
     */
    public static String charset(Result result) {
        String h = header(CONTENT_TYPE, result);
        if(h == null) return null;
        if(h.contains("; charset=")) {
            return h.substring(h.indexOf("; charset=") + 10, h.length()).trim();
        } else {
            return null;
        }
    }

    @Deprecated
    public static String charset(Result result, long timeout) {
        return charset(result);
    }

    /**
     * Extracts the content as bytes.
     */
    public static byte[] contentAsBytes(Result result) {
        return contentAsBytes(result, DEFAULT_TIMEOUT);
    }

    public static byte[] contentAsBytes(Result result, long timeout) {
        return JavaResultExtractor.getBody(result, timeout);
    }

    /**
     * Extracts the content as bytes.
     */
    public static byte[] contentAsBytes(Content content) {
        return content.body().getBytes();
    }

    /**
     * Extracts the content as String.
     */
    public static String contentAsString(Content content) {
        return content.body();
    }

    /**
     * Extracts the content as String.
     */
    public static String contentAsString(Result result) {
        return contentAsString(result, DEFAULT_TIMEOUT);
    }

    public static String contentAsString(Result result, long timeout) {
        try {
            String charset = charset(result);
            if(charset == null) {
                charset = "utf-8";
            }
            return new String(contentAsBytes(result, timeout), charset);
        } catch(RuntimeException e) {
            throw e;
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Use the Router to determine the Action to call for this request and executes it.
     * @deprecated
     * @see #route instead
     */
    public static Result routeAndCall(FakeRequest fakeRequest) {
        return routeAndCall(fakeRequest, DEFAULT_TIMEOUT);
    }

    @SuppressWarnings(value = "unchecked")
    public static Result routeAndCall(FakeRequest fakeRequest, long timeout) {
        try {
            return routeAndCall((Class<? extends Routes>)FakeRequest.class.getClassLoader().loadClass("Routes"), fakeRequest, timeout);
        } catch(RuntimeException e) {
            throw e;
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Use the Router to determine the Action to call for this request and executes it.
     * @deprecated
     * @see #route instead
     */
    public static Result routeAndCall(Class<? extends Routes> router, FakeRequest fakeRequest) {
        return routeAndCall(router, fakeRequest, DEFAULT_TIMEOUT);
    }

    public static Result routeAndCall(Class<? extends Routes> router, FakeRequest fakeRequest, long timeout) {
        try {
            Routes routes = (Routes) router.getClassLoader().loadClass(router.getName() + "$").getDeclaredField("MODULE$").get(null);
            if(routes.routes().isDefinedAt(fakeRequest.getWrappedRequest())) {
                return invokeHandler(routes.routes().apply(fakeRequest.getWrappedRequest()), fakeRequest, timeout);
            } else {
                return null;
            }
        } catch(RuntimeException e) {
            throw e;
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static Result route(FakeRequest fakeRequest) {
      return route(fakeRequest, DEFAULT_TIMEOUT);
    }

    public static Result route(FakeRequest fakeRequest, long timeout) {
      return route(play.Play.application(), fakeRequest, timeout);
    }

    public static Result route(Application app, FakeRequest fakeRequest) {
      return route(app, fakeRequest, DEFAULT_TIMEOUT);
    }

    public static Result route(Application app, FakeRequest fakeRequest, long timeout) {
      final scala.Option<scala.concurrent.Future<play.api.mvc.Result>> opt = play.api.test.Helpers.jRoute(app.getWrappedApplication(), fakeRequest.fake);
      return wrapScalaResult(Scala.orNull(opt), timeout);
    }

    public static Result route(Application app, FakeRequest fakeRequest, byte[] body) {
      return route(app, fakeRequest, body, DEFAULT_TIMEOUT);
    }

    public static Result route(Application app, FakeRequest fakeRequest, byte[] body, long timeout) {
      return wrapScalaResult(Scala.orNull(play.api.test.Helpers.jRoute(app.getWrappedApplication(), fakeRequest.getWrappedRequest(), body)), timeout);
    }

    public static Result route(FakeRequest fakeRequest, byte[] body) {
      return route(fakeRequest, body, DEFAULT_TIMEOUT);
    }

    public static Result route(FakeRequest fakeRequest, byte[] body, long timeout) {
      return route(play.Play.application(), fakeRequest, body, timeout);
    }

    /**
     * Starts a new application.
     */
    public static void start(FakeApplication fakeApplication) {

        play.api.Play.start(fakeApplication.getWrappedApplication());
    }

    /**
     * Stops an application.
     */
    public static void stop(FakeApplication fakeApplication) {
        play.api.Play.stop();
    }

    /**
     * Executes a block of code in a running application.
     */
    public static synchronized void running(FakeApplication fakeApplication, final Runnable block) {
        try {
            start(fakeApplication);
            block.run();
        } finally {
            stop(fakeApplication);
        }
    }


    /**
     * Creates a new Test server.
     */
    public static TestServer testServer(int port) {
        return new TestServer(port, fakeApplication());
    }

    /**
     * Creates a new Test server.
     */
    public static TestServer testServer(int port, FakeApplication app) {
        return new TestServer(port, app);
    }

    /**
     * Starts a Test server.
     */
    public static void start(TestServer server) {
        server.start();
    }

    /**
     * Stops a Test server.
     */
    public static void stop(TestServer server) {
        server.stop();
    }

    /**
     * Executes a block of code in a running server.
     */
    public static synchronized void running(TestServer server, final Runnable block) {
        try {
            start(server);
            block.run();
        } finally {
            stop(server);
        }
    }

    /**
     * Executes a block of code in a running server, with a test browser.
     */
    public static synchronized void running(TestServer server, Class<? extends WebDriver> webDriver, final Callback<TestBrowser> block) {
        running(server, play.api.test.WebDriverFactory.apply(webDriver), block);
    }

    /**
     * Executes a block of code in a running server, with a test browser.
     */
    public static synchronized void running(TestServer server, WebDriver webDriver, final Callback<TestBrowser> block) {
        TestBrowser browser = null;
        TestServer startedServer = null;
        try {
            start(server);
            startedServer = server;
            browser = testBrowser(webDriver);
            block.invoke(browser);
        } catch(Error e) {
            throw e;
        } catch(RuntimeException re) {
            throw re;
        } catch(Throwable t) {
            throw new RuntimeException(t);
        } finally {
            if(browser != null) {
                browser.quit();
            }
            if(startedServer != null) {
                stop(startedServer);
            }
        }
    }

    /**
     * Creates a Test Browser.
     */
    public static TestBrowser testBrowser() {
        return testBrowser(HTMLUNIT);
    }

    /**
     * Creates a Test Browser.
     */
    public static TestBrowser testBrowser(int port) {
        return testBrowser(HTMLUNIT, port);
    }

    /**
     * Creates a Test Browser.
     */
    public static TestBrowser testBrowser(Class<? extends WebDriver> webDriver) {
        return testBrowser(webDriver, Helpers$.MODULE$.testServerPort());
    }

    /**
     * Creates a Test Browser.
     */
    public static TestBrowser testBrowser(Class<? extends WebDriver> webDriver, int port) {
        try {
            return new TestBrowser(webDriver, "http://localhost:" + port);
        } catch(RuntimeException e) {
            throw e;
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Creates a Test Browser.
     */
    public static TestBrowser testBrowser(WebDriver of, int port) {
        return new TestBrowser(of, "http://localhost:" + port);
    }

    /**
     * Creates a Test Browser.
     */
    public static TestBrowser testBrowser(WebDriver of) {
        return testBrowser(of, Helpers$.MODULE$.testServerPort());
    }

}
