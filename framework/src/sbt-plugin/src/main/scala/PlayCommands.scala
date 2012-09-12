package sbt

import Keys._
import CommandSupport.{ ClearOnFailure, FailureWall }
import complete.Parser
import Parser._
import Cache.seqFormat
import sbinary.DefaultProtocol.StringFormat

import play.api._
import play.core._

import play.utils.Colors

import PlayExceptions._
import PlayKeys._

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import java.lang.{ ProcessBuilder => JProcessBuilder }

trait PlayCommands extends PlayEclipse {
  this: PlayReloader =>

  //- mainly scala, mainly java or none

  val JAVA = "java"
  val SCALA = "scala"
  val NONE = "none"

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
      case jar if jar.getName.startsWith("h2-") || jar.getName == "h2.jar" => jar.toURI.toURL
    }

    if (commonClassLoader == null) {
      commonClassLoader = new java.net.URLClassLoader(classpath.map(_.data).collect(commonJars).toArray, si.loader) {
        override def toString = "Common ClassLoader: " + getURLs.map(_.toString).mkString(",")
      }
    }

    commonClassLoader
  }

  val playVersion = SettingKey[String]("play-version")

  val playCompileEverything = TaskKey[Seq[sbt.inc.Analysis]]("play-compile-everything")
  val playCompileEverythingTask = (state, thisProjectRef) flatMap { (s, r) =>
    inAllDependencies(r, (compile in Compile).task, Project structure s).join
  }

  val playPackageEverything = TaskKey[Seq[File]]("play-package-everything")
  val playPackageEverythingTask = (state, thisProjectRef) flatMap { (s, r) =>
    inAllDependencies(r, (packageBin in Compile).task, Project structure s).join
  }

  val playCopyAssets = TaskKey[Seq[(File, File)]]("play-copy-assets")
  val playCopyAssetsTask = (baseDirectory, managedResources in Compile, resourceManaged in Compile, playAssetsDirectories, playExternalAssets, classDirectory in Compile, cacheDirectory, streams) map { (b, resources, resourcesDirectories, r, externals, t, c, s) =>
    val cacheFile = c / "copy-assets"
    
    val mappings = (r.map(_ ***).foldLeft(PathFinder.empty)(_ +++ _).filter(_.isFile) x relativeTo(b +: r.filterNot(_.getAbsolutePath.startsWith(b.getAbsolutePath))) map {
      case (origin, name) => (origin, new java.io.File(t, name))
    }) ++ (resources x rebase(resourcesDirectories, t))
    
    val externalMappings = externals.map {
      case (root, paths, common) => {
        paths(root) x relativeTo(root :: Nil) map {
          case (origin, name) => (origin, new java.io.File(t, common + "/" + name))
        }
      }
    }.foldLeft(Seq.empty[(java.io.File, java.io.File)])(_ ++ _)

    /*
    Disable GZIP Generation for this release.
    -----
     
    val toZip = mappings.collect { case (resource, _) if resource.isFile && !resource.getName.endsWith(".gz") => resource } x relativeTo(Seq(b, resourcesDirectories))

    val gzipped = toZip.map {
      case (resource, path) => {
        s.log.debug("Gzipping " + resource)
        val zipFile = new File(resourcesDirectories, path + ".gz")
        IO.gzip(resource, zipFile)
        zipFile -> new File(t, path + ".gz")
      }
    }

    val assetsMapping = mappings ++ gzipped*/

    val assetsMapping = mappings ++ externalMappings

    s.log.debug("Copy play resource mappings: " + mappings.mkString("\n\t", "\n\t", ""))

    Sync(cacheFile)(assetsMapping)
    assetsMapping
  }

  //- test reporter
  private[sbt] lazy val testListener = new PlayTestListener

  val testResultReporter = TaskKey[List[String]]("test-result-reporter")
  val testResultReporterTask = (state, thisProjectRef) map { (s, r) =>
    testListener.result.toList
  }
  val testResultReporterReset = TaskKey[Unit]("test-result-reporter-reset")
  val testResultReporterResetTask = (state, thisProjectRef) map { (s, r) =>
    testListener.result.clear
  }

  val playReload = TaskKey[sbt.inc.Analysis]("play-reload")
  val playReloadTask = (playCopyAssets, playCompileEverything) map { (_, analysises) =>
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
        val filename = for {
          module <- dependency.metadata.get(AttributeKey[ModuleID]("module-id"))
          artifact <- dependency.metadata.get(AttributeKey[Artifact]("artifact"))
        } yield {
          module.organization + "." + module.name + "-" + artifact.name + "-" + module.revision + ".jar"
        }
        val path = (packageName + "/lib/" + filename.getOrElse(dependency.data.getName))
        dependency.data -> path
      } ++ packaged.map(jar => jar -> (packageName + "/lib/" + jar.getName))
    }

    val start = target / "start"

    val customConfig = Option(System.getProperty("config.file"))
    val customFileName = customConfig.map(f=>Some( (new File(f)).getName)).getOrElse(None)

    IO.write(start,
      """#!/usr/bin/env sh

exec java $* -cp "`dirname $0`/lib/*" """ + customFileName.map(fn => "-Dconfig.file=`dirname $0`/"+fn+" ").getOrElse("") + """play.core.server.NettyServer `dirname $0`
""" /* */ )
    val scripts = Seq(start -> (packageName + "/start"))

    val other = Seq((root / "README") -> (packageName + "/README"))

    val productionConfig = customFileName.map(fn => target / fn).getOrElse(target / "application.conf")

    val prodApplicationConf = customConfig.map { location =>
      val customConfigFile = new File(location)
      IO.copyFile(customConfigFile, productionConfig)
      Seq(productionConfig -> (packageName + "/"+customConfigFile.getName))
    }.getOrElse(Nil)

    IO.zip(libs ++ scripts ++ other ++ prodApplicationConf, zip)
    IO.delete(start)
    IO.delete(productionConfig)

    println()
    println("Your application is ready in " + zip.getCanonicalPath)
    println()

    zip
  }


  def intellijCommandSettings(mainLang: String) = {
    import org.sbtidea.SbtIdeaPlugin
    SbtIdeaPlugin.ideaSettings ++ 
    Seq(
      SbtIdeaPlugin.commandName := "idea",
      SbtIdeaPlugin.addGeneratedClasses := true,
      SbtIdeaPlugin.includeScalaFacet := {mainLang == SCALA},
      SbtIdeaPlugin.defaultClassifierPolicy := false
    )
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
      """|#!/usr/bin/env sh
         |
         |exec java $@ -cp "`dirname $0`/staged/*" play.core.server.NettyServer `dirname $0`/..
         |""".stripMargin)

    "chmod a+x %s".format(start.getAbsolutePath) !

    s.log.info("")
    s.log.info("Your application is ready to be run in place: target/start")
    s.log.info("")

    ()
  }

  val playHash = TaskKey[String]("play-hash")
  val playHashTask = (state, thisProjectRef, playExternalAssets) map { (s,r, externalAssets) =>
    val filesToHash = inAllDependencies(r, baseDirectory, Project structure s).map {base =>
      (base / "src" / "main" ** "*") +++ (base / "app" ** "*") +++ (base / "conf" ** "*") +++ (base / "public" ** "*")
    }.foldLeft(PathFinder.empty)(_ +++ _)
    ( filesToHash +++ externalAssets.map {
      case (root, paths, _) => paths(root)
    }.foldLeft(PathFinder.empty)(_ +++ _)).get.map(_.lastModified).mkString(",").hashCode.toString
  }

  // ----- Assets

  // Name: name of the compiler
  // files: the function to find files to compile from the assets directory
  // naming: how to name the generated file from the original file and whether it should be minified or not
  // compile: compile the file and return the compiled sources, the minified source (if relevant) and the list of dependencies
  def AssetsCompiler(name: String,
    watch: File => PathFinder,
    filesSetting: sbt.SettingKey[PathFinder],
    naming: (String, Boolean) => String,
    compile: (File, Seq[String]) => (String, Option[String], Seq[File]),
    optionsSettings: sbt.SettingKey[Seq[String]]) =
    (sourceDirectory in Compile, resourceManaged in Compile, cacheDirectory, optionsSettings, filesSetting) map { (src, resources, cache, options, files) =>

      import java.io._

      val cacheFile = cache / name
      val currentInfos = watch(src).get.map(f => f -> FileInfo.lastModified(f)).toMap
      val (previousRelation, previousInfo) = Sync.readInfo(cacheFile)(FileInfo.lastModified.format)

      if (previousInfo != currentInfos) {

        // Delete previous generated files
        previousRelation._2s.foreach(IO.delete)

        val generated = (files x relativeTo(Seq(src / "assets"))).flatMap {
          case (sourceFile, name) => {
            val (debug, min, dependencies) = compile(sourceFile, options)
            val out = new File(resources, "public/" + naming(name, false))
            val outMin = new File(resources, "public/" + naming(name, true))
            IO.write(out, debug)
            dependencies.map(_ -> out) ++ min.map { minified =>
              IO.write(outMin, minified)
              dependencies.map(_ -> outMin)
            }.getOrElse(Nil)
          }
        }

        Sync.writeInfo(cacheFile,
          Relation.empty[File, File] ++ generated,
          currentInfos)(FileInfo.lastModified.format)

        // Return new files
        generated.map(_._2).distinct.toList

      } else {

        // Return previously generated files
        previousRelation._2s.toSeq

      }

    }

  val LessCompiler = AssetsCompiler("less",
    (_ ** "*.less"),
    lessEntryPoints,
    { (name, min) => name.replace(".less", if (min) ".min.css" else ".css") },
    { (lessFile, options) => play.core.less.LessCompiler.compile(lessFile) },
    lessOptions
  )

  val JavascriptCompiler = AssetsCompiler("javascripts",
    (_ ** "*.js"),
    javascriptEntryPoints,
    { (name, min) => name.replace(".js", if (min) ".min.js" else ".js") },
    { (jsFile: File, options) => play.core.jscompile.JavascriptCompiler.compile(jsFile, options) },
    closureCompilerOptions
  )

  val CoffeescriptCompiler = AssetsCompiler("coffeescript",
    (_ ** "*.coffee"),
    coffeescriptEntryPoints,
    { (name, min) => name.replace(".coffee", if (min) ".min.js" else ".js") },
    { (coffeeFile, options) =>
      import scala.util.control.Exception._
      val jsSource = play.core.coffeescript.CoffeescriptCompiler.compile(coffeeFile, options)
      // Any error here would be because of CoffeeScript, not the developer;
      // so we don't want compilation to fail.
      val minified = catching(classOf[CompilationException])
        .opt(play.core.jscompile.JavascriptCompiler.minify(jsSource, Some(coffeeFile.getName())))
      (jsSource, minified, Seq(coffeeFile))
    },
    coffeescriptOptions
  )

  // ----- Post compile (need to be refactored and fully configurable)

  def PostCompile(scope: Configuration) = (sourceDirectory in scope, dependencyClasspath in scope, compile in scope, javaSource in scope, sourceManaged in scope, classDirectory in scope, ebeanEnabled) map { (src, deps, analysis, javaSrc, srcManaged, classes, ebean) =>

    val classpath = (deps.map(_.data.getAbsolutePath).toArray :+ classes.getAbsolutePath).mkString(java.io.File.pathSeparator)

    val javaClasses = (javaSrc ** "*.java").get.map { sourceFile =>
      analysis.relations.products(sourceFile)
    }.flatten.distinct 

    javaClasses.foreach(play.core.enhancers.PropertiesEnhancer.generateAccessors(classpath, _))
    javaClasses.foreach(play.core.enhancers.PropertiesEnhancer.rewriteAccess(classpath, _))

    // EBean
    if (ebean) {
      
      val originalContextClassLoader = Thread.currentThread.getContextClassLoader
      
      try {

        val cp = deps.map(_.data.toURI.toURL).toArray :+ classes.toURI.toURL
        
        Thread.currentThread.setContextClassLoader(new java.net.URLClassLoader(cp, ClassLoader.getSystemClassLoader))

        import com.avaje.ebean.enhance.agent._
        import com.avaje.ebean.enhance.ant._
        import collection.JavaConverters._
        import com.typesafe.config._
        
        val cl = ClassLoader.getSystemClassLoader

        val t = new Transformer(cp, "debug=-1")

        val ft = new OfflineFileTransform(t, cl, classes.getAbsolutePath, classes.getAbsolutePath)

        val config = ConfigFactory.load(ConfigFactory.parseFileAnySyntax(new File("conf/application.conf")))

        val models = try {
          config.getConfig("ebean").entrySet.asScala.map(_.getValue.unwrapped).toSet.mkString(",")
        } catch { case e: ConfigException.Missing => "models.*" }
        
        try {
          ft.process(models)
        } catch {
          case _ =>
        }
        
      } catch {
        case e => throw e
      } finally {
        Thread.currentThread.setContextClassLoader(originalContextClassLoader)
      }
    }
    // Copy managed classes - only needed in Compile scope
    if (scope.name.toLowerCase == "compile") {
      val managedClassesDirectory = classes.getParentFile / (classes.getName + "_managed")

      val managedClasses = ((srcManaged ** "*.scala").get ++ (srcManaged ** "*.java").get).map { managedSourceFile =>
        analysis.relations.products(managedSourceFile)
      }.flatten x rebase(classes, managedClassesDirectory)

      // Copy modified class files
      val managedSet = IO.copy(managedClasses)

      // Remove deleted class files
      (managedClassesDirectory ** "*.class").get.filterNot(managedSet.contains(_)).foreach(_.delete())
    }
    analysis
  }

  // ----- Source generators

  val RouteFiles = (confDirectory: File, generatedDir: File, additionalImports: Seq[String]) => {
    import play.core.Router.RoutesCompiler._

    ((generatedDir ** "routes.java").get ++ (generatedDir ** "routes_*.scala").get).map(GeneratedSource(_)).foreach(_.sync())
    try {
      (confDirectory * "routes").get.foreach { routesFile =>
        compile(routesFile, generatedDir, additionalImports)
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

  private def fork(args: Seq[String]) = {
    val builder = new JProcessBuilder(args: _*)
    Process(builder).run(JvmIO(new JvmLogger(), false))
  }

  val shCommand = Command.args("sh", "<shell command>") { (state: State, args: Seq[String]) =>
    if (args.isEmpty)
      println("sh <command to run>")
    else
      fork(args)
    state
  }

  private def filterArgs(args: Seq[String]): (Seq[(String, String)], Int) = {
    val (properties, others) = args.span(_.startsWith("-D"))
    // collect arguments plus config file property if present 
    val javaProperties = properties.map(_.drop(2).split('=')).map(a => a(0) -> a(1)).toSeq
    val port = others.headOption.map { portString =>
      try {
        Integer.parseInt(portString)
      } catch {
        case e => sys.error("Invalid port argument: " + portString)
      }
    }.getOrElse(9000)
    (javaProperties, port)
  }

  val playRunCommand = Command.args("run", "<args>") { (state: State, args: Seq[String]) =>

    // Parse HTTP port argument
    val (properties, port) = filterArgs(args)

    // Set Java properties
    properties.foreach {
      case (key, value) => System.setProperty(key, value)
    }

    println()

    val sbtLoader = this.getClass.getClassLoader
    def commonLoaderEither = Project.runTask(playCommonClassloader, state).get._2.toEither
    val commonLoader = commonLoaderEither.right.toOption.getOrElse{
        state.log.warn("some of the dependencies were not recompiled properly, so classloader is not avaialable")
        throw commonLoaderEither.left.get
      }
    val maybeNewState = Project.runTask(dependencyClasspath in Compile, state).get._2.toEither.right.map { dependencies =>

      // All jar dependencies. They will not been reloaded and must be part of this top classloader
      val classpath = dependencies.map(_.data.toURI.toURL).filter(_.toString.endsWith(".jar")).toArray

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

        // -- Delegate resource loading. We have to hack here because the default implementation are already recursives.

        override def getResource(name: String): java.net.URL = {
          val findResource = classOf[ClassLoader].getDeclaredMethod("findResource", classOf[String])
          findResource.setAccessible(true)
          val resource = reloader.currentApplicationClassLoader.map(findResource.invoke(_, name).asInstanceOf[java.net.URL]).orNull
          if (resource == null) {
            super.getResource(name)
          } else {
            resource
          }
        }

        override def getResources(name: String): java.util.Enumeration[java.net.URL] = {
          val findResources = classOf[ClassLoader].getDeclaredMethod("findResources", classOf[String])
          findResources.setAccessible(true)
          val resources1 = reloader.currentApplicationClassLoader.map(findResources.invoke(_, name).asInstanceOf[java.util.Enumeration[java.net.URL]]).getOrElse(new java.util.Vector[java.net.URL]().elements)
          val resources2 = super.getResources(name)
          val resources = new java.util.Vector[java.net.URL](
            (resources1.asScala.toList ++ resources2.asScala.toList).distinct.asJava
          )
          resources.elements
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
      println(Colors.green("(Server started, use Ctrl+D to stop and go back to the console...)"))
      println()

      val ContinuousState = AttributeKey[WatchState]("watch state", "Internal: tracks state for continuous execution.")
      def isEOF(c: Int): Boolean = c == 4

      @tailrec def executeContinuously(watched: Watched, s: State, reloader: SBTLink, ws: Option[WatchState] = None): Option[String] = {
        @tailrec def shouldTerminate: Boolean = (System.in.available > 0) && (isEOF(System.in.read()) || shouldTerminate)

        val sourcesFinder = PathFinder { watched watchPaths s }
        val watchState = ws.getOrElse(s get ContinuousState getOrElse WatchState.empty)

        val (triggered, newWatchState, newState) =
          try {
            val (triggered, newWatchState) = SourceModificationWatch.watch(sourcesFinder, watched.pollInterval, watchState)(shouldTerminate)
            (triggered, newWatchState, s)
          } catch {
            case e: Exception =>
              val log = s.log
              log.error("Error occurred obtaining files to watch.  Terminating continuous execution...")
              BuiltinCommands.handleException(e, s, log)
              (false, watchState, s.fail)
          }

        if (triggered) {
          //Then launch compile
          PlayProject.synchronized {
            val start = System.currentTimeMillis
            Project.runTask(compile in Compile, newState).get._2.toEither.right.map { _ =>
              val duration = System.currentTimeMillis - start
              val formatted = duration match {
                case ms if ms < 1000 => ms + "ms"
                case s => (s / 1000) + "s"
              }
              println("[" + Colors.green("success") + "] Compiled in " + formatted)
            }
          }

          // Avoid launching too much compilation
          Thread.sleep(Watched.PollDelayMillis)

          // Call back myself
          executeContinuously(watched, newState, reloader, Some(newWatchState))
        } else {
          // Stop 
          Some("Okay, i'm done")
        }
      }

      // If we have both Watched.Configuration and Watched.ContinuousState
      // attributes and if Watched.ContinuousState.count is 1 then we assume
      // we're in ~ run mode
      val maybeContinuous = state.get(Watched.Configuration).map { w =>
        state.get(Watched.ContinuousState).map { ws =>
          (ws.count == 1, w, ws)
        }.getOrElse((false, None, None))
      }.getOrElse((false, None, None))

      val newState = maybeContinuous match {
        case (true, w: sbt.Watched, ws) => {
          // ~ run mode
          consoleReader.getTerminal.disableEcho()
          executeContinuously(w, state, reloader)
          consoleReader.getTerminal.enableEcho()

          // Remove state two first commands added by sbt ~
          state.copy(remainingCommands = state.remainingCommands.drop(2)).remove(Watched.ContinuousState)
        }
        case _ => {
          // run mode
          waitForKey()
          state
        }
      }

      server.stop()
      reloader.clean()

      newState
    }

    // Remove Java properties
    properties.foreach {
      case (key, _) => System.clearProperty(key)
    }

    println()

    maybeNewState match {
      case Right(x) => x
      case _ => state
    }
  }

  val playStartCommand = Command.args("start", "<port>") { (state: State, args: Seq[String]) =>

    // Parse HTTP port argument
    val (properties, port) = filterArgs(args)

    val extracted = Project.extract(state)

    Project.runTask(compile in Compile, state).get._2.toEither match {
      case Left(_) => {
        println()
        println("Cannot start with errors.")
        println()
        state.fail
      }
      case Right(_) => {

        Project.runTask(dependencyClasspath in Runtime, state).get._2.toEither.right.map { dependencies =>

          val classpath = dependencies.map(_.data).map(_.getCanonicalPath).reduceLeft(_ + java.io.File.pathSeparator + _)

          import java.lang.{ ProcessBuilder => JProcessBuilder }
          val builder = new JProcessBuilder(Seq(
            "java") ++ (properties ++ System.getProperties.asScala).map { case (key, value) => "-D" + key + "=" + value } ++ Seq("-Dhttp.port=" + port, "-cp", classpath, "play.core.server.NettyServer", extracted.currentProject.base.getCanonicalPath): _*)

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

  val playCommand = Command.command("play", Help("play", ("play", "Enter the play console"), """
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
        |play-version               Display the Play version.
        |publish                    Publish your application in a remote repository.
        |publish-local              Publish your application in the local repository.
        |reload                     Reload the current application build file.
        |run <port>                 Run the current application in DEV mode.
        |test                       Run Junit tests and/or Specs from the command line
        |eclipsify                  generate eclipse project file
        |idea                       generate Intellij IDEA project file
        |sh <command to run>        execute a shell command 
        |start <port>               Start the current application in another JVM in PROD mode.
        |update                     Update application dependencies.
        |
        |Type `help` to get the standard sbt help.
        |""".stripMargin)) { state: State =>

    val extracted = Project.extract(state)
    import extracted._

    // Display logo
    println(play.console.Console.logo)
    println("""
            |> Type "help play" or "license" for more information.
            |> Type "exit" or use Ctrl+D to leave this console.
            |""".stripMargin)

    state.copy(
      remainingCommands = state.remainingCommands :+ "shell")

  }

  val h2Command = Command.command("h2-browser") { state: State =>
    try {
      val commonLoader = Project.runTask(playCommonClassloader, state).get._2.toEither.right.get
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
      |Copyright 2012 Typesafe <http://www.typesafe.com>
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

    Project.runTask(dependencyClasspath in Runtime, state).get._2.toEither match {
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

    Project.runTask(computeDependencies, state).get._2.toEither match {
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
              case callers: Seq[_] => callers.map {
                case (org, name, rev) => org.toString + ":" + name.toString + ":" + rev.toString
              }
            }.flatten.toSeq,

            module.get('evictedBy).map {
              case Some(rev) => Seq("Evicted by " + rev)
              case None => module.get('artifacts).map {
                case artifacts: Seq[_] => artifacts.map("As " + _.toString)
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
