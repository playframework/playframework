/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import static java.nio.charset.StandardCharsets.*;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.StreamConverters;
import org.apache.pekko.util.ByteString;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import play.api.http.HttpConfiguration;
import play.api.http.JavaHttpErrorHandlerDelegate;
import play.api.libs.Files;
import play.api.mvc.BodyParserUtils;
import play.api.mvc.MaxSizeNotExceeded$;
import play.api.mvc.MaxSizeStatus;
import play.api.mvc.PlayBodyParsers;
import play.core.j.JavaHttpErrorHandlerAdapter;
import play.core.j.JavaParsers;
import play.core.parsers.FormUrlEncodedParser;
import play.core.parsers.Multipart;
import play.http.HttpErrorHandler;
import play.libs.F;
import play.libs.Scala;
import play.libs.XML;
import play.libs.streams.Accumulator;
import play.mvc.Http.Status;
import scala.concurrent.Future;
import scala.jdk.javaapi.CollectionConverters;
import scala.jdk.javaapi.FutureConverters;
import scala.runtime.AbstractFunction1;

/** A body parser parses the HTTP request body content. */
public interface BodyParser<A> {

  /**
   * Return an accumulator to parse the body of the given HTTP request.
   *
   * <p>The accumulator should either produce a result if an error was encountered, or the parsed
   * body.
   *
   * @param request The request to create the body parser for.
   * @return The accumulator to parse the body.
   */
  Accumulator<ByteString, F.Either<Result, A>> apply(Http.RequestHeader request);

  /** Specify the body parser to use for an Action method. */
  @Target({ElementType.TYPE, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  @interface Of {

    /**
     * The class of the body parser to use.
     *
     * @return the class
     */
    Class<? extends BodyParser<?>> value();
  }

  /** If the request has a body, guess the body content by checking the Content-Type header. */
  class Default extends AnyContent {
    @Inject
    public Default(
        HttpErrorHandler errorHandler,
        HttpConfiguration httpConfiguration,
        PlayBodyParsers parsers) {
      super(errorHandler, httpConfiguration, parsers);
    }

    @Override
    public Accumulator<ByteString, F.Either<Result, Object>> apply(Http.RequestHeader request) {
      if (request.hasBody()) {
        return super.apply(request);
      } else {
        return BodyParser.<Optional<Void>, Object>widen(new Empty()).apply(request);
      }
    }
  }

  /** Guess the body content by checking the Content-Type header. */
  class AnyContent implements BodyParser<Object> {
    private final HttpErrorHandler errorHandler;
    private final HttpConfiguration httpConfiguration;
    private final PlayBodyParsers parsers;

    @Inject
    public AnyContent(
        HttpErrorHandler errorHandler,
        HttpConfiguration httpConfiguration,
        PlayBodyParsers parsers) {
      this.errorHandler = errorHandler;
      this.httpConfiguration = httpConfiguration;
      this.parsers = parsers;
    }

    @Override
    public Accumulator<ByteString, F.Either<Result, Object>> apply(Http.RequestHeader request) {
      String contentType =
          request.contentType().map(ct -> ct.toLowerCase(Locale.ENGLISH)).orElse(null);
      final BodyParser<?> parser;
      if (contentType != null) {
        if (contentType.equals("text/plain")) {
          return new TolerantText(httpConfiguration, errorHandler)
              .apply(request)
              .map(
                  either ->
                      either
                          .right
                          .map(
                              b ->
                                  F.Either.<Result, Object>Right(
                                      b == null || b.isEmpty() ? Optional.empty() : b))
                          .orElseGet(() -> either.left.map(r -> F.Either.Left(r)).get()),
                  JavaParsers.trampoline());
        } else if (contentType.equals("text/xml")
            || contentType.equals("application/xml")
            || parsers.ApplicationXmlMatcher().pattern().matcher(contentType).matches()) {
          return new TolerantXml(httpConfiguration, errorHandler)
              .apply(request)
              .map(
                  either ->
                      either
                          .right
                          .map(
                              b ->
                                  F.Either.<Result, Object>Right(
                                      b == null || !b.hasChildNodes() ? Optional.empty() : b))
                          .orElseGet(() -> either.left.map(r -> F.Either.Left(r)).get()),
                  JavaParsers.trampoline());
        } else if (contentType.equals("text/json") || contentType.equals("application/json")) {
          return new TolerantJson(httpConfiguration, errorHandler)
              .apply(request)
              .map(
                  either ->
                      either
                          .right
                          .map(
                              b ->
                                  F.Either.<Result, Object>Right(
                                      b == null || (b.isEmpty() && b.isMissingNode())
                                          ? Optional.empty()
                                          : b))
                          .orElseGet(() -> either.left.map(r -> F.Either.Left(r)).get()),
                  JavaParsers.trampoline());
        } else if (contentType.equals("application/x-www-form-urlencoded")) {
          return new FormUrlEncoded(httpConfiguration, errorHandler)
              .apply(request)
              .map(
                  either ->
                      either
                          .right
                          .map(
                              b ->
                                  F.Either.<Result, Object>Right(
                                      b == null || b.isEmpty() ? Optional.empty() : b))
                          .orElseGet(() -> either.left.map(r -> F.Either.Left(r)).get()),
                  JavaParsers.trampoline());
        } else if (contentType.equals("multipart/form-data")) {
          return new MultipartFormData(parsers)
              .apply(request)
              .map(
                  either ->
                      either
                          .right
                          .map(
                              b ->
                                  F.Either.<Result, Object>Right(
                                      b == null || b.isEmpty() ? Optional.empty() : b))
                          .orElseGet(() -> either.left.map(r -> F.Either.Left(r)).get()),
                  JavaParsers.trampoline());
        }
      }
      return new Raw(parsers)
          .apply(request)
          .map(
              either ->
                  either
                      .right
                      .map(
                          b ->
                              F.Either.<Result, Object>Right(
                                  b == null || b.size() == 0 ? Optional.empty() : b))
                      .orElseGet(() -> either.left.map(r -> F.Either.Left(r)).get()),
              JavaParsers.trampoline());
    }
  }

  /** Parse the body as Json if the Content-Type is text/json or application/json. */
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
      return BodyParsers.validateContentType(
          errorHandler,
          request,
          "Expected application/json",
          ct -> ct.equalsIgnoreCase("application/json") || ct.equalsIgnoreCase("text/json"),
          super::apply);
    }
  }

