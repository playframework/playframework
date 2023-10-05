/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import sbt.AutoPlugin

import xerial.sbt.Sonatype
import xerial.sbt.Sonatype.autoImport.sonatypeProfileName

/**
 * Base plugin for all projects that publish to sonatype (which is all of them!)
 */
object PlaySonatypeBase extends AutoPlugin {
  override def trigger  = noTrigger
  override def requires = Sonatype

  override def projectSettings = Seq(
    sonatypeProfileName := "org.playframework"
  )
}
