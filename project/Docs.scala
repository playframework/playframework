/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
import sbt._
import sbt.internal.BuildStructure
import sbt.Keys._
import sbt.File
import sbt.util.CacheStoreFactory
import sbt.internal.inc.AnalyzingCompiler
import sbt.internal.inc.LoggedReporter
import java.net.URLClassLoader
import org.webjars.WebJarExtractor
import interplay.Playdoc
import interplay.Playdoc.autoImport._
import xsbti.compile.Compilers
import xsbti.compile._
import sbt.io.Path._
import interplay.Playdoc
import interplay.Playdoc.autoImport._
import sbt.inc.Doc.JavaDoc
import sbt.internal.util.ManagedLogger
import xsbti.Reporter

object Docs {
  val Webjars = config("webjars").hide

  val apiDocsInclude =
    SettingKey[Boolean]("apiDocsInclude", "Whether this sub project should be included in the API docs")
  val apiDocsIncludeManaged = SettingKey[Boolean](
    "apiDocsIncludeManaged",
    "Whether managed sources from this project should be included in the API docs"
  )
  val apiDocsScalaSources = TaskKey[Seq[File]]("apiDocsScalaSources", "All the scala sources for all projects")
  val apiDocsJavaSources  = TaskKey[Seq[File]]("apiDocsJavaSources", "All the Java sources for all projects")
  val apiDocsClasspath    = TaskKey[Seq[File]]("apiDocsClasspath", "The classpath for API docs generation")
  val apiDocsUseCache =
    SettingKey[Boolean]("apiDocsUseCache", "Whether to cache the doc inputs (can hit cache limit with dbuild)")
  val apiDocs        = TaskKey[File]("apiDocs", "Generate the API docs")
  val extractWebjars = TaskKey[File]("extractWebjars", "Extract webjar contents")
  val allConfs       = TaskKey[Seq[(String, File)]]("allConfs", "Gather all configuration files")

  lazy val settings = Seq(
    apiDocsInclude        := false,
    apiDocsIncludeManaged := false,
    apiDocsScalaSources := Def.taskDyn {
      val pr = thisProjectRef.value
      val bs = buildStructure.value
      Def.task(allSources(Compile, ".scala", pr, bs).value)
    }.value,
    apiDocsClasspath := Def.taskDyn {
      val pr = thisProjectRef.value
      val bs = buildStructure.value
      Def.task(allClasspaths(pr, bs).value)
    }.value,
    apiDocsJavaSources := Def.taskDyn {
      val pr = thisProjectRef.value
      val bs = buildStructure.value
      Def.task(allSources(Compile, ".java", pr, bs).value)
    }.value,
    (Global / allConfs) := Def.taskDyn {
      val pr = thisProjectRef.value
      val bs = buildStructure.value
      Def.task(allConfsTask(pr, bs).value)
    }.value,
    apiDocsUseCache := true,
    apiDocs         := apiDocsTask.value,
    ivyConfigurations += Webjars,
    extractWebjars := extractWebjarContents.value,
    (Compile / packageBin / mappings) ++= {
      val apiBase = apiDocs.value
      val webjars = extractWebjars.value
      // Include documentation and API docs in main binary JAR
      val docBase     = baseDirectory.value / "../../documentation"
      val raw         = (docBase \ "manual" ** "*") +++ (docBase \ "style" ** "*")
      val filtered    = raw.filter(_.getName != ".DS_Store")
      val docMappings = filtered.get.pair(rebase(docBase, "play/docs/content/"))

      val apiDocMappings = (apiBase ** "*").get.pair(rebase(apiBase, "play/docs/content/api"))

      // The play version is added so that resource paths are versioned
      val webjarMappings = webjars.allPaths.pair(rebase(webjars, "play/docs/content/webjars/" + version.value))

      // Gather all the conf files into the project
      val referenceConfMappings = allConfs.value.map {
        case (projectName, conf) => conf -> s"play/docs/content/confs/$projectName/${conf.getName}"
      }

      docMappings ++ apiDocMappings ++ webjarMappings ++ referenceConfMappings
    }
  )

