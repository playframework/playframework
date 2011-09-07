package play.mvc;

public class Http {
    
    public abstract static class Context {
        
        public static ThreadLocal<Context> current = new ThreadLocal<Context>();
        
        public static Context current() {
            return current.get();
        }
        
        public abstract Request request();
        
    }
    
    public abstract static class Request {
        
        public abstract String uri();
        public abstract String method();
        public abstract String path();
        
    }
    
}