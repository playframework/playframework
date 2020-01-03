/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.j;

import play.api.http.HttpConfiguration;
import play.http.ActionCreator;
import play.mvc.Action;
import play.mvc.BodyParser;
import scala.concurrent.ExecutionContext;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The components necessary to handle a Java handler.
 *
 * <p>But this implementation does not uses an Injector. Instead, the necessary {@link
 * play.mvc.Action} and {@link play.mvc.BodyParser} must be added here manually. This is way we
 * avoid mixing runtime dependency injector components with compile time injected ones.
 */
public class MappedJavaHandlerComponents implements JavaHandlerComponents {

  private final ActionCreator actionCreator;
  private final HttpConfiguration httpConfiguration;
  private final ExecutionContext executionContext;
  private final JavaContextComponents contextComponents;

  private final Map<Class<? extends Action<?>>, Supplier<Action<?>>> actions = new HashMap<>();
  private final Map<Class<? extends BodyParser<?>>, Supplier<BodyParser<?>>> bodyPasers =
      new HashMap<>();

  public MappedJavaHandlerComponents(
      ActionCreator actionCreator,
      HttpConfiguration httpConfiguration,
      ExecutionContext executionContext) {
    this(actionCreator, httpConfiguration, executionContext, null);
  }

  /** @deprecated Deprecated as of 2.8.0. Use constructor without JavaContextComponents */
  @Deprecated
  public MappedJavaHandlerComponents(
      ActionCreator actionCreator,
      HttpConfiguration httpConfiguration,
      ExecutionContext executionContext,
      JavaContextComponents contextComponents) {
    this.actionCreator = actionCreator;
    this.httpConfiguration = httpConfiguration;
    this.executionContext = executionContext;
    this.contextComponents = contextComponents;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <A extends BodyParser<?>> A getBodyParser(Class<A> parserClass) {
    return (A) this.bodyPasers.get(parserClass).get();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <A extends Action<?>> A getAction(Class<A> actionClass) {
    return (A) this.actions.get(actionClass).get();
  }

  @Override
  public ActionCreator actionCreator() {
    return this.actionCreator;
  }

  @Override
  public HttpConfiguration httpConfiguration() {
    return this.httpConfiguration;
  }

  @Override
  public ExecutionContext executionContext() {
    return this.executionContext;
  }

  @Deprecated
  @Override
  public JavaContextComponents contextComponents() {
    return this.contextComponents;
  }

  public <A extends Action<?>> MappedJavaHandlerComponents addAction(
      Class<A> clazz, Supplier<A> actionSupplier) {
    actions.put(clazz, widenSupplier(actionSupplier));
    return this;
  }

  public <B extends BodyParser<?>> MappedJavaHandlerComponents addBodyParser(
      Class<B> clazz, Supplier<B> bodyParserSupplier) {
    bodyPasers.put(clazz, widenSupplier(bodyParserSupplier));
    return this;
  }

  @SuppressWarnings("unchecked")
  // covariance: Supplier<?> <: Supplier<Object>, given Supplier<A> is covariant in A
  static <A extends B, B> Supplier<B> widenSupplier(final Supplier<A> parser) {
    return (Supplier<B>) parser;
  }
}
