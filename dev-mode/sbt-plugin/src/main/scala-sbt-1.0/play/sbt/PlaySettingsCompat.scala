/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import sbt.Path._
import sbt.io.syntax._
import sbt.{ File, Task }
import scala.language.postfixOps

import scala.concurrent.duration.Duration

/**
 * Fix compatibility issues for PlaySettings. This is the version compatible with sbt 1.0.
 */
private[sbt] trait PlaySettingsCompat {

  def getPoolInterval(poolInterval: Duration): Duration = poolInterval

  def getPlayCompileEverything(analysisSeq: Seq[xsbti.compile.CompileAnalysis]): Seq[sbt.internal.inc.Analysis] = {
    analysisSeq.map(_.asInstanceOf[sbt.internal.inc.Analysis])
  }

  def getPlayAssetsWithCompilation(compileValue: xsbti.compile.CompileAnalysis): sbt.internal.inc.Analysis = {
    compileValue.asInstanceOf[sbt.internal.inc.Analysis]
  }

  def getPlayExternalizedResources(rdirs: Seq[File], unmanagedResourcesValue: Seq[File], externalizeResourcesExcludes: Seq[File]): Seq[(File, String)] = {
    (unmanagedResourcesValue --- rdirs --- externalizeResourcesExcludes) pair (relativeTo(rdirs) | flat)
  }

}
