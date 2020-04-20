/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import akka.stream.IOResult;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.StreamConverters;
import akka.util.ByteString;
import akka.util.ByteString$;
import akka.util.ByteStringBuilder;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import play.http.HttpEntity;
import play.libs.Json;
import play.mvc.Http.MimeTypes;

/** A status with no body */
public class StatusHeader extends Result {

  private static final int DEFAULT_CHUNK_SIZE = 1024 * 8;
  private static final boolean DEFAULT_INLINE_MODE = true;

  public StatusHeader(int status) {
    super(status);
  }

  /**
   * Send the given input stream.
   *
   * <p>The input stream will be sent chunked since there is no specified content length.
   *
   * @param stream The input stream to send.
   * @return The result.
   */
  public Result sendInputStream(InputStream stream) {
    return sendInputStream(stream, () -> {}, null);
  }

  /**
   * Send the given input stream.
   *
   * <p>The input stream will be sent chunked since there is no specified content length.
   *
   * @param stream The input stream to send.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return The result.
   */
  public Result sendInputStream(InputStream stream, Runnable onClose, Executor executor) {
    return sendInputStream(stream, Optional.empty(), onClose, executor);
  }

  /**
   * Send the given input stream.
   *
   * <p>The input stream will be sent chunked since there is no specified content length.
   *
   * @param stream The input stream to send.
   * @param contentType the entity content type.
   * @return The result.
   */
  public Result sendInputStream(InputStream stream, Optional<String> contentType) {
    return sendInputStream(stream, contentType, () -> {}, null);
  }

  /**
   * Send the given input stream.
   *
   * <p>The input stream will be sent chunked since there is no specified content length.
   *
   * @param stream The input stream to send.
   * @param contentType the entity content type.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return The result.
   */
  public Result sendInputStream(
      InputStream stream, Optional<String> contentType, Runnable onClose, Executor executor) {
    if (stream == null) {
      throw new NullPointerException("Null stream");
    }
    return new Result(
        status(),
        HttpEntity.chunked(
            attachOnClose(
                StreamConverters.fromInputStream(() -> stream, DEFAULT_CHUNK_SIZE),
                onClose,
                executor),
            contentType));
  }

  /**
   * Send the given input stream.
   *
   * @param stream The input stream to send.
   * @param contentLength The length of the content in the stream.
   * @return The result.
   */
  public Result sendInputStream(InputStream stream, long contentLength) {
    return sendInputStream(stream, contentLength, () -> {}, null);
  }

  /**
   * Send the given input stream.
   *
   * @param stream The input stream to send.
   * @param contentLength The length of the content in the stream.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return The result.
   */
  public Result sendInputStream(
      InputStream stream, long contentLength, Runnable onClose, Executor executor) {
    return sendInputStream(stream, contentLength, Optional.empty(), onClose, executor);
  }

  /**
   * Send the given input stream.
   *
   * @param stream The input stream to send.
   * @param contentLength The length of the content in the stream.
   * @param contentType the entity content type.
   * @return The result.
   */
  public Result sendInputStream(
      InputStream stream, long contentLength, Optional<String> contentType) {
    return sendInputStream(stream, contentLength, contentType, () -> {}, null);
  }

  /**
   * Send the given input stream.
   *
   * @param stream The input stream to send.
   * @param contentLength The length of the content in the stream.
   * @param contentType the entity content type.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return The result.
   */
  public Result sendInputStream(
      InputStream stream,
      long contentLength,
      Optional<String> contentType,
      Runnable onClose,
      Executor executor) {
    if (stream == null) {
      throw new NullPointerException("Null stream");
    }
    return new Result(
        status(),
        new HttpEntity.Streamed(
            attachOnClose(
                StreamConverters.fromInputStream(() -> stream, DEFAULT_CHUNK_SIZE),
                onClose,
                executor),
            Optional.of(contentLength),
            contentType));
  }

  /**
   * Send the given bytes.
   *
   * @param content The bytes to send.
   * @return The result.
   */
  public Result sendBytes(byte[] content) {
    return sendBytes(content, Optional.empty());
  }

  /**
   * Send the given bytes.
   *
   * @param content The bytes to send.
   * @param contentType the entity content type.
   * @return The result.
   */
  public Result sendBytes(byte[] content, Optional<String> contentType) {
    return new Result(status(), new HttpEntity.Strict(ByteString.fromArray(content), contentType));
  }

  /**
   * Send the given bytes.
   *
   * @param content The bytes to send.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name or fallback to {@code application/octet-stream} if unknown.
   * @return The result.
   */
  public Result sendBytes(byte[] content, boolean inline, Optional<String> fileName) {
    return sendBytes(content, inline, fileName, StaticFileMimeTypes.fileMimeTypes());
  }

  /**
   * Send the given bytes.
   *
   * @param content The bytes to send.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name if {@code fileMimeTypes} includes it or fallback to {@code
   *     application/octet-stream} if unknown.
   * @param fileMimeTypes Used for file type mapping.
   * @return The result.
   */
  public Result sendBytes(
      byte[] content, boolean inline, Optional<String> fileName, FileMimeTypes fileMimeTypes) {
    return new Result(
        status(),
        Results.contentDispositionHeader(inline, fileName),
        new HttpEntity.Strict(
            ByteString.fromArray(content),
            fileName.map(name -> fileMimeTypes.forFileName(name).orElse(Http.MimeTypes.BINARY))));
  }

  /**
   * Send the given ByteString.
   *
   * @param content The ByteString to send.
   * @return The result.
   */
  public Result sendByteString(ByteString content) {
    return sendByteString(content, Optional.empty());
  }

  /**
   * Send the given ByteString.
   *
   * @param content The ByteString to send.
   * @param contentType the entity content type.
   * @return The result.
   */
  public Result sendByteString(ByteString content, Optional<String> contentType) {
    return new Result(status(), new HttpEntity.Strict(content, contentType));
  }

  /**
   * Send the given ByteString.
   *
   * @param content The ByteString to send.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name or fallback to {@code application/octet-stream} if unknown.
   * @return The result.
   */
  public Result sendByteString(ByteString content, boolean inline, Optional<String> fileName) {
    return sendByteString(content, inline, fileName, StaticFileMimeTypes.fileMimeTypes());
  }

  /**
   * Send the given ByteString.
   *
   * @param content The ByteString to send.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name if {@code fileMimeTypes} includes it or fallback to {@code
   *     application/octet-stream} if unknown.
   * @param fileMimeTypes Used for file type mapping.
   * @return The result.
   */
  public Result sendByteString(
      ByteString content, boolean inline, Optional<String> fileName, FileMimeTypes fileMimeTypes) {
    return new Result(
        status(),
        Results.contentDispositionHeader(inline, fileName),
        new HttpEntity.Strict(
            content,
            fileName.map(name -> fileMimeTypes.forFileName(name).orElse(Http.MimeTypes.BINARY))));
  }

