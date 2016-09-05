package play.api.i18n

import play.api.libs.typedmap.TypedKey

/**
 * RequestAttributes belonging to the i18n package.
 */
object RequestAttributes {
  val MessagesApiAttr = TypedKey[MessagesApi]("messagesApi")
}
