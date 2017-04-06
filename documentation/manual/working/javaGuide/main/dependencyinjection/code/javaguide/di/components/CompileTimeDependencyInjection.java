/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.di.components;

//#basic-imports
import play.Application;
import play.ApplicationLoader;
import play.BuiltInComponentsFromContext;
import play.api.LoggerConfigurator$;
import play.db.ConnectionPool;
import play.db.HikariCPComponents;
import play.filters.components.HttpFiltersComponents;
import play.routing.Router;
//###skip: 1
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
    public class MyComponents extends BuiltInComponentsFromContext implements HttpFiltersComponents {

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
}
