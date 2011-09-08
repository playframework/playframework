package play.mvc;

import play.api.mvc.Results.* ;

public interface Result {

    public play.api.mvc.Result getInternalResult();
    
    public static class Html implements Result {
        
        private play.api.mvc.Result internalResult;
        
        public Html(String html) {
            this.internalResult = play.api.mvc.Results.Html(html);
        }
        
        public play.api.mvc.Result getInternalResult() {
            return this.internalResult;
        }
        
    }
    
    public static class Redirect implements Result {
        
        private play.api.mvc.Result internalResult;
        
        public Redirect(String url) {
            this.internalResult = play.api.mvc.Results.Redirect(url);
        }
        
        public play.api.mvc.Result getInternalResult() {
            return this.internalResult;
        }
        
    }
    
    public static class Ok implements Result {
        
        private play.api.mvc.Result internalResult = play.api.mvc.Results.Text("OK");
        
        public play.api.mvc.Result getInternalResult() {
            return this.internalResult;
        }
        
    }
    
    public static class Text implements Result {
        
        private play.api.mvc.Result internalResult;
        
        public Text(String text) {
            this.internalResult = play.api.mvc.Results.Text(text);
        }
        
        public play.api.mvc.Result getInternalResult() {
            return this.internalResult;
        }
    
    }
    
}