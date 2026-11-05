/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.di.noscope;

import com.typesafe.config.Config;
import jakarta.inject.Singleton;
import java.util.Collections;
import java.util.List;
import play.Environment;
import play.inject.Binding;
import play.inject.Module;
import play.inject.guice.NoScope;

public class FreshHelloModule extends Module {
  // #no-scope-construction
  @Override
  public List<Binding<?>> bindings(Environment environment, Config config) {
    return Collections.singletonList(
        bindClass(Hello.class).to(EnglishHello.class).in(NoScope.class));
  }

  // #no-scope-construction

  public interface Hello {}

  @Singleton
  public static class EnglishHello implements Hello {}
}
