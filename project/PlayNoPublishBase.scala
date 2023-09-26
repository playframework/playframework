/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import sbt.*
import sbt.Keys.*

import com.jsuereth.sbtpgp.PgpKeys

object PlayNoPublishBase extends AutoPlugin {
  override def trigger  = noTrigger
  override def requires = PlayBuildBase

  override def projectSettings = Seq(
    PgpKeys.publishSigned := {},
    publish               := {},
    publishLocal          := {},
    publishTo             := Some(Resolver.file("no-publish", crossTarget.value / "no-publish"))
  )
}
