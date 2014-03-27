/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
import sbt._
import Keys._
import sbt.File

object Generators {
  // Generates a scala file that contains the play version for use at runtime.
  def PlayVersion(dir: File): Seq[File] = {
      val file = dir / "PlayVersion.scala"
      IO.write(file,
        """|package play.core
            |
            |object PlayVersion {
            |    val current = "%s"
            |    val scalaVersion = "%s"
            |    val sbtVersion = "%s"
            |}
          """.stripMargin.format(BuildSettings.buildVersion, BuildSettings.buildScalaVersion, BuildSettings.buildSbtVersion))
      Seq(file)
  }
}

object Tasks {

  // ----- Compile templates

  lazy val ScalaTemplates = {
    (classpath: Seq[Attributed[File]], templateEngine: File, sourceDirectory: File, generatedDir: File, streams: TaskStreams) =>
      // Parent classloader must be null to ensure that we get the right scala on the classpath
      val classloader = new java.net.URLClassLoader(classpath.map(_.data.toURI.toURL).toArray, null)
      val compiler = classloader.loadClass("play.templates.ScalaTemplateCompiler")
      val generatedSource = classloader.loadClass("play.templates.GeneratedSource")

      (generatedDir ** "*.template.scala").get.foreach {
        source =>
          val constructor = generatedSource.getDeclaredConstructor(classOf[java.io.File])
          val sync = generatedSource.getDeclaredMethod("sync")
          val generated = constructor.newInstance(source)
          try {
            sync.invoke(generated)
          } catch {
            case e: java.lang.reflect.InvocationTargetException => {
              val t = e.getTargetException
              t.printStackTrace()
              throw t
            }
          }
      }

      (sourceDirectory ** "*.scala.html").get.foreach {
        template =>
          val compile = compiler.getDeclaredMethod("compile", classOf[java.io.File], classOf[java.io.File], classOf[java.io.File], classOf[String], classOf[String])
          try {
            compile.invoke(null, template, sourceDirectory, generatedDir, "play.api.templates.HtmlFormat", "import play.api.templates._\nimport play.api.templates.PlayMagic._")
          } catch {
            case e: java.lang.reflect.InvocationTargetException => {
              streams.log.error("Compilation failed for %s".format(template))
              throw e.getTargetException
            }
          }
      }

      (generatedDir ** "*.scala").get.map(_.getAbsoluteFile)
  }

  def scalaTemplateSourceMappings = (excludeFilter in unmanagedSources, unmanagedSourceDirectories in Compile, baseDirectory) map {
    (excludes, sdirs, base) =>
      val scalaTemplateSources = sdirs.descendantsExcept("*.scala.html", excludes)
      ((scalaTemplateSources --- sdirs --- base) pair (relativeTo(sdirs) | relativeTo(base) | flat)) toSeq
  }

}