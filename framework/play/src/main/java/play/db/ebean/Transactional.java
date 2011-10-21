package play.db.ebean;

import play.mvc.*;
import play.mvc.Http.*;

import java.util.*;
import java.lang.annotation.*;

/**
 * Wrap the annotated action into an Ebean transaction.
 */
@With(TransactionalAction.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Transactional {
}