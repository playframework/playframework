import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "computer-database-jpa"
    val appVersion      = "1.0"

    val appDependencies = Seq(
      "play" %% "play-java" % play.core.PlayVersion.current,
      "play" %% "play-java-jdbc" % play.core.PlayVersion.current,
      "play" %% "play-java-jpa" % play.core.PlayVersion.current,
      "org.hibernate" % "hibernate-entitymanager" % "3.6.9.Final"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = JAVA).settings(
      ebeanEnabled := false   
    )

}
            
