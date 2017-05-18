/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.cache;

import play.cache.Cache;
import play.libs.F;
import play.mvc.*;
import play.mvc.Http.*;

/**
 * Invalidate cache after trigger action.
 */
public class CachedInvalidateAction extends Action<CachedInvalidate> {

    public F.Promise<Result> call(Context ctx) {
        try {
            final String[] keys = configuration.keys();
            F.Promise<Result> promise = delegate.call(ctx);
            for(String key: keys){
                Cache.remove(key);
            }
            return promise;
        } catch(RuntimeException e) {
            throw e;
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

}
