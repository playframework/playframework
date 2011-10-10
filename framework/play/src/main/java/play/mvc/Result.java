package play.mvc;

import play.api.*;
import play.api.mvc.Results.* ;

public interface Result {

    public play.api.mvc.Result getWrappedResult();

    public static class Ok implements Result {

        final private play.api.mvc.Result wrappedResult;

        public Ok(String content) {
            wrappedResult = play.api.mvc.JResults.Ok().apply(
                content,
                play.api.mvc.JResults.emptyHeaders(),
                play.api.mvc.JResults.writeString(),
                play.api.mvc.JResults.contentTypeOfString()
            );
        }

        public Ok(Content content) {
            wrappedResult = play.api.mvc.JResults.Ok().apply(
                content,
                play.api.mvc.JResults.emptyHeaders(),
                play.api.mvc.JResults.writeContent(),
                play.api.mvc.JResults.contentTypeOfContent()
            );
        }

        public play.api.mvc.Result getWrappedResult() {
            return this.wrappedResult;
        }

    }

    public static class NotFound implements Result {

        final private play.api.mvc.Result wrappedResult;

        public NotFound(String content) {
            wrappedResult = play.api.mvc.JResults.NotFound().apply(
                content,
                play.api.mvc.JResults.emptyHeaders(),
                play.api.mvc.JResults.writeString(),
                play.api.mvc.JResults.contentTypeOfString()
            );
        }

        public NotFound(Content content) {
            wrappedResult = play.api.mvc.JResults.NotFound().apply(
                content,
                play.api.mvc.JResults.emptyHeaders(),
                play.api.mvc.JResults.writeContent(),
                play.api.mvc.JResults.contentTypeOfContent()
            );
        }

        public play.api.mvc.Result getWrappedResult() {
            return this.wrappedResult;
        }

    }

    public static class InternalServerError implements Result {

        final private play.api.mvc.Result wrappedResult;

        public InternalServerError(String content) {
            wrappedResult = play.api.mvc.JResults.InternalServerError().apply(
                content,
                play.api.mvc.JResults.emptyHeaders(),
                play.api.mvc.JResults.writeString(),
                play.api.mvc.JResults.contentTypeOfString()
            );
        }

        public InternalServerError(Content content) {
            wrappedResult = play.api.mvc.JResults.InternalServerError().apply(
                content,
                play.api.mvc.JResults.emptyHeaders(),
                play.api.mvc.JResults.writeContent(),
                play.api.mvc.JResults.contentTypeOfContent()
            );
        }

        public play.api.mvc.Result getWrappedResult() {
            return this.wrappedResult;
        }

    }

    public static class Forbidden implements Result {

        final private play.api.mvc.Result wrappedResult;

        public Forbidden(String content) {
            wrappedResult = play.api.mvc.JResults.Forbidden().apply(
                content,
                play.api.mvc.JResults.emptyHeaders(),
                play.api.mvc.JResults.writeString(),
                play.api.mvc.JResults.contentTypeOfString()
            );
        }

        public Forbidden(Content content) {
            wrappedResult = play.api.mvc.JResults.Forbidden().apply(
                content,
                play.api.mvc.JResults.emptyHeaders(),
                play.api.mvc.JResults.writeContent(),
                play.api.mvc.JResults.contentTypeOfContent()
            );
        }

        public play.api.mvc.Result getWrappedResult() {
            return this.wrappedResult;
        }

    }

    public static class BadRequest implements Result {

        final private play.api.mvc.Result wrappedResult;

        public BadRequest(String content) {
            wrappedResult = play.api.mvc.JResults.BadRequest().apply(
                content,
                play.api.mvc.JResults.emptyHeaders(),
                play.api.mvc.JResults.writeString(),
                play.api.mvc.JResults.contentTypeOfString()
            );
        }

        public BadRequest(Content content) {
            wrappedResult = play.api.mvc.JResults.BadRequest().apply(
                content,
                play.api.mvc.JResults.emptyHeaders(),
                play.api.mvc.JResults.writeContent(),
                play.api.mvc.JResults.contentTypeOfContent()
            );
        }

        public play.api.mvc.Result getWrappedResult() {
            return this.wrappedResult;
        }

    }

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