package play.mvc;

import java.lang.annotation.*;

import play.api.libs.iteratee.*;

public interface BodyParser {
    
    play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength);
    
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Of {
        Class<? extends BodyParser> value();
        int maxLength() default Integer.MAX_VALUE;
    }
    
    public static class AnyContent implements BodyParser {        
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return play.core.j.JParsers.anyContent(maxLength);
        }
    }
    
    public static class Json implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return play.core.j.JParsers.json(maxLength);
        }
    }
    
    public static class TolerantJson implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return play.core.j.JParsers.tolerantJson(maxLength);
        }
    }
    
    public static class Xml implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return play.core.j.JParsers.xml(maxLength);
        }
    }
    
    public static class TolerantXml implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return play.core.j.JParsers.tolerantXml(maxLength);
        }
    }
    
    public static class Text implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return play.core.j.JParsers.text(maxLength);
        }
    }
    
    public static class TolerantText implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return play.core.j.JParsers.tolerantText(maxLength);
        }
    }
    
    public static class Raw implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return play.core.j.JParsers.raw(maxLength);
        }
    }
    
    public static class UrlFormEncoded implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return play.core.j.JParsers.urlFormEncoded(maxLength);
        }
    }
    
    public static class MultipartFormData implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return play.core.j.JParsers.multipartFormData(maxLength);
        }
    }
    
}