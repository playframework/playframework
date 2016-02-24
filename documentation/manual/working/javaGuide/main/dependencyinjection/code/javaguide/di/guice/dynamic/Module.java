/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.di.guice.configured;

import javaguide.di.*;

//#dynamic-guice-module
import com.google.inject.AbstractModule;
import com.google.inject.ConfigurationException;
import com.google.inject.name.Names;
import play.Configuration;
import play.Environment;

public class Module extends AbstractModule {

    private final Environment environment;
    private final Configuration configuration;

    public Module(
          Environment environment,
          Configuration configuration) {
        this.environment = environment;
        this.configuration = configuration;
    }

    protected void configure() {
        // Expect configuration like:
        // hello.en = "myapp.EnglishHello"
        // hello.de = "myapp.GermanHello"
        Configuration helloConf = configuration.getConfig("hello");
        // Iterate through all the languages and bind the
        // class associated with that language. Use Play's
        // ClassLoader to load the classes.
        for (String l: helloConf.subKeys()) {
            try {
                String bindingClassName = helloConf.getString(l);
                Class<? extends Hello> bindingClass =
                  environment.classLoader().loadClass(bindingClassName)
                  .asSubclass(Hello.class);
                bind(Hello.class)
                        .annotatedWith(Names.named(l))
                        .to(bindingClass);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
//#dynamic-guice-module
