/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

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
import play.core.utils.HttpHeaderParameterEncoding;
import play.http.HttpEntity;
import play.libs.Json;
import play.mvc.Http.MimeTypes;

/**
 * A status with no body
 */
public class StatusHeader extends Result {

    private static final int DEFAULT_CHUNK_SIZE = 1024 * 8;
    private static final boolean DEFAULT_INLINE_MODE = true;

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
        return new Result(status(), HttpEntity.chunked(StreamConverters.fromInputStream(() -> stream, DEFAULT_CHUNK_SIZE),
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
            throw new NullPointerException("Null stream");
        }
        return new Result(status(), new HttpEntity.Streamed(StreamConverters.fromInputStream(() -> stream, DEFAULT_CHUNK_SIZE),
                Optional.of(contentLength), Optional.empty()));
    }

    /**
     * Send the given resource.
     * <p>
     * The resource will be loaded from the same classloader that this class comes from.
     *
     * @param resourceName The path of the resource to load.
     * @return a '200 OK' result containing the resource in the body with in-line content disposition.
     */
    public Result sendResource(String resourceName) {
        return sendResource(resourceName, StaticFileMimeTypes.fileMimeTypes());
    }

    /**
     * Send the given resource.
     * <p>
     * The resource will be loaded from the same classloader that this class comes from.
     *
     * @param resourceName The path of the resource to load.
     * @param fileMimeTypes Used for file type mapping.
     * @return a '200 OK' result containing the resource in the body with in-line content disposition.
     */
    public Result sendResource(String resourceName, FileMimeTypes fileMimeTypes) {
        return sendResource(resourceName, DEFAULT_INLINE_MODE, fileMimeTypes);
    }

    /**
     * Send the given resource from the given classloader.
     *
     * @param resourceName The path of the resource to load.
     * @param classLoader  The classloader to load it from.
     * @return a '200 OK' result containing the resource in the body with in-line content disposition.
     */
    public Result sendResource(String resourceName, ClassLoader classLoader) {
        return sendResource(resourceName, classLoader, StaticFileMimeTypes.fileMimeTypes());
    }

    /**
     * Send the given resource from the given classloader.
     *
     * @param resourceName The path of the resource to load.
     * @param classLoader  The classloader to load it from.
     * @param fileMimeTypes Used for file type mapping.
     * @return a '200 OK' result containing the resource in the body with in-line content disposition.
     */
    public Result sendResource(String resourceName, ClassLoader classLoader, FileMimeTypes fileMimeTypes) {
        return sendResource(resourceName, classLoader, DEFAULT_INLINE_MODE, fileMimeTypes);
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
        return sendResource(resourceName, inline, StaticFileMimeTypes.fileMimeTypes());
    }

    /**
     * Send the given resource.
     * <p>
     * The resource will be loaded from the same classloader that this class comes from.
     *
     * @param resourceName The path of the resource to load.
     * @param inline       Whether it should be served as an inline file, or as an attachment.
     * @param fileMimeTypes Used for file type mapping.
     * @return a '200 OK' result containing the resource in the body with in-line content disposition.
     */
    public Result sendResource(String resourceName, boolean inline, FileMimeTypes fileMimeTypes) {
        return sendResource(resourceName, this.getClass().getClassLoader(), inline, fileMimeTypes);
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
        return sendResource(resourceName, classLoader, inline, StaticFileMimeTypes.fileMimeTypes());
    }

    /**
     * Send the given resource from the given classloader.
     *
     * @param resourceName The path of the resource to load.
     * @param classLoader  The classloader to load it from.
     * @param inline       Whether it should be served as an inline file, or as an attachment.
     * @param fileMimeTypes Used for file type mapping.
     * @return a '200 OK' result containing the resource in the body.
     */
    public Result sendResource(String resourceName, ClassLoader classLoader, boolean inline, FileMimeTypes fileMimeTypes) {
        return doSendResource(StreamConverters.fromInputStream(() -> classLoader.getResourceAsStream(resourceName)),
                Optional.empty(), Optional.of(resourceName), inline, fileMimeTypes);
    }

    /**
     * Send the given resource.
     * <p>
     * The resource will be loaded from the same classloader that this class comes from.
     *
     * @param resourceName The path of the resource to load.
     * @param inline       Whether it should be served as an inline file, or as an attachment.
     * @param filename     The file name of the resource.
     * @return a '200 OK' result containing the resource in the body.
     */
    public Result sendResource(String resourceName, boolean inline, String filename) {
        return sendResource(resourceName, inline, filename, StaticFileMimeTypes.fileMimeTypes());
    }

    /**
     * Send the given resource.
     * <p>
     * The resource will be loaded from the same classloader that this class comes from.
     *
     * @param resourceName The path of the resource to load.
     * @param inline       Whether it should be served as an inline file, or as an attachment.
     * @param filename     The file name of the resource.
     * @param fileMimeTypes Used for file type mapping.
     * @return a '200 OK' result containing the resource in the body.
     */
    public Result sendResource(String resourceName, boolean inline, String filename, FileMimeTypes fileMimeTypes) {
        return sendResource(resourceName, this.getClass().getClassLoader(), inline, filename, fileMimeTypes);
    }

