package play.db.jpa;

import play.mvc.*;
import play.mvc.Http.*;

import javax.persistence.*;

/**
 * Wraps an action in am JPA transaction.
 */
public class TransactionalAction extends Action<Transactional> {

    public Result call(final Context ctx) throws Exception {
        return JPA.withTransaction(
            configuration.value(),
            configuration.readOnly(),
            new play.libs.F.Function0<Result>() {
                public Result apply() throws Exception {
                    return delegate.call(ctx);
                }
            }
        );
    }

}