  /**
   * Send the given resource.
   *
   * <p>The resource will be loaded from the same classloader that this class comes from.
   *
   * @param resourceName The path of the resource to load.
   * @return a '200 OK' result containing the resource in the body with in-line content disposition.
   */
  public Result sendResource(String resourceName) {
    return sendResource(resourceName, () -> {}, null);
  }

  /**
   * Send the given resource.
   *
   * <p>The resource will be loaded from the same classloader that this class comes from.
   *
   * @param resourceName The path of the resource to load.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the resource in the body with in-line content disposition.
   */
  public Result sendResource(String resourceName, Runnable onClose, Executor executor) {
    return sendResource(resourceName, StaticFileMimeTypes.fileMimeTypes(), onClose, executor);
  }

  /**
   * Send the given resource.
   *
   * <p>The resource will be loaded from the same classloader that this class comes from.
   *
   * @param resourceName The path of the resource to load.
   * @param fileMimeTypes Used for file type mapping.
   * @return a '200 OK' result containing the resource in the body with in-line content disposition.
   */
  public Result sendResource(String resourceName, FileMimeTypes fileMimeTypes) {
    return sendResource(resourceName, fileMimeTypes, () -> {}, null);
  }

  /**
   * Send the given resource.
   *
   * <p>The resource will be loaded from the same classloader that this class comes from.
   *
   * @param resourceName The path of the resource to load.
   * @param fileMimeTypes Used for file type mapping.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the resource in the body with in-line content disposition.
   */
  public Result sendResource(
      String resourceName, FileMimeTypes fileMimeTypes, Runnable onClose, Executor executor) {
    return sendResource(resourceName, DEFAULT_INLINE_MODE, fileMimeTypes, onClose, executor);
  }

  /**
   * Send the given resource from the given classloader.
   *
   * @param resourceName The path of the resource to load.
   * @param classLoader The classloader to load it from.
   * @return a '200 OK' result containing the resource in the body with in-line content disposition.
   */
  public Result sendResource(String resourceName, ClassLoader classLoader) {
    return sendResource(resourceName, classLoader, () -> {}, null);
  }

  /**
   * Send the given resource from the given classloader.
   *
   * @param resourceName The path of the resource to load.
   * @param classLoader The classloader to load it from.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the resource in the body with in-line content disposition.
   */
  public Result sendResource(
      String resourceName, ClassLoader classLoader, Runnable onClose, Executor executor) {
    return sendResource(
        resourceName, classLoader, StaticFileMimeTypes.fileMimeTypes(), onClose, executor);
  }

  /**
   * Send the given resource from the given classloader.
   *
   * @param resourceName The path of the resource to load.
   * @param classLoader The classloader to load it from.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name or fallback to {@code application/octet-stream} if unknown.
   * @return a '200 OK' result containing the resource in the body with in-line content disposition.
   */
  public Result sendResource(
      String resourceName, ClassLoader classLoader, Optional<String> fileName) {
    return sendResource(resourceName, classLoader, fileName, () -> {}, null);
  }

  /**
   * Send the given resource from the given classloader.
   *
   * @param resourceName The path of the resource to load.
   * @param classLoader The classloader to load it from.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name or fallback to {@code application/octet-stream} if unknown.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the resource in the body with in-line content disposition.
   */
  public Result sendResource(
      String resourceName,
      ClassLoader classLoader,
      Optional<String> fileName,
      Runnable onClose,
      Executor executor) {
    return sendResource(
        resourceName,
        classLoader,
        fileName,
        StaticFileMimeTypes.fileMimeTypes(),
        onClose,
        executor);
  }

  /**
   * Send the given resource from the given classloader.
   *
   * @param resourceName The path of the resource to load.
   * @param classLoader The classloader to load it from.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name if {@code fileMimeTypes} includes it or fallback to {@code
   *     application/octet-stream} if unknown.
   * @param fileMimeTypes Used for file type mapping.
   * @return a '200 OK' result containing the resource in the body with in-line content disposition.
   */
  public Result sendResource(
      String resourceName,
      ClassLoader classLoader,
      Optional<String> fileName,
      FileMimeTypes fileMimeTypes) {
    return sendResource(resourceName, classLoader, fileName, fileMimeTypes, () -> {}, null);
  }

  /**
   * Send the given resource from the given classloader.
   *
   * @param resourceName The path of the resource to load.
   * @param classLoader The classloader to load it from.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name if {@code fileMimeTypes} includes it or fallback to {@code
   *     application/octet-stream} if unknown.
   * @param fileMimeTypes Used for file type mapping.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the resource in the body with in-line content disposition.
   */
  public Result sendResource(
      String resourceName,
      ClassLoader classLoader,
      Optional<String> fileName,
      FileMimeTypes fileMimeTypes,
      Runnable onClose,
      Executor executor) {
    return sendResource(
        resourceName, classLoader, DEFAULT_INLINE_MODE, fileName, fileMimeTypes, onClose, executor);
  }

  /**
   * Send the given resource from the given classloader.
   *
   * @param resourceName The path of the resource to load.
   * @param classLoader The classloader to load it from.
   * @param fileMimeTypes Used for file type mapping.
   * @return a '200 OK' result containing the resource in the body with in-line content disposition.
   */
  public Result sendResource(
      String resourceName, ClassLoader classLoader, FileMimeTypes fileMimeTypes) {
    return sendResource(resourceName, classLoader, fileMimeTypes, () -> {}, null);
  }

  /**
   * Send the given resource from the given classloader.
   *
   * @param resourceName The path of the resource to load.
   * @param classLoader The classloader to load it from.
   * @param fileMimeTypes Used for file type mapping.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the resource in the body with in-line content disposition.
   */
  public Result sendResource(
      String resourceName,
      ClassLoader classLoader,
      FileMimeTypes fileMimeTypes,
      Runnable onClose,
      Executor executor) {
    return sendResource(
        resourceName, classLoader, DEFAULT_INLINE_MODE, fileMimeTypes, onClose, executor);
  }

  /**
   * Send the given resource.
   *
   * <p>The resource will be loaded from the same classloader that this class comes from.
   *
   * @param resourceName The path of the resource to load.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name or fallback to {@code application/octet-stream} if unknown.
   * @return a '200 OK' result containing the resource in the body with in-line content disposition.
   */
  public Result sendResource(String resourceName, Optional<String> fileName) {
    return sendResource(resourceName, fileName, () -> {}, null);
  }

