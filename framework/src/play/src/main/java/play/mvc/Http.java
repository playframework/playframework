/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.mvc;

import static play.libs.Scala.asScala;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import akka.util.ByteString;
import akka.util.ByteString$;
import akka.util.ByteStringBuilder;
import akka.util.CompactByteString;
import play.core.j.JavaParsers;
import play.libs.Json;
import play.libs.XML;
import scala.Predef;
import scala.Tuple2;
import scala.collection.JavaConversions;
import scala.collection.JavaConverters;
import scala.collection.Seq;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import com.fasterxml.jackson.databind.JsonNode;

import play.api.libs.json.JsValue;
import play.api.libs.json.jackson.JacksonJson;
import play.api.mvc.AnyContent;
import play.api.mvc.AnyContentAsFormUrlEncoded;
import play.api.mvc.AnyContentAsJson;
import play.api.mvc.AnyContentAsRaw;
import play.api.mvc.AnyContentAsText;
import play.api.mvc.AnyContentAsXml;
import play.api.mvc.Headers;
import play.core.system.RequestIdProvider;
import play.Play;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;

/**
 * Defines HTTP standard objects.
 */
public class Http {

    /**
     * The global HTTP context.
     */
    public static class Context {

        public static ThreadLocal<Context> current = new ThreadLocal<Context>();

        /**
         * Retrieves the current HTTP context, for the current thread.
         */
        public static Context current() {
            Context c = current.get();
            if(c == null) {
                throw new RuntimeException("There is no HTTP Context available from here.");
            }
            return c;
        }

        //

        private final Long id;
        private final play.api.mvc.RequestHeader header;
        private final Request request;
        private final Response response;
        private final Session session;
        private final Flash flash;

        private Lang lang = null;

        /**
         * Creates a new HTTP context.
         *
         * @param requestBuilder the HTTP request builder
         */
        public Context(RequestBuilder requestBuilder) {
            this(requestBuilder.build());
        }

        /**
         * Creates a new HTTP context.
         *
         * @param request the HTTP request
         */
        public Context(Request request) {
            this.request = request;
            this.header = request._underlyingHeader();
            this.id = header.id();
            this.response = new Response();
            this.session = new Session(JavaConversions.mapAsJavaMap(header.session().data()));
            this.flash = new Flash(JavaConversions.mapAsJavaMap(header.flash().data()));
            this.args = new HashMap<String,Object>();
            this.args.putAll(JavaConversions.mapAsJavaMap(header.tags()));
        }

        /**
         * Creates a new HTTP context.
         *
         * @param request the HTTP request
         * @param sessionData the session data extracted from the session cookie
         * @param flashData the flash data extracted from the flash cookie
         */
        public Context(Long id, play.api.mvc.RequestHeader header, Request request, Map<String,String> sessionData, Map<String,String> flashData, Map<String,Object> args) {
            this.id = id;
            this.header = header;
            this.request = request;
            this.response = new Response();
            this.session = new Session(sessionData);
            this.flash = new Flash(flashData);
            this.args = new HashMap<String,Object>(args);
        }

        /**
         * The context id (unique)
         */
        public Long id() {
            return id;
        }

        /**
         * Returns the current request.
         */
        public Request request() {
            return request;
        }

        /**
         * Returns the current response.
         */
        public Response response() {
            return response;
        }

        /**
         * Returns the current session.
         */
        public Session session() {
            return session;
        }

        /**
         * Returns the current flash scope.
         */
        public Flash flash() {
            return flash;
        }

        /**
         * The original Play request Header used to create this context.
         * For internal usage only.
         */
        public play.api.mvc.RequestHeader _requestHeader() {
            return header;
        }

        /**
         * @return the current lang
         */
        public Lang lang() {
            if (lang != null) {
                return lang;
            } else {
                return messages().lang();
            }
        }

        /**
         * @return the messages for the current lang
         */
        public Messages messages() {
            return Play.application().injector().instanceOf(MessagesApi.class).preferred(request());
        }

        /**
         * Change durably the lang for the current user.
         * @param code New lang code to use (e.g. "fr", "en-US", etc.)
         * @return true if the requested lang was supported by the application, otherwise false
         */
        public boolean changeLang(String code) {
            return changeLang(Lang.forCode(code));
        }

        /**
         * Change durably the lang for the current user.
         * @param lang New Lang object to use
         * @return true if the requested lang was supported by the application, otherwise false
         */
        public boolean changeLang(Lang lang) {
            if (Lang.availables().contains(lang)) {
                this.lang = lang;
                scala.Option<String> domain = play.api.mvc.Session.domain();
                response.setCookie(Play.langCookieName(), lang.code(), null, play.api.mvc.Session.path(),
                    domain.isDefined() ? domain.get() : null, Play.langCookieSecure(), Play.langCookieHttpOnly());
                return true;
            } else {
                return false;
            }
        }

        /**
         * Clear the lang for the current user.
         */
        public void clearLang() {
            this.lang = null;
            scala.Option<String> domain = play.api.mvc.Session.domain();
            response.discardCookie(Play.langCookieName(), play.api.mvc.Session.path(),
                domain.isDefined() ? domain.get() : null, Play.langCookieSecure());
        }

        /**
         * Set the language for the current request, but don't
         * change the language cookie. This means the language
         * will be set for this request, but will not change for
         * future requests.
         *
         * @throws IllegalArgumentException If the given language
         * is not supported by the application.
         */
        public void setTransientLang(String code) {
            setTransientLang(Lang.forCode(code));
        }

        /**
         * Set the language for the current request, but don't
         * change the language cookie. This means the language
         * will be set for this request, but will not change for
         * future requests.
         *
         * @throws IllegalArgumentException If the given language
         * is not supported by the application.
         */
        public void setTransientLang(Lang lang) {
            if (Lang.availables().contains(lang)) {
                this.lang = lang;
            } else {
                throw new IllegalArgumentException("Language not supported in this application: " + lang + " not in Lang.availables()");
            }
        }

