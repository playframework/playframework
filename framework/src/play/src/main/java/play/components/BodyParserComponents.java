/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
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

    default PlayBodyParsers scalaParsers() {
        return PlayBodyParsers$.MODULE$.apply(
                httpConfiguration().parser(),
                scalaHttpErrorHandler(),
                materializer(),
                tempFileCreator().asScala()
        );
    }

    default play.api.mvc.BodyParser<AnyContent> defaultScalaParser() {
        return scalaParsers().defaultBodyParser();
    }

    /**
     * @return the default body parser
     *
     * @see BodyParser.Default
     */
    default BodyParser.Default defaultParser() {
        return new BodyParser.Default(
                httpErrorHandler(),
                httpConfiguration(),
                scalaParsers()
        );
    }

    /**
     * @return the body parser for any content
     *
     * @see BodyParser.AnyContent
     */
    default BodyParser.AnyContent anyContentParser() {
        return new BodyParser.AnyContent(
                httpErrorHandler(),
                httpConfiguration(),
                scalaParsers()
        );
    }

    /**
     * @return the json body parser
     *
     * @see BodyParser.Json
     */
    default BodyParser.Json jsonParser() {
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
    default BodyParser.TolerantJson tolerantJsonParser() {
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
    default BodyParser.Xml xmlParser() {
        return new BodyParser.Xml(
                httpConfiguration(),
                httpErrorHandler(),
                scalaParsers()
        );
    }

    /**
     * @return the tolerant xml body parser
     *
     * @see BodyParser.TolerantXml
     */
    default BodyParser.TolerantXml tolerantXmlParser() {
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
    default BodyParser.Text textParser() {
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
    default BodyParser.TolerantText tolerantTextParser() {
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
    default BodyParser.Bytes bytesParser() {
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
    default BodyParser.Raw rawParser() {
        return new BodyParser.Raw(scalaParsers());
    }

    /**
     * @return the body parser for form url encoded
     *
     * @see BodyParser.FormUrlEncoded
     */
    default BodyParser.FormUrlEncoded formUrlEncodedParser() {
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
    default BodyParser.MultipartFormData multipartFormDataParser() {
        return new BodyParser.MultipartFormData(scalaParsers());
    }

    /**
     * @return the empty body parser
     *
     * @see BodyParser.Empty
     */
    default BodyParser.Empty emptyParser() {
        return new BodyParser.Empty();
    }
}
