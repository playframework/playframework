/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

import akka.stream.Materializer;
import akka.util.ByteString;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import play.Application;
import play.api.i18n.DefaultLangs;
import play.api.test.Helpers$;
import play.core.j.JavaContextComponents;
import play.core.j.JavaHandler;
import play.core.j.JavaHandlerComponents;
import play.core.j.JavaHelpers$;
import play.http.HttpEntity;
import play.i18n.MessagesApi;
import play.inject.guice.GuiceApplicationBuilder;
import play.libs.Scala;
import play.mvc.Call;
import play.mvc.Http;
import play.mvc.Result;
import play.routing.Router;
import play.twirl.api.Content;
import scala.compat.java8.FutureConverters;
import scala.compat.java8.OptionConverters;

import static play.libs.Scala.asScala;
import static play.mvc.Http.Context;
import static play.mvc.Http.Request;
import static play.mvc.Http.RequestBuilder;

/**
 * Helper functions to run tests.
 */
public class Helpers implements play.mvc.Http.Status, play.mvc.Http.HeaderNames {

    /**
     * Default Timeout (milliseconds) for fake requests issued by these Helpers.
     * This value is determined from System property <b>test.timeout</b>.
     * The default value is <b>30000</b> (30 seconds).
     */
    public static final long DEFAULT_TIMEOUT = Long.getLong("test.timeout", 30000L);
    public static String GET = "GET";
    public static String POST = "POST";
    public static String PUT = "PUT";
    public static String DELETE = "DELETE";

    // --
    public static String HEAD = "HEAD";
    public static Class<? extends WebDriver> HTMLUNIT = HtmlUnitDriver.class;
    public static Class<? extends WebDriver> FIREFOX = FirefoxDriver.class;

    // --
    @SuppressWarnings(value = "unchecked")
    private static Result invokeHandler(play.api.Application app, play.api.mvc.Handler handler, Request requestBuilder, long timeout) {
        if (handler instanceof play.api.mvc.Action) {
            play.api.mvc.Action action = (play.api.mvc.Action) handler;
            return wrapScalaResult(action.apply(requestBuilder.asScala()), timeout);
        } else if (handler instanceof JavaHandler) {
            final play.api.inject.Injector injector = app.injector();
            final JavaHandlerComponents handlerComponents = injector.instanceOf(JavaHandlerComponents.class);
            return invokeHandler(
                    app,
                    ((JavaHandler) handler).withComponents(handlerComponents),
                    requestBuilder, timeout
            );
        } else {
            throw new RuntimeException("This is not a JavaAction and can't be invoked this way.");
        }
    }

