package sbt

import Keys._
import CommandSupport.{ClearOnFailure,FailureWall}

import play.api._
import play.core._

import play.utils.Colors

import PlayExceptions._
import PlayKeys._

import scala.annotation.tailrec

trait PlayCommands {
  this: PlayReloader =>

  private[sbt] lazy val testListener = new PlayTestListener

  // ----- We need this later

  private val consoleReader = new jline.ConsoleReader

  private def waitForKey() = {
    consoleReader.getTerminal.disableEcho()
    def waitEOF() {
      consoleReader.readVirtualKey() match {
        case 4 => // STOP
        case 11 => consoleReader.clearScreen(); waitEOF()
        case 10 => println(); waitEOF()
        case _ => waitEOF()
      }

    }
    waitEOF()
    consoleReader.getTerminal.enableEcho()
  }

  // -- Utility methods for 0.10-> 0.11 migration
  def inAllDeps[T](base: ProjectRef, deps: ProjectRef => Seq[ProjectRef], key: SettingKey[T], data: Settings[Scope]): Seq[T] =
    inAllProjects(Dag.topologicalSort(base)(deps), key, data)
  def inAllProjects[T](allProjects: Seq[Reference], key: SettingKey[T], data: Settings[Scope]): Seq[T] =
    allProjects.flatMap { p => key in p get data }

  def inAllDependencies[T](base: ProjectRef, key: SettingKey[T], structure: Load.BuildStructure): Seq[T] = {
    def deps(ref: ProjectRef): Seq[ProjectRef] =
      Project.getProject(ref, structure).toList.flatMap { p =>
        p.dependencies.map(_.project) ++ p.aggregate
      }
    inAllDeps(base, deps, key, structure.data)
  }

  // ----- Play specific tasks

  private[this] var commonClassLoader: ClassLoader = _

  val playCommonClassloader = TaskKey[ClassLoader]("play-common-classloader")
  val playCommonClassloaderTask = (scalaInstance, dependencyClasspath in Compile) map { (si, classpath) =>

    lazy val commonJars: PartialFunction[java.io.File, java.net.URL] = {
      case jar if jar.getName.startsWith("h2-") => jar.toURI.toURL
    }

    if (commonClassLoader == null) {
      commonClassLoader = new java.net.URLClassLoader(classpath.map(_.data).collect(commonJars).toArray, si.loader)
    }

    commonClassLoader
  }

  val playCompileEverything = TaskKey[Seq[sbt.inc.Analysis]]("play-compile-everything")
  val playCompileEverythingTask = (state, thisProjectRef) flatMap { (s, r) =>
    inAllDependencies(r, (compile in Compile).task, Project structure s).join
  }

  val playPackageEverything = TaskKey[Seq[File]]("play-package-everything")
  val playPackageEverythingTask = (state, thisProjectRef) flatMap { (s, r) =>
    inAllDependencies(r, (packageBin in Compile).task, Project structure s).join
  }

  val playCopyResources = TaskKey[Seq[(File, File)]]("play-copy-resources")
  val playCopyResourcesTask = (baseDirectory, managedResources in Compile, resourceManaged in Compile, playResourceDirectories, classDirectory in Compile, cacheDirectory, streams) map { (b, resources, resourcesDirectories, r, t, c, s) =>
    val cacheFile = c / "copy-resources"
    val mappings = (r.map(_ ***).reduceLeft(_ +++ _) x rebase(b, t)) ++ (resources x rebase(resourcesDirectories, t))
    s.log.debug("Copy play resource mappings: " + mappings.mkString("\n\t", "\n\t", ""))
    Sync(cacheFile)(mappings)
    mappings
  }

  val playReload = TaskKey[sbt.inc.Analysis]("play-reload")
  val playReloadTask = (playCopyResources, playCompileEverything) map { (_, analysises) =>
    analysises.reduceLeft(_ ++ _)
  }

