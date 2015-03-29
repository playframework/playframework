/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
import sbt._
import sbt.Keys._
import sbt.File
import java.net.URLClassLoader
import org.webjars.{ FileSystemCache, WebJarExtractor }
import interplay.Playdoc
import interplay.Playdoc.Import.PlaydocKeys

object Docs {

  val Webjars = config("webjars").hide

  val apiDocsInclude = SettingKey[Boolean]("api-docs-include", "Whether this sub project should be included in the API docs")
  val apiDocsIncludeManaged = SettingKey[Boolean]("api-docs-include-managed", "Whether managed sources from this project should be included in the API docs")
  val apiDocsScalaSources = TaskKey[Seq[File]]("api-docs-scala-sources", "All the scala sources for all projects")
  val apiDocsJavaSources = TaskKey[Seq[File]]("api-docs-java-sources", "All the Java sources for all projects")
  val apiDocsClasspath = TaskKey[Seq[File]]("api-docs-classpath", "The classpath for API docs generation")
  val apiDocsUseCache = SettingKey[Boolean]("api-docs-use-cache", "Whether to cache the doc inputs (can hit cache limit with dbuild)")
  val apiDocs = TaskKey[File]("api-docs", "Generate the API docs")
  val extractWebjars = TaskKey[File]("extract-webjars", "Extract webjar contents")
  val allReferenceConfs = TaskKey[Seq[(String, File)]]("all-reference-confs", "Gather all reference confs")

  lazy val settings = Seq(
    apiDocsInclude := false,
    apiDocsIncludeManaged := false,
    apiDocsScalaSources <<= (thisProjectRef, buildStructure) flatMap allSources(Compile, ".scala"),
    apiDocsClasspath <<= (thisProjectRef, buildStructure) flatMap allClasspaths,
    apiDocsJavaSources <<= (thisProjectRef, buildStructure) flatMap allSources(Compile, ".java"),
    apiDocsUseCache := true,
    apiDocs <<= apiDocsTask,
    ivyConfigurations += Webjars,
    extractWebjars <<= extractWebjarContents,
    allReferenceConfs in Global <<= (thisProjectRef, buildStructure) flatMap allReferenceConfsTask,
    mappings in (Compile, packageBin) <++= (baseDirectory, apiDocs, extractWebjars, version, allReferenceConfs) map { (base, apiBase, webjars, playVersion, referenceConfs) =>
      // Include documentation and API docs in main binary JAR
      val docBase = base / "../../../documentation"
      val raw = (docBase \ "manual" ** "*") +++ (docBase \ "style" ** "*")
      val filtered = raw.filter(_.getName != ".DS_Store")
      val docMappings = filtered.get pair rebase(docBase, "play/docs/content/")

      val apiDocMappings = (apiBase ** "*").get pair rebase(apiBase, "play/docs/content/api")

      // The play version is added so that resource paths are versioned
      val webjarMappings = webjars.*** pair rebase(webjars, "play/docs/content/webjars/" + playVersion)

      // Gather all the reference.conf files into the project
      val referenceConfMappings = referenceConfs.map {
        case (projectName, referenceConf) => referenceConf -> s"play/docs/content/confs/$projectName/reference.conf"
      }

      docMappings ++ apiDocMappings ++ webjarMappings ++ referenceConfMappings
    }
  )

  def playdocSettings: Seq[Setting[_]] = Playdoc.projectSettings ++
    Seq(
      ivyConfigurations += Webjars,
      extractWebjars <<= extractWebjarContents,
      libraryDependencies ++= Dependencies.playdocWebjarDependencies,
      mappings in PlaydocKeys.packagePlaydoc := {
        val base = (baseDirectory in ThisBuild).value
        val docBase = base.getParentFile / "documentation"
        val raw = (docBase / "manual").*** +++ (docBase / "style").***
        val filtered = raw.filter(_.getName != ".DS_Store")
        val docMappings = filtered.get pair relativeTo(docBase)

        // The play version is added so that resource paths are versioned
        val webjars = extractWebjars.value
        val playVersion = version.value
        val webjarMappings = webjars.*** pair rebase(webjars, "webjars/" + playVersion)

        // Gather all the reference.conf files into the project
        val referenceConfs = allReferenceConfs.value.map {
          case (projectName, referenceConf) => referenceConf -> s"confs/$projectName/reference.conf"
        }

        docMappings ++ webjarMappings ++ referenceConfs
      }
    )

