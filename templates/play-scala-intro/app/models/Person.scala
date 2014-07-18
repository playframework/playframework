package models

import play.api.libs.json._

case class Person(name: String)

object Person {
  
  implicit val personFormat = Json.format[Person]
}