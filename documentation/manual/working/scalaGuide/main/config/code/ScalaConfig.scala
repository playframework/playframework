/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.config

import javax.inject.Inject

import com.typesafe.config.Config
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.ConfigLoader
import play.api.Configuration
import play.api.mvc._
import play.api.test.Helpers
import play.api.test.PlaySpecification
import java.net.URI

import org.specs2.mutable.SpecificationLike

@RunWith(classOf[JUnitRunner])
class ScalaConfigSpec extends AbstractController(Helpers.stubControllerComponents()) with PlaySpecification {

  val config: Configuration = Configuration.from(
    Map(
      "foo"        -> "bar",
      "bar"        -> "1.25",
      "baz"        -> "true",
      "listOfFoos" -> Seq("bar", "baz"),
      "app.config" -> Map(
        "title"   -> "Foo",
        "baseUri" -> "https://example.com"
      )
    )
  )

  "Scala Configuration" should {

    "be injectable" in {
      running() { app =>
        val controller = app.injector.instanceOf[MyController]
        ok
      }
    }

    "get different types of keys" in {
      //#config-get

      // foo = bar
      config.get[String]("foo")

      // bar = 8
      config.get[Int]("bar")

      // baz = true
      config.get[Boolean]("baz")

      // listOfFoos = ["bar", "baz"]
      config.get[Seq[String]]("listOfFoos")

      //#config-get

      //#config-validate
      config.getAndValidate[String]("foo", Set("bar", "baz"))
      //#config-validate

      // check that a bad key doesn't work
      config.get[String]("bogus") must throwAn[Exception]

      ok
    }

    "allow defining custom config loaders" in {
      //#config-loader-get
      // app.config = {
      //   title = "My App
      //   baseUri = "https://example.com/"
      // }
      config.get[AppConfig]("app.config")
      //#config-loader-get

      ok
    }

  }

}

//#config-loader-example
case class AppConfig(title: String, baseUri: URI)
object AppConfig {

  implicit val configLoader: ConfigLoader[AppConfig] = new ConfigLoader[AppConfig] {
    def load(rootConfig: Config, path: String): AppConfig = {
      val config = rootConfig.getConfig(path)
      AppConfig(
        title = config.getString("title"),
        baseUri = new URI(config.getString("baseUri"))
      )
    }
  }
}
//#config-loader-example

//#inject-config
class MyController @Inject()(config: Configuration, c: ControllerComponents) extends AbstractController(c) {
  def getFoo = Action {
    Ok(config.get[String]("foo"))
  }
}
//#inject-config
