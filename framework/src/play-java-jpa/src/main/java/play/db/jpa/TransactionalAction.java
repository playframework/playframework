/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db.jpa;

import play.mvc.*;
import play.mvc.Http.*;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;

/**
 * Wraps an action in am JPA transaction.
 *
 * This is a deprecated class. An injected JPAApi instance should be used instead.
 *
 * Please see <a href="https://www.playframework.com/documentation/latest/JavaJPA#Using-play.db.jpa.JPAApi">Using play.db.jpa.JPAApi</a> for more details.
 *
 * @deprecated Use a dependency injected JPAApi instance here, since 2.7.0
 */
@Deprecated
public class TransactionalAction extends Action<Transactional> {

    private JPAApi jpaApi;

    @Inject
    public TransactionalAction(JPAApi jpaApi) {
        this.jpaApi = jpaApi;
    }

    public CompletionStage<Result> call(final Request req) {
        return jpaApi.withTransaction(
            configuration.value(),
            configuration.readOnly(),
            () -> delegate.call(req)
        );
    }

}
