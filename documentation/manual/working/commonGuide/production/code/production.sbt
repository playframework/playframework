// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

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
