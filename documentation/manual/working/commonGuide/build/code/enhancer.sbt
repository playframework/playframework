//
// Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
//

//#plugins.sbt
addSbtPlugin("com.typesafe.sbt" % "sbt-play-enhancer" % "1.2.2")
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
