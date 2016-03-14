/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.mvc;

import akka.stream.javadsl.Source;
import akka.stream.javadsl.StreamConverters;
import akka.util.ByteString;
import akka.util.ByteString$;
import akka.util.ByteStringBuilder;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import play.api.libs.streams.Streams;
import play.http.HttpEntity;
import play.libs.Json;
import scala.Option;
import scala.compat.java8.OptionConverters;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * A status with no body
 */
public class StatusHeader extends Result {

    private static final int defaultChunkSize = 1024 * 8;

    public StatusHeader(int status) {
        super(status);
    }

    /**
     * Send the given input stream.
     *
     * The input stream will be sent chunked since there is no specified content length.
     *
     * @param stream The input stream to send.
     * @return The result.
     */
    public Result sendInputStream(InputStream stream) {
        if (stream == null) {
            throw new NullPointerException("Null stream");
        }
        return new Result(status(), HttpEntity.chunked(StreamConverters.fromInputStream(() -> stream, defaultChunkSize),
                Optional.empty()));
    }

    /**
     * Send the given input stream.
     *
     * @param stream The input stream to send.
     * @param contentLength The length of the content in the stream.
     * @return The result.
     */
    public Result sendInputStream(InputStream stream, long contentLength) {
        if (stream == null) {
            throw new NullPointerException("Null content");
        }
        return new Result(status(), new HttpEntity.Streamed(StreamConverters.fromInputStream(() -> stream, defaultChunkSize),
                Optional.of(contentLength), Optional.empty()));
    }

    /**
     * Send the given resource.
     * <p>
     * The resource will be loaded from the same classloader that this class comes from.
     *
     * @param resourceName The path of the resource to load.
     * @return an inlined '200 OK' result containing the resource in the body with in-line content disposition.
     */
    public Result sendResource(String resourceName) {
        return sendResource(resourceName, true);
    }

    /**
     * Send the given resource from the given classloader.
     *
     * @param resourceName The path of the resource to load.
     * @param classLoader  The classloader to load it from.
     * @return a '200 OK' result containing the resource in the body with in-line content disposition.
     */
    public Result sendResource(String resourceName, ClassLoader classLoader) {
        return sendResource(resourceName, classLoader, true);
    }

    /**
     * Send the given resource.
     * <p>
     * The resource will be loaded from the same classloader that this class comes from.
     *
     * @param resourceName The path of the resource to load.
     * @param inline       Whether it should be served as an inline file, or as an attachment.
     * @return a '200 OK' result containing the resource in the body with in-line content disposition.
     */
    public Result sendResource(String resourceName, boolean inline) {
        return sendResource(resourceName, this.getClass().getClassLoader(), inline);
    }

    /**
     * Send the given resource from the given classloader.
     *
     * @param resourceName The path of the resource to load.
     * @param classLoader  The classloader to load it from.
     * @param inline       Whether it should be served as an inline file, or as an attachment.
     * @return a '200 OK' result containing the resource in the body.
     */
    public Result sendResource(String resourceName, ClassLoader classLoader, boolean inline) {
        return doSendResource(StreamConverters.fromInputStream(() -> classLoader.getResourceAsStream(resourceName)),
                Optional.empty(), Optional.of(resourceName), inline);
    }

    /**
     * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions.
     *
     * @param path The path to send.
     * @return a '200 OK' result containing the file at the provided path with inline content disposition.
     */
    public Result sendPath(Path path) {
        return sendPath(path, false);
    }

    /**
     * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions.
     *
     * @param path   The path to send.
     * @param inline Whether it should be served as an inline file, or as an attachment.
     * @return a '200 OK' result containing the file at the provided path
     */
    public Result sendPath(Path path, boolean inline) {
        return sendPath(path, inline, path.getFileName().toString());
    }

