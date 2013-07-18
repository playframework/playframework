package play.mvc;

import java.lang.annotation.*;

/**
 * A body parser parses the HTTP request body content.
 */
public interface BodyParser {

    play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength);

    /**
     * Specify the body parser to use for an Action method.
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Of {
        Class<? extends BodyParser> value();
        int maxLength() default -1;
    }

    /**
     * Guess the body content by checking the Content-Type header.
     */
    public static class AnyContent implements BodyParser {        
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return play.core.j.JavaParsers.anyContent(maxLength);
        }
    }

    /**
     * Parse the body as Json if the Content-Type is text/json or application/json.
     */
    public static class Json implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return play.core.j.JavaParsers.json(maxLength);
        }
    }

    /**
     * Parse the body as Json without checking the Content-Type.
     */
    public static class TolerantJson implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return play.core.j.JavaParsers.tolerantJson(maxLength);
        }
    }

    /**
     * Parse the body as Xml if the Content-Type is application/xml.
     */
    public static class Xml implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return play.core.j.JavaParsers.xml(maxLength);
        }
    }

    /**
     * Parse the body as Xml without checking the Content-Type.
     */
    public static class TolerantXml implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return play.core.j.JavaParsers.tolerantXml(maxLength);
        }
    }

    /**
     * Parse the body as text if the Content-Type is text/plain.
     */
    public static class Text implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return play.core.j.JavaParsers.text(maxLength);
        }
    }

    /**
     * Parse the body as text without checking the Content-Type.
     */
    public static class TolerantText implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return play.core.j.JavaParsers.tolerantText(maxLength);
        }
    }

    /**
     * Store the body content in a RawBuffer.
     */
    public static class Raw implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return play.core.j.JavaParsers.raw(maxLength);
        }
    }

    /**
     * Parse the body as form url encoded if the Content-Type is application/x-www-form-urlencoded.
     */
    public static class FormUrlEncoded implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return play.core.j.JavaParsers.formUrlEncoded(maxLength);
        }
    }

    /**
     * Parse the body as form url encoded without checking the Content-Type.
     */
    public static class MultipartFormData implements BodyParser {
        public play.api.mvc.BodyParser<Http.RequestBody> parser(int maxLength) {
            return play.core.j.JavaParsers.multipartFormData(maxLength);
        }
    }

}