  /**
   * Send the given resource.
   *
   * <p>The resource will be loaded from the same classloader that this class comes from.
   *
   * @param resourceName The path of the resource to load.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name or fallback to {@code application/octet-stream} if unknown.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the resource in the body with in-line content disposition.
   */
  public Result sendResource(
      String resourceName, Optional<String> fileName, Runnable onClose, Executor executor) {
    return sendResource(
        resourceName, fileName, StaticFileMimeTypes.fileMimeTypes(), onClose, executor);
  }

  /**
   * Send the given resource.
   *
   * <p>The resource will be loaded from the same classloader that this class comes from.
   *
   * @param resourceName The path of the resource to load.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name if {@code fileMimeTypes} includes it or fallback to {@code
   *     application/octet-stream} if unknown.
   * @param fileMimeTypes Used for file type mapping.
   * @return a '200 OK' result containing the resource in the body with in-line content disposition.
   */
  public Result sendResource(
      String resourceName, Optional<String> fileName, FileMimeTypes fileMimeTypes) {
    return sendResource(resourceName, fileName, fileMimeTypes, () -> {}, null);
  }

  /**
   * Send the given resource.
   *
   * <p>The resource will be loaded from the same classloader that this class comes from.
   *
   * @param resourceName The path of the resource to load.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name if {@code fileMimeTypes} includes it or fallback to {@code
   *     application/octet-stream} if unknown.
   * @param fileMimeTypes Used for file type mapping.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the resource in the body with in-line content disposition.
   */
  public Result sendResource(
      String resourceName,
      Optional<String> fileName,
      FileMimeTypes fileMimeTypes,
      Runnable onClose,
      Executor executor) {
    return sendResource(
        resourceName, DEFAULT_INLINE_MODE, fileName, fileMimeTypes, onClose, executor);
  }

  /**
   * Send the given resource.
   *
   * <p>The resource will be loaded from the same classloader that this class comes from.
   *
   * @param resourceName The path of the resource to load.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @return a '200 OK' result containing the resource in the body with in-line content disposition.
   */
  public Result sendResource(String resourceName, boolean inline) {
    return sendResource(resourceName, inline, () -> {}, null);
  }

  /**
   * Send the given resource.
   *
   * <p>The resource will be loaded from the same classloader that this class comes from.
   *
   * @param resourceName The path of the resource to load.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the resource in the body with in-line content disposition.
   */
  public Result sendResource(
      String resourceName, boolean inline, Runnable onClose, Executor executor) {
    return sendResource(
        resourceName, inline, StaticFileMimeTypes.fileMimeTypes(), onClose, executor);
  }

  /**
   * Send the given resource.
   *
   * <p>The resource will be loaded from the same classloader that this class comes from.
   *
   * @param resourceName The path of the resource to load.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param fileMimeTypes Used for file type mapping.
   * @return a '200 OK' result containing the resource in the body with in-line content disposition.
   */
  public Result sendResource(String resourceName, boolean inline, FileMimeTypes fileMimeTypes) {
    return sendResource(resourceName, inline, fileMimeTypes, () -> {}, null);
  }

  /**
   * Send the given resource.
   *
   * <p>The resource will be loaded from the same classloader that this class comes from.
   *
   * @param resourceName The path of the resource to load.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param fileMimeTypes Used for file type mapping.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the resource in the body with in-line content disposition.
   */
  public Result sendResource(
      String resourceName,
      boolean inline,
      FileMimeTypes fileMimeTypes,
      Runnable onClose,
      Executor executor) {
    return sendResource(
        resourceName, this.getClass().getClassLoader(), inline, fileMimeTypes, onClose, executor);
  }

  /**
   * Send the given resource from the given classloader.
   *
   * @param resourceName The path of the resource to load.
   * @param classLoader The classloader to load it from.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @return a '200 OK' result containing the resource in the body.
   */
  public Result sendResource(String resourceName, ClassLoader classLoader, boolean inline) {
    return sendResource(resourceName, classLoader, inline, () -> {}, null);
  }

  /**
   * Send the given resource from the given classloader.
   *
   * @param resourceName The path of the resource to load.
   * @param classLoader The classloader to load it from.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the resource in the body.
   */
  public Result sendResource(
      String resourceName,
      ClassLoader classLoader,
      boolean inline,
      Runnable onClose,
      Executor executor) {
    return sendResource(
        resourceName, classLoader, inline, StaticFileMimeTypes.fileMimeTypes(), onClose, executor);
  }

  /**
   * Send the given resource from the given classloader.
   *
   * @param resourceName The path of the resource to load.
   * @param classLoader The classloader to load it from.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param fileMimeTypes Used for file type mapping.
   * @return a '200 OK' result containing the resource in the body.
   */
  public Result sendResource(
      String resourceName, ClassLoader classLoader, boolean inline, FileMimeTypes fileMimeTypes) {
    return sendResource(resourceName, classLoader, inline, fileMimeTypes, () -> {}, null);
  }

  /**
   * Send the given resource from the given classloader.
   *
   * @param resourceName The path of the resource to load.
   * @param classLoader The classloader to load it from.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param fileMimeTypes Used for file type mapping.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the resource in the body.
   */
  public Result sendResource(
      String resourceName,
      ClassLoader classLoader,
      boolean inline,
      FileMimeTypes fileMimeTypes,
      Runnable onClose,
      Executor executor) {
    return sendResource(
        resourceName,
        classLoader,
        inline,
        Optional.ofNullable(resourceName),
        fileMimeTypes,
        onClose,
        executor);
  }

  /**
   * Send the given resource.
   *
   * <p>The resource will be loaded from the same classloader that this class comes from.
   *
   * @param resourceName The path of the resource to load.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param filename The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name or fallback to {@code application/octet-stream} if unknown.
   * @return a '200 OK' result containing the resource in the body.
   * @deprecated Deprecated as of 2.8.0. Use {@link #sendResource(String,boolean,Optional)}.
   */
  @Deprecated
  public Result sendResource(String resourceName, boolean inline, String filename) {
    return sendResource(resourceName, inline, Optional.ofNullable(filename));
  }

  /**
   * Send the given resource.
   *
   * <p>The resource will be loaded from the same classloader that this class comes from.
   *
   * @param resourceName The path of the resource to load.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param filename The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name or fallback to {@code application/octet-stream} if unknown.
   * @return a '200 OK' result containing the resource in the body.
   */
  public Result sendResource(String resourceName, boolean inline, Optional<String> filename) {
    return sendResource(resourceName, inline, filename, () -> {}, null);
  }

