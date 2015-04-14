/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db.jpa;

import play.libs.F;
import play.mvc.*;
import play.mvc.Http.*;

/**
 * Wraps an action in am JPA transaction.
 */
public class TransactionalAction extends Action<Transactional> {

    public F.Promise<Result> call(final Context ctx) throws Throwable {
        return JPA.withTransaction(
            configuration.value(),
            configuration.readOnly(),
            () -> delegate.call(ctx)
        );
    }

}
