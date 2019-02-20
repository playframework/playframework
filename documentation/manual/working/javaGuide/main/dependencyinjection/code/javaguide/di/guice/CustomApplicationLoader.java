/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.di.guice;

// #custom-application-loader
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import play.ApplicationLoader;
import play.inject.guice.GuiceApplicationBuilder;
import play.inject.guice.GuiceApplicationLoader;

public class CustomApplicationLoader extends GuiceApplicationLoader {

  @Override
  public GuiceApplicationBuilder builder(ApplicationLoader.Context context) {
    Config extra = ConfigFactory.parseString("a = 1");
    return initialBuilder
        .in(context.environment())
        .loadConfig(extra.withFallback(context.initialConfig()))
        .overrides(overrides(context));
  }
}
// #custom-application-loader
