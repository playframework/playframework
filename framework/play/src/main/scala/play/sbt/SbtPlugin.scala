package sbt

import Keys._
import jline._

import play.api._
import play.core._

import play.console.Colors

object PlayProject extends Plugin {

  // ----- We need this later

  private val consoleReader = new jline.ConsoleReader

  private def waitForKey() = {
    consoleReader.getTerminal.disableEcho()
    def waitEOF {
      consoleReader.readVirtualKey() match {
        case 4 => // STOP
        case 11 => consoleReader.clearScreen(); waitEOF
        case 10 => println(); waitEOF
        case _ => waitEOF
      }

    }
    waitEOF
    consoleReader.getTerminal.enableEcho()
  }

  // ----- Exceptions

  case class CompilationException(problem: xsbti.Problem) extends PlayException(
    "Compilation error", problem.message) with PlayException.ExceptionSource {
    def line = problem.position.line.map(m => m.asInstanceOf[Int])
    def position = problem.position.pointer.map(m => m.asInstanceOf[Int])
    def input = problem.position.sourceFile.map(scalax.file.Path(_))
    def sourceName = problem.position.sourceFile.map(_.getAbsolutePath)
  }

  case class TemplateCompilationException(source: File, message: String, atLine: Int, column: Int) extends PlayException(
    "Compilation error", message) with PlayException.ExceptionSource {
    def line = Some(atLine)
    def position = Some(column)
    def input = Some(scalax.file.Path(source))
    def sourceName = Some(source.getAbsolutePath)
  }

  case class RoutesCompilationException(source: File, message: String, atLine: Option[Int], column: Option[Int]) extends PlayException(
    "Compilation error", message) with PlayException.ExceptionSource {
    def line = atLine
    def position = column
    def input = Some(scalax.file.Path(source))
    def sourceName = Some(source.getAbsolutePath)
  }

  // ----- Keys

  val distDirectory = SettingKey[File]("play-dist")
  val playResourceDirectories = SettingKey[Seq[File]]("play-resource-directories")
  val confDirectory = SettingKey[File]("play-conf")
  val templatesImport = SettingKey[Seq[String]]("play-templates-imports")

  val templatesTypes = SettingKey[PartialFunction[String, (String, String)]]("play-templates-formats")
  val minify = SettingKey[Boolean]("minify", "Whether assets (Javascript and CSS) should be minified or not")

  // -- Utility methods for 0.10-> 0.11 migration
  def inAllDeps[T](base: ProjectRef, deps: ProjectRef => Seq[ProjectRef], key: ScopedSetting[T], data: Settings[Scope]): Seq[T] =
    inAllProjects(Dag.topologicalSort(base)(deps), key, data)
  def inAllProjects[T](allProjects: Seq[Reference], key: ScopedSetting[T], data: Settings[Scope]): Seq[T] =
    allProjects.flatMap { p => key in p get data }

  def inAllDependencies[T](base: ProjectRef, key: ScopedSetting[T], structure: Load.BuildStructure): Seq[T] = {
    def deps(ref: ProjectRef): Seq[ProjectRef] =
      Project.getProject(ref, structure).toList.flatMap { p =>
        p.dependencies.map(_.project) ++ p.aggregate
      }
    inAllDeps(base, deps, key, structure.data)
  }

  // ----- Play specific tasks
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

      val cp = deps.map(_.data.toURL).toArray :+ classes.toURL

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

  // ----- Reloader

