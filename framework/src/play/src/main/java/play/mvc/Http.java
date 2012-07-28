package play.mvc;

import java.io.*;
import java.util.*;

import org.w3c.dom.*;
import org.codehaus.jackson.*;

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
        
        private final Request request;
        private final Response response;
        private final Session session;
        private final Flash flash;
        
        
        /**
         * Creates a new HTTP context.
         *
         * @param request the HTTP request
         * @param sessionData the session data extracted from the session cookie
         * @param flashData the flash data extracted from the flash cookie
         */
        public Context(Request request, Map<String,String> sessionData, Map<String,String> flashData) {
            this.request = request;
            this.response = new Response();
            this.session = new Session(sessionData);
            this.flash = new Flash(flashData);
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
         * Free space to store your request specific data
         */
        public Map<String, Object> args = new HashMap<String, Object>(16);

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
            public static play.i18n.Lang lang() {
                return play.i18n.Lang.preferred(Context.current().request().acceptLanguages());
            }
            
            /**
             * Returns the current context.
             */
            public static Context ctx() {
                return Context.current();
            }
            
        }
        
    }
    
    public abstract static class RequestHeader {
        /**
         * The complete request URI, containing both path and query string.
         */
        public abstract String uri();
        
        /**
         * The HTTP Method.
         */
        public abstract String method();

        /**
         * The client IP address.
         */
        public abstract String remoteAddress();

        
        /**
         * The request host.
         */
        public abstract String host();
        /**
         * The URI path.
         */
        public abstract String path();
        
        /**
         * The Request Langs, extracted from the Accept-Language header.
         */
        public abstract List<play.i18n.Lang> acceptLanguages();
        
        /**
         * @return The media types set in the request Accept header, not sorted in any particular order.
         */
        public abstract List<String> accept();
        
        /**
         * Check if this request accepts a given media type.
         * @returns true if <code>mediaType</code> is in the Accept header, otherwise false
         */
        public abstract boolean accepts(String mediaType);
        
        /**
         * The query string content.
         */
        public abstract Map<String,String[]> queryString();

        /**
         * @return the request cookies
         */
        public abstract Cookies cookies(); // FIXME Provide a “Cookie cookie(String name)” function instead of this one?
        
        /**
         * Retrieves all headers.
         *
         * @return headers
         */
        public abstract java.util.Map<String,String[]> headers();
        
        /**
         * Retrieves a single header.
         */
        public String getHeader(String headerName) {
            String[] headers = null;
            for(String h: headers().keySet()) {
                if(headerName.toLowerCase().equals(h.toLowerCase())) {
                    headers = headers().get(h);
                    break;
                }
            }
            if(headers == null || headers.length == 0) {
                return null;
            }
            return headers[0];
        }

    }
    
    /**
     * An HTTP request.
     */
    public abstract static class Request extends RequestHeader {

        /**
         * The request body.
         */
        public abstract RequestBody body();

        // -- username

        private String username = null;
        
        /**
         * The user name for this request, if defined.
         * This is usually set by annotating your Action with <code>@Authenticated</code>.
         */
        public String username() {
            return username;
        }
        
        /**
         * Defines the user name for this request.
         */
        public void setUsername(String username) {
            this.username = username;
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
        
        private final Map<String,String> headers = new HashMap<String,String>();
        private final List<Cookie> cookies = new ArrayList<Cookie>();
        private final List<String> discardedCookies = new ArrayList<String>();
        
        /**
         * Adds a new header to the response.
         */ 
        public void setHeader(String name, String Stringue) {
            this.headers.put(name, Stringue);
        }
        
        /**
         * Gets the current response headers.
         */
        public Map<String,String> getHeaders() {
            return headers;
        }
        
        /**
         * Sets the content-type of the response.
         */
        public void setContentType(String contentType) {
            setHeader(CONTENT_TYPE, contentType);
        }
        
        /**
         * Set a new transient cookie with path “/”<br />
         * For example:
         * <pre>
         * response().setCookie("theme", "blue");
         * </pre>
         * @param name Cookie name
         * @param value Cookie value
         */
        public void setCookie(String name, String value) {
          setCookie(name, value, -1);
        }
        
        /**
         * Set a new cookie with path “/”
         * @param name Cookie name
         * @param value Cookie value
         * @param maxAge Cookie duration (-1 for a transient cookie and 0 for a cookie that expires now)
         */
        public void setCookie(String name, String value, int maxAge) {
          setCookie(name, value, maxAge, "/");
        }

        /**
         * Set a new cookie
         * @param name Cookie name
         * @param value Cookie value
         * @param maxAge Cookie duration (-1 for a transient cookie and 0 for a cookie that expires now)
         * @param path Cookie path
         */
        public void setCookie(String name, String value, int maxAge, String path) {
          setCookie(name, value, maxAge, path, null);
        }
        
        /**
         * Set a new cookie
         * @param name Cookie name
         * @param value Cookie value
         * @param maxAge Cookie duration (-1 for a transient cookie and 0 for a cookie that expires now)
         * @param path Cookie path
         * @param domain Cookie domain
         */
        public void setCookie(String name, String value, int maxAge, String path, String domain) {
          setCookie(name, value, maxAge, path, domain, false, false);
        }

        /**
         * Set a new cookie
         * @param name Cookie name
         * @param value Cookie value
         * @param maxAge Cookie duration (-1 for a transient cookie and 0 for a cookie that expires now)
         * @param path Cookie path
         * @param domain Cookie domain
         * @param secure Whether the cookie is secured (for HTTPS requests)
         * @param httpOnly Whether the cookie is HTTP only (i.e. not accessible from client-side JavaScript code)
         */
        public void setCookie(String name, String value, int maxAge, String path, String domain, boolean secure, boolean httpOnly) {
          cookies.add(new Cookie(name, value, maxAge, path, domain, secure, httpOnly));
        }

        /**
         * Discard cookies along this result<br />
         * For example:
         * <pre>
         * response().discardCookies("theme");
         * </pre>
         * @param names Names of the cookies to discard
         */
        public void discardCookies(String... names) {
          discardedCookies.addAll(Arrays.asList(names));
        }
        
        // FIXME return a more convenient type? e.g. Map<String, Cookie>
        public Iterable<Cookie> cookies() {
          return cookies;
        }
        
        public Iterable<String> discardedCookies() {
          return discardedCookies;
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
        private final int maxAge;
        private final String path;
        private final String domain;
        private final boolean secure;
        private final boolean httpOnly;
        
        public Cookie(String name, String value, int maxAge, String path,
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
         * @return the cookie expiration date in seconds, -1 for a transient cookie, 0 for a cookie that expires now
         */
        public int maxAge() {
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
    public interface Cookies {
        
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
        String ETAG = "Etag";
        String EXPECT = "Expect";
        String EXPIRES = "Expires";
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
    
}