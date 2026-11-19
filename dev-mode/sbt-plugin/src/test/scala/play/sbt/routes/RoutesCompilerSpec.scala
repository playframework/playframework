/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt.routes

import java.io.File

import org.specs2.mutable.*
import play.routes.compiler.Language
import play.routes.compiler.RoutesCompiler.RoutesCompilerTask

class RoutesCompilerSpec extends Specification {
  private def task(lang: Language, namespaceReverseRouter: Boolean) =
    RoutesCompilerTask(new File("routes"), Nil, true, true, true, namespaceReverseRouter, lang)

  "The routes compiler plugin" should {
    "know that Scala 3 before 3.7 can't compile Java routers with namespaced reverse routers" in {
      RoutesCompiler.scalaCantCompileJavaRouters(Seq(task(Language.JAVA, true)), "3.3.9") must beTrue
      RoutesCompiler.scalaCantCompileJavaRouters(Seq(task(Language.JAVA, true)), "3.6.4") must beTrue
    }

    "know that other Scala versions and routers compile" in {
      RoutesCompiler.scalaCantCompileJavaRouters(Seq(task(Language.JAVA, true)), "3.7.0") must beFalse
      RoutesCompiler.scalaCantCompileJavaRouters(Seq(task(Language.JAVA, true)), "3.9.0") must beFalse
      RoutesCompiler.scalaCantCompileJavaRouters(Seq(task(Language.JAVA, true)), "2.13.18") must beFalse
      RoutesCompiler.scalaCantCompileJavaRouters(Seq(task(Language.JAVA, false)), "3.3.9") must beFalse
      RoutesCompiler.scalaCantCompileJavaRouters(Seq(task(Language.SCALA, true)), "3.3.9") must beFalse
    }
  }
}
