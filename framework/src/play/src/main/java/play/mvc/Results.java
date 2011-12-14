package play.mvc;

import play.api.*;
import play.api.mvc.*;
import play.api.mvc.Results.* ;

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
     * Generates a 200 OK simple result.
     */
    public static Result ok() {
        return new Ok();
    }
    
    /**
     * Generates a 200 OK simple result.
     */
    public static Result ok(Content content) {
        return new Ok(content);
    }
    
    /**
     * Generates a 200 OK simple result.
     */
    public static Result ok(String content) {
        return new Ok(content);
    }
    
    /**
     * Generates a 500 INTERNAL_SERVER_ERROR simple result.
     */
    public static Result internalServerError() {
        return new InternalServerError();
    }
    
    /**
     * Generates a 500 INTERNAL_SERVER_ERROR simple result.
     */
    public static Result internalServerError(Content content) {
        return new InternalServerError(content);
    }
    
    /**
     * Generates a 500 INTERNAL_SERVER_ERROR simple result.
     */
    public static Result internalServerError(String content) {
        return new InternalServerError(content);
    }
        
    /**
     * Generates a 404 NOT_FOUND simple result.
     */
    public static Result notFound() {
        return new NotFound();
    }
    
    /**
     * Generates a 404 NOT_FOUND simple result.
     */
    public static Result notFound(Content content) {
        return new NotFound(content);
    }
    
    /**
     * Generates a 404 NOT_FOUND simple result.
     */
    public static Result notFound(String content) {
        return new NotFound(content);
    }
     
    /**
     * Generates a 403 FORBIDDEN simple result.
     */   
    public static Result forbidden() {
        return new Forbidden();
    }
    
    /**
     * Generates a 403 FORBIDDEN simple result.
     */
    public static Result forbidden(Content content) {
        return new Forbidden(content);
    }
    
    /**
     * Generates a 403 FORBIDDEN simple result.
     */
    public static Result forbidden(String content) {
        return new Forbidden(content);
    }
    
    /**
     * Generates a 401 UNAUTHORIZED simple result.
     */
    public static Result unauthorized() {
        return new Unauthorized();
    }
    
    /**
     * Generates a 401 UNAUTHORIZED simple result.
     */
    public static Result unauthorized(Content content) {
        return new Unauthorized(content);
    }
    
    /**
     * Generates a 401 UNAUTHORIZED simple result.
     */
    public static Result unauthorized(String content) {
        return new Unauthorized(content);
    }
       
    /**
     * Generates a 400 BAD_REQUEST simple result.
     */ 
    public static Result badRequest() {
        return new BadRequest();
    }
    
    /**
     * Generates a 400 BAD_REQUEST simple result.
     */
    public static Result badRequest(Content content) {
        return new BadRequest(content);
    }
    
    /**
     * Generates a 400 BAD_REQUEST simple result.
     */
    public static Result badRequest(String content) {
        return new BadRequest(content);
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

    /**
     * A 501 NOT_IMPLEMENTED simple result.
     */
    public static class Todo implements Result {
        
        final private play.api.mvc.Result wrappedResult;
        
        public Todo() {
            wrappedResult = play.api.mvc.JResults.NotImplemented().apply(
                views.html.defaultpages.todo.render(),
                play.api.mvc.JResults.writeContent(),
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
    public static class Ok implements Result {
        
        final private play.api.mvc.Result wrappedResult;
        
        public Ok() {
            wrappedResult = play.api.mvc.JResults.Ok().apply(
                play.api.mvc.JResults.empty(),
                play.api.mvc.JResults.writeEmptyContent(),
                play.api.mvc.JResults.contentTypeOfEmptyContent()
            );
        }
        
        public Ok(String content) {
            wrappedResult = play.api.mvc.JResults.Ok().apply(
                content,
                play.api.mvc.JResults.writeString(),
                play.api.mvc.JResults.contentTypeOfString()
            );
        }
        
        public Ok(Content content) {
            wrappedResult = play.api.mvc.JResults.Ok().apply(
                content,
                play.api.mvc.JResults.writeContent(),
                play.api.mvc.JResults.contentTypeOf(content.contentType() + "; charset=utf-8")
            );
        }
        
        public play.api.mvc.Result getWrappedResult() {
            return this.wrappedResult;
        }
        
    }
    
    /**
     * A 404 NOT_FOUND simple result.
     */
    public static class NotFound implements Result {
        
        final private play.api.mvc.Result wrappedResult;
        
        public NotFound() {
            wrappedResult = play.api.mvc.JResults.NotFound().apply(
                play.api.mvc.JResults.empty(),
                play.api.mvc.JResults.writeEmptyContent(),
                play.api.mvc.JResults.contentTypeOfEmptyContent()
            );
        }
        
        public NotFound(String content) {
            wrappedResult = play.api.mvc.JResults.NotFound().apply(
                content,
                play.api.mvc.JResults.writeString(),
                play.api.mvc.JResults.contentTypeOfString()
            );
        }
        
        public NotFound(Content content) {
            wrappedResult = play.api.mvc.JResults.NotFound().apply(
                content,
                play.api.mvc.JResults.writeContent(),
                play.api.mvc.JResults.contentTypeOf(content.contentType() + "; charset=utf-8")
            );        
        }
                
        public play.api.mvc.Result getWrappedResult() {
            return this.wrappedResult;
        }
        
    }
    
    /**
     * A 500 INTERNAL_SERVER_ERROR simple result.
     */
    public static class InternalServerError implements Result {
        
        final private play.api.mvc.Result wrappedResult;
        
        public InternalServerError() {
            wrappedResult = play.api.mvc.JResults.InternalServerError().apply(
                play.api.mvc.JResults.empty(),
                play.api.mvc.JResults.writeEmptyContent(),
                play.api.mvc.JResults.contentTypeOfEmptyContent()
            );
        }
        
        public InternalServerError(String content) {
            wrappedResult = play.api.mvc.JResults.InternalServerError().apply(
                content,
                play.api.mvc.JResults.writeString(),
                play.api.mvc.JResults.contentTypeOfString()
            );
        }
        
        public InternalServerError(Content content) {
            wrappedResult = play.api.mvc.JResults.InternalServerError().apply(
                content,
                play.api.mvc.JResults.writeContent(),
                play.api.mvc.JResults.contentTypeOf(content.contentType() + "; charset=utf-8")
            );        
        }
                
        public play.api.mvc.Result getWrappedResult() {
            return this.wrappedResult;
        }
        
    }
    
    /**
     * A 403 FORBIDDEN simple result.
     */
    public static class Forbidden implements Result {
        
        final private play.api.mvc.Result wrappedResult;
        
        public Forbidden() {
            wrappedResult = play.api.mvc.JResults.Forbidden().apply(
                play.api.mvc.JResults.empty(),
                play.api.mvc.JResults.writeEmptyContent(),
                play.api.mvc.JResults.contentTypeOfEmptyContent()
            );
        }
        
        public Forbidden(String content) {
            wrappedResult = play.api.mvc.JResults.Forbidden().apply(
                content,
                play.api.mvc.JResults.writeString(),
                play.api.mvc.JResults.contentTypeOfString()
            );
        }
        
        public Forbidden(Content content) {
            wrappedResult = play.api.mvc.JResults.Forbidden().apply(
                content,
                play.api.mvc.JResults.writeContent(),
                play.api.mvc.JResults.contentTypeOf(content.contentType() + "; charset=utf-8")
            );        
        }
                
        public play.api.mvc.Result getWrappedResult() {
            return this.wrappedResult;
        }
        
    }
    
    /**
     * A 401 UNAUTHORIZED simple result.
     */
    public static class Unauthorized implements Result {
        
        final private play.api.mvc.Result wrappedResult;
        
        public Unauthorized() {
            wrappedResult = play.api.mvc.JResults.Unauthorized().apply(
                play.api.mvc.JResults.empty(),
                play.api.mvc.JResults.writeEmptyContent(),
                play.api.mvc.JResults.contentTypeOfEmptyContent()
            );
        }
        
        public Unauthorized(String content) {
            wrappedResult = play.api.mvc.JResults.Unauthorized().apply(
                content,
                play.api.mvc.JResults.writeString(),
                play.api.mvc.JResults.contentTypeOfString()
            );
        }
        
        public Unauthorized(Content content) {
            wrappedResult = play.api.mvc.JResults.Unauthorized().apply(
                content,
                play.api.mvc.JResults.writeContent(),
                play.api.mvc.JResults.contentTypeOf(content.contentType() + "; charset=utf-8")
            );        
        }
                
        public play.api.mvc.Result getWrappedResult() {
            return this.wrappedResult;
        }
        
    }
    
    /**
     * A 400 BAD_REQUEST simple result.
     */
    public static class BadRequest implements Result {
        
        final private play.api.mvc.Result wrappedResult;
        
        public BadRequest() {
            wrappedResult = play.api.mvc.JResults.BadRequest().apply(
                play.api.mvc.JResults.empty(),
                play.api.mvc.JResults.writeEmptyContent(),
                play.api.mvc.JResults.contentTypeOfEmptyContent()
            );
        }
        
        public BadRequest(String content) {
            wrappedResult = play.api.mvc.JResults.BadRequest().apply(
                content,
                play.api.mvc.JResults.writeString(),
                play.api.mvc.JResults.contentTypeOfString()
            );
        }
        
        public BadRequest(Content content) {
            wrappedResult = play.api.mvc.JResults.BadRequest().apply(
                content,
                play.api.mvc.JResults.writeContent(),
                play.api.mvc.JResults.contentTypeOf(content.contentType() + "; charset=utf-8")
            );        
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