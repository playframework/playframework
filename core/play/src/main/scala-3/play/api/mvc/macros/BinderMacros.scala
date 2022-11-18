/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc.macros

import scala.quoted.*
import play.api.mvc.{PathBindable, QueryStringBindable}

object BinderMacros {

  def anyValPathBindable[A <: AnyVal: Type](using
      q: Quotes
  ): Expr[PathBindable[A]] = {
    import q.reflect.*

    val aTpr = TypeRepr.of[A]
    val ctor = aTpr.typeSymbol.primaryConstructor

    ctor.paramSymss match {
      case List(v: Symbol) :: Nil =>
        v.tree match {
          case vd: ValDef => {
            val tpr = vd.tpt.tpe

            tpr.asType match {
              case vtpe @ '[t] =>
                Expr.summon[PathBindable[t]] match {
                  case Some(binder) => {
                    def mapf(in: Expr[t]): Expr[A] =
                      New(Inferred(aTpr)).select(ctor).appliedTo(in.asTerm).asExprOf[A]

                    def contramapf(in: Expr[A]): Expr[t] = {
                      val term = in.asTerm

                      term
                        .select(term.symbol.fieldMember(v.name))
                        .asExprOf[t](using vtpe)
                    }

                    val expr = '{
                      ${ binder }.transform({ v => ${ mapf('v) } }, { v => ${ contramapf('v) } })
                    }

                    //debug(expr.show)

                    expr
                  }

                  case None =>
                    report.errorAndAbort(
                      s"Instance not found: ${classOf[PathBindable[_]].getName}[${tpr.typeSymbol.fullName}]"
                    )
                }
            }
          }

          case _ =>
            report.errorAndAbort(
              s"Constructor parameter expected, found: ${v}"
            )
        }

      case _ =>
        report.errorAndAbort(
          s"Cannot resolve value reader for '${aTpr.typeSymbol.name}'"
        )

    }
  }

  def anyValQueryStringBindable[A <: AnyVal: Type](using
      q: Quotes
  ): Expr[QueryStringBindable[A]] = {
    import q.reflect.*

    val aTpr = TypeRepr.of[A]
    val ctor = aTpr.typeSymbol.primaryConstructor

    ctor.paramSymss match {
      case List(v: Symbol) :: Nil =>
        v.tree match {
          case vd: ValDef => {
            val tpr = vd.tpt.tpe

            tpr.asType match {
              case vtpe @ '[t] =>
                Expr.summon[QueryStringBindable[t]] match {
                  case Some(binder) => {
                    def mapf(in: Expr[t]): Expr[A] =
                      New(Inferred(aTpr)).select(ctor).appliedTo(in.asTerm).asExprOf[A]

                    def contramapf(in: Expr[A]): Expr[t] = {
                      val term = in.asTerm

                      term
                        .select(term.symbol.fieldMember(v.name))
                        .asExprOf[t](using vtpe)
                    }

                    val expr = '{
                      ${ binder }.transform({ v => ${ mapf('v) } }, { v => ${ contramapf('v) } })
                    }

                    //debug(expr.show)

                    expr
                  }

                  case None =>
                    report.errorAndAbort(
                      s"Instance not found: ${classOf[QueryStringBindable[_]].getName}[${tpr.typeSymbol.fullName}]"
                    )
                }
            }
          }

          case _ =>
            report.errorAndAbort(
              s"Constructor parameter expected, found: ${v}"
            )
        }

      case _ =>
        report.errorAndAbort(
          s"Cannot resolve value reader for '${aTpr.typeSymbol.name}'"
        )

    }
  }

}
