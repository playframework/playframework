lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := sys.props.get("scala.version").getOrElse("2.10.4")

// can't use test directory since scripted calls its script "test"
sourceDirectory in Test := baseDirectory.value / "tests"

scalaSource in Test := baseDirectory.value / "tests"

// Generate a js router so we can test it with mocha
val generateJsRouter = TaskKey[Seq[File]]("generate-js-router")

generateJsRouter := {
  (runMain in Compile).toTask(" utils.JavaScriptRouterGenerator target/web/jsrouter/jsRoutes.js").value
  Seq(target.value / "web" / "jsrouter" / "jsRoutes.js")
}

resourceGenerators in TestAssets <+= generateJsRouter

managedResourceDirectories in TestAssets += target.value / "web" / "jsrouter"
