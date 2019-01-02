/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc.macros

import scala.reflect.macros.blackbox.{ Context => MacroContext }

class BinderMacros(val c: MacroContext) {

  import c.universe._

  def anyValPathBindable[T](implicit t: WeakTypeTag[T]): Tree = {
    withAnyValParam(t.tpe) { param =>
      // currently we do not need to invoke `import _root_.play.api.mvc.PathBindable._`
      // since we are in the same package
      q"""
         new _root_.play.api.mvc.PathBindable[${t.tpe}] {
           private val binder = _root_.scala.Predef.implicitly[_root_.play.api.mvc.PathBindable[${param.typeSignature}]]
           override def bind(key: String, value: String): Either[String, ${t.tpe}] = {
             binder.bind(key, value).right.map((p: ${param.typeSignature}) => new ${t.tpe}(p))
           }

           override def unbind(key: String, value: ${t.tpe}): String = {
             binder.unbind(key, value.${param.name.toTermName})
           }

         }
       """
    }.getOrElse(fail("PathBindable", t.tpe))
  }

  def anyValQueryStringBindable[T](implicit t: WeakTypeTag[T]): Tree = {
    withAnyValParam(t.tpe) { param =>
      // currently we do not need to invoke `import _root_.play.api.mvc.QueryStringBindable._`
      // since we are in the same package
      q"""
         private val binder = _root_.scala.Predef.implicitly[_root_.play.api.mvc.QueryStringBindable[${param.typeSignature}]]
         binder.transform((p: ${param.typeSignature}) => new ${t.tpe}(p), (p: ${t.tpe}) => p.${param.name.toTermName})
       """
    }.getOrElse(fail("QueryStringBindable", t.tpe))
  }

  private def fail(enc: String, t: Type) = {
    c.abort(c.enclosingPosition, s"could not find the implicit $enc for AnyVal Type $t")
  }

  private def withAnyValParam[R](tpe: Type)(f: Symbol => R): Option[R] = {
    tpe.baseType(c.symbolOf[AnyVal]) match {
      case NoType => None
      case _ =>
        primaryConstructor(tpe).map(_.paramLists.flatten).collect {
          case param :: Nil => f(param)
        }
    }
  }

  private def primaryConstructor(t: Type) = {
    t.members.collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor => m.typeSignature.asSeenFrom(t, t.typeSymbol)
    }
  }

}
