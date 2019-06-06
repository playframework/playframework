/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
//###replace: package tasks
package scalaguide.scheduling

import play.api.inject.SimpleModule
import play.api.inject._

class TasksModule extends SimpleModule(bind[MyActorTask].toSelf.eagerly())
