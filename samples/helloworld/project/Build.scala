import sbt._
import Keys._

object ApplicationBuild extends Build {

    val appName         = "helloworld"
    val appVersion      = "0.1"

    val appDependencies = Seq( "org.specs2" %% "specs2" % "1.6.1" % "test,test",
                        "com.novocode" % "junit-interface" % "0.7" % "test,test") 

    val main = PlayProject(appName, appVersion, appDependencies)

}
            