  /** Parse the body as Json without checking the Content-Type. */
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
      return play.libs.Json.parse(bytes.asInputStream());
    }
  }

  /** Parse the body as Xml if the Content-Type is application/xml. */
  class Xml extends TolerantXml {
    private final HttpErrorHandler errorHandler;
    private final PlayBodyParsers parsers;

    public Xml(long maxLength, HttpErrorHandler errorHandler, PlayBodyParsers parsers) {
      super(maxLength, errorHandler);
      this.errorHandler = errorHandler;
      this.parsers = parsers;
    }

    @Inject
    public Xml(
        HttpConfiguration httpConfiguration,
        HttpErrorHandler errorHandler,
        PlayBodyParsers parsers) {
      super(httpConfiguration, errorHandler);
      this.errorHandler = errorHandler;
      this.parsers = parsers;
    }

    @Override
    public Accumulator<ByteString, F.Either<Result, Document>> apply(Http.RequestHeader request) {
      return BodyParsers.validateContentType(
          errorHandler,
          request,
          "Expected XML",
          ct ->
              ct.startsWith("text/xml")
                  || ct.startsWith("application/xml")
                  || parsers.ApplicationXmlMatcher().pattern().matcher(ct).matches(),
          super::apply);
    }
  }

  /** Parse the body as Xml without checking the Content-Type. */
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
      return XML.fromInputStream(bytes.asInputStream(), request.charset().orElse(null));
    }
  }

  /** Parse the body as text if the Content-Type is text/plain. */
  class Text extends BufferingBodyParser<String> {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(Text.class);

    private final HttpErrorHandler errorHandler;

    public Text(long maxLength, HttpErrorHandler errorHandler) {
      super(maxLength, errorHandler, "Error decoding text/plain body");
      this.errorHandler = errorHandler;
    }

    @Inject
    public Text(HttpConfiguration httpConfiguration, HttpErrorHandler errorHandler) {
      super(httpConfiguration, errorHandler, "Error decoding text/plain body");
      this.errorHandler = errorHandler;
    }

    @Override
    public Accumulator<ByteString, F.Either<Result, String>> apply(Http.RequestHeader request) {
      return BodyParsers.validateContentType(
          errorHandler,
          request,
          "Expected text/plain",
          ct -> ct.equalsIgnoreCase("text/plain"),
          super::apply);
    }

    @Override
    protected String parse(Http.RequestHeader request, ByteString bytes) throws Exception {
      // Per RFC 7231:
      // The default charset of ISO-8859-1 for text media types has been removed; the default is now
      // whatever the media type definition says.
      // Per RFC 6657:
      // The default "charset" parameter value for "text/plain" is unchanged from [RFC2046] and
      // remains as "US-ASCII".
      // https://tools.ietf.org/html/rfc6657#section-4
      Charset charset = request.charset().map(Charset::forName).orElse(US_ASCII);
      try {
        CharsetDecoder decoder = charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT);
        return decoder.decode(bytes.toByteBuffer()).toString();
      } catch (CharacterCodingException e) {
        String msg =
            String.format(
                "Parser tried to parse request %s as text body with charset %s, but it contains invalid characters!",
                request.id(), charset);
        logger.warn(msg);
        return bytes.decodeString(charset); // parse and return with unmappable characters.
      } catch (Exception e) {
        String msg = "Unexpected exception while parsing text/plain body";
        logger.error(msg, e);
        return bytes.decodeString(charset);
      }
    }
  }

  /** Parse the body as text without checking the Content-Type. */
  class TolerantText extends BufferingBodyParser<String> {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(TolerantText.class);

    public TolerantText(long maxLength, HttpErrorHandler errorHandler) {
      super(maxLength, errorHandler, "Error decoding text body");
    }

    @Inject
    public TolerantText(HttpConfiguration httpConfiguration, HttpErrorHandler errorHandler) {
      super(httpConfiguration, errorHandler, "Error decoding text body");
    }

    @Override
    protected String parse(Http.RequestHeader request, ByteString bytes) throws Exception {
      ByteBuffer byteBuffer = bytes.toByteBuffer();
      final Function<Charset, F.Either<Exception, String>> decode =
          (Charset encodingToTry) -> {
            try {
              // Make sure we are at the beginning of the buffer - previous decoding attempts may
              // have managed to advance through a part of the buffer before failing.
              byteBuffer.rewind();
              CharsetDecoder decoder =
                  encodingToTry.newDecoder().onMalformedInput(CodingErrorAction.REPORT);
              return F.Either.Right(decoder.decode(byteBuffer).toString());
            } catch (CharacterCodingException e) {
              String msg =
                  String.format(
                      "Parser tried to parse request %s as text body with charset %s, but it contains invalid characters!",
                      request.id(), encodingToTry);
              logger.warn(msg);
              return F.Either.Left(e);
            } catch (Exception e) {
              String msg = "Unexpected exception!";
              logger.error(msg, e);
              return F.Either.Left(e);
            }
          };

      // Run through a common set of encoders to get an idea of the best character encoding.

      // Per RFC 7231:
      // The default charset of ISO-8859-1 for text media types has been removed; the default is now
      // whatever the media type definition says.
      // Per RFC 6657:
      // The default "charset" parameter value for "text/plain" is unchanged from [RFC2046] and
      // remains as "US-ASCII".
      // https://tools.ietf.org/html/rfc6657#section-4
      Charset charset = request.charset().map(Charset::forName).orElse(US_ASCII);
      return decode
          .apply(charset)
          .right
          .orElseGet(
              () -> {
                // Fallback to UTF-8 if user supplied charset doesn't work...
                return decode
                    .apply(UTF_8)
                    .right
                    .orElseGet(
                        () -> {
                          // Fallback to ISO_8859_1 if UTF-8 doesn't decode right...
                          return decode
                              .apply(ISO_8859_1)
                              .right
                              .orElseGet(
                                  () -> {
                                    // We can't get a decent charset.
                                    // Parse as given codeset, using ? for any unmappable
                                    // characters.
                                    return bytes.decodeString(charset);
                                  });
                        });
              });
    }
  }

  /** Parse the body as a byte string. */
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

  /** Store the body content in a RawBuffer. */
  class Raw extends DelegatingBodyParser<Http.RawBuffer, play.api.mvc.RawBuffer> {
    @Inject
    public Raw(PlayBodyParsers parsers) {
      super(parsers.raw(), JavaParsers::toJavaRaw);
    }

    public Raw(PlayBodyParsers parsers, long memoryThreshold, long maxLength) {
      super(parsers.raw(memoryThreshold, maxLength), JavaParsers::toJavaRaw);
    }
  }

  class ToFile extends MaxLengthBodyParser<File> {

    private final File to;
    private final Materializer materializer;

    public ToFile(
        File to, long maxLength, HttpErrorHandler errorHandler, Materializer materializer) {
      super(maxLength, errorHandler);
      this.to = to;
      this.materializer = materializer;
    }

    public ToFile(
        File to,
        HttpConfiguration httpConfiguration,
        HttpErrorHandler errorHandler,
        Materializer materializer) {
      this(to, httpConfiguration.parser().maxDiskBuffer(), errorHandler, materializer);
    }

    @Override
    protected Accumulator<ByteString, F.Either<Result, File>> apply1(Http.RequestHeader request) {
      return Accumulator.fromSink(
              StreamConverters.fromOutputStream(
                  () -> java.nio.file.Files.newOutputStream(this.to.toPath())))
          .map(ioResult -> F.Either.Right(this.to), materializer.executionContext());
    }
  }

  class TemporaryFile extends MaxLengthBodyParser<play.libs.Files.TemporaryFile> {

    private final play.libs.Files.TemporaryFileCreator temporaryFileCreator;
    private final Materializer materializer;

    public TemporaryFile(
        long maxLength,
        play.libs.Files.TemporaryFileCreator temporaryFileCreator,
        HttpErrorHandler errorHandler,
        Materializer materializer) {
      super(maxLength, errorHandler);
      this.temporaryFileCreator = temporaryFileCreator;
      this.materializer = materializer;
    }

    @Inject
    public TemporaryFile(
        HttpConfiguration httpConfiguration,
        play.libs.Files.TemporaryFileCreator temporaryFileCreator,
        HttpErrorHandler errorHandler,
        Materializer materializer) {
      this(
          httpConfiguration.parser().maxDiskBuffer(),
          temporaryFileCreator,
          errorHandler,
          materializer);
    }

    @Override
    protected Accumulator<ByteString, F.Either<Result, play.libs.Files.TemporaryFile>> apply1(
        Http.RequestHeader request) {
      if (BodyParserUtils.contentLengthHeaderExceedsMaxLength(request.asScala(), super.maxLength)) {
        // We check early here already to not even create a temporary file
        return Accumulator.done(requestEntityTooLarge(request));
      } else {
        play.libs.Files.TemporaryFile tempFile =
            temporaryFileCreator.create("requestBody", "asTemporaryFile");
        return Accumulator.fromSink(
                StreamConverters.fromOutputStream(
                    () -> java.nio.file.Files.newOutputStream(tempFile.path())))
            .map(ioResult -> F.Either.Right(tempFile), materializer.executionContext());
      }
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
    public Accumulator<ByteString, F.Either<Result, Map<String, String[]>>> apply(
        Http.RequestHeader request) {
      return BodyParsers.validateContentType(
          errorHandler,
          request,
          "Expected application/x-www-form-urlencoded",
          ct -> ct.equalsIgnoreCase("application/x-www-form-urlencoded"),
          super::apply);
    }

    @Override
    protected Map<String, String[]> parse(Http.RequestHeader request, ByteString bytes)
        throws Exception {
      String charset = request.charset().orElse("UTF-8");
      String urlEncodedString = bytes.decodeString("UTF-8");
      return FormUrlEncodedParser.parseAsJavaArrayValues(urlEncodedString, charset);
    }
  }

  /** Parse the body as multipart form-data without checking the Content-Type. */
  class MultipartFormData
      extends DelegatingBodyParser<
          Http.MultipartFormData<play.libs.Files.TemporaryFile>,
          play.api.mvc.MultipartFormData<Files.TemporaryFile>> {
    @Inject
    public MultipartFormData(PlayBodyParsers parsers) {
      super(parsers.multipartFormData(), JavaParsers::toJavaMultipartFormData);
    }

    public MultipartFormData(PlayBodyParsers parsers, boolean allowEmptyFiles) {
      super(parsers.multipartFormData(allowEmptyFiles), JavaParsers::toJavaMultipartFormData);
    }

    public MultipartFormData(PlayBodyParsers parsers, long maxLength) {
      super(parsers.multipartFormData(maxLength), JavaParsers::toJavaMultipartFormData);
    }

    public MultipartFormData(PlayBodyParsers parsers, long maxLength, boolean allowEmptyFiles) {
      super(
          parsers.multipartFormData(maxLength, allowEmptyFiles),
          JavaParsers::toJavaMultipartFormData);
    }
  }

  /** Don't parse the body. */
  class Empty implements BodyParser<Optional<Void>> {
    @Override
    public Accumulator<ByteString, F.Either<Result, Optional<Void>>> apply(
        Http.RequestHeader request) {
      return Accumulator.done(F.Either.Right(Optional.empty()));
    }
  }

  /** Abstract body parser that enforces a maximum length. */
  abstract class MaxLengthBodyParser<A> implements BodyParser<A> {
    private final long maxLength;
    private final HttpErrorHandler errorHandler;

    protected MaxLengthBodyParser(long maxLength, HttpErrorHandler errorHandler) {
      this.maxLength = maxLength;
      this.errorHandler = errorHandler;
    }

    CompletionStage<F.Either<Result, A>> requestEntityTooLarge(Http.RequestHeader request) {
      return errorHandler
          .onClientError(request, Status.REQUEST_ENTITY_TOO_LARGE, "Request entity too large")
          .thenApply(F.Either::Left);
    }

    @Override
    public Accumulator<ByteString, F.Either<Result, A>> apply(Http.RequestHeader request) {
      Flow<ByteString, ByteString, Future<MaxSizeStatus>> takeUpToFlow =
          Flow.fromGraph(play.api.mvc.BodyParsers$.MODULE$.takeUpTo(maxLength));
      if (BodyParserUtils.contentLengthHeaderExceedsMaxLength(request.asScala(), maxLength)) {
        return Accumulator.done(requestEntityTooLarge(request));
      } else {
        Sink<ByteString, CompletionStage<F.Either<Result, A>>> result = apply1(request).toSink();
        return Accumulator.fromSink(
            takeUpToFlow.toMat(
                result,
                (statusFuture, resultFuture) ->
                    FutureConverters.asJava(statusFuture)
                        .thenCompose(
                            status -> {
                              if (status instanceof MaxSizeNotExceeded$) {
                                return resultFuture;
                              } else {
                                return requestEntityTooLarge(request);
                              }
                            })));
      }
    }

    /**
     * Implement this method to implement the actual body parser.
     *
     * @param request header for the request to parse
     * @return the accumulator that parses the request
     */
    protected abstract Accumulator<ByteString, F.Either<Result, A>> apply1(
        Http.RequestHeader request);
  }

  /** A body parser that first buffers */
  abstract class BufferingBodyParser<A> extends MaxLengthBodyParser<A> {
    private final HttpErrorHandler errorHandler;
    private final String errorMessage;

    protected BufferingBodyParser(
        long maxLength, HttpErrorHandler errorHandler, String errorMessage) {
      super(maxLength, errorHandler);
      this.errorHandler = errorHandler;
      this.errorMessage = errorMessage;
    }

    protected BufferingBodyParser(
        HttpConfiguration httpConfiguration, HttpErrorHandler errorHandler, String errorMessage) {
      this(httpConfiguration.parser().maxMemoryBuffer(), errorHandler, errorMessage);
    }

    @Override
    protected final Accumulator<ByteString, F.Either<Result, A>> apply1(
        Http.RequestHeader request) {
      Accumulator<ByteString, ByteString> byteStringByteStringAccumulator =
          Accumulator.strict(
              maybeStrictBytes ->
                  CompletableFuture.completedFuture(
                      maybeStrictBytes.orElse(ByteString.emptyByteString())),
              Sink.fold(ByteString.emptyByteString(), ByteString::concat));
      Accumulator<ByteString, F.Either<Result, A>> byteStringEitherAccumulator =
          byteStringByteStringAccumulator.mapFuture(
              bytes -> {
                try {
                  return CompletableFuture.completedFuture(F.Either.Right(parse(request, bytes)));
                } catch (Exception e) {
                  return errorHandler
                      .onClientError(
                          request, Status.BAD_REQUEST, errorMessage + ": " + e.getMessage())
                      .thenApply(F.Either::<Result, A>Left);
                }
              },
              JavaParsers.trampoline());
      return byteStringEitherAccumulator;
    }

    /**
     * Parse the body.
     *
     * @param request The request associated with the body.
     * @param bytes The bytes of the body.
     * @return The body.
     * @throws Exception If the body failed to parse. It is assumed that any exceptions thrown by
     *     this method are the fault of the client, so a 400 bad request error will be returned if
     *     this method throws an exception.
     */
    protected abstract A parse(Http.RequestHeader request, ByteString bytes) throws Exception;
  }

  /**
   * A body parser that delegates to a Scala body parser, and uses the supplied function to
   * transform its result to a Java body.
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
      return BodyParsers.delegate(delegate, transform, request);
    }
  }

  /** A body parser that completes the underlying one. */
  abstract class CompletableBodyParser<A> implements BodyParser<A> {
    private final CompletionStage<BodyParser<A>> underlying;
    private final Materializer materializer;

    public CompletableBodyParser(
        CompletionStage<BodyParser<A>> underlying, Materializer materializer) {

      this.underlying = underlying;
      this.materializer = materializer;
    }

    @Override
    public Accumulator<ByteString, F.Either<Result, A>> apply(Http.RequestHeader request) {
      CompletionStage<Accumulator<ByteString, F.Either<Result, A>>> completion =
          underlying.thenApply(parser -> parser.apply(request));

      return Accumulator.flatten(completion, this.materializer);
    }
  }

  /**
   * A body parser that exposes a file part handler as an abstract method and delegates the
   * implementation to the underlying Scala multipartParser.
   */
  abstract class DelegatingMultipartFormDataBodyParser<A>
      extends MaxLengthBodyParser<Http.MultipartFormData<A>> {

    private final Materializer materializer;
    private final long maxMemoryBufferSize;
    private final play.api.mvc.BodyParser<play.api.mvc.MultipartFormData<A>> delegate;
    private final play.api.http.HttpErrorHandler errorHandler;

    /**
     * @deprecated Deprecated as of 2.8.0. Use {@link
     *     #DelegatingMultipartFormDataBodyParser(Materializer, long, long, HttpErrorHandler)}
     *     instead.
     */
    @Deprecated
    public DelegatingMultipartFormDataBodyParser(
        Materializer materializer, long maxLength, play.api.http.HttpErrorHandler errorHandler) {
      super(maxLength, new JavaHttpErrorHandlerDelegate(errorHandler));
      this.materializer = materializer;
      this.errorHandler = errorHandler;
      this.maxMemoryBufferSize = 102400; // 100k, default for play.http.parser.maxMemoryBuffer
      delegate = multipartParser(false);
    }

    /**
     * @deprecated Deprecated as of 2.9.0. Use {@link
     *     #DelegatingMultipartFormDataBodyParser(Materializer, long, long, boolean,
     *     HttpErrorHandler)} instead.
     */
    @Deprecated
    public DelegatingMultipartFormDataBodyParser(
        Materializer materializer,
        long maxMemoryBufferSize,
        long maxLength,
        HttpErrorHandler errorHandler) {
      this(materializer, maxMemoryBufferSize, maxLength, false, errorHandler);
    }

    public DelegatingMultipartFormDataBodyParser(
        Materializer materializer,
        long maxMemoryBufferSize,
        long maxLength,
        boolean allowEmptyFiles,
        HttpErrorHandler errorHandler) {
      super(maxLength, errorHandler);
      this.materializer = materializer;
      this.maxMemoryBufferSize = maxMemoryBufferSize;
      this.errorHandler = new JavaHttpErrorHandlerAdapter(errorHandler);
      delegate = multipartParser(allowEmptyFiles);
    }

    /**
     * Returns a FilePartHandler expressed as a Java function.
     *
     * @return a file part handler function.
     */
    public abstract Function<
            Multipart.FileInfo,
            play.libs.streams.Accumulator<ByteString, Http.MultipartFormData.FilePart<A>>>
        createFilePartHandler();

    /** Calls out to the Scala API to create a multipart parser. */
    private play.api.mvc.BodyParser<play.api.mvc.MultipartFormData<A>> multipartParser(
        boolean allowEmptyFiles) {
      ScalaFilePartHandler filePartHandler = new ScalaFilePartHandler();
      return Multipart.multipartParser(
          maxMemoryBufferSize, allowEmptyFiles, filePartHandler, errorHandler, materializer);
    }

    private class ScalaFilePartHandler
        extends AbstractFunction1<
            Multipart.FileInfo,
            play.api.libs.streams.Accumulator<
                ByteString, play.api.mvc.MultipartFormData.FilePart<A>>> {
      @Override
      public play.api.libs.streams.Accumulator<
              ByteString, play.api.mvc.MultipartFormData.FilePart<A>>
          apply(Multipart.FileInfo fileInfo) {
        return createFilePartHandler()
            .apply(fileInfo)
            .asScala()
            .map(new JavaFilePartToScalaFilePart(), materializer.executionContext());
      }
    }

    private class JavaFilePartToScalaFilePart
        extends AbstractFunction1<
            Http.MultipartFormData.FilePart<A>, play.api.mvc.MultipartFormData.FilePart<A>> {
      @Override
      public play.api.mvc.MultipartFormData.FilePart<A> apply(
          Http.MultipartFormData.FilePart<A> filePart) {
        return filePart.asScala();
      }
    }

    /**
     * Delegates underlying functionality to another body parser and converts the result to Java
     * API.
     */
    @Override
    public play.libs.streams.Accumulator<ByteString, F.Either<Result, Http.MultipartFormData<A>>>
        apply1(Http.RequestHeader request) {
      return delegate
          .apply(request.asScala())
          .asJava()
          .map(
              result -> {
                if (result.isLeft()) {
                  return F.Either.Left(result.swap().toOption().get().asJava());
                } else {
                  final play.api.mvc.MultipartFormData<A> scalaData = result.toOption().get();
                  return F.Either.Right(new DelegatingMultipartFormData(scalaData));
                }
              },
              JavaParsers.trampoline());
    }

    /**
     * Extends Http.MultipartFormData to use File specifically, converting from Scala API to Java
     * API.
     */
    private class DelegatingMultipartFormData extends Http.MultipartFormData<A> {
      private final play.api.mvc.MultipartFormData<A> scalaFormData;

      DelegatingMultipartFormData(play.api.mvc.MultipartFormData<A> scalaFormData) {
        this.scalaFormData = scalaFormData;
      }

      @Override
      public Map<String, String[]> asFormUrlEncoded() {
        // TODO have this transformations in Scala is easier.
        return CollectionConverters.asJava(scalaFormData.asFormUrlEncoded()).entrySet().stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey, entry -> Scala.asArray(String.class, entry.getValue())));
      }

      @Override
      public List<FilePart<A>> getFiles() {
        return CollectionConverters.asJava(scalaFormData.files()).stream()
            .map(play.api.mvc.MultipartFormData.FilePart::asJava)
            .collect(Collectors.toList());
      }

      @Override
      public boolean isEmpty() {
        return scalaFormData.isEmpty();
      }
    }
  }

  @SuppressWarnings("unchecked")
  // covariance: BodyParser<?> <: BodyParser<Object>, given BodyParser<A> is covariant in A
  static <A extends B, B> BodyParser<B> widen(final BodyParser<A> parser) {
    return (BodyParser<B>) parser;
  }
}
