/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package com.typesafe.play.docs.sbtplugin

import Imports.PlayDocsKeys.javaManualSourceDirectories
import play.core.enhancers.PropertiesEnhancer
import sbt._
import sbt.compiler.AggressiveCompile
import sbt.inc._
import sbt.Keys._

object Enhancement {

  val enhanceJavaClasses = (javaManualSourceDirectories in Test, dependencyClasspath in Test, compile in Test, classDirectory in Test, compileInputs in compile in Test, streams) map {
    (sourceDirs, deps, analysis, classes, inputs, s) =>
      val classpath = (deps.map(_.data.getAbsolutePath).toArray :+ classes.getAbsolutePath).mkString(java.io.File.pathSeparator)

      val timestampFile = s.cacheDirectory / "play_instrumentation"
      val lastEnhanced = if (timestampFile.exists) IO.read(timestampFile).toLong else Long.MinValue
      val javaClasses = (sourceDirs ** "*.java").get flatMap { sourceFile =>
        // PropertiesEnhancer is class-local, so no need to check outside the class.
        if (analysis.apis.internal(sourceFile).compilation.startTime > lastEnhanced)
          analysis.relations.products(sourceFile)
        else
          Nil
      }

      val javaClassesWithGeneratedAccessors = javaClasses.filter(PropertiesEnhancer.generateAccessors(classpath, _))
      val javaClassesWithAccessorsRewritten = javaClasses.filter(PropertiesEnhancer.rewriteAccess(classpath, _))

      if (javaClassesWithGeneratedAccessors.nonEmpty || javaClassesWithAccessorsRewritten.nonEmpty) {
        /**
         * Updates stamp of product (class file) by preserving the type of a passed stamp.
         * This way any stamp incremental compiler chooses to use to mark class files will
         * be supported.
         */
        def updateStampForClassFile(classFile: File, stamp: Stamp): Stamp = stamp match {
          case _: Exists => Stamp.exists(classFile)
          case _: LastModified => Stamp.lastModified(classFile)
          case _: Hash => Stamp.hash(classFile)
        }
        // Since we may have modified some of the products of the incremental compiler, that is, the compiled template
        // classes and compiled Java sources, we need to update their timestamps in the incremental compiler, otherwise
        // the incremental compiler will see that they've changed since it last compiled them, and recompile them.
        val updatedAnalysis = analysis.copy(stamps = javaClasses.foldLeft(analysis.stamps) { (stamps, classFile) =>
          val existingStamp = stamps.product(classFile)
          if (existingStamp == Stamp.notPresent) {
            throw new java.io.IOException("Tried to update a stamp for class file that is not recorded as "
              + s"product of incremental compiler: $classFile")
          }
          stamps.markProduct(classFile, updateStampForClassFile(classFile, existingStamp))
        })

        // Need to persist the updated analysis.
        val agg = new AggressiveCompile(inputs.incSetup.cacheFile)
        // Load the old one. We do this so that we can get a copy of CompileSetup, which is the cache compiler
        // configuration used to determine when everything should be invalidated. We could calculate it ourselves, but
        // that would by a heck of a lot of fragile code due to the vast number of things we would have to depend on.
        // Reading it out of the existing file is good enough.
        val existing: Option[(Analysis, CompileSetup)] = agg.store.get()
        // Since we've just done a compile before this task, this should never return None, so don't worry about what to
        // do when it returns None.
        existing.foreach {
          case (_, compileSetup) => agg.store.set(updatedAnalysis, compileSetup)
        }

        updatedAnalysis
      } else {
        analysis
      }
  }
}
