// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

//#compiler-options
scalacOptions += "-feature"
//#compiler-options

//#add-assets
Assets / unmanagedResourceDirectories += baseDirectory.value / "pictures"
//#add-assets

//#disable-scaladoc
Compile / doc / sources                := Seq.empty
Compile / packageDoc / publishArtifact := false
//#disable-scaladoc

//#ivy-logging
ivyLoggingLevel := UpdateLogging.Quiet
//#ivy-logging

//#fork-parallel-test
Test / parallelExecution := true
Test / fork              := false
//#fork-parallel-test
