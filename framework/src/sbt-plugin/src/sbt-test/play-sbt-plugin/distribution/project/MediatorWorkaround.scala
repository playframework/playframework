import sbt._
import Keys._

// Track https://github.com/sbt/sbt/issues/2786
object MediatorWorkaround extends AutoPlugin {
  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements
  override def projectSettings =
    Seq(
      ivyScala := { ivyScala.value map {_.copy(overrideScalaVersion = sbtPlugin.value)} }
    )
}
