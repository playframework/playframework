/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import java.lang.reflect.AnnotatedElement;
import java.util.concurrent.CompletionStage;

import play.core.j.JavaContextComponents;
import play.mvc.Http.Request;

/** An action acts as decorator for the action method call. */
public abstract class Action<T> extends Results {

  /** @deprecated Deprecated as of 2.8.0. Method does nothing. */
  @Deprecated
  public void setContextComponents(JavaContextComponents contextComponents) {}

  /** The action configuration - typically the annotation used to decorate the action method. */
  public T configuration;

  /** Where an action was defined. */
  public AnnotatedElement annotatedElement;

  /**
   * The precursor action.
   *
   * <p>If this action was called in a chain then this will contain the value of the action that is
   * called before this action. If no action was called first, then this value will be null.
   */
  public Action<?> precursor;

  /**
   * The wrapped action.
   *
   * <p>If this action was called in a chain then this will contain the value of the action that is
   * called after this action. If there is no action left to be called, then this value will be
   * null.
   */
  public Action<?> delegate;

  /**
   * Executes this action with the given HTTP request and returns the result.
   *
   * @param req the http request with which to execute this action
   * @return a promise to the action's result
   */
  public abstract CompletionStage<Result> call(Request req);

  /** A simple action with no configuration. */
  public abstract static class Simple extends Action<Void> {}
}