  val dist = TaskKey[File]("dist", "Build the standalone application package")
  val distTask = (baseDirectory, playPackageEverything, dependencyClasspath in Runtime, target, normalizedName, version) map { (root, packaged, dependencies, target, id, version) =>

    import sbt.NameFilter._

    val dist = root / "dist"
    val packageName = id + "-" + version
    val zip = dist / (packageName + ".zip")

    IO.delete(dist)
    IO.createDirectory(dist)

    val libs = {
      dependencies.filter(_.data.ext == "jar").map { dependency =>
        dependency.data -> (packageName + "/lib/" + (dependency.metadata.get(AttributeKey[ModuleID]("module")).map { module =>
          module.organization + "." + module.name + "-" + module.revision + ".jar"
        }.getOrElse(dependency.data.getName)))
      } ++ packaged.map(jar => jar -> (packageName + "/lib/" + jar.getName))
    }

    val start = target / "start"
    IO.write(start,
      """java "$@" -cp "`dirname $0`/lib/*" play.core.server.NettyServer `dirname $0`""" /* */ )
    val scripts = Seq(start -> (packageName + "/start"))

    val conf = Seq((root / "conf" / "application.conf") -> (packageName + "/conf/application.conf"))

    IO.zip(libs ++ scripts ++ conf, zip)
    IO.delete(start)

    println()
    println("Your application is ready in " + zip.getCanonicalPath)
    println()

    zip
  }

  val playIntellij = TaskKey[Unit]("idea")
  val playIntellijTask = (javaSource in Compile, javaSource in Test, dependencyClasspath in Test, baseDirectory, dependencyClasspath in Runtime, normalizedName, version, scalaVersion, streams) map { (javaSource, jTestSource, testDeps, root, dependencies, id, version, scalaVersion, s) =>

    val mainClasses = "file://$MODULE_DIR$/target/scala-" + scalaVersion + "/classes"

    val sl = java.io.File.separator

    val build = IO.read(new File(root + sl + "project" + sl + "Build.scala"))

    val compVersion = "scala-compiler-" + scalaVersion

    lazy val facet =
      <component name="FacetManager">
        <facet type="scala" name="Scala">
          <configuration>
            <option name="compilerLibraryLevel" value="Global"/>
            <option name="compilerLibraryName" value={ compVersion }/>
          </configuration>
        </facet>
      </component>

    def sourceRef(s: String, defaultMain: String = "main/src/"): List[String] = {
      val folder = s.substring(s.lastIndexOf(sl) + 1)
      //maven layout?
      if (folder == "java")
        List("file://$MODULE_DIR$/" + defaultMain + "/java" + "file://$MODULE_DIR$/" + defaultMain + "/scala")
      else
        List("file://$MODULE_DIR$/" + folder)
    }

    def sourceEntry(name: String, test: String = "false") = <sourceFolder url={ name } isTestSource={ test }/>

    def entry(j: String, scope: String = "COMPILE") =
      <orderEntry type="module-library" scope={ scope }>
        <library>
          <CLASSES>
            <root url={ j }/>
          </CLASSES>
          <JAVADOC/>
          <SOURCES/>
        </library>
      </orderEntry>

    //generate project file  
    val scalaFacet = if (build.contains("mainLang") && build.contains("SCALA")) Some(facet.toString) else None

    val mainLang = scalaFacet.map(_ => "SCALA").getOrElse("JAVA")

    val genClasses = "file://$MODULE_DIR$/target/scala-" + scalaVersion + "/classes_managed"

    val testJars = testDeps.flatMap {
      case (dep) if dep.data.ext == "jar" =>
        val ref = "jar://" + dep.data + "!/"
        entry(ref, "TEST")
      case _ => None
    }.mkString("\n")

    //calculate sources, capture both play and standard maven layout in case of multi project setups
    val sources = (sourceRef(javaSource.getCanonicalPath).map(dir => sourceEntry(dir)) ++ sourceRef(jTestSource.getCanonicalPath, "main/test").map(dir => sourceEntry(dir, test = "true"))).mkString("\n")

    //calculate dependencies
    val jars = dependencies.flatMap {
      case (dep) if dep.data.ext == "jar" =>
        val ref = "jar://" + dep.data + "!/"
        entry(ref)
      case _ => None
    }.mkString("\n") + testJars + entry(genClasses).toString + mainClasses

    val target = new File(root + sl + id + ".iml")
    s.log.warn(play.console.Console.logo)
    s.log.info("...about to generate an Intellij project module(" + mainLang + ") called " + target.getName)
    if (target.exists) s.log.warn(target.toString + " will be overwritten")
    IO.delete(target)

    IO.copyFile(new File(System.getProperty("play.home") + sl + "skeletons" + sl + "intellij-skel" + sl + "template.iml"), target)

    play.console.Console.replace(target, "SCALA_FACET" -> scalaFacet.getOrElse(""))
    play.console.Console.replace(target, "SCALA_VERSION" -> scalaVersion)
    play.console.Console.replace(target, "JARS" -> jars)
    play.console.Console.replace(target, "SOURCE" -> sources)
    s.log.warn(target.getName + " was generated")
    s.log.warn("Have fun!")
  }

