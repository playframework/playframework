/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.routing;

import java.util.List;
import scala.collection.Seq;

import play.libs.Scala;

public abstract class HandlerDef {
  public abstract ClassLoader classLoader();

  public abstract String routerPackage();

  public abstract String controller();

  public abstract String method();

  protected abstract Seq<Class<?>> parameterTypes();

  public List<Class<?>> getParameterTypes() {
    return Scala.asJava(parameterTypes());
  }
}
