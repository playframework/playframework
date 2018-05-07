/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api

import scala.reflect.macros.blackbox

private[api] class ConfigLoaderMacros(val c: blackbox.Context) {

  import c.universe._

  def forClass[T](implicit t: WeakTypeTag[T]): Tree = {
    val tpe = t.tpe
    // Look at all public constructors
    val publicConstructors: Seq[MethodSymbol] = tpe.members.collect {
      case m if m.isConstructor && m.isPublic && !m.fullName.endsWith("$init$") =>
        m.asMethod
    }(collection.breakOut)

    val constructor = publicConstructors match {
      case Seq() =>
        c.abort(c.enclosingPosition, s"Public constructor not found for type $tpe")
      case Seq(cons) =>
        cons
      case constructors =>
        // If multiple public constructors are found, try to select the primary constructor, otherwise give up
        constructors.find(_.isPrimaryConstructor).getOrElse(
          c.abort(c.enclosingPosition, s"Multiple public constructors found for $tpe but one is not primary")
        )
    }

    val confTerm = TermName(c.freshName("conf$"))
    val argumentLists = constructor.paramLists.map { params =>
      params.map { p =>
        q"implicitly[_root_.play.api.ConfigLoader[${p.typeSignature}]].load($confTerm, ${p.name.decodedName.toString})"
      }
    }
    q"""
      new _root_.play.api.ConfigLoader[$tpe] {
        override def load(config: _root_.com.typesafe.config.Config, path: String) = {
          val $confTerm = config.getConfig(path)
          new $tpe(...$argumentLists)
        }
      }
    """
  }

}
