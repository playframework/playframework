/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.sbt

import sbt._
import sbt.Keys._

import play.sbt.PlayImport.PlayKeys._
import play.sbt.PlayInternalKeys._

import com.typesafe.sbt.web.SbtWeb.autoImport._

object PlayCommands {

  val playReloadTask = Def.task(playCompileEverything.value.reduceLeft(_ ++ _))

  // ----- Play prompt

  val playPrompt = { state: State =>

    val extracted = Project.extract(state)
    import extracted._

    (name in currentRef get structure.data).map { name =>
      "[" + Colors.cyan(name) + "] $ "
    }.getOrElse("> ")

  }

  // ----- Play commands

  // -- Utility methods for 0.10-> 0.11 migration
  def inAllDeps[T](base: ProjectRef, deps: ProjectRef => Seq[ProjectRef], key: SettingKey[T], data: Settings[Scope]): Seq[T] =
    inAllProjects(Dag.topologicalSort(base)(deps), key, data)
  def inAllProjects[T](allProjects: Seq[Reference], key: SettingKey[T], data: Settings[Scope]): Seq[T] =
    allProjects.flatMap { p => key in p get data }

  def inAllDependencies[T](base: ProjectRef, key: SettingKey[T], structure: BuildStructure): Seq[T] = {
    def deps(ref: ProjectRef): Seq[ProjectRef] =
      Project.getProject(ref, structure).toList.flatMap { p =>
        p.dependencies.map(_.project) ++ p.aggregate
      }
    inAllDeps(base, deps, key, structure.data)
  }

  def taskToSetting[T](task: TaskKey[T]): SettingKey[Task[T]] = Scoped.scopedSetting(task.scope, task.key)

  private[this] var commonClassLoader: ClassLoader = _

  val playCommonClassloaderTask = (dependencyClasspath in Compile, streams) map { (classpath, streams) =>
    lazy val commonJars: PartialFunction[java.io.File, java.net.URL] = {
      case jar if jar.getName.startsWith("h2-") || jar.getName == "h2.jar" => jar.toURI.toURL
    }

    if (commonClassLoader == null) {

      // The parent of the system classloader *should* be the extension classloader:
      // http://www.onjava.com/pub/a/onjava/2005/01/26/classloading.html
      // We use this because this is where things like Nashorn are located. We don't use the system classloader
      // because it will be polluted with the sbt launcher and dependencies of the sbt launcher.
      // See https://github.com/playframework/playframework/issues/3420 for discussion.
      val parent = ClassLoader.getSystemClassLoader.getParent
      streams.log.debug("Using parent loader for play common classloader: " + parent)

      commonClassLoader = new java.net.URLClassLoader(classpath.map(_.data).collect(commonJars).toArray, parent) {
        override def toString = "Common ClassLoader: " + getURLs.map(_.toString).mkString(",")
      }
    }

    commonClassLoader
  }

  val playCompileEverythingTask = (state, thisProjectRef) flatMap { (s, r) =>
    inAllDependencies(r, taskToSetting(playAssetsWithCompilation), Project structure s).join
  }

  val h2Command = Command.command("h2-browser") { state: State =>
    try {
      val commonLoader = Project.runTask(playCommonClassloader, state).get._2.toEither.right.get
      val h2ServerClass = commonLoader.loadClass(classOf[org.h2.tools.Server].getName)
      h2ServerClass.getMethod("main", classOf[Array[String]]).invoke(null, Array.empty[String])
    } catch {
      case e: Exception => e.printStackTrace
    }
    state
  }

  val playMonitoredFilesTask = (thisProjectRef, state) map { (ref, state) =>
    val src = inAllDependencies(ref, unmanagedSourceDirectories in Compile, Project structure state).foldLeft(Seq.empty[File])(_ ++ _)
    val resources = inAllDependencies(ref, unmanagedResourceDirectories in Compile, Project structure state).foldLeft(Seq.empty[File])(_ ++ _)
    val assets = inAllDependencies(ref, unmanagedSourceDirectories in Assets, Project structure state).foldLeft(Seq.empty[File])(_ ++ _)
    val public = inAllDependencies(ref, unmanagedResourceDirectories in Assets, Project structure state).foldLeft(Seq.empty[File])(_ ++ _)
    (src ++ resources ++ assets ++ public).map { f =>
      if (!f.exists) f.mkdirs(); f
    }.map(_.getCanonicalPath).distinct
  }

}