  def newReloader(state: State) = {

    val extracted = Project.extract(state)

    new ReloadableApplication(extracted.currentProject.base) {

      // ----- Internal state used for reloading is kept here

      val watchFiles = {
        ((extracted.currentProject.base / "db" / "evolutions") ** "*.sql").get ++ ((extracted.currentProject.base / "conf") ** "*").get
      }

      var forceReload = false
      var currentProducts = Map.empty[java.io.File, Long]
      var currentAnalysis = Option.empty[sbt.inc.Analysis]

      def forceReloadNextTime() { forceReload = true }

      def updateAnalysis(newAnalysis: sbt.inc.Analysis) = {
        val classFiles = newAnalysis.stamps.allProducts ++ watchFiles
        val newProducts = classFiles.map { classFile =>
          classFile -> classFile.lastModified
        }.toMap
        val updated = if (newProducts != currentProducts || forceReload) {
          Some(newProducts)
        } else {
          None
        }
        updated.foreach(currentProducts = _)
        currentAnalysis = Some(newAnalysis)

        forceReload = false

        updated
      }

      def findSource(className: String) = {
        val topType = className.split('$').head
        currentAnalysis.flatMap { analysis =>
          analysis.apis.internal.flatMap {
            case (sourceFile, source) => {
              source.api.definitions.find(defined => defined.name == topType).map(_ => {
                sourceFile: java.io.File
              })
            }
          }.headOption
        }
      }

      def remapProblemForGeneratedSources(problem: xsbti.Problem) = {

        problem.position.sourceFile.collect {

          // Templates
          case play.templates.MaybeGeneratedSource(generatedSource) => {
            new xsbti.Problem {
              def message = problem.message
              def position = new xsbti.Position {
                def line = {
                  problem.position.line.map(l => generatedSource.mapLine(l.asInstanceOf[Int])).map(l => xsbti.Maybe.just(l.asInstanceOf[java.lang.Integer])).getOrElse(xsbti.Maybe.nothing[java.lang.Integer])
                }
                def lineContent = ""
                def offset = xsbti.Maybe.nothing[java.lang.Integer]
                def pointer = {
                  problem.position.offset.map { offset =>
                    generatedSource.mapPosition(offset.asInstanceOf[Int]) - IO.read(generatedSource.source.get).split('\n').take(problem.position.line.map(l => generatedSource.mapLine(l.asInstanceOf[Int])).get - 1).mkString("\n").size - 1
                  }.map { p =>
                    xsbti.Maybe.just(p.asInstanceOf[java.lang.Integer])
                  }.getOrElse(xsbti.Maybe.nothing[java.lang.Integer])
                }
                def pointerSpace = xsbti.Maybe.nothing[String]
                def sourceFile = xsbti.Maybe.just(generatedSource.source.get)
                def sourcePath = xsbti.Maybe.just(sourceFile.get.getCanonicalPath)
              }
              def severity = problem.severity
            }
          }

          // Routes files
          case play.core.Router.RoutesCompiler.MaybeGeneratedSource(generatedSource) => {
            new xsbti.Problem {
              def message = problem.message
              def position = new xsbti.Position {
                def line = {
                  problem.position.line.flatMap(l => generatedSource.mapLine(l.asInstanceOf[Int])).map(l => xsbti.Maybe.just(l.asInstanceOf[java.lang.Integer])).getOrElse(xsbti.Maybe.nothing[java.lang.Integer])
                }
                def lineContent = ""
                def offset = xsbti.Maybe.nothing[java.lang.Integer]
                def pointer = xsbti.Maybe.nothing[java.lang.Integer]
                def pointerSpace = xsbti.Maybe.nothing[String]
                def sourceFile = xsbti.Maybe.just(new File(generatedSource.source.get.path))
                def sourcePath = xsbti.Maybe.just(sourceFile.get.getCanonicalPath)
              }
              def severity = problem.severity
            }
          }

        }.getOrElse {
          problem
        }

      }

      def getProblems(incomplete: Incomplete): Seq[xsbti.Problem] = {
        (Compiler.allProblems(incomplete) ++ {
          Incomplete.linearize(incomplete).filter(i => i.node.isDefined && i.node.get.isInstanceOf[ScopedKey[_]]).flatMap { i =>
            val JavacError = """\[error\]\s*(.*[.]java):(\d+):\s*(.*)""".r
            val JavacErrorInfo = """\[error\]\s*([a-z ]+):(.*)""".r
            val JavacErrorPosition = """\[error\](\s*)\^\s*""".r

            Project.evaluateTask(streamsManager, state).get.toEither.right.toOption.map { streamsManager =>
              var first: (Option[(String, String, String)], Option[Int]) = (None, None)
              var parsed: (Option[(String, String, String)], Option[Int]) = (None, None)
              Output.lastLines(i.node.get.asInstanceOf[ScopedKey[_]], streamsManager).map(_.replace(scala.Console.RESET, "")).map(_.replace(scala.Console.RED, "")).collect {
                case JavacError(file, line, message) => parsed = Some((file, line, message)) -> None
                case JavacErrorInfo(key, message) => parsed._1.foreach { o =>
                  parsed = Some((parsed._1.get._1, parsed._1.get._2, parsed._1.get._3 + " [" + key.trim + ": " + message.trim + "]")) -> None
                }
                case JavacErrorPosition(pos) => {
                  parsed = parsed._1 -> Some(pos.size)
                  if (first == (None, None)) {
                    first = parsed
                  }
                }
              }
              first
            }.collect {
              case (Some(error), maybePosition) => new xsbti.Problem {
                def message = error._3
                def position = new xsbti.Position {
                  def line = xsbti.Maybe.just(error._2.toInt)
                  def lineContent = ""
                  def offset = xsbti.Maybe.nothing[java.lang.Integer]
                  def pointer = maybePosition.map(pos => xsbti.Maybe.just((pos - 1).asInstanceOf[java.lang.Integer])).getOrElse(xsbti.Maybe.nothing[java.lang.Integer])
                  def pointerSpace = xsbti.Maybe.nothing[String]
                  def sourceFile = xsbti.Maybe.just(file(error._1))
                  def sourcePath = xsbti.Maybe.just(error._1)
                }
                def severity = xsbti.Severity.Error
              }
            }

          }
        }).map(remapProblemForGeneratedSources)
      }

      private def newClassloader = {
        new ApplicationClassLoader(this.getClass.getClassLoader, {
          Project.evaluateTask(dependencyClasspath in Runtime, state).get.toEither.right.get.map(_.data.toURI.toURL).toArray
        })
      }

      def reload = {

        PlayProject.synchronized {

          Project.evaluateTask(playReload, state).get.toEither
            .left.map { incomplete =>
              Incomplete.allExceptions(incomplete).headOption.map {
                case e: PlayException => e
                case e: xsbti.CompileFailed => {
                  getProblems(incomplete).headOption.map(CompilationException(_)).getOrElse {
                    UnexpectedException(Some("Compilation failed without reporting any problem!?"), Some(e))
                  }
                }
                case e => UnexpectedException(unexpected = Some(e))
              }.getOrElse(
                UnexpectedException(Some("Compilation task failed without any exception!?")))
            }
            .right.map { compilationResult =>
              updateAnalysis(compilationResult).map { _ =>
                newClassloader
              }
            }

        }

      }

      override def handleWebCommand(request: play.api.mvc.RequestHeader) = {

        val applyEvolutions = """/@evolutions/apply/([a-zA-Z0-9_]+)""".r

        request.path match {

          case applyEvolutions(db) => {
            import play.api.db._
            import play.api.db.evolutions._
            import play.api.mvc.Results._

            OfflineEvolutions.applyScript(extracted.currentProject.base, newClassloader, db)

            forceReloadNextTime()

            Some(Redirect(request.queryString.get("redirect").filterNot(_.isEmpty).map(_(0)).getOrElse("/")))
          }

          case _ => None

        }
      }

    }

  }

