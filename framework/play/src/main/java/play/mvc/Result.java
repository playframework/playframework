package play.mvc;

import play.api.mvc.Results.* ;

public abstract class Result {
    

    public play.api.mvc.Result original;
    
    public Result(play.api.mvc.Result original) {
        this.original = original;
    }
    
    public static class Html extends Result {
        public Html(String html) {
            super( play.api.mvc.Results.Html(html));
        }
    }
    
    public static class Ok extends Result {
        public Ok() {
            super( play.api.mvc.Results.Text("OK"));
        }
    }
    
    public static class Redirect extends Result {
        public Redirect(String url) {
            super( play.api.mvc.Results.Redirect(url));
        }
    }
    
    public static class Text extends Result {
        public Text(String text) {
            super( play.api.mvc.Results.Text(text));
        }
    }
    
}