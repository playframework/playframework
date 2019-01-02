/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import java.util.concurrent.TimeUnit

import sbt.File
import sbt.inc.Analysis
import sbt.Path._
import scala.language.postfixOps

import scala.concurrent.duration.Duration

/**
 * Fix compatibility issues for PlaySettings. This is the version compatible with sbt 0.13.
 */
private[sbt] trait PlaySettingsCompat {

  def getPoolInterval(poolInterval: Int): Duration = {
    Duration(poolInterval, TimeUnit.MILLISECONDS)
  }

  def getPlayCompileEverything(analysisSeq: Seq[Analysis]): Seq[Analysis] = analysisSeq

  def getPlayAssetsWithCompilation(compileValue: Analysis): Analysis = compileValue

  def getPlayExternalizedResources(rdirs: Seq[File], unmanagedResourcesValue: Seq[File], externalizeResourcesExcludes: Seq[File]): Seq[(File, String)] = {
    (unmanagedResourcesValue --- rdirs --- externalizeResourcesExcludes) pair (relativeTo(rdirs) | flat)
  }
}
