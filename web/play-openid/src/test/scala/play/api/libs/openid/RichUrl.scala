/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.openid

trait RichUrl[A] {
  def hostAndPath: String
}
