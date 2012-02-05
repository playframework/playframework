package play.mvc;

import play.api.*;
import play.api.mvc.*;
import play.api.mvc.Results.* ;

import play.libs.*;
import play.libs.F.*;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;

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
     */
    public static AsyncResult async(play.libs.F.Promise<Result> p) {
        return new AsyncResult(p);
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
        return status(status, content, defaultChunkSize);
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
     * Generates a 200 OK chunked result.
     */
    public static Status ok(File content) {
        return ok(content, defaultChunkSize);
    }
    
    /**
     * Generates a 200 OK simple result.
     */
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
     * Generates a 201 CREATED chunked result.
     */
    public static Status created(File content) {
        return created(content, defaultChunkSize);
    }
    
    /**
     * Generates a 201 CREATED simple result.
     */
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
     * Generates a 500 INTERNAL_SERVER_ERROR chunked result.
     */
    public static Status internalServerError(File content) {
        return internalServerError(content, defaultChunkSize);
    }
    
    /**
     * Generates a 500 INTERNAL_SERVER_ERROR simple result.
     */
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
     * Generates a 404 NOT_FOUND chunked result.
     */
    public static Status notFound(File content) {
        return notFound(content, defaultChunkSize);
    }
    
    /**
     * Generates a 404 NOT_FOUND simple result.
     */
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
     * Generates a 403 FORBIDDEN chunked result.
     */
    public static Status forbidden(File content) {
        return forbidden(content, defaultChunkSize);
    }
    
    /**
     * Generates a 403 FORBIDDEN simple result.
     */
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
     * Generates a 401 UNAUTHORIZED chunked result.
     */
    public static Status unauthorized(File content) {
        return unauthorized(content, defaultChunkSize);
    }
    
    /**
     * Generates a 401 UNAUTHORIZED simple result.
     */
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
     * Generates a 400 BAD_REQUEST chunked result.
     */
    public static Status badRequest(File content) {
        return badRequest(content, defaultChunkSize);
    }
    
    /**
     * Generates a 400 BAD_REQUEST simple result.
     */
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
    public static Result redirect(String url) {
        return new Redirect(303, url);
    }

    /**
     * Generates a 303 SEE_OTHER simple result.
     *
     * @param call Call defining the url to redirect (typically comes from reverse router).
     */
    public static Result redirect(Call call) {
        return new Redirect(303, call.url());
    }
    
    // -- Found

    /**
     * Generates a 302 FOUND simple result.
     *
     * @param url The url to redirect.
     */
    public static Result found(String url) {
        return new Redirect(302, url);
    }

    /**
     * Generates a 302 FOUND simple result.
     *
     * @param call Call defining the url to redirect (typically comes from reverse router).
     */
    public static Result found(Call call) {
        return new Redirect(302, call.url());
    }
    
    // -- Moved Permanently

    /**
     * Generates a 301 MOVED_PERMANENTLY simple result.
     *
     * @param url The url to redirect.
     */
    public static Result movedPermanently(String url) {
        return new Redirect(301, url);
    }

    /**
     * Generates a 301 MOVED_PERMANENTLY simple result.
     *
     * @param call Call defining the url to redirect (typically comes from reverse router).
     */
    public static Result movedPermanently(Call call) {
        return new Redirect(301, call.url());
    }
    
    // -- See Other

    /**
     * Generates a 303 SEE_OTHER simple result.
     *
     * @param url The url to redirect.
     */
    public static Result seeOther(String url) {
        return new Redirect(303, url);
    }

    /**
     * Generates a 303 SEE_OTHER simple result.
     *
     * @param call Call defining the url to redirect (typically comes from reverse router).
     */
    public static Result seeOther(Call call) {
        return new Redirect(303, call.url());
    }
    
    // -- Temporary Redirect

    /**
     * Generates a 307 TEMPORARY_REDIRECT simple result.
     *
     * @param url The url to redirect.
     */
    public static Result temporaryRedirect(String url) {
        return new Redirect(307, url);
    }

    /**
     * Generates a 307 TEMPORARY_REDIRECT simple result.
     *
     * @param call Call defining the url to redirect (typically comes from reverse router).
     */
    public static Result temporaryRedirect(Call call) {
        return new Redirect(307, call.url());
    }

    // -- Definitions

    /**
     * A Chunked result.
     */
    public abstract static class Chunks<A> {

        final scala.Function1<play.api.libs.iteratee.Iteratee<A,scala.runtime.BoxedUnit>, scala.runtime.BoxedUnit> f;
        final play.api.http.Writeable<A> w;
        final play.api.http.ContentTypeOf<A> ct;

        public Chunks(play.api.http.Writeable<A> w, play.api.http.ContentTypeOf<A> ct) {
            final Chunks<A> self = this;
            this.w = w;
            this.ct = ct;
            f = new scala.runtime.AbstractFunction1<play.api.libs.iteratee.Iteratee<A,scala.runtime.BoxedUnit>, scala.runtime.BoxedUnit>() {
                public scala.runtime.BoxedUnit apply(play.api.libs.iteratee.Iteratee<A,scala.runtime.BoxedUnit> iteratee) {
                    final List<Callback0> disconnectedCallbacks = new ArrayList<Callback0>();
                    play.api.libs.iteratee.PushEnumerator<A> enumerator = play.core.j.JavaResults.chunked(
                        new scala.runtime.AbstractFunction0<scala.runtime.BoxedUnit>() {
                            public scala.runtime.BoxedUnit apply() {
                                for(Callback0 callback: disconnectedCallbacks) {
                                    try {
                                        callback.invoke();
                                    } catch(Throwable e) {
                                        play.Logger.of("play").error("Exception is Chunks disconnected callback", e);
                                    }
                                }
                                return null;
                            }
                        }
                    );
                    enumerator.apply(iteratee);
                    Chunks.Out<A> chunked = new Chunks.Out<A>(enumerator, disconnectedCallbacks);
                    self.onReady(chunked);
                    return null;
                }
            };
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
            final play.api.libs.iteratee.PushEnumerator<A>  enumerator;

            public Out(play.api.libs.iteratee.PushEnumerator<A> enumerator, List<Callback0> disconnectedCallbacks) {
                this.enumerator = enumerator;
                this.disconnectedCallbacks = disconnectedCallbacks;
            }

            /**
             * Write a Chunk.
             */
            public void write(A chunk) {
                enumerator.push(chunk);
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
                enumerator.close();
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
            super(
                play.core.j.JavaResults.writeString(codec),
                play.core.j.JavaResults.contentTypeOfString(codec)
            );
        }

    }

    /**
     * Chunked result based on byte[] chunks.
     */
    public abstract static class ByteChunks extends Chunks<byte[]> {

        public ByteChunks() {
            super(
                play.core.j.JavaResults.writeBytes(),
                play.core.j.JavaResults.contentTypeOfBytes()
            );
        }

    }

    /**
     * An asynchronous result.
     */
    public static class AsyncResult implements Result {

        final private play.api.mvc.Result wrappedResult;

        public AsyncResult(play.libs.F.Promise<Result> p) {
            wrappedResult = play.core.j.JavaResults.async(
                p.map(new play.libs.F.Function<Result,play.api.mvc.Result>() {
                    public play.api.mvc.Result apply(Result r) {
                        return r.getWrappedResult();
                    }
                }).getWrappedPromise()
            );
        }

        public play.api.mvc.Result getWrappedResult() {
            return this.wrappedResult;
        }

    }

    /**
     * A 501 NOT_IMPLEMENTED simple result.
     */
    public static class Todo implements Result {

        final private play.api.mvc.Result wrappedResult;

        public Todo() {
            wrappedResult = play.core.j.JavaResults.NotImplemented().apply(
                views.html.defaultpages.todo.render(),
                play.core.j.JavaResults.writeContent(utf8),
                play.core.j.JavaResults.contentTypeOf("text/html; charset=utf-8")
            );
        }

        public play.api.mvc.Result getWrappedResult() {
            return this.wrappedResult;
        }

    }

    /**
     * A simple result.
     */
    public static class Status implements Result {

       private play.api.mvc.PlainResult wrappedResult;

       public Status(play.api.mvc.Results.Status status) {
            wrappedResult = status.apply(
                play.core.j.JavaResults.empty(),
                play.core.j.JavaResults.writeEmptyContent(),
                play.core.j.JavaResults.contentTypeOfEmptyContent()
            );
        }

        public Status(play.api.mvc.Results.Status status, String content, Codec codec) {
            if(content == null) {
                throw new NullPointerException("null content");
            }
            wrappedResult = status.apply(
                content,
                play.core.j.JavaResults.writeString(codec),
                play.core.j.JavaResults.contentTypeOfString(codec)
            );
        }
        
        public Status(play.api.mvc.Results.Status status, JsonNode content, Codec codec) {
            if(content == null) {
                throw new NullPointerException("null content");
            }
            wrappedResult = status.apply(
                content.toString(),
                play.core.j.JavaResults.writeString(codec),
                play.core.j.JavaResults.contentTypeOfJson(codec)
            );
        }

        public Status(play.api.mvc.Results.Status status, Content content, Codec codec) {
            if(content == null) {
                throw new NullPointerException("null content");
            }
            wrappedResult = status.apply(
                content,
                play.core.j.JavaResults.writeContent(codec),
                play.core.j.JavaResults.contentTypeOf(content.contentType() + "; charset=" + codec.charset())
            );
        }

        public <A> Status(play.api.mvc.Results.Status status, Chunks<A> chunks) {
            if(chunks == null) {
                throw new NullPointerException("null content");
            }
            wrappedResult = status.stream(chunks.f, chunks.w, chunks.ct);
        }
        
        public Status(play.api.mvc.Results.Status status, byte[] content) {
            if(content == null) {
                throw new NullPointerException("null content");
            }
            wrappedResult = status.apply(
                content,
                play.core.j.JavaResults.writeBytes(),
                play.core.j.JavaResults.contentTypeOfBytes()
            );
        }
        
        public Status(play.api.mvc.Results.Status status, InputStream content, int chunkSize) {
            if(content == null) {
                throw new NullPointerException("null content");
            }
            wrappedResult = status.stream(
                play.core.j.JavaResults.chunked(content, chunkSize), 
                play.core.j.JavaResults.writeBytes(),
                play.core.j.JavaResults.contentTypeOfBytes()
            );
        }
        
        public Status(play.api.mvc.Results.Status status, File content, int chunkSize) {
            if(content == null) {
                throw new NullPointerException("null content");
            }
            wrappedResult = status.stream(
                play.core.j.JavaResults.chunked(content, chunkSize), 
                play.core.j.JavaResults.writeBytes(),
                play.core.j.JavaResults.contentTypeOfBytes(Scala.orNull(play.api.libs.MimeTypes.forFileName(content.getName())))
            );
        }

        public play.api.mvc.Result getWrappedResult() {
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
    public static class Redirect implements Result {

        final private play.api.mvc.Result wrappedResult;

        public Redirect(int status, String url) {
            wrappedResult = play.core.j.JavaResults.Redirect(url, status);
        }

        public play.api.mvc.Result getWrappedResult() {
            return this.wrappedResult;
        }

    }

}
