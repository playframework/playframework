// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

// #add-sbt-eclipse-plugin
addSbtPlugin("com.github.sbt" % "sbt-eclipse" % "6.0.0")
// #add-sbt-eclipse-plugin

// #sbt-eclipse-plugin-preTasks
// Compile the project before generating Eclipse files, so
// that generated .scala or .class files for views and routes are present

EclipseKeys.preTasks := Seq(Compile / compile, Test / compile)
// #sbt-eclipse-plugin-preTasks

// #sbt-eclipse-plugin-projectFlavor
// Java project. Don't expect Scala IDE
EclipseKeys.projectFlavor := EclipseProjectFlavor.Java

// Use .class files instead of generated .scala files for views and routes
EclipseKeys.createSrc := EclipseCreateSrc.ValueSet(EclipseCreateSrc.ManagedClasses, EclipseCreateSrc.ManagedResources)
// #sbt-eclipse-plugin-projectFlavor

// #sbt-eclipse-plugin-skipParents
ThisBuild / EclipseKeys.skipParents := false
// #sbt-eclipse-plugin-skipParents
