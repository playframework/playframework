/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

// ###replace: package tasks
package scalaguide.scheduling

import play.api.inject._
import play.api.inject.SimpleModule

class TasksModule extends SimpleModule(bind[MyActorTask].toSelf.eagerly())
