// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

updateOptions                   := updateOptions.value.withLatestSnapshots(false)
addSbtPlugin("org.playframework" % "sbt-plugin"         % sys.props("project.version"))
addSbtPlugin("org.playframework" % "sbt-scripted-tools" % sys.props("project.version"))
addSbtPlugin("com.typesafe.sbt"  % "sbt-less"           % "1.1.2")
