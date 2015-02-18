/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.db.evolutions

import org.specs2.mutable.Specification
import play.api.Configuration

object DefaultEvolutionsConfigParserSpec extends Specification {

  def parse(config: (String, Any)*): EvolutionsConfig = {
    new DefaultEvolutionsConfigParser(Configuration.from(config.toMap)).get
  }

  def test(key: String)(read: EvolutionsDatasourceConfig => Boolean) = {
    read(parse(key -> true).forDatasource("default")) must_== true
    read(parse(key -> false).forDatasource("default")) must_== false
  }

  def testN(key: String)(read: EvolutionsDatasourceConfig => Boolean) = {
    test("play.modules.evolutions." + key)(read)
  }

  val default = parse().forDatasource("default")

  "The evolutions config parser" should {
    "parse the deprecated style of configuration" in {
      "autocommit" in {
        test("evolutions.autocommit")(_.autocommit)
      }
      "useLocks" in {
        test("evolutions.use.locks")(_.useLocks)
      }
      "autoApply" in {
        test("applyEvolutions.default")(_.autoApply)
      }
      "autoApplyDowns" in {
        test("applyDownEvolutions.default")(_.autoApplyDowns)
      }
    }
    "parse global configuration" in {
      "autocommit" in {
        testN("autocommit")(_.autocommit)
      }
      "useLocks" in {
        testN("useLocks")(_.useLocks)
      }
      "autoApply" in {
        testN("autoApply")(_.autoApply)
      }
      "autoApplyDowns" in {
        testN("autoApplyDowns")(_.autoApplyDowns)
      }
    }
    "parse datasource specific configuration" in {
      "enabled" in {
        testN("db.default.enabled")(_.enabled)
      }
      "autocommit" in {
        testN("db.default.autocommit")(_.autocommit)
      }
      "useLocks" in {
        testN("db.default.useLocks")(_.useLocks)
      }
      "autoApply" in {
        testN("db.default.autoApply")(_.autoApply)
      }
      "autoApplyDowns" in {
        testN("db.default.autoApplyDowns")(_.autoApplyDowns)
      }
    }
    "parse defaults" in {
      "enabled" in {
        default.enabled must_== true
      }
      "autocommit" in {
        default.autocommit must_== true
      }
      "useLocks" in {
        default.useLocks must_== false
      }
      "autoApply" in {
        default.autoApply must_== false
      }
      "autoApplyDowns" in {
        default.autoApplyDowns must_== false
      }
    }

  }

}
