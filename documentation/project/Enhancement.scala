/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
import sbt.Keys._
import sbt._
import ApplicationBuild.javaManualSourceDirectories

object Enhancement {

  val enhanceJavaClasses = (javaManualSourceDirectories in Test, dependencyClasspath in Test, compile in Test,
    classDirectory in Test, cacheDirectory in Test) map {
    (sourceDirs, deps, analysis, classes, cacheDir) =>
    val classpath = (deps.map(_.data.getAbsolutePath).toArray :+ classes.getAbsolutePath).mkString(java.io.File.pathSeparator)

    val timestampFile = cacheDir / "play_instrumentation"
    val lastEnhanced = if (timestampFile.exists) IO.read(timestampFile).toLong else Long.MinValue
    val javaClasses = (sourceDirs ** "*.java").get flatMap { sourceFile =>
    // PropertiesEnhancer is class-local, so no need to check outside the class.
      if (analysis.apis.internal(sourceFile).compilation.startTime > lastEnhanced)
        analysis.relations.products(sourceFile)
      else
        Nil
    }

    javaClasses.foreach(play.core.enhancers.PropertiesEnhancer.generateAccessors(classpath, _))
    javaClasses.foreach(play.core.enhancers.PropertiesEnhancer.rewriteAccess(classpath, _))
    analysis
  }

}