import sbt._
import sbt.Keys._
import sbt.File

object ApiDocs {

  val apiDocsInclude = SettingKey[Boolean]("api-docs-include", "Whether this sub project should be included in the API docs")
  val apiDocsIncludeManaged = SettingKey[Boolean]("api-docs-include-managed", "Whether managed sources from this project should be included in the API docs")
  val apiDocsScalaSources = TaskKey[Seq[File]]("api-docs-scala-sources", "All the scala sources for all projects")
  val apiDocsJavaSourceDirectories = TaskKey[Seq[File]]("api-docs-java-source-directories", "All the Java source directories for all projects")
  val apiDocsClasspath = TaskKey[Seq[File]]("api-docs-classpath", "The classpath for API docs generation")
  val apiDocs = TaskKey[Unit]("api-docs", "Generate the API docs")

  lazy val settings = Seq(
    apiDocsInclude := false,
    apiDocsIncludeManaged := false,
    apiDocsScalaSources <<= (thisProjectRef, buildStructure) flatMap allScalaSources(Compile),
    apiDocsClasspath <<= (thisProjectRef, buildStructure) flatMap allClasspaths,
    apiDocsJavaSourceDirectories <<= (thisProjectRef, buildStructure) map allJavaSourceDirectories(Compile),
    apiDocs <<= (apiDocsScalaSources, apiDocsJavaSourceDirectories, apiDocsClasspath, baseDirectory, compilers, streams) map apiDocsTask
  )

  def apiDocsTask(scalaSources: Seq[File], javaSourceDirectories: Seq[File], classpath: Seq[File], base: File, compilers: Compiler.Compilers, streams: TaskStreams): Unit = {

    val version = BuildSettings.buildVersion
    val sourceTree = if (version.endsWith("-SNAPSHOT")) {
      BuildSettings.sourceCodeBranch
    } else {
      version
    }

    val scalaApiTarget = new File(base, "target/apidocs/scala")
    val options = Seq(
      "-sourcepath", base.getAbsolutePath,
      "-doc-source-url", "https://github.com/playframework/Play20/tree/" + sourceTree + "/framework€{FILE_PATH}.scala")
    new Scaladoc(10, compilers.scalac)("Play " + BuildSettings.buildVersion + " Scala API", scalaSources, classpath,
      scalaApiTarget, options, streams.log)

    val javadocOptions = Seq(
      "-windowtitle", "playframework",
      "-doctitle", "Play&nbsp;" + BuildSettings.buildVersion + "&nbsp;Java&nbsp;API",
      "-notimestamp",
      "-sourcepath", javaSourceDirectories.mkString(":"),
      "-d", new File(base, "target/apidocs/java").getPath,
      "-subpackages", "play",
      "-exclude", "play.api:play.core",
      "-classpath", classpath.mkString(":")
    )
    "javadoc " + javadocOptions.mkString(" ") ! streams.log
  }

  def allScalaSources(conf: Configuration)(projectRef: ProjectRef, structure: Load.BuildStructure): Task[Seq[File]] = {
    val projects = aggregated(projectRef, structure)
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
    val projects = aggregated(projectRef, structure)
    projects flatMap { ref =>
      javaSource in conf in ref get structure.data
    }
  }

  def aggregated(projectRef: ProjectRef, structure: Load.BuildStructure): Seq[ProjectRef] = {
    val aggregate = Project.getProject(projectRef, structure).toSeq.flatMap(_.aggregate)
    aggregate flatMap { ref =>
      if (apiDocsInclude in ref get structure.data getOrElse false) ref +: aggregated(ref, structure)
      else aggregated(ref, structure)
    }
  }

  def allClasspaths(projectRef: ProjectRef, structure: Load.BuildStructure): Task[Seq[File]] = {
    val projects = aggregated(projectRef, structure)
    val tasks = projects flatMap { dependencyClasspath in Compile in _ get structure.data }
    tasks.join.map(_.flatten.map(_.data).distinct)
  }
}