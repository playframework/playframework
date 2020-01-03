/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.components;

import play.core.j.JavaContextComponents;
import play.core.j.JavaHttpErrorHandlerAdapter;
import play.http.HttpErrorHandler;

/** The HTTP Error handler Java Components. */
public interface HttpErrorHandlerComponents {

  /**
   * @deprecated Deprecated as of 2.8.0. Use the corresponding methods that provide MessagesApi,
   *     Langs, FileMimeTypes or HttpConfiguration.
   */
  @Deprecated
  JavaContextComponents javaContextComponents();

  HttpErrorHandler httpErrorHandler();

  default play.api.http.HttpErrorHandler scalaHttpErrorHandler() {
    return new JavaHttpErrorHandlerAdapter(httpErrorHandler());
  }
}
