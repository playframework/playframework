/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.di.components;

// #basic-imports
import play.Application;
import play.ApplicationLoader;
import play.BuiltInComponentsFromContext;
import play.LoggerConfigurator;
import play.controllers.AssetsComponents;
import play.db.ConnectionPool;
import play.db.HikariCPComponents;
import play.filters.components.HttpFiltersComponents;
import play.mvc.Results;
import play.routing.Router;
import play.routing.RoutingDslComponentsFromContext;
// #basic-imports

import javaguide.dependencyinjection.controllers.Assets;
import javaguide.dependencyinjection.controllers.HomeController;

public class CompileTimeDependencyInjection {

  // #basic-app-loader
  public class MyApplicationLoader implements ApplicationLoader {

    @Override
    public Application load(Context context) {
      return new MyComponents(context).application();
    }
  }
  // #basic-app-loader

  // #basic-my-components
  public class MyComponents extends BuiltInComponentsFromContext implements HttpFiltersComponents {

    public MyComponents(ApplicationLoader.Context context) {
      super(context);
    }

    @Override
    public Router router() {
      return Router.empty();
    }
  }
  // #basic-my-components

  // #basic-logger-configurator
  // ###insert: import scala.compat.java8.OptionConverters;
  public class MyAppLoaderWithLoggerConfiguration implements ApplicationLoader {
    @Override
    public Application load(Context context) {

      LoggerConfigurator.apply(context.environment().classLoader())
          .ifPresent(
              loggerConfigurator ->
                  loggerConfigurator.configure(context.environment(), context.initialConfig()));

      return new MyComponents(context).application();
    }
  }
  // #basic-logger-configurator

  // #connection-pool
  public class MyComponentsWithDatabase extends BuiltInComponentsFromContext
      implements HikariCPComponents, HttpFiltersComponents {

    public MyComponentsWithDatabase(ApplicationLoader.Context context) {
      super(context);
    }

    @Override
    public Router router() {
      return Router.empty();
    }

    public SomeComponent someComponent() {
      // connectionPool method is provided by HikariCPComponents
      return new SomeComponent(connectionPool());
    }
  }
  // #connection-pool

  static class SomeComponent {
    SomeComponent(ConnectionPool pool) {
      // do nothing
    }
  }

  // #with-routing-dsl
  public class MyComponentsWithRouter extends RoutingDslComponentsFromContext
      implements HttpFiltersComponents {

    public MyComponentsWithRouter(ApplicationLoader.Context context) {
      super(context);
    }

    @Override
    public Router router() {
      // routingDsl method is provided by RoutingDslComponentsFromContext
      return routingDsl().GET("/path").routeTo(() -> Results.ok("The content")).build();
    }
  }
  // #with-routing-dsl

  // #with-generated-router
  public class MyComponentsWithGeneratedRouter extends BuiltInComponentsFromContext
      implements HttpFiltersComponents, AssetsComponents {

    public MyComponentsWithGeneratedRouter(ApplicationLoader.Context context) {
      super(context);
    }

    @Override
    public Router router() {
      HomeController homeController = new HomeController();
      Assets assets = new Assets(scalaHttpErrorHandler(), assetsMetadata());
      // ###replace: return new router.Routes(scalaHttpErrorHandler(), homeController,
      // assets).asJava();
      return new javaguide.dependencyinjection.Routes(
              scalaHttpErrorHandler(), homeController, assets)
          .asJava();
    }
  }
  // #with-generated-router
}
