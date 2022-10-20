/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

// ###replace: package tasks;
package javaguide.scheduling;

import com.google.inject.AbstractModule;

public class TasksModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(MyActorTask.class).asEagerSingleton();
  }
}
