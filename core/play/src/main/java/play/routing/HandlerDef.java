/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.routing;

import java.util.List;
import play.libs.Scala;
import scala.collection.Seq;

public abstract class HandlerDef {
  public abstract ClassLoader classLoader();

  public abstract String routerPackage();

  public abstract String controller();

  public abstract String method();

  protected abstract Seq<Class<?>> parameterTypes();

  public abstract String verb();

  public abstract String path();

  public abstract String comments();

  protected abstract Seq<String> modifiers();

  public List<Class<?>> getParameterTypes() {
    return Scala.asJava(parameterTypes());
  }

  public List<String> getModifiers() {
    return Scala.asJava(modifiers());
  }
}
