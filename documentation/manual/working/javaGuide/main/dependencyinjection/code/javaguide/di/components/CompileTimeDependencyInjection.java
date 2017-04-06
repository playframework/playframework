/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.di.components;

//#basic-imports
import controllers.HomeController;
import play.Application;
import play.ApplicationLoader;
import play.BuiltInComponentsFromContext;
import play.api.LoggerConfigurator$;
import play.controllers.AssetsComponents;
import play.db.ConnectionPool;
import play.db.HikariCPComponents;
import play.filters.components.HttpFiltersComponents;
import play.mvc.Results;
import play.routing.Router;
import play.routing.RoutingDslComponentsFromContext;
import scala.compat.java8.OptionConverters;
//#basic-imports

public class CompileTimeDependencyInjection {

    //#basic-app-loader
    public class MyApplicationLoader implements ApplicationLoader {

        @Override
        public Application load(Context context) {
            return new MyComponents(context).application();
        }

    }
    //#basic-app-loader

    //#basic-my-components
    public class MyComponents extends BuiltInComponentsFromContext
            implements HttpFiltersComponents {

        public MyComponents(ApplicationLoader.Context context) {
            super(context);
        }

        @Override
        public Router router() {
            return Router.empty();
        }
    }
    //#basic-my-components

    //#basic-logger-configurator
    //###insert: import scala.compat.java8.OptionConverters;
    public class MyAppLoaderWithLoggerConfiguration implements ApplicationLoader {
        @Override
        public Application load(Context context) {

            OptionConverters.toJava(
                LoggerConfigurator$.MODULE$.apply(context.environment().classLoader())
            ).ifPresent(loggerConfigurator ->
                loggerConfigurator.configure(context.environment().asScala())
            );

            return new MyComponents(context).application();
        }
    }
    //#basic-logger-configurator

    //#connection-pool
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
    //#connection-pool

    static class SomeComponent {
        private final ConnectionPool pool;

        SomeComponent(ConnectionPool pool) {
            this.pool = pool;
        }
    }

    //#with-routing-dsl
    public class MyComponentsWithRouter extends RoutingDslComponentsFromContext
            implements HttpFiltersComponents {

        public MyComponentsWithRouter(ApplicationLoader.Context context) {
            super(context);
        }

        @Override
        public Router router() {
            //
            return routingDsl()
                    .GET("/path").routeTo(() -> Results.ok("The content"))
                    .build();
        }
    }
    //#with-routing-dsl

    //#with-generated-router
    public class MyComponentsWithGeneratedRouter extends BuiltInComponentsFromContext
            implements HttpFiltersComponents, AssetsComponents {

        public MyComponentsWithGeneratedRouter(ApplicationLoader.Context context) {
            super(context);
        }

        @Override
        public Router router() {
            bar.Routes barRoutes = new bar.Routes(scalaHttpErrorHandler());
            HomeController homeController = new HomeController();
            return new router.Routes(scalaHttpErrorHandler(), homeController, barRoutes, assets());
        }
    }
    //#with-generated-router
}