        /**
         * Clear the language for the current request, but don't
         * change the language cookie. This means the language
         * will be cleared for this request (so a default will be
         * used), but will not change for future requests.
         */
        public void clearTransientLang() {
            this.lang = null;
        }

        /**
         * Free space to store your request specific data.
         */
        public Map<String, Object> args;

        /**
         * Import in templates to get implicit HTTP context.
         */
        public static class Implicit {

            /**
             * Returns the current response.
             */
            public static Response response() {
                return Context.current().response();
            }

            /**
             * Returns the current request.
             */
            public static Request request() {
                return Context.current().request();
            }

            /**
             * Returns the current flash scope.
             */
            public static Flash flash() {
                return Context.current().flash();
            }

            /**
             * Returns the current session.
             */
            public static Session session() {
                return Context.current().session();
            }

            /**
             * Returns the current lang.
             */
            public static Lang lang() {
                return Context.current().lang();
            }

            /**
             * @return the messages for the current lang
             */
            public static Messages messages() {
                return Context.current().messages();
            }

            /**
             * Returns the current context.
             */
            public static Context ctx() {
                return Context.current();
            }

        }

        /**
         * @return a String representation
         */
        public String toString() {
            return "Context attached to (" + request() + ")";
        }

        /**
         * Create a new context with the given request.
         *
         * The id, Scala RequestHeader, session, flash and args remain unchanged.
         *
         * @param request The request to create the new header from.
         * @return The new context.
         */
        public Context withRequest(Request request) {
            return new Context(id, header, request, session, flash, args);
        }
    }

    /**
     * A wrapped context.
     * Use this to modify the context in some way.
     */
    public static abstract class WrappedContext extends Context {
        private final Context wrapped;

        /**
         * @param wrapped
         */
        public WrappedContext(Context wrapped) {
            super(wrapped.id(), wrapped._requestHeader(), wrapped.request(), wrapped.session(), wrapped.flash(), wrapped.args);
            this.args = wrapped.args;
            this.wrapped = wrapped;
        }

        @Override
        public Long id() {
            return wrapped.id();
        }

        @Override
        public Request request() {
            return wrapped.request();
        }

        @Override
        public Response response() {
            return wrapped.response();
        }

        @Override
        public Session session() {
            return wrapped.session();
        }

        @Override
        public Flash flash() {
            return wrapped.flash();
        }

        @Override
        public play.api.mvc.RequestHeader _requestHeader() {
            return wrapped._requestHeader();
        }

        @Override
        public Lang lang() {
            return wrapped.lang();
        }

        @Override
        public boolean changeLang(String code) {
            return wrapped.changeLang(code);
        }

        @Override
        public boolean changeLang(Lang lang) {
            return wrapped.changeLang(lang);
        }

        @Override
        public void clearLang() {
            wrapped.clearLang();
        }
    }

    public static interface RequestHeader {

        /**
         * The complete request URI, containing both path and query string.
         */
        String uri();

        /**
         * The HTTP Method.
         */
        String method();

        /**
         * The HTTP version.
         */
        String version();

        /**
         * The client IP address.
         *
         * retrieves the last untrusted proxy
         * from the Forwarded-Headers or the X-Forwarded-*-Headers.
         */
        String remoteAddress();

        /**
         * Is the client using SSL?
         *
         */
        boolean secure();

        /**
         * The request host.
         */
        String host();

        /**
         * The URI path.
         */
        String path();

        /**
         * The Request Langs extracted from the Accept-Language header and sorted by preference (preferred first).
         */
        List<play.i18n.Lang> acceptLanguages();

        /**
         * @return The media types set in the request Accept header, sorted by preference (preferred first)
         */
        List<play.api.http.MediaRange> acceptedTypes();

        /**
         * Check if this request accepts a given media type.
         * @return true if <code>mimeType</code> is in the Accept header, otherwise false
         */
        boolean accepts(String mimeType);

        /**
         * The query string content.
         */
        Map<String,String[]> queryString();

        /**
         * Helper method to access a queryString parameter.
         */
        String getQueryString(String key);

        /**
         * @return the request cookies
         */
        Cookies cookies();

        /**
         * @param name Name of the cookie to retrieve
         * @return the cookie, if found, otherwise null
         */
        Cookie cookie(String name);

        /**
         * Retrieves all headers.
         *
         * @return a map of of header name to headers with case-insensitive keys
         */
        Map<String,String[]> headers();

        /**
         * Retrieves a single header.
         *
         * @param headerName The name of the header (case-insensitive)
         */
        String getHeader(String headerName);

        /**
         * Checks if the request has the header.
         *
         * @param headerName The name of the header (case-insensitive)
         */
        boolean hasHeader(String headerName);

        /**
         * Get the content type of the request.
         *
         * @return The request content type excluding the charset, if it exists.
         */
        Optional<String> contentType();

        /**
         * Get the charset of the request.
         *
         * @return The request charset, which comes from the content type header, if it exists.
         */
        Optional<String> charset();

        /**
         * For internal Play-use only
         */
        play.api.mvc.RequestHeader _underlyingHeader();
    }

    /**
     * An HTTP request.
     */
    public static interface Request extends RequestHeader {

        /**
         * The request body.
         */
        RequestBody body();

        /**
         * The user name for this request, if defined.
         * This is usually set by annotating your Action with <code>@Authenticated</code>.
         */
        String username();

        /**
         * Defines the user name for this request.
         * @deprecated As of release 2.4, use {@link #withUsername}
         */
        @Deprecated void setUsername(String username);

        /**
         * Returns a request updated with specified user name
         * @param username the new user name
         */
        Request withUsername(String username);

