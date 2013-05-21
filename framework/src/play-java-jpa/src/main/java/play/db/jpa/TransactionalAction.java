package play.db.jpa;

import play.libs.F;
import play.mvc.*;
import play.mvc.Http.*;

import javax.persistence.*;

/**
 * Wraps an action in am JPA transaction.
 */
public class TransactionalAction extends Action<Transactional> {
    
    public F.Promise<SimpleResult> call(final Context ctx) throws Throwable {
        return JPA.withTransactionAsync(
            configuration.value(),
            configuration.readOnly(),
            new play.libs.F.Function0<F.Promise<SimpleResult>>() {
                public F.Promise<SimpleResult> apply() throws Throwable {
                    return delegate.call(ctx);
                }
            }
        );
    }
    
}
