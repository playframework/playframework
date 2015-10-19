/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.test;

import akka.stream.Materializer;
import akka.util.ByteString;
import org.openqa.selenium.WebDriver;
import play.*;

import play.api.routing.Router;
import play.api.test.PlayRunners$;
import play.core.j.JavaHandler;
import play.core.j.JavaHandlerComponents;
import play.core.j.JavaResultExtractor;
import play.http.HttpEntity;
import play.mvc.*;
import play.api.test.Helpers$;
import play.libs.*;
import play.libs.F.*;
import play.twirl.api.Content;

import org.openqa.selenium.firefox.*;
import org.openqa.selenium.htmlunit.*;


import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

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
            return scalaResult.asJava();
        }
    }

    // --

    /**
     * Calls a Callable which invokes a Controller or some other method with a Context
     */
    public static <V> V invokeWithContext(RequestBuilder requestBuilder, Callable<V> callable) {
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
     * Extracts the content as a {@link akka.util.ByteString}.
     *
     * This method is only capable of extracting the content of results with strict entities. To extract the content of
     * results with streamed entities, use {@link #contentAsBytes(Result, Materializer)}.
     *
     * @param result The result to extract the content from.
     * @return The content of the result as a ByteString.
     * @throws UnsupportedOperationException if the result does not have a strict entity.
     */
    public static ByteString contentAsBytes(Result result) {
        if (result.body() instanceof HttpEntity.Strict) {
            return ((HttpEntity.Strict) result.body()).data();
        } else {
            throw new UnsupportedOperationException("Tried to extract body from a non strict HTTP entity without a materializer, use the version of this method that accepts a materializer instead");
        }
    }

    /**
     * Extracts the content as a {@link akka.util.ByteString}.
     *
     * @param result The result to extract the content from.
     * @param mat The materialiser to use to extract the body from the result stream.
     * @return The content of the result as a ByteString.
     */
    public static ByteString contentAsBytes(Result result, Materializer mat) {
        return contentAsBytes(result, mat, DEFAULT_TIMEOUT);
    }

    /**
     * Extracts the content as a {@link akka.util.ByteString}.
     *
     * @param result The result to extract the content from.
     * @param mat The materialiser to use to extract the body from the result stream.
     * @param timeout The amount of time, in milliseconds, to wait for the body to be produced.
     * @return The content of the result as a ByteString.
     */
    public static ByteString contentAsBytes(Result result, Materializer mat, long timeout) {
        try {
            return result.body().consumeData(mat).thenApply(Function.identity()).toCompletableFuture().get(timeout, TimeUnit.MILLISECONDS);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts the content as bytes.
     */
    public static ByteString contentAsBytes(Content content) {
        return ByteString.fromString(content.body());
    }

    /**
     * Extracts the content as a String.
     */
    public static String contentAsString(Content content) {
        return content.body();
    }

    /**
     * Extracts the content as a String.
     *
     * This method is only capable of extracting the content of results with strict entities. To extract the content of
     * results with streamed entities, use {@link #contentAsString(Result, Materializer)}.
     *
     * @param result The result to extract the content from.
     * @return The content of the result as a String.
     * @throws UnsupportedOperationException if the result does not have a strict entity.
     */
    public static String contentAsString(Result result) {
        return contentAsBytes(result)
                .decodeString(result.charset().orElse("utf-8"));
    }

    /**
     * Extracts the content as a String.
     *
     * @param result The result to extract the content from.
     * @param mat The materialiser to use to extract the body from the result stream.
     * @return The content of the result as a String.
     */
    public static String contentAsString(Result result, Materializer mat) {
        return contentAsBytes(result, mat, DEFAULT_TIMEOUT)
            .decodeString(result.charset().orElse("utf-8"));
    }

    /**
     * Extracts the content as a String.
     *
     * @param result The result to extract the content from.
     * @param mat The materialiser to use to extract the body from the result stream.
     * @param timeout The amount of time, in milliseconds, to wait for the body to be produced.
     * @return The content of the result as a String.
     */
    public static String contentAsString(Result result, Materializer mat, long timeout) {
        return contentAsBytes(result, mat, timeout)
                .decodeString(result.charset().orElse("utf-8"));
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
          app.getWrappedApplication(), requestBuilder.build()._underlyingRequest(), requestBuilder.body());
      return wrapScalaResult(Scala.orNull(opt), timeout);
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
    public static void running(TestServer server, Class<? extends WebDriver> webDriver, final Consumer<TestBrowser> block) {
        running(server, play.api.test.WebDriverFactory.apply(webDriver), block);
    }

    /**
     * Executes a block of code in a running server, with a test browser.
     */
    public static void running(TestServer server, WebDriver webDriver, final Consumer<TestBrowser> block) {
        synchronized (PlayRunners$.MODULE$.mutex()) {
            TestBrowser browser = null;
            TestServer startedServer = null;
            try {
                start(server);
                startedServer = server;
                browser = testBrowser(webDriver);
                block.accept(browser);
            } 
            finally {
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
