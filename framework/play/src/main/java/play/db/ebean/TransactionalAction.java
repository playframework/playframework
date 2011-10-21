package play.db.ebean;

import play.mvc.*;
import play.mvc.Http.*;

import com.avaje.ebean.*;

/**
 * Wrap an action into am Ebean transaction.
 */
public class TransactionalAction extends Action<Transactional> {
    
    public Result call(final Context ctx) {
        return Ebean.execute(new TxCallable<Result>() {  
            public Result call() {
                return deleguate.call(ctx);
            }
        });
    }
    
}