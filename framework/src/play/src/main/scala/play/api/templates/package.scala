/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api

/**
 * Contains Template adapters for typical Play applications.
 */
package object templates {
  @deprecated("Use play.twirl.api.Html", "2.3.0")
  type Html = play.twirl.api.Html

  @deprecated("Use play.twirl.api.Html", "2.3.0")
  val Html = play.twirl.api.Html

  @deprecated("Use play.twirl.api.HtmlFormat", "2.3.0")
  val HtmlFormat = play.twirl.api.HtmlFormat

  @deprecated("Use play.twirl.api.Txt", "2.3.0")
  type Txt = play.twirl.api.Txt

  @deprecated("Use play.twirl.api.Txt", "2.3.0")
  val Txt = play.twirl.api.Txt

  @deprecated("Use play.twirl.api.TxtFormat", "2.3.0")
  val TxtFormat = play.twirl.api.TxtFormat

  @deprecated("Use play.twirl.api.Xml", "2.3.0")
  type Xml = play.twirl.api.Xml

  @deprecated("Use play.twirl.api.Xml", "2.3.0")
  val Xml = play.twirl.api.Xml

  @deprecated("Use play.twirl.api.XmlFormat", "2.3.0")
  val XmlFormat = play.twirl.api.XmlFormat

  @deprecated("Use play.twirl.api.JavaScript", "2.3.0")
  type JavaScript = play.twirl.api.JavaScript

  @deprecated("Use play.twirl.api.JavaScript", "2.3.0")
  val JavaScript = play.twirl.api.JavaScript

  @deprecated("Use play.twirl.api.JavaScriptFormat", "2.3.0")
  val JavaScriptFormat = play.twirl.api.JavaScriptFormat
}