  def playdocSettings: Seq[Setting[_]] =
    Playdoc.projectSettings ++
      Seq(
        ivyConfigurations += Webjars,
        extractWebjars := extractWebjarContents.value,
        libraryDependencies ++= Dependencies.playdocWebjarDependencies,
        (playdocPackage / mappings) := {
          val base        = ((ThisBuild / baseDirectory)).value
          val docBase     = base / "documentation"
          val raw         = (docBase / "manual").allPaths +++ (docBase / "style").allPaths
          val filtered    = raw.filter(_.getName != ".DS_Store")
          val docMappings = filtered.get.pair(relativeTo(docBase))

          // The play version is added so that resource paths are versioned
          val webjars        = extractWebjars.value
          val playVersion    = version.value
          val webjarMappings = webjars.allPaths.pair(rebase(webjars, "webjars/" + playVersion))

          // Gather all the conf files into the project
          val referenceConfs = allConfs.value.map {
            case (projectName, conf) => conf -> s"confs/$projectName/${conf.getName}"
          }

          docMappings ++ webjarMappings ++ referenceConfs
        }
      )

  // This is a specialized task that does not replace "sbt doc" but packages all the doc at once.
  def apiDocsTask = Def.taskDyn {
    val apiDocsDir = Docs.apiDocsDir.value
    if (((packageDoc / publishArtifact)).value) {
      genApiScaladocs.value
      genApiJavadocs.value
    }
    fixJavadocLinks(apiDocsDir)
    Def.task(apiDocsDir)
  }

  val apiDocsDir                 = Def.setting(crossTarget.value / "apidocs")
  def apiDocsCache(name: String) = Def.setting(CacheStoreFactory(crossTarget.value / name))

  def genApiScaladocs = Def.task {
    val version = Keys.version.value
    val label   = s"Play $version"

    val commitish = if (version.endsWith("-SNAPSHOT")) BuildSettings.snapshotBranch else version
    val externalDoc =
      Opts.doc.externalAPI(apiMappings.value).head.replace("-doc-external-doc:", "") // from the "doc" task

    val options = Seq(
      // Note, this is used by the doc-source-url feature to determine the relative path of a given source file.
      // If it's not a prefix of a the absolute path of the source file, the absolute path of that file will be put
      // into the FILE_SOURCE variable below, which is definitely not what we want.
      // Hence it needs to be the base directory for the build, not the base directory for the play-docs project.
      "-sourcepath",
      ((ThisBuild / baseDirectory)).value.getAbsolutePath,
      "-doc-source-url",
      s"https://github.com/playframework/playframework/tree/${commitish}€{FILE_PATH}.scala",
      s"-doc-external-doc:$externalDoc"
    )

    val useCache  = apiDocsUseCache.value
    val classpath = apiDocsClasspath.value.toList
    val sources   = apiDocsScalaSources.value
    val outputDir = apiDocsDir.value / "scala"
    val cache     = apiDocsCache("scalaapidocs.cache").value
    val streams   = Keys.streams.value
    val compilers = Keys.compilers.value
    val scalac    = compilers.scalac().asInstanceOf[AnalyzingCompiler]

    val scaladoc = {
      if (useCache) Doc.scaladoc(label, cache, scalac)
      else DocNoCache.scaladoc(label, scalac)
    }

    scaladoc(sources, classpath, outputDir, options, 10, streams.log)
  }

  def genApiJavadocs = Def.task {
    val label = s"Play ${version.value}"

    val options = List(
      "-windowtitle",
      label,
      // Adding a user agent when we run `javadoc` is necessary to create link docs
      // with Akka (at least, maybe play too) because doc.akka.io is served by Cloudflare
      // which blocks requests without a User-Agent header.
      "-J-Dhttp.agent=Play-Unidoc-Javadoc",
      "-link",
      "https://docs.oracle.com/javase/8/docs/api/",
      "-link",
      "https://doc.akka.io/japi/akka/2.6/",
      "-link",
      "https://doc.akka.io/japi/akka-http/current/",
      "-notimestamp",
      "-Xmaxwarns",
      "1000",
      "-exclude",
      "play.api:play.core",
      "-source",
      "8",
    )

    val useCache  = apiDocsUseCache.value
    val classpath = apiDocsClasspath.value.toList
    val sources   = apiDocsJavaSources.value.toList
    val outputDir = apiDocsDir.value / "java"
    val cache     = apiDocsCache("javaapidocs.cache").value
    val compilers = Keys.compilers.value
    val streams   = Keys.streams.value

    val javadoc = {
      if (useCache) sbt.inc.Doc.cachedJavadoc(label, cache, compilers.javaTools())
      else DocNoCache.javadoc(label, compilers, 10, streams.log)
    }

    val incToolOpt = IncToolOptions.create(java.util.Optional.empty[ClassFileManager](), false)
    val reporter   = new LoggedReporter(10, streams.log)

    javadoc.run(sources, classpath, outputDir, options, incToolOpt, streams.log, reporter)
  }

