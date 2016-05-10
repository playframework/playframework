//
// Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
//

// Ensure sbt just goes straight to local for SNAPSHOTs, and doesn't try anything else
fullResolvers := Resolver.defaultLocal +: fullResolvers.value
updateOptions := updateOptions.value.withLatestSnapshots(false)

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % sys.props("project.version"))
addSbtPlugin("com.typesafe.sbt" % "sbt-mocha" % "1.1.0")
