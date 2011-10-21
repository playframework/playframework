package play.mvc;

import java.util.*;

/**
 * Define HTTP standard objects.
 */
public class Http {
    
    /**
     * The global HTTP context.
     */
    public static class Context {
        
        public static ThreadLocal<Context> current = new ThreadLocal<Context>();
        
        /**
         * Retrieve the current HTTP context, for the current thread.
         */
        public static Context current() {
            return current.get();
        }
        
        //
        
        private final Request request;
        private final Response response;
        private final Session session;
        private final Flash flash;
        
        /**
         * Create a new HTTP context.
         *
         * @param request The HTTP request
         * @param sessionData The session data extracted from the session coookie.
         * @param flashData The flash data extracted from the flash coookie.
         */
        public Context(Request request, Map<String,String> sessionData, Map<String,String> flashData) {
            this.request = request;
            this.response = new Response();
            this.session = new Session(sessionData);
            this.flash = new Flash(flashData);
        }
        
        /**
         * Retrieve the current request.
         */
        public Request request() {
            return request;
        }
        
        /**
         * Retrieve the current response.
         */
        public Response response() {
            return response;
        }
        
        /**
         * Retrieve the current session.
         */
        public Session session() {
            return session;
        }
        
        /**
         * Retrieve the current flash scope.
         */
        public Flash flash() {
            return flash;
        }
        
        /**
         * Import in templates to get implicit HTTP context.
         */
        public static class Implicit {
            
            /**
             * Retrieve the current response.
             */
            public static Response response() {
                return Context.current().response();
            }
            
            /**
             * Retrieve the current request.
             */
            public static Request request() {
                return Context.current().request();
            }
            
            /**
             * Retrieve the current flash scope.
             */
            public static Flash flash() {
                return Context.current().flash();
            }
            
            /**
             * Retrieve the current session.
             */
            public static Session session() {
                return Context.current().session();
            }
            
        }
        
    }
    
    /**
     * An HTTP request.
     */
    public abstract static class Request {
        
        /**
         * The complete request URI (contains both path and query string).
         */
        public abstract String uri();
        
        /**
         * The HTTP Method.
         */
        public abstract String method();
        
        /**
         * The URI path.
         */
        public abstract String path();
        
        /**
         * The request content parsed as url form encoded.
         */
        public abstract Map<String,String[]> urlFormEncoded();
        

        private String username = null;
        
        /**
         * The username if defined for this request.
         * It is usually set by annotation your Action with @Authenticated.
         */
        public String username() {
            return username;
        }
        
        /**
         * Defines the username for this request.
         */
        public void setUsername(String username) {
            this.username = username;
        }
        
    }
    
    /**
     * The HTTP response.
     */
    public static class Response implements HeaderNames {
        
        private final Map<String,String> headers = new HashMap<String,String>();
        
        /**
         * Add a new header to the response.
         */ 
        public void setHeader(String name, String Stringue) {
            this.headers.put(name, Stringue);
        }
        
        /**
         * Get the current response headers.
         */
        public Map<String,String> getHeaders() {
            return headers;
        }
        
        /**
         * Set the content-type of the response.
         */
        public void setContentType(String contentType) {
            setHeader(CONTENT_TYPE, contentType);
        }
        
    }
    
    /**
     * HTTP Session.
     *
     * Session data are encoded into an HTTP cookie, and can only contain simple String values.
     */
    public static class Session extends HashMap<String,String>{
        
        public boolean isDirty = false;
        
        public Session(Map<String,String> data) {
            super(data);
        }
        
        /**
         * Remove any value from the session.
         */
        @Override
        public String remove(Object key) {
            isDirty = true;
            return super.remove(key);
        }
        
        /**
         * Add value to the session.
         */
        @Override
        public String put(String key, String value) {
            isDirty = true;
            return super.put(key, value);
        }
        
        /**
         * Add values to the session.
         */
        @Override
        public void putAll(Map<? extends String,? extends String> values) {
            isDirty = true;
            super.putAll(values);
        }
        
        /**
         * Clear the session.
         */
        @Override
        public void clear() {
            isDirty = true;
            super.clear();
        }
        
    }
    
    /**
     * HTTP Flash.
     *
     * Flash data are encoded into an HTTP cookie, and can only contain simple String values.
     */
    public static class Flash extends HashMap<String,String>{
        
        public boolean isDirty = false;
        
        public Flash(Map<String,String> data) {
            super(data);
        }
        
        /**
         * Remove any value from the flash scope.
         */
        @Override
        public String remove(Object key) {
            isDirty = true;
            return super.remove(key);
        }
        
        /**
         * Add value to the flash scope.
         */
        @Override
        public String put(String key, String value) {
            isDirty = true;
            return super.put(key, value);
        }
        
        /**
         * Add values to the flash scope.
         */
        @Override
        public void putAll(Map<? extends String,? extends String> values) {
            isDirty = true;
            super.putAll(values);
        }
        
        /**
         * Clear the flash scope.
         */
        @Override
        public void clear() {
            isDirty = true;
            super.clear();
        }        
        
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
     * Defines all standard HTTP Status.
     */
    public static class Status {

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