        /**
         * For internal Play-use only
         */
        play.api.mvc.Request<RequestBody> _underlyingRequest();
    }

    /**
     * An HTTP request.
     */
    public static class RequestImpl extends play.core.j.RequestHeaderImpl implements Request {

        private final play.api.mvc.Request<RequestBody> underlying;
        private String username; // Keep it non-final until setUsername is removed

        /**
         * Constructor only based on a header.
         * @param header the header from a request
         */
        public RequestImpl(play.api.mvc.RequestHeader header) {
            super(header);
            this.underlying = null;
        }

        /**
         * Constructor with a requestbody.
         * @param request the body of the request
         */
        public RequestImpl(play.api.mvc.Request<RequestBody> request) {
            super(request);
            this.underlying = request;
        }

        /**
         * Constructor with a request and a username.
         * @param request he body of the request
         * @param username the user which is making the request
         */
        private RequestImpl(play.api.mvc.Request<RequestBody> request,
                            String username) {

            super(request);

            this.underlying = request;
            this.username = username;
        }

        /**
         * @return the underlying body, if present otherwise null
         */
        public RequestBody body() {
            return underlying != null ? underlying.body() : null;
        }

        /**
         * @return the username
         */
        public String username() {
            return username;
        }

        /**
         * Sets the username.
         * @param username the username of the requester
         */
        public void setUsername(String username) {
            this.username = username;
        }

        /**
         * This method returns a new request, based on the current underlying with a giving username.
         * @param username the new user name
         * @return a new request with a request body based on the current request
         */
        public Request withUsername(String username) {
            return new RequestImpl(this.underlying, username);
        }

        /**
         * @return the underlying body of the request
         */
        public play.api.mvc.Request<RequestBody> _underlyingRequest() {
            return underlying;
        }

    }

    /**
     * The builder for building a request.
     */
    public static class RequestBuilder {

        protected RequestBody body;
        protected String username;

        /**
         * Returns a simple request builder, based on get and local address.
         */
        public RequestBuilder() {
          method("GET");
          uri("/");
          host("localhost");
          version("HTTP/1.1");
          remoteAddress("127.0.0.1");
          body(new RequestBody(null));
        }

        /**
         * @return the request body, if a previously the body has been set
         */
        public RequestBody body() {
            return body;
        }

        /**
         * @return the username
         */
        public String username() {
            return username;
        }

        /**
         * @param username the username for the request
         * @return the builder
         */
        public RequestBuilder username(String username) {
            this.username = username;
            return this;
        }

        /**
         * Set the body of the request.
         *
         * @param body the body
         * @param contentType Content-Type header value
         */
        protected RequestBuilder body(RequestBody body, String contentType) {
            header("Content-Type", contentType);
            body(body);
            return this;
        }

        /**
         * Set the body of the request.
         *
         * @param body The body.
         */
        protected RequestBuilder body(RequestBody body) {
            this.body = body;
            return this;
        }

        /**
         * Set a Binary Data to this request.
         * The <tt>Content-Type</tt> header of the request is set to <tt>application/octet-stream</tt>.
         * @param data the Binary Data
         */
        public RequestBuilder bodyRaw(ByteString data) {
            play.api.mvc.RawBuffer buffer = new play.api.mvc.RawBuffer(data.size(), data);
            return body(new RequestBody(JavaParsers.toJavaRaw(buffer)), "application/octet-stream");
        }

        /**
         * Set a Binary Data to this request.
         * The <tt>Content-Type</tt> header of the request is set to <tt>application/octet-stream</tt>.
         * @param data the Binary Data
         */
        public RequestBuilder bodyRaw(byte[] data) {
            return bodyRaw(ByteString.fromArray(data));
        }

        /**
         * Set a Form url encoded body to this request.
         */
        public RequestBuilder bodyFormArrayValues(Map<String, String[]> data) {
            return body(new RequestBody(data), "application/x-www-form-urlencoded");
        }

        /**
         * Set a Form url encoded body to this request.
         */
        public RequestBuilder bodyForm(Map<String, String> data) {
            Map<String, String[]> arrayValues = new HashMap<>();
            for (Entry<String, String> entry: data.entrySet()) {
                arrayValues.put(entry.getKey(), new String[]{entry.getValue()});
            }
            return bodyFormArrayValues(arrayValues);
        }

        /**
         * Set a Json Body to this request.
         * The <tt>Content-Type</tt> header of the request is set to <tt>application/json</tt>.
         * @param node the Json Node
         */
        public RequestBuilder bodyJson(JsonNode node) {
            return body(new RequestBody(node), "application/json");
        }

        /**
         * Set a Json Body to this request.
         * The <tt>Content-Type</tt> header of the request is set to <tt>application/json</tt>.
         * @param json the JsValue
         */
        public RequestBuilder bodyJson(JsValue json) {
            return bodyJson(Json.parse(play.api.libs.json.Json.stringify(json)));
        }

        /**
         * Set a XML to this request.
         * The <tt>Content-Type</tt> header of the request is set to <tt>application/xml</tt>.
         * @param xml the XML
         */
        public RequestBuilder bodyXml(InputSource xml) {
            return bodyXml(XML.fromInputSource(xml));
        }

        /**
         * Set a XML to this request.
         * The <tt>Content-Type</tt> header of the request is set to <tt>application/xml</tt>.
         * @param xml the XML
         */
        public RequestBuilder bodyXml(Document xml) {
            return body(new RequestBody(xml), "application/xml");
        }

        /**
         * Set a Text to this request.
         * The <tt>Content-Type</tt> header of the request is set to <tt>text/plain</tt>.
         * @param text the text
         */
        public RequestBuilder bodyText(String text) {
            return body(new RequestBody(text), "text/plain");
        }

