/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.components;

import play.core.j.JavaContextComponents;
import play.core.j.JavaHttpErrorHandlerAdapter;
import play.http.HttpErrorHandler;

/**
 * The HTTP Error handler Java Components.
 */
public interface HttpErrorHandlerComponents {

    JavaContextComponents javaContextComponents();

    HttpErrorHandler httpErrorHandler();

    default play.api.http.HttpErrorHandler scalaHttpErrorHandler() {
        return new JavaHttpErrorHandlerAdapter(httpErrorHandler(), javaContextComponents());
    }
}
