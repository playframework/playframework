package play.mvc;

import play.api.*;

import play.mvc.Http.*;
import play.mvc.Result.*;

import play.data.*;

import java.util.*;

public abstract class Controller {
    
    public static Context ctx() {
        return Http.Context.current();
    }
    
    public static Request request() {
        return Http.Context.current().request();
    }
    
    public static Response response() {
        return Http.Context.current().response();
    }
    
    public static Session session() {
        return Http.Context.current().session();
    }
    
    // -- Form
    
    public static DynamicForm form() {
        return new DynamicForm();
    }
    
    public static <T> Form<T> form(Class<T> clazz) {
        return new Form(clazz);
    }
    
    public static <T> Form<T> form(T t) {
        return new Form(t);
    }
    
    // -- Results
    
    public static Result todo() {
        return new Todo();
    }
    
    public static Result ok() {
        return new Ok();
    }
    
    public static Result ok(Content content) {
        return new Ok(content);
    }
    
    public static Result ok(String content) {
        return new Ok(content);
    }
        
    public static Result notFound() {
        return new NotFound();
    }
    
    public static Result notFound(Content content) {
        return new NotFound(content);
    }
    
    public static Result notFound(String content) {
        return new NotFound(content);
    }
        
    public static Result forbidden() {
        return new Forbidden();
    }
    
    public static Result forbidden(Content content) {
        return new Forbidden(content);
    }
    
    public static Result forbidden(String content) {
        return new Forbidden(content);
    }
    
    public static Result unauthorized() {
        return new Unauthorized();
    }
    
    public static Result unauthorized(Content content) {
        return new Unauthorized(content);
    }
    
    public static Result unauthorized(String content) {
        return new Unauthorized(content);
    }
        
    public static Result badRequest() {
        return new BadRequest();
    }
    
    public static Result badRequest(Content content) {
        return new BadRequest(content);
    }
    
    public static Result badRequest(String content) {
        return new BadRequest(content);
    }
        
    public static Result redirect(String url) {
        return new Redirect(url);
    }
    
    public static Result redirect(Call call) {
        return new Redirect(call.url());
    }
    
}