name := "play-scala-intro"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "%SCALA_VERSION%"

resolvers += "sorm Scala 2.11 fork" at "http://markusjura.github.io/sorm"

libraryDependencies ++= Seq(  
  "org.sorm-framework" % "sorm" % "0.4.1",
  "com.h2database" % "h2" % "1.4.177",
  specs2 % Test
)     

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
