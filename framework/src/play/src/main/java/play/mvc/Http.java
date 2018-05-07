/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.fasterxml.jackson.databind.JsonNode;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import play.api.http.HttpConfiguration;
import play.api.i18n.Messages$;
import play.api.libs.json.JsValue;
import play.api.mvc.Headers$;
import play.api.mvc.request.*;
import play.core.j.JavaContextComponents;
import play.core.j.JavaHelpers$;
import play.core.j.JavaParsers;
import play.i18n.Lang;
import play.i18n.Langs;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.libs.Files;
import play.libs.Json;
import play.libs.Scala;
import play.libs.XML;
import play.libs.typedmap.TypedKey;
import play.libs.typedmap.TypedMap;
import play.mvc.Http.Cookie.SameSite;
import scala.collection.immutable.Map$;
import scala.compat.java8.OptionConverters;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Defines HTTP standard objects.
 */
public class Http {

    /**
     * The global HTTP context.
     */
    public static class Context {

        public static ThreadLocal<Context> current = new ThreadLocal<>();

        /**
         * Retrieves the current HTTP context, for the current thread.
         *
         * @return the context
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
        private final JavaContextComponents components;

        private Lang lang = null;

        /**
         * Creates a new HTTP context.
         *
         * @param requestBuilder the HTTP request builder.
         * @param components the context components.
         */
        public Context(RequestBuilder requestBuilder, JavaContextComponents components) {
            this(requestBuilder.build(), components);
        }

        /**
         * Creates a new HTTP context.
         *
         * @param request the HTTP request
         * @param components the context components.
         */
        public Context(Request request, JavaContextComponents components) {
            this.request = request;
            this.header = request.asScala();
            this.id = header.id();
            this.response = new Response();
            this.session = new Session(Scala.asJava(header.session().data()));
            this.flash = new Flash(Scala.asJava(header.flash().data()));
            this.args = new HashMap<>();
            this.components = components;
        }

        /**
         * Creates a new HTTP context.
         *
         * @param id the unique context ID
         * @param header the request header
         * @param request the request with body
         * @param sessionData the session data extracted from the session cookie
         * @param flashData the flash data extracted from the flash cookie
         * @param args any arbitrary data to associate with this request context.
         * @param components the context components.
         */
        public Context(Long id, play.api.mvc.RequestHeader header, Request request,
                Map<String,String> sessionData, Map<String,String> flashData, Map<String,Object> args,
                JavaContextComponents components) {
            this(id, header, request, new Response(), new Session(sessionData), new Flash(flashData),
                new HashMap<>(args), components);
        }

        /**
         * Creates a new HTTP context, using the references provided.
         *
         * Use this constructor (or withRequest) to copy a context within a Java Action to be passed to a delegate.
         *
         * @param id the unique context ID
         * @param header the request header
         * @param request the request with body
         * @param response the response instance to use
         * @param session the session instance to use
         * @param flash the flash instance to use
         * @param args any arbitrary data to associate with this request context.
         * @param components the context components.
         */
        public Context(Long id, play.api.mvc.RequestHeader header, Request request, Response response,
                Session session, Flash flash, Map<String,Object> args, JavaContextComponents components) {
            this.id = id;
            this.header = header;
            this.request = request;
            this.response = response;
            this.session = session;
            this.flash = flash;
            this.args = args;
            this.components = components;
        }

        /**
         * The context id (unique)
         *
         * @return the id
         */
        public Long id() {
            return id;
        }

        /**
         * Returns the current request.
         *
         * @return the request
         */
        public Request request() {
            return request;
        }

        /**
         * Returns the current response.
         *
         * @return the response
         */
        public Response response() {
            return response;
        }

        /**
         * Returns the current session.
         *
         * @return the session
         */
        public Session session() {
            return session;
        }

        /**
         * Returns the current flash scope.
         *
         * @return the flash scope
         */
        public Flash flash() {
            return flash;
        }

        /**
         * The original Play request Header used to create this context.
         * For internal usage only.
         *
         * @return the original request header.
         */
        public play.api.mvc.RequestHeader _requestHeader() {
            return header;
        }

        /**
         * The current lang
         *
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
            Request request = lang != null ? request().addAttr(Messages.Attrs.CurrentLang, lang) : request();
            return messagesApi().preferred(request);
        }

        /**
         * Change durably the lang for the current user.
         *
         * @param code New lang code to use (e.g. "fr", "en-US", etc.)
         * @return true if the requested lang was supported by the application, otherwise false
         */
        public boolean changeLang(String code) {
            return changeLang(Lang.forCode(code));
        }