    /**
     * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions
     *
     * @param path     The path to send.
     * @param inline   Whether it should be served as an inline file, or as an attachment.
     * @param filename The file name of the path.
     * @return a '200 OK' result containing the file at the provided path
     */
    public Result sendPath(Path path, boolean inline, String filename) {
        if (path == null) {
            throw new NullPointerException("null content");
        }
        try {
            return doSendResource(
                    StreamConverters.fromInputStream(() -> Files.newInputStream(path)),
                    Optional.of(Files.size(path)),
                    Optional.of(filename),
                    inline
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sends the given file.
     *
     * @param file The file to send.
     */
    private Result sendFile(File file) {
        return sendFile(file, true);
    }

    /**
     * Sends the given file.
     *
     * @param file The file to send.
     * @param inline  True if the file should be sent inline, false if it should be sent as an attachment.
     * @return a '200 OK' result containing the file
     */
    public Result sendFile(File file, boolean inline) {
        if (file == null) {
            throw new NullPointerException("null file");
        }
        return doSendResource(
                StreamConverters.fromInputStream(() -> Files.newInputStream(file.toPath())),
                Optional.of(file.length()),
                Optional.of(file.getName()),
                inline
        );
    }

    /**
     * Send the given file as an attachment.
     *
     * @param file The file to send.
     * @param fileName The name of the attachment
     * @return a '200 OK' result containing the file
     */
    public Result sendFile(File file, String fileName) {
        if (file == null) {
            throw new NullPointerException("null file");
        }
        return doSendResource(
                StreamConverters.fromInputStream(() -> Files.newInputStream(file.toPath())),
                Optional.of(file.length()),
                Optional.of(fileName),
                true
        );
    }

    private Result doSendResource(Source<ByteString, ?> data, Optional<Long> contentLength,
                                  Optional<String> resourceName, boolean inline) {
        Map<String, String> headers = Collections.singletonMap(
                Http.HeaderNames.CONTENT_DISPOSITION,
                (inline ? "inline" : "attachment") +
                (resourceName.isPresent() ? "; filename=\"" + resourceName.get() + "\"" : "")
        );

        return new Result(status(), headers, new HttpEntity.Streamed(
                data,
                contentLength,
                resourceName.map(name -> OptionConverters.toJava(play.api.libs.MimeTypes.forFileName(name))
                        .orElse(Http.MimeTypes.BINARY)
                )
        ));
    }

    /**
     * Send a chunked response with the given chunks.
     *
     * @param chunks the chunks to send
     * @return a '200 OK' response with the given chunks.
     */
    public Result chunked(Source<ByteString, ?> chunks) {
        return new Result(status(), HttpEntity.chunked(chunks, Optional.empty()));
    }

    /**
     * Send a chunked response with the given chunks.
     *
     * @deprecated Use {@link #chunked(Source)} instead.
     * @param chunks Deprecated
     * @param <T> Deprecated
     * @return Deprecated
     */
    public <T> Result chunked(Results.Chunks<T> chunks) {
        return new Result(status(), HttpEntity.chunked(
                Source.fromPublisher(Streams.<T>enumeratorToPublisher(chunks.enumerator, Option.<T>empty()))
                        .<ByteString>map(t -> chunks.writable.transform().apply(t)),
                OptionConverters.toJava(chunks.writable.contentType())
        ));
    }

    /**
     * Send a json result.
     *
     * @param json the json node to send
     * @return a '200 OK' result containing the json encoded as UTF-8.
     */
    public Result sendJson(JsonNode json) {
        return sendJson(json, "utf-8");
    }

    /**
     * Send a json result.
     *
     * @param json the json to send
     * @param charset the charset in which to encode the json (e.g. "UTF-8")
     * @return a '200 OK' result containing the json encoded with the given charset
     */
    public Result sendJson(JsonNode json, String charset) {
        if (json == null) {
            throw new NullPointerException("Null content");
        }

        ObjectMapper mapper = Json.mapper();
        ByteStringBuilder builder = ByteString$.MODULE$.newBuilder();

        try {
            JsonGenerator jgen = new JsonFactory(mapper)
                    .createGenerator(new OutputStreamWriter(builder.asOutputStream(), charset));

            mapper.writeValue(jgen, json);
            return new Result(status(), new HttpEntity.Strict(builder.result(),
                    Optional.of("application/json;charset=" + charset)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Result sendEntity(HttpEntity entity) {
        return new Result(status(),entity);
    }
}
