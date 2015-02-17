/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db.jpa;

import play.libs.F;
import play.mvc.*;
import play.mvc.Http.*;

import javax.persistence.*;

/**
 * Wraps an action in am JPA transaction.
 */
public class TransactionalAction extends Action<Transactional> {
    
    public F.Promise<Result> call(final Context ctx) throws Throwable {
        return JPA.withTransaction(
            configuration.value(),
            configuration.readOnly(),
            new play.libs.F.Function0<F.Promise<Result>>() {
                public F.Promise<Result> apply() throws Throwable {
                    return delegate.call(ctx);
                }
            }
        );
    }
    
}