  val playStage = TaskKey[Unit]("stage")
  val playStageTask = (baseDirectory, playPackageEverything, dependencyClasspath in Runtime, target, streams) map { (root, packaged, dependencies, target, s) =>

    import sbt.NameFilter._

    val staged = target / "staged"

    IO.delete(staged)
    IO.createDirectory(staged)

    val libs = dependencies.filter(_.data.ext == "jar").map(_.data) ++ packaged

    libs.foreach { jar =>
      IO.copyFile(jar, new File(staged, jar.getName))
    }

    val start = target / "start"
    IO.write(start,
      """|#! /usr/bin/env sh
         |
         |java "$@" -cp "`dirname $0`/staged/*" play.core.server.NettyServer `dirname $0`/..
         |""".stripMargin)

    "chmod a+x %s".format(start.getAbsolutePath) !

    s.log.info("")
    s.log.info("Your application is ready to be run in place: target/start")
    s.log.info("")

    ()
  }

  // ----- Assets

  def AssetsCompiler(name: String, files: (File) => PathFinder, naming: (String) => String, compile: (File, Boolean) => (String, Seq[File])) =
    (sourceDirectory in Compile, resourceManaged in Compile, cacheDirectory, minify) map { (src, resources, cache, min) =>

      import java.io._

      val cacheFile = cache / name
      val sourceFiles = files(src / "assets")
      val currentInfos = sourceFiles.get.map(f => f -> FileInfo.lastModified(f)).toMap
      val (previousRelation, previousInfo) = Sync.readInfo(cacheFile)(FileInfo.lastModified.format)

      if (previousInfo != currentInfos) {

        // Delete previous generated files
        previousRelation._2s.foreach(IO.delete)

        val generated = ((sourceFiles --- ((src / "assets") ** "_*")) x relativeTo(Seq(src / "assets"))).map {
          case (sourceFile, name) => sourceFile -> ("public/" + naming(name))
        }.flatMap {
          case (sourceFile, name) => {
            val ((css, dependencies), out) = compile(sourceFile, min) -> new File(resources, name)
            IO.write(out, css)
            dependencies.map(_ -> out)
          }
        }

        Sync.writeInfo(cacheFile,
          Relation.empty[File, File] ++ generated,
          currentInfos)(FileInfo.lastModified.format)

        // Return new files
        generated.toMap.values.toSeq

      } else {

        // Return previously generated files
        previousRelation._2s.toSeq

      }

    }

  val LessCompiler = AssetsCompiler("less",
    { assets => (assets ** "*.less") },
    { name => name.replace(".less", ".css") },
    { (lessFile, minify) => play.core.less.LessCompiler.compile(lessFile, minify) })

  val JavascriptCompiler = AssetsCompiler("javascripts",
    { assets => (assets ** "*.js") },
    identity,
    { (jsFile, minify) =>
      val (fullSource, minified, dependencies) = play.core.jscompile.JavascriptCompiler.compile(jsFile)
      (if (minify) minified else fullSource, dependencies)
    })