  def fixJavadocLinks(apiTarget: File) = {
    val externalJavadocLinks = {
      // Known Java libraries in non-standard locations...
      // All the external Javadoc URLs that must be fixed.
      val nonStandardJavadocLinks = Set(javaApiUrl, javaxInjectUrl, ehCacheUrl, guiceUrl)

      import Dependencies._
      val standardJavadocModuleIDs = Set(playJson) ++ slf4j

      nonStandardJavadocLinks ++ standardJavadocModuleIDs.map(moduleIDToJavadoc)
    }

    import scala.util.matching.Regex
    import scala.util.matching.Regex.Match

    def javadocLinkRegex(javadocURL: String): Regex = ("""\"(\Q""" + javadocURL + """\E)#([^"]*)\"""").r

    def hasJavadocLink(f: File): Boolean =
      externalJavadocLinks.exists { javadocURL: String =>
        javadocLinkRegex(javadocURL).findFirstIn(IO.read(f)).nonEmpty
      }

    val fixJavaLinks: Match => String = m => m.group(1) + "?" + m.group(2).replace(".", "/") + ".html"

    // Maps to Javadoc references in Scaladoc, and fixes the link so that it uses query parameters in
    // Javadoc style to link directly to the referenced class.
    // http://stackoverflow.com/questions/16934488/how-to-link-classes-from-jdk-into-scaladoc-generated-doc/
    (apiTarget ** "*.html").get.filter(hasJavadocLink).foreach { f =>
      val newContent: String = externalJavadocLinks.foldLeft(IO.read(f)) {
        case (oldContent: String, javadocURL: String) =>
          javadocLinkRegex(javadocURL).replaceAllIn(oldContent, fixJavaLinks)
      }
      IO.write(f, newContent)
    }
  }

  // Converts an artifact into a Javadoc URL.
  def artifactToJavadoc(organization: String, name: String, apiVersion: String, jarBaseFile: String): String = {
    val slashedOrg = organization.replace('.', '/')
    raw"""https://oss.sonatype.org/service/local/repositories/public/archive/$slashedOrg/$name/$apiVersion/$jarBaseFile-javadoc.jar/!/index.html"""
  }

  // Converts an sbt module into a Javadoc URL.
  def moduleIDToJavadoc(id: ModuleID): String = {
    artifactToJavadoc(id.organization, id.name, id.revision, s"${id.name}-${id.revision}")
  }

  val javaApiUrl     = "http://docs.oracle.com/javase/8/docs/api/index.html"
  val javaxInjectUrl = "https://javax-inject.github.io/javax-inject/api/index.html"
  // ehcache has 2.6.11 as version, but latest is only 2.6.9 on the site!
  val ehCacheUrl = raw"http://www.ehcache.org/apidocs/2.6.9/index.html"
  // nonstandard guice location
  val guiceUrl = raw"http://google.github.io/guice/api-docs/${Dependencies.guiceVersion}/javadoc/index.html"

  def allConfsTask(projectRef: ProjectRef, structure: BuildStructure): Task[Seq[(String, File)]] = {
    val projects = allApiProjects(projectRef.build, structure)
    val unmanagedResourcesTasks = projects.map { ref =>
      def taskFromProject[T](task: TaskKey[T]) = ((ref / task in Compile)(Compile / task)).get(structure.data)

      val projectId = ((ref / moduleName)).get(structure.data)

      val confs = ((ref / unmanagedResources in Compile)(Compile / unmanagedResources))
        .get(structure.data)
        .map(_.map { resources =>
          (for {
            conf <- resources.filter(resource =>
              resource.name == "reference.conf" || resource.name.endsWith(".xml") || resource.name
                .endsWith(".default")
            )
            id <- projectId.toSeq
          } yield id -> conf).distinct
        })

      // Join them
      val tasks = confs.toSeq
      tasks.join.map(_.flatten)
    }
    unmanagedResourcesTasks.join.map(_.flatten)
  }

