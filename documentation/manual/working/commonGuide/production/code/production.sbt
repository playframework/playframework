//
// Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
//

//#no-scaladoc
sources in (Compile, doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false
//#no-scaladoc

//#publish-repo
publishTo := Some(
  "My resolver".at("https://mycompany.com/repo")
)

credentials += Credentials(
  "Repo",
  "https://mycompany.com/repo",
  "admin",
  "admin123"
)
//#publish-repo
