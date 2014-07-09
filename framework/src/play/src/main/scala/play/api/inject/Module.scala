/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.inject

import play.api.Environment

trait Module {
  def bindings(env: Environment): Seq[Binding[_]]
}
