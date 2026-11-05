/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.di.noscope;

import com.typesafe.config.Config;
import java.util.Arrays;
import java.util.List;
import javaguide.di.noscope.FreshHelloModule.EnglishHello;
import javaguide.di.noscope.FreshHelloModule.Hello;
import play.Environment;
import play.inject.Binding;
import play.inject.Module;
import play.inject.guice.NoScope;

public class UnscopedHelloModule extends Module {
  // #no-scope-self
  @Override
  public List<Binding<?>> bindings(Environment environment, Config config) {
    return Arrays.asList(
        bindClass(Hello.class).to(EnglishHello.class),
        bindClass(EnglishHello.class).toSelf().in(NoScope.class));
  }
  // #no-scope-self
}