        /**
         * Builds the request.
         * @return a build of the given parameters
         */
        public RequestImpl build() {
            return new RequestImpl(new play.api.mvc.RequestImpl(
                body(),
                id,
                asScala(tags()),
                uri.toString(),
                uri.getRawPath(),
                method,
                version,
                mapListToScala(splitQuery()),
                buildHeaders(),
                remoteAddress,
                secure));
        }

        // -------------------
        // REQUEST HEADER CODE

        protected Long id = RequestIdProvider.requestIDs().incrementAndGet();
        protected Map<String, String> tags = new HashMap<>();
        protected String method;
        protected boolean secure;
        protected URI uri;
        protected String version;
        protected Map<String, String[]> headers = new HashMap<>();
        protected String remoteAddress;

        /**
         * @return the id of the request
         */
        public Long id() {
            return id;
        }

        /**
         * @param id the id to be used
         * @return the builder instance
         */
        public RequestBuilder id(Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return the tags for the request
         */
        public Map<String, String> tags() {
            return tags;
        }

        /**
         * @param tags overwrites the tags for this request
         * @return the builder instance
         */
        public RequestBuilder tags(Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        /**
         * Puts an extra tag.
         * @param key the key for the tag
         * @param value the value for the tag
         * @return the builder
         */
        public RequestBuilder tag(String key, String value) {
            tags.put(key, value);
            return this;
        }

        /**
         * @return the builder instance.
         */
        public String method() {
            return method;
        }

        /**
         * @param method sets the method
         * @return the builder instance
         */
        public RequestBuilder method(String method) {
            this.method = method;
            return this;
        }

        /**
         * @return gives the uri of the request
         */
        public String uri() {
            return uri.toString();
        }

        public RequestBuilder uri(URI uri) {
            if (uri.getScheme() != null) {
                if (!uri.getScheme().equals("http") && !uri.getScheme().equals("https")) {
                    throw new IllegalArgumentException("URI scheme must be http or https");
                }
                this.secure = uri.getScheme().equals("https");
            }
            this.uri = uri;
            host(uri.getHost());
            return this;
        }

        /**
         * Sets the uri.
         * @param str the uri
         * @return the builder instance
         */
        public RequestBuilder uri(String str) {
            try {
                uri(new URI(str));
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Exception parsing URI", e);
            }
            return this;
        }

        /**
         * @param secure true if the request is secure
         * @return the builder instance
         */
        public RequestBuilder secure(boolean secure) {
           this.secure = secure;
           return this;
        }

        /**
         * @return the status if the request is secure
         */
        public boolean secure() {
           return secure;
        }

        /**
         * @return the host name from the header
         */
        public String host() {
          return header(HeaderNames.HOST);
        }

        /**
         * @param host sets the host in the header
         * @return the builder instance
         */
        public RequestBuilder host(String host) {
          header(HeaderNames.HOST, host);
          return this;
        }

        /**
         * @return the raw path of the uri
         */
        public String path() {
            return uri.getRawPath();
        }

        /**
         * This method sets the path of the uri.
         * @param path the path after the port and for the query in a uri
         * @return the builder instance
         */
        public RequestBuilder path(String path) {
            try {
                uri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), path, uri.getQuery(), uri.getFragment());
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
            return this;
        }

        /**
         * @return the version
         */
        public String version() {
            return version;
        }

        /**
         * @param version the version
         * @return the builder instance
         */
        public RequestBuilder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * @param key the key to be used in the header
         * @return the value associated with the key, if multiple, the first, if none returns null
         */
        public String header(String key) {
            String[] values = headers.get(key);
            return values == null || values.length == 0 ? null : values[0];
        }

        /**
         * @param key the key to be used in the header
         * @return all values (could be 0) associated with the key
         */
        public String[] headers(String key) {
            return headers.get(key);
        }

        /**
         * @return the headers
         */
        public Map<String, String[]> headers() {
            return headers;
        }

        /**
         * @param headers the headers to be replaced
         * @return the builder instance
         */
        public RequestBuilder headers(Map<String, String[]> headers) {
            this.headers = headers;
            return this;
        }

        /**
         * @param key the key for in the header
         * @param values the values associated with the key
         * @return the builder instance
         */
        public RequestBuilder header(String key, String[] values) {
            headers.put(key, values);
            return this;
        }

        /**
         * @param key the key for in the header
         * @param value the value (one) associated with the key
         * @return the builder instance
         */
        public RequestBuilder header(String key, String value) {
            headers.put(key, new String[] { value });
            return this;
        }

        /**
         * @return the cookies in Scala instances
         */
        private play.api.mvc.Cookies scalaCookies() {
          String cookieHeader = header(HeaderNames.COOKIE);
          scala.Option<String> cookieHeaderOpt = scala.Option.apply(cookieHeader);
          return play.api.mvc.Cookies$.MODULE$.fromCookieHeader(cookieHeaderOpt);
        }

        /**
         * @return the cookies in Java instances
         */
        public Cookies cookies() {
          return play.core.j.JavaHelpers$.MODULE$.cookiesToJavaCookies(scalaCookies());
        }

        /**
         * Sets the cookies in the header.
         * @param cookies the cookies in a Scala sequence
         */
        private void cookies(Seq<play.api.mvc.Cookie> cookies) {
          String cookieHeader = header(HeaderNames.COOKIE);
          String value = play.api.mvc.Cookies$.MODULE$.mergeCookieHeader(cookieHeader != null ? cookieHeader : "", cookies);
          header(HeaderNames.COOKIE, value);
        }

        /**
         * Sets one cookie.
         * @param cookie the cookie to be set
         * @return the builder instance
         */
        public RequestBuilder cookie(Cookie cookie) {
          cookies(play.core.j.JavaHelpers$.MODULE$.cookiesToScalaCookies(Arrays.asList(cookie)));
          return this;
        }

        /**
         * @return the cookies in a Java map
         */
        public Map<String,String> flash() {
          play.api.mvc.Cookies scalaCookies = scalaCookies();
          scala.Option<play.api.mvc.Cookie> cookie = scalaCookies.get(play.api.mvc.Flash$.MODULE$.COOKIE_NAME());
          scala.collection.Map<String,String> data = play.api.mvc.Flash$.MODULE$.decodeCookieToMap(cookie);
          return JavaConversions.mapAsJavaMap(data);
        }

        /**
         * Sets a cookie in the request.
         * @param key the key for the cookie
         * @param value the value for the cookie
         * @return the builder instance
         */
        public RequestBuilder flash(String key, String value) {
          Map<String,String> data = new HashMap<>(flash());
          data.put(key, value);
          flash(data);
          return this;
        }

        /**
         * Sets cookies in a request.
         * @param data a key value mapping of cookies
         * @return the builder instance
         */
        public RequestBuilder flash(Map<String,String> data) {
          play.api.mvc.Flash flash = new play.api.mvc.Flash(asScala(data));
          cookies(JavaConversions.asScalaBuffer(Arrays.asList(play.api.mvc.Flash$.MODULE$.encodeAsCookie(flash))));
          return this;
        }

        /**
         * @return the sessions in the request
         */
        public Map<String,String> session() {
          play.api.mvc.Cookies scalaCookies = scalaCookies();
          scala.Option<play.api.mvc.Cookie> cookie = scalaCookies.get(play.api.mvc.Session$.MODULE$.COOKIE_NAME());
          scala.collection.Map<String,String> data = play.api.mvc.Session$.MODULE$.decodeCookieToMap(cookie);
          return JavaConversions.mapAsJavaMap(data);
        }

        /**
         * Sets a session.
         * @param key the key for the session
         * @param value the value associated with the key for the session
         * @return the builder instance
         */
        public RequestBuilder session(String key, String value) {
          Map<String,String> data = new HashMap<>(session());
          data.put(key, value);
          session(data);
          return this;
        }

        /**
         * Sets all parameters for the session.
         * @param data a key value mapping of the session data
         * @return the builder instance
         */
        public RequestBuilder session(Map<String,String> data) {
          play.api.mvc.Session session = new play.api.mvc.Session(asScala(data));
          cookies(JavaConversions.asScalaBuffer(Arrays.asList(play.api.mvc.Session$.MODULE$.encodeAsCookie(session))));
          return this;
        }

        /**
         * @return the remote address
         */
        public String remoteAddress() {
            return remoteAddress;
        }

        /**
         * @param remoteAddress sets the remote address
         * @return the builder instance
         */
        public RequestBuilder remoteAddress(String remoteAddress) {
            this.remoteAddress = remoteAddress;
            return this;
        }

        protected Map<String, List<String>> splitQuery() {
            try {
                Map<String, List<String>> query_pairs = new LinkedHashMap<String, List<String>>();
                String query = uri.getQuery();
                if (query == null) {
                    return new HashMap<>();
                }
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    int idx = pair.indexOf("=");
                    String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
                    if (!query_pairs.containsKey(key)) {
                        query_pairs.put(key, new LinkedList<String>());
                    }
                    String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
                    query_pairs.get(key).add(value);
                }
                return query_pairs;
            } catch(UnsupportedEncodingException e) {
                throw new IllegalStateException("This can never happen", e);
            }
        }

