package play.test;

import play.mvc.*;
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
    
    private static Result invokeHandler(play.api.mvc.Handler handler, FakeRequest fakeRequest) {
        if(handler instanceof play.core.j.JavaAction) {
            play.api.mvc.Action action = (play.api.mvc.Action)handler;
            final play.api.mvc.Result iResult = action.apply(fakeRequest.getWrappedRequest());
            return new Result() {
                
                public play.api.mvc.Result getWrappedResult() {
                    return iResult;
                }

                public String toString() {
                    return iResult.toString();
                }
                
            };
        } else {
            throw new RuntimeException("This is not a JavaAction and can't be invoked this way.");
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
        return new FakeApplication(new java.io.File("."), Helpers.class.getClassLoader(), new HashMap<String,String>());
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
        return Scala.asJava(play.api.test.Helpers.inMemoryDatabase(name));
    }
    
    /**
     * Build a new fake application.
     */
    public static FakeApplication fakeApplication(Map<String,String> additionalConfiguration) {
        return new FakeApplication(new java.io.File("."), Helpers.class.getClassLoader(), additionalConfiguration);
    }
    
    /**
     * Extracts the Status code of this Result value.
     */
    public static int status(Result result) {
        return play.core.j.JavaResultExtractor.getStatus(result);
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
        return new play.mvc.Http.Flash(
            Scala.asJava(play.api.test.Helpers.flash(result.getWrappedResult()).data())
        );
    }
    
    /**
     * Extracts an Header value of this Result value.
     */
    public static String header(String header, Result result) {
        return play.core.j.JavaResultExtractor.getHeaders(result).get(header);
    }
    
    /**
     * Extracts all Headers of this Result value.
     */
    public static Map<String,String> headers(Result result) {
        return play.core.j.JavaResultExtractor.getHeaders(result);
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
        return play.core.j.JavaResultExtractor.getBody(result);
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
     */
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
    public static void running(FakeApplication fakeApplication, final Runnable block) {
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
    public static void running(TestServer server, final Runnable block) {
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
    public static void running(TestServer server, Class<? extends WebDriver> webDriver, final Callback<TestBrowser> block) {
        TestBrowser browser = null;
        try {
            start(server);
            browser = testBrowser(webDriver);
            block.invoke(browser);
        } catch(Throwable t) {
            throw new RuntimeException(t);
        } finally {
            if(browser != null) {
                browser.quit();
            }
            stop(server);
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
    public static TestBrowser testBrowser(Class<? extends WebDriver> webDriver) {
        try {
            return new TestBrowser(webDriver);
        } catch(RuntimeException e) {
            throw e;
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }
    
    /**
     * Creates a Test Browser.
     */
    public static TestBrowser testBrowser(WebDriver of) {
        return new TestBrowser(of);
    }
    
}