  def apiDocsTask = Def.task {

    val targetDir = new File(target.value, "scala-" + CrossVersion.binaryScalaVersion(scalaVersion.value))
    val apiTarget = new File(targetDir, "apidocs")

    if ((publishArtifact in packageDoc).value) {

      val version = Keys.version.value
      val sourceTree = if (version.endsWith("-SNAPSHOT")) {
        BuildSettings.snapshotBranch
      } else {
        version
      }

      val scalaCache = new File(targetDir, "scalaapidocs.cache")
      val javaCache = new File(targetDir, "javaapidocs.cache")

      val label = "Play " + version

      val options = Seq(
        // Note, this is used by the doc-source-url feature to determine the relative path of a given source file.
        // If it's not a prefix of a the absolute path of the source file, the absolute path of that file will be put
        // into the FILE_SOURCE variable below, which is definitely not what we want.
        // Hence it needs to be the base directory for the build, not the base directory for the play-docs project.
        "-sourcepath", (baseDirectory in ThisBuild).value.getAbsolutePath,
        "-doc-source-url", "https://github.com/playframework/playframework/tree/" + sourceTree + "/framework€{FILE_PATH}.scala")

      val compilers = Keys.compilers.value
      val useCache = apiDocsUseCache.value
      val classpath = apiDocsClasspath.value

      val scaladoc = {
        if (useCache) Doc.scaladoc(label, scalaCache, compilers.scalac)
        else DocNoCache.scaladoc(label, compilers.scalac)
      }
      // Since there is absolutely no documentation on what the arguments here should be aside from their types, here
      // are the parameter names of the method that does eventually get called:
      // (sources, classpath, outputDirectory, options, maxErrors, log)
      scaladoc(apiDocsScalaSources.value, classpath, apiTarget / "scala", options, 10, streams.value.log)

      val javadocOptions = Seq(
        "-windowtitle", label,
        "-notimestamp",
        "-subpackages", "play",
        "-exclude", "play.api:play.core"
      )

      val javadoc = {
        if (useCache) Doc.javadoc(label, javaCache, compilers.javac)
        else DocNoCache.javadoc(label, compilers.javac)
      }
      javadoc(apiDocsJavaSources.value, classpath, apiTarget / "java", javadocOptions, 10, streams.value.log)
    }

    apiTarget
  }

  def allReferenceConfsTask(projectRef: ProjectRef, structure: BuildStructure): Task[Seq[(String, File)]] = {
    val projects = allApiProjects(projectRef.build, structure)
    val unmanagedResourcesTasks = projects map { ref =>
      def taskFromProject[T](task: TaskKey[T]) = task in Compile in ref get structure.data

      val projectId = moduleName in ref get structure.data

      val referenceConfs = (unmanagedResources in Compile in ref get structure.data).map(_.map { resources =>
        (for {
          referenceConf <- resources.find(_.name == "reference.conf")
          id <- projectId
        } yield id -> referenceConf).toSeq
      })

      // Join them
      val tasks = referenceConfs.toSeq
      tasks.join.map(_.flatten)
    }
    unmanagedResourcesTasks.join.map(_.flatten)
  }

  def allSources(conf: Configuration, extension: String)(projectRef: ProjectRef, structure: BuildStructure): Task[Seq[File]] = {
    val projects = allApiProjects(projectRef.build, structure)
    val sourceTasks = projects map { ref =>
      def taskFromProject[T](task: TaskKey[T]) = task in conf in ref get structure.data

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
      val project = Project.getProject(projectRef, structure)
      val childRefs = project.toSeq.flatMap(_.aggregate)
      childRefs flatMap { childRef =>
        val includeApiDocs = (apiDocsInclude in childRef).get(structure.data).getOrElse(false)
        if (includeApiDocs) childRef +: aggregated(childRef) else aggregated(childRef)
      }
    }
    val rootProjectId = Load.getRootProject(structure.units)(build)
    val rootProjectRef = ProjectRef(build, rootProjectId)
    aggregated(rootProjectRef)
  }

  def allClasspaths(projectRef: ProjectRef, structure: BuildStructure): Task[Seq[File]] = {
    val projects = allApiProjects(projectRef.build, structure)
    // Full classpath is necessary to ensure that scaladoc and javadoc can see the compiled classes of the other language.
    val tasks = projects flatMap { fullClasspath in Compile in _ get structure.data }
    tasks.join.map(_.flatten.map(_.data).distinct)
  }

  // Note: webjars are extracted without versions
  def extractWebjarContents: Def.Initialize[Task[File]] = (update, target, streams) map { (report, targetDir, s) =>
    val webjars = report.matching(configurationFilter(name = Webjars.name))
    val webjarsDir = targetDir / "webjars"
    val cache = new FileSystemCache(s.cacheDirectory / "webjars-cache")
    val classLoader = new URLClassLoader(Path.toURLs(webjars), null)
    val extractor = new WebJarExtractor(cache, classLoader)
    extractor.extractAllWebJarsTo(webjarsDir)
    cache.save()
    webjarsDir
  }

  // Generate documentation but avoid caching the inputs because of https://github.com/sbt/sbt/issues/1614
  object DocNoCache {
    type GenerateDoc = (Seq[File], Seq[File], File, Seq[String], Int, Logger) => Unit

    def scaladoc(label: String, compile: compiler.AnalyzingCompiler): GenerateDoc =
      RawCompileLike.prepare(label + " Scala API documentation", compile.doc)

    def javadoc(label: String, compile: compiler.Javadoc): GenerateDoc =
      RawCompileLike.prepare(label + " Java API documentation", RawCompileLike.filterSources(Doc.javaSourcesOnly, compile.doc))
  }
}
