/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc.macros

import scala.quoted.*
import scala.reflect.ClassTag

import play.api.mvc.PathBindable
import play.api.mvc.QueryStringBindable

object BinderMacros {

  def anyValPathBindable[A <: AnyVal: Type](using q: Quotes): Expr[PathBindable[A]] =
    withAnyValParam[A, PathBindable]

  def anyValQueryStringBindable[A <: AnyVal: Type](using q: Quotes): Expr[QueryStringBindable[A]] =
    withAnyValParam[A, QueryStringBindable]

  def withAnyValParam[A <: AnyVal: Type, T[_] <: PathBindable[_] | QueryStringBindable[_]: Type](
      using q: Quotes
  ): Expr[T[A]] = {
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
                Expr.summon[T[t]] match {
                  case Some(binder) => {
                    def mapf(in: Expr[t]): Expr[A] =
                      New(Inferred(aTpr)).select(ctor).appliedTo(in.asTerm).asExprOf[A]

                    def contramapf(in: Expr[A]): Expr[t] = {
                      val term = in.asTerm

                      term
                        .select(term.symbol.fieldMember(v.name))
                        .asExprOf[t](using vtpe)
                    }

                    val expr = binder match {
                      case '{ $binder: PathBindable[?] } =>
                        '{
                          ${ binder.asExprOf[PathBindable[t]] }
                            .transform({ v => ${ mapf('v) } }, { v => ${ contramapf('v) } })
                        }
                      case '{ $binder: QueryStringBindable[?] } =>
                        '{
                          ${ binder.asExprOf[QueryStringBindable[t]] }
                            .transform({ v => ${ mapf('v) } }, { v => ${ contramapf('v) } })
                        }
                    }

                    expr.asExprOf[T[A]]
                  }

                  case None =>
                    report.errorAndAbort(
                      s"Instance not found: ${Type.show[T]}[${tpr.typeSymbol.fullName}]"
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
