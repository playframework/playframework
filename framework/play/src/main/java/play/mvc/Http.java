package play.mvc;

import java.util.*;

public class Http {
    
    public static class Context {
        
        public static ThreadLocal<Context> current = new ThreadLocal<Context>();
        
        public static Context current() {
            return current.get();
        }
        
        //
        
        Request request;
        Response response;
        Session session;
        Map<String,Object> data;
        
        public Context(Request request, Map<String,String> sessionData) {
            this.request = request;
            this.response = new Response();
            this.session = new Session(sessionData);
            this.data = new HashMap<String,Object>();
        }
        
        public Request request() {
            return request;
        }
        
        public Response response() {
            return response;
        }
        
        public Session session() {
            return session;
        }
        
        public Object get(String key) {
            return data.get(key);
        }
        
        public void set(String key, Object value) {
            data.put(key, value);
        }
        
    }
    
    public abstract static class Request {
        
        public abstract String uri();
        public abstract String method();
        public abstract String path();
        public abstract Map<String,String[]> urlFormEncoded();
        
        private String username = null;
        
        public String username() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
    }
    
    public static class Response implements HeaderNames {
        
        final Map<String,String> headers = new HashMap<String,String>();
        
        public void setHeader(String name, String Stringue) {
            this.headers.put(name, Stringue);
        }
        
        public Map<String,String> getHeaders() {
            return headers;
        }
        
        public void setContentType(String contentType) {
            setHeader(CONTENT_TYPE, contentType);
        }
        
    }
    
    public static class Session extends HashMap<String,String>{
        
        public Session(Map<String,String> data) {
            super(data);
        }
        
    }
    
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
    
}