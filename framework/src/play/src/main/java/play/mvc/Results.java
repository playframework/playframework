package play.mvc;

import play.api.libs.iteratee.Concurrent;
import play.api.libs.iteratee.Enumerator;
import play.api.mvc.*;

import play.core.j.JavaResults;
import play.libs.*;
import play.libs.F.*;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;


/**
 * Common results.
 */
public class Results {

    static Codec utf8 = Codec.javaSupported("utf-8");
    static int defaultChunkSize = 1024 * 8;

    // -- Constructor methods

    /**
     * Generates a 501 NOT_IMPLEMENTED simple result.
     */
    public static Result TODO = new Todo();

    /**
     * Handles an Asynchronous result.
     *
     * @deprecated Return Promise&lt;Result&gt; from your action instead
     */
    @Deprecated
    public static <R extends Result> AsyncResult async(play.libs.F.Promise<R> p) {
        return new AsyncResult(p.flatMap(new Function<R, play.libs.F.Promise<SimpleResult>>() {
            @Override
            public play.libs.F.Promise<SimpleResult> apply(R result) throws Throwable {
                if (result instanceof AsyncResult) {
                    return ((AsyncResult)result).promise;
                } else {
                    // Must be a simple result
                    return play.libs.F.Promise.pure((SimpleResult) result);
                }
            }
        }));
    }

    // -- Status

    /**
     * Generates a simple result.
     */
    public static Status status(int status) {
        return new Status(play.core.j.JavaResults.Status(status));
    }

    /**
     * Generates a simple result.
     */
    public static Status status(int status, Content content) {
        return new Status(play.core.j.JavaResults.Status(status), content, utf8);
    }

