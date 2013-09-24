/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package test
import org.specs2.mutable._


class AssetsBuilderSpec extends Specification {

    "Custom Assets" should {
      "work with Scala API" in {
         controllers.my.ScalaAssets.at("sdfd","dsfd").toString must equalTo ("Action(parser=BodyParser(anyContent))")
      }
      "work with Java API" in {
         controllers.my.JavaAssets.delegate.at("sdfd","dsfd").toString must equalTo ("Action(parser=BodyParser(anyContent))")
      }
      
    }
  }

