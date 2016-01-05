/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db.jpa;

import play.mvc.*;
import play.mvc.Http.*;

import java.util.concurrent.CompletionStage;

/**
 * Wraps an action in am JPA transaction.
 */
public class TransactionalAction extends Action<Transactional> {

    public CompletionStage<Result> call(final Context ctx) {
        return JPA.withTransaction(
            configuration.value(),
            configuration.readOnly(),
            () -> delegate.call(ctx)
        );
    }

}