    /**
     * Generates a simple result.
     */
    public static Status status(int status, Content content, String charset) {
        return new Status(play.core.j.JavaResults.Status(status), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a simple result.
     */
    public static Status status(int status, String content) {
        return new Status(play.core.j.JavaResults.Status(status), content, utf8);
    }

    /**
     * Generates a simple result.
     */
    public static Status status(int status, String content, String charset) {
        return new Status(play.core.j.JavaResults.Status(status), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a simple result.
     */
    public static Status status(int status, JsonNode content) {
        return new Status(play.core.j.JavaResults.Status(status), content, utf8);
    }

    /**
     * Generates a simple result.
     */
    public static Status status(int status, JsonNode content, String charset) {
        return new Status(play.core.j.JavaResults.Status(status), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a simple result.
     */
    public static Status status(int status, byte[] content) {
        return new Status(play.core.j.JavaResults.Status(status), content);
    }

    /**
     * Generates a chunked result.
     */
    public static Status status(int status, InputStream content) {
        return status(status, content, defaultChunkSize);
    }

    /**
     * Generates a chunked result.
     */
    public static Status status(int status, InputStream content, int chunkSize) {
        return new Status(play.core.j.JavaResults.Status(status), content, chunkSize);
    }

    /**
     * Generates a chunked result.
     */
    public static Status status(int status, File content) {
        return new Status(play.core.j.JavaResults.Status(status), content);
    }

    /**
     * Generates a simple result.
     */
    public static Status status(int status, File content, int chunkSize) {
        return new Status(play.core.j.JavaResults.Status(status), content, chunkSize);
    }

    /**
     * Generates a chunked result.
     */
    public static Status status(int status, Chunks<?> chunks) {
        return new Status(play.core.j.JavaResults.Status(status), chunks);
    }

    // -- OK

    /**
     * Generates a 200 OK simple result.
     */
    public static Status ok() {
        return new Status(play.core.j.JavaResults.Ok());
    }

    /**
     * Generates a 200 OK simple result.
     */
    public static Status ok(Content content) {
        return new Status(play.core.j.JavaResults.Ok(), content, utf8);
    }

    /**
     * Generates a 200 OK simple result.
     */
    public static Status ok(Content content, String charset) {
        return new Status(play.core.j.JavaResults.Ok(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 200 OK simple result.
     */
    public static Status ok(String content) {
        return new Status(play.core.j.JavaResults.Ok(), content, utf8);
    }

    /**
     * Generates a 200 OK simple result.
     */
    public static Status ok(String content, String charset) {
        return new Status(play.core.j.JavaResults.Ok(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 200 OK simple result.
     */
    public static Status ok(JsonNode content) {
        return new Status(play.core.j.JavaResults.Ok(), content, utf8);
    }

    /**
     * Generates a 200 OK simple result.
     */
    public static Status ok(JsonNode content, String charset) {
        return new Status(play.core.j.JavaResults.Ok(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 200 OK simple result.
     */
    public static Status ok(byte[] content) {
        return new Status(play.core.j.JavaResults.Ok(), content);
    }

    /**
     * Generates a 200 OK chunked result.
     */
    public static Status ok(InputStream content) {
        return ok(content, defaultChunkSize);
    }

    /**
     * Generates a 200 OK chunked result.
     */
    public static Status ok(InputStream content, int chunkSize) {
        return new Status(play.core.j.JavaResults.Ok(), content, chunkSize);
    }

    /**
     * Generates a 200 OK file result as an attachment.
     *
     * @param content The file to send.
     */
    public static Status ok(File content) {
        return new Status(play.core.j.JavaResults.Ok(), content);
    }

    /**
     * Generates a 200 OK file result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     */
    public static Status ok(File content, boolean inline) {
        return new Status(JavaResults.Ok(), content, inline);
    }

    /**
     * Generates a 200 OK file result as an attachment.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     */
    public static Status ok(File content, String filename) {
        return new Status(JavaResults.Ok(), content, true, filename);
    }

    /**
     * Generates a 200 OK file result, sent as a chunked response.
     *
     * @deprecated Since the length of the file is known, there is little reason to send a file as chunked.
     */
    @Deprecated
    public static Status ok(File content, int chunkSize) {
        return new Status(play.core.j.JavaResults.Ok(), content, chunkSize);
    }

    /**
     * Generates a 200 OK chunked result.
     */
    public static Status ok(Chunks<?> chunks) {
        return new Status(play.core.j.JavaResults.Ok(), chunks);
    }

    // -- CREATED

    /**
     * Generates a 201 CREATED simple result.
     */
    public static Status created() {
        return new Status(play.core.j.JavaResults.Created());
    }

    /**
     * Generates a 201 CREATED simple result.
     */
    public static Status created(Content content) {
        return new Status(play.core.j.JavaResults.Created(), content, utf8);
    }

    /**
     * Generates a 201 CREATED simple result.
     */
    public static Status created(Content content, String charset) {
        return new Status(play.core.j.JavaResults.Created(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 201 CREATED simple result.
     */
    public static Status created(String content) {
        return new Status(play.core.j.JavaResults.Created(), content, utf8);
    }

    /**
     * Generates a 201 CREATED simple result.
     */
    public static Status created(String content, String charset) {
        return new Status(play.core.j.JavaResults.Created(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 201 CREATED simple result.
     */
    public static Status created(JsonNode content) {
        return new Status(play.core.j.JavaResults.Created(), content, utf8);
    }

    /**
     * Generates a 201 CREATED simple result.
     */
    public static Status created(JsonNode content, String charset) {
        return new Status(play.core.j.JavaResults.Created(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 201 CREATED simple result.
     */
    public static Status created(byte[] content) {
        return new Status(play.core.j.JavaResults.Created(), content);
    }

    /**
     * Generates a 201 CREATED chunked result.
     */
    public static Status created(InputStream content) {
        return created(content, defaultChunkSize);
    }

    /**
     * Generates a 201 CREATED chunked result.
     */
    public static Status created(InputStream content, int chunkSize) {
        return new Status(play.core.j.JavaResults.Created(), content, chunkSize);
    }

    /**
     * Generates a 201 CREATED file result as an attachment.
     *
     * @param content The file to send.
     */
    public static Status created(File content) {
        return new Status(play.core.j.JavaResults.Created(), content);
    }

    /**
     * Generates a 201 CREATED file result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     */
    public static Status created(File content, boolean inline) {
        return new Status(JavaResults.Created(), content, inline);
    }

    /**
     * Generates a 201 CREATED file result as an attachment.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     */
    public static Status created(File content, String filename) {
        return new Status(JavaResults.Created(), content, true, filename);
    }

    /**
     * Generates a 201 CREATED file result, sent as a chunked response.
     *
     * @deprecated Since the length of the file is known, there is little reason to send a file as chunked.
     */
    @Deprecated
    public static Status created(File content, int chunkSize) {
        return new Status(play.core.j.JavaResults.Created(), content, chunkSize);
    }

    /**
     * Generates a 201 CREATED chunked result.
     */
    public static Status created(Chunks<?> chunks) {
        return new Status(play.core.j.JavaResults.Created(), chunks);
    }

    // -- NO_CONTENT

    /**
     * Generates a 204 NO_CONTENT simple result.
     */
    public static Status noContent() {
        return new Status(play.core.j.JavaResults.Status(204));
    }

    // -- INTERNAL_SERVER_ERROR

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR simple result.
     */
    public static Status internalServerError() {
        return new Status(play.core.j.JavaResults.InternalServerError());
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR simple result.
     */
    public static Status internalServerError(Content content) {
        return new Status(play.core.j.JavaResults.InternalServerError(), content, utf8);
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR simple result.
     */
    public static Status internalServerError(Content content, String charset) {
        return new Status(play.core.j.JavaResults.InternalServerError(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR simple result.
     */
    public static Status internalServerError(String content) {
        return new Status(play.core.j.JavaResults.InternalServerError(), content, utf8);
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR simple result.
     */
    public static Status internalServerError(String content, String charset) {
        return new Status(play.core.j.JavaResults.InternalServerError(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR simple result.
     */
    public static Status internalServerError(JsonNode content) {
        return new Status(play.core.j.JavaResults.InternalServerError(), content, utf8);
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR simple result.
     */
    public static Status internalServerError(JsonNode content, String charset) {
        return new Status(play.core.j.JavaResults.InternalServerError(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR simple result.
     */
    public static Status internalServerError(byte[] content) {
        return new Status(play.core.j.JavaResults.InternalServerError(), content);
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR chunked result.
     */
    public static Status internalServerError(InputStream content) {
        return internalServerError(content, defaultChunkSize);
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR chunked result.
     */
    public static Status internalServerError(InputStream content, int chunkSize) {
        return new Status(play.core.j.JavaResults.InternalServerError(), content, chunkSize);
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR file result as an attachment.
     *
     * @param content The file to send.
     */
    public static Status internalServerError(File content) {
        return new Status(play.core.j.JavaResults.InternalServerError(), content);
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR file result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     */
    public static Status internalServerError(File content, boolean inline) {
        return new Status(JavaResults.InternalServerError(), content, inline);
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR file result as an attachment.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     */
    public static Status internalServerError(File content, String filename) {
        return new Status(JavaResults.InternalServerError(), content, true, filename);
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR file result, sent as a chunked response.
     *
     * @deprecated Since the length of the file is known, there is little reason to send a file as chunked.
     */
    @Deprecated
    public static Status internalServerError(File content, int chunkSize) {
        return new Status(play.core.j.JavaResults.InternalServerError(), content, chunkSize);
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR chunked result.
     */
    public static Status internalServerError(Chunks<?> chunks) {
        return new Status(play.core.j.JavaResults.InternalServerError(), chunks);
    }

    // -- NOT_FOUND

    /**
     * Generates a 404 NOT_FOUND simple result.
     */
    public static Status notFound() {
        return new Status(play.core.j.JavaResults.NotFound());
    }

    /**
     * Generates a 404 NOT_FOUND simple result.
     */
    public static Status notFound(Content content) {
        return new Status(play.core.j.JavaResults.NotFound(), content, utf8);
    }

    /**
     * Generates a 404 NOT_FOUND simple result.
     */
    public static Status notFound(Content content, String charset) {
        return new Status(play.core.j.JavaResults.NotFound(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 404 NOT_FOUND simple result.
     */
    public static Status notFound(String content) {
        return new Status(play.core.j.JavaResults.NotFound(), content, utf8);
    }

    /**
     * Generates a 404 NOT_FOUND simple result.
     */
    public static Status notFound(String content, String charset) {
        return new Status(play.core.j.JavaResults.NotFound(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 404 NOT_FOUND simple result.
     */
    public static Status notFound(JsonNode content) {
        return new Status(play.core.j.JavaResults.NotFound(), content, utf8);
    }

    /**
     * Generates a 404 NOT_FOUND simple result.
     */
    public static Status notFound(JsonNode content, String charset) {
        return new Status(play.core.j.JavaResults.NotFound(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 404 NOT_FOUND simple result.
     */
    public static Status notFound(byte[] content) {
        return new Status(play.core.j.JavaResults.NotFound(), content);
    }

    /**
     * Generates a 404 NOT_FOUND chunked result.
     */
    public static Status notFound(InputStream content) {
        return notFound(content, defaultChunkSize);
    }

    /**
     * Generates a 404 NOT_FOUND chunked result.
     */
    public static Status notFound(InputStream content, int chunkSize) {
        return new Status(play.core.j.JavaResults.NotFound(), content, chunkSize);
    }

    /**
     * Generates a 404 NOT_FOUND file result as an attachment.
     *
     * @param content The file to send.
     */
    public static Status notFound(File content) {
        return new Status(play.core.j.JavaResults.NotFound(), content);
    }

    /**
     * Generates a 404 NOT_FOUND file result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     */
    public static Status notFound(File content, boolean inline) {
        return new Status(JavaResults.NotFound(), content, inline);
    }

    /**
     * Generates a 404 NOT_FOUND file result as an attachment.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     */
    public static Status notFound(File content, String filename) {
        return new Status(JavaResults.NotFound(), content, true, filename);
    }

    /**
     * Generates a 404 NOT_FOUND file result, sent as a chunked response.
     *
     * @deprecated Since the length of the file is known, there is little reason to send a file as chunked.
     */
    @Deprecated
    public static Status notFound(File content, int chunkSize) {
        return new Status(play.core.j.JavaResults.NotFound(), content, chunkSize);
    }

    /**
     * Generates a 404 NOT_FOUND chunked result.
     */
    public static Status notFound(Chunks<?> chunks) {
        return new Status(play.core.j.JavaResults.NotFound(), chunks);
    }

    // -- FORBIDDEN

    /**
     * Generates a 403 FORBIDDEN simple result.
     */
    public static Status forbidden() {
        return new Status(play.core.j.JavaResults.Forbidden());
    }

    /**
     * Generates a 403 FORBIDDEN simple result.
     */
    public static Status forbidden(Content content) {
        return new Status(play.core.j.JavaResults.Forbidden(), content, utf8);
    }

    /**
     * Generates a 403 FORBIDDEN simple result.
     */
    public static Status forbidden(Content content, String charset) {
        return new Status(play.core.j.JavaResults.Forbidden(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 403 FORBIDDEN simple result.
     */
    public static Status forbidden(String content) {
        return new Status(play.core.j.JavaResults.Forbidden(), content, utf8);
    }

    /**
     * Generates a 403 FORBIDDEN simple result.
     */
    public static Status forbidden(String content, String charset) {
        return new Status(play.core.j.JavaResults.Forbidden(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 403 FORBIDDEN simple result.
     */
    public static Status forbidden(JsonNode content) {
        return new Status(play.core.j.JavaResults.Forbidden(), content, utf8);
    }

    /**
     * Generates a 403 FORBIDDEN simple result.
     */
    public static Status forbidden(JsonNode content, String charset) {
        return new Status(play.core.j.JavaResults.Forbidden(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 403 FORBIDDEN simple result.
     */
    public static Status forbidden(byte[] content) {
        return new Status(play.core.j.JavaResults.Forbidden(), content);
    }

    /**
     * Generates a 403 FORBIDDEN chunked result.
     */
    public static Status forbidden(InputStream content) {
        return forbidden(content, defaultChunkSize);
    }

    /**
     * Generates a 403 FORBIDDEN chunked result.
     */
    public static Status forbidden(InputStream content, int chunkSize) {
        return new Status(play.core.j.JavaResults.Forbidden(), content, chunkSize);
    }

    /**
     * Generates a 403 FORBIDDEN file result as an attachment.
     *
     * @param content The file to send.
     */
    public static Status forbidden(File content) {
        return new Status(play.core.j.JavaResults.Forbidden(), content);
    }

    /**
     * Generates a 403 FORBIDDEN file result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     */
    public static Status forbidden(File content, boolean inline) {
        return new Status(JavaResults.Forbidden(), content, inline);
    }

    /**
     * Generates a 403 FORBIDDEN file result as an attachment.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     */
    public static Status forbidden(File content, String filename) {
        return new Status(JavaResults.Forbidden(), content, true, filename);
    }

    /**
     * Generates a 403 FORBIDDEN file result, sent as a chunked response.
     *
     * @deprecated Since the length of the file is known, there is little reason to send a file as chunked.
     */
    @Deprecated
    public static Status forbidden(File content, int chunkSize) {
        return new Status(play.core.j.JavaResults.Forbidden(), content, chunkSize);
    }

    /**
     * Generates a 403 FORBIDDEN chunked result.
     */
    public static Status forbidden(Chunks<?> chunks) {
        return new Status(play.core.j.JavaResults.Forbidden(), chunks);
    }

    // -- UNAUTHORIZED

    /**
     * Generates a 401 UNAUTHORIZED simple result.
     */
    public static Status unauthorized() {
        return new Status(play.core.j.JavaResults.Unauthorized());
    }

    /**
     * Generates a 401 UNAUTHORIZED simple result.
     */
    public static Status unauthorized(Content content) {
        return new Status(play.core.j.JavaResults.Unauthorized(), content, utf8);
    }

    /**
     * Generates a 401 UNAUTHORIZED simple result.
     */
    public static Status unauthorized(Content content, String charset) {
        return new Status(play.core.j.JavaResults.Unauthorized(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 401 UNAUTHORIZED simple result.
     */
    public static Status unauthorized(String content) {
        return new Status(play.core.j.JavaResults.Unauthorized(), content, utf8);
    }

    /**
     * Generates a 401 UNAUTHORIZED simple result.
     */
    public static Status unauthorized(String content, String charset) {
        return new Status(play.core.j.JavaResults.Unauthorized(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 401 UNAUTHORIZED simple result.
     */
    public static Status unauthorized(JsonNode content) {
        return new Status(play.core.j.JavaResults.Unauthorized(), content, utf8);
    }

    /**
     * Generates a 401 UNAUTHORIZED simple result.
     */
    public static Status unauthorized(JsonNode content, String charset) {
        return new Status(play.core.j.JavaResults.Unauthorized(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 401 UNAUTHORIZED simple result.
     */
    public static Status unauthorized(byte[] content) {
        return new Status(play.core.j.JavaResults.Unauthorized(), content);
    }

    /**
     * Generates a 401 UNAUTHORIZED chunked result.
     */
    public static Status unauthorized(InputStream content) {
        return unauthorized(content, defaultChunkSize);
    }

    /**
     * Generates a 401 UNAUTHORIZED chunked result.
     */
    public static Status unauthorized(InputStream content, int chunkSize) {
        return new Status(play.core.j.JavaResults.Unauthorized(), content, chunkSize);
    }

    /**
     * Generates a 401 UNAUTHORIZED file result as an attachment.
     *
     * @param content The file to send.
     */
    public static Status unauthorized(File content) {
        return new Status(play.core.j.JavaResults.Unauthorized(), content);
    }

    /**
     * Generates a 401 UNAUTHORIZED file result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     */
    public static Status unauthorized(File content, boolean inline) {
        return new Status(JavaResults.Unauthorized(), content, inline);
    }

    /**
     * Generates a 401 UNAUTHORIZED file result as an attachment.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     */
    public static Status unauthorized(File content, String filename) {
        return new Status(JavaResults.Unauthorized(), content, true, filename);
    }

    /**
     * Generates a 401 UNAUTHORIZED file result, sent as a chunked response.
     *
     * @deprecated Since the length of the file is known, there is little reason to send a file as chunked.
     */
    @Deprecated
    public static Status unauthorized(File content, int chunkSize) {
        return new Status(play.core.j.JavaResults.Unauthorized(), content, chunkSize);
    }

    /**
     * Generates a 401 UNAUTHORIZED chunked result.
     */
    public static Status unauthorized(Chunks<?> chunks) {
        return new Status(play.core.j.JavaResults.Unauthorized(), chunks);
    }

    // -- BAD_REQUEST

    /**
     * Generates a 400 BAD_REQUEST simple result.
     */
    public static Status badRequest() {
        return new Status(play.core.j.JavaResults.BadRequest());
    }

    /**
     * Generates a 400 BAD_REQUEST simple result.
     */
    public static Status badRequest(Content content) {
        return new Status(play.core.j.JavaResults.BadRequest(), content, utf8);
    }

    /**
     * Generates a 400 BAD_REQUEST simple result.
     */
    public static Status badRequest(Content content, String charset) {
        return new Status(play.core.j.JavaResults.BadRequest(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 400 BAD_REQUEST simple result.
     */
    public static Status badRequest(String content) {
        return new Status(play.core.j.JavaResults.BadRequest(), content, utf8);
    }

    /**
     * Generates a 400 BAD_REQUEST simple result.
     */
    public static Status badRequest(String content, String charset) {
        return new Status(play.core.j.JavaResults.BadRequest(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 400 BAD_REQUEST simple result.
     */
    public static Status badRequest(JsonNode content) {
        return new Status(play.core.j.JavaResults.BadRequest(), content, utf8);
    }

    /**
     * Generates a 400 BAD_REQUEST simple result.
     */
    public static Status badRequest(JsonNode content, String charset) {
        return new Status(play.core.j.JavaResults.BadRequest(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 400 BAD_REQUEST simple result.
     */
    public static Status badRequest(byte[] content) {
        return new Status(play.core.j.JavaResults.BadRequest(), content);
    }

    /**
     * Generates a 400 BAD_REQUEST chunked result.
     */
    public static Status badRequest(InputStream content) {
        return badRequest(content, defaultChunkSize);
    }

    /**
     * Generates a 400 BAD_REQUEST chunked result.
     */
    public static Status badRequest(InputStream content, int chunkSize) {
        return new Status(play.core.j.JavaResults.BadRequest(), content, chunkSize);
    }

    /**
     * Generates a 400 BAD_REQUEST file result as an attachment.
     *
     * @param content The file to send.
     */
    public static Status badRequest(File content) {
        return new Status(play.core.j.JavaResults.BadRequest(), content);
    }

    /**
     * Generates a 400 BAD_REQUEST file result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     */
    public static Status badRequest(File content, boolean inline) {
        return new Status(JavaResults.BadRequest(), content, inline);
    }

    /**
     * Generates a 400 BAD_REQUEST file result as an attachment.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     */
    public static Status badRequest(File content, String filename) {
        return new Status(JavaResults.BadRequest(), content, true, filename);
    }

    /**
     * Generates a 400 BAD_REQUEST file result, sent as a chunked response.
     *
     * @deprecated Since the length of the file is known, there is little reason to send a file as chunked.
     */
    @Deprecated
    public static Status badRequest(File content, int chunkSize) {
        return new Status(play.core.j.JavaResults.BadRequest(), content, chunkSize);
    }

    /**
     * Generates a 400 BAD_REQUEST chunked result.
     */
    public static Status badRequest(Chunks<?> chunks) {
        return new Status(play.core.j.JavaResults.BadRequest(), chunks);
    }

    // -- Redirect

    /**
     * Generates a 303 SEE_OTHER simple result.
     *
     * @param url The url to redirect.
     */
    public static SimpleResult redirect(String url) {
        return new Redirect(303, url);
    }

    /**
     * Generates a 303 SEE_OTHER simple result.
     *
     * @param call Call defining the url to redirect (typically comes from reverse router).
     */
    public static SimpleResult redirect(Call call) {
        return new Redirect(303, call.url());
    }

    // -- Found

    /**
     * Generates a 302 FOUND simple result.
     *
     * @param url The url to redirect.
     */
    public static SimpleResult found(String url) {
        return new Redirect(302, url);
    }

    /**
     * Generates a 302 FOUND simple result.
     *
     * @param call Call defining the url to redirect (typically comes from reverse router).
     */
    public static SimpleResult found(Call call) {
        return new Redirect(302, call.url());
    }

    // -- Moved Permanently

    /**
     * Generates a 301 MOVED_PERMANENTLY simple result.
     *
     * @param url The url to redirect.
     */
    public static SimpleResult movedPermanently(String url) {
        return new Redirect(301, url);
    }

    /**
     * Generates a 301 MOVED_PERMANENTLY simple result.
     *
     * @param call Call defining the url to redirect (typically comes from reverse router).
     */
    public static SimpleResult movedPermanently(Call call) {
        return new Redirect(301, call.url());
    }

    // -- See Other

    /**
     * Generates a 303 SEE_OTHER simple result.
     *
     * @param url The url to redirect.
     */
    public static SimpleResult seeOther(String url) {
        return new Redirect(303, url);
    }

    /**
     * Generates a 303 SEE_OTHER simple result.
     *
     * @param call Call defining the url to redirect (typically comes from reverse router).
     */
    public static SimpleResult seeOther(Call call) {
        return new Redirect(303, call.url());
    }

    // -- Temporary Redirect

    /**
     * Generates a 307 TEMPORARY_REDIRECT simple result.
     *
     * @param url The url to redirect.
     */
    public static SimpleResult temporaryRedirect(String url) {
        return new Redirect(307, url);
    }

    /**
     * Generates a 307 TEMPORARY_REDIRECT simple result.
     *
     * @param call Call defining the url to redirect (typically comes from reverse router).
     */
    public static SimpleResult temporaryRedirect(Call call) {
        return new Redirect(307, call.url());
    }

    // -- Definitions

    /**
     * A Chunked result.
     */
    public abstract static class Chunks<A> {

        final Enumerator<A> enumerator;
        final play.api.http.Writeable<A> writable;

        public Chunks(play.api.http.Writeable<A> writable) {
            final Chunks<A> self = this;
            this.writable = writable;
            final List<Callback0> disconnectedCallbacks = new ArrayList<Callback0>();
            this.enumerator = play.core.j.JavaResults.chunked(new Callback<Concurrent.Channel<A>>() {
                @Override
                public void invoke(Concurrent.Channel<A> channel) {
                    Chunks.Out<A> chunked = new Chunks.Out<A>(channel, disconnectedCallbacks);
                    self.onReady(chunked);
                }
            }, new Callback0() {
                @Override
                public void invoke() throws Throwable {
                    for(Callback0 callback: disconnectedCallbacks) {
                        try {
                            callback.invoke();
                        } catch(Throwable e) {
                            play.Logger.of("play").error("Exception is Chunks disconnected callback", e);
                        }
                    }
                }
            });
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

            final List<Callback0> disconnectedCallbacks;
            final play.api.libs.iteratee.Concurrent.Channel<A> channel;

            public Out(play.api.libs.iteratee.Concurrent.Channel<A> channel, List<Callback0> disconnectedCallbacks) {
                this.channel = channel;
                this.disconnectedCallbacks = disconnectedCallbacks;
            }

            /**
             * Write a Chunk.
             */
            public void write(A chunk) {
                channel.push(chunk);
            }

            /**
             * Called when the socket is disconnected.
             */
            public void onDisconnected(Callback0 callback) {
                disconnectedCallbacks.add(callback);
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
     */
    public abstract static class StringChunks extends Chunks<String> {

        public StringChunks() {
            this(utf8);
        }

        public StringChunks(String codec) {
            this(Codec.javaSupported(codec));
        }

        public StringChunks(Codec codec) {
            super(play.core.j.JavaResults.writeString(codec));
        }

    }

    /**
     * Chunked result based on byte[] chunks.
     */
    public abstract static class ByteChunks extends Chunks<byte[]> {

        public ByteChunks() {
            super(play.core.j.JavaResults.writeBytes());
        }

    }

    /**
     * An asynchronous result.
     *
     * @deprecated return Promise&lt;Result&gt; from your actions instead.
     */
    @Deprecated
    public static class AsyncResult implements Result {

        private final F.Promise<SimpleResult> promise;
        private final Http.Context context = Http.Context.current();

        public AsyncResult(F.Promise<SimpleResult> promise) {
            this.promise = promise;
        }

        /**
         * Transform this asynchronous result
         *
         * @param f The transformation function
         * @return The transformed AsyncResult
         */
        public AsyncResult transform(F.Function<SimpleResult, SimpleResult> f) {
            return new AsyncResult(promise.map(f));
        }

        public scala.concurrent.Future<play.api.mvc.SimpleResult> getWrappedResult() {
            return promise.map(new Function<SimpleResult, play.api.mvc.SimpleResult>() {
                @Override
                public play.api.mvc.SimpleResult apply(SimpleResult result) throws Throwable {
                    return play.core.j.JavaHelpers$.MODULE$.createResult(context, result);
                }
            }).wrapped();
        }

        public Promise<SimpleResult> getPromise() {
            return promise;
        }
    }

    /**
     * A 501 NOT_IMPLEMENTED simple result.
     */
    public static class Todo extends SimpleResult {

        final private play.api.mvc.SimpleResult wrappedResult;

        public Todo() {
            wrappedResult = play.core.j.JavaResults.NotImplemented().apply(
                    views.html.defaultpages.todo.render(),
                    play.core.j.JavaResults.writeContent("text/html", utf8)
                    );
        }

        @Override
        public play.api.mvc.SimpleResult getWrappedSimpleResult() {
            return this.wrappedResult;
        }
    }

    /**
     * A simple result.
     */
    public static class Status extends SimpleResult {

        private play.api.mvc.SimpleResult wrappedResult;

        public Status(play.api.mvc.Results.Status status) {
            wrappedResult = status.apply(
                    play.core.j.JavaResults.empty(),
                    play.core.j.JavaResults.writeEmptyContent()
                    );
        }

        public Status(play.api.mvc.Results.Status status, String content, Codec codec) {
            if(content == null) {
                throw new NullPointerException("null content");
            }
            wrappedResult = status.apply(
                    content,
                    play.core.j.JavaResults.writeString(codec)
                    );
        }

        public Status(play.api.mvc.Results.Status status, JsonNode content, Codec codec) {
            if(content == null) {
                throw new NullPointerException("null content");
            }
            wrappedResult = status.apply(
                    content,
                    play.core.j.JavaResults.writeJson(codec)
                    );
        }

        public Status(play.api.mvc.Results.Status status, Content content, Codec codec) {
            if(content == null) {
                throw new NullPointerException("null content");
            }
            wrappedResult = status.apply(
                    content,
                    play.core.j.JavaResults.writeContent(content.contentType(), codec)
                    );
        }

        public <A> Status(play.api.mvc.Results.Status status, Chunks<A> chunks) {
            if(chunks == null) {
                throw new NullPointerException("null content");
            }
            wrappedResult = status.chunked(chunks.enumerator, chunks.writable);
        }

        public Status(play.api.mvc.Results.Status status, byte[] content) {
            if(content == null) {
                throw new NullPointerException("null content");
            }
            wrappedResult = status.apply(
                    content,
                    play.core.j.JavaResults.writeBytes()
                    );
        }

        public Status(play.api.mvc.Results.Status status, File content) {
            this(status, content, false);
        }

        public Status(play.api.mvc.Results.Status status, File content, boolean inline) {
            this(status, content, inline, content.getName());
        }

        public Status(play.api.mvc.Results.Status status, File content, boolean inline, String filename) {
            if(content == null) {
                throw new NullPointerException("null content");
            }
            wrappedResult = play.core.j.JavaResults.sendFile(status, content, inline, filename);
        }

        public Status(play.api.mvc.Results.Status status, File content, int chunkSize) {
            if(content == null) {
                throw new NullPointerException("null content");
            }
            wrappedResult = status.chunked(
                    play.core.j.JavaResults.chunked(content, chunkSize),
                    play.core.j.JavaResults.writeBytes(Scala.orNull(play.api.libs.MimeTypes.forFileName(content.getName())))
                    );
        }

        public Status(play.api.mvc.Results.Status status, InputStream content, int chunkSize) {
            if(content == null) {
                throw new NullPointerException("null content");
            }
            wrappedResult = status.chunked(
                    play.core.j.JavaResults.chunked(content, chunkSize),
                    play.core.j.JavaResults.writeBytes()
                    );
        }

        public play.api.mvc.SimpleResult getWrappedSimpleResult() {
            return wrappedResult;
        }

        /**
         * Change the Content-Type header for this result.
         */
        public Status as(String contentType) {
            wrappedResult = wrappedResult.as(contentType);
            return this;
        }

        public String toString() {
            return wrappedResult.toString();
        }

    }

    /**
     * A redirect result.
     */
    public static class Redirect extends SimpleResult {

        final private play.api.mvc.SimpleResult wrappedResult;

        public Redirect(int status, String url) {
            wrappedResult = play.core.j.JavaResults.Redirect(url, status);
        }

        public play.api.mvc.SimpleResult getWrappedSimpleResult() {
            return this.wrappedResult;
        }

    }
}
