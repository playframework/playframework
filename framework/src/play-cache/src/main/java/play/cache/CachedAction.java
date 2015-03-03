/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.cache;

import play.libs.F;
import play.mvc.*;
import play.mvc.Http.*;

/**
 * Cache another action or invalidate it.
 */
public class CachedAction extends Action<Cached> {

    public F.Promise<Result> call(Context ctx) {
        try {
            final String key = configuration.key();
            final Integer duration = configuration.duration();
            final Boolean remove = configuration.remove();
            F.Promise<Result> promise;
            if(remove){
                Cache.remove(key);
                promise = delegate.call(ctx);
            }else {
                Result result = (Result) Cache.get(key);
                if (result == null) {
                    promise = delegate.call(ctx);
                    promise.onRedeem(new F.Callback<Result>() {
                        @Override
                        public void invoke(Result result) throws Throwable {
                            Cache.set(key, result, duration);
                        }
                    });
                } else {
                    promise = F.Promise.pure(result);
                }
            }
            return promise;
        } catch(RuntimeException e) {
            throw e;
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

}
