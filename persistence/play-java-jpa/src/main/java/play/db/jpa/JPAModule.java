/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db.jpa;

import com.typesafe.config.Config;
import java.util.Arrays;
import java.util.List;
import play.Environment;
import play.inject.Binding;
import play.inject.Module;

/** Injection module with default JPA components. */
public class JPAModule extends Module {

  @Override
  public List<Binding<?>> bindings(final Environment environment, final Config config) {
    return Arrays.asList(
        bindClass(JPAApi.class).toProvider(DefaultJPAApi.JPAApiProvider.class),
        bindClass(JPAConfig.class).toProvider(DefaultJPAConfig.JPAConfigProvider.class));
  }
}