  def allSources(
      conf: Configuration,
      extension: String,
      projectRef: ProjectRef,
      structure: BuildStructure
  ): Task[Seq[File]] = {
    val projects = allApiProjects(projectRef.build, structure)
    val sourceTasks = projects.map { ref =>
      def taskFromProject[T](task: TaskKey[T]) = ((ref / task in conf)(conf / task)).get(structure.data)

      // Get all the Scala sources (not the Java ones)
      val filteredSources = taskFromProject(sources).map(_.map(_.filter(_.name.endsWith(extension))))

      // Join them
      val tasks = filteredSources.toSeq
      tasks.join.map(_.flatten)
    }
    sourceTasks.join.map(_.flatten)
  }

  /**
   * Get all projects in the given build that have `apiDocsInclude` set to `true`.
   * Recursively searches aggregated projects starting from the root project.
   */
  def allApiProjects(build: URI, structure: BuildStructure): Seq[ProjectRef] = {
    def aggregated(projectRef: ProjectRef): Seq[ProjectRef] = {
      val project   = Project.getProject(projectRef, structure)
      val childRefs = project.toSeq.flatMap(_.aggregate)
      childRefs.flatMap { childRef =>
        val includeApiDocs = ((childRef / apiDocsInclude)).get(structure.data).getOrElse(false)
        if (includeApiDocs) childRef +: aggregated(childRef) else aggregated(childRef)
      }
    }
    val rootProjectId  = structure.rootProject(build)
    val rootProjectRef = ProjectRef(build, rootProjectId)
    aggregated(rootProjectRef)
  }

  def allClasspaths(projectRef: ProjectRef, structure: BuildStructure): Task[Seq[File]] = {
    val projects = allApiProjects(projectRef.build, structure)
    // Full classpath is necessary to ensure that scaladoc and javadoc can see the compiled classes of the other language.
    val tasks = projects.flatMap { p => ((p / fullClasspath in Compile)(Compile / fullClasspath)).get(structure.data) }
    tasks.join.map(_.flatten.map(_.data).distinct)
  }

  // Note: webjars are extracted without versions
  def extractWebjarContents: Def.Initialize[Task[File]] = Def.task {
    val report    = update.value
    val targetDir = target.value
    val s         = streams.value

    val webjars     = report.matching(configurationFilter(name = Webjars.name))
    val webjarsDir  = targetDir / "webjars"
    val classLoader = new URLClassLoader(Path.toURLs(webjars), null)
    val extractor   = new WebJarExtractor(classLoader)
    extractor.extractAllWebJarsTo(webjarsDir)
    webjarsDir
  }

  // Generate documentation but avoid caching the inputs because of https://github.com/sbt/sbt/issues/1614
  object DocNoCache {
    type GenerateDoc = RawCompileLike.Gen

    def scaladoc(label: String, compile: sbt.internal.inc.AnalyzingCompiler): GenerateDoc =
      RawCompileLike.prepare(s"$label Scala API documentation", compile.doc)

    def javadoc(
        label: String,
        compiler: Compilers,
        maxRetry: Int,
        managedLogger: ManagedLogger
    ): JavaDoc = new JavaDoc {
      def run(
          sources: List[File],
          classpath: List[File],
          outputDirectory: File,
          options: List[String],
          incToolOptions: IncToolOptions,
          log: Logger,
          reporter: Reporter
      ): Unit = {
        val impl = RawCompileLike.prepare(
          s"$label Java API documentation",
          RawCompileLike.filterSources(
            _.getName.endsWith(".java"),
            (srcs, _, _, opts, _, log) => {
              compiler.javaTools().javadoc.run(srcs.toArray, opts.toArray, incToolOptions, reporter, log)
            }
          )
        )
        impl(sources, classpath, outputDirectory, options, maxRetry, managedLogger)
      }
    }
  }
}
