//
// Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
//

//#compiler-options
scalacOptions += "-feature"
//#compiler-options

//#add-assets
unmanagedResourceDirectories in Assets += baseDirectory.value / "pictures"
//#add-assets

//#disable-scaladoc
sources in (Compile, doc) := Seq.empty
publishArtifact in (Compile, packageDoc) := false
//#disable-scaladoc

//#ivy-logging
ivyLoggingLevel := UpdateLogging.Quiet
//#ivy-logging

//#fork-parallel-test
parallelExecution in Test := true
fork in Test := false
//#fork-parallel-test