  val CoffeescriptCompiler = AssetsCompiler("coffeescript",
    { assets => (assets ** "*.coffee") },
    { name => name.replace(".coffee", ".js") },
    { (coffeeFile, minify) => (play.core.coffeescript.CoffeescriptCompiler.compile(coffeeFile), Seq(coffeeFile)) })

  // ----- Post compile (need to be refactored and fully configurable)

  val PostCompile = (sourceDirectory in Compile, dependencyClasspath in Compile, compile in Compile, javaSource in Compile, sourceManaged in Compile, classDirectory in Compile) map { (src, deps, analysis, javaSrc, srcManaged, classes) =>

    // Properties

    val classpath = (deps.map(_.data.getAbsolutePath).toArray :+ classes.getAbsolutePath).mkString(java.io.File.pathSeparator)

    val javaClasses = (javaSrc ** "*.java").get.map { sourceFile =>
      analysis.relations.products(sourceFile)
    }.flatten.distinct

    javaClasses.foreach(play.core.enhancers.PropertiesEnhancer.generateAccessors(classpath, _))
    javaClasses.foreach(play.core.enhancers.PropertiesEnhancer.rewriteAccess(classpath, _))

    // EBean

    try {

      val cp = deps.map(_.data.toURI.toURL).toArray :+ classes.toURI.toURL

      import com.avaje.ebean.enhance.agent._
      import com.avaje.ebean.enhance.ant._

      val cl = ClassLoader.getSystemClassLoader

      val t = new Transformer(cp, "debug=-1")

      val ft = new OfflineFileTransform(t, cl, classes.getAbsolutePath, classes.getAbsolutePath)
      ft.process("models/**")

    } catch {
      case _ =>
    }

    // Copy managed classes

    val managedClassesDirectory = classes.getParentFile / (classes.getName + "_managed")

    val managedClasses = (srcManaged ** "*.scala").get.map { managedSourceFile =>
      analysis.relations.products(managedSourceFile)
    }.flatten x rebase(classes, managedClassesDirectory)

    // Copy modified class files
    val managedSet = IO.copy(managedClasses)

    // Remove deleted class files
    (managedClassesDirectory ** "*.class").get.filterNot(managedSet.contains(_)).foreach(_.delete())

    analysis
  }

  // ----- Source generators

  val RouteFiles = (confDirectory: File, generatedDir: File) => {
    import play.core.Router.RoutesCompiler._

    ((generatedDir ** "routes.java").get ++ (generatedDir ** "routes_*.scala").get).map(GeneratedSource(_)).foreach(_.sync())
    try {
      (confDirectory * "routes").get.foreach { routesFile =>
        compile(routesFile, generatedDir)
      }
    } catch {
      case RoutesCompilationError(source, message, line, column) => {
        throw RoutesCompilationException(source, message, line, column.map(_ - 1))
      }
      case e => throw e
    }

    ((generatedDir ** "routes_*.scala").get ++ (generatedDir ** "routes.java").get).map(_.getAbsoluteFile)

  }

  val ScalaTemplates = (sourceDirectory: File, generatedDir: File, templateTypes: PartialFunction[String, (String, String)], additionalImports: Seq[String]) => {
    import play.templates._

    val templateExt: PartialFunction[File, (File, String, String, String)] = {
      case p if templateTypes.isDefinedAt(p.name.split('.').last) =>
        val extension = p.name.split('.').last
        val exts = templateTypes(extension)
        (p, extension, exts._1, exts._2)
    }
    (generatedDir ** "*.template.scala").get.map(GeneratedSource(_)).foreach(_.sync())
    try {

      (sourceDirectory ** "*.scala.*").get.collect(templateExt).foreach {
        case (template, extension, t, format) =>
          ScalaTemplateCompiler.compile(
            template,
            sourceDirectory,
            generatedDir,
            t,
            format,
            additionalImports.map("import " + _.replace("%format%", extension)).mkString("\n"))
      }
    } catch {
      case TemplateCompilationError(source, message, line, column) => {
        throw TemplateCompilationException(source, message, line, column - 1)
      }
      case e => throw e
    }

    (generatedDir ** "*.template.scala").get.map(_.getAbsoluteFile)
  }

