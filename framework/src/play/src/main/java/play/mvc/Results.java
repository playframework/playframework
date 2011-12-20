package play.mvc;

import play.api.*;
import play.api.mvc.*;
import play.api.mvc.Results.* ;

import play.libs.*;
import play.libs.F.*;

/**
 * Common results.
 */
public class Results {

    static Codec utf8 = Codec.javaSupported("utf-8");

    // -- Constructor methods

    /**
     * Generates a 501 NOT_IMPLEMENTED simple result.
     */
    public static Result TODO = new Todo();

    /**
     * Handles an Asynchronous result.
     */
    public static Result async(play.libs.F.Promise<Result> p) {
        return new AsyncResult(p);
    }

    /**
     * Generates a 200 OK simple result.
     */
    public static Result ok() {
        return new Status(play.api.mvc.JResults.Ok());
    }

    /**
     * Generates a 200 OK simple result.
     */
    public static Result ok(Content content) {
        return new Status(play.api.mvc.JResults.Ok(), content, utf8);
    }

    /**
     * Generates a 200 OK simple result.
     */
    public static Result ok(Content content, String charset) {
        return new Status(play.api.mvc.JResults.Ok(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 200 OK simple result.
     */
    public static Result ok(String content) {
        return new Status(play.api.mvc.JResults.Ok(), content, utf8);
    }

    /**
     * Generates a 200 OK simple result.
     */
    public static Result ok(String content, String charset) {
        return new Status(play.api.mvc.JResults.Ok(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 200 OK chunked result.
     */
    public static Result ok(Chunks<?> chunks) {
        return new Status(play.api.mvc.JResults.Ok(), chunks);
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR simple result.
     */
    public static Result internalServerError() {
        return new Status(play.api.mvc.JResults.InternalServerError());
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR simple result.
     */
    public static Result internalServerError(Content content) {
        return new Status(play.api.mvc.JResults.InternalServerError(), content, utf8);
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR simple result.
     */
    public static Result internalServerError(Content content, String charset) {
        return new Status(play.api.mvc.JResults.InternalServerError(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR simple result.
     */
    public static Result internalServerError(String content) {
        return new Status(play.api.mvc.JResults.InternalServerError(), content, utf8);
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR simple result.
     */
    public static Result internalServerError(String content, String charset) {
        return new Status(play.api.mvc.JResults.InternalServerError(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 404 NOT_FOUND simple result.
     */
    public static Result notFound() {
        return new Status(play.api.mvc.JResults.NotFound());
    }

    /**
     * Generates a 404 NOT_FOUND simple result.
     */
    public static Result notFound(Content content) {
        return new Status(play.api.mvc.JResults.NotFound(), content, utf8);
    }

    /**
     * Generates a 404 NOT_FOUND simple result.
     */
    public static Result notFound(Content content, String charset) {
        return new Status(play.api.mvc.JResults.NotFound(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 404 NOT_FOUND simple result.
     */
    public static Result notFound(String content) {
        return new Status(play.api.mvc.JResults.NotFound(), content, utf8);
    }

    /**
     * Generates a 404 NOT_FOUND simple result.
     */
    public static Result notFound(String content, String charset) {
        return new Status(play.api.mvc.JResults.NotFound(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 403 FORBIDDEN simple result.
     */
    public static Result forbidden() {
        return new Status(play.api.mvc.JResults.Forbidden());
    }

    /**
     * Generates a 403 FORBIDDEN simple result.
     */
    public static Result forbidden(Content content) {
        return new Status(play.api.mvc.JResults.Forbidden(), content, utf8);
    }

    /**
     * Generates a 403 FORBIDDEN simple result.
     */
    public static Result forbidden(Content content, String charset) {
        return new Status(play.api.mvc.JResults.Forbidden(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 403 FORBIDDEN simple result.
     */
    public static Result forbidden(String content) {
        return new Status(play.api.mvc.JResults.Forbidden(), content, utf8);
    }

    /**
     * Generates a 403 FORBIDDEN simple result.
     */
    public static Result forbidden(String content, String charset) {
        return new Status(play.api.mvc.JResults.Forbidden(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 401 UNAUTHORIZED simple result.
     */
    public static Result unauthorized() {
        return new Status(play.api.mvc.JResults.Unauthorized());
    }

    /**
     * Generates a 401 UNAUTHORIZED simple result.
     */
    public static Result unauthorized(Content content) {
        return new Status(play.api.mvc.JResults.Unauthorized(), content, utf8);
    }

    /**
     * Generates a 401 UNAUTHORIZED simple result.
     */
    public static Result unauthorized(Content content, String charset) {
        return new Status(play.api.mvc.JResults.Unauthorized(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 401 UNAUTHORIZED simple result.
     */
    public static Result unauthorized(String content) {
        return new Status(play.api.mvc.JResults.Unauthorized(), content, utf8);
    }

    /**
     * Generates a 401 UNAUTHORIZED simple result.
     */
    public static Result unauthorized(String content, String charset) {
        return new Status(play.api.mvc.JResults.Unauthorized(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 400 BAD_REQUEST simple result.
     */
    public static Result badRequest() {
        return new Status(play.api.mvc.JResults.BadRequest());
    }

    /**
     * Generates a 400 BAD_REQUEST simple result.
     */
    public static Result badRequest(Content content) {
        return new Status(play.api.mvc.JResults.BadRequest(), content, utf8);
    }

    /**
     * Generates a 400 BAD_REQUEST simple result.
     */
    public static Result badRequest(Content content, String charset) {
        return new Status(play.api.mvc.JResults.BadRequest(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 400 BAD_REQUEST simple result.
     */
    public static Result badRequest(String content) {
        return new Status(play.api.mvc.JResults.BadRequest(), content, utf8);
    }

    /**
     * Generates a 400 BAD_REQUEST simple result.
     */
    public static Result badRequest(String content, String charset) {
        return new Status(play.api.mvc.JResults.BadRequest(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 302 FOUND simple result.
     *
     * @param url The url to redirect.
     */
    public static Result redirect(String url) {
        return new Redirect(url);
    }

    /**
     * Generates a 302 FOUND simple result.
     *
     * @param call Call defining the url to redirect (typically comes from reverse router).
     */
    public static Result redirect(Call call) {
        return new Redirect(call.url());
    }

    // -- Definitions

    public abstract static class Chunks<A> {

        final scala.Function1<play.api.libs.iteratee.Iteratee<A,scala.runtime.BoxedUnit>, scala.runtime.BoxedUnit> f;
        final play.api.mvc.Writeable<A> w;
        final play.api.mvc.ContentTypeOf<A> ct;

        public Chunks(play.api.mvc.Writeable<A> w, play.api.mvc.ContentTypeOf<A> ct) {
            final Chunks<A> self = this;
            this.w = w;
            this.ct = ct;
            f = new Scala.Function1<play.api.libs.iteratee.Iteratee<A,scala.runtime.BoxedUnit>, scala.runtime.BoxedUnit>() {
                public scala.runtime.BoxedUnit apply(play.api.libs.iteratee.Iteratee<A,scala.runtime.BoxedUnit> iteratee) {
                    play.api.libs.iteratee.CallbackEnumerator<A> enumerator = play.api.mvc.JResults.chunked();
                    enumerator.apply(iteratee);
                    Chunks.Out<A> chunked = new Chunks.Out<A>(enumerator);
                    self.onReady(chunked);
                    return null;
                }
            };
        }

        public abstract void onReady(Chunks.Out<A> out);

        public static class Out<A> {

            final play.api.libs.iteratee.CallbackEnumerator<A> enumerator;

            public Out(play.api.libs.iteratee.CallbackEnumerator<A> enumerator) {
                this.enumerator = enumerator;
            }

            public void write(A chunk) {
                enumerator.push(chunk);
            }

            public void close() {
                enumerator.close();
            }

        }

    }

    public abstract static class StringChunks extends Chunks<String> {

        public StringChunks() {
            this(utf8);
        }

        public StringChunks(String codec) {
            this(Codec.javaSupported(codec));
        }

        public StringChunks(Codec codec) {
            super(
                play.api.mvc.JResults.writeString(codec),
                play.api.mvc.JResults.contentTypeOfString(codec)
            );
        }

    }

    public abstract static class ByteChunks extends Chunks<byte[]> {

        public ByteChunks() {
            super(
                play.api.mvc.JResults.writeBytes(),
                play.api.mvc.JResults.contentTypeOfBytes()
            );
        }

    }

    /**
     * An asynchronous result.
     */
    public static class AsyncResult implements Result {

        final private play.api.mvc.Result wrappedResult;

        public AsyncResult(play.libs.F.Promise<Result> p) {
            wrappedResult = play.api.mvc.JResults.async(
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
            wrappedResult = play.api.mvc.JResults.NotImplemented().apply(
                views.html.defaultpages.todo.render(),
                play.api.mvc.JResults.writeContent(utf8),
                play.api.mvc.JResults.contentTypeOf("text/html; charset=utf-8")
            );
        }

        public play.api.mvc.Result getWrappedResult() {
            return this.wrappedResult;
        }

    }

    /**
     * A 200 OK simple result.
     */
    public static class Status implements Result {

        final private play.api.mvc.Result wrappedResult;

       public Status(play.api.mvc.Results.Status status) {
            wrappedResult = status.apply(
                play.api.mvc.JResults.empty(),
                play.api.mvc.JResults.writeEmptyContent(),
                play.api.mvc.JResults.contentTypeOfEmptyContent()
            );
        }

        public Status(play.api.mvc.Results.Status status, String content, Codec codec) {
            wrappedResult = status.apply(
                content,
                play.api.mvc.JResults.writeString(codec),
                play.api.mvc.JResults.contentTypeOfString(codec)
            );
        }

        public Status(play.api.mvc.Results.Status status, Content content, Codec codec) {
            wrappedResult = status.apply(
                content,
                play.api.mvc.JResults.writeContent(codec),
                play.api.mvc.JResults.contentTypeOf(content.contentType() + "; charset=" + codec.charset())
            );
        }

        public <A> Status(play.api.mvc.Results.Status status, Chunks<A> chunks) {
            wrappedResult = status.apply(chunks.f, chunks.w, chunks.ct);
        }

        public play.api.mvc.Result getWrappedResult() {
            return this.wrappedResult;
        }

    }

    /**
     * A 302 FOUND simple result.
     */
    public static class Redirect implements Result {

        final private play.api.mvc.Result wrappedResult;

        public Redirect(String url) {
            wrappedResult = play.api.mvc.JResults.Redirect(url);
        }

        public play.api.mvc.Result getWrappedResult() {
            return this.wrappedResult;
        }

    }

}