  /**
   * Send the given resource.
   *
   * <p>The resource will be loaded from the same classloader that this class comes from.
   *
   * @param resourceName The path of the resource to load.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param filename The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name or fallback to {@code application/octet-stream} if unknown.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the resource in the body.
   */
  public Result sendResource(
      String resourceName,
      boolean inline,
      Optional<String> filename,
      Runnable onClose,
      Executor executor) {
    return sendResource(
        resourceName, inline, filename, StaticFileMimeTypes.fileMimeTypes(), onClose, executor);
  }

  /**
   * Send the given resource.
   *
   * <p>The resource will be loaded from the same classloader that this class comes from.
   *
   * @param resourceName The path of the resource to load.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param filename The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name if {@code fileMimeTypes} includes it or fallback to {@code
   *     application/octet-stream} if unknown.
   * @param fileMimeTypes Used for file type mapping.
   * @return a '200 OK' result containing the resource in the body.
   * @deprecated Deprecated as of 2.8.0. Use to {@link #sendResource(String, boolean, Optional,
   *     FileMimeTypes)}.
   */
  @Deprecated
  public Result sendResource(
      String resourceName, boolean inline, String filename, FileMimeTypes fileMimeTypes) {
    return sendResource(resourceName, inline, Optional.ofNullable(filename), fileMimeTypes);
  }

  /**
   * Send the given resource.
   *
   * <p>The resource will be loaded from the same classloader that this class comes from.
   *
   * @param resourceName The path of the resource to load.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param filename The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name if {@code fileMimeTypes} includes it or fallback to {@code
   *     application/octet-stream} if unknown.
   * @param fileMimeTypes Used for file type mapping.
   * @return a '200 OK' result containing the resource in the body.
   */
  public Result sendResource(
      String resourceName, boolean inline, Optional<String> filename, FileMimeTypes fileMimeTypes) {
    return sendResource(resourceName, inline, filename, fileMimeTypes, () -> {}, null);
  }

  /**
   * Send the given resource.
   *
   * <p>The resource will be loaded from the same classloader that this class comes from.
   *
   * @param resourceName The path of the resource to load.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param filename The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name if {@code fileMimeTypes} includes it or fallback to {@code
   *     application/octet-stream} if unknown.
   * @param fileMimeTypes Used for file type mapping.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the resource in the body.
   */
  public Result sendResource(
      String resourceName,
      boolean inline,
      Optional<String> filename,
      FileMimeTypes fileMimeTypes,
      Runnable onClose,
      Executor executor) {
    return sendResource(
        resourceName,
        this.getClass().getClassLoader(),
        inline,
        filename,
        fileMimeTypes,
        onClose,
        executor);
  }

  /**
   * Send the given resource from the given classloader.
   *
   * @param resourceName The path of the resource to load.
   * @param classLoader The classloader to load it from.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param filename The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name or fallback to {@code application/octet-stream} if unknown.
   * @return a '200 OK' result containing the resource in the body.
   * @deprecated Deprecated as of 2.8.0. Use to {@link #sendResource(String, ClassLoader, boolean,
   *     Optional)}.
   */
  @Deprecated
  public Result sendResource(
      String resourceName, ClassLoader classLoader, boolean inline, String filename) {
    return sendResource(resourceName, classLoader, inline, Optional.ofNullable(filename));
  }

  /**
   * Send the given resource from the given classloader.
   *
   * @param resourceName The path of the resource to load.
   * @param classLoader The classloader to load it from.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param filename The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name or fallback to {@code application/octet-stream} if unknown.
   * @return a '200 OK' result containing the resource in the body.
   */
  public Result sendResource(
      String resourceName, ClassLoader classLoader, boolean inline, Optional<String> filename) {
    return sendResource(resourceName, classLoader, inline, filename, () -> {}, null);
  }

  /**
   * Send the given resource from the given classloader.
   *
   * @param resourceName The path of the resource to load.
   * @param classLoader The classloader to load it from.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param filename The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name or fallback to {@code application/octet-stream} if unknown.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the resource in the body.
   */
  public Result sendResource(
      String resourceName,
      ClassLoader classLoader,
      boolean inline,
      Optional<String> filename,
      Runnable onClose,
      Executor executor) {
    return sendResource(
        resourceName,
        classLoader,
        inline,
        filename,
        StaticFileMimeTypes.fileMimeTypes(),
        onClose,
        executor);
  }

  /**
   * Send the given resource from the given classloader.
   *
   * @param resourceName The path of the resource to load.
   * @param classLoader The classloader to load it from.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param filename The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name if {@code fileMimeTypes} includes it or fallback to {@code
   *     application/octet-stream} if unknown.
   * @param fileMimeTypes Used for file type mapping.
   * @return a '200 OK' result containing the resource in the body.
   * @deprecated Deprecated as of 2.8.0. Use to {@link #sendResource(String, ClassLoader, boolean,
   *     Optional, FileMimeTypes)}.
   */
  @Deprecated
  public Result sendResource(
      String resourceName,
      ClassLoader classLoader,
      boolean inline,
      String filename,
      FileMimeTypes fileMimeTypes) {
    return sendResource(
        resourceName, classLoader, inline, Optional.ofNullable(filename), fileMimeTypes);
  }

  /**
   * Send the given resource from the given classloader.
   *
   * @param resourceName The path of the resource to load.
   * @param classLoader The classloader to load it from.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param filename The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name if {@code fileMimeTypes} includes it or fallback to {@code
   *     application/octet-stream} if unknown.
   * @param fileMimeTypes Used for file type mapping.
   * @return a '200 OK' result containing the resource in the body.
   */
  public Result sendResource(
      String resourceName,
      ClassLoader classLoader,
      boolean inline,
      Optional<String> filename,
      FileMimeTypes fileMimeTypes) {
    return sendResource(resourceName, classLoader, inline, filename, fileMimeTypes, () -> {}, null);
  }

  /**
   * Send the given resource from the given classloader.
   *
   * @param resourceName The path of the resource to load.
   * @param classLoader The classloader to load it from.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param filename The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name if {@code fileMimeTypes} includes it or fallback to {@code
   *     application/octet-stream} if unknown.
   * @param fileMimeTypes Used for file type mapping.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the resource in the body.
   */
  public Result sendResource(
      String resourceName,
      ClassLoader classLoader,
      boolean inline,
      Optional<String> filename,
      FileMimeTypes fileMimeTypes,
      Runnable onClose,
      Executor executor) {
    return doSendResource(
        StreamConverters.fromInputStream(() -> classLoader.getResourceAsStream(resourceName)),
        Optional.empty(),
        filename,
        inline,
        fileMimeTypes,
        onClose,
        executor);
  }

