import sbt._
import Keys._
import play.Project._
import sbt.Artifact

object DistTest {
  private val MUST_DIST_ARTIFACT_NAME = "mustDist"
  private val MUST_NOT_DIST_ARTIFACT_NAME = "mustNotDist"

  private val MUST_DIST_FILENAME = "mustDistThisArtifact.jar"
  private val MUST_NOT_DIST_FILENAME = "mustNotDistThisArtifact.jar"

  private def testDistCmd(appName: String, appVersion: String) = {
    val help = "Run the dist task and validate the output"
    Command.command("test-dist", help, help) { (s0: State) =>
      Project.runTask(dist, s0) match {
        case Some((s1, Value(distFile))) =>
          IO.withTemporaryDirectory { tmpUnzipDir =>
            validateDistFiles(appName, appVersion, IO.unzip(distFile, tmpUnzipDir))
            s1
          }
        case r @ _ => throw new RuntimeException("Unexpected result from dist task: " + r)
      }
    }
  }

  private val distTestArtifactsSetting =
    packagedArtifacts <<= (packagedArtifacts, target) map {
      (artifacts, targetDir) =>
          val mustDistFile = targetDir / MUST_DIST_FILENAME
          val mustNotDistFile = targetDir / MUST_NOT_DIST_FILENAME

          IO.writeLines(mustDistFile, Seq("must dist this artifact"))
          IO.writeLines(mustNotDistFile, Seq("must not dist this artifact"))

          def artifact(name: String) = new Artifact(name, "jar", "jar", None,
                                                    Seq.empty, None, Map.empty)

          val mustDistArtifact = artifact(MUST_DIST_ARTIFACT_NAME)
          val mustNotDistArtifact = artifact(MUST_NOT_DIST_ARTIFACT_NAME)

          artifacts ++ Seq((mustDistArtifact, mustDistFile),
                           (mustNotDistArtifact, mustNotDistFile))
    }

  private def validateDistFiles(appName: String, appVersion: String, files: Set[File]) {

    def matches(file: File, pat: String) = file.getAbsolutePath().matches(pat)

    def contains(pat: String) = files.exists(matches(_, pat))

    def mustContain(pat: String) =
      if (!contains(pat))
        throw new RuntimeException("dist-test failed!  No dist file matched the required pattern " + pat +
                                   "\nDist files were:\n" + files)

    def mustNotContain(pat: String) =
      if (contains(pat))
        throw new RuntimeException("dist-test failed!  A dist file matched the forbidden pattern " + pat +
                                   "\nDist files were:\n" + files)

    val root = ".*/%s-%s" format (appName, appVersion)
    val lib = root + "/lib/"
    val scalaVersionPattern = "[0-9]+[.][0-9]+"

    mustContain(lib + "%s_%s-%s.jar" format (appName, scalaVersionPattern, appVersion))
    mustContain(lib + MUST_DIST_FILENAME)
    mustNotContain(lib + MUST_NOT_DIST_FILENAME)
  }

  def makeSettings(appName: String, appVersion: String) =
    Seq(distTestArtifactsSetting,
        distExcludes := Seq(MUST_NOT_DIST_ARTIFACT_NAME),
        commands += testDistCmd(appName, appVersion))

}
