/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core;

/**
 * Interface used by the build to call a DocumentationHandler. We don't use
 * a DocumentationHandler directly because Play's build and application code can be compiled
 * with different versions of Scala and there can be binary compatibility problems.
 *
 * <p>BuildDocHandler objects can created by calling the static methods on BuildDocHandlerFactory.
 *
 * <p>This interface is written in Java and uses only Java types so that
 * communication can work even when the calling code and the play-docs project
 * are built with different versions of Scala.
 */
public interface BuildDocHandler {

  /**
   * Given a request, either handle it and return some result, or don't, and return none.
   *
   * @param request A request of type {@code play.api.mvc.RequestHeader}.
   * @return A value of type {@code Option<play.api.mvc.SimpleResult>}, Some if the result was
   *        handled, None otherwise.
   */
  public Object maybeHandleDocRequest(Object request);

}
