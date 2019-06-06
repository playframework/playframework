/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.forms;

// #register-formatter
import com.google.inject.AbstractModule;

import play.data.format.Formatters;

public class FormattersModule extends AbstractModule {

  @Override
  protected void configure() {

    bind(Formatters.class).toProvider(FormattersProvider.class);
  }
}
// #register-formatter