  // ----- Play prompt

  val playPrompt = { state: State =>

    val extracted = Project.extract(state)
    import extracted._

    (name in currentRef get structure.data).map { name =>
      "[" + Colors.cyan(name) + "] $ "
    }.getOrElse("> ")

  }

  // ----- Play commands

  val playRunCommand = playRunCommandBase("run", "(Server started, use Ctrl+D to stop and go back to the console...)")

  def playRunCommandBase(commandName: String, message: String) = Command.args(commandName, "<port>") { (state: State, args: Seq[String]) =>

    // Parse HTTP port argument
    val port = args.headOption.map { portString =>
      try {
        Integer.parseInt(portString)
      } catch {
        case e => sys.error("Invalid port argument: " + portString)
      }
    }.getOrElse(9000)

    println()

    val sbtLoader = this.getClass.getClassLoader
    val commonLoader = Project.evaluateTask(playCommonClassloader, state).get.toEither.right.get

    Project.evaluateTask(dependencyClasspath in Compile, state).get.toEither.right.map { dependencies =>

      val classpath = dependencies.map(_.data.toURI.toURL).toArray

      /**
       * Create a temporary classloader to run the application.
       * This classloader share the minimal set of interface needed for
       * communication between SBT and Play.
       * It also uses the same Scala classLoader as SBT allowing to share any
       * values coming from the Scala library between both.
       */
      lazy val applicationLoader: ClassLoader = new java.net.URLClassLoader(classpath, commonLoader) {

        val sharedClasses = Seq(
          classOf[play.core.SBTLink].getName,
          classOf[play.core.server.ServerWithStop].getName,
          classOf[play.api.PlayException.UsefulException].getName,
          classOf[play.api.PlayException.ExceptionSource].getName,
          classOf[play.api.PlayException.ExceptionAttachment].getName)

        override def loadClass(name: String): Class[_] = {
          if (sharedClasses.contains(name)) {
            sbtLoader.loadClass(name)
          } else {
            try {
              super.loadClass(name)
            } catch {
              case e: ClassNotFoundException => {
                reloader.currentApplicationClassLoader.map(_.loadClass(name)).getOrElse(throw e)
              }
            }
          }
        }

        override def toString = {
          "SBT/Play shared ClassLoader, with: " + (getURLs.toSeq) + ", using parent: " + (getParent)
        }

      }

      lazy val reloader = newReloader(state, playReload, applicationLoader)

      val mainClass = applicationLoader.loadClass(classOf[play.core.server.NettyServer].getName)
      val mainDev = mainClass.getMethod("mainDev", classOf[SBTLink], classOf[Int])

      // Run in DEV
      val server = mainDev.invoke(null, reloader, port: java.lang.Integer).asInstanceOf[play.core.server.ServerWithStop]

      println()
      println(Colors.green(message))
      println()

      val ContinuousState = AttributeKey[WatchState]("watch state", "Internal: tracks state for continuous execution.")
      def isEOF(c: Int): Boolean = c == 4

      def executeContinuously(watched: Watched, s: State, reloader: SBTLink, ws:
      Option[WatchState] = None): Option[String] = {
        @tailrec def shouldTerminate: Boolean = (System.in.available > 0) && (isEOF(System.in.read()) || shouldTerminate)

        val sourcesFinder = PathFinder { watched watchPaths s }
        val watchState = ws.getOrElse(s get ContinuousState getOrElse WatchState.empty)

        val (triggered, newWatchState, newState) =
          try {
            val (triggered, newWatchState) = SourceModificationWatch.watch(sourcesFinder, watched.pollInterval, watchState)(shouldTerminate)
            (triggered, newWatchState, s)
          }
          catch { case e: Exception =>
            val log = s.log
            log.error("Error occurred obtaining files to watch.  Terminating continuous execution...")
            BuiltinCommands.handleException(e, s, log)
            (false, watchState, s.fail)
          }


        if(triggered) {
          PlayProject.synchronized{
            Project.evaluateTask(compile in Compile, newState)
          }
          Thread.sleep(Watched.PollDelayMillis)
          executeContinuously(watched, newState, reloader, Some(newWatchState))
        }
        else {
          Some("Okay, i'm done")
        }
      }

      val maybeContinuous = state.get(Watched.Configuration).map{ w =>
        state.get(Watched.ContinuousState).map { ws => 
          (ws.count == 1, w, ws)
        }.getOrElse((false, None, None))
      }.getOrElse((false, None, None))

      maybeContinuous match {
        case (true, w:sbt.Watched, ws) => executeContinuously(w, state, reloader)
        case _ => waitForKey()
      }

      server.stop()

    }

    println()

    state.copy(remainingCommands = Seq("shell")).remove(Watched.ContinuousState)
  }



