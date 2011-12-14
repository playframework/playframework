package play.mvc;

import java.lang.annotation.*;

import play.api.libs.iteratee.*;

public interface BodyParser {
    
    play.api.mvc.BodyParser<Http.RequestBody> parser();
    
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Of {
        Class<? extends BodyParser> value();
    }
    
    public static class AnyContent implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser() {
            return play.api.mvc.JParsers.anyContent();
        }
    }
    
    public static class Json implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser() {
            return play.api.mvc.JParsers.json();
        }
    }
    
    public static class TolerantJson implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser() {
            return play.api.mvc.JParsers.tolerantJson();
        }
    }
    
    public static class Xml implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser() {
            return play.api.mvc.JParsers.xml();
        }
    }
    
    public static class TolerantXml implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser() {
            return play.api.mvc.JParsers.tolerantXml();
        }
    }
    
    public static class Text implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser() {
            return play.api.mvc.JParsers.text();
        }
    }
    
    public static class TolerantText implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser() {
            return play.api.mvc.JParsers.tolerantText();
        }
    }
    
    public static class Raw implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser() {
            return play.api.mvc.JParsers.raw();
        }
    }
    
    public static class UrlFormEncoded implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser() {
            return play.api.mvc.JParsers.urlFormEncoded();
        }
    }
    
}