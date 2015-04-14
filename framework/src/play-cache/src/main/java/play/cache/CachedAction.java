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
            F.Promise<Result> promise;
            if(result == null) {
                promise = delegate.call(ctx);
                promise.onRedeem(result1 -> Cache.set(key, result1, duration));
            } else {
                promise = F.Promise.pure(result);
            }
            return promise;
        } catch(RuntimeException e) {
            throw e;
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

}