        protected static scala.collection.immutable.Map<String,Seq<String>> mapListToScala(Map<String,List<String>> data) {
            Map<String,Seq<String>> seqs = new HashMap<>();
            for (String key: data.keySet()) {
                seqs.put(key, JavaConversions.asScalaBuffer(data.get(key)));
            }
            return asScala(seqs);
        }

        protected Headers buildHeaders() {
            List<Tuple2<String, String>> list = new ArrayList<>();
            for (Map.Entry<String,String[]> entry : headers().entrySet()) {
                for (String value : entry.getValue()) {
                    list.add(new Tuple2<>(entry.getKey(), value));
                }
            }
            return new Headers(JavaConversions.asScalaBuffer(list));
        }

    }

    /**
     * Handle the request body a raw bytes data.
     */
    public abstract static class RawBuffer {

        /**
         * Buffer size.
         */
        public abstract Long size();

        /**
         * Returns the buffer content as a bytes array.
         *
         * @param maxLength The max length allowed to be stored in memory
         * @return null if the content is too big to fit in memory
         */
        public abstract ByteString asBytes(int maxLength);

        /**
         * Returns the buffer content as a bytes array
         */
        public abstract ByteString asBytes();

        /**
         * Returns the buffer content as File
         */
        public abstract File asFile();

    }

    /**
     * Multipart form data body.
     */
    public abstract static class MultipartFormData<A> {

        /**
         * Info about a file part
         */
        public static class FileInfo {
            private final String key;
            private final String filename;
            private final String contentType;

            public FileInfo(String key, String filename, String contentType) {
                this.key = key;
                this.filename = filename;
                this.contentType = contentType;
            }

            public String getKey() {
                return key;
            }

            public String getFilename() {
                return filename;
            }

            public String getContentType() {
                return contentType;
            }
        }

        /**
         * A file part.
         */
        public static class FilePart<A> {

            final String key;
            final String filename;
            final String contentType;
            final A file;

            public FilePart(String key, String filename, String contentType, A file) {
                this.key = key;
                this.filename = filename;
                this.contentType = contentType;
                this.file = file;
            }

            /**
             * The part name.
             */
            public String getKey() {
                return key;
            }

            /**
             * The file name.
             */
            public String getFilename() {
                return filename;
            }

            /**
             * The file Content-Type
             */
            public String getContentType() {
                return contentType;
            }

            /**
             * The File.
             */
            public A getFile() {
                return file;
            }

        }

