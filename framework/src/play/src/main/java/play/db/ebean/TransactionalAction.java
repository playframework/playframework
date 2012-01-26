package play.db.ebean;

import play.mvc.*;
import play.mvc.Http.*;

import com.avaje.ebean.*;

/**
 * Wraps an action in an Ebean transaction.
 */
public class TransactionalAction extends Action<Transactional> {
    
    public Result call(final Context ctx) throws Throwable {
        return Ebean.execute(new TxCallable<Result>() {  
            public Result call() {
                try {
                    return delegate.call(ctx);
                } catch(RuntimeException e) {
                    throw e;
                } catch(Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        });
    }
    
}
