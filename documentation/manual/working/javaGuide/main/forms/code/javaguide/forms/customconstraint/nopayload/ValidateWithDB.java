/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.forms.customconstraint.nopayload;

// #annotation
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Target({TYPE, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Repeatable(ValidateWithDB.List.class)
@Constraint(validatedBy = ValidateWithDBValidator.class)
public @interface ValidateWithDB {
  String message() default "error.invalid";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  /** Defines several {@code @ValidateWithDB} annotations on the same element. */
  @Target({TYPE, ANNOTATION_TYPE})
  @Retention(RUNTIME)
  public @interface List {
    ValidateWithDB[] value();
  }
}
// #annotation
