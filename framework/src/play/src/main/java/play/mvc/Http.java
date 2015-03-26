/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.mvc;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.*;
import java.util.Map.Entry;
import scala.Predef;
import scala.Tuple2;
import scala.collection.JavaConversions;
import scala.collection.JavaConverters;
import scala.collection.Seq;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import com.fasterxml.jackson.databind.JsonNode;

import play.api.libs.json.JsValue;
import play.api.mvc.AnyContent;
import play.api.mvc.AnyContentAsFormUrlEncoded;
import play.api.mvc.AnyContentAsJson;
import play.api.mvc.AnyContentAsRaw;
import play.api.mvc.AnyContentAsText;
import play.api.mvc.AnyContentAsXml;
import play.api.mvc.Headers;
import play.api.mvc.RawBuffer;
import play.core.system.RequestIdProvider;
import play.i18n.Lang;
import play.Play;
import play.i18n.Langs;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.libs.Scala;

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
         * @return the current lang.
         */
        public Lang lang() {
            if (lang != null) {
                return lang;
            } else {
                return messages().lang();
            }
        }

        /**
         * @return the messages for the current lang.
         */
        public Messages messages() {
            return Play.application().injector().instanceOf(MessagesApi.class).preferred(request());
        }

        /**
         * Change durably the lang for the current user.
         * @param code New lang code to use (e.g. "fr", "en-US", etc.)
         * @return true if the requested lang was supported by the application, otherwise false.
         */
        public boolean changeLang(String code) {
            return changeLang(Lang.forCode(code));
        }

        /**
         * Change durably the lang for the current user.
         * @param lang New Lang object to use.
         * @return true if the requested lang was supported by the application, otherwise false.
         */
        public boolean changeLang(Lang lang) {
            if (Lang.availables().contains(lang)) {
                this.lang = lang;
                response.setCookie(Play.langCookieName(), lang.code());
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
            response.discardCookie(Play.langCookieName());
        }

        /**
         * Free space to store your request specific data
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
             * @return the messages for the current lang.
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

        public String toString() {
            return "Context attached to (" + request() + ")";
        }

    }

    /**
     * A wrapped context.
     *
     * Use this to modify the context in some way.
     */
    public static abstract class WrappedContext extends Context {
        private final Context wrapped;

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
         * @return The media types set in the request Accept header, sorted by preference (preferred first).
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
         * @return the cookie, if found, otherwise null.
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
         * @param headerName The name of the header (case-insensitive).
         */
        String getHeader(String headerName);

        /**
         * Checks if the request has the header.
         *
         * @param headerName The name of the header (case-insensitive).
         */
        boolean hasHeader(String headerName);

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
         * @deprecated As of release 2.4, use {@link #withUsername}.
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

        public RequestImpl(play.api.mvc.RequestHeader header) {
            super(header);
            this.underlying = null;
        }

        public RequestImpl(play.api.mvc.Request<RequestBody> request) {
            super(request);
            this.underlying = request;
        }

        private RequestImpl(play.api.mvc.Request<RequestBody> request,
                            String username) {
            
            super(request);
            
            this.underlying = request;
            this.username = username;
        }        

        public RequestBody body() {
            return underlying != null ? underlying.body() : null;
        }

        public String username() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public Request withUsername(String username) {
            return new RequestImpl(this.underlying, username);
        }

        public play.api.mvc.Request<RequestBody> _underlyingRequest() {
            return underlying;
        }

    }

    public static class RequestBuilder {

        protected AnyContent body;
        protected String username;

        public RequestBuilder() {
          method("GET");
          uri("/");
          host("localhost");
          version("HTTP/1.1");
          remoteAddress("127.0.0.1");
          body(play.api.mvc.AnyContentAsEmpty$.MODULE$);
        }

        public RequestBody body() {
            if (body == null) {
                return null;
            }
            return new play.core.j.JavaParsers.DefaultRequestBody(
                body.asFormUrlEncoded(),
                body.asRaw(),
                body.asText(),
                body.asJson(),
                body.asXml(),
                body.asMultipartFormData());
        }

        public AnyContent bodyAsAnyContent() {
            return body;
        }

        public String username() {
            return username;
        }

        public RequestBuilder username(String username) {
            this.username = username;
            return this;
        }

        /**
         * Set a AnyContent to this request.
         * @param anyContent the AnyContent
         * @param contentType Content-Type header value
         */
        protected RequestBuilder body(AnyContent anyContent, String contentType) {
            header("Content-Type", contentType);
            body(anyContent);
            return this;
        }

        /**
         * Set a AnyContent to this request.
         * @param anyContent the AnyContent
         */
        protected RequestBuilder body(AnyContent anyContent) {
            body = anyContent;
            return this;
        }

        /**
         * Set a Binary Data to this request.
         * The <tt>Content-Type</tt> header of the request is set to <tt>application/octet-stream</tt>.
         * @param data the Binary Data
         */
        public RequestBuilder bodyRaw(byte[] data) {
            play.api.mvc.RawBuffer buffer = new play.api.mvc.RawBuffer(data.length, data);
            return body(new AnyContentAsRaw(buffer), "application/octet-stream");
        }

        /**
         * Set a Form url encoded body to this request.
         */
        public RequestBuilder bodyFormArrayValues(Map<String,String[]> data) {
            Map<String,Seq<String>> seqs = new HashMap<>();
            for (Entry<String,String[]> entry: data.entrySet()) {
                seqs.put(entry.getKey(), Predef.genericWrapArray(entry.getValue()));
            }
            scala.collection.immutable.Map<String,Seq<String>> map = mapToScala(seqs);
            return body(new AnyContentAsFormUrlEncoded(map), "application/x-www-form-urlencoded");
        }

        /**
         * Set a Form url encoded body to this request.
         */
        public RequestBuilder bodyForm(Map<String,String> data) {
            Map<String,Seq<String>> seqs = new HashMap<>();
            for (Entry<String,String> entry: data.entrySet()) {
                seqs.put(entry.getKey(), JavaConversions.asScalaBuffer(Arrays.asList(entry.getValue())));
            }
            scala.collection.immutable.Map<String,Seq<String>> map = mapToScala(seqs);
            return body(new AnyContentAsFormUrlEncoded(map), "application/x-www-form-urlencoded");
        }

        /**
         * Set a Json Body to this request.
         * The <tt>Content-Type</tt> header of the request is set to <tt>application/json</tt>.
         * @param node the Json Node
         */
        public RequestBuilder bodyJson(JsonNode node) {
            return bodyJson(play.api.libs.json.JacksonJson$.MODULE$.jsonNodeToJsValue(node));
        }

        /**
         * Set a Json Body to this request.
         * The <tt>Content-Type</tt> header of the request is set to <tt>application/json</tt>.
         * @param json the JsValue
         */
        public RequestBuilder bodyJson(JsValue json) {
            return body(new AnyContentAsJson(json), "application/json");
        }

        /**
         * Set a XML to this request.
         * The <tt>Content-Type</tt> header of the request is set to <tt>application/xml</tt>.
         * @param xml the XML
         */
        public RequestBuilder bodyXml(InputSource xml) {
            return body(new AnyContentAsXml(scala.xml.XML.load(xml)), "application/xml");
        }

        /**
         * Set a Text to this request.
         * The <tt>Content-Type</tt> header of the request is set to <tt>text/plain</tt>.
         * @param text the text
         */
        public RequestBuilder bodyText(String text) {
            return body(new AnyContentAsText(text), "text/plain");
        }

        public RequestImpl build() {
            return new RequestImpl(new play.api.mvc.RequestImpl(
                body(),
                id,
                mapToScala(tags()),
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

        public Long id() {
            return id;
        }

        public RequestBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public Map<String, String> tags() {
            return tags;
        }

        public RequestBuilder tags(Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        public RequestBuilder tag(String key, String value) {
            tags.put(key, value);
            return this;
        }

        public String method() {
            return method;
        }

        public RequestBuilder method(String method) {
            this.method = method;
            return this;
        }

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

        public RequestBuilder uri(String str) {
            try {
                uri(new URI(str));
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Exception parsing URI", e);
            }
            return this;
        }

        public RequestBuilder secure(boolean secure) {
           this.secure = secure;
           return this;
        }

        public boolean secure() {
           return secure;
        }

        public String host() {
          return header(HeaderNames.HOST);
        }

        public RequestBuilder host(String host) {
          header(HeaderNames.HOST, host);
          return this;
        }

        public String path() {
            return uri.getRawPath();
        }

        public RequestBuilder path(String path) {
            try {
                uri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), path, uri.getQuery(), uri.getFragment());
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
            return this;
        }

        public String version() {
            return version;
        }

        public RequestBuilder version(String version) {
            this.version = version;
            return this;
        }

        public String header(String key) {
            String[] values = headers.get(key);
            return values == null || values.length == 0 ? null : values[0];
        }

        public String[] headers(String key) {
            return headers.get(key);
        }

        public Map<String, String[]> headers() {
            return headers;
        }

        public RequestBuilder headers(Map<String, String[]> headers) {
            this.headers = headers;
            return this;
        }

        public RequestBuilder header(String key, String[] values) {
            headers.put(key, values);
            return this;
        }

        public RequestBuilder header(String key, String value) {
            headers.put(key, new String[] { value });
            return this;
        }

        private play.api.mvc.Cookies scalaCookies() {
          String cookieHeader = header(HeaderNames.COOKIE);
          scala.Option<String> cookieHeaderOpt = scala.Option.apply(cookieHeader);
          return play.api.mvc.Cookies$.MODULE$.apply(cookieHeaderOpt);
        }

        public Cookies cookies() {
          return play.core.j.JavaHelpers$.MODULE$.cookiesToJavaCookies(scalaCookies());
        }

        private void cookies(Seq<play.api.mvc.Cookie> cookies) {
          String cookieHeader = header(HeaderNames.COOKIE);
          String value = play.api.mvc.Cookies$.MODULE$.merge(cookieHeader != null ? cookieHeader : "", cookies);
          header(HeaderNames.COOKIE, value);
        }

        public RequestBuilder cookie(Cookie cookie) {
          cookies(play.core.j.JavaHelpers$.MODULE$.cookiesToScalaCookies(Arrays.asList(cookie)));
          return this;
        }

        public Map<String,String> flash() {
          play.api.mvc.Cookies scalaCookies = scalaCookies();
          scala.Option<play.api.mvc.Cookie> cookie = scalaCookies.get(play.api.mvc.Flash$.MODULE$.COOKIE_NAME());
          scala.collection.Map<String,String> data = play.api.mvc.Flash$.MODULE$.decodeCookieToMap(cookie);
          return JavaConversions.mapAsJavaMap(data);
        }

        public RequestBuilder flash(String key, String value) {
          Map<String,String> data = new HashMap<>(flash());
          data.put(key, value);
          flash(data);
          return this;
        }

        public RequestBuilder flash(Map<String,String> data) {
          play.api.mvc.Flash flash = new play.api.mvc.Flash(mapToScala(data));
          cookies(JavaConversions.asScalaBuffer(Arrays.asList(play.api.mvc.Flash$.MODULE$.encodeAsCookie(flash))));
          return this;
        }

        public Map<String,String> session() {
          play.api.mvc.Cookies scalaCookies = scalaCookies();
          scala.Option<play.api.mvc.Cookie> cookie = scalaCookies.get(play.api.mvc.Session$.MODULE$.COOKIE_NAME());
          scala.collection.Map<String,String> data = play.api.mvc.Session$.MODULE$.decodeCookieToMap(cookie);
          return JavaConversions.mapAsJavaMap(data);
        }

        public RequestBuilder session(String key, String value) {
          Map<String,String> data = new HashMap<>(session());
          data.put(key, value);
          session(data);
          return this;
        }

        public RequestBuilder session(Map<String,String> data) {
          play.api.mvc.Session session = new play.api.mvc.Session(mapToScala(data));
          cookies(JavaConversions.asScalaBuffer(Arrays.asList(play.api.mvc.Session$.MODULE$.encodeAsCookie(session))));
          return this;
        }

        public String remoteAddress() {
            return remoteAddress;
        }

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
            return mapToScala(seqs);
        }

        protected static <A, B> scala.collection.immutable.Map<A, B> mapToScala(java.util.Map<A, B> m) {
          return JavaConverters.mapAsScalaMapConverter(m).asScala().toMap(
            Predef.<Tuple2<A, B>>conforms()
          );
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
         * @param maxLength The max length allowed to be stored in memory.
         * @return null if the content is too big to fit in memory.
         */
        public abstract byte[] asBytes(int maxLength);

        /**
         * Returns the buffer content as a bytes array.
         */
        public abstract byte[] asBytes();

        /**
         * Returns the buffer content as File.
         */
        public abstract File asFile();

    }

    /**
     * Multipart form data body.
     */
    public abstract static class MultipartFormData {

        /**
         * A file part.
         */
        public static class FilePart {

            final String key;
            final String filename;
            final String contentType;
            final File file;

            public FilePart(String key, String filename, String contentType, File file) {
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
            public File getFile() {
                return file;
            }

        }

        /**
         * Extract the data parts as Form url encoded.
         */
        public abstract Map<String,String[]> asFormUrlEncoded();

        /**
         * Retrieves all file parts.
         */
        public abstract List<FilePart> getFiles();

        /**
         * Access a file part.
         */
        public FilePart getFile(String key) {
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
    public static class RequestBody {

        /**
         * @deprecated Since Play 2.4, this method always returns false. When the max size is exceeded, a 413 error is
         *             returned.
         */
        @Deprecated
        public boolean isMaxSizeExceeded() {
            return false;
        }

        /**
         * The request content parsed as multipart form data.
         */
        public MultipartFormData asMultipartFormData() {
            return null;
        }

        /**
         * The request content parsed as URL form-encoded.
         */
        public Map<String,String[]> asFormUrlEncoded() {
            return null;
        }

        /**
         * The request content as Array bytes.
         */
        public RawBuffer asRaw() {
            return null;
        }

        /**
         * The request content as text.
         */
        public String asText() {
            return null;
        }

        /**
         * The request content as XML.
         */
        public Document asXml() {
            return null;
        }

        /**
         * The request content as Json.
         */
        public JsonNode asJson() {
            return null;
        }

        /**
         * Cast this RequestBody as T if possible.
         */
        @SuppressWarnings("unchecked")
        public <T> T as(Class<T> tType) {
            if(this.getClass().isAssignableFrom(tType)) {
                return (T)this;
            } else {
                return null;
            }
        }

    }

    /**
     * The HTTP response.
     */
    public static class Response implements HeaderNames {

        private final Map<String, String> headers = new TreeMap<String, String>(new Comparator<String>() {
                @Override public int compare(String s1, String s2) {
                    return s1.compareToIgnoreCase(s2);
                }
            });
        private final List<Cookie> cookies = new ArrayList<Cookie>();

        /**
         * Adds a new header to the response.
         *
         * @param name The name of the header. Must not be null.
         * @param value The value of the header. Must not be null.
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
         * Sets the content-type of the response.
         *
         * @param contentType The content type.  Must not be null.
         */
        public void setContentType(String contentType) {
            setHeader(CONTENT_TYPE, contentType);
        }

        /**
         * Set a new transient cookie with path "/"<br>
         * For example:
         * <pre>
         * response().setCookie("theme", "blue");
         * </pre>
         * @param name Cookie name.  Must not be null.
         * @param value Cookie value.
         */
        public void setCookie(String name, String value) {
            setCookie(name, value, null);
        }

        /**
         * Set a new cookie with path "/"
         * @param name Cookie name.  Must not be null.
         * @param value Cookie value.
         * @param maxAge Cookie duration (null for a transient cookie and 0 or less for a cookie that expires now).
         */
        public void setCookie(String name, String value, Integer maxAge) {
            setCookie(name, value, maxAge, "/");
        }

        /**
         * Set a new cookie
         * @param name Cookie name.  Must not be null.
         * @param value Cookie value
         * @param maxAge Cookie duration (null for a transient cookie and 0 or less for a cookie that expires now)
         * @param path Cookie path
         */
        public void setCookie(String name, String value, Integer maxAge, String path) {
            setCookie(name, value, maxAge, path, null);
        }

        /**
         * Set a new cookie
         * @param name Cookie name.  Must not be null.
         * @param value Cookie value
         * @param maxAge Cookie duration (null for a transient cookie and 0 or less for a cookie that expires now)
         * @param path Cookie path
         * @param domain Cookie domain
         */
        public void setCookie(String name, String value, Integer maxAge, String path, String domain) {
            setCookie(name, value, maxAge, path, domain, false, false);
        }

        /**
         * Set a new cookie
         * @param name Cookie name.  Must not be null.
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
         * Discard a cookie on the default path ("/") with no domain and that's not secure
         *
         * @param name The name of the cookie to discard.  Must not be null.
         */
        public void discardCookie(String name) {
            discardCookie(name, "/", null, false);
        }

        /**
         * Discard a cookie on the given path with no domain and not that's secure
         *
         * @param name The name of the cookie to discard.  Must not be null.
         * @param path The path of the cookie te discard, may be null
         */
        public void discardCookie(String name, String path) {
            discardCookie(name, path, null, false);
        }

        /**
         * Discard a cookie on the given path and domain that's not secure
         *
         * @param name The name of the cookie to discard.  Must not be null.
         * @param path The path of the cookie te discard, may be null
         * @param domain The domain of the cookie to discard, may be null
         */
        public void discardCookie(String name, String path, String domain) {
            discardCookie(name, path, domain, false);
        }

        /**
         * Discard a cookie in this result
         *
         * @param name The name of the cookie to discard.  Must not be null.
         * @param path The path of the cookie te discard, may be null
         * @param domain The domain of the cookie to discard, may be null
         * @param secure Whether the cookie to discard is secure
         */
        public void discardCookie(String name, String path, String domain, boolean secure) {
            cookies.add(new Cookie(name, "", -86400, path, domain, secure, false));
        }

        // FIXME return a more convenient type? e.g. Map<String, Cookie>
        public Iterable<Cookie> cookies() {
            return cookies;
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
         * @return the cookie that is associated with the given name, or null if there is no such cookie
         */
        public Cookie get(String name);

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
