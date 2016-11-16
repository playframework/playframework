/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.db.jpa;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import javax.inject.Inject;

import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Result;

/**
 * Wraps an action in am JPA transaction.
 */
public class TransactionalAction extends Action<Transactional> {

    private JPAApi jpaApi;

    @Inject
    public TransactionalAction(JPAApi jpaApi) {
        this.jpaApi = jpaApi;
    }

    public CompletionStage<Result> call(Context ctx, Function<Context, CompletionStage<Result>> delegate) {
        return jpaApi.withTransaction(
            configuration.value(),
            configuration.readOnly(),
            () -> delegate.apply(ctx)
        );
    }

}
