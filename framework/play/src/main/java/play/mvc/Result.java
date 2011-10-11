package play.mvc;

import play.api.*;
import play.api.mvc.Results.* ;

public interface Result {

    public play.api.mvc.Result getWrappedResult();
    
    public static class Todo implements Result {
        
        final private play.api.mvc.Result wrappedResult;
        
        public Todo() {
            wrappedResult = play.api.mvc.JResults.NotImplemented().apply(
                play.core.views.html.todo.render(),
                play.api.mvc.JResults.emptyHeaders(), 
                play.api.mvc.JResults.writeContent(),
                play.api.mvc.JResults.contentTypeOfContent()
            );
        }
        
        public play.api.mvc.Result getWrappedResult() {
            return this.wrappedResult;
        }
        
    }
    
    public static class Ok implements Result {
        
        final private play.api.mvc.Result wrappedResult;
        
        public Ok() {
            wrappedResult = play.api.mvc.JResults.Ok().apply(
                play.api.mvc.JResults.empty(),
                play.api.mvc.JResults.emptyHeaders(), 
                play.api.mvc.JResults.writeEmpty(),
                play.api.mvc.JResults.contentTypeOfEmpty()
            );
        }
        
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
        
        public NotFound() {
            wrappedResult = play.api.mvc.JResults.NotFound().apply(
                play.api.mvc.JResults.empty(),
                play.api.mvc.JResults.emptyHeaders(), 
                play.api.mvc.JResults.writeEmpty(),
                play.api.mvc.JResults.contentTypeOfEmpty()
            );
        }
        
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
        
        public InternalServerError() {
            wrappedResult = play.api.mvc.JResults.InternalServerError().apply(
                play.api.mvc.JResults.empty(),
                play.api.mvc.JResults.emptyHeaders(), 
                play.api.mvc.JResults.writeEmpty(),
                play.api.mvc.JResults.contentTypeOfEmpty()
            );
        }
        
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
        
        public Forbidden() {
            wrappedResult = play.api.mvc.JResults.Forbidden().apply(
                play.api.mvc.JResults.empty(),
                play.api.mvc.JResults.emptyHeaders(), 
                play.api.mvc.JResults.writeEmpty(),
                play.api.mvc.JResults.contentTypeOfEmpty()
            );
        }
        
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
    
    public static class Unauthorized implements Result {
        
        final private play.api.mvc.Result wrappedResult;
        
        public Unauthorized() {
            wrappedResult = play.api.mvc.JResults.Unauthorized().apply(
                play.api.mvc.JResults.empty(),
                play.api.mvc.JResults.emptyHeaders(), 
                play.api.mvc.JResults.writeEmpty(),
                play.api.mvc.JResults.contentTypeOfEmpty()
            );
        }
        
        public Unauthorized(String content) {
            wrappedResult = play.api.mvc.JResults.Unauthorized().apply(
                content,
                play.api.mvc.JResults.emptyHeaders(), 
                play.api.mvc.JResults.writeString(),
                play.api.mvc.JResults.contentTypeOfString()
            );
        }
        
        public Unauthorized(Content content) {
            wrappedResult = play.api.mvc.JResults.Unauthorized().apply(
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
        
        public BadRequest() {
            wrappedResult = play.api.mvc.JResults.BadRequest().apply(
                play.api.mvc.JResults.empty(),
                play.api.mvc.JResults.emptyHeaders(), 
                play.api.mvc.JResults.writeEmpty(),
                play.api.mvc.JResults.contentTypeOfEmpty()
            );
        }
        
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