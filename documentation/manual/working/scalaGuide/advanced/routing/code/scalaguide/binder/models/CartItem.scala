/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.binder.models

import java.net.URLEncoder
import scala.Left
import scala.Right
import play.api.mvc.PathBindable
import play.Logger
import play.api.mvc.QueryStringBindable

//#declaration
case class CartItem(identifier: String) {}
//#declaration
object CartItem {
  implicit def queryStringBindable(implicit strBinder: QueryStringBindable[String]) =
    new QueryStringBindable[CartItem] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, CartItem]] = {
        for {
          identifierEither <- strBinder.bind("identifier", params)
        } yield {
          identifierEither match {
            case Right(identifier) => Right(CartItem(identifier))
            case _                 => Left("Unable to bind an CartItem identifier")
          }
        }
      }
      //#unbind
      override def unbind(key: String, cartItem: CartItem): String = {
        // If we don't use Play's QueryStringBindable[String].unbind() for some reason, we need to construct the result string manually.
        // The key is constant and does not contain any special character, but
        // value may contain special characters => need form URL encoding for cartItem.identifier:
        "identifier=" + URLEncoder.encode(cartItem.identifier, "utf-8")
      }
      //#unbind
    }
}