  val playStartCommand = Command.args("start", "<port>") { (state: State, args: Seq[String]) =>

    // Parse HTTP port argument
    val port = args.headOption.map { portString =>
      try {
        Integer.parseInt(portString)
      } catch {
        case e => sys.error("Invalid port argument: " + portString)
      }
    }.getOrElse(9000)

    val extracted = Project.extract(state)

    Project.evaluateTask(compile in Compile, state).get.toEither match {
      case Left(_) => {
        println()
        println("Cannot start with errors.")
        println()
        state.fail
      }
      case Right(_) => {

        Project.evaluateTask(dependencyClasspath in Runtime, state).get.toEither.right.map { dependencies =>

          val classpath = dependencies.map(_.data).map(_.getCanonicalPath).reduceLeft(_ + java.io.File.pathSeparator + _)

          import java.lang.{ ProcessBuilder => JProcessBuilder }
          val builder = new JProcessBuilder(Array(
            "java", "-Dhttp.port=" + port, "-cp", classpath, "play.core.server.NettyServer", extracted.currentProject.base.getCanonicalPath): _*)

          new Thread {
            override def run {
              System.exit(Process(builder) !)
            }
          }.start()

          println(Colors.green(
            """|
               |(Starting server. Type Ctrl+D to exit logs, the server will remain in background)
               |""".stripMargin))

          waitForKey()

          println()

          state.copy(remainingCommands = Seq.empty)

        }.right.getOrElse {
          println()
          println("Oops, cannot start the server?")
          println()
          state.fail
        }

      }
    }

  }

