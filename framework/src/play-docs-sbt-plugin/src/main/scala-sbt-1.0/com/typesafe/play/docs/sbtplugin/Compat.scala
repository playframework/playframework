/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package sbt.internal {

  import sbt._
  import sbt.internal._
  import sbt.compiler.Eval

  object PlayLoad {

    def defaultLoad(state: State, localBase: java.io.File): (() => Eval, BuildStructure) = {
      Load.defaultLoad(state, localBase, state.log)
    }

  }

  object PlayEvaluateConfigurations {

    def evaluateConfigurations(sbtFile: java.io.File, imports: Seq[String], classLoader: ClassLoader, eval: () => Eval): Seq[Def.Setting[_]] = {
      EvaluateConfigurations.evaluateConfiguration(eval(), sbtFile, imports)(classLoader)
    }

  }
}