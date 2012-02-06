package sbt

trait PlayKeys {

  val distDirectory = SettingKey[File]("play-dist")

  val playAssetsDirectories = SettingKey[Seq[File]]("play-assets-directories")

  val confDirectory = SettingKey[File]("play-conf")

  val templatesImport = SettingKey[Seq[String]]("play-templates-imports")

  val routesImport = SettingKey[Seq[String]]("play-routes-imports")

  val ebeanEnabled = SettingKey[Boolean]("play-ebean-enabled")

  val templatesTypes = SettingKey[PartialFunction[String, (String, String)]]("play-templates-formats")

  val closureCompilerOptions = SettingKey[Seq[String]]("play-closure-compiler-options")

  val lessOptions = SettingKey[Seq[String]]("play-less-options")

  val coffeeScriptOptions = SettingKey[Seq[String]]("play-coffeescript-options")

}
object PlayKeys extends PlayKeys