  val playHelpCommand = Command.command("help") { state: State =>

    println(
      """
                |Welcome to Play 2.0!
                |
                |These commands are available:
                |-----------------------------
                |classpath                  Display the project classpath.
                |clean                      Clean all generated files.
                |compile                    Compile the current application.
                |console                    Launch the interactive Scala console (use :quit to exit).
                |dependencies               Display the dependencies summary.
                |dist                       Construct standalone application package.
                |exit                       Exit the console.
                |h2-browser                 Launch the H2 Web browser.
                |license                    Display licensing informations.
                |package                    Package your application as a JAR.
                |publish                    Publish your application in a remote repository.
                |publish-local              Publish your application in the local repository.
                |reload                     Reload the current application build file.
                |run <port>                 Run the current application in DEV mode.
                |test                       Run Junit tests and/or Specs from the command line
                |idea                       generate intellij IDEA project file
                |eclipsify                  generate eclipse project file
                |start <port>               Start the current application in another JVM in PROD mode.
                |update                     Update application dependencies.
                |
                |You can also use any sbt feature:
                |---------------------------------
                |about                      Displays basic information about sbt and the build.
                |reboot [full]              Reboots sbt and then executes the remaining commands.
                |< file*                    Reads command lines from the provided files.
                |!!                         Execute the last command again
                |!:                         Show all previous commands
                |!:n                        Show the last n commands
                |!n                         Execute the command with index n, as shown by the !: command
                |!-n                        Execute the nth command before this one
                |!string                    Execute the most recent command starting with 'string'
                |!?string                   Execute the most recent command containing 'string'
                |~ <action>                 Executes the specified command whenever source files change.
                |projects                   Displays the names of available projects.
                |project [project]          Displays the current project or changes to the provided `project`.
                |- command                  Registers 'command' to run if a command fails.
                |iflast command             If there are no more commands after this one, 'command' is run.
                |( ; command )+             Runs the provided semicolon-separated commands.
                |set <setting-expression>   Evaluates the given Setting and applies to the current project.
                |tasks                      Displays the tasks defined for the current project.
                |inspect <key>              Prints the value for 'key', the defining scope, delegates, related definitions, and dependencies.
                |eval <expression>          Evaluates the given Scala expression and prints the result and type.
                |alias                      Adds, removes, or prints command aliases.
                |append command             Appends `command` to list of commands to run.
                |last <key>                 Prints the last output associated with 'key'.
                |last-grep <pattern> <key>  Shows lines from the last output for 'key' that match 'pattern'.
                |session ...                Manipulates session settings.  For details, run 'help session'..
                |
                |Browse the complete documentation at http://www.playframework.org.
                |""".stripMargin)

    state
  }

  val playCommand = Command.command("play") { state: State =>

    val extracted = Project.extract(state)
    import extracted._

    // Display logo
    println(play.console.Console.logo)
    println("""
            |> Type "help" or "license" for more information.
            |> Type "exit" or use Ctrl+D to leave this console.
            |""".stripMargin)

    state.copy(
      remainingCommands = state.remainingCommands :+ "shell")

  }

  val h2Command = Command.command("h2-browser") { state: State =>
    try {
      val commonLoader = Project.evaluateTask(playCommonClassloader, state).get.toEither.right.get
      val h2ServerClass = commonLoader.loadClass(classOf[org.h2.tools.Server].getName)
      h2ServerClass.getMethod("main", classOf[Array[String]]).invoke(null, Array.empty[String])
    } catch {
      case e => e.printStackTrace
    }
    state
  }

  val licenseCommand = Command.command("license") { state: State =>
    println(
      """
      |This software is licensed under the Apache 2 license, quoted below.
      |
      |Copyright 2011 Zenexity <http://www.zenexity.com>
      |
      |Licensed under the Apache License, Version 2.0 (the "License"); you may not
      |use this file except in compliance with the License. You may obtain a copy of
      |the License at
      |
      |    http://www.apache.org/licenses/LICENSE-2.0
      |
      |Unless required by applicable law or agreed to in writing, software
      |distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
      |WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
      |License for the specific language governing permissions and limitations under
      |the License.
      """.stripMargin)
    state
  }

  val classpathCommand = Command.command("classpath") { state: State =>

    val extracted = Project.extract(state)

    Project.evaluateTask(dependencyClasspath in Runtime, state).get.toEither match {
      case Left(_) => {
        println()
        println("Cannot compute the classpath")
        println()
        state.fail
      }
      case Right(classpath) => {
        println()
        println("Here is the computed classpath of your application:")
        println()
        classpath.foreach { item =>
          println("\t- " + item.data.getAbsolutePath)
        }
        println()
        state
      }
    }

  }

  // -- Dependencies
  val testResultReporter = TaskKey[List[String]]("test-result-reporter")
  val testResultReporterTask = (state, thisProjectRef) map { (s, r) =>
    testListener.result.toList
  }
  // -- Dependencies
  val testResultReporterReset = TaskKey[Unit]("test-result-reporter-reset")
  val testResultReporterResetTask = (state, thisProjectRef) map { (s, r) =>
    testListener.result.clear
  }

