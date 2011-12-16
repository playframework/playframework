package play.db.jpa;

import play.mvc.*;
import play.mvc.Http.*;

import java.util.*;
import java.lang.annotation.*;

/**
 * Wraps the annotated action in an JPA transaction.
 */
@With(TransactionalAction.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Transactional {
    String value() default "default";
    boolean readOnly() default false;
}