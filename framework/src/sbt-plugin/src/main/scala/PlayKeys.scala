package sbt

trait PlayKeys {
  
  val jdbc =  "play" %% "play-jdbc" % play.core.PlayVersion.current
  
  val anorm = "play" %% "anorm" % play.core.PlayVersion.current
  
  val javaCore = "play" %% "play-java" % play.core.PlayVersion.current
  
  val javaJdbc = "play" %% "play-java-jdbc" % play.core.PlayVersion.current
  
  val javaEbean = "play" %% "play-java-ebean" % play.core.PlayVersion.current
  
  val javaJpa = "play" %% "play-java-jpa" % play.core.PlayVersion.current

  def component(id: String) = "play" %% id % play.core.PlayVersion.current

  val filters = "play" %% "filters-helpers" % play.core.PlayVersion.current

  val playVersion = SettingKey[String]("play-version")

  val playDefaultPort = SettingKey[Int]("play-default-port")

  val requireJs = SettingKey[Seq[String]]("play-require-js")

  val requireNativePath = SettingKey[Option[String]]("play-require-native-path")

  val playOnStarted = SettingKey[Seq[(java.net.InetSocketAddress) => Unit]]("play-onStarted")

  val playOnStopped = SettingKey[Seq[() => Unit]]("play-onStopped")

  val distDirectory = SettingKey[File]("play-dist")

  val distExcludes = SettingKey[Seq[String]]("dist-excludes")

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

  val playPlugin = SettingKey[Boolean]("play-plugin")

}
object PlayKeys extends PlayKeys