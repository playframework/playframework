/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.pekko.modules;

import javaguide.pekko.ConfiguredActor;

// #binding
import com.google.inject.AbstractModule;
import play.libs.pekko.PekkoGuiceSupport;

public class MyModule extends AbstractModule implements PekkoGuiceSupport {
  @Override
  protected void configure() {
    bindActor(ConfiguredActor.class, "configured-actor");
  }
}
// #binding