  /**
   * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions.
   *
   * @param path The path to send.
   * @return a '200 OK' result containing the file at the provided path with inline content
   *     disposition.
   */
  public Result sendPath(Path path) {
    return sendPath(path, () -> {}, null);
  }

  /**
   * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions.
   *
   * @param path The path to send.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the file at the provided path with inline content
   *     disposition.
   */
  public Result sendPath(Path path, Runnable onClose, Executor executor) {
    return sendPath(path, StaticFileMimeTypes.fileMimeTypes(), onClose, executor);
  }

  /**
   * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions.
   *
   * @param path The path to send.
   * @param fileMimeTypes Used for file type mapping.
   * @return a '200 OK' result containing the file at the provided path with inline content
   *     disposition.
   */
  public Result sendPath(Path path, FileMimeTypes fileMimeTypes) {
    return sendPath(path, fileMimeTypes, () -> {}, null);
  }

  /**
   * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions.
   *
   * @param path The path to send.
   * @param fileMimeTypes Used for file type mapping.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the file at the provided path with inline content
   *     disposition.
   */
  public Result sendPath(
      Path path, FileMimeTypes fileMimeTypes, Runnable onClose, Executor executor) {
    return sendPath(path, DEFAULT_INLINE_MODE, fileMimeTypes, onClose, executor);
  }

  /**
   * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions.
   *
   * @param path The path to send.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @return a '200 OK' result containing the file at the provided path
   */
  public Result sendPath(Path path, boolean inline) {
    return sendPath(path, inline, () -> {}, null);
  }

  /**
   * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions.
   *
   * @param path The path to send.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the file at the provided path
   */
  public Result sendPath(Path path, boolean inline, Runnable onClose, Executor executor) {
    return sendPath(path, inline, StaticFileMimeTypes.fileMimeTypes(), onClose, executor);
  }

  /**
   * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions.
   *
   * @param path The path to send.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param fileMimeTypes Used for file type mapping.
   * @return a '200 OK' result containing the file at the provided path
   */
  public Result sendPath(Path path, boolean inline, FileMimeTypes fileMimeTypes) {
    return sendPath(path, inline, fileMimeTypes, () -> {}, null);
  }

  /**
   * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions.
   *
   * @param path The path to send.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param fileMimeTypes Used for file type mapping.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the file at the provided path
   */
  public Result sendPath(
      Path path, boolean inline, FileMimeTypes fileMimeTypes, Runnable onClose, Executor executor) {
    return sendPath(
        path,
        inline,
        Optional.ofNullable(path).map(p -> p.getFileName().toString()),
        fileMimeTypes,
        onClose,
        executor);
  }

  /**
   * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions.
   *
   * @param path The path to send.
   * @param filename The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name or fallback to {@code application/octet-stream} if unknown.
   * @return a '200 OK' result containing the file at the provided path
   * @deprecated Deprecated as of 2.8.0. Use to {@link #sendPath(Path, Optional)}.
   */
  @Deprecated
  public Result sendPath(Path path, String filename) {
    return sendPath(path, Optional.ofNullable(filename));
  }

  /**
   * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions.
   *
   * @param path The path to send.
   * @param filename The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name or fallback to {@code application/octet-stream} if unknown.
   * @return a '200 OK' result containing the file at the provided path
   */
  public Result sendPath(Path path, Optional<String> filename) {
    return sendPath(path, filename, () -> {}, null);
  }

  /**
   * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions.
   *
   * @param path The path to send.
   * @param filename The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name or fallback to {@code application/octet-stream} if unknown.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the file at the provided path
   */
  public Result sendPath(
      Path path, Optional<String> filename, Runnable onClose, Executor executor) {
    return sendPath(path, filename, StaticFileMimeTypes.fileMimeTypes(), onClose, executor);
  }

  /**
   * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions.
   *
   * @param path The path to send.
   * @param filename The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name if {@code fileMimeTypes} includes it or fallback to {@code
   *     application/octet-stream} if unknown.
   * @param fileMimeTypes Used for file type mapping.
   * @return a '200 OK' result containing the file at the provided path
   * @deprecated Deprecated as of 2.8.0. Use to {@link #sendPath(Path, Optional, FileMimeTypes)}.
   */
  @Deprecated
  public Result sendPath(Path path, String filename, FileMimeTypes fileMimeTypes) {
    return sendPath(path, Optional.ofNullable(filename), fileMimeTypes);
  }

  /**
   * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions.
   *
   * @param path The path to send.
   * @param filename The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name if {@code fileMimeTypes} includes it or fallback to {@code
   *     application/octet-stream} if unknown.
   * @param fileMimeTypes Used for file type mapping.
   * @return a '200 OK' result containing the file at the provided path
   */
  public Result sendPath(Path path, Optional<String> filename, FileMimeTypes fileMimeTypes) {
    return sendPath(path, filename, fileMimeTypes, () -> {}, null);
  }

  /**
   * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions.
   *
   * @param path The path to send.
   * @param filename The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name if {@code fileMimeTypes} includes it or fallback to {@code
   *     application/octet-stream} if unknown.
   * @param fileMimeTypes Used for file type mapping.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the file at the provided path
   */
  public Result sendPath(
      Path path,
      Optional<String> filename,
      FileMimeTypes fileMimeTypes,
      Runnable onClose,
      Executor executor) {
    return sendPath(path, DEFAULT_INLINE_MODE, filename, fileMimeTypes, onClose, executor);
  }

  /**
   * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions
   *
   * @param path The path to send.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param filename The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name or fallback to {@code application/octet-stream} if unknown.
   * @return a '200 OK' result containing the file at the provided path
   * @deprecated Deprecated as of 2.8.0. Use to {@link #sendPath(Path, boolean, Optional)}.
   */
  @Deprecated
  public Result sendPath(Path path, boolean inline, String filename) {
    return sendPath(path, inline, Optional.ofNullable(filename));
  }

  /**
   * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions
   *
   * @param path The path to send.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param filename The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name or fallback to {@code application/octet-stream} if unknown.
   * @return a '200 OK' result containing the file at the provided path
   */
  public Result sendPath(Path path, boolean inline, Optional<String> filename) {
    return sendPath(path, inline, filename, () -> {}, null);
  }

