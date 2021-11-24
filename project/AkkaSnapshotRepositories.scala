import sbt.Keys._
import sbt._

/**
 * This plugins adds Akka snapshot repositories when running a nightly build.
 */
object AkkaSnapshotRepositories extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements

  // This is also copy/pasted in ScriptedTools for scripted tests to also use the snapshot repositories.
  override def projectSettings: Seq[Def.Setting[_]] = {
    // If this is a cron job in Travis:
    // https://docs.travis-ci.com/user/cron-jobs/#detecting-builds-triggered-by-cron
    resolvers ++= sys.env
      .get("TRAVIS_EVENT_TYPE")
      .filter(_.equalsIgnoreCase("cron"))
      .map(_ => Resolver.sonatypeRepo("snapshots")) // contains akka(-http) snapshots
      .toSeq
  }
}
