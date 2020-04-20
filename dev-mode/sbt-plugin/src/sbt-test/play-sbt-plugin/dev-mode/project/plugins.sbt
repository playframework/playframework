//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//

updateOptions := updateOptions.value.withLatestSnapshots(false)
addSbtPlugin("com.typesafe.play" % "sbt-plugin"         % sys.props("project.version"))
addSbtPlugin("com.typesafe.play" % "sbt-scripted-tools" % sys.props("project.version"))
addSbtPlugin("com.typesafe.sbt"  % "sbt-less"           % "1.1.2")
libraryDependencies += "com.lightbend.play" % "jnotify" % "0.94-play-2" // to test JNotify
