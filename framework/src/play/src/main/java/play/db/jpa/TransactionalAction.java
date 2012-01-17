package play.db.jpa;

import play.mvc.*;
import play.mvc.Http.*;

import javax.persistence.*;

/**
 * Wraps an action in am JPA transaction.
 */
public class TransactionalAction extends Action<Transactional> {
    
    public Result call(final Context ctx) throws Throwable {
        return JPA.withTransaction(
            configuration.value(),
            configuration.readOnly(),
            new play.libs.F.Function0<Result>() {
                public Result apply() throws Throwable {
                    return delegate.call(ctx);
                }
            }
        );
    }
    
}
