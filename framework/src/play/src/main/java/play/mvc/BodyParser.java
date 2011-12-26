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
            return play.core.j.JavaParsers.anyContent(maxLength);
        }
    }
    
    public static class Json implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return play.core.j.JavaParsers.json(maxLength);
        }
    }
    
    public static class TolerantJson implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return play.core.j.JavaParsers.tolerantJson(maxLength);
        }
    }
    
    public static class Xml implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return play.core.j.JavaParsers.xml(maxLength);
        }
    }
    
    public static class TolerantXml implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return play.core.j.JavaParsers.tolerantXml(maxLength);
        }
    }
    
    public static class Text implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return play.core.j.JavaParsers.text(maxLength);
        }
    }
    
    public static class TolerantText implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return play.core.j.JavaParsers.tolerantText(maxLength);
        }
    }
    
    public static class Raw implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return play.core.j.JavaParsers.raw(maxLength);
        }
    }
    
    public static class UrlFormEncoded implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return play.core.j.JavaParsers.urlFormEncoded(maxLength);
        }
    }
    
    public static class MultipartFormData implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return play.core.j.JavaParsers.multipartFormData(maxLength);
        }
    }
    
}