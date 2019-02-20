/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.di.guice.configured;

import javaguide.di.*;

// #dynamic-guice-module
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import play.Environment;

public class Module extends AbstractModule {

  private final Environment environment;
  private final Config config;

  public Module(Environment environment, Config config) {
    this.environment = environment;
    this.config = config;
  }

  protected void configure() {
    // Expect configuration like:
    // hello.en = "myapp.EnglishHello"
    // hello.de = "myapp.GermanHello"
    final Config helloConf = config.getConfig("hello");
    // Iterate through all the languages and bind the
    // class associated with that language. Use Play's
    // ClassLoader to load the classes.
    helloConf
        .entrySet()
        .forEach(
            entry -> {
              try {
                String name = entry.getKey();
                Class<? extends Hello> bindingClass =
                    environment
                        .classLoader()
                        .loadClass(entry.getValue().toString())
                        .asSubclass(Hello.class);
                bind(Hello.class).annotatedWith(Names.named(name)).to(bindingClass);
              } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
              }
            });
  }
}
// #dynamic-guice-module
