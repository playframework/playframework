//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//

//#no-scaladoc
Compile / doc / sources := Seq.empty

Compile / packageDoc / publishArtifact := false
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
