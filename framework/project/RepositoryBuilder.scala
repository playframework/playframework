import sbt._
import Keys._


// NOTE - PRONOUNCED REPOSITORY BOUGHDA
object RepositoryBuilder {
  val localRepoProjectsPublished = TaskKey[Unit]("local-repo-projects-published", "Ensures local projects are published before generating the local repo.")
  val localRepoArtifacts = SettingKey[Seq[ModuleID]]("local-repository-artifacts", "Artifacts included in the local repository.")
  val localRepoName = "install-to-local-repository"
  val localRepo = SettingKey[File]("local-repository", "The location to install a local repository.")
  val localRepoCreated = TaskKey[File]("local-repository-created", "Creates a local repository in the specified location.")
  
  
  
  def addProjectsToRepository(projects: Seq[ProjectReference]): Setting[_] =
    localRepoArtifacts <++= (projects map { ref =>
        // The annoyance caused by cross-versioning.
        (Keys.projectID in ref, Keys.ivyScala in ref) apply {
          (id, ivy) =>
            CrossVersion(id, ivy) match {
              case Some(f) =>
                id.copy(name = f(id.name), explicitArtifacts = id.explicitArtifacts map (a => a.copy(name = f(a.name))))
              case _ => id
            }
        }
      }).join
  
  def localRepoCreationSettings: Seq[Setting[_]] = Seq(
    Keys.fullResolvers <<= (Keys.externalResolvers, Keys.sbtResolver) map (_ :+ _),
    localRepo <<= target apply (_ / "local-repository"),
    localRepoArtifacts := Seq.empty,
    resolvers <+= localRepo apply { f => Resolver.file(localRepoName, f)(Resolver.ivyStylePatterns) },
    localRepoProjectsPublished := (),
    //localRepoProjectsPublished <<= (ThePlayBuild.publishedProjects map (publishLocal in _)).dependOn,
    localRepoCreated <<= (localRepo, localRepoArtifacts, ivySbt, streams, localRepoProjectsPublished) map { (r, m, i, s, _) =>
      // TODO - Hook to detect if we need to recreate the repository....
      // That way we don't have to clean all the time.
      createLocalRepository(m, localRepoName, i, s.log)
      r
    }
  )
  
  
  /** Resolves a set of modules from an SBT configured ivy and pushes them into
   * the given repository (by name).
   * 
   * Intended usage, requires the named resovle to exist, and be on that accepts installed artifacts (i.e. file://)
   */
  def createLocalRepository(
      modules: Seq[ModuleID],
      localRepoName: String,
      ivy: IvySbt, 
      log: Logger): Unit = ivy.withIvy(log) { ivy =>

    import org.apache.ivy.core.report.ResolveReport
    import org.apache.ivy.core.install.InstallOptions
    import org.apache.ivy.plugins.matcher.PatternMatcher
    import org.apache.ivy.util.filter.FilterHelper


    // This helper method installs a particular module and transitive dependencies.
    def installModule(module: ModuleID): Option[ResolveReport] = {
      // TODO - Use SBT's default ModuleID -> ModuleRevisionId
      val mrid = IvySbtCheater toID module
      val name = ivy.getResolveEngine.getSettings.getResolverName(mrid)
      log.debug("Module: " + mrid + " should use resolver: " + name)
      try Some(ivy.install(mrid, name, localRepoName,
                new InstallOptions()
                    .setTransitive(true)
                    .setValidate(true)
                    .setOverwrite(true)
                    .setMatcherName(PatternMatcher.EXACT)
                    .setArtifactFilter(FilterHelper.NO_FILTER)
                ))
       catch {
         case e: Exception =>
           log.debug("Failed to resolve module: " + module)
           log.trace(e)
           None
       }
    }
    // Grab all Artifacts
    modules foreach installModule     
  }
}

// THIS PIECE OF HACKERY IS AMAZING.
// We need access to "toID" which is private[sbt].  If SBT changes, this may need to change, but
// it has been stable for a while.
package sbt {
  object IvySbtCheater {
    // Converts from SBT module id to Ivy's version of a module ID so we can use the Ivy library directly.
    def toID(m: ModuleID) = IvySbt toID m
  }
}