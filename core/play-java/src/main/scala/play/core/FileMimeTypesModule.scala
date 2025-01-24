/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core

import scala.concurrent.Future

import jakarta.inject._
import play.api.inject._
import play.mvc.FileMimeTypes
import play.mvc.StaticFileMimeTypes

/**
 * Module that injects a {@link FileMimeTypes} to {@link StaticFileMimeTypes} on start and on stop.
 *
 * This solves the issue of having the need to explicitly pass {@link FileMimeTypes} to Results.ok(...) and StatusHeader.sendResource(...)
 */
class FileMimeTypesModule
    extends SimpleModule(
      bind[FileMimeTypes].toProvider[FileMimeTypesProvider].eagerly()
    )

@Singleton
class FileMimeTypesProvider @Inject() (lifecycle: ApplicationLifecycle, scalaFileMimeTypes: play.api.http.FileMimeTypes)
    extends Provider[FileMimeTypes] {
  lazy val get: FileMimeTypes = {
    val fileMimeTypes = new FileMimeTypes(scalaFileMimeTypes)
    StaticFileMimeTypes.setFileMimeTypes(fileMimeTypes)
    lifecycle.addStopHook { () => Future.successful(StaticFileMimeTypes.setFileMimeTypes(null)) }
    fileMimeTypes
  }
}