    /**
     * Send the given resource from the given classloader.
     *
     * @param resourceName The path of the resource to load.
     * @param classLoader  The classloader to load it from.
     * @param inline       Whether it should be served as an inline file, or as an attachment.
     * @param filename     The file name of the resource.
     * @return a '200 OK' result containing the resource in the body.
     */
    public Result sendResource(String resourceName, ClassLoader classLoader, boolean inline, String filename) {
        return sendResource(resourceName, classLoader, inline, filename, StaticFileMimeTypes.fileMimeTypes());
    }
    /**
     * Send the given resource from the given classloader.
     *
     * @param resourceName The path of the resource to load.
     * @param classLoader  The classloader to load it from.
     * @param inline       Whether it should be served as an inline file, or as an attachment.
     * @param filename     The file name of the resource.
     * @param fileMimeTypes Used for file type mapping.
     * @return a '200 OK' result containing the resource in the body.
     */
    public Result sendResource(String resourceName, ClassLoader classLoader, boolean inline, String filename, FileMimeTypes fileMimeTypes) {
        return doSendResource(StreamConverters.fromInputStream(() -> classLoader.getResourceAsStream(resourceName)),
                Optional.empty(), Optional.of(filename), inline, fileMimeTypes);
    }

    /**
     * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions.
     *
     * @param path The path to send.
     * @return a '200 OK' result containing the file at the provided path with inline content disposition.
     */
    public Result sendPath(Path path) {
        return sendPath(path, StaticFileMimeTypes.fileMimeTypes());
    }

    /**
     * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions.
     *
     * @param path The path to send.
     * @param fileMimeTypes Used for file type mapping.
     * @return a '200 OK' result containing the file at the provided path with inline content disposition.
     */
    public Result sendPath(Path path, FileMimeTypes fileMimeTypes) {
        return sendPath(path, DEFAULT_INLINE_MODE, fileMimeTypes);
    }

    /**
     * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions.
     *
     * @param path   The path to send.
     * @param inline Whether it should be served as an inline file, or as an attachment.
     * @return a '200 OK' result containing the file at the provided path
     */
    public Result sendPath(Path path, boolean inline) {
        return sendPath(path, inline, StaticFileMimeTypes.fileMimeTypes());
    }

    /**
     * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions.
     *
     * @param path   The path to send.
     * @param inline Whether it should be served as an inline file, or as an attachment.
     * @param fileMimeTypes Used for file type mapping.
     * @return a '200 OK' result containing the file at the provided path
     */
    public Result sendPath(Path path, boolean inline, FileMimeTypes fileMimeTypes) {
        return sendPath(path, inline, path.getFileName().toString(), fileMimeTypes);
    }

    /**
     * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions.
     *
     * @param path     The path to send.
     * @param filename The file name of the path.
     * @return a '200 OK' result containing the file at the provided path
     */
    public Result sendPath(Path path, String filename) {
        return sendPath(path, filename, StaticFileMimeTypes.fileMimeTypes());
    }

    /**
     * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions.
     *
     * @param path     The path to send.
     * @param filename The file name of the path.
     * @param fileMimeTypes Used for file type mapping.
     * @return a '200 OK' result containing the file at the provided path
     */
    public Result sendPath(Path path, String filename, FileMimeTypes fileMimeTypes) {
        return sendPath(path, DEFAULT_INLINE_MODE, filename, fileMimeTypes);
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
        return sendPath(path, inline, filename, StaticFileMimeTypes.fileMimeTypes());
    }

