package play.test;

import play.*;
import play.mvc.*;
import play.libs.F.*;

import org.openqa.selenium.*;
import org.openqa.selenium.firefox.*;
import org.openqa.selenium.htmlunit.*;

import java.util.*;

public class Helpers implements play.mvc.Http.Status, play.mvc.Http.HeaderNames {
    
    public static String GET = "GET";
    public static String POST = "POST";
    public static String PUT = "PUT";
    public static String DELETE = "DELETE";
    public static String HEAD = "HEAD";
    
    //
    
    public static Class<? extends WebDriver> HTMLUNIT = HtmlUnitDriver.class;
    public static Class<? extends WebDriver> FIREFOX = FirefoxDriver.class;
    
    public static Result callAction(HandlerRef actionReference) {
        return callAction(actionReference, fakeRequest());
    }
    
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
    
    public static Result callAction(HandlerRef actionReference, FakeRequest fakeRequest) {
        play.api.mvc.HandlerRef handlerRef = (play.api.mvc.HandlerRef)actionReference;
        return invokeHandler(handlerRef.handler(), fakeRequest);
    }
    
    public static FakeRequest fakeRequest() {
        return new FakeRequest();
    }
    
    public static FakeRequest fakeRequest(String method, String uri) {
        return new FakeRequest(method, uri);
    }
    
    public static FakeApplication fakeApplication() {
        return new FakeApplication(new java.io.File("."), Helpers.class.getClassLoader());
    }
    
    public static int status(Result result) {
        return play.core.j.JavaResultExtractor.getStatus(result);
    }
    
    public static String header(String header, Result result) {
        return play.core.j.JavaResultExtractor.getHeaders(result).get(header);
    }
    
    public static Map<String,String> headers(Result result) {
        return play.core.j.JavaResultExtractor.getHeaders(result);
    }
    
    public static String contentType(Content content) {
        return content.contentType();
    }
    
    public static String contentType(Result result) {
        String h = header(CONTENT_TYPE, result);
        if(h == null) return null;
        if(h.contains(";")) {
            return h.substring(0, h.indexOf(";")).trim();
        } else {
            return h.trim();
        }
    }
    
    public static String charset(Result result) {
        String h = header(CONTENT_TYPE, result);
        if(h == null) return null;
        if(h.contains("; charset=")) {
            return h.substring(h.indexOf("; charset=") + 10, h.length()).trim();
        } else {
            return null;
        }
    }
    
    public static byte[] contentAsBytes(Result result) {
        return play.core.j.JavaResultExtractor.getBody(result);
    }
    
    public static byte[] contentAsBytes(Content content) {
        return content.body().getBytes();
    }
    
    public static String contentAsString(Content content) {
        return content.body();
    }
    
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
    
    public static Result routeAndCall(FakeRequest fakeRequest) {
        try {
            return routeAndCall((Class<? extends play.core.Router.Routes>)FakeRequest.class.getClassLoader().loadClass("Routes"), fakeRequest);
        } catch(RuntimeException e) {
            throw e;
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }
    
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
    
    public static void start(FakeApplication fakeApplication) {
        play.api.Play.start(fakeApplication.getWrappedApplication());
    }
    
    public static void stop(FakeApplication fakeApplication) {
        play.api.Play.stop();
    }
    
    public static void running(FakeApplication fakeApplication, final Runnable block) {
        try {
            start(fakeApplication);
            block.run();
        } finally {
            stop(fakeApplication);
        }
    }
    
    public static TestServer testServer(int port) {
        return new TestServer(port, fakeApplication());
    }
    
    public static void start(TestServer server) {
        server.start();
    }
    
    public static void stop(TestServer server) {
        server.stop();
    }
    
    public static void running(TestServer server, final Runnable block) {
        try {
            start(server);
            block.run();
        } finally {
            stop(server);
        }
    }
    
    public static void running(TestServer server, Class<? extends WebDriver> webDriver, final Callback<TestBrowser> block) {
        TestBrowser browser = null;
        try {
            start(server);
            browser = testBrowser(webDriver);
            block.invoke(browser);
        } finally {
            if(browser != null) {
                browser.quit();
            }
            stop(server);
        }
    }

    public static TestBrowser testBrowser() {
        return new TestBrowser(new HtmlUnitDriver());
    }
    
    public static TestBrowser testBrowser(Class<? extends WebDriver> webDriver) {
        try {
            return new TestBrowser(webDriver);
        } catch(RuntimeException e) {
            throw e;
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }
    
    public static TestBrowser testBrowser(WebDriver of) {
        return new TestBrowser(of);
    }
    
}