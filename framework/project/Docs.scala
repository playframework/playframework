/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
import sbt._
import sbt.Keys._
import sbt.File
import java.net.URLClassLoader
import org.webjars.{ FileSystemCache, WebJarExtractor }

object Docs {

  val Webjars = config("webjars").hide

  val apiDocsInclude = SettingKey[Boolean]("api-docs-include", "Whether this sub project should be included in the API docs")
  val apiDocsIncludeManaged = SettingKey[Boolean]("api-docs-include-managed", "Whether managed sources from this project should be included in the API docs")
  val apiDocsScalaSources = TaskKey[Seq[File]]("api-docs-scala-sources", "All the scala sources for all projects")
  val apiDocsJavaSources = TaskKey[Seq[File]]("api-docs-java-sources", "All the Java sources for all projects")
  val apiDocsClasspath = TaskKey[Seq[File]]("api-docs-classpath", "The classpath for API docs generation")
  val apiDocs = TaskKey[File]("api-docs", "Generate the API docs")
  val extractWebjars = TaskKey[File]("extract-webjars", "Extract webjar contents")

  lazy val settings = Seq(
    apiDocsInclude := false,
    apiDocsIncludeManaged := false,
    apiDocsScalaSources <<= (thisProjectRef, buildStructure) flatMap allSources(Compile, ".scala"),
    apiDocsClasspath <<= (thisProjectRef, buildStructure) flatMap allClasspaths,
    apiDocsJavaSources <<= (thisProjectRef, buildStructure) flatMap allSources(Compile, ".java"),
    apiDocs <<= (apiDocsScalaSources, apiDocsJavaSources, apiDocsClasspath, baseDirectory in ThisBuild, baseDirectory, compilers, streams) map apiDocsTask,
    ivyConfigurations += Webjars,
    extractWebjars <<= extractWebjarContents,
    mappings in (Compile, packageBin) <++= (baseDirectory, apiDocs, extractWebjars, version) map { (base, apiBase, webjars, playVersion) =>
      // Include documentation and API docs in main binary JAR
      val docBase = base / "../../../documentation"
      val raw = (docBase \ "manual" ** "*") +++ (docBase \ "style" ** "*")
      val filtered = raw.filter(_.getName != ".DS_Store")
      val docMappings = filtered.get x rebase(docBase, "play/docs/content/")

      val apiDocMappings = (apiBase ** "*").get x rebase(apiBase, "play/docs/content/api")

      // The play version is added so that resource paths are versioned
      val webjarMappings = webjars.*** pair rebase(webjars, "play/docs/content/webjars/" + playVersion)

      docMappings ++ apiDocMappings ++ webjarMappings
    }
  )

  def apiDocsTask(scalaSources: Seq[File], javaSources: Seq[File], classpath: Seq[File], buildBase: File, projectBase: File, compilers: Compiler.Compilers, streams: TaskStreams): File = {

    val version = BuildSettings.buildVersion
    val sourceTree = if (version.endsWith("-SNAPSHOT")) {
      BuildSettings.sourceCodeBranch
    } else {
      version
    }

    val apiTarget = new File(projectBase, "target/apidocs")
    val scalaCache = new File(projectBase, "target/scalaapidocs.cache")
    val javaCache = new File(projectBase, "target/javaapidocs.cache")

    val label = "Play " + BuildSettings.buildVersion

    val options = Seq(
      // Note, this is used by the doc-source-url feature to determine the relative path of a given source file.
      // If it's not a prefix of a the absolute path of the source file, the absolute path of that file will be put
      // into the FILE_SOURCE variable below, which is definitely not what we want.
      // Hence it needs to be the base directory for the build, not the base directory for the play-docs project.
      "-sourcepath", buildBase.getAbsolutePath,
      "-doc-source-url", "https://github.com/playframework/playframework/tree/" + sourceTree + "/framework€{FILE_PATH}.scala")

    val scaladoc = Doc.scaladoc(label, scalaCache, compilers.scalac)
    // Since there is absolutely no documentation on what the arguments here should be aside from their types, here
    // are the parameter names of the method that does eventually get called:
    // (sources, classpath, outputDirectory, options, maxErrors, log)
    scaladoc(scalaSources, classpath, apiTarget / "scala", options, 10, streams.log)

    val javadocOptions = Seq(
      "-windowtitle", label,
      "-notimestamp",
      "-subpackages", "play",
      "-exclude", "play.api:play.core"
    )

    val javadoc = Doc.javadoc(label, javaCache, compilers.javac)
    javadoc(javaSources, classpath, apiTarget / "java", javadocOptions, 10, streams.log)

    apiTarget
  }

  def allSources(conf: Configuration, extension: String)(projectRef: ProjectRef, structure: Load.BuildStructure): Task[Seq[File]] = {
    val projects = allApiProjects(projectRef.build, structure)
    val sourceTasks = projects map { ref =>
      def taskFromProject[T](task: TaskKey[T]) = task in conf in ref get structure.data

      // Get all the Scala sources (not the Java ones)
      val filteredSources = taskFromProject(sources).map(_.map(_.filter(_.name.endsWith(extension))))
      // Only include managed if told to do so
      val managedFilteredSources = apiDocsIncludeManaged in ref get structure.data match {
        case Some(true) => taskFromProject(managedSources).map(_.map(_.filter(_.name.endsWith(extension))))
        case _ => None
      }

      // Join them
      val tasks = filteredSources.toSeq ++ managedFilteredSources
      tasks.join.map(_.flatten)
    }
    sourceTasks.join.map(_.flatten)
  }

  /**
   * Get all projects in the given build that have `apiDocsInclude` set to `true`.
   * Recursively searches aggregated projects starting from the root project.
   */
  def allApiProjects(build: URI, structure: Load.BuildStructure): Seq[ProjectRef] = {
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

  def allClasspaths(projectRef: ProjectRef, structure: Load.BuildStructure): Task[Seq[File]] = {
    val projects = allApiProjects(projectRef.build, structure)
    val tasks = projects flatMap { dependencyClasspath in Compile in _ get structure.data }
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
}
