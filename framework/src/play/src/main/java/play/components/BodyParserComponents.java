/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.components;

import play.api.mvc.AnyContent;
import play.api.mvc.PlayBodyParsers;
import play.api.mvc.PlayBodyParsers$;
import play.mvc.BodyParser;

/**
 * Java BodyParser components.
 *
 * @see BodyParser
 */
public interface BodyParserComponents extends HttpErrorHandlerComponents,
        HttpConfigurationComponents,
        AkkaComponents,
        TemporaryFileComponents {

    default PlayBodyParsers scalaBodyParsers() {
        return PlayBodyParsers$.MODULE$.apply(
                tempFileCreator().asScala(),
                scalaHttpErrorHandler(),
                httpConfiguration().parser(),
                materializer()
        );
    }

    default play.api.mvc.BodyParser<AnyContent> defaultScalaBodyParser() {
        return scalaBodyParsers().defaultBodyParser();
    }

    /**
     * @return the default body parser
     *
     * @see BodyParser.Default
     */
    default BodyParser.Default defaultBodyParser() {
        return new BodyParser.Default(
                httpErrorHandler(),
                httpConfiguration(),
                scalaBodyParsers()
        );
    }

    /**
     * @return the body parser for any content
     *
     * @see BodyParser.AnyContent
     */
    default BodyParser.AnyContent anyContentBodyParser() {
        return new BodyParser.AnyContent(
                httpErrorHandler(),
                httpConfiguration(),
                scalaBodyParsers()
        );
    }

    /**
     * @return the json body parser
     *
     * @see BodyParser.Json
     */
    default BodyParser.Json jsonBodyParser() {
        return new BodyParser.Json(
                httpConfiguration(),
                httpErrorHandler()
        );
    }

    /**
     * @return the tolerant json body parser
     *
     * @see BodyParser.TolerantJson
     */
    default BodyParser.TolerantJson tolerantJsonBodyParser() {
        return new BodyParser.TolerantJson(
                httpConfiguration(),
                httpErrorHandler()
        );
    }

    /**
     * @return the xml body parser
     *
     * @see BodyParser.Xml
     */
    default BodyParser.Xml xmlBodyParser() {
        return new BodyParser.Xml(
                httpConfiguration(),
                httpErrorHandler(),
                scalaBodyParsers()
        );
    }

    /**
     * @return the tolerant xml body parser
     *
     * @see BodyParser.TolerantXml
     */
    default BodyParser.TolerantXml tolerantXmlBodyParser() {
        return new BodyParser.TolerantXml(
                httpConfiguration(),
                httpErrorHandler()
        );
    }

    /**
     * @return the text body parser
     *
     * @see BodyParser.Text
     */
    default BodyParser.Text textBodyParser() {
        return new BodyParser.Text(
                httpConfiguration(),
                httpErrorHandler()
        );
    }

    /**
     * @return the tolerant text body parser
     *
     * @see BodyParser.TolerantText
     */
    default BodyParser.TolerantText tolerantTextBodyParser() {
        return new BodyParser.TolerantText(
                httpConfiguration(),
                httpErrorHandler()
        );
    }

    /**
     * @return the bytes body parser
     *
     * @see BodyParser.Bytes
     */
    default BodyParser.Bytes bytesBodyParser() {
        return new BodyParser.Bytes(
                httpConfiguration(),
                httpErrorHandler()
        );
    }

    /**
     * @return the raw body parser
     *
     * @see BodyParser.Raw
     */
    default BodyParser.Raw rawBodyParser() {
        return new BodyParser.Raw(scalaBodyParsers());
    }

    /**
     * @return the body parser for form url encoded
     *
     * @see BodyParser.FormUrlEncoded
     */
    default BodyParser.FormUrlEncoded formUrlEncodedBodyParser() {
        return new BodyParser.FormUrlEncoded(
                httpConfiguration(),
                httpErrorHandler()
        );
    }

    /**
     * @return the multipart form data body parser
     *
     * @see BodyParser.MultipartFormData
     */
    default BodyParser.MultipartFormData multipartFormDataBodyParser() {
        return new BodyParser.MultipartFormData(scalaBodyParsers());
    }

    /**
     * @return the empty body parser
     *
     * @see BodyParser.Empty
     */
    default BodyParser.Empty emptyBodyParser() {
        return new BodyParser.Empty();
    }
}
