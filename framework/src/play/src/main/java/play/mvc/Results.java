/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.mvc;

import akka.util.ByteString;
import play.api.libs.iteratee.Enumerator;
import play.api.mvc.*;

import play.http.HttpEntity;
import play.libs.F.*;

import play.twirl.api.Content;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;

import static play.mvc.Http.Status.*;
import static play.mvc.Http.HeaderNames.*;

/**
 * Common results.
 */
public class Results {

    private static final String UTF8 = "utf-8";
    private static final int defaultChunkSize = 1024 * 8;

    // -- Constructor methods

    /**
     * Generates a 501 NOT_IMPLEMENTED simple result.
     */
    public static final Result TODO = status(NOT_IMPLEMENTED,
            views.html.defaultpages.todo.render());

    // -- Status

    /**
     * Generates a simple result.
     *
     * @param status the HTTP status for this result e.g. 200 (OK), 404 (NOT_FOUND)
     * @return the header-only result
     */
    public static StatusHeader status(int status) {
        return new StatusHeader(status);
    }

    /**
     * Generates a simple result.
     *
     * @param status the HTTP status for this result e.g. 200 (OK), 404 (NOT_FOUND)
     * @param content the result's body content
     * @return the result
     */
    public static Result status(int status, Content content) {
        return status(status, content, UTF8);
    }

    /**
     * Generates a simple result.
     * @param status the HTTP status for this result e.g. 200 (OK), 404 (NOT_FOUND)
     * @param content the result's body content
     * @param charset the charset to encode the content with (e.g. "UTF-8")
     * @return the result
     */
    public static Result status(int status, Content content, String charset) {
        if (content == null) {
            throw new NullPointerException("Null content");
        }
        return new Result(status, HttpEntity.fromContent(content, charset));
    }

    /**
     * Generates a simple result.
     *
     * @param status the HTTP status for this result e.g. 200 (OK), 404 (NOT_FOUND)
     * @param content the result's body content. It will be encoded as a UTF-8 string.
     * @return the result
     */
    public static Result status(int status, String content) {
        return status(status, content, UTF8);
    }

    /**
     * Generates a simple result.
     *
     * @param status the HTTP status for this result e.g. 200 (OK), 404 (NOT_FOUND)
     * @param content the result's body content.
     * @param charset the charset in which to encode the content (e.g. "UTF-8")
     * @return the result
     */
    public static Result status(int status, String content, String charset) {
        if (content == null) {
            throw new NullPointerException("Null content");
        }
        return new Result(status, HttpEntity.fromString(content, charset));
    }

    /**
     * Generates a simple result with json content and UTF8 encoding.
     *
     * @param status the HTTP status for this result e.g. 200 (OK), 404 (NOT_FOUND)
     * @param content the result's body content as a play-json object
     * @return the result
     *
     */
    public static Result status(int status, JsonNode content) {
        return status(status, content, UTF8);
    }

    /**
     * Generates a simple result with json content.
     *
     * @param status the HTTP status for this result e.g. 200 (OK), 404 (NOT_FOUND)
     * @param content the result's body content, as a play-json object
     * @param charset the charset into which the json should be encoded
     * @return the result
     *
     */
    public static Result status(int status, JsonNode content, String charset) {
        if (content == null) {
            throw new NullPointerException("Null content");
        }
        return status(status).sendJson(content, charset);
    }

    /**
     * Generates a simple result with byte-array content.
     *
     * @param status the HTTP status for this result e.g. 200 (OK), 404 (NOT_FOUND)
     * @param content the result's body content, as a byte array
     * @return the result
     */
    public static Result status(int status, byte[] content) {
        if (content == null) {
            throw new NullPointerException("Null content");
        }
        return new Result(status, new HttpEntity.Strict(ByteString.fromArray(content), Optional.empty()));
    }

    /**
     * Generates a simple result.
     *
     * @param status the HTTP status for this result e.g. 200 (OK), 404 (NOT_FOUND)
     * @param content the result's body content
     * @return the result
     */
    public static Result status(int status, ByteString content) {
        if (content == null) {
            throw new NullPointerException("Null content");
        }
        return new Result(status, new HttpEntity.Strict(content, Optional.empty()));
    }

    /**
     * Generates a chunked result.
     *
     * @param status the HTTP status for this result e.g. 200 (OK), 404 (NOT_FOUND)
     * @param content the input stream containing data to chunk over
     * @return the result
     */
    public static Result status(int status, InputStream content) {
        return status(status).sendInputStream(content);
    }

    /**
     * Generates a chunked result.
     *
     * @param status the HTTP status for this result e.g. 200 (OK), 404 (NOT_FOUND)
     * @param content the input stream containing data to chunk over
     * @param contentLength the length of the provided content in bytes.
     * @return the result
     */
    public static Result status(int status, InputStream content, long contentLength) {
        return status(status).sendInputStream(content, contentLength);
    }

    /**
     * Generates a result with file contents.
     *
     * @param status the HTTP status for this result e.g. 200 (OK), 404 (NOT_FOUND)
     * @param content the file to send
     * @return the result
     */
    public static Result status(int status, File content) {
        return status(status, content, true);
    }