    private static Result wrapScalaResult(scala.concurrent.Future<play.api.mvc.Result> result, long timeout) {
        if (result == null) {
            return null;
        } else {
            try {
                final play.api.mvc.Result scalaResult = FutureConverters.toJava(result).toCompletableFuture().get(timeout, TimeUnit.MILLISECONDS);
                return scalaResult.asJava();
            } catch (ExecutionException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new RuntimeException(e.getCause());
                }
            } catch (InterruptedException | TimeoutException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // --

    /**
     * Calls a Callable which invokes a Controller or some other method with a Context.
     *
     * @param requestBuilder the request builder to invoke in this context.
     * @param contextComponents the context components to run.
     * @param callable the callable block to run.
     * @param <V> the return type.
     * @return the value from {@code callable}.
     */
    public static <V> V invokeWithContext(RequestBuilder requestBuilder, JavaContextComponents contextComponents, Callable<V> callable) {
        try {
            Context.current.set(new Context(requestBuilder, contextComponents));
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Context.current.remove();
        }
    }

    /**
     * Builds a new "GET /" fake request.
     * @return the request builder.
     */
    public static RequestBuilder fakeRequest() {
        return fakeRequest("GET", "/");
    }

    /**
     * Builds a new fake request.
     * @param method    the request method.
     * @param uri the relative URL.
     * @return the request builder.
     */
    public static RequestBuilder fakeRequest(String method, String uri) {
        return new RequestBuilder().method(method).uri(uri);
    }

    /**
     * Builds a new fake request corresponding to a given route call.
     * @param call    the route call.
     * @return the request builder.
     */
    public static RequestBuilder fakeRequest(Call call) {
        return fakeRequest(call.method(), call.url());
    }

    /**
     * Builds a new Http.Context from a new request
     * @return a new Http.Context using the default request
     */
    public static Http.Context httpContext() {
        return httpContext(new Http.RequestBuilder().build());
    }

    /**
     * Builds a new Http.Context for a specific request
     * @param request the Request you want to use for this Context
     * @return a new Http.Context for this request
     */
    public static Http.Context httpContext(Http.Request request) {
        return new Http.Context(request, contextComponents());
    }

    /**
     * Creates a new JavaContextComponents using play.api.Configuration.reference and play.api.Environment.simple as defaults
     * @return the newly created JavaContextComponents
     */
    public static JavaContextComponents contextComponents() {
        return JavaHelpers$.MODULE$.createContextComponents();
    }

    /**
     * Builds a new fake application, using GuiceApplicationBuilder.
     *
     * @return an application from the current path with no additional configuration.
     */
    public static Application fakeApplication() {
        return new GuiceApplicationBuilder().build();
    }

    /**
     * Constructs a in-memory (h2) database configuration to add to a fake application.
     *
     * @return a map of String containing database config info.
     */
    public static Map<String, String> inMemoryDatabase() {
        return inMemoryDatabase("default");
    }

    /**
     * Constructs a in-memory (h2) database configuration to add to a fake application.
     *
     * @param name    the database name.
     * @return a map of String containing database config info.
     */
    public static Map<String, String> inMemoryDatabase(String name) {
        return inMemoryDatabase(name, Collections.<String, String>emptyMap());
    }

    /**
     * Constructs a in-memory (h2) database configuration to add to a fake application.
     *
     * @param name       the database name.
     * @param options the database options.
     * @return a map of String containing database config info.
     */
    public static Map<String, String> inMemoryDatabase(String name, Map<String, String> options) {
        return Scala.asJava(play.api.test.Helpers.inMemoryDatabase(name, Scala.asScala(options)));
    }

    /**
     * Constructs an empty messagesApi instance.
     *
     * @return a messagesApi instance containing no values.
     */
    public static MessagesApi stubMessagesApi() {
        return new play.i18n.MessagesApi(new play.api.i18n.DefaultMessagesApi
                (Collections.emptyMap(), new DefaultLangs().asJava()));
    }

    /**
     * Constructs a MessagesApi instance containing the given keys and values.
     *
     * @return a messagesApi instance containing given keys and values.
     */
    public static MessagesApi stubMessagesApi(Map<String, Map<String, String>> messages, play.i18n.Langs langs) {
        return new play.i18n.MessagesApi(
            new play.api.i18n.DefaultMessagesApi(messages, langs)
        );
    }

    /**
     * Build a new fake application.  Uses GuiceApplicationBuilder.
     *
     * @param additionalConfiguration map containing config info for the app.
     * @return an application from the current path with additional configuration.
     */
    public static Application fakeApplication(Map<String, ? extends Object> additionalConfiguration) {
        //noinspection unchecked
        Map<String, Object> conf = (Map<String, Object>) additionalConfiguration;
        return new GuiceApplicationBuilder().configure(conf).build();
    }

    /**
     * Extracts the content as a {@link akka.util.ByteString}.
     * <p>
     * This method is only capable of extracting the content of results with strict entities. To extract the content of
     * results with streamed entities, use {@link Helpers#contentAsBytes(Result, Materializer)}.
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
     * @param mat    The materializer to use to extract the body from the result stream.
     * @return The content of the result as a ByteString.
     */
    public static ByteString contentAsBytes(Result result, Materializer mat) {
        return contentAsBytes(result, mat, DEFAULT_TIMEOUT);
    }

    /**
     * Extracts the content as a {@link akka.util.ByteString}.
     *
     * @param result  The result to extract the content from.
     * @param mat     The materializer to use to extract the body from the result stream.
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
     *
     * @param content the content to be turned into bytes.
     * @return the body of the content as a byte string.
     */
    public static ByteString contentAsBytes(Content content) {
        return ByteString.fromString(content.body());
    }

    /**
     * Extracts the content as a String.
     *
     * @param content the content.
     * @return the body of the content as a String.
     */
    public static String contentAsString(Content content) {
        return content.body();
    }

    /**
     * Extracts the content as a String.
     * <p>
     * This method is only capable of extracting the content of results with strict entities. To extract the content of
     * results with streamed entities, use {@link Helpers#contentAsString(Result, Materializer)}.
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
     * @param mat    The materializer to use to extract the body from the result stream.
     * @return The content of the result as a String.
     */
    public static String contentAsString(Result result, Materializer mat) {
        return contentAsBytes(result, mat, DEFAULT_TIMEOUT)
                .decodeString(result.charset().orElse("utf-8"));
    }

    /**
     * Extracts the content as a String.
     *
     * @param result  The result to extract the content from.
     * @param mat     The materializer to use to extract the body from the result stream.
     * @param timeout The amount of time, in milliseconds, to wait for the body to be produced.
     * @return The content of the result as a String.
     */
    public static String contentAsString(Result result, Materializer mat, long timeout) {
        return contentAsBytes(result, mat, timeout)
                .decodeString(result.charset().orElse("utf-8"));
    }

    /**
     * Route and call the request, respecting the given timeout.
     *
     * @param app The application used while routing and executing the request
     * @param requestBuilder The request builder
     * @param timeout The amount of time, in milliseconds, to wait for the body to be produced.
     * @return the result
     */
    public static Result routeAndCall(Application app, RequestBuilder requestBuilder, long timeout) {
        try {
            return routeAndCall(app, (Class<? extends Router>) RequestBuilder.class.getClassLoader().loadClass("Routes"), requestBuilder, timeout);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Route and call the request, respecting the given timeout.
     *
     * @param app The application used while routing and executing the request
     * @param router The router type
     * @param requestBuilder The request builder
     * @param timeout The amount of time, in milliseconds, to wait for the body to be produced.
     * @return the result
     */
    public static Result routeAndCall(Application app, Class<? extends Router> router, RequestBuilder requestBuilder, long timeout) {
        try {
            Request request = requestBuilder.build();
            Router routes = (Router) router.getClassLoader().loadClass(router.getName() + "$").getDeclaredField("MODULE$").get(null);
            return routes.route(request).map(handler ->
                invokeHandler(app.getWrappedApplication(), handler, request, timeout)
            ).orElse(null);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Route and call the request.
     *
     * @param app The application used while routing and executing the request
     * @param router The router
     * @param requestBuilder The request builder
     * @return the result
     */
    public static Result routeAndCall(Application app, Router router, RequestBuilder requestBuilder) {
        return routeAndCall(app, router, requestBuilder, DEFAULT_TIMEOUT);
    }

    /**
     * Route and call the request, respecting the given timeout.
     *
     * @param app The application used while routing and executing the request
     * @param router The router
     * @param requestBuilder The request builder
     * @param timeout The amount of time, in milliseconds, to wait for the body to be produced.
     * @return the result
     */
    public static Result routeAndCall(Application app, Router router, RequestBuilder requestBuilder, long timeout) {
        try {
            Request request = requestBuilder.build();
            return router.route(request).map(handler ->
                    invokeHandler(app.getWrappedApplication(), handler, request, timeout)
            ).orElse(null);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Route a call using the given application.
     *
     * @param app the application
     * @param call the call to route
     * @see GuiceApplicationBuilder
     * @return the result
     */
    public static Result route(Application app, Call call) {
        return route(app, fakeRequest(call));
    }

    /**
     * Route a call using the given application and timeout.
     *
     * @param app the application
     * @param call the call to route
     * @param timeout the time out
     * @see GuiceApplicationBuilder
     * @return the result
     */
    public static Result route(Application app, Call call, long timeout) {
        return route(app, fakeRequest(call), timeout);
    }

    /**
     * Route a request.
     *
     * @param app The application used while routing and executing the request
     * @param requestBuilder the request builder
     * @return the result.
     */
    public static Result route(Application app, RequestBuilder requestBuilder) {
        return route(app, requestBuilder, DEFAULT_TIMEOUT);
    }

    /**
     * Route the request considering the given timeout.
     *
     * @param app The application used while routing and executing the request
     * @param requestBuilder the request builder
     * @param timeout the amount of time, in milliseconds, to wait for the body to be produced.
     * @return the result
     */
    @SuppressWarnings("unchecked")
    public static Result route(Application app, RequestBuilder requestBuilder, long timeout) {
        final scala.Option<scala.concurrent.Future<play.api.mvc.Result>> opt = play.api.test.Helpers.jRoute(
                app.getWrappedApplication(),
                requestBuilder.build().asScala(),
                requestBuilder.body()
        );
        return wrapScalaResult(Scala.orNull(opt), timeout);
    }

    /**
     * Starts a new application.
     *
     * @param application the application to start.
     */
    public static void start(Application application) {
        play.api.Play.start(application.getWrappedApplication());
    }

    /**
     * Stops an application.
     *
     * @param application the application to stop.
     */
    public static void stop(Application application) {
        play.api.Play.stop(application.getWrappedApplication());
    }

    /**
     * Executes a block of code in a running application.
     *
     * @param application the application context.
     * @param block       the block to run after the Play app is started.
     */
    public static void running(Application application, final Runnable block) {
        Helpers$.MODULE$.running(application.getWrappedApplication(), asScala(() -> { block.run(); return null; }));
    }

    /**
     * Creates a new Test server listening on port defined by configuration setting "testserver.port" (defaults to 19001).
     *
     * @return the test server.
     */
    public static TestServer testServer() {
        return testServer(play.api.test.Helpers.testServerPort());
    }

    /**
     * Creates a new Test server listening on port defined by configuration setting "testserver.port" (defaults to 19001) and using the given Application.
     *
     * @param app the application.
     * @return the test server.
     */
    public static TestServer testServer(Application app) {
        return testServer(play.api.test.Helpers.testServerPort(), app);
    }

    /**
     * Creates a new Test server.
     *
     * @param port    the port to run the server on.
     * @return the test server.
     */
    public static TestServer testServer(int port) {
        return new TestServer(port, fakeApplication());
    }

    /**
     * Creates a new Test server.
     *
     *
     * @param port    the port to run the server on.
     * @param app     the Play application.
     * @return the test server.
     */
    public static TestServer testServer(int port, Application app) {
        return new TestServer(port, app);
    }

    /**
     * Starts a Test server.
     * @param server    the test server to start.
     */
    public static void start(TestServer server) {
        server.start();
    }

    /**
     * Stops a Test server.
     * @param server    the test server to stop.a
     */
    public static void stop(TestServer server) {
        server.stop();
    }

    /**
     * Executes a block of code in a running server.
     * @param server    the server to start.
     * @param block  the block of code to run after the server starts.
     */
    public static void running(TestServer server, final Runnable block) {
        Helpers$.MODULE$.running(server, asScala(() -> { block.run(); return null; }));
    }

    /**
     * Executes a block of code in a running server, with a test browser.
     *
     * @param server    the test server.
     * @param webDriver the web driver class.
     * @param block     the block of code to execute.
     */
    public static void running(TestServer server, Class<? extends WebDriver> webDriver, final Consumer<TestBrowser> block) {
        running(server, play.api.test.WebDriverFactory.apply(webDriver), block);
    }

    /**
     * Executes a block of code in a running server, with a test browser.
     *
     * @param server    the test server.
     * @param webDriver the web driver instance.
     * @param block     the block of code to execute.
     */
    public static void running(TestServer server, WebDriver webDriver, final Consumer<TestBrowser> block) {
        Helpers$.MODULE$.runSynchronized(server.application(), asScala(() -> {
            TestBrowser browser = null;
            TestServer startedServer = null;
            try {
                start(server);
                startedServer = server;
                browser = testBrowser(webDriver, (Integer) OptionConverters.toJava(server.config().port()).get());
                block.accept(browser);
            } finally {
                if (browser != null) {
                    browser.quit();
                }
                if (startedServer != null) {
                    stop(startedServer);
                }
            }
            return null;
        }));
    }

    /**
     * Creates a Test Browser.
     *
     * @return the test browser.
     */
    public static TestBrowser testBrowser() {
        return testBrowser(HTMLUNIT);
    }

    /**
     * Creates a Test Browser.
     *
     * @param port    the local port.
     * @return the test browser.
     */
    public static TestBrowser testBrowser(int port) {
        return testBrowser(HTMLUNIT, port);
    }

    /**
     * Creates a Test Browser.
     *
     * @param webDriver    the class of webdriver.
     * @return the test browser.
     */
    public static TestBrowser testBrowser(Class<? extends WebDriver> webDriver) {
        return testBrowser(webDriver, Helpers$.MODULE$.testServerPort());
    }

    /**
     * Creates a Test Browser.
     *
     * @param webDriver    the class of webdriver.
     * @param port  the local port to test against.
     * @return the test browser.
     */
    public static TestBrowser testBrowser(Class<? extends WebDriver> webDriver, int port) {
        try {
            return new TestBrowser(webDriver, "http://localhost:" + port);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Creates a Test Browser.
     *
     * @param of      the web driver to run the browser with.
     * @param port    the port to run against http://localhost
     * @return the test browser.
     */
    public static TestBrowser testBrowser(WebDriver of, int port) {
        return new TestBrowser(of, "http://localhost:" + port);
    }

    /**
     * Creates a Test Browser.
     *
     * @param of      the web driver to run the browser with.
     * @return the test browser.
     */
    public static TestBrowser testBrowser(WebDriver of) {
        return testBrowser(of, Helpers$.MODULE$.testServerPort());
    }

}
