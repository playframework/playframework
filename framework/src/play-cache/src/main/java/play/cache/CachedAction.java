/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.cache;

import play.libs.F;
import play.mvc.*;
import play.mvc.Http.*;

/**
 * Cache another action.
 */
public class CachedAction extends Action<Cached> {

    public F.Promise<Result> call(Context ctx) {
        try {
            final String key = configuration.key();
            final Integer duration = configuration.duration();
            Result result = (Result) Cache.get(key);
            if(result == null) {
                return delegate.call(ctx).map(result1 -> {
                    Cache.set(key, result1, duration);
                    return result1;
                });
            } else {
                return F.Promise.pure(result);
            }
        } catch(RuntimeException e) {
            throw e;
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

}
