//#plugins.sbt
addSbtPlugin("com.typesafe.sbt" % "sbt-play-enhancer" % "1.1.0")
//#plugins.sbt

//#disable-project
lazy val nonEnhancedProject = (project in file("non-enhanced"))
  .disablePlugins(PlayEnhancer)
//#disable-project

//#disable-enhancement
playEnhancerEnabled := false
//#disable-enhancement

//#select-generate
sources in (Compile, playEnhancerGenerateAccessors) := {
  ((javaSource in Compile).value / "models" ** "*.java").get
}
//#select-generate