    /**
     * Sends the given path if it is a valid file. Otherwise throws RuntimeExceptions
     *
     * @param path     The path to send.
     * @param inline   Whether it should be served as an inline file, or as an attachment.
     * @param filename The file name of the path.
     * @param fileMimeTypes Used for file type mapping.
     * @return a '200 OK' result containing the file at the provided path
     */
    public Result sendPath(Path path, boolean inline, String filename, FileMimeTypes fileMimeTypes) {
        if (path == null) {
            throw new NullPointerException("null content");
        }
        try {
            return doSendResource(
                    FileIO.fromPath(path),
                    Optional.of(Files.size(path)),
                    Optional.of(filename),
                    inline,
                    fileMimeTypes
            );
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
        return sendFile(file, StaticFileMimeTypes.fileMimeTypes());
    }

    /**
     * Sends the given file using the default inline mode.
     *
     * @param file The file to send.
     * @param fileMimeTypes Used for file type mapping.
     * @return a '200 OK' result containing the file.
     */
    public Result sendFile(File file, FileMimeTypes fileMimeTypes) {
        return sendFile(file, DEFAULT_INLINE_MODE, fileMimeTypes);
    }

    /**
     * Sends the given file.
     *
     * @param file The file to send.
     * @param inline  True if the file should be sent inline, false if it should be sent as an attachment.
     * @return a '200 OK' result containing the file
     */
    public Result sendFile(File file, boolean inline) {
        return sendFile(file, inline, StaticFileMimeTypes.fileMimeTypes());
    }

    /**
     * Sends the given file.
     *
     * @param file The file to send.
     * @param inline  True if the file should be sent inline, false if it should be sent as an attachment.
     * @param fileMimeTypes Used for file type mapping.
     * @return a '200 OK' result containing the file
     */
    public Result sendFile(File file, boolean inline, FileMimeTypes fileMimeTypes) {
        if (file == null) {
            throw new NullPointerException("null file");
        }
        return doSendResource(
                FileIO.fromPath(file.toPath()),
                Optional.of(file.length()),
                Optional.of(file.getName()),
                inline,
                fileMimeTypes
        );
    }

    /**
     * Send the given file.
     *
     * @param file The file to send.
     * @param fileName The name of the attachment
     * @return a '200 OK' result containing the file
     */
    public Result sendFile(File file, String fileName) {
        return sendFile(file, fileName, StaticFileMimeTypes.fileMimeTypes());
    }

    /**
     * Send the given file.
     *
     * @param file The file to send.
     * @param fileName The name of the attachment
     * @param fileMimeTypes Used for file type mapping.
     * @return a '200 OK' result containing the file
     */
    public Result sendFile(File file, String fileName, FileMimeTypes fileMimeTypes) {
        return sendFile(file, DEFAULT_INLINE_MODE, fileName, fileMimeTypes);
    }

    /**
     * Send the given file.
     *
     * @param file     The file to send.
     * @param fileName The name of the attachment
     * @param inline   True if the file should be sent inline, false if it should be sent as an attachment.
     * @return a '200 OK' result containing the file
     */
    public Result sendFile(File file, boolean inline, String fileName) {
        return sendFile(file, inline, fileName, StaticFileMimeTypes.fileMimeTypes());
    }

    /**
     * Send the given file.
     *
     * @param file     The file to send.
     * @param fileName The name of the attachment
     * @param inline   True if the file should be sent inline, false if it should be sent as an attachment.
     * @param fileMimeTypes Used for file type mapping.
     * @return a '200 OK' result containing the file
     */
    public Result sendFile(File file, boolean inline, String fileName, FileMimeTypes fileMimeTypes) {
        if (file == null) {
            throw new NullPointerException("null file");
        }
        return doSendResource(
                FileIO.fromPath(file.toPath()),
                Optional.of(file.length()),
                Optional.of(fileName),
                inline,
                fileMimeTypes
        );
    }

    private Result doSendResource(Source<ByteString, ?> data, Optional<Long> contentLength,
                                  Optional<String> resourceName, boolean inline, FileMimeTypes fileMimeTypes) {

        // Create a Content-Disposition header
        StringBuilder cdBuilder = new StringBuilder();
        cdBuilder.append(inline ? "inline" : "attachment");
        if (resourceName.isPresent()) {
            cdBuilder.append("; ");
            HttpHeaderParameterEncoding.encodeToBuilder("filename", resourceName.get(), cdBuilder);
        }
        Map<String, String> headers = Collections.singletonMap(
                Http.HeaderNames.CONTENT_DISPOSITION,
                cdBuilder.toString()
        );

        return new Result(status(), headers, new HttpEntity.Streamed(
                data,
                contentLength,
                resourceName.map(name -> fileMimeTypes.forFileName(name)
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
     * Send a streamed response with the given source.
     *
     * @param body the source to send
     * @param contentLength the entity content length.
     * @param contentType the entity content type.
     * @return a '200 OK' response with the given body.
     */
    public Result streamed(Source<ByteString, ?> body, Optional<Long> contentLength, Optional<String> contentType) {
        return new Result(status(), new HttpEntity.Streamed(body, contentLength, contentType));
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
     * @param json the json to send
     * @param encoding the encoding in which to encode the json (e.g. "UTF-8")
     * @return a '200 OK' result containing the json encoded with the given charset
     */
    public Result sendJson(JsonNode json, JsonEncoding encoding) {
        if (json == null) {
            throw new NullPointerException("Null content");
        }

        ObjectMapper mapper = Json.mapper();
        ByteStringBuilder builder = ByteString$.MODULE$.newBuilder();

        try {
            JsonGenerator jgen = mapper.getFactory().createGenerator(builder.asOutputStream(), encoding);

            mapper.writeValue(jgen, json);
            String contentType = MimeTypes.JSON;
            return new Result(status(), new HttpEntity.Strict(builder.result(), Optional.of(contentType)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Result sendEntity(HttpEntity entity) {
        return new Result(status(),entity);
    }
}
