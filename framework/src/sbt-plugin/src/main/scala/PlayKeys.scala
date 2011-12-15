package sbt

trait PlayKeys {
  val distDirectory = SettingKey[File]("play-dist")
  val playResourceDirectories = SettingKey[Seq[File]]("play-resource-directories")
  val confDirectory = SettingKey[File]("play-conf")
  val templatesImport = SettingKey[Seq[String]]("play-templates-imports")

  val templatesTypes = SettingKey[PartialFunction[String, (String, String)]]("play-templates-formats")
  val minify = SettingKey[Boolean]("minify", "Whether assets (Javascript and CSS) should be minified or not")

}
object PlayKeys extends PlayKeys