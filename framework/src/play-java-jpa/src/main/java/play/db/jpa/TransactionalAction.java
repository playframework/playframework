/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db.jpa;

import play.mvc.*;
import play.mvc.Http.*;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;

/**
 * Wraps an action in am JPA transaction.
 */
public class TransactionalAction extends Action<Transactional> {

    private JPAApi jpaApi;

    @Inject
    public TransactionalAction(JPAApi jpaApi) {
        this.jpaApi = jpaApi;
    }

    public CompletionStage<Result> call(final Context ctx) {
        return jpaApi.withTransaction(
            configuration.value(),
            configuration.readOnly(),
            () -> delegate.call(ctx)
        );
    }

}