        /**
         * Extract the data parts as Form url encoded.
         */
        public abstract Map<String, String[]> asFormUrlEncoded();

        /**
         * Retrieves all file parts.
         */
        public abstract List<FilePart<A>> getFiles();

        /**
         * Access a file part.
         */
        public FilePart<A> getFile(String key) {
            for(FilePart filePart: getFiles()) {
                if(filePart.getKey().equals(key)) {
                    return filePart;
                }
            }
            return null;
        }
    }

    /**
     * The request body.
     */
    public static final class RequestBody {

        private final Object body;

        public RequestBody(Object body) {
            this.body = body;
        }

        /**
         * The request content parsed as multipart form data.
         */
        public <A> MultipartFormData<A> asMultipartFormData() {
            return as(MultipartFormData.class);
        }

        /**
         * The request content parsed as URL form-encoded.
         */
        public Map<String,String[]> asFormUrlEncoded() {
            // Best effort, check if it's a map, then check if the first element in that map is String -> String[].
            if (body instanceof Map) {
                if (((Map) body).isEmpty()) {
                    return Collections.emptyMap();
                } else {
                    Map.Entry<Object, Object> first = ((Map<Object, Object>) body).entrySet().iterator().next();
                    if (first.getKey() instanceof String && first.getValue() instanceof String[]) {
                        return (Map<String, String[]>) body;
                    }
                }
            }
            return null;
        }

        /**
         * The request content as Array bytes.
         */
        public RawBuffer asRaw() {
            return as(RawBuffer.class);
        }

        /**
         * The request content as text.
         */
        public String asText() {
            return as(String.class);
        }

        /**
         * The request content as XML.
         */
        public Document asXml() {
            return as(Document.class);
        }

        /**
         * The request content as Json.
         */
        public JsonNode asJson() {
            return as(JsonNode.class);
        }

        /**
         * The request content as a ByteString.
         *
         * This makes a best effort attempt to convert the parsed body to a ByteString, if it knows how. This includes
         * String, json, XML and form bodies.  It doesn't include multipart/form-data or raw bodies that don't fit in
         * the configured max memory buffer, nor does it include custom output types from custom body parsers.
         */
        public ByteString asBytes() {
            if (body instanceof Optional) {
                if (!((Optional<?>) body).isPresent()) {
                    return ByteString.empty();
                }
            } else if (body instanceof ByteString) {
                return (ByteString) body;
            } else if (body instanceof byte[]) {
                return ByteString.fromArray((byte[]) body);
            } else if (body instanceof String) {
                return ByteString.fromString((String) body);
            } else if (body instanceof RawBuffer) {
                return ((RawBuffer) body).asBytes();
            } else if (body instanceof JsonNode) {
                return ByteString.fromString(Json.stringify((JsonNode) body));
            } else if (body instanceof Document) {
                return XML.toBytes((Document) body);
            } else {
                Map<String, String[]> form = asFormUrlEncoded();
                if (form != null) {
                    return ByteString.fromString(form.entrySet().stream()
                            .flatMap(entry -> {
                                String key = encode(entry.getKey());
                                return Arrays.asList(entry.getValue()).stream().map(
                                        value -> key + "=" + encode(value)
                                );
                            }).collect(Collectors.joining("&")));
                }
            }
            return null;
        }

