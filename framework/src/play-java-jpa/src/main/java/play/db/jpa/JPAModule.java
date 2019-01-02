/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db.jpa;

import com.typesafe.config.Config;
import play.Environment;
import play.inject.Binding;
import play.inject.Module;

import java.util.Arrays;
import java.util.List;

/**
 * Injection module with default JPA components.
 */
public class JPAModule extends Module {

    @Override
    public List<Binding<?>> bindings(final Environment environment, final Config config) {
        return Arrays.asList(
            bindClass(JPAApi.class).toProvider(DefaultJPAApi.JPAApiProvider.class),
            bindClass(JPAConfig.class).toProvider(DefaultJPAConfig.JPAConfigProvider.class)
        );
    }

}
