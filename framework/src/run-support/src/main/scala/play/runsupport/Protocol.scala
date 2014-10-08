package play.runsupport

object protocol {
  final case class SourceMapTarget(sourceFile: java.io.File, originalSource: Option[java.io.File])

  final case class PlayForkSupportResult(sourceMap: Map[String, SourceMapTarget],
    dependencyClasspath: Seq[java.io.File],
    reloaderClasspath: Seq[java.io.File],
    allAssets: Seq[(String, java.io.File)],
    monitoredFiles: Seq[String],
    devSettings: Seq[(String, String)],
    docsClasspath: Seq[java.io.File])
}

final case class PlayServerStarted(url: String)