  def parseRunArgs(args: Seq[String], defaultPort: Int = 9000, defaultHost: String = "0.0.0.0") = {

    def parsePort(portString: String): Option[Int] = {
      try {
        Some(Integer.parseInt(portString))
      } catch {
        case e => {
          sys.error("Invalid port argument: " + portString)
          None
        }
      }
    }

    args match {
      case Seq(port) => (parsePort(port).getOrElse(defaultPort), defaultHost)
      case Seq(port, host) => (parsePort(port).getOrElse(defaultPort), host)
      case _ => (defaultPort, defaultHost)
    }
  }
  // ----- Play commands

  val playRunCommand = Command.args("run", "<port> <host>") { (state: State, args: Seq[String]) =>

    val (port, host) = parseRunArgs(args)

    val reloader = newReloader(state)

    println()

    val server = new play.core.server.NettyServer(reloader, host, port, allowKeepAlive = false)

    println()
    println(Colors.green("(Server started, use Ctrl+D to stop and go back to the console...)"))
    println()

    waitForKey()

    server.stop()

    println()

    state
  }

  val playStartCommand = Command.args("start", "<port> <host>") { (state: State, args: Seq[String]) =>

    val (port, host) = parseRunArgs(args)

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
            "java", "-Dhttp.port=" + port, "-Dhttp.host=" + host, "-cp", classpath, "play.core.server.NettyServer", extracted.currentProject.base.getCanonicalPath): _*)

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
                |run <port> <host>          Run the current application in DEV mode.
                |start <port> <host>        Start the current application in another JVM in PROD mode.
                |test                       Run Junit tests and/or Specs
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
      org.h2.tools.Server.main()
    } catch {
      case _ =>
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

  // ----- Default settings

  lazy val defaultJavaSettings = Seq[Setting[_]](

    templatesImport ++= Seq(
      "models._",
      "controllers._",

      "java.lang._",
      "java.util._",

      "scala.collection.JavaConversions._",
      "scala.collection.JavaConverters._",

      "play.api.i18n.Messages",

      "play.mvc._",
      "play.data._",
      "com.avaje.ebean._",

      "play.mvc.Http.Context.Implicit._",

      "views.%format%._"))

  lazy val defaultScalaSettings = Seq[Setting[_]](

    templatesImport ++= Seq(
      "models._",
      "controllers._",

      "play.api.i18n.Messages",

      "play.api.mvc._",
      "play.api.data._",

      "views.%format%._"))

  lazy val defaultSettings = Seq[Setting[_]](

    resolvers ++= Seq(
      Resolver.url("Play Repository", url("http://download.playframework.org/ivy-releases/"))(Resolver.ivyStylePatterns),
      "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"),

    target <<= baseDirectory / "target",

    sourceDirectory in Compile <<= baseDirectory / "app",
    sourceDirectory in Test <<= baseDirectory / "test",

    confDirectory <<= baseDirectory / "conf",

    scalaSource in Compile <<= baseDirectory / "app",
    scalaSource in Test <<= baseDirectory / "test",

    javaSource in Compile <<= baseDirectory / "app",
    javaSource in Test <<= baseDirectory / "test",

    distDirectory <<= baseDirectory / "dist",

    libraryDependencies += "play" %% "play" % play.core.PlayVersion.current,

    libraryDependencies ++= Seq("org.specs2" %% "specs2" % "1.6.1" % "test",
      "com.novocode" % "junit-interface" % "0.7" % "test",
      "org.seleniumhq.selenium" % "selenium-chrome-driver" % "2.11.0" % "test",
      "org.seleniumhq.selenium" % "selenium-htmlunit-driver" % "2.11.0" % "test"),

    sourceGenerators in Compile <+= (confDirectory, sourceManaged in Compile) map RouteFiles,

    sourceGenerators in Compile <+= (sourceDirectory in Compile, sourceManaged in Compile, templatesTypes, templatesImport) map ScalaTemplates,

    commands ++= Seq(playCommand, playRunCommand, playStartCommand, playHelpCommand, h2Command, classpathCommand, licenseCommand, computeDependenciesCommand),

    shellPrompt := playPrompt,

    copyResources in Compile <<= (copyResources in Compile, playCopyResources) map { (r, pr) => r ++ pr },

    mainClass in (Compile, run) := Some(classOf[play.core.server.NettyServer].getName),

    compile in (Compile) <<= PostCompile,

    dist <<= distTask,

    computeDependencies <<= computeDependenciesTask,

    playCopyResources <<= playCopyResourcesTask,

    playCompileEverything <<= playCompileEverythingTask,

    playPackageEverything <<= playPackageEverythingTask,

    playReload <<= playReloadTask,

    playStage <<= playStageTask,

    cleanFiles <+= distDirectory.identity,

    resourceGenerators in Compile <+= LessCompiler,

    resourceGenerators in Compile <+= CoffeescriptCompiler,

    resourceGenerators in Compile <+= JavascriptCompiler,

    minify := false,

    playResourceDirectories := Seq.empty[File],

    playResourceDirectories <+= baseDirectory / "conf",

    playResourceDirectories <+= baseDirectory / "public",

    templatesImport := Seq("play.api.templates._", "play.api.templates.PlayMagic._"),

    templatesTypes := {
      case "html" => ("play.api.templates.Html", "play.api.templates.HtmlFormat")
      case "txt" => ("play.api.templates.Txt", "play.api.templates.TxtFormat")
      case "xml" => ("play.api.templates.Xml", "play.api.templates.XmlFormat")
    })

  // ----- Create a Play project with default settings

  def apply(name: String, applicationVersion: String = "1.0", dependencies: Seq[ModuleID] = Nil, path: File = file(".")) = {

    Project(name, path)
      .settings(parallelExecution in Test := false)
      .settings(PlayProject.defaultSettings: _*)
      .settings(

        version := applicationVersion,

        libraryDependencies ++= dependencies)
  }
}
