/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

//###replace: package tasks
package scalaguide.scheduling

import play.api.inject.{SimpleModule, _}

class TasksModule extends SimpleModule(bind[MyActorTask].toSelf.eagerly())
