import sbt._
import sbt.Keys._
import sbt.File

object Docs {

  val apiDocsInclude = SettingKey[Boolean]("api-docs-include", "Whether this sub project should be included in the API docs")
  val apiDocsIncludeManaged = SettingKey[Boolean]("api-docs-include-managed", "Whether managed sources from this project should be included in the API docs")
  val apiDocsScalaSources = TaskKey[Seq[File]]("api-docs-scala-sources", "All the scala sources for all projects")
  val apiDocsJavaSourceDirectories = TaskKey[Seq[File]]("api-docs-java-source-directories", "All the Java source directories for all projects")
  val apiDocsClasspath = TaskKey[Seq[File]]("api-docs-classpath", "The classpath for API docs generation")
  val apiDocs = TaskKey[File]("api-docs", "Generate the API docs")

  lazy val settings = Seq(
    apiDocsInclude := false,
    apiDocsIncludeManaged := false,
    apiDocsScalaSources <<= (thisProjectRef, buildStructure) flatMap allScalaSources(Compile),
    apiDocsClasspath <<= (thisProjectRef, buildStructure) flatMap allClasspaths,
    apiDocsJavaSourceDirectories <<= (thisProjectRef, buildStructure) map allJavaSourceDirectories(Compile),
    apiDocs <<= (apiDocsScalaSources, apiDocsJavaSourceDirectories, apiDocsClasspath, baseDirectory in ThisBuild, baseDirectory, compilers, streams) map apiDocsTask,
    mappings in (Compile, packageBin) <++= (baseDirectory, apiDocs) map { (base, apiBase) =>
      // Include documentation and API docs in main binary JAR
      val docBase = base / "../../../documentation"
      val raw = (docBase \ "manual" ** "*") +++ (docBase \ "style" ** "*")
      val filtered = raw.filter(_.getName != ".DS_Store")
      val docMappings = filtered.get x rebase(docBase, "play/docs/content/")

      val apiDocMappings = (apiBase ** "*").get x rebase(apiBase, "play/docs/content/api")

      docMappings ++ apiDocMappings
    }
  )

  def apiDocsTask(scalaSources: Seq[File], javaSourceDirectories: Seq[File], classpath: Seq[File], buildBase: File, projectBase: File, compilers: Compiler.Compilers, streams: TaskStreams): File = {

    val version = BuildSettings.buildVersion
    val sourceTree = if (version.endsWith("-SNAPSHOT")) {
      BuildSettings.sourceCodeBranch
    } else {
      version
    }

    val apiTarget = new File(projectBase, "target/apidocs")

    val options = Seq(
      // Note, this is used by the doc-source-url feature to determine the relative path of a given source file.
      // If it's not a prefix of a the absolute path of the source file, the absolute path of that file will be put
      // into the FILE_SOURCE variable below, which is definitely not what we want.
      // Hence it needs to be the base directory for the build, not the base directory for the play-docs project.
      "-sourcepath", buildBase.getAbsolutePath,
      "-doc-source-url", "https://github.com/playframework/playframework/tree/" + sourceTree + "/framework€{FILE_PATH}.scala")

    // Update to SBT 0.13 notes - there will be a Doc.scaladoc method that allows you to pass a cache file, passing
    // that cache file may greatly speed up our builds.
    new Scaladoc(10, compilers.scalac)("Play " + BuildSettings.buildVersion + " Scala API", scalaSources, classpath,
      apiTarget / "scala", options, streams.log)

    val javadocOptions = Seq(
      "-windowtitle", "playframework",
      "-doctitle", "Play&nbsp;" + BuildSettings.buildVersion + "&nbsp;Java&nbsp;API",
      "-notimestamp",
      "-sourcepath", javaSourceDirectories.mkString(":"),
      "-d", apiTarget / "java",
      "-subpackages", "play",
      "-exclude", "play.api:play.core",
      "-classpath", classpath.mkString(":")
    )
    "javadoc " + javadocOptions.mkString(" ") ! streams.log

    apiTarget
  }

  def allScalaSources(conf: Configuration)(projectRef: ProjectRef, structure: Load.BuildStructure): Task[Seq[File]] = {
    val projects = allApiProjects(projectRef.build, structure)
    val sourceTasks = projects map { ref =>
      def taskFromProject[T](task: TaskKey[T]) = task in conf in ref get structure.data

      // Get all the Scala sources (not the Java ones)
      val scalaSources = taskFromProject(sources).map(_.map(_.filter(_.name.endsWith(".scala"))))
      // Only include managed if told to do so
      val managedScalaSources = apiDocsIncludeManaged in ref get structure.data match {
        case Some(true) => taskFromProject(managedSources).map(_.map(_.filter(_.name.endsWith(".scala"))))
        case _ => None
      }

      // Join them
      val tasks = scalaSources.toSeq ++ managedScalaSources
      tasks.join.map(_.flatten)
    }
    sourceTasks.join.map(_.flatten)
  }

  def allJavaSourceDirectories(conf: Configuration)(projectRef: ProjectRef, structure: Load.BuildStructure): Seq[File] = {
    val projects = allApiProjects(projectRef.build, structure)
    projects flatMap { ref =>
      javaSource in conf in ref get structure.data
    }
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
}