/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.advanced.di.guice.eager;

import javaguide.advanced.di.*;

//#eager-guice-module
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class HelloModule extends AbstractModule {
    protected void configure() {

        bind(Hello.class)
                .annotatedWith(Names.named("en"))
                .to(EnglishHello.class)
                .asEagerSingleton();

        bind(Hello.class)
                .annotatedWith(Names.named("de"))
                .to(GermanHello.class)
                .asEagerSingleton();
    }
}
//#eager-guice-module