  val computeDependencies = TaskKey[Seq[Map[Symbol, Any]]]("ivy-dependencies")
  val computeDependenciesTask = (deliverLocal, ivySbt, streams, organizationName, moduleName, version, scalaVersion) map { (_, ivySbt, s, org, id, version, scalaVersion) =>

    import scala.xml._

    ivySbt.withIvy(s.log) { ivy =>
      val report = XML.loadFile(
        ivy.getResolutionCacheManager.getConfigurationResolveReportInCache(org + "-" + id + "_" + scalaVersion, "runtime"))

      val deps: Seq[Map[Symbol, Any]] = (report \ "dependencies" \ "module").flatMap { module =>

        (module \ "revision").map { rev =>
          Map(
            'module -> (module \ "@organisation" text, module \ "@name" text, rev \ "@name"),
            'evictedBy -> (rev \ "evicted-by").headOption.map(_ \ "@rev" text),
            'requiredBy -> (rev \ "caller").map { caller =>
              (caller \ "@organisation" text, caller \ "@name" text, caller \ "@callerrev" text)
            },
            'artifacts -> (rev \ "artifacts" \ "artifact").flatMap { artifact =>
              (artifact \ "@location").headOption.map(node => new java.io.File(node.text).getName)
            })
        }

      }

      deps

    }

  }

  val computeDependenciesCommand = Command.command("dependencies") { state: State =>

    val extracted = Project.extract(state)

    Project.evaluateTask(computeDependencies, state).get.toEither match {
      case Left(_) => {
        println()
        println("Cannot compute dependencies")
        println()
        state.fail
      }
      case Right(dependencies) => {
        println()
        println("Here are the resolved dependencies of your application:")
        println()

        import scala.Console._

        def asTableRow(module: Map[Symbol, Any]): Seq[(String, String, String, Boolean)] = {
          val formatted = (Seq(module.get('module).map {
            case (org, name, rev) => org + ":" + name + ":" + rev
          }).flatten,

            module.get('requiredBy).map {
              case callers @ Seq(_*) => callers.map {
                case (org, name, rev) => org + ":" + name + ":" + rev
              }
            }.flatten.toSeq,

            module.get('evictedBy).map {
              case Some(rev) => Seq("Evicted by " + rev)
              case None => module.get('artifacts).map {
                case artifacts: Seq[String] => artifacts.map("As " + _)
              }.flatten
            }.flatten.toSeq)
          val maxLines = Seq(formatted._1.size, formatted._2.size, formatted._3.size).max

          formatted._1.padTo(maxLines, "").zip(
            formatted._2.padTo(maxLines, "")).zip(
              formatted._3.padTo(maxLines, "")).map {
                case ((name, callers), notes) => (name, callers, notes, module.get('evictedBy).map { case Some(_) => true; case _ => false }.get)
              }
        }

        def display(modules: Seq[Seq[(String, String, String, Boolean)]]) {
          val c1Size = modules.flatten.map(_._1.size).max
          val c2Size = modules.flatten.map(_._2.size).max
          val c3Size = modules.flatten.map(_._3.size).max

          def bar(length: Int) = (1 to length).map(_ => "-").mkString

          val lineFormat = "| %-" + (c1Size + 9) + "s | %-" + (c2Size + 9) + "s | %-" + (c3Size + 9) + "s |"
          val separator = "+-%s-+-%s-+-%s-+".format(
            bar(c1Size), bar(c2Size), bar(c3Size))

          println(separator)
          println(lineFormat.format(CYAN + "Module" + RESET, CYAN + "Required by" + RESET, CYAN + "Note" + RESET))
          println(separator)

          modules.foreach { lines =>
            lines.foreach {
              case (module, caller, note, evicted) => {
                println(lineFormat.format(
                  if (evicted) (RED + module + RESET) else (GREEN + module + RESET),
                  (WHITE + caller + RESET),
                  if (evicted) (RED + note + RESET) else (WHITE + note + RESET)))
              }
            }
            println(separator)
          }
        }

        display(dependencies.map(asTableRow))

        println()
        state
      }
    }

  }

}
