//
// Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
//

//#assembly
mainClass in assembly := Some("play.core.server.ProdServerStart")
fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value)
//#assembly
