package play.mvc;

import java.lang.annotation.*;

/**
 * Decorate an Action or a Controller with another Action.
 */ 
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME )
public @interface With {
    Class<? extends Action<?>> value();
}