    /**
     * Generates a result with file content.
     *
     * @param status the HTTP status for this result e.g. 200 (OK), 404 (NOT_FOUND)
     * @param content the file to send
     * @param inline <code>true</code> to have it sent with inline Content-Disposition.
     * @return the result
     *
     */
    public static Result status(int status, File content, boolean inline) {
        return status(status).sendFile(content, inline);
    }

    /**
     * Generates a result.
     *
     * @param status the HTTP status for this result e.g. 200 (OK), 404 (NOT_FOUND)
     * @param content the file to send
     * @param fileName the name that the client should receive this file as
     * @return the result
     */
    public static Result status(int status, File content, String fileName) {
        return status(status).sendFile(content, fileName);
    }

    public static Result status(int status, Chunks<?> chunks) {
        return status(status).chunked(chunks);
    }

    /**
     * A Chunked result.
     *
     * @deprecated use {@link akka.stream.javadsl.Source} instead.
     */
    public abstract static class Chunks<A> {

        final Enumerator<A> enumerator;
        final play.api.http.Writeable<A> writable;

        public Chunks(play.api.http.Writeable<A> writable) {
            final Chunks<A> self = this;
            this.writable = writable;
            final RedeemablePromise<Object> disconnected = RedeemablePromise.<Object>empty();
            this.enumerator = play.core.j.JavaResults.chunked(
                    (channel) -> {
                        Chunks.Out<A> chunked = new Chunks.Out<A>(channel, disconnected);
                        self.onReady(chunked);
                    },
                    () -> disconnected.success(null)
            );
        }

        /**
         * Called when the Chunked stream is ready.
         *
         * @param out The out stream.
         */
        public abstract void onReady(Chunks.Out<A> out);

        /**
         * A Chunked stream.
         */
        public static class Out<A> {

            /** A Promise that will be redeemed to null when the channel is disconnected. */
            final RedeemablePromise<Object> disconnected;
            final play.api.libs.iteratee.Concurrent.Channel<A> channel;

            public Out(play.api.libs.iteratee.Concurrent.Channel<A> channel, RedeemablePromise<Object> disconnected) {
                this.channel = channel;
                this.disconnected = disconnected;
            }

            public Out(play.api.libs.iteratee.Concurrent.Channel<A> channel, List<Runnable> disconnectedCallbacks) {
                this.channel = channel;
                this.disconnected = RedeemablePromise.<Object>empty();
                for(Runnable callback: disconnectedCallbacks) {
                    onDisconnected(callback);
                }
            }

            /**
             * Write a Chunk.
             *
             * @param chunk the chunk to write
             */
            public void write(A chunk) {
                channel.push(chunk);
            }

            /**
             * Attach a callback to be called when the socket is disconnected.
             *
             * @param callback the function to run when the socket is disconnected
             */
            public void onDisconnected(final Runnable callback) {
                disconnected.onRedeem(ignored -> callback.run());
            }

            /**
             * Closes the stream.
             */
            public void close() {
                channel.eofAndEnd();
            }

        }

    }

    /**
     * Chunked result based on String chunks.
     *
     * @deprecated use {@link akka.stream.javadsl.Source} instead.
     */
    public abstract static class StringChunks extends Chunks<String> {

        private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(StringChunks.class);

        public StringChunks() {
            this(Codec.utf_8());
        }

        public StringChunks(String codec) {
            this(Codec.javaSupported(codec));
        }

        public StringChunks(Codec codec) {
            super(play.core.j.JavaResults.writeString(codec));
        }

        /**
         * Creates a StringChunks. The abstract {@code onReady} method is
         * implemented using the specified {@code Callback<Chunks.Out<String>>}.
         *
         * Uses UTF-8 by default.
         *
         * @param callback the callback used to implement onReady
         * @return a new StringChunks
         * @throws NullPointerException if the specified callback is null
         */
        public static StringChunks whenReady(Consumer<Chunks.Out<String>> callback) {
            return whenReady(Codec.utf_8(), callback);
        }

        /**
         * Creates a StringChunks. The abstract {@code onReady} method is
         * implemented using the specified {@code Callback<Chunks.Out<String>>}.
         *
         * @param codec the Codec charset used
         * @param callback the callback used to implement onReady
         * @return a new StringChunks
         * @throws NullPointerException if the specified callback is null
         */
        public static StringChunks whenReady(String codec, Consumer<Chunks.Out<String>> callback) {
            return whenReady(Codec.javaSupported(codec), callback);
        }

        /**
         * Creates a StringChunks. The abstract {@code onReady} method is
         * implemented using the specified {@code Callback<Chunks.Out<String>>}.
         *
         * @param codec the Codec used
         * @param callback the callback used to implement onReady
         * @return a new StringChunks
         * @throws NullPointerException if the specified callback is null
         */
        public static StringChunks whenReady(Codec codec, Consumer<Chunks.Out<String>> callback) {
            return new WhenReadyStringChunks(codec, callback);
        }

        /**
         * An extension of StringChunks that obtains its onReady from
         * the specified {@code Callback<Chunks.Out<String>>}.
         */
        static final class WhenReadyStringChunks extends StringChunks {

            private final Consumer<Chunks.Out<String>> callback;

