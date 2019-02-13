/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core

import play.api.inject._

import javax.inject._
import play.mvc.{ FileMimeTypes, StaticFileMimeTypes }

import scala.concurrent.Future

/**
 * Module that injects a {@link FileMimeTypes} to {@link StaticFileMimeTypes} on start and on stop.
 *
 * This solves the issue of having the need to explicitly pass {@link FileMimeTypes} to Results.ok(...) and StatusHeader.sendResource(...)
 */
class FileMimeTypesModule extends SimpleModule(
  bind[FileMimeTypes].toProvider[FileMimeTypesProvider].eagerly()
)

@Singleton
class FileMimeTypesProvider @Inject() (lifecycle: ApplicationLifecycle, scalaFileMimeTypes: play.api.http.FileMimeTypes) extends Provider[FileMimeTypes] {
  lazy val get: FileMimeTypes = {
    val fileMimeTypes = new FileMimeTypes(scalaFileMimeTypes)
    StaticFileMimeTypes.setFileMimeTypes(fileMimeTypes)
    lifecycle.addStopHook { () =>
      Future.successful(StaticFileMimeTypes.setFileMimeTypes(null))
    }
    fileMimeTypes
  }
}
