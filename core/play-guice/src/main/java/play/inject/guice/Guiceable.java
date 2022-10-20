/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject.guice;

import play.api.inject.guice.GuiceableModule;
import play.api.inject.guice.GuiceableModule$;
import play.libs.Scala;

public class Guiceable {

  public static GuiceableModule modules(com.google.inject.Module... modules) {
    return GuiceableModule$.MODULE$.fromGuiceModules(Scala.toSeq(modules));
  }

  public static GuiceableModule modules(play.api.inject.Module... modules) {
    return GuiceableModule$.MODULE$.fromPlayModules(Scala.toSeq(modules));
  }

  public static GuiceableModule bindings(play.api.inject.Binding... bindings) {
    return GuiceableModule$.MODULE$.fromPlayBindings(Scala.toSeq(bindings));
  }

  public static GuiceableModule module(Object module) {
    return GuiceableModule$.MODULE$.guiceable(module);
  }
}