            WhenReadyStringChunks(Codec codec, Consumer<Chunks.Out<String>> callback) {
                super(codec);
                if (callback == null) throw new NullPointerException("StringChunks onReady callback cannot be null");
                this.callback = callback;
            }

            @Override
            public void onReady(Chunks.Out<String> out) {
                try {
                    callback.accept(out);
                } catch (Throwable e) {
                    logger.error("Exception in StringChunks.onReady", e);
                }
            }
        }

    }

    /**
     * Chunked result based on byte[] chunks.
     *
     * @deprecated use {@link akka.stream.javadsl.Source} instead.
     */
    @Deprecated
    public abstract static class ByteChunks extends Chunks<byte[]> {

        public ByteChunks() {
            super(play.core.j.JavaResults.writeBytes());
        }

        /**
         * Creates a ByteChunks. The abstract {@code onReady} method is
         * implemented using the specified {@code Callback<Chunks.Out<byte[]>>}.
         *
         * @param callback the callback used to implement onReady
         * @return a new ByteChunks
         * @throws NullPointerException if the specified callback is null
         */
        public static ByteChunks whenReady(Consumer<Chunks.Out<byte[]>> callback) {
            return new WhenReadyByteChunks(callback);
        }

        /**
         * An extension of ByteChunks that obtains its onReady from
         * the specified {@code Callback<Chunks.Out<byte[]>>}.
         */
        static final class WhenReadyByteChunks extends ByteChunks {

            private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WhenReadyByteChunks.class);

            private final Consumer<Chunks.Out<byte[]>> callback;

            WhenReadyByteChunks(Consumer<Chunks.Out<byte[]>> callback) {
                super();
                if (callback == null) throw new NullPointerException("ByteChunks onReady callback cannot be null");
                this.callback = callback;
            }

