package sbt

trait PlayKeys {


  val playVersion = SettingKey[String]("play-version")
  
  val playDefaultPort = SettingKey[Int]("play-default-port")
  
  val requireSubFolder = SettingKey[String]("play-require-subfolder")

  val requireNativePath = SettingKey[Option[String]]("play-require-native-path")
  
  val playOnStarted = SettingKey[Seq[(java.net.InetSocketAddress) => Unit]]("play-onStarted")
  
  val playOnStopped = SettingKey[Seq[() => Unit]]("play-onStopped")

  val distDirectory = SettingKey[File]("play-dist")

  val playAssetsDirectories = SettingKey[Seq[File]]("play-assets-directories")

  val incrementalAssetsCompilation = SettingKey[Boolean]("play-incremental-assets-compilation")

  val playExternalAssets = SettingKey[Seq[(File, File => PathFinder, String)]]("play-external-assets")

  val confDirectory = SettingKey[File]("play-conf")

  val templatesImport = SettingKey[Seq[String]]("play-templates-imports")

  val routesImport = SettingKey[Seq[String]]("play-routes-imports")

  val ebeanEnabled = SettingKey[Boolean]("play-ebean-enabled")

  val templatesTypes = SettingKey[PartialFunction[String, (String, String)]]("play-templates-formats")

  val closureCompilerOptions = SettingKey[Seq[String]]("play-closure-compiler-options")

  val lessOptions = SettingKey[Seq[String]]("play-less-options")

  val coffeescriptOptions = SettingKey[Seq[String]]("play-coffeescript-options")

  val lessEntryPoints = SettingKey[PathFinder]("play-less-entry-points")

  val coffeescriptEntryPoints = SettingKey[PathFinder]("play-coffeescript-entry-points")

  val javascriptEntryPoints = SettingKey[PathFinder]("play-javascript-entry-points")

}
object PlayKeys extends PlayKeys