        private String encode(String value) {
            try {
                return URLEncoder.encode(value, "utf8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }


        /**
         * Cast this RequestBody as T if possible.
         */
        public <T> T as(Class<T> tType) {
            if (tType.isInstance(body)) {
                return tType.cast(body);
            } else {
                return null;
            }
        }

    }

    /**
     * The HTTP response.
     */
    public static class Response implements HeaderNames {

        private final Map<String, String> headers = new TreeMap<>((Comparator<String>) String::compareToIgnoreCase);
        private final List<Cookie> cookies = new ArrayList<>();

        /**
         * Adds a new header to the response.
         *
         * @param name The name of the header, must not be null
         * @param value The value of the header, must not be null
         */
        public void setHeader(String name, String value) {
            this.headers.put(name, value);
        }

        /**
         * Gets the current response headers.
         */
        public Map<String,String> getHeaders() {
            return headers;
        }

        /**
         * @deprecated noop. Use {@link Result#as(String)} instead.
         */
        @Deprecated
        public void setContentType(String contentType) {
        }

        /**
         * Set a new transient cookie with path "/".<br>
         * For example:
         * <pre>
         * response().setCookie("theme", "blue");
         * </pre>
         * @param name Cookie name, must not be null
         * @param value Cookie value
         */
        public void setCookie(String name, String value) {
            setCookie(name, value, null);
        }

        /**
         * Set a new cookie with path "/".
         * @param name Cookie name, must not be null
         * @param value Cookie value
         * @param maxAge Cookie duration (null for a transient cookie and 0 or less for a cookie that expires now)
         */
        public void setCookie(String name, String value, Integer maxAge) {
            setCookie(name, value, maxAge, "/");
        }

        /**
         * Set a new cookie.
         * @param name Cookie name, must not be null
         * @param value Cookie value
         * @param maxAge Cookie duration (null for a transient cookie and 0 or less for a cookie that expires now)
         * @param path Cookie path
         */
        public void setCookie(String name, String value, Integer maxAge, String path) {
            setCookie(name, value, maxAge, path, null);
        }

        /**
         * Set a new cookie.
         * @param name Cookie name, must not be null
         * @param value Cookie value
         * @param maxAge Cookie duration (null for a transient cookie and 0 or less for a cookie that expires now)
         * @param path Cookie path
         * @param domain Cookie domain
         */
        public void setCookie(String name, String value, Integer maxAge, String path, String domain) {
            setCookie(name, value, maxAge, path, domain, false, false);
        }

        /**
         * Set a new cookie.
         * @param name Cookie name, must not be null
         * @param value Cookie value
         * @param maxAge Cookie duration (null for a transient cookie and 0 or less for a cookie that expires now)
         * @param path Cookie path
         * @param domain Cookie domain
         * @param secure Whether the cookie is secured (for HTTPS requests)
         * @param httpOnly Whether the cookie is HTTP only (i.e. not accessible from client-side JavaScript code)
         */
        public void setCookie(String name, String value, Integer maxAge, String path, String domain, boolean secure, boolean httpOnly) {
            cookies.add(new Cookie(name, value, maxAge, path, domain, secure, httpOnly));
        }

        /**
         * Discard a cookie on the default path ("/") with no domain and that's not secure.
         *
         * @param name The name of the cookie to discard, must not be null
         */
        public void discardCookie(String name) {
            discardCookie(name, "/", null, false);
        }

        /**
         * Discard a cookie on the given path with no domain and not that's secure.
         *
         * @param name The name of the cookie to discard, must not be null
         * @param path The path of the cookie te discard, may be null
         */
        public void discardCookie(String name, String path) {
            discardCookie(name, path, null, false);
        }

        /**
         * Discard a cookie on the given path and domain that's not secure.
         *
         * @param name The name of the cookie to discard, must not be null
         * @param path The path of the cookie te discard, may be null
         * @param domain The domain of the cookie to discard, may be null
         */
        public void discardCookie(String name, String path, String domain) {
            discardCookie(name, path, domain, false);
        }

        /**
         * Discard a cookie in this result
         *
         * @param name The name of the cookie to discard, must not be null
         * @param path The path of the cookie te discard, may be null
         * @param domain The domain of the cookie to discard, may be null
         * @param secure Whether the cookie to discard is secure
         */
        public void discardCookie(String name, String path, String domain, boolean secure) {
            cookies.add(new Cookie(name, "", -86400, path, domain, secure, false));
        }

        public Collection<Cookie> cookies() {
            return cookies;
        }

        public Optional<Cookie> cookie(String name) {
            return cookies.stream().filter(x -> { return x.name().equals(name); }).findFirst();
        }

    }

    /**
     * HTTP Session.
     * <p>
     * Session data are encoded into an HTTP cookie, and can only contain simple <code>String</code> values.
     */
    public static class Session extends HashMap<String,String>{

        public boolean isDirty = false;

        public Session(Map<String,String> data) {
            super(data);
        }

        /**
         * Removes the specified value from the session.
         */
        @Override
        public String remove(Object key) {
            isDirty = true;
            return super.remove(key);
        }

        /**
         * Adds the given value to the session.
         */
        @Override
        public String put(String key, String value) {
            isDirty = true;
            return super.put(key, value);
        }

        /**
         * Adds the given values to the session.
         */
        @Override
        public void putAll(Map<? extends String,? extends String> values) {
            isDirty = true;
            super.putAll(values);
        }

        /**
         * Clears the session.
         */
        @Override
        public void clear() {
            isDirty = true;
            super.clear();
        }

    }

    /**
     * HTTP Flash.
     * <p>
     * Flash data are encoded into an HTTP cookie, and can only contain simple String values.
     */
    public static class Flash extends HashMap<String,String>{

        public boolean isDirty = false;

        public Flash(Map<String,String> data) {
            super(data);
        }

        /**
         * Removes the specified value from the flash scope.
         */
        @Override
        public String remove(Object key) {
            isDirty = true;
            return super.remove(key);
        }

        /**
         * Adds the given value to the flash scope.
         */
        @Override
        public String put(String key, String value) {
            isDirty = true;
            return super.put(key, value);
        }

        /**
         * Adds the given values to the flash scope.
         */
        @Override
        public void putAll(Map<? extends String,? extends String> values) {
            isDirty = true;
            super.putAll(values);
        }

        /**
         * Clears the flash scope.
         */
        @Override
        public void clear() {
            isDirty = true;
            super.clear();
        }

    }

    /**
     * HTTP Cookie
     */
    public static class Cookie {
        private final String name;
        private final String value;
        private final Integer maxAge;
        private final String path;
        private final String domain;
        private final boolean secure;
        private final boolean httpOnly;

        public Cookie(String name, String value, Integer maxAge, String path,
                      String domain, boolean secure, boolean httpOnly) {
            this.name = name;
            this.value = value;
            this.maxAge = maxAge;
            this.path = path;
            this.domain = domain;
            this.secure = secure;
            this.httpOnly = httpOnly;
        }

        /**
         * @return the cookie name
         */
        public String name() {
            return name;
        }

        /**
         * @return the cookie value
         */
        public String value() {
            return value;
        }

        /**
         * @return the cookie expiration date in seconds, null for a transient cookie, a value less than zero for a
         * cookie that expires now
         */
        public Integer maxAge() {
            return maxAge;
        }

        /**
         * @return the cookie path
         */
        public String path() {
            return path;
        }

        /**
         * @return the cookie domain, or null if not defined
         */
        public String domain() {
            return domain;
        }

        /**
         * @return wether the cookie is secured, sent only for HTTPS requests
         */
        public boolean secure() {
            return secure;
        }

        /**
         * @return wether the cookie is HTTP only, i.e. not accessible from client-side JavaScript code
         */
        public boolean httpOnly() {
            return httpOnly;
        }

    }

    /**
     * HTTP Cookies set
     */
    public interface Cookies extends Iterable<Cookie> {

        /**
         * @param name Name of the cookie to retrieve
         * @return the cookie that is associated with the given name
         */
        Cookie get(String name);

    }


    /**
     * Defines all standard HTTP headers.
     */
    public static interface HeaderNames {

        String ACCEPT = "Accept";
        String ACCEPT_CHARSET = "Accept-Charset";
        String ACCEPT_ENCODING = "Accept-Encoding";
        String ACCEPT_LANGUAGE = "Accept-Language";
        String ACCEPT_RANGES = "Accept-Ranges";
        String AGE = "Age";
        String ALLOW = "Allow";
        String AUTHORIZATION = "Authorization";
        String CACHE_CONTROL = "Cache-Control";
        String CONNECTION = "Connection";
        String CONTENT_DISPOSITION = "Content-Disposition";
        String CONTENT_ENCODING = "Content-Encoding";
        String CONTENT_LANGUAGE = "Content-Language";
        String CONTENT_LENGTH = "Content-Length";
        String CONTENT_LOCATION = "Content-Location";
        String CONTENT_MD5 = "Content-MD5";
        String CONTENT_RANGE = "Content-Range";
        String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
        String CONTENT_TYPE = "Content-Type";
        String COOKIE = "Cookie";
        String DATE = "Date";
        String ETAG = "ETag";
        String EXPECT = "Expect";
        String EXPIRES = "Expires";
        String FORWARDED = "Forwarded";
        String FROM = "From";
        String HOST = "Host";
        String IF_MATCH = "If-Match";
        String IF_MODIFIED_SINCE = "If-Modified-Since";
        String IF_NONE_MATCH = "If-None-Match";
        String IF_RANGE = "If-Range";
        String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
        String LAST_MODIFIED = "Last-Modified";
        String LOCATION = "Location";
        String MAX_FORWARDS = "Max-Forwards";
        String PRAGMA = "Pragma";
        String PROXY_AUTHENTICATE = "Proxy-Authenticate";
        String PROXY_AUTHORIZATION = "Proxy-Authorization";
        String RANGE = "Range";
        String REFERER = "Referer";
        String RETRY_AFTER = "Retry-After";
        String SERVER = "Server";
        String SET_COOKIE = "Set-Cookie";
        String SET_COOKIE2 = "Set-Cookie2";
        String TE = "Te";
        String TRAILER = "Trailer";
        String TRANSFER_ENCODING = "Transfer-Encoding";
        String UPGRADE = "Upgrade";
        String USER_AGENT = "User-Agent";
        String VARY = "Vary";
        String VIA = "Via";
        String WARNING = "Warning";
        String WWW_AUTHENTICATE = "WWW-Authenticate";
        String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
        String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
        String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
        String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
        String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
        String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
        String ORIGIN = "Origin";
        String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
        String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";
        String X_FORWARDED_FOR = "X-Forwarded-For";
        String X_FORWARDED_HOST = "X-Forwarded-Host";
        String X_FORWARDED_PORT = "X-Forwarded-Port";
        String X_FORWARDED_PROTO = "X-Forwarded-Proto";
        String X_REQUESTED_WITH = "X-Requested-With";
    }

    /**
     * Defines all standard HTTP status codes.
     */
    public static interface Status {

        int CONTINUE = 100;
        int SWITCHING_PROTOCOLS = 101;
        int OK = 200;
        int CREATED = 201;
        int ACCEPTED = 202;
        int NON_AUTHORITATIVE_INFORMATION = 203;
        int NO_CONTENT = 204;
        int RESET_CONTENT = 205;
        int PARTIAL_CONTENT = 206;
        int MULTIPLE_CHOICES = 300;
        int MOVED_PERMANENTLY = 301;
        int FOUND = 302;
        int SEE_OTHER = 303;
        int NOT_MODIFIED = 304;
        int USE_PROXY = 305;
        int TEMPORARY_REDIRECT = 307;
        int BAD_REQUEST = 400;
        int UNAUTHORIZED = 401;
        int PAYMENT_REQUIRED = 402;
        int FORBIDDEN = 403;
        int NOT_FOUND = 404;
        int METHOD_NOT_ALLOWED = 405;
        int NOT_ACCEPTABLE = 406;
        int PROXY_AUTHENTICATION_REQUIRED = 407;
        int REQUEST_TIMEOUT = 408;
        int CONFLICT = 409;
        int GONE = 410;
        int LENGTH_REQUIRED = 411;
        int PRECONDITION_FAILED = 412;
        int REQUEST_ENTITY_TOO_LARGE = 413;
        int REQUEST_URI_TOO_LONG = 414;
        int UNSUPPORTED_MEDIA_TYPE = 415;
        int REQUESTED_RANGE_NOT_SATISFIABLE = 416;
        int EXPECTATION_FAILED = 417;
        int UNPROCESSABLE_ENTITY = 422;
        int LOCKED = 423;
        int FAILED_DEPENDENCY = 424;
        int TOO_MANY_REQUESTS = 429;
        int INTERNAL_SERVER_ERROR = 500;
        int NOT_IMPLEMENTED = 501;
        int BAD_GATEWAY = 502;
        int SERVICE_UNAVAILABLE = 503;
        int GATEWAY_TIMEOUT = 504;
        int HTTP_VERSION_NOT_SUPPORTED = 505;
    }

    /** Common HTTP MIME types */
    public static interface MimeTypes {

        /**
         * Content-Type of text.
         */
        String TEXT = "text/plain";

        /**
         * Content-Type of html.
         */
        String HTML = "text/html";

        /**
         * Content-Type of json.
         */
        String JSON = "application/json";

        /**
         * Content-Type of xml.
         */
        String XML = "application/xml";

        /**
         * Content-Type of css.
         */
        String CSS = "text/css";

        /**
         * Content-Type of javascript.
         */
        String JAVASCRIPT = "text/javascript";

        /**
         * Content-Type of form-urlencoded.
         */
        String FORM = "application/x-www-form-urlencoded";

        /**
         * Content-Type of server sent events.
         */
        String EVENT_STREAM = "text/event-stream";

        /**
         * Content-Type of binary data.
         */
        String BINARY = "application/octet-stream";
    }
}
