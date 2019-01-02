/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db.jpa;

import play.mvc.*;

import java.lang.annotation.*;

/**
 * Wraps the annotated action in an JPA transaction.
 *
 * This is a deprecated class. An injected JPAApi instance should be used instead.
 *
 * Please see <a href="https://www.playframework.com/documentation/latest/JavaJPA#Using-play.db.jpa.JPAApi">Using play.db.jpa.JPAApi</a> for more details.
 *
 * @deprecated Use a dependency injected JPAApi instance here, since 2.7.0
 */
@With(TransactionalAction.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface Transactional {
    String value() default "default";
    boolean readOnly() default false;
}