  /**
   * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions
   *
   * @param path The path to send.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param filename The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name or fallback to {@code application/octet-stream} if unknown.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the file at the provided path
   */
  public Result sendPath(
      Path path, boolean inline, Optional<String> filename, Runnable onClose, Executor executor) {
    return sendPath(path, inline, filename, StaticFileMimeTypes.fileMimeTypes(), onClose, executor);
  }

  /**
   * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions
   *
   * @param path The path to send.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param filename The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name if {@code fileMimeTypes} includes it or fallback to {@code
   *     application/octet-stream} if unknown.
   * @param fileMimeTypes Used for file type mapping.
   * @return a '200 OK' result containing the file at the provided path
   * @deprecated Deprecated as of 2.8.0. Use to {@link #sendPath(Path, boolean, Optional,
   *     FileMimeTypes)}.
   */
  @Deprecated
  public Result sendPath(Path path, boolean inline, String filename, FileMimeTypes fileMimeTypes) {
    return sendPath(path, inline, Optional.ofNullable(filename), fileMimeTypes);
  }

  /**
   * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions
   *
   * @param path The path to send.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param filename The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name if {@code fileMimeTypes} includes it or fallback to {@code
   *     application/octet-stream} if unknown.
   * @param fileMimeTypes Used for file type mapping.
   * @return a '200 OK' result containing the file at the provided path
   */
  public Result sendPath(
      Path path, boolean inline, Optional<String> filename, FileMimeTypes fileMimeTypes) {
    return sendPath(path, inline, filename, fileMimeTypes, () -> {}, null);
  }

