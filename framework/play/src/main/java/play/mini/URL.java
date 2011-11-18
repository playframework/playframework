package play.mini;
import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface URL {
   String value() default "/";
   String id() default "";
}   
