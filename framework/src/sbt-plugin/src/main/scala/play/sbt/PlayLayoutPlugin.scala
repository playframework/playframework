package play.sbt

import sbt._
import Keys._
import play.twirl.sbt.Import.TwirlKeys
import com.typesafe.sbt.web.SbtWeb.autoImport._

object PlayLayoutPlugin extends AutoPlugin {

  override def requires = Play

  override def trigger = AllRequirements

  override def projectSettings = Seq(
    target := baseDirectory.value / "target",

    sourceDirectory in Compile := baseDirectory.value / "app",
    sourceDirectory in Test := baseDirectory.value / "test",

    resourceDirectory in Compile := baseDirectory.value / "conf",

    scalaSource in Compile := baseDirectory.value / "app",
    scalaSource in Test := baseDirectory.value / "test",

    javaSource in Compile := baseDirectory.value / "app",
    javaSource in Test := baseDirectory.value / "test",

    sourceDirectories in (Compile, TwirlKeys.compileTemplates) := Seq((sourceDirectory in Compile).value),
    sourceDirectories in (Test, TwirlKeys.compileTemplates) := Seq((sourceDirectory in Test).value),

    // sbt-web
    sourceDirectory in Assets := (sourceDirectory in Compile).value / "assets",
    sourceDirectory in TestAssets := (sourceDirectory in Test).value / "assets",
    resourceDirectory in Assets := baseDirectory.value / "public"
  )

}