  /**
   * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions
   *
   * @param path The path to send.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param filename The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name if {@code fileMimeTypes} includes it or fallback to {@code
   *     application/octet-stream} if unknown.
   * @param fileMimeTypes Used for file type mapping.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the file at the provided path
   */
  public Result sendPath(
      Path path,
      boolean inline,
      Optional<String> filename,
      FileMimeTypes fileMimeTypes,
      Runnable onClose,
      Executor executor) {
    if (path == null) {
      throw new NullPointerException("null content");
    }
    try {
      return doSendResource(
          FileIO.fromPath(path),
          Optional.of(Files.size(path)),
          filename,
          inline,
          fileMimeTypes,
          onClose,
          executor);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Sends the given file using the default inline mode.
   *
   * @param file The file to send.
   * @return a '200 OK' result containing the file.
   */
  public Result sendFile(File file) {
    return sendFile(file, () -> {}, null);
  }

  /**
   * Sends the given file using the default inline mode.
   *
   * @param file The file to send.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the file.
   */
  public Result sendFile(File file, Runnable onClose, Executor executor) {
    return sendFile(file, StaticFileMimeTypes.fileMimeTypes(), onClose, executor);
  }

  /**
   * Sends the given file using the default inline mode.
   *
   * @param file The file to send.
   * @param fileMimeTypes Used for file type mapping.
   * @return a '200 OK' result containing the file.
   */
  public Result sendFile(File file, FileMimeTypes fileMimeTypes) {
    return sendFile(file, fileMimeTypes, () -> {}, null);
  }

  /**
   * Sends the given file using the default inline mode.
   *
   * @param file The file to send.
   * @param fileMimeTypes Used for file type mapping.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the file.
   */
  public Result sendFile(
      File file, FileMimeTypes fileMimeTypes, Runnable onClose, Executor executor) {
    return sendFile(file, DEFAULT_INLINE_MODE, fileMimeTypes, onClose, executor);
  }

  /**
   * Sends the given file.
   *
   * @param file The file to send.
   * @param inline True if the file should be sent inline, false if it should be sent as an
   *     attachment.
   * @return a '200 OK' result containing the file
   */
  public Result sendFile(File file, boolean inline) {
    return sendFile(file, inline, () -> {}, null);
  }

  /**
   * Sends the given file.
   *
   * @param file The file to send.
   * @param inline True if the file should be sent inline, false if it should be sent as an
   *     attachment.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the file
   */
  public Result sendFile(File file, boolean inline, Runnable onClose, Executor executor) {
    return sendFile(file, inline, StaticFileMimeTypes.fileMimeTypes(), onClose, executor);
  }

  /**
   * Sends the given file.
   *
   * @param file The file to send.
   * @param inline True if the file should be sent inline, false if it should be sent as an
   *     attachment.
   * @param fileMimeTypes Used for file type mapping.
   * @return a '200 OK' result containing the file
   */
  public Result sendFile(File file, boolean inline, FileMimeTypes fileMimeTypes) {
    return sendFile(file, inline, fileMimeTypes, () -> {}, null);
  }

  /**
   * Sends the given file.
   *
   * @param file The file to send.
   * @param inline True if the file should be sent inline, false if it should be sent as an
   *     attachment.
   * @param fileMimeTypes Used for file type mapping.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the file
   */
  public Result sendFile(
      File file, boolean inline, FileMimeTypes fileMimeTypes, Runnable onClose, Executor executor) {
    if (file == null) {
      throw new NullPointerException("null file");
    }
    return sendFile(
        file,
        inline,
        Optional.ofNullable(file).map(f -> f.getName()),
        fileMimeTypes,
        onClose,
        executor);
  }

  /**
   * Send the given file.
   *
   * @param file The file to send.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name or fallback to {@code application/octet-stream} if unknown.
   * @return a '200 OK' result containing the file
   * @deprecated Deprecated as of 2.8.0. Use to {@link #sendFile(File, Optional)}.
   */
  @Deprecated
  public Result sendFile(File file, String fileName) {
    return sendFile(file, Optional.ofNullable(fileName));
  }

  /**
   * Send the given file.
   *
   * @param file The file to send.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name or fallback to {@code application/octet-stream} if unknown.
   * @return a '200 OK' result containing the file
   */
  public Result sendFile(File file, Optional<String> fileName) {
    return sendFile(file, fileName, () -> {}, null);
  }

  /**
   * Send the given file.
   *
   * @param file The file to send.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name or fallback to {@code application/octet-stream} if unknown.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the file
   */
  public Result sendFile(
      File file, Optional<String> fileName, Runnable onClose, Executor executor) {
    return sendFile(file, fileName, StaticFileMimeTypes.fileMimeTypes(), onClose, executor);
  }

  /**
   * Send the given file.
   *
   * @param file The file to send.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name if {@code fileMimeTypes} includes it or fallback to {@code
   *     application/octet-stream} if unknown.
   * @param fileMimeTypes Used for file type mapping.
   * @return a '200 OK' result containing the file
   * @deprecated Deprecated as of 2.8.0. Use to {@link #sendFile(File, Optional, FileMimeTypes)}.
   */
  @Deprecated
  public Result sendFile(File file, String fileName, FileMimeTypes fileMimeTypes) {
    return sendFile(file, Optional.ofNullable(fileName), fileMimeTypes);
  }

  /**
   * Send the given file.
   *
   * @param file The file to send.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name if {@code fileMimeTypes} includes it or fallback to {@code
   *     application/octet-stream} if unknown.
   * @param fileMimeTypes Used for file type mapping.
   * @return a '200 OK' result containing the file
   */
  public Result sendFile(File file, Optional<String> fileName, FileMimeTypes fileMimeTypes) {
    return sendFile(file, fileName, fileMimeTypes, () -> {}, null);
  }

  /**
   * Send the given file.
   *
   * @param file The file to send.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name if {@code fileMimeTypes} includes it or fallback to {@code
   *     application/octet-stream} if unknown.
   * @param fileMimeTypes Used for file type mapping.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the file
   */
  public Result sendFile(
      File file,
      Optional<String> fileName,
      FileMimeTypes fileMimeTypes,
      Runnable onClose,
      Executor executor) {
    return sendFile(file, DEFAULT_INLINE_MODE, fileName, fileMimeTypes, onClose, executor);
  }

  /**
   * Send the given file.
   *
   * @param file The file to send.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name or fallback to {@code application/octet-stream} if unknown.
   * @param inline True if the file should be sent inline, false if it should be sent as an
   *     attachment.
   * @return a '200 OK' result containing the file
   * @deprecated Deprecated as of 2.8.0. Use to {@link #sendFile(File, boolean, Optional)}.
   */
  @Deprecated
  public Result sendFile(File file, boolean inline, String fileName) {
    return sendFile(file, inline, Optional.ofNullable(fileName));
  }

  /**
   * Send the given file.
   *
   * @param file The file to send.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name or fallback to {@code application/octet-stream} if unknown.
   * @param inline True if the file should be sent inline, false if it should be sent as an
   *     attachment.
   * @return a '200 OK' result containing the file
   */
  public Result sendFile(File file, boolean inline, Optional<String> fileName) {
    return sendFile(file, inline, fileName, () -> {}, null);
  }

  /**
   * Send the given file.
   *
   * @param file The file to send.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name or fallback to {@code application/octet-stream} if unknown.
   * @param inline True if the file should be sent inline, false if it should be sent as an
   *     attachment.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the file
   */
  public Result sendFile(
      File file, boolean inline, Optional<String> fileName, Runnable onClose, Executor executor) {
    return sendFile(file, inline, fileName, StaticFileMimeTypes.fileMimeTypes(), onClose, executor);
  }

  /**
   * Send the given file.
   *
   * @param file The file to send.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name if {@code fileMimeTypes} includes it or fallback to {@code
   *     application/octet-stream} if unknown.
   * @param inline True if the file should be sent inline, false if it should be sent as an
   *     attachment.
   * @param fileMimeTypes Used for file type mapping.
   * @return a '200 OK' result containing the file
   * @deprecated Deprecated as of 2.8.0. Use to {@link #sendFile(File, boolean, Optional,
   *     FileMimeTypes)}.
   */
  @Deprecated
  public Result sendFile(File file, boolean inline, String fileName, FileMimeTypes fileMimeTypes) {
    return sendFile(file, inline, Optional.ofNullable(fileName), fileMimeTypes);
  }

  /**
   * Send the given file.
   *
   * @param file The file to send.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name if {@code fileMimeTypes} includes it or fallback to {@code
   *     application/octet-stream} if unknown.
   * @param inline True if the file should be sent inline, false if it should be sent as an
   *     attachment.
   * @param fileMimeTypes Used for file type mapping.
   * @return a '200 OK' result containing the file
   */
  public Result sendFile(
      File file, boolean inline, Optional<String> fileName, FileMimeTypes fileMimeTypes) {
    return sendFile(file, inline, fileName, fileMimeTypes, () -> {}, null);
  }

  /**
   * Send the given file.
   *
   * @param file The file to send.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name if {@code fileMimeTypes} includes it or fallback to {@code
   *     application/octet-stream} if unknown.
   * @param inline True if the file should be sent inline, false if it should be sent as an
   *     attachment.
   * @param fileMimeTypes Used for file type mapping.
   * @param onClose Useful in order to perform cleanup operations (e.g. deleting a temporary file
   *     generated for a download).
   * @param executor The executor to use for asynchronous execution of {@code onClose}.
   * @return a '200 OK' result containing the file
   */
  public Result sendFile(
      File file,
      boolean inline,
      Optional<String> fileName,
      FileMimeTypes fileMimeTypes,
      Runnable onClose,
      Executor executor) {
    if (file == null) {
      throw new NullPointerException("null file");
    }
    try {
      return doSendResource(
          FileIO.fromPath(file.toPath()),
          Optional.of(Files.size(file.toPath())),
          fileName,
          inline,
          fileMimeTypes,
          onClose,
          executor);
    } catch (final IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  private Result doSendResource(
      Source<ByteString, CompletionStage<IOResult>> data,
      Optional<Long> contentLength,
      Optional<String> resourceName,
      boolean inline,
      FileMimeTypes fileMimeTypes,
      Runnable onClose,
      Executor executor) {
    return new Result(
        status(),
        Results.contentDispositionHeader(inline, resourceName),
        new HttpEntity.Streamed(
            attachOnClose(data, onClose, executor),
            contentLength,
            resourceName.map(
                name -> fileMimeTypes.forFileName(name).orElse(Http.MimeTypes.BINARY))));
  }

  private static Source<ByteString, CompletionStage<IOResult>> attachOnClose(
      Source<ByteString, CompletionStage<IOResult>> data, Runnable onClose, Executor executor) {
    return data.mapMaterializedValue(
        cs ->
            executor != null
                ? cs.whenCompleteAsync((ioResult, exception) -> onClose.run(), executor)
                : cs.whenCompleteAsync((ioResult, exception) -> onClose.run()));
  }

  /**
   * Send a chunked response with the given chunks.
   *
   * @param chunks the chunks to send
   * @return a '200 OK' response with the given chunks.
   */
  public Result chunked(Source<ByteString, ?> chunks) {
    return chunked(chunks, Optional.empty());
  }

  /**
   * Send a chunked response with the given chunks.
   *
   * @param chunks the chunks to send
   * @param contentType the entity content type.
   * @return a '200 OK' response with the given chunks.
   */
  public Result chunked(Source<ByteString, ?> chunks, Optional<String> contentType) {
    return new Result(status(), HttpEntity.chunked(chunks, contentType));
  }

  /**
   * Send a chunked response with the given chunks.
   *
   * @param chunks the chunks to send
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name or fallback to {@code application/octet-stream} if unknown.
   * @return a '200 OK' response with the given chunks.
   */
  public Result chunked(Source<ByteString, ?> chunks, boolean inline, Optional<String> fileName) {
    return chunked(chunks, inline, fileName, StaticFileMimeTypes.fileMimeTypes());
  }

  /**
   * Send a chunked response with the given chunks.
   *
   * @param chunks the chunks to send
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name if {@code fileMimeTypes} includes it or fallback to {@code
   *     application/octet-stream} if unknown.
   * @param fileMimeTypes Used for file type mapping.
   * @return a '200 OK' response with the given chunks.
   */
  public Result chunked(
      Source<ByteString, ?> chunks,
      boolean inline,
      Optional<String> fileName,
      FileMimeTypes fileMimeTypes) {
    return new Result(
        status(),
        Results.contentDispositionHeader(inline, fileName),
        HttpEntity.chunked(
            chunks,
            fileName.map(name -> fileMimeTypes.forFileName(name).orElse(Http.MimeTypes.BINARY))));
  }

  /**
   * Send a streamed response with the given source.
   *
   * @param body the source to send
   * @param contentLength the entity content length.
   * @return a '200 OK' response with the given body.
   */
  public Result streamed(Source<ByteString, ?> body, Optional<Long> contentLength) {
    return streamed(body, contentLength, Optional.empty());
  }

  /**
   * Send a streamed response with the given source.
   *
   * @param body the source to send
   * @param contentLength the entity content length.
   * @param contentType the entity content type.
   * @return a '200 OK' response with the given body.
   */
  public Result streamed(
      Source<ByteString, ?> body, Optional<Long> contentLength, Optional<String> contentType) {
    return new Result(status(), new HttpEntity.Streamed(body, contentLength, contentType));
  }

  /**
   * Send a streamed response with the given source.
   *
   * @param body the source to send
   * @param contentLength the entity content length.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name or fallback to {@code application/octet-stream} if unknown.
   * @return a '200 OK' response with the given body.
   */
  public Result streamed(
      Source<ByteString, ?> body,
      Optional<Long> contentLength,
      boolean inline,
      Optional<String> fileName) {
    return streamed(body, contentLength, inline, fileName, StaticFileMimeTypes.fileMimeTypes());
  }

  /**
   * Send a streamed response with the given source.
   *
   * @param body the source to send
   * @param contentLength the entity content length.
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name or fallback to {@code application/octet-stream} if unknown.
   * @param fileMimeTypes Used for file type mapping.
   * @return a '200 OK' response with the given body.
   */
  public Result streamed(
      Source<ByteString, ?> body,
      Optional<Long> contentLength,
      boolean inline,
      Optional<String> fileName,
      FileMimeTypes fileMimeTypes) {
    return new Result(
        status(),
        Results.contentDispositionHeader(inline, fileName),
        new HttpEntity.Streamed(
            body,
            contentLength,
            fileName.map(name -> fileMimeTypes.forFileName(name).orElse(Http.MimeTypes.BINARY))));
  }

  /**
   * Send a json result.
   *
   * @param json the json node to send
   * @return a '200 OK' result containing the json encoded as UTF-8.
   */
  public Result sendJson(JsonNode json) {
    return sendJson(json, JsonEncoding.UTF8);
  }

  /**
   * Send a json result.
   *
   * @param json the json node to send
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param fileName The file name rendered in the {@code Content-Disposition} header.
   * @return a '200 OK' result containing the json encoded as UTF-8.
   */
  public Result sendJson(JsonNode json, boolean inline, Optional<String> fileName) {
    return sendJson(json, JsonEncoding.UTF8, inline, fileName);
  }

  /**
   * Send a json result.
   *
   * @param json the json to send
   * @param encoding the encoding in which to encode the json (e.g. "UTF-8")
   * @return a '200 OK' result containing the json encoded with the given charset
   */
  public Result sendJson(JsonNode json, JsonEncoding encoding) {
    return sendJson(json, encoding, true, Optional.empty());
  }

  /**
   * Send a json result.
   *
   * @param json the json to send
   * @param encoding the encoding in which to encode the json (e.g. "UTF-8")
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param fileName The file name rendered in the {@code Content-Disposition} header.
   * @return a '200 OK' result containing the json encoded with the given charset
   */
  public Result sendJson(
      JsonNode json, JsonEncoding encoding, boolean inline, Optional<String> fileName) {
    if (json == null) {
      throw new NullPointerException("Null content");
    }

    ObjectMapper mapper = Json.mapper();
    ByteStringBuilder builder = ByteString$.MODULE$.newBuilder();

    try {
      JsonGenerator jgen = mapper.getFactory().createGenerator(builder.asOutputStream(), encoding);

      mapper.writeValue(jgen, json);
      String contentType = MimeTypes.JSON;
      return new Result(
          status(),
          Results.contentDispositionHeader(inline, fileName),
          new HttpEntity.Strict(builder.result(), Optional.of(contentType)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Send an HTTP entity.
   *
   * @param entity the entity to send
   * @return a response with the given body.
   */
  public Result sendEntity(HttpEntity entity) {
    return new Result(status(), entity);
  }

  /**
   * Send an HTTP entity.
   *
   * @param entity the entity to send
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name or fallback to {@code application/octet-stream} if unknown.
   * @return a response with the given body.
   */
  public Result sendEntity(HttpEntity entity, boolean inline, Optional<String> fileName) {
    return sendEntity(entity, inline, fileName, StaticFileMimeTypes.fileMimeTypes());
  }

  /**
   * Send an HTTP entity.
   *
   * @param entity the entity to send
   * @param inline Whether it should be served as an inline file, or as an attachment.
   * @param fileName The file name rendered in the {@code Content-Disposition} header. The response
   *     will also automatically include the MIME type in the {@code Content-Type} header deducing
   *     it from this file name if {@code fileMimeTypes} includes it or fallback to {@code
   *     application/octet-stream} if unknown.
   * @param fileMimeTypes Used for file type mapping.
   * @return a response with the given body.
   */
  public Result sendEntity(
      HttpEntity entity, boolean inline, Optional<String> fileName, FileMimeTypes fileMimeTypes) {
    return new Result(
        status(),
        Results.contentDispositionHeader(inline, fileName),
        entity.as(
            fileName
                .flatMap(name -> fileMimeTypes.forFileName(name))
                .orElse(Http.MimeTypes.BINARY)));
  }
}
