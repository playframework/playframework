//
// Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
//

//#assembly
import AssemblyKeys._

assemblySettings

mainClass in assembly := Some("play.core.server.ProdServerStart")

fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value)
//#assembly
