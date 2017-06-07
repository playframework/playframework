/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.di.guice.configured;

import com.typesafe.config.Config;
import javaguide.di.*;

//#dynamic-guice-module
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import play.Environment;

public class Module extends AbstractModule {

    private final Environment environment;
    private final Config configuration;

    public Module(
          Environment environment,
          Config configuration) {
        this.environment = environment;
        this.configuration = configuration;
    }

    protected void configure() {
        // Expect configuration like:
        // hello.en = "myapp.EnglishHello"
        // hello.de = "myapp.GermanHello"
        final Config helloConf = configuration.getConfig("hello");
        // Iterate through all the languages and bind the
        // class associated with that language. Use Play's
        // ClassLoader to load the classes.
        helloConf.entrySet().forEach(entry -> {
            try {
                String name = entry.getKey();
                Class<? extends Hello> bindingClass = environment
                        .classLoader()
                        .loadClass(entry.getValue().toString())
                        .asSubclass(Hello.class);
                bind(Hello.class)
                        .annotatedWith(Names.named(name))
                        .to(bindingClass);
            } catch (ClassNotFoundException ex) {
              throw new RuntimeException(ex);
            }
        });
    }
}
//#dynamic-guice-module
