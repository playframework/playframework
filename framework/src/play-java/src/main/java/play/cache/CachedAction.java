package play.cache;

import play.libs.F;
import play.mvc.*;
import play.mvc.Http.*;

/**
 * Cache another action.
 */
public class CachedAction extends Action<Cached> {
    
    public F.Promise<SimpleResult> call(Context ctx) {
        try {
            final String key = configuration.key();
            final Integer duration = configuration.duration();
            F.Promise<SimpleResult> result = F.Promise.pure((SimpleResult) Cache.get(key));
            if(result == null) {
                result = delegate.call(ctx);
                result.onRedeem(new F.Callback<SimpleResult>() {
                    @Override
                    public void invoke(SimpleResult simpleResult) throws Throwable {
                        Cache.set(key, simpleResult, duration);
                    }
                });
            }
            return result;
        } catch(RuntimeException e) {
            throw e;
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

}