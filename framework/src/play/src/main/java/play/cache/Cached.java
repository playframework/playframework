package play.cache;
import java.lang.annotation.*;

/**
 * Mark an action to be cached on server side.
 *
 * @param key The cache key
 * @param duration Cache duration in seconds
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Cached {
    String key();
    int duration() default 0;
}   
