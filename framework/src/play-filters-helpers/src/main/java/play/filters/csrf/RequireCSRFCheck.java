/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.csrf;

import play.mvc.With;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This action requires a CSRF check.
 */
@With(RequireCSRFCheckAction.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequireCSRFCheck {

    /**
     * Calls a implementation class for handling the CSRF error.
     *
     * @see play.filters.csrf.CSRFErrorHandler
     * @return the subtype of CSRFErrorHandler
     */
    Class<? extends CSRFErrorHandler> error() default CSRFErrorHandler.DefaultCSRFErrorHandler.class;

}