        /**
         * Change durably the lang for the current user.
         *
         * @param lang New Lang object to use
         * @return true if the requested lang was supported by the application, otherwise false
         */
        public boolean changeLang(Lang lang) {
            if (langs().availables().contains(lang)) {
                this.lang = lang;
                scala.Option<String> domain = sessionDomain();
                Cookie langCookie = new Cookie(messagesApi().langCookieName(),
                        lang.code(),
                        null,
                        sessionPath(),
                        domain.isDefined() ? domain.get() : null,
                        messagesApi().langCookieSecure(),
                        messagesApi().langCookieHttpOnly(),
                        SameSite.LAX
                    );
                response.setCookie(langCookie);
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
            scala.Option<String> domain = sessionDomain();
            response.discardCookie(messagesApi().langCookieName(), sessionPath(),
                domain.isDefined() ? domain.get() : null, messagesApi().langCookieSecure());
        }

        private Langs langs() {
            return components.langs();
        }

        private MessagesApi messagesApi() {
            return components.messagesApi();
        }

        private scala.Option<String> sessionDomain() {
            return components.httpConfiguration().session().domain();
        }

        private String sessionPath() {
            return components.httpConfiguration().context();
        }

        /**
         * Set the language for the current request, but don't
         * change the language cookie. This means the language
         * will be set for this request, but will not change for
         * future requests.
         *
         * @param code the language code to set (e.g. "en-US")
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
         * @param lang the language to set
         * @throws IllegalArgumentException If the given language
         * is not supported by the application.
         */
        public void setTransientLang(Lang lang) {
            final Langs langs = components.langs();
            if (langs.availables().contains(lang)) {
                this.lang = lang;
            } else {
                throw new IllegalArgumentException("Language not supported in this application: " + lang + " not in " + langs.availables());
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

        public FileMimeTypes fileMimeTypes() {
            return components.fileMimeTypes();
        }

        /**
         * Import in templates to get implicit HTTP context.
         */
        public static class Implicit {

            /**
             * Returns the current response.
             *
             * @return the current response.
             */
            public static Response response() {
                return Context.current().response();
            }

            /**
             * Returns the current request.
             *
             * @return the current request.
             */
            public static Request request() {
                return Context.current().request();
            }

            /**
             * Returns the current flash scope.
             *
             * @return the current flash scope.
             */
            public static Flash flash() {
                return Context.current().flash();
            }

            /**
             * Returns the current session.
             *
             * @return the current session.
             */
            public static Session session() {
                return Context.current().session();
            }

            /**
             * Returns the current lang.
             *
             * @return the current lang.
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
             *
             * @return the current context.
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
         * This method is intended for use within a Java action, to create a new Context to pass to a delegate action.
         *
         * @param request The request to create the new header from.
         * @return The new context.
         */
        public Context withRequest(Request request) {
            return new Context(id, header, request, response, session, flash, args, components);
        }
    }

    /**
     * A wrapped context.
     * Use this to modify the context in some way.
     */
    public static abstract class WrappedContext extends Context {
        private final Context wrapped;

        /**
         * @param wrapped the context the created instance will wrap
         */
        public WrappedContext(Context wrapped) {
            super(wrapped.id(), wrapped._requestHeader(), wrapped.request(), wrapped.session(), wrapped.flash(), wrapped.args, wrapped.components);
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

    public static class Headers {

        private final Map<String,  List<String>> headers;

        public Headers(Map<String, List<String>> headers) {
            this.headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            this.headers.putAll(headers);
        }

        /**
         * @return all the headers as a map.
         */
        public Map<String, List<String>> toMap() {
            return headers;
        }

        /**
         * Checks if the given header is present.
         *
         * @param headerName The name of the header (case-insensitive)
         * @return <code>true</code> if the request did contain the header.
         */
        public boolean contains(String headerName) {
            return headers.containsKey(headerName);
        }

        /**
         * Gets the header value. If more than one value is associated with this header, then returns the first one.
         *
         * @param name the header name
         * @return the first header value or empty if no value available.
         */
        public Optional<String> get(String name) {
            return Optional.ofNullable(headers.get(name)).flatMap(headerValues -> headerValues.stream().findFirst());
        }

        /**
         * Get all the values associated with the header name.
         *
         * @param name the header name.
         * @return the list of values associates with the header of empty.
         */
        public List<String> getAll(String name) {
            return headers.getOrDefault(name, Collections.emptyList());
        }

        /**
         * @return the scala version of this headers.
         */
        public play.api.mvc.Headers asScala() {
            return new play.api.mvc.Headers(JavaHelpers$.MODULE$.javaMapOfListToScalaSeqOfPairs(this.headers));
        }

        /**
         * Add a new header with the given value.
         *
         * @param name the header name
         * @param value the header value
         * @return this with the new header added
         */
        public Headers addHeader(String name, String value) {
            this.headers.put(name, Collections.singletonList(value));
            return this;
        }

        /**
         * Add a new header with the given values.
         *
         * @param name the header name
         * @param values the header values
         * @return this with the new header added
         */
        public Headers addHeader(String name, List<String> values) {
            this.headers.put(name, values);
            return this;
        }

        /**
         * Remove a header.
         *
         * @param name the header name.
         * @return this without the removed header.
         */
        public Headers remove(String name) {
            this.headers.remove(name);
            return this;
        }
    }

    public interface RequestHeader {

        /**
         * The complete request URI, containing both path and query string.
         *
         * @return the uri
         */
        String uri();

        /**
         * The HTTP Method.
         *
         * @return the http method
         */
        String method();

        /**
         * The HTTP version.
         *
         * @return the version
         */
        String version();

        /**
         * The client IP address.
         *
         * retrieves the last untrusted proxy
         * from the Forwarded-Headers or the X-Forwarded-*-Headers.
         *
         * @return the remote address
         */
        String remoteAddress();

        /**
         * Is the client using SSL?
         *
         * @return true that the client is using SSL
         */
        boolean secure();

        /**
         * @return a map of typed attributes associated with the request.
         */
        TypedMap attrs();

        /**
         * Create a new version of this object with the given attributes attached to it.
         *
         * @param newAttrs The new attributes to add.
         * @return The new version of this object with the attributes attached.
         */
        RequestHeader withAttrs(TypedMap newAttrs);

        /**
         * Create a new versions of this object with the given attribute attached to it.
         *
         * @param key The new attribute key.
         * @param value  The attribute value.
         * @param <A> the attribute type
         * @return The new version of this object with the new attribute.
         */
        <A> RequestHeader addAttr(TypedKey<A> key, A value);

        /**
         * Attach a body to this header.
         *
         * @param body The body to attach.
         * @return A new request with the body attached to the header.
         */
        Request withBody(RequestBody body);

        /**
         * The request host.
         *
         * @return the host
         */
        String host();

        /**
         * The URI path.
         *
         * @return the path
         */
        String path();

        /**
         * The Request Langs extracted from the Accept-Language header and sorted by preference (preferred first).
         *
         * @return the preference-ordered list of languages accepted by the client
         */
        List<play.i18n.Lang> acceptLanguages();

        /**
         * @return The media types set in the request Accept header, sorted by preference (preferred first)
         */
        List<play.api.http.MediaRange> acceptedTypes();

        /**
         * Check if this request accepts a given media type.
         *
         * @param mimeType the mimeType to check for support.
         * @return true if <code>mimeType</code> is in the Accept header, otherwise false
         */
        boolean accepts(String mimeType);

        /**
         * The query string content.
         *
         * @return the query string map
         */
        Map<String,String[]> queryString();

        /**
         * Helper method to access a queryString parameter.
         *
         * @param key the query string parameter to look up
         * @return the value for the provided <code>key</code>.
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
         * Retrieve all headers.
         *
         * @return the request headers for this request.
         */
        Headers getHeaders();

        /**
         * Retrieves a single header.
         *
         * @param headerName The name of the header (case-insensitive)
         * @return the value corresponding to <code>headerName</code>, or empty if it was not present
         */
        default Optional<String> header(String headerName) {
            return getHeaders().get(headerName);
        }

        /**
         * Checks if the request has the header.
         *
         * @param headerName The name of the header (case-insensitive)
         * @return <code>true</code> if the request did contain the header.
         */
        default boolean hasHeader(String headerName) {
            return getHeaders().contains(headerName);
        }

        /**
         * Checks if the request has a body.
         *
         * @return true if request has a body, false otherwise.
         */
        boolean hasBody();

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
         * The X509 certificate chain presented by a client during SSL requests.
         *
         * @return The chain of X509Certificates used for the request if the request is secure and the server supports it.
         */
        Optional<List<X509Certificate>> clientCertificateChain();

        /**
         * Return the Scala version of the request header.
         *
         * @return the Scala version for this request header.
         * @see play.api.mvc.RequestHeader
         */
        play.api.mvc.RequestHeader asScala();
    }

    /**
     * An HTTP request.
     */
    public interface Request extends RequestHeader {

        /**
         * The request body.
         *
         * @return the body
         */
        RequestBody body();

        Request withBody(RequestBody body);

        // Override return type
        Request withAttrs(TypedMap newAttrs);

        // Override return type
        <A> Request addAttr(TypedKey<A> key, A value);

        /**
         * Return the Scala version of the request
         *
         * @return the underlying request.
         * @see play.api.mvc.Request
         */
        play.api.mvc.Request<RequestBody> asScala();
    }

    /**
     * An HTTP request.
     */
    public static class RequestImpl extends play.core.j.RequestImpl {

        /**
         * Constructor only based on a header.
         * @param header the header from a request
         *
         * @deprecated Since 2.7.0. Use {@link #RequestImpl(play.api.mvc.Request)} instead.
         */
        @Deprecated
        public RequestImpl(play.api.mvc.RequestHeader header) {
            super(header.withBody(null));
        }

        /**
         * Constructor with a requestbody.
         * @param request the body of the request
         */
        public RequestImpl(play.api.mvc.Request<RequestBody> request) {
            super(request);
        }
    }

    /**
     * The builder for building a request.
     */
    public static class RequestBuilder {

        protected play.api.mvc.Request<RequestBody> req;

        /**
         * Returns a simple request builder. The initial request is "GET / HTTP/1.1" from
         * 127.0.0.1 over an insecure connection. The request is created using the default
         * factory.
         */
        public RequestBuilder() {
            this(new DefaultRequestFactory(HttpConfiguration.createWithDefaults()));
            // Add a host of "localhost" to validate against the AllowedHostsFilter.
            this.host("localhost");
        }

        /**
         * Returns a simple request builder. The initial request is "GET / HTTP/1.1" from
         * 127.0.0.1 over an insecure connection. The request is created using the given
         * factory.
         * @param requestFactory the incoming request factory
         */
        public RequestBuilder(RequestFactory requestFactory) {
            req = requestFactory.createRequest(
                    RemoteConnection$.MODULE$.apply("127.0.0.1", false, OptionConverters.toScala(Optional.empty())),
                    "GET",
                    RequestTarget$.MODULE$.apply("/", "/", Map$.MODULE$.empty()),
                    "HTTP/1.1",
                    Headers$.MODULE$.create(),
                    TypedMap.empty().underlying(),
                    new RequestBody(null)
            );
        }

        /**
         * @return the request body, if a previously the body has been set
         */
        public RequestBody body() {
            return req.body();
        }

        /**
         * Set the body of the request.
         *
         * @param body the body
         * @param contentType Content-Type header value
         * @return the modified builder
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
         * @return the modified builder
         */
        protected RequestBuilder body(RequestBody body) {
            if (body == null || body.as(Object.class) == null) {
                // assume null signifies no body; RequestBody is a wrapper for the actual body content
                headers(getHeaders().remove(HeaderNames.CONTENT_LENGTH).remove(HeaderNames.TRANSFER_ENCODING));
            } else {
                if (!getHeaders().get(HeaderNames.TRANSFER_ENCODING).isPresent()) {
                    int length = body.asBytes().length();
                    header(HeaderNames.CONTENT_LENGTH, Integer.toString(length));
                }
            }
            req = req.withBody(body);
            return this;
        }

        /**
         * Set a Binary Data to this request using a singleton temp file creator
         * The <tt>Content-Type</tt> header of the request is set to <tt>application/octet-stream</tt>.
         *
         * @param data the Binary Data
         * @return the modified builder
         */
        public RequestBuilder bodyRaw(ByteString data) {
            final Files.TemporaryFileCreator tempFileCreator = Files.singletonTemporaryFileCreator();
            play.api.mvc.RawBuffer buffer = new play.api.mvc.RawBuffer(data.size(), tempFileCreator.asScala(), data);
            return body(new RequestBody(JavaParsers.toJavaRaw(buffer)), "application/octet-stream");
        }

        /**
         * Set a Binary Data to this request.
         * The <tt>Content-Type</tt> header of the request is set to <tt>application/octet-stream</tt>.
         *
         * @param data the Binary Data
         * @param tempFileCreator the temporary file creator for binary data.
         * @return the modified builder
         */
        public RequestBuilder bodyRaw(ByteString data, Files.TemporaryFileCreator tempFileCreator) {
            play.api.mvc.RawBuffer buffer = new play.api.mvc.RawBuffer(data.size(), tempFileCreator.asScala(), data);
            return body(new RequestBody(JavaParsers.toJavaRaw(buffer)), "application/octet-stream");
        }

        /**
         * Set a Binary Data to this request using a singleton temporary file creator.
         * The <tt>Content-Type</tt> header of the request is set to <tt>application/octet-stream</tt>.
         *
         * @param data the Binary Data
         * @return the modified builder
         */
        public RequestBuilder bodyRaw(byte[] data) {
            Files.TemporaryFileCreator tempFileCreator = Files.singletonTemporaryFileCreator();
            return bodyRaw(ByteString.fromArray(data), tempFileCreator);
        }

        /**
         * Set a Binary Data to this request.
         * The <tt>Content-Type</tt> header of the request is set to <tt>application/octet-stream</tt>.
         *
         * @param data the Binary Data
         * @param tempFileCreator the temporary file creator for binary data.
         * @return the modified builder
         */
        public RequestBuilder bodyRaw(byte[] data, Files.TemporaryFileCreator tempFileCreator) {
            return bodyRaw(ByteString.fromArray(data), tempFileCreator);
        }

        /**
         * Set a Form url encoded body to this request.
         *
         * @param data the x-www-form-urlencoded parameters
         * @return the modified builder
         */
        public RequestBuilder bodyFormArrayValues(Map<String, String[]> data) {
            return body(new RequestBody(data), "application/x-www-form-urlencoded");
        }

        /**
         * Set a Form url encoded body to this request.
         *
         * @param data the x-www-form-urlencoded parameters
         * @return the modified builder
         */
        public RequestBuilder bodyForm(Map<String, String> data) {
            Map<String, String[]> arrayValues = new HashMap<>();
            for (Entry<String, String> entry: data.entrySet()) {
                arrayValues.put(entry.getKey(), new String[]{entry.getValue()});
            }
            return bodyFormArrayValues(arrayValues);
        }

        /**
         * Set a Multipart Form url encoded body to this request.
         *
         * @param data the multipart-form parameters
         * @param temporaryFileCreator the temporary file creator.
         * @param mat a Akka Streams Materializer
         * @return the modified builder
         */
        public RequestBuilder bodyMultipart(List<MultipartFormData.Part<Source<ByteString, ?>>> data, Files.TemporaryFileCreator temporaryFileCreator, Materializer mat) {
            String boundary = MultipartFormatter.randomBoundary();
            try {
                ByteString materializedData = MultipartFormatter
                        .transform(Source.from(data), boundary)
                        .runWith(Sink.reduce(ByteString::concat), mat)
                        .toCompletableFuture()
                        .get();

                play.api.mvc.RawBuffer buffer = new play.api.mvc.RawBuffer(materializedData.size(), temporaryFileCreator.asScala(), materializedData);
                return body(new RequestBody(JavaParsers.toJavaRaw(buffer)), MultipartFormatter.boundaryToContentType(boundary));
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Failure while materializing Multipart/Form Data", e);
            }
        }

        /**
         * Set a Json Body to this request.
         * The <tt>Content-Type</tt> header of the request is set to <tt>application/json</tt>.
         *
         * @param node the Json Node
         * @return this builder, updated
         */
        public RequestBuilder bodyJson(JsonNode node) {
            return body(new RequestBody(node), "application/json");
        }

        /**
         * Set a Json Body to this request.
         * The <tt>Content-Type</tt> header of the request is set to <tt>application/json</tt>.
         *
         * @param json the JsValue
         * @return the modified builder
         */
        public RequestBuilder bodyJson(JsValue json) {
            return bodyJson(Json.parse(play.api.libs.json.Json.stringify(json)));
        }

        /**
         * Set a XML to this request.
         * The <tt>Content-Type</tt> header of the request is set to <tt>application/xml</tt>.
         *
         * @param xml the XML
         * @return the modified builder
         */
        public RequestBuilder bodyXml(InputSource xml) {
            return bodyXml(XML.fromInputSource(xml));
        }

        /**
         * Set a XML to this request.
         *
         * The <tt>Content-Type</tt> header of the request is set to <tt>application/xml</tt>.
         *
         * @param xml the XML
         * @return the modified builder
         */
        public RequestBuilder bodyXml(Document xml) {
            return body(new RequestBody(xml), "application/xml");
        }

        /**
         * Set a Text to this request.
         * The <tt>Content-Type</tt> header of the request is set to <tt>text/plain</tt>.
         *
         * @param text the text
         * @return this builder, updated
         */
        public RequestBuilder bodyText(String text) {
            return body(new RequestBody(text), "text/plain");
        }

        /**
         * Builds the request.
         *
         * @return a build of the given parameters
         */
        public RequestImpl build() {
            return new RequestImpl(req);
        }

        // -------------------
        // REQUEST HEADER CODE

        /**
         * @return the id of the request
         */
        public Long id() {
            return req.id();
        }

        /**
         * @param id the id to be used
         * @return the builder instance
         */
        public RequestBuilder id(Long id) {
            attr(new TypedKey(RequestAttrKey.Id()), id);
            return this;
        }

        /**
         * Add an attribute to the request.
         *
         * @param key The key of the attribute to add.
         * @param value The value of the attribute to add.
         * @param <T> The type of the attribute to add.
         * @return the request builder with extra attribute
         */
        public <T> RequestBuilder attr(TypedKey<T> key, T value) {
            req = req.addAttr(key.underlying(), value);
            return this;
        }

        /**
         * Update the request attributes. This replaces all existing attributes.
         *
         * @param newAttrs The attribute entries to add.
         * @return the request builder with extra attributes set.
         */
        public RequestBuilder attrs(TypedMap newAttrs) {
            req = req.withAttrs(newAttrs.underlying());
            return this;
        }

        /**
         * Get the request attributes.
         * @return the request builder's request attributes.
         */
        public TypedMap attrs() {
            return new TypedMap(req.attrs());
        }

        /**
         * @return the builder instance.
         */
        public String method() {
            return req.method();
        }

        /**
         * @param method sets the method
         * @return the builder instance
         */
        public RequestBuilder method(String method) {
            req = req.withMethod(method);
            return this;
        }

        /**
         * @return gives the uri of the request
         */
        public String uri() {
            return req.uri();
        }

        public RequestBuilder uri(URI uri) {
            req = JavaHelpers$.MODULE$.updateRequestWithUri(req, uri);
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
            req = req.withConnection(RemoteConnection$.MODULE$.apply(
                    req.connection().remoteAddress(),
                    secure,
                    req.connection().clientCertificateChain()
            ));
            return this;
        }

        /**
         * @return the status if the request is secure
         */
        public boolean secure() {
           return req.connection().secure();
        }

        /**
         * @return the host name from the header
         */
        public String host() {
          return getHeaders().get(HeaderNames.HOST).orElse(null);
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
            return req.target().path();
        }

        /**
         * This method sets the path of the uri.
         * @param path the path after the port and for the query in a uri
         * @return the builder instance
         */
        public RequestBuilder path(String path) {
            // Update URI with new path element
            URI existingUri = req.target().uri();
            URI newUri;
            try {
                newUri = new URI(
                        existingUri.getScheme(), existingUri.getUserInfo(), existingUri.getHost(),
                        existingUri.getPort(), path, existingUri.getQuery(), existingUri.getFragment());
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("New path couldn't be parsed", e);
            }
            uri(newUri);
            return this;
        }

        /**
         * @return the version
         */
        public String version() {
            return req.version();
        }

        /**
         * @param version the version
         * @return the builder instance
         */
        public RequestBuilder version(String version) {
            req = req.withVersion(version);
            return this;
        }

        /**
         * @return the headers for this request builder
         */
        public Headers getHeaders() {
            return req.headers().asJava();
        }

        /**
         * Set the headers to be used by the request builder.
         *
         * @param headers the headers to be replaced
         * @return the builder instance
         */
        public RequestBuilder headers(Headers headers) {
            req = req.withHeaders(headers.asScala());
            return this;
        }

        /**
         * @param key the key for in the header
         * @param values the values associated with the key
         * @return the builder instance
         */
        public RequestBuilder header(String key, List<String> values) {
            return this.headers(getHeaders().addHeader(key, values));
        }

        /**
         * @param key the key for in the header
         * @param value the value (one) associated with the key
         * @return the builder instance
         */
        public RequestBuilder header(String key, String value) {
            return this.headers(getHeaders().addHeader(key, value));
        }

        /**
         * @return the cookies in Java instances
         */
        public Cookies cookies() {
          return play.core.j.JavaHelpers$.MODULE$.cookiesToJavaCookies(req.cookies());
        }

        /**
         * Sets one cookie.
         * @param cookie the cookie to be set
         * @return the builder instance
         */
        public RequestBuilder cookie(Cookie cookie) {
            play.api.mvc.Cookies newCookies = JavaHelpers$.MODULE$.mergeNewCookie(
                    req.cookies(),
                    cookie.asScala()
            );
            attr(new TypedKey(RequestAttrKey.Cookies()), new AssignedCell(newCookies));
            return this;
        }

        /**
         * @return the cookies in a Java map
         */
        public Map<String,String> flash() {
          return Scala.asJava(req.flash().data());
        }

        /**
         * Sets a cookie in the request.
         * @param key the key for the cookie
         * @param value the value for the cookie
         * @return the builder instance
         */
        public RequestBuilder flash(String key, String value) {
            scala.collection.immutable.Map<String,String> data = req.flash().data();
            scala.collection.immutable.Map<String,String> newData = data.updated(key, value);
            play.api.mvc.Flash newFlash = new play.api.mvc.Flash(newData);
            attr(new TypedKey(RequestAttrKey.Flash()), new AssignedCell(newFlash));
            return this;
        }

        /**
         * Sets cookies in a request.
         * @param data a key value mapping of cookies
         * @return the builder instance
         */
        public RequestBuilder flash(Map<String,String> data) {
            play.api.mvc.Flash flash = new play.api.mvc.Flash(Scala.asScala(data));
            attr(new TypedKey(RequestAttrKey.Flash()), new AssignedCell(flash));
            return this;
        }

        /**
         * @return the sessions in the request
         */
        public Map<String,String> session() {
            return Scala.asJava(req.session().data());
        }

        /**
         * Sets a session.
         * @param key the key for the session
         * @param value the value associated with the key for the session
         * @return the builder instance
         */
        public RequestBuilder session(String key, String value) {
            scala.collection.immutable.Map<String,String> data = req.session().data();
            scala.collection.immutable.Map<String,String> newData = data.updated(key, value);
            play.api.mvc.Session newSession = new play.api.mvc.Session(newData);
            attr(new TypedKey(RequestAttrKey.Session()), new AssignedCell(newSession));
            return this;
        }

        /**
         * Sets all parameters for the session.
         * @param data a key value mapping of the session data
         * @return the builder instance
         */
        public RequestBuilder session(Map<String,String> data) {
            play.api.mvc.Session session = new play.api.mvc.Session(Scala.asScala(data));
              attr(new TypedKey(RequestAttrKey.Session()), new AssignedCell(session));
            return this;
        }

        /**
         * @return the remote address
         */
        public String remoteAddress() {
            return req.connection().remoteAddressString();
        }

        /**
         * @param remoteAddress sets the remote address
         * @return the builder instance
         */
        public RequestBuilder remoteAddress(String remoteAddress) {
            req = req.withConnection(RemoteConnection$.MODULE$.apply(
                    remoteAddress,
                    req.connection().secure(),
                    req.connection().clientCertificateChain()
            ));
            return this;
        }

        /**
         * @return the client X509Certificates if they have been set
         */
        public Optional<List<X509Certificate>> clientCertificateChain() {
            return OptionConverters.toJava(
                    req.connection().clientCertificateChain()).map(
                            list -> new ArrayList<>(Scala.asJava(list)));
        }

        /**
         *
         * @param clientCertificateChain sets the X509Certificates to use
         * @return the builder instance
         */
        public RequestBuilder clientCertificateChain(List<X509Certificate> clientCertificateChain) {
            req = req.withConnection(RemoteConnection$.MODULE$.apply(
                    req.connection().remoteAddress(),
                    req.connection().secure(),
                    OptionConverters.toScala(Optional.ofNullable(Scala.asScala(clientCertificateChain)))
            ));
            return this;
        }
    }

    /**
     * Handle the request body a raw bytes data.
     */
    public abstract static class RawBuffer {

        /**
         * Buffer size.
         *
         * @return the buffer size
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
         *
         * @return the bytes
         */
        public abstract ByteString asBytes();

        /**
         * Returns the buffer content as File
         *
         * @return the file
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

        public interface Part<A> {

        }

        /**
         * A file part.
         */
        public static class FilePart<A> implements Part<A> {

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
             *
             * @return the part name
             */
            public String getKey() {
                return key;
            }

            /**
             * The file name.
             *
             * @return the file name
             */
            public String getFilename() {
                return filename;
            }

            /**
             * The file Content-Type
             *
             * @return the content type
             */
            public String getContentType() {
                return contentType;
            }

            /**
             * The File.
             *
             * @return the file
             */
            public A getFile() {
                return file;
            }

        }

        public static class DataPart implements Part<Source<ByteString, ?>> {
            private final String key;
            private final String value;

            public DataPart(String key, String value) {
                this.key = key;
                this.value = value;
            }

            /**
             * The part name.
             *
             * @return the part name
             */
            public String getKey() {
                return key;
            }

            /**
             * The part value.
             *
             * @return the part value
             */
            public String getValue() {
                return value;
            }

        }

        /**
         * Extract the data parts as Form url encoded.
         *
         * @return the data that was URL encoded
         */
        public abstract Map<String, String[]> asFormUrlEncoded();

        /**
         * Retrieves all file parts.
         *
         * @return the file parts
         */
        public abstract List<FilePart<A>> getFiles();

        /**
         * Access a file part.
         *
         * @param key name of the file part to access
         * @return the file part specified by key
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
         *
         * @param <A> the file type (e.g. play.api.libs.Files.TemporaryFile)
         * @return the content parsed as multipart form data
         */
        public <A> MultipartFormData<A> asMultipartFormData() {
            return as(MultipartFormData.class);
        }

        /**
         * The request content parsed as URL form-encoded.
         *
         * @return the request content parsed as URL form-encoded.
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
         *
         * @return The request content as Array bytes.
         */
        public RawBuffer asRaw() {
            return as(RawBuffer.class);
        }

        /**
         * The request content as text.
         *
         * @return The request content as text.
         */
        public String asText() {
            return as(String.class);
        }

        /**
         * The request content as XML.
         *
         * @return The request content as XML.
         */
        public Document asXml() {
            return as(Document.class);
        }

        /**
         * The request content as Json.
         *
         * @return The request content as Json.
         */
        public JsonNode asJson() {
            return as(JsonNode.class);
        }

        /**
         * Converts a JSON request to a given class. Conversion is performed
         * with [[Json.fromJson(JsonNode,Class)]].
         *
         * Will return Optional.empty() if the request body is not an instance of JsonNode.
         * If the JsonNode simply has missing fields, a valid reference with null fields is returne.
         *
         * @param <A> The type to convert the JSON value to.
         * @param clazz The class to convert the JSON value to.
         * @return The converted value if the request has a JSON body or an empty value if the request has an empty body or a body of a different type.
         */
        public <A> Optional<A> parseJson(Class<A> clazz) {
            return (body instanceof JsonNode) ? Optional.of(Json.fromJson(asJson(), clazz)) : Optional.empty();
        }

        /**
         * The request content as a ByteString.
         *
         * This makes a best effort attempt to convert the parsed body to a ByteString, if it knows how. This includes
         * String, json, XML and form bodies.  It doesn't include multipart/form-data or raw bodies that don't fit in
         * the configured max memory buffer, nor does it include custom output types from custom body parsers.
         *
         * @return the request content as a ByteString
         */
        public ByteString asBytes() {
            if (body == null) {
                return ByteString.empty();
            } else if (body instanceof Optional) {
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
                    return ByteString.fromString(
                            form.entrySet()
                                .stream()
                                .flatMap(entry -> {
                                    String key = encode(entry.getKey());
                                    return Arrays.stream(entry.getValue()).map(
                                        value -> key + "=" + encode(value)
                                    );
                                })
                                .collect(Collectors.joining("&")));
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
         *
         * @param tType class that we are trying to cast the body as
         * @param <T> type of the provided <code>tType</code>
         * @return either a successful cast into T or null
         */
        public <T> T as(Class<T> tType) {
            if (tType.isInstance(body)) {
                return tType.cast(body);
            } else {
                return null;
            }
        }

        public String toString() {
            return "RequestBody of " + (body == null ? "null" : body.getClass());
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
            if (name == null) {
                throw new NullPointerException("Header name cannot be null!");
            }
            if (value == null) {
                throw new NullPointerException("Header value cannot be null!");
            }
            this.headers.put(name, value);
        }

        /**
         * Gets the current response headers.
         *
         * @return the current response headers.
         */
        public Map<String,String> getHeaders() {
            return headers;
        }

        /**
         * Set a new cookie.
         *
         * @param cookie to set
         */
        public void setCookie(Cookie cookie) {
            cookies.add(cookie);
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
            cookies.add(new Cookie(name, "", play.api.mvc.Cookie.DiscardedMaxAge(), path, domain, secure, false, null));
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
        private final SameSite sameSite;

        /**
         * Construct a new cookie. Prefer {@link Cookie#builder} for creating new cookies in your application.
         *
         * @param name Cookie name, must not be null
         * @param value Cookie value
         * @param maxAge Cookie duration in seconds (null for a transient cookie, 0 or less for one that expires now)
         * @param path Cookie path
         * @param domain Cookie domain
         * @param secure Whether the cookie is secured (for HTTPS requests)
         * @param httpOnly Whether the cookie is HTTP only (i.e. not accessible from client-side JavaScript code)
         * @param sameSite the SameSite attribute for this cookie (for CSRF protection).
         */
        public Cookie(String name, String value, Integer maxAge, String path,
                      String domain, boolean secure, boolean httpOnly, SameSite sameSite) {
            this.name = name;
            this.value = value;
            this.maxAge = maxAge;
            this.path = path;
            this.domain = domain;
            this.secure = secure;
            this.httpOnly = httpOnly;
            this.sameSite = sameSite;
        }

        /**
         * @param name the cookie builder name
         * @param value the cookie builder value
         * @return the cookie builder with the specified name and value
         */
        public static CookieBuilder builder(String name, String value) {
            return new CookieBuilder(name, value);
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

        /**
         * @return the SameSite attribute for this cookie
         */
        public Optional<SameSite> sameSite() {
            return Optional.ofNullable(sameSite);
        }

        /**
         * The cookie SameSite attribute
         */
        public enum SameSite {
            STRICT("Strict"), LAX("Lax");

            private final String value;

            SameSite(String value) {
                this.value = value;
            }

            public String value() {
                return this.value;
            }

            public play.api.mvc.Cookie.SameSite asScala() {
                return play.api.mvc.Cookie.SameSite$.MODULE$.parse(value).get();
            }

            public static Optional<SameSite> parse(String sameSite) {
                for (SameSite value : values()) {
                    if (value.value.equalsIgnoreCase(sameSite)) {
                        return Optional.of(value);
                    }
                }
                return Optional.empty();
            }
        }

        public play.api.mvc.Cookie asScala() {
            OptionalInt optMaxAge = maxAge == null ? OptionalInt.empty() : OptionalInt.of(maxAge);
            Optional<String> optDomain = Optional.ofNullable(domain());
            Optional<play.api.mvc.Cookie.SameSite> optSameSite = sameSite().map(SameSite::asScala);
            return new play.api.mvc.Cookie(name(), value(), OptionConverters.toScala(optMaxAge), path(),
                OptionConverters.toScala(optDomain), secure(), httpOnly(), OptionConverters.toScala(optSameSite));
        }
    }

    /*
     * HTTP Cookie builder
     */

    public static class CookieBuilder {

        private String name;
        private String value;
        private Integer maxAge;
        private String path = "/";
        private String domain;
        private boolean secure = false;
        private boolean httpOnly = false;
        private SameSite sameSite;

        /**
         * @param name the cookie builder name
         * @param value the cookie builder value
         * @return the cookie builder with the specified name and value
         */
        private CookieBuilder(String name, String value){
            this.name = name;
            this.value = value;
        }

        /**
         * @param name The name of the cookie
         * @return the cookie builder with the new name
         * */
        public CookieBuilder withName(String name) {
            this.name = name;
            return this;
        }

        /**
         * @param value The value of the cookie
         * @return the cookie builder with the new value
         * */
        public CookieBuilder withValue(String value) {
            this.value = value;
            return this;
        }

        /**
         * Set the maximum age of the cookie.
         *
         * For example, to set a maxAge of 40 days: <code>builder.withMaxAge(Duration.of(40, ChronoUnit.DAYS))</code>
         *
         * @param maxAge a duration representing the maximum age of the cookie. Will be truncated to the nearest second.
         * @return the cookie builder with the new maxAge
         * */
        public CookieBuilder withMaxAge(Duration maxAge) {
            this.maxAge = (int)maxAge.getSeconds();
            return this;
        }

        /**
         * @param path The path of the cookie
         * @return the cookie builder with the new path
         * */
        public CookieBuilder withPath(String path) {
            this.path = path;
            return this;
        }

        /**
         * @param domain The domain of the cookie
         * @return the cookie builder with the new domain
         * */
        public CookieBuilder withDomain(String domain) {
            this.domain = domain;
            return this;
        }

        /**
         * @param secure specify if the cookie is secure
         * @return the cookie builder with the new is secure flag
         * */
        public CookieBuilder withSecure(boolean secure) {
            this.secure = secure;
            return this;
        }

        /**
         * @param httpOnly specify if the cookie is httpOnly
         * @return the cookie builder with the new is httpOnly flag
         * */
        public CookieBuilder withHttpOnly(boolean httpOnly) {
            this.httpOnly = httpOnly;
            return this;
        }

        /**
         * @param sameSite specify if the cookie is SameSite
         * @return the cookie builder with the new SameSite flag
         * */
        public CookieBuilder withSameSite(SameSite sameSite) {
            this.sameSite = sameSite;
            return this;
        }

        /**
         * @return a new cookie with the current builder parameters
         * */
        public Cookie build() {
            return new Cookie(
                this.name, this.value, this.maxAge, this.path, this.domain, this.secure, this.httpOnly, this.sameSite);
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
        String LINK = "Link";
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
        String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";
        String X_FRAME_OPTIONS = "X-Frame-Options";
        String X_XSS_PROTECTION = "X-XSS-Protection";
        String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
        String X_PERMITTED_CROSS_DOMAIN_POLICIES = "X-Permitted-Cross-Domain-Policies";
        String CONTENT_SECURITY_POLICY = "Content-Security-Policy";
        String CONTENT_SECURITY_POLICY_REPORT_ONLY = "Content-Security-Policy-Report-Only";
        String X_CONTENT_SECURITY_POLICY_NONCE_HEADER = "X-Content-Security-Policy-Nonce";
        String REFERRER_POLICY = "Referrer-Policy";
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
        int MULTI_STATUS = 207;

        int MULTIPLE_CHOICES = 300;
        int MOVED_PERMANENTLY = 301;
        int FOUND = 302;
        int SEE_OTHER = 303;
        int NOT_MODIFIED = 304;
        int USE_PROXY = 305;
        int TEMPORARY_REDIRECT = 307;
        int PERMANENT_REDIRECT = 308;

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
        int IM_A_TEAPOT = 418;
        int UNPROCESSABLE_ENTITY = 422;
        int LOCKED = 423;
        int FAILED_DEPENDENCY = 424;
        int UPGRADE_REQUIRED = 426;
        int TOO_MANY_REQUESTS = 429;

        int INTERNAL_SERVER_ERROR = 500;
        int NOT_IMPLEMENTED = 501;
        int BAD_GATEWAY = 502;
        int SERVICE_UNAVAILABLE = 503;
        int GATEWAY_TIMEOUT = 504;
        int HTTP_VERSION_NOT_SUPPORTED = 505;
        int INSUFFICIENT_STORAGE = 507;
    }

    /** Common HTTP MIME types */
    public interface MimeTypes {

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
         * Content-Type of xhtml.
         */
        String XHTML = "application/xhtml+xml";

        /**
         * Content-Type of css.
         */
        String CSS = "text/css";

        /**
         * Content-Type of javascript.
         */
        String JAVASCRIPT = "application/javascript";

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

    /**
     * Standard HTTP Verbs
     */
    public static interface HttpVerbs {
        String GET = "GET";
        String POST = "POST";
        String PUT = "PUT";
        String PATCH = "PATCH";
        String DELETE = "DELETE";
        String HEAD = "HEAD";
        String OPTIONS = "OPTIONS";
    }
}
