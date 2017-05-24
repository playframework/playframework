/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.forms.customconstraint;

// #constraint-compile-timed-di
import play.ApplicationLoader;
import play.BuiltInComponentsFromContext;
import play.data.FormFactoryComponents;
import play.data.validation.StaticConstraintValidatorFactory;
import play.db.ConnectionPool;
import play.db.DBComponents;
import play.db.HikariCPComponents;
import play.filters.components.NoHttpFiltersComponents;
import play.routing.Router;

public class ValidateWithDBComponents extends BuiltInComponentsFromContext
        implements FormFactoryComponents, DBComponents, HikariCPComponents, NoHttpFiltersComponents {

    public ValidateWithDBComponents(ApplicationLoader.Context context) {
        super(context);
    }

    @Override
    public Router router() {
        return Router.empty();
    }

    @Override
    public StaticConstraintValidatorFactory constraintValidatorFactory() {
        return new StaticConstraintValidatorFactory()
                .addConstraintValidator(
                        ValidateWithDBValidator.class,
                        new ValidateWithDBValidator(database("default"))
                );
    }
}

// #constraint-compile-timed-di
