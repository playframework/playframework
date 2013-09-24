/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db.ebean;

import play.mvc.*;
import play.mvc.Http.*;

import java.util.*;
import java.lang.annotation.*;

/**
 * Wraps the annotated action in an Ebean transaction.
 */
@With(TransactionalAction.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Transactional {
}