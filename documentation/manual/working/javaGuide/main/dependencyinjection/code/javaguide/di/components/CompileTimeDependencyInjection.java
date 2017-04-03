/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.di.components;

//#basic-imports
import play.Application;
import play.ApplicationLoader;
import play.BuiltInComponentsFromContext;
import play.filters.components.HttpFiltersComponents;
import play.routing.Router;
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
}
