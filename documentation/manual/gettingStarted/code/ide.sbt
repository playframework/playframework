// #add-sbt-eclipse-plugin
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "5.2.4")
// #add-sbt-eclipse-plugin

// #sbt-eclipse-plugin-preTasks
// Compile the project before generating Eclipse files, so
// that generated .scala or .class files for views and routes are present

EclipseKeys.preTasks := Seq(compile in Compile, compile in Test)
// #sbt-eclipse-plugin-preTasks

// #sbt-eclipse-plugin-projectFlavor
// Java project. Don't expect Scala IDE
EclipseKeys.projectFlavor := EclipseProjectFlavor.Java

// Use .class files instead of generated .scala files for views and routes
EclipseKeys.createSrc := EclipseCreateSrc.ValueSet(EclipseCreateSrc.ManagedClasses, EclipseCreateSrc.ManagedResources)
// #sbt-eclipse-plugin-projectFlavor

// #sbt-eclipse-plugin-skipParents
EclipseKeys.skipParents in ThisBuild := false
// #sbt-eclipse-plugin-skipParents
