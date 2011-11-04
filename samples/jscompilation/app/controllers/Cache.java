package controllers;

import play.mvc.*;
import play.mvc.Http.*;

import java.util.*;
import java.lang.annotation.*;

public class Cache {
    
    static HashMap<String,Result> cache = new HashMap<String,Result>();
    
    @With(CacheAction.class)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Cached {
    }
    
    public static class CacheAction extends Action<Cache> {
        
        public Result call(Context ctx) {
            System.out.println("CHECK CACHE");
            String key = ctx.request().path();
            if(cache.containsKey(key)) {
                return cache.get(key);
            } else {
                Result r = deleguate.call(ctx);
                cache.put(key, r);
                return r;
            }
        }

    }
    
}