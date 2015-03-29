/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.test;

import org.openqa.selenium.WebDriver;
import play.*;

import play.api.routing.Router;
import play.api.test.PlayRunners$;
import play.core.j.JavaAction;
import play.core.j.JavaHandler;
import play.core.j.JavaHandlerComponents;
import play.core.j.JavaResultExtractor;
import play.mvc.*;
import play.api.test.Helpers$;
import play.libs.*;
import play.libs.F.*;
import play.twirl.api.Content;

import org.openqa.selenium.firefox.*;
import org.openqa.selenium.htmlunit.*;


import java.util.*;
import java.util.concurrent.Callable;

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
    private static Result invokeHandler(play.api.mvc.Handler handler, Request requestBuilder, long timeout) {
        if (handler instanceof play.api.mvc.Action) {
            play.api.mvc.Action action = (play.api.mvc.Action) handler;
            return wrapScalaResult(action.apply(requestBuilder._underlyingRequest()), timeout);
        } else if (handler instanceof JavaHandler) {
            return invokeHandler(
                ((JavaHandler) handler).withComponents(Play.application().injector().instanceOf(JavaHandlerComponents.class)),
                requestBuilder, timeout
            );
        } else {
            throw new RuntimeException("This is not a JavaAction and can't be invoked this way.");
        }
    }

    /**
     * Default Timeout (milliseconds) for fake requests issued by these Helpers.
     * This value is determined from System property <b>test.timeout</b>.
     * The default value is <b>30000</b> (30 seconds).
     */
    public static final long DEFAULT_TIMEOUT = Long.getLong("test.timeout", 30000L);

    private static Result wrapScalaResult(scala.concurrent.Future<play.api.mvc.Result> result, long timeout) {
        if (result == null) {
            return null;
        } else {
            final play.api.mvc.Result scalaResult = Promise.wrap(result).get(timeout);
            return new Result() {
                public play.api.mvc.Result toScala() {
                    return scalaResult;
                }
            };
        }
    }

    // --

    /**
     * Calls a Callable which invokes a Controller or some other method with a Context
     */
    public <V> V invokeWithContext(RequestBuilder requestBuilder, Callable<V> callable) {
      try {
        Context.current.set(new Context(requestBuilder));
        return callable.call();
      } catch (Exception e) {
        throw new RuntimeException(e);
      } finally {
        Context.current.remove();
      }
    }

    /**
     * Build a new GET / fake request.
     */
    public static RequestBuilder fakeRequest() {
        return fakeRequest("GET", "/");
    }

    /**
     * Build a new fake request.
     */
    public static RequestBuilder fakeRequest(String method, String uri) {
        return new RequestBuilder().method(method).uri(uri);
    }

    /**
     * Build a new fake request corresponding to a given route call
     */
    public static RequestBuilder fakeRequest(Call call) {
        return fakeRequest(call.method(), call.url());
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
     * @deprecated use {@link Result#status()} instead.  
     */
    @Deprecated
    public static int status(Result result) {
        return result.toScala().header().status();
    }

    /**
     * @deprecated use {@link Result#redirectLocation()} instead.  
     */
    @Deprecated
    public static String redirectLocation(Result result) {
        return header(LOCATION, result);
    }

    /**
     * @deprecated use {@link Result#flash()} instead.  
     */
    @Deprecated
    public static Flash flash(Result result) {
        return JavaResultExtractor.getFlash(result);
    }

    /**
     * @deprecated use {@link Result#session()} instead.  
     */
    @Deprecated
    public static Session session(Result result) {
        return JavaResultExtractor.getSession(result);
    }

    /**
     * @deprecated use {@link Result#cookie(String)} instead.  
     */
    @Deprecated
    public static Cookie cookie(String name, Result result) {
        return JavaResultExtractor.getCookies(result).get(name);
    }

    /**
     * @deprecated use {@link Result#cookies()} instead.  
     */
    @Deprecated
    public static Cookies cookies(Result result) {
        return play.core.j.JavaResultExtractor.getCookies(result);
    }

    /**
     * @deprecated use {@link Result#header(String)} instead.  
     */
    @Deprecated
    public static String header(String header, Result result) {
        return JavaResultExtractor.getHeaders(result).get(header);
    }

    /**
     * @deprecated use {@link Result#headers()} instead.  
     */
    @Deprecated
    public static Map<String, String> headers(Result result) {
        return JavaResultExtractor.getHeaders(result);
    }

    /**
     * @deprecated use {@link Result#contentType()} instead.  
     */
    @Deprecated
    public static String contentType(Content content) {
        return content.contentType();
    }

    /**
     * @deprecated use {@link Result#contentType()} instead.  
     */
    @Deprecated
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
     * @deprecated use {@link Result#charset()} instead.  
     */
    @Deprecated
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

    @SuppressWarnings(value = "unchecked")
    public static Result routeAndCall(RequestBuilder requestBuilder, long timeout) {
        try {
            return routeAndCall((Class<? extends Router>) RequestBuilder.class.getClassLoader().loadClass("Routes"), requestBuilder, timeout);
        } catch(RuntimeException e) {
            throw e;
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static Result routeAndCall(Class<? extends Router> router, RequestBuilder requestBuilder, long timeout) {
        try {
            Request request = requestBuilder.build();
            Router routes = (Router) router.getClassLoader().loadClass(router.getName() + "$").getDeclaredField("MODULE$").get(null);
            if(routes.routes().isDefinedAt(request._underlyingRequest())) {
                return invokeHandler(routes.routes().apply(request._underlyingRequest()), request, timeout);
            } else {
                return null;
            }
        } catch(RuntimeException e) {
            throw e;
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static Result routeAndCall(Router router, RequestBuilder requestBuilder) {
        return routeAndCall(router, requestBuilder, DEFAULT_TIMEOUT);
    }

    public static Result routeAndCall(Router router, RequestBuilder requestBuilder, long timeout) {
        try {
            Request request = requestBuilder.build();
            if(router.routes().isDefinedAt(request._underlyingRequest())) {
                return invokeHandler(router.routes().apply(request._underlyingRequest()), request, timeout);
            } else {
                return null;
            }
        } catch(RuntimeException e) {
            throw e;
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static Result route(Call call) {
      return route(fakeRequest(call));
    }

    public static Result route(Call call, long timeout) {
      return route(fakeRequest(call), timeout);
    }

    public static Result route(Application app, Call call) {
      return route(app, fakeRequest(call));
    }

    public static Result route(Application app, Call call, long timeout) {
      return route(app, fakeRequest(call), timeout);
    }

    public static Result route(RequestBuilder requestBuilder) {
      return route(requestBuilder, DEFAULT_TIMEOUT);
    }

    public static Result route(RequestBuilder requestBuilder, long timeout) {
      return route(play.Play.application(), requestBuilder, timeout);
    }

    public static Result route(Application app, RequestBuilder requestBuilder) {
      return route(app, requestBuilder, DEFAULT_TIMEOUT);
    }

    @SuppressWarnings("unchecked")
    public static Result route(Application app, RequestBuilder requestBuilder, long timeout) {
      final scala.Option<scala.concurrent.Future<play.api.mvc.Result>> opt = play.api.test.Helpers.jRoute(
          app.getWrappedApplication(), requestBuilder.build()._underlyingRequest(), requestBuilder.bodyAsAnyContent());
      return wrapScalaResult(Scala.orNull(opt), timeout);
    }

    @Deprecated
    public static Result route(Application app, RequestBuilder requestBuilder, byte[] body) {
      return route(app, requestBuilder, body, DEFAULT_TIMEOUT);
    }

    @Deprecated
    public static Result route(Application app, RequestBuilder requestBuilder, byte[] body, long timeout) {
      return wrapScalaResult(Scala.orNull(play.api.test.Helpers.jRoute(app.getWrappedApplication(), requestBuilder.build()._underlyingRequest(), body)), timeout);
    }

    @Deprecated
    public static Result route(RequestBuilder requestBuilder, byte[] body) {
      return route(requestBuilder, body, DEFAULT_TIMEOUT);
    }

    @Deprecated
    public static Result route(RequestBuilder requestBuilder, byte[] body, long timeout) {
      return route(play.Play.application(), requestBuilder, body, timeout);
    }

    /**
     * Starts a new application.
     */
    public static void start(Application application) {
        play.api.Play.start(application.getWrappedApplication());
    }

    /**
     * Stops an application.
     */
    public static void stop(Application application) {
        play.api.Play.stop(application.getWrappedApplication());
    }

    /**
     * Executes a block of code in a running application.
     */
    public static void running(Application application, final Runnable block) {
        synchronized (PlayRunners$.MODULE$.mutex()) {
            try {
                start(application);
                block.run();
            } finally {
                stop(application);
            }
        }
    }


    /**
     * Creates a new Test server listening on port defined by configuration setting "testserver.port" (defaults to 19001).
     */
    public static TestServer testServer() {
        return testServer(play.api.test.Helpers.testServerPort());
    }

    /**
     * Creates a new Test server listening on port defined by configuration setting "testserver.port" (defaults to 19001) and using the given FakeApplication.
     */
    public static TestServer testServer(Application app) {
        return testServer(play.api.test.Helpers.testServerPort(), app);
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
    public static TestServer testServer(int port, Application app) {
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
    public static void running(TestServer server, final Runnable block) {
        synchronized (PlayRunners$.MODULE$.mutex()) {
            try {
                start(server);
                block.run();
            } finally {
                stop(server);
            }
        }
    }

    /**
     * Executes a block of code in a running server, with a test browser.
     */
    public static void running(TestServer server, Class<? extends WebDriver> webDriver, final Callback<TestBrowser> block) {
        running(server, play.api.test.WebDriverFactory.apply(webDriver), block);
    }

    /**
     * Executes a block of code in a running server, with a test browser.
     */
    public static void running(TestServer server, WebDriver webDriver, final Callback<TestBrowser> block) {
        synchronized (PlayRunners$.MODULE$.mutex()) {
            TestBrowser browser = null;
            TestServer startedServer = null;
            try {
                start(server);
                startedServer = server;
                browser = testBrowser(webDriver);
                block.invoke(browser);
            } catch (Error e) {
                throw e;
            } catch (RuntimeException re) {
                throw re;
            } catch (Throwable t) {
                throw new RuntimeException(t);
            } finally {
                if (browser != null) {
                    browser.quit();
                }
                if (startedServer != null) {
                    stop(startedServer);
                }
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
