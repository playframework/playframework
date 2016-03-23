/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.mvc;

import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.util.ByteString;
import com.fasterxml.jackson.databind.JsonNode;
import org.w3c.dom.Document;
import play.api.http.HttpConfiguration;
import play.api.http.Status$;
import play.api.libs.Files;
import play.api.mvc.MaxSizeNotExceeded$;
import play.api.mvc.MaxSizeStatus;
import play.core.j.JavaParsers;
import play.core.parsers.FormUrlEncodedParser;
import play.http.HttpErrorHandler;
import play.libs.F;
import play.libs.XML;
import play.libs.streams.Accumulator;
import scala.compat.java8.FutureConverters;
import scala.concurrent.Future;

import javax.inject.Inject;
import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * A body parser parses the HTTP request body content.
 */
public interface BodyParser<A> {

    /**
     * Return an accumulator to parse the body of the given HTTP request.
     *
     * The accumulator should either produce a result if an error was encountered, or the parsed body.
     *
     * @param request The request to create the body parser for.
     * @return The accumulator to parse the body.
     */
    Accumulator<ByteString, F.Either<Result, A>> apply(Http.RequestHeader request);

    /**
     * Specify the body parser to use for an Action method.
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Of {

        /**
         * The class of the body parser to use.
         *
         * @return the class
         */
        Class<? extends BodyParser> value();

    }

    /**
     * If PATCH, POST, or PUT, guess the body content by checking the Content-Type header.
     */
    class Default extends AnyContent {
        @Inject
        public Default(HttpErrorHandler errorHandler, HttpConfiguration httpConfiguration) {
            super(errorHandler, httpConfiguration);
        }

        @Override
        public Accumulator<ByteString, F.Either<Result, Object>> apply(Http.RequestHeader request) {
            if (request.method().equals("POST") || request.method().equals("PUT") || request.method().equals("PATCH")) {
                return super.apply(request);
            } else {
                return (Accumulator) new Empty().apply(request);
            }
        }
    }

    /**
     * Guess the body content by checking the Content-Type header.
     */
    class AnyContent implements BodyParser<Object> {
        private final HttpErrorHandler errorHandler;
        private final HttpConfiguration httpConfiguration;

        @Inject
        public AnyContent(HttpErrorHandler errorHandler, HttpConfiguration httpConfiguration) {
            this.errorHandler = errorHandler;
            this.httpConfiguration = httpConfiguration;
        }

        @Override
        public Accumulator<ByteString, F.Either<Result, Object>> apply(Http.RequestHeader request) {
            String contentType = request.contentType().map(ct -> ct.toLowerCase(Locale.ENGLISH)).orElse(null);
            BodyParser parser;
            if (contentType == null) {
                parser = new Raw();
            } else if (contentType.equals("text/plain")) {
                parser = new TolerantText(httpConfiguration, errorHandler);
            } else if (contentType.equals("text/xml") || contentType.equals("application/xml") ||
                    JavaParsers.parse().ApplicationXmlMatcher().pattern().matcher(contentType).matches()) {
                parser = new TolerantXml(httpConfiguration, errorHandler);
            } else if (contentType.equals("text/json") || contentType.equals("application/json")) {
                parser = new TolerantJson(httpConfiguration, errorHandler);
            } else if (contentType.equals("application/x-www-form-urlencoded")) {
                parser = new FormUrlEncoded(httpConfiguration, errorHandler);
            } else if (contentType.equals("multipart/form-data")) {
                parser = new MultipartFormData();
            } else {
                parser = new Raw();
            }
            return parser.apply(request);
        }
    }

    /**
     * Parse the body as Json if the Content-Type is text/json or application/json.
     */
    class Json extends TolerantJson {
        private final HttpErrorHandler errorHandler;

        public Json(long maxLength, HttpErrorHandler errorHandler) {
            super(maxLength, errorHandler);
            this.errorHandler = errorHandler;
        }

        @Inject
        public Json(HttpConfiguration httpConfiguration, HttpErrorHandler errorHandler) {
            super(httpConfiguration, errorHandler);
            this.errorHandler = errorHandler;
        }

        @Override
        public Accumulator<ByteString, F.Either<Result, JsonNode>> apply(Http.RequestHeader request) {
            return BodyParsers.validateContentType(errorHandler, request, "Expected application/json",
                ct -> ct.equalsIgnoreCase("application/json") || ct.equalsIgnoreCase("text/json"),
                super::apply
            );
        }
    }

    /**
     * Parse the body as Json without checking the Content-Type.
     */
    class TolerantJson extends BufferingBodyParser<JsonNode> {
        public TolerantJson(long maxLength, HttpErrorHandler errorHandler) {
            super(maxLength, errorHandler, "Error decoding json body");
        }

        @Inject
        public TolerantJson(HttpConfiguration httpConfiguration, HttpErrorHandler errorHandler) {
            super(httpConfiguration, errorHandler, "Error decoding json body");
        }

        @Override
        protected JsonNode parse(Http.RequestHeader request, ByteString bytes) throws Exception {
            return play.libs.Json.parse(bytes.iterator().asInputStream());
        }
    }

    /**
     * Parse the body as Xml if the Content-Type is application/xml.
     */
    class Xml extends TolerantXml {
        private final HttpErrorHandler errorHandler;

        public Xml(long maxLength, HttpErrorHandler errorHandler) {
            super(maxLength, errorHandler);
            this.errorHandler = errorHandler;
        }

        @Inject
        public Xml(HttpConfiguration httpConfiguration, HttpErrorHandler errorHandler) {
            super(httpConfiguration, errorHandler);
            this.errorHandler = errorHandler;
        }

        @Override
        public Accumulator<ByteString, F.Either<Result, Document>> apply(Http.RequestHeader request) {
            return BodyParsers.validateContentType(errorHandler, request, "Expected XML",
                ct -> ct.startsWith("text/xml") || ct.startsWith("application/xml") ||
                    JavaParsers.parse().ApplicationXmlMatcher().pattern().matcher(ct).matches(),
                super::apply
            );
        }
    }

    /**
     * Parse the body as Xml without checking the Content-Type.
     */
    class TolerantXml extends BufferingBodyParser<Document> {
        public TolerantXml(long maxLength, HttpErrorHandler errorHandler) {
            super(maxLength, errorHandler, "Error decoding xml body");
        }

        @Inject
        public TolerantXml(HttpConfiguration httpConfiguration, HttpErrorHandler errorHandler) {
            super(httpConfiguration, errorHandler, "Error decoding xml body");
        }

        @Override
        protected Document parse(Http.RequestHeader request, ByteString bytes) throws Exception {
            return XML.fromInputStream(bytes.iterator().asInputStream(), request.charset().orElse(null));
        }
    }

    /**
     * Parse the body as text if the Content-Type is text/plain.
     */
    class Text extends TolerantText {
        private final HttpErrorHandler errorHandler;

        public Text(long maxLength, HttpErrorHandler errorHandler) {
            super(maxLength, errorHandler);
            this.errorHandler = errorHandler;
        }

        @Inject
        public Text(HttpConfiguration httpConfiguration, HttpErrorHandler errorHandler) {
            super(httpConfiguration, errorHandler);
            this.errorHandler = errorHandler;
        }

        @Override
        public Accumulator<ByteString, F.Either<Result, String>> apply(Http.RequestHeader request) {
            return BodyParsers.validateContentType(errorHandler, request, "Expected text/plain",
                ct -> ct.equalsIgnoreCase("text/plain"), super::apply
            );
        }
    }

    /**
     * Parse the body as text without checking the Content-Type.
     */
    class TolerantText extends BufferingBodyParser<String> {

        public TolerantText(long maxLength, HttpErrorHandler errorHandler) {
            super(maxLength, errorHandler, "Error decoding text body");
        }

        @Inject
        public TolerantText(HttpConfiguration httpConfiguration, HttpErrorHandler errorHandler) {
            super(httpConfiguration, errorHandler, "Error decoding text body");
        }

        @Override
        protected String parse(Http.RequestHeader request, ByteString bytes) throws Exception {
            String charset = request.charset().orElse("ISO-8859-1");
            return bytes.decodeString(charset);
        }
    }

    /**
     * Parse the body as a byte string.
     */
    class Bytes extends BufferingBodyParser<ByteString> {

        public Bytes(long maxLength, HttpErrorHandler errorHandler) {
            super(maxLength, errorHandler, "Error decoding byte body");
        }

        @Inject
        public Bytes(HttpConfiguration httpConfiguration, HttpErrorHandler errorHandler) {
            super(httpConfiguration, errorHandler, "Error decoding byte body");
        }

        @Override
        protected ByteString parse(Http.RequestHeader request, ByteString bytes) throws Exception {
            return bytes;
        }
    }

    /**
     * Store the body content in a RawBuffer.
     */
    class Raw extends DelegatingBodyParser<Http.RawBuffer, play.api.mvc.RawBuffer> {
        @Inject
        public Raw() {
            super(JavaParsers.parse().raw(), JavaParsers::toJavaRaw);
        }
    }

    /**
     * Parse the body as form url encoded if the Content-Type is application/x-www-form-urlencoded.
     */
    class FormUrlEncoded extends BufferingBodyParser<Map<String, String[]>> {
        private final HttpErrorHandler errorHandler;

        public FormUrlEncoded(long maxLength, HttpErrorHandler errorHandler) {
            super(maxLength, errorHandler, "Error parsing form");
            this.errorHandler = errorHandler;
        }

        @Inject
        public FormUrlEncoded(HttpConfiguration httpConfiguration, HttpErrorHandler errorHandler) {
            super(httpConfiguration, errorHandler, "Error parsing form");
            this.errorHandler = errorHandler;
        }

        @Override
        public Accumulator<ByteString, F.Either<Result, Map<String, String[]>>> apply(Http.RequestHeader request) {
            return BodyParsers.validateContentType(errorHandler, request, "Expected application/x-www-form-urlencoded",
                    ct -> ct.equalsIgnoreCase("application/x-www-form-urlencoded"), super::apply);
        }

        @Override
        protected Map<String, String[]> parse(Http.RequestHeader request, ByteString bytes) throws Exception {
            String charset = request.charset().orElse("UTF-8");
            String urlEncodedString = bytes.decodeString("UTF-8");
            return FormUrlEncodedParser.parseAsJavaArrayValues(urlEncodedString, charset);
        }
    }

    /**
     * Parse the body as multipart form-data without checking the Content-Type.
     */
    class MultipartFormData extends DelegatingBodyParser<Http.MultipartFormData<File>, play.api.mvc.MultipartFormData<Files.TemporaryFile>> {
        @Inject
        public MultipartFormData() {
            super(JavaParsers.parse().multipartFormData(), JavaParsers::toJavaMultipartFormData);
        }
    }

    /**
     * Don't parse the body.
     */
    class Empty implements BodyParser<Optional<Void>> {
        @Override
        public Accumulator<ByteString, F.Either<Result, Optional<Void>>> apply(Http.RequestHeader request) {
            return Accumulator.done(F.Either.Right(Optional.empty()));
        }
    }

    /**
     * Abstract body parser that enforces a maximum length.
     */
    abstract class MaxLengthBodyParser<A> implements BodyParser<A> {
        private final long maxLength;
        private final HttpErrorHandler errorHandler;

        protected MaxLengthBodyParser(long maxLength, HttpErrorHandler errorHandler) {
            this.maxLength = maxLength;
            this.errorHandler = errorHandler;
        }

        @Override
        public Accumulator<ByteString, F.Either<Result, A>> apply(Http.RequestHeader request) {
            Flow<ByteString, ByteString, Future<MaxSizeStatus>> takeUpToFlow = Flow.fromGraph(play.api.mvc.BodyParsers$.MODULE$.takeUpTo(maxLength));
            Sink<ByteString, CompletionStage<F.Either<Result, A>>> result = apply1(request).toSink();

            return Accumulator.fromSink(takeUpToFlow.toMat(result, (statusFuture, resultFuture) ->
               FutureConverters.toJava(statusFuture).thenCompose(status -> {
                  if (status instanceof MaxSizeNotExceeded$) {
                      return resultFuture;
                  } else {
                      return errorHandler.onClientError(request, Status$.MODULE$.REQUEST_ENTITY_TOO_LARGE(), "Request entity too large")
                              .thenApply(F.Either::<Result, A>Left);
                  }
               })
            ));
        }

        /**
         * Implement this method to implement the actual body parser.
         *
         * @param request header for the request to parse
         * @return the accumulator that parses the request
         */
        protected abstract Accumulator<ByteString, F.Either<Result, A>> apply1(Http.RequestHeader request);
    }

    /**
     * A body parser that first buffers
     */
    abstract class BufferingBodyParser<A> extends MaxLengthBodyParser<A> {
        private final HttpErrorHandler errorHandler;
        private final String errorMessage;

        protected BufferingBodyParser(long maxLength, HttpErrorHandler errorHandler, String errorMessage) {
            super(maxLength, errorHandler);
            this.errorHandler = errorHandler;
            this.errorMessage = errorMessage;
        }

        protected BufferingBodyParser(HttpConfiguration httpConfiguration, HttpErrorHandler errorHandler,
                                      String errorMessage) {
            this(httpConfiguration.parser().maxMemoryBuffer(), errorHandler, errorMessage);
        }

        @Override
        protected final Accumulator<ByteString, F.Either<Result, A>> apply1(Http.RequestHeader request) {
            return Accumulator.fromSink(Sink.fold(ByteString.empty(), ByteString::concat)).mapFuture(bytes -> {
                try {
                    return CompletableFuture.completedFuture(F.Either.Right(parse(request, bytes)));
                } catch (Exception e) {
                    return errorHandler.onClientError(request, Status$.MODULE$.BAD_REQUEST(), errorMessage + ": " + e.getMessage())
                            .thenApply(F.Either::<Result, A>Left);
                }
            }, JavaParsers.trampoline());
        }

        /**
         * Parse the body.
         *
         * @param request The request associated with the body.
         * @param bytes The bytes of the body.
         * @return The body.
         * @throws Exception If the body failed to parse.  It is assumed that any exceptions thrown by this method are
         *      the fault of the client, so a 400 bad request error will be returned if this method throws an exception.
         */
        protected abstract A parse(Http.RequestHeader request, ByteString bytes) throws Exception;

    }

    /**
     * A body parser that delegates to a Scala body parser, and uses the supplied function to transform its result to
     * a Java body.
     */
    abstract class DelegatingBodyParser<A, B> implements BodyParser<A> {
        private final play.api.mvc.BodyParser<B> delegate;
        private final Function<B, A> transform;

        public DelegatingBodyParser(play.api.mvc.BodyParser<B> delegate, Function<B, A> transform) {
            this.delegate = delegate;
            this.transform = transform;
        }

        @Override
        public Accumulator<ByteString, F.Either<Result, A>> apply(Http.RequestHeader request) {
            Accumulator<ByteString, scala.util.Either<play.api.mvc.Result, B>> javaAccumulator =
                    delegate.apply(request._underlyingHeader()).asJava();
            return javaAccumulator.map(result -> {
                        if (result.isLeft()) {
                            return F.Either.Left(result.left().get().asJava());
                        } else {
                            return F.Either.Right(transform.apply(result.right().get()));
                        }
                    },
                    JavaParsers.trampoline()
            );
        }
    }

}
