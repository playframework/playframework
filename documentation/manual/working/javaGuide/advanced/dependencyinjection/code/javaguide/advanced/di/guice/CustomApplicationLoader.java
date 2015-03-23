/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.advanced.di.guice;

//#guice-app-loader
import play.api.Application;
import play.api.ApplicationLoader;
import play.api.inject.guice.GuiceApplicationLoader;

public class CustomApplicationLoader extends GuiceApplicationLoader {

  @Override
  public Application load(ApplicationLoader.Context context) {
    // TODO: document how to create a Guice Module for the builder which relies on configuration settings
    return builder(context).build();
  }

}
//#guice-app-loader
