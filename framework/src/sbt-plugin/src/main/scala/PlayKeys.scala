package sbt

trait PlayKeys {

  val distDirectory = SettingKey[File]("play-dist")

  val playAssetsDirectories = SettingKey[Seq[File]]("play-assets-directories")

  val confDirectory = SettingKey[File]("play-conf")

  val templatesImport = SettingKey[Seq[String]]("play-templates-imports")

  val templatesTypes = SettingKey[PartialFunction[String, (String, String)]]("play-templates-formats")

  val minify = SettingKey[Boolean]("play-minify", "Whether assets (Javascript and CSS) should be minified or not")

}
object PlayKeys extends PlayKeys