/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.advanced.di.guice;

//#guice-app-loader
import com.google.inject.AbstractModule;
import com.google.inject.Module;

import play.api.Application;
import play.api.ApplicationLoader;
import play.api.inject.guice.GuiceApplicationLoader;

public class CustomApplicationLoader extends GuiceApplicationLoader {

  @Override
  public Module[] guiceModules(ApplicationLoader.Context context) {
    return new Module[] { new MyModule() };
  }

  private static class MyModule extends AbstractModule {
    @Override 
    protected void configure() {}
  }

}
//#guice-app-loader
