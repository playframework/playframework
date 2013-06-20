package play.api.db.slick

import scala.slick.driver.ExtendedProfile

/**
 * This profile makes it easier to use
 * the cake pattern with Slick
 *
 * ==Example==
 *
 *  {{{
 *   class DAO(override val profile: ExtendedProfile) extends CatComponent with Profile
 *
 *   trait CatComponent { this: Profile => //<- step 1: you must add this "self-type"
 *     import profile.simple._ //<- step 2: then import the correct Table, ... from the profile
 *
 *     object Cats extends Table[Cat]("CAT") {
 *       ...
 *     }
 *   }
 *
 *   object current {
 *     val dao = new DAO(DB.driver(play.api.Play.current))
 *   }
 *  }}}
 *
 * See the sample application for another example
 */
trait Profile {
  val profile: ExtendedProfile
}
