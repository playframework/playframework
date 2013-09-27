package play.test;

import play.*;

import play.mvc.*;
import play.api.test.Helpers$;
import play.libs.*;
import play.libs.F.*;

import org.openqa.selenium.*;
import org.openqa.selenium.firefox.*;
import org.openqa.selenium.htmlunit.*;


import java.util.*;

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
    private static Result invokeHandler(play.api.mvc.Handler handler, FakeRequest fakeRequest) {
        if(handler instanceof play.core.j.JavaAction) {
            play.api.mvc.Action action = (play.api.mvc.Action)handler;
            return wrapScalaResult(action.apply(fakeRequest.getWrappedRequest()));
        } else {
            throw new RuntimeException("This is not a JavaAction and can't be invoked this way.");
        }
    }

    private static SimpleResult wrapScalaResult(scala.concurrent.Future<play.api.mvc.SimpleResult> result) {
        if (result == null) {
            return null;
        } else {
            final play.api.mvc.SimpleResult simpleResult = new Promise<play.api.mvc.SimpleResult>(result).get();
            return new SimpleResult() {
                public play.api.mvc.SimpleResult getWrappedSimpleResult() {
                    return simpleResult;
                }
            };
        }
    }

    private static SimpleResult unwrapJavaResult(Result result) {
        if (result instanceof SimpleResult) {
            return (SimpleResult) result;
        } else {
            return wrapScalaResult(result.getWrappedResult());
        }
    }

    // --

    /**
     * Call an action method while decorating it with the right @With interceptors.
     */
    public static Result callAction(HandlerRef actionReference) {
        return callAction(actionReference, fakeRequest());
    }

    /**
     * Call an action method while decorating it with the right @With interceptors.
     */
    public static Result callAction(HandlerRef actionReference, FakeRequest fakeRequest) {
        play.api.mvc.HandlerRef handlerRef = (play.api.mvc.HandlerRef)actionReference;
        return invokeHandler(handlerRef.handler(), fakeRequest);
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
        return unwrapJavaResult(result).getWrappedSimpleResult().header().status();
    }

    /**
     * Extracts the Location header of this Result value if this Result is a Redirect.
     */
    public static String redirectLocation(Result result) {
        return header(LOCATION, result);
    }

    /**
     * Extracts the Flash values of this Result value.
     */
    public static play.mvc.Http.Flash flash(Result result) {
        return play.core.j.JavaResultExtractor.getFlash(unwrapJavaResult(result));
    }

    /**
     * Extracts the Session of this Result value.
     */
    public static play.mvc.Http.Session session(Result result) {
        return play.core.j.JavaResultExtractor.getSession(unwrapJavaResult(result));
    }

    /**
     * * Extracts a Cookie value from this Result value
     */
    public static play.mvc.Http.Cookie cookie(String name, Result result) {
        return play.core.j.JavaResultExtractor.getCookies(unwrapJavaResult(result)).get(name);
    }

    /**
     * Extracts an Header value of this Result value.
     */
    public static String header(String header, Result result) {
        return play.core.j.JavaResultExtractor.getHeaders(unwrapJavaResult(result)).get(header);
    }

    /**
     * Extracts all Headers of this Result value.
     */
    public static Map<String,String> headers(Result result) {
        return play.core.j.JavaResultExtractor.getHeaders(unwrapJavaResult(result));
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

    /**
     * Extracts the content as bytes.
     */
    public static byte[] contentAsBytes(Result result) {
        return play.core.j.JavaResultExtractor.getBody(unwrapJavaResult(result));
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
        try {
            String charset = charset(result);
            if(charset == null) {
                charset = "utf-8";
            }
            return new String(contentAsBytes(result), charset);
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
    @SuppressWarnings(value = "unchecked")
    public static Result routeAndCall(FakeRequest fakeRequest) {
        try {
            return routeAndCall((Class<? extends play.core.Router.Routes>)FakeRequest.class.getClassLoader().loadClass("Routes"), fakeRequest);
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
    public static Result routeAndCall(Class<? extends play.core.Router.Routes> router, FakeRequest fakeRequest) {
        try {
            play.core.Router.Routes routes = (play.core.Router.Routes)router.getClassLoader().loadClass(router.getName() + "$").getDeclaredField("MODULE$").get(null);
            if(routes.routes().isDefinedAt(fakeRequest.getWrappedRequest())) {
                return invokeHandler(routes.routes().apply(fakeRequest.getWrappedRequest()), fakeRequest);
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
      return route(play.Play.application(), fakeRequest);
    }

    public static Result route(Application app, FakeRequest fakeRequest) {
      final scala.Option<scala.concurrent.Future<play.api.mvc.SimpleResult>> opt = play.api.test.Helpers.jRoute(app.getWrappedApplication(), fakeRequest.fake);
      return wrapScalaResult(Scala.orNull(opt));
    }

    public static Result route(Application app, FakeRequest fakeRequest, byte[] body) {
      return wrapScalaResult(Scala.orNull(play.api.test.Helpers.jRoute(app.getWrappedApplication(), fakeRequest.getWrappedRequest(), body)));
    }

    public static Result route(FakeRequest fakeRequest, byte[] body) {
      return route(play.Play.application(), fakeRequest, body);
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
        play.api.libs.ws.WS$.MODULE$.resetClient();
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