            @Override
            public void onReady(Chunks.Out<byte[]> out) {
                try {
                    callback.accept(out);
                } catch (Throwable e) {
                    logger.error("Exception in ByteChunks.onReady", e);
                }
            }
        }

    }

    /**
     * Generates a 204 No Content result.
     *
     * @return the result
     */
    public static StatusHeader noContent() {
        return new StatusHeader(NO_CONTENT);
    }

    //////////////////////////////////////////////////////
    // EVERYTHING BELOW HERE IS GENERATED
    //
    // See https://github.com/jroper/play-source-generator
    //////////////////////////////////////////////////////


    /**
     * Generates a 200 OK result.
     *
     * @return the result
     */
    public static StatusHeader ok() {
        return new StatusHeader(OK);
    }

    /**
     * Generates a 200 OK result.
     *
     * @param content the HTTP response body
     * @return the result
     */
    public static Result ok(Content content) {
        return status(OK, content);
    }

    /**
     * Generates a 200 OK result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result ok(Content content, String charset) {
        return status(OK, content, charset);
    }

    /**
     * Generates a 200 OK result.
     *
     * @param content HTTP response body, encoded as a UTF-8 string
     * @return the result
     */
    public static Result ok(String content) {
        return status(OK, content);
    }

    /**
     * Generates a 200 OK result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result ok(String content, String charset) {
        return status(OK, content, charset);
    }

    /**
     * Generates a 200 OK result.
     *
     * @param content the result's body content as a play-json object. It will be encoded
     *        as a UTF-8 string.
     * @return the result
     */
    public static Result ok(JsonNode content) {
        return status(OK, content);
    }

    /**
     * Generates a 200 OK result.
     *
     * @param content the result's body content as a play-json object
     * @param charset the charset into which the json should be encoded
     * @return the result
     */
    public static Result ok(JsonNode content, String charset) {
        return status(OK, content, charset);
    }

    /**
     * Generates a 200 OK result.
     *
     * @param content the result's body content
     * @return the result
     */
    public static Result ok(byte[] content) {
        return status(OK, content);
    }

    /**
     * Generates a 200 OK result.
     *
     * @param content the input stream containing data to chunk over
     * @return the result
     */
    public static Result ok(InputStream content) {
        return status(OK, content);
    }

    /**
     * Generates a 200 OK result.
     *
     * @param content the input stream containing data to chunk over
     * @param contentLength the length of the provided content in bytes.
     * @return the result
     */
    public static Result ok(InputStream content, long contentLength) {
        return status(OK, content, contentLength);
    }

    /**
     * Generates a 200 OK result.
     *
     * @param content The file to send.
     * @return the result
     */
    public static Result ok(File content) {
        return status(OK, content);
    }

    /**
     * Generates a 200 OK result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     * @return the result
     */
    public static Result ok(File content, boolean inline) {
        return status(OK, content, inline);
    }

    /**
     * Generates a 200 OK result.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     * @return the result
     */
    public static Result ok(File content, String filename) {
        return status(OK, content, filename);
    }

    /**
     * Generates a 200 OK result.
     *
     * @deprecated Use {@link #ok } with {@link StatusHeader#chunked(akka.stream.javadsl.Source) }
     * instead.
     * @param chunks Deprecated
     * @return Deprecated
     */
    @Deprecated
    public static Result ok(Chunks<?> chunks) {
        return status(OK, chunks);
    }


    /**
     * Generates a 201 Created result.
     *
     * @return the result
     */
    public static StatusHeader created() {
        return new StatusHeader(CREATED);
    }

    /**
     * Generates a 201 Created result.
     *
     * @param content the HTTP response body
     * @return the result
     */
    public static Result created(Content content) {
        return status(CREATED, content);
    }

    /**
     * Generates a 201 Created result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result created(Content content, String charset) {
        return status(CREATED, content, charset);
    }

    /**
     * Generates a 201 Created result.
     *
     * @param content HTTP response body, encoded as a UTF-8 string
     * @return the result
     */
    public static Result created(String content) {
        return status(CREATED, content);
    }

    /**
     * Generates a 201 Created result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result created(String content, String charset) {
        return status(CREATED, content, charset);
    }

    /**
     * Generates a 201 Created result.
     *
     * @param content the result's body content as a play-json object. It will be encoded
     *        as a UTF-8 string.
     * @return the result
     */
    public static Result created(JsonNode content) {
        return status(CREATED, content);
    }

    /**
     * Generates a 201 Created result.
     *
     * @param content the result's body content as a play-json object
     * @param charset the charset into which the json should be encoded
     * @return the result
     */
    public static Result created(JsonNode content, String charset) {
        return status(CREATED, content, charset);
    }

    /**
     * Generates a 201 Created result.
     *
     * @param content the result's body content
     * @return the result
     */
    public static Result created(byte[] content) {
        return status(CREATED, content);
    }

    /**
     * Generates a 201 Created result.
     *
     * @param content the input stream containing data to chunk over
     * @return the result
     */
    public static Result created(InputStream content) {
        return status(CREATED, content);
    }

    /**
     * Generates a 201 Created result.
     *
     * @param content the input stream containing data to chunk over
     * @param contentLength the length of the provided content in bytes.
     * @return the result
     */
    public static Result created(InputStream content, long contentLength) {
        return status(CREATED, content, contentLength);
    }

    /**
     * Generates a 201 Created result.
     *
     * @param content The file to send.
     * @return the result
     */
    public static Result created(File content) {
        return status(CREATED, content);
    }

    /**
     * Generates a 201 Created result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     * @return the result
     */
    public static Result created(File content, boolean inline) {
        return status(CREATED, content, inline);
    }

    /**
     * Generates a 201 Created result.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     * @return the result
     */
    public static Result created(File content, String filename) {
        return status(CREATED, content, filename);
    }

    /**
     * Generates a 201 Created result.
     *
     * @deprecated Use {@link #created } with {@link StatusHeader#chunked(akka.stream.javadsl.Source) }
     * instead.
     * @param chunks Deprecated
     * @return Deprecated
     */
    @Deprecated
    public static Result created(Chunks<?> chunks) {
        return status(CREATED, chunks);
    }


    /**
     * Generates a 400 Bad Request result.
     *
     * @return the result
     */
    public static StatusHeader badRequest() {
        return new StatusHeader(BAD_REQUEST);
    }

    /**
     * Generates a 400 Bad Request result.
     *
     * @param content the HTTP response body
     * @return the result
     */
    public static Result badRequest(Content content) {
        return status(BAD_REQUEST, content);
    }

    /**
     * Generates a 400 Bad Request result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result badRequest(Content content, String charset) {
        return status(BAD_REQUEST, content, charset);
    }

    /**
     * Generates a 400 Bad Request result.
     *
     * @param content HTTP response body, encoded as a UTF-8 string
     * @return the result
     */
    public static Result badRequest(String content) {
        return status(BAD_REQUEST, content);
    }

    /**
     * Generates a 400 Bad Request result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result badRequest(String content, String charset) {
        return status(BAD_REQUEST, content, charset);
    }

    /**
     * Generates a 400 Bad Request result.
     *
     * @param content the result's body content as a play-json object. It will be encoded
     *        as a UTF-8 string.
     * @return the result
     */
    public static Result badRequest(JsonNode content) {
        return status(BAD_REQUEST, content);
    }

    /**
     * Generates a 400 Bad Request result.
     *
     * @param content the result's body content as a play-json object
     * @param charset the charset into which the json should be encoded
     * @return the result
     */
    public static Result badRequest(JsonNode content, String charset) {
        return status(BAD_REQUEST, content, charset);
    }

    /**
     * Generates a 400 Bad Request result.
     *
     * @param content the result's body content
     * @return the result
     */
    public static Result badRequest(byte[] content) {
        return status(BAD_REQUEST, content);
    }

    /**
     * Generates a 400 Bad Request result.
     *
     * @param content the input stream containing data to chunk over
     * @return the result
     */
    public static Result badRequest(InputStream content) {
        return status(BAD_REQUEST, content);
    }

    /**
     * Generates a 400 Bad Request result.
     *
     * @param content the input stream containing data to chunk over
     * @param contentLength the length of the provided content in bytes.
     * @return the result
     */
    public static Result badRequest(InputStream content, long contentLength) {
        return status(BAD_REQUEST, content, contentLength);
    }

    /**
     * Generates a 400 Bad Request result.
     *
     * @param content The file to send.
     * @return the result
     */
    public static Result badRequest(File content) {
        return status(BAD_REQUEST, content);
    }

    /**
     * Generates a 400 Bad Request result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     * @return the result
     */
    public static Result badRequest(File content, boolean inline) {
        return status(BAD_REQUEST, content, inline);
    }

    /**
     * Generates a 400 Bad Request result.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     * @return the result
     */
    public static Result badRequest(File content, String filename) {
        return status(BAD_REQUEST, content, filename);
    }

    /**
     * Generates a 400 Bad Request result.
     *
     * @deprecated Use {@link #badRequest } with {@link StatusHeader#chunked(akka.stream.javadsl.Source) }
     * instead.
     * @param chunks Deprecated
     * @return Deprecated
     */
    @Deprecated
    public static Result badRequest(Chunks<?> chunks) {
        return status(BAD_REQUEST, chunks);
    }


    /**
     * Generates a 401 Unauthorized result.
     *
     * @return the result
     */
    public static StatusHeader unauthorized() {
        return new StatusHeader(UNAUTHORIZED);
    }

    /**
     * Generates a 401 Unauthorized result.
     *
     * @param content the HTTP response body
     * @return the result
     */
    public static Result unauthorized(Content content) {
        return status(UNAUTHORIZED, content);
    }

    /**
     * Generates a 401 Unauthorized result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result unauthorized(Content content, String charset) {
        return status(UNAUTHORIZED, content, charset);
    }

    /**
     * Generates a 401 Unauthorized result.
     *
     * @param content HTTP response body, encoded as a UTF-8 string
     * @return the result
     */
    public static Result unauthorized(String content) {
        return status(UNAUTHORIZED, content);
    }

    /**
     * Generates a 401 Unauthorized result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result unauthorized(String content, String charset) {
        return status(UNAUTHORIZED, content, charset);
    }

    /**
     * Generates a 401 Unauthorized result.
     *
     * @param content the result's body content as a play-json object. It will be encoded
     *        as a UTF-8 string.
     * @return the result
     */
    public static Result unauthorized(JsonNode content) {
        return status(UNAUTHORIZED, content);
    }

    /**
     * Generates a 401 Unauthorized result.
     *
     * @param content the result's body content as a play-json object
     * @param charset the charset into which the json should be encoded
     * @return the result
     */
    public static Result unauthorized(JsonNode content, String charset) {
        return status(UNAUTHORIZED, content, charset);
    }

    /**
     * Generates a 401 Unauthorized result.
     *
     * @param content the result's body content
     * @return the result
     */
    public static Result unauthorized(byte[] content) {
        return status(UNAUTHORIZED, content);
    }

    /**
     * Generates a 401 Unauthorized result.
     *
     * @param content the input stream containing data to chunk over
     * @return the result
     */
    public static Result unauthorized(InputStream content) {
        return status(UNAUTHORIZED, content);
    }

    /**
     * Generates a 401 Unauthorized result.
     *
     * @param content the input stream containing data to chunk over
     * @param contentLength the length of the provided content in bytes.
     * @return the result
     */
    public static Result unauthorized(InputStream content, long contentLength) {
        return status(UNAUTHORIZED, content, contentLength);
    }

    /**
     * Generates a 401 Unauthorized result.
     *
     * @param content The file to send.
     * @return the result
     */
    public static Result unauthorized(File content) {
        return status(UNAUTHORIZED, content);
    }

    /**
     * Generates a 401 Unauthorized result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     * @return the result
     */
    public static Result unauthorized(File content, boolean inline) {
        return status(UNAUTHORIZED, content, inline);
    }

    /**
     * Generates a 401 Unauthorized result.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     * @return the result
     */
    public static Result unauthorized(File content, String filename) {
        return status(UNAUTHORIZED, content, filename);
    }

    /**
     * Generates a 401 Unauthorized result.
     *
     * @deprecated Use {@link #unauthorized } with {@link StatusHeader#chunked(akka.stream.javadsl.Source) }
     * instead.
     * @param chunks Deprecated
     * @return Deprecated
     */
    @Deprecated
    public static Result unauthorized(Chunks<?> chunks) {
        return status(UNAUTHORIZED, chunks);
    }


    /**
     * Generates a 402 Payment Required result.
     *
     * @return the result
     */
    public static StatusHeader paymentRequired() {
        return new StatusHeader(PAYMENT_REQUIRED);
    }

    /**
     * Generates a 402 Payment Required result.
     *
     * @param content the HTTP response body
     * @return the result
     */
    public static Result paymentRequired(Content content) {
        return status(PAYMENT_REQUIRED, content);
    }

    /**
     * Generates a 402 Payment Required result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result paymentRequired(Content content, String charset) {
        return status(PAYMENT_REQUIRED, content, charset);
    }

    /**
     * Generates a 402 Payment Required result.
     *
     * @param content HTTP response body, encoded as a UTF-8 string
     * @return the result
     */
    public static Result paymentRequired(String content) {
        return status(PAYMENT_REQUIRED, content);
    }

    /**
     * Generates a 402 Payment Required result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result paymentRequired(String content, String charset) {
        return status(PAYMENT_REQUIRED, content, charset);
    }

    /**
     * Generates a 402 Payment Required result.
     *
     * @param content the result's body content as a play-json object. It will be encoded
     *        as a UTF-8 string.
     * @return the result
     */
    public static Result paymentRequired(JsonNode content) {
        return status(PAYMENT_REQUIRED, content);
    }

    /**
     * Generates a 402 Payment Required result.
     *
     * @param content the result's body content as a play-json object
     * @param charset the charset into which the json should be encoded
     * @return the result
     */
    public static Result paymentRequired(JsonNode content, String charset) {
        return status(PAYMENT_REQUIRED, content, charset);
    }

    /**
     * Generates a 402 Payment Required result.
     *
     * @param content the result's body content
     * @return the result
     */
    public static Result paymentRequired(byte[] content) {
        return status(PAYMENT_REQUIRED, content);
    }

    /**
     * Generates a 402 Payment Required result.
     *
     * @param content the input stream containing data to chunk over
     * @return the result
     */
    public static Result paymentRequired(InputStream content) {
        return status(PAYMENT_REQUIRED, content);
    }

    /**
     * Generates a 402 Payment Required result.
     *
     * @param content the input stream containing data to chunk over
     * @param contentLength the length of the provided content in bytes.
     * @return the result
     */
    public static Result paymentRequired(InputStream content, long contentLength) {
        return status(PAYMENT_REQUIRED, content, contentLength);
    }

    /**
     * Generates a 402 Payment Required result.
     *
     * @param content The file to send.
     * @return the result
     */
    public static Result paymentRequired(File content) {
        return status(PAYMENT_REQUIRED, content);
    }

    /**
     * Generates a 402 Payment Required result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     * @return the result
     */
    public static Result paymentRequired(File content, boolean inline) {
        return status(PAYMENT_REQUIRED, content, inline);
    }

    /**
     * Generates a 402 Payment Required result.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     * @return the result
     */
    public static Result paymentRequired(File content, String filename) {
        return status(PAYMENT_REQUIRED, content, filename);
    }

    /**
     * Generates a 402 Payment Required result.
     *
     * @deprecated Use {@link #paymentRequired } with {@link StatusHeader#chunked(akka.stream.javadsl.Source) }
     * instead.
     * @param chunks Deprecated
     * @return Deprecated
     */
    @Deprecated
    public static Result paymentRequired(Chunks<?> chunks) {
        return status(PAYMENT_REQUIRED, chunks);
    }


    /**
     * Generates a 403 Forbidden result.
     *
     * @return the result
     */
    public static StatusHeader forbidden() {
        return new StatusHeader(FORBIDDEN);
    }

    /**
     * Generates a 403 Forbidden result.
     *
     * @param content the HTTP response body
     * @return the result
     */
    public static Result forbidden(Content content) {
        return status(FORBIDDEN, content);
    }

    /**
     * Generates a 403 Forbidden result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result forbidden(Content content, String charset) {
        return status(FORBIDDEN, content, charset);
    }

    /**
     * Generates a 403 Forbidden result.
     *
     * @param content HTTP response body, encoded as a UTF-8 string
     * @return the result
     */
    public static Result forbidden(String content) {
        return status(FORBIDDEN, content);
    }

    /**
     * Generates a 403 Forbidden result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result forbidden(String content, String charset) {
        return status(FORBIDDEN, content, charset);
    }

    /**
     * Generates a 403 Forbidden result.
     *
     * @param content the result's body content as a play-json object. It will be encoded
     *        as a UTF-8 string.
     * @return the result
     */
    public static Result forbidden(JsonNode content) {
        return status(FORBIDDEN, content);
    }

    /**
     * Generates a 403 Forbidden result.
     *
     * @param content the result's body content as a play-json object
     * @param charset the charset into which the json should be encoded
     * @return the result
     */
    public static Result forbidden(JsonNode content, String charset) {
        return status(FORBIDDEN, content, charset);
    }

    /**
     * Generates a 403 Forbidden result.
     *
     * @param content the result's body content
     * @return the result
     */
    public static Result forbidden(byte[] content) {
        return status(FORBIDDEN, content);
    }

    /**
     * Generates a 403 Forbidden result.
     *
     * @param content the input stream containing data to chunk over
     * @return the result
     */
    public static Result forbidden(InputStream content) {
        return status(FORBIDDEN, content);
    }

    /**
     * Generates a 403 Forbidden result.
     *
     * @param content the input stream containing data to chunk over
     * @param contentLength the length of the provided content in bytes.
     * @return the result
     */
    public static Result forbidden(InputStream content, long contentLength) {
        return status(FORBIDDEN, content, contentLength);
    }

    /**
     * Generates a 403 Forbidden result.
     *
     * @param content The file to send.
     * @return the result
     */
    public static Result forbidden(File content) {
        return status(FORBIDDEN, content);
    }

    /**
     * Generates a 403 Forbidden result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     * @return the result
     */
    public static Result forbidden(File content, boolean inline) {
        return status(FORBIDDEN, content, inline);
    }

    /**
     * Generates a 403 Forbidden result.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     * @return the result
     */
    public static Result forbidden(File content, String filename) {
        return status(FORBIDDEN, content, filename);
    }

    /**
     * Generates a 403 Forbidden result.
     *
     * @deprecated Use {@link #forbidden } with {@link StatusHeader#chunked(akka.stream.javadsl.Source) }
     * instead.
     * @param chunks Deprecated
     * @return Deprecated
     */
    @Deprecated
    public static Result forbidden(Chunks<?> chunks) {
        return status(FORBIDDEN, chunks);
    }


    /**
     * Generates a 404 Not Found result.
     *
     * @return the result
     */
    public static StatusHeader notFound() {
        return new StatusHeader(NOT_FOUND);
    }

    /**
     * Generates a 404 Not Found result.
     *
     * @param content the HTTP response body
     * @return the result
     */
    public static Result notFound(Content content) {
        return status(NOT_FOUND, content);
    }

    /**
     * Generates a 404 Not Found result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result notFound(Content content, String charset) {
        return status(NOT_FOUND, content, charset);
    }

    /**
     * Generates a 404 Not Found result.
     *
     * @param content HTTP response body, encoded as a UTF-8 string
     * @return the result
     */
    public static Result notFound(String content) {
        return status(NOT_FOUND, content);
    }

    /**
     * Generates a 404 Not Found result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result notFound(String content, String charset) {
        return status(NOT_FOUND, content, charset);
    }

    /**
     * Generates a 404 Not Found result.
     *
     * @param content the result's body content as a play-json object. It will be encoded
     *        as a UTF-8 string.
     * @return the result
     */
    public static Result notFound(JsonNode content) {
        return status(NOT_FOUND, content);
    }

    /**
     * Generates a 404 Not Found result.
     *
     * @param content the result's body content as a play-json object
     * @param charset the charset into which the json should be encoded
     * @return the result
     */
    public static Result notFound(JsonNode content, String charset) {
        return status(NOT_FOUND, content, charset);
    }

    /**
     * Generates a 404 Not Found result.
     *
     * @param content the result's body content
     * @return the result
     */
    public static Result notFound(byte[] content) {
        return status(NOT_FOUND, content);
    }

    /**
     * Generates a 404 Not Found result.
     *
     * @param content the input stream containing data to chunk over
     * @return the result
     */
    public static Result notFound(InputStream content) {
        return status(NOT_FOUND, content);
    }

    /**
     * Generates a 404 Not Found result.
     *
     * @param content the input stream containing data to chunk over
     * @param contentLength the length of the provided content in bytes.
     * @return the result
     */
    public static Result notFound(InputStream content, long contentLength) {
        return status(NOT_FOUND, content, contentLength);
    }

    /**
     * Generates a 404 Not Found result.
     *
     * @param content The file to send.
     * @return the result
     */
    public static Result notFound(File content) {
        return status(NOT_FOUND, content);
    }

    /**
     * Generates a 404 Not Found result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     * @return the result
     */
    public static Result notFound(File content, boolean inline) {
        return status(NOT_FOUND, content, inline);
    }

    /**
     * Generates a 404 Not Found result.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     * @return the result
     */
    public static Result notFound(File content, String filename) {
        return status(NOT_FOUND, content, filename);
    }

    /**
     * Generates a 404 Not Found result.
     *
     * @deprecated Use {@link #notFound } with {@link StatusHeader#chunked(akka.stream.javadsl.Source) }
     * instead.
     * @param chunks Deprecated
     * @return Deprecated
     */
    @Deprecated
    public static Result notFound(Chunks<?> chunks) {
        return status(NOT_FOUND, chunks);
    }


    /**
     * Generates a 500 Internal Server Error result.
     *
     * @return the result
     */
    public static StatusHeader internalServerError() {
        return new StatusHeader(INTERNAL_SERVER_ERROR);
    }

    /**
     * Generates a 500 Internal Server Error result.
     *
     * @param content the HTTP response body
     * @return the result
     */
    public static Result internalServerError(Content content) {
        return status(INTERNAL_SERVER_ERROR, content);
    }

    /**
     * Generates a 500 Internal Server Error result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result internalServerError(Content content, String charset) {
        return status(INTERNAL_SERVER_ERROR, content, charset);
    }

    /**
     * Generates a 500 Internal Server Error result.
     *
     * @param content HTTP response body, encoded as a UTF-8 string
     * @return the result
     */
    public static Result internalServerError(String content) {
        return status(INTERNAL_SERVER_ERROR, content);
    }

    /**
     * Generates a 500 Internal Server Error result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result internalServerError(String content, String charset) {
        return status(INTERNAL_SERVER_ERROR, content, charset);
    }

    /**
     * Generates a 500 Internal Server Error result.
     *
     * @param content the result's body content as a play-json object. It will be encoded
     *        as a UTF-8 string.
     * @return the result
     */
    public static Result internalServerError(JsonNode content) {
        return status(INTERNAL_SERVER_ERROR, content);
    }

    /**
     * Generates a 500 Internal Server Error result.
     *
     * @param content the result's body content as a play-json object
     * @param charset the charset into which the json should be encoded
     * @return the result
     */
    public static Result internalServerError(JsonNode content, String charset) {
        return status(INTERNAL_SERVER_ERROR, content, charset);
    }

    /**
     * Generates a 500 Internal Server Error result.
     *
     * @param content the result's body content
     * @return the result
     */
    public static Result internalServerError(byte[] content) {
        return status(INTERNAL_SERVER_ERROR, content);
    }

    /**
     * Generates a 500 Internal Server Error result.
     *
     * @param content the input stream containing data to chunk over
     * @return the result
     */
    public static Result internalServerError(InputStream content) {
        return status(INTERNAL_SERVER_ERROR, content);
    }

    /**
     * Generates a 500 Internal Server Error result.
     *
     * @param content the input stream containing data to chunk over
     * @param contentLength the length of the provided content in bytes.
     * @return the result
     */
    public static Result internalServerError(InputStream content, long contentLength) {
        return status(INTERNAL_SERVER_ERROR, content, contentLength);
    }

    /**
     * Generates a 500 Internal Server Error result.
     *
     * @param content The file to send.
     * @return the result
     */
    public static Result internalServerError(File content) {
        return status(INTERNAL_SERVER_ERROR, content);
    }

    /**
     * Generates a 500 Internal Server Error result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     * @return the result
     */
    public static Result internalServerError(File content, boolean inline) {
        return status(INTERNAL_SERVER_ERROR, content, inline);
    }

    /**
     * Generates a 500 Internal Server Error result.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     * @return the result
     */
    public static Result internalServerError(File content, String filename) {
        return status(INTERNAL_SERVER_ERROR, content, filename);
    }

    /**
     * Generates a 500 Internal Server Error result.
     *
     * @deprecated Use {@link #internalServerError } with {@link StatusHeader#chunked(akka.stream.javadsl.Source) }
     * instead.
     * @param chunks Deprecated
     * @return Deprecated
     */
    @Deprecated
    public static Result internalServerError(Chunks<?> chunks) {
        return status(INTERNAL_SERVER_ERROR, chunks);
    }

    /**
     * Generates a 301 Moved Permanently result.
     *
     * @param url The url to redirect.
     * @return the result
     */
    public static Result movedPermanently(String url) {
        return new Result(MOVED_PERMANENTLY, Collections.singletonMap(LOCATION, url));
    }

    /**
     * Generates a 301 Moved Permanently result.
     *
     * @param call Call defining the url to redirect (typically comes from reverse router).
     * @return the result
     */
    public static Result movedPermanently(Call call) {
        return new Result(MOVED_PERMANENTLY, Collections.singletonMap(LOCATION, call.url()));
    }

    /**
     * Generates a 302 Found result.
     *
     * @param url The url to redirect.
     * @return the result
     */
    public static Result found(String url) {
        return new Result(FOUND, Collections.singletonMap(LOCATION, url));
    }

    /**
     * Generates a 302 Found result.
     *
     * @param call Call defining the url to redirect (typically comes from reverse router).
     * @return the result
     */
    public static Result found(Call call) {
        return new Result(FOUND, Collections.singletonMap(LOCATION, call.url()));
    }

    /**
     * Generates a 303 See Other result.
     *
     * @param url The url to redirect.
     * @return the result
     */
    public static Result seeOther(String url) {
        return new Result(SEE_OTHER, Collections.singletonMap(LOCATION, url));
    }

    /**
     * Generates a 303 See Other result.
     *
     * @param call Call defining the url to redirect (typically comes from reverse router).
     * @return the result
     */
    public static Result seeOther(Call call) {
        return new Result(SEE_OTHER, Collections.singletonMap(LOCATION, call.url()));
    }

    /**
     * Generates a 303 See Other result.
     *
     * @param url The url to redirect.
     * @return the result
     */
    public static Result redirect(String url) {
        return new Result(SEE_OTHER, Collections.singletonMap(LOCATION, url));
    }

    /**
     * Generates a 303 See Other result.
     *
     * @param call Call defining the url to redirect (typically comes from reverse router).
     * @return the result
     */
    public static Result redirect(Call call) {
        return new Result(SEE_OTHER, Collections.singletonMap(LOCATION, call.url()));
    }

    /**
     * Generates a 307 Temporary Redirect result.
     *
     * @param url The url to redirect.
     * @return the result
     */
    public static Result temporaryRedirect(String url) {
        return new Result(TEMPORARY_REDIRECT, Collections.singletonMap(LOCATION, url));
    }

    /**
     * Generates a 307 Temporary Redirect result.
     *
     * @param call Call defining the url to redirect (typically comes from reverse router).
     * @return the result
     */
    public static Result temporaryRedirect(Call call) {
        return new Result(TEMPORARY_REDIRECT, Collections.singletonMap(LOCATION, call.url()));
    }

    /**
     * Generates a 308 Permanent Redirect result.
     *
     * @param url The url to redirect.
     * @return the result
     */
    public static Result permanentRedirect(String url) {
        return new Result(PERMANENT_REDIRECT, Collections.singletonMap(LOCATION, url));
    }

    /**
     * Generates a 308 Permanent Redirect result.
     *
     * @param call Call defining the url to redirect (typically comes from reverse router).
     * @return the result
     */
    public static Result permanentRedirect(Call call) {
        return new Result(PERMANENT_REDIRECT, Collections.singletonMap(LOCATION, call.url()));
    }

}
