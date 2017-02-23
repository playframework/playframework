/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.di.guice.eager;

import javaguide.di.*;

//#eager-guice-module
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class Module extends AbstractModule {
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
