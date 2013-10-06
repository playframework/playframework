package play.api.data.mapping

import scala.language.implicitConversions

trait Write[I, +O] {

  /**
   * "Serialize" `i` to the output type
   */
  def writes(i: I): O

  /**
   * returns a new Write that applies function `f` to the result of this write.
   * {{{
   *  val w = Writes.int.map("Number: " + _)
   *  w.writes(42) == "Number: 42"
   * }}}
   */
  def map[B](f: O => B) = Write[I, B] {
    f.compose(x => this.writes(x))
  }

  /**
   * Returns a new Write that applies `this` Write, and then applies `w` to its result
   */
  def compose[OO >: O, P](w: Write[OO, P]) =
    this.map(o => w.writes(o))
}

trait DefaultMonoids {
  import play.api.libs.functional.Monoid

  implicit def mapMonoid = new Monoid[UrlFormEncoded] {
    def append(a1: UrlFormEncoded, a2: UrlFormEncoded) = a1 ++ a2
    def identity = Map.empty
  }
}

object MappingMacros {
  import language.experimental.macros
  import scala.reflect.macros.Context

  private abstract class Helper {
    val context: Context
    import context.universe._

    def findAltMethod(s: MethodSymbol, paramTypes: List[Type]): Option[MethodSymbol] =
      // TODO: we can make this a bit faster by checking the number of params
      s.alternatives.collectFirst {
        case (apply: MethodSymbol) if (apply.paramss.headOption.toSeq.flatMap(_.map(_.asTerm.typeSignature)) == paramTypes) => apply
      }

    def getMethod(t: Type, methodName: String): Option[MethodSymbol] = {
      t.declaration(stringToTermName(methodName)) match {
        case NoSymbol => None
        case s => Some(s.asMethod)
      }
    }

    def getReturnTypes(s: MethodSymbol): List[Type] =
      s.returnType match {
        case TypeRef(_, _, args) =>
          args.head match {
            case t @ TypeRef(_, _, Nil) => List(t)
            case t @ TypeRef(_, _, args) =>
              if (t <:< typeOf[Option[_]]) List(t)
              else if (t <:< typeOf[Seq[_]]) List(t)
              else if (t <:< typeOf[Set[_]]) List(t)
              else if (t <:< typeOf[Map[_, _]]) List(t)
              else if (t <:< typeOf[Product]) args
              else context.abort(context.enclosingPosition, s"$s has unsupported return types")
            case t => context.abort(context.enclosingPosition, s" expected TypeRef, got $t")
          }
        case t => context.abort(context.enclosingPosition, s" expected TypeRef, got $t")
      }

  }

  def write[I: c.WeakTypeTag, O: c.WeakTypeTag](c: Context): c.Expr[Write[I, O]] = {
    import c.universe._
    import c.universe.Flag._

    val helper = new { val context: c.type = c } with Helper
    import helper._

    val companioned = weakTypeOf[I].typeSymbol
    val companionSymbol = companioned.companionSymbol
    val companionType = companionSymbol.typeSignature

    val unapply = getMethod(companionType, "unapply")
      .getOrElse(c.abort(c.enclosingPosition, s"No unapply method found"))

    val rts = getReturnTypes(unapply)
    val app = getMethod(companionType, "apply")
      .getOrElse(c.abort(c.enclosingPosition, s"No apply method found"))
    val apply = findAltMethod(app, rts)

    val writes = for(
      a <- apply.toList;
      g <- a.paramss.headOption.toList;
      p <- g
    ) yield {
      val term = p.asTerm
      q"""(__ \ ${c.literal(term.name.toString)}).write[${term.typeSignature}]"""
    }

    val typeI = weakTypeOf[I].normalize
    val typeO = weakTypeOf[O].normalize

    // TODO: check return type, should be Option[X]
    val TypeRef(_, _, ps) = unapply.returnType
    val t = tq"${typeI} => ${ps.head}"
    val body = writes match {
      case w1 :: w2 :: ts =>
        val typeApply = ts.foldLeft(q"$w1 ~ $w2"){ (t1, t2) => q"$t1 ~ $t2" }
        q"($typeApply).apply(unlift($unapply(_)): $t)"

      case w1 :: Nil =>
        q"$w1.contramap(unlift($unapply(_)): $t)"
    }

    // XXX: recursive values need the user to use explcitly typed implicit val
    c.Expr[Write[I, O]](q"""To[${typeO}] { __ => $body }""")
  }

  def rule[I: c.WeakTypeTag, O: c.WeakTypeTag](c: Context): c.Expr[Rule[I, O]] = {
    ???
  }
}

object Write {
  import scala.language.experimental.macros

  def apply[I, O](w: I => O): Write[I, O] = new Write[I, O] {
    def writes(i: I) = w(i)
  }

  def gen[I, O]: Write[I, O] = macro MappingMacros.write[I, O]

  implicit def zero[I]: Write[I, I] = Write(identity[I] _)

  import play.api.libs.functional._
  implicit def functionalCanBuildWrite[O](implicit m: Monoid[O]) = new FunctionalCanBuild[({type λ[I] = Write[I, O]})#λ] {
    def apply[A, B](wa: Write[A, O], wb: Write[B, O]): Write[A ~ B, O] = Write[A ~ B, O] { (x: A ~ B) =>
      x match {
        case a ~ b => m.append(wa.writes(a), wb.writes(b))
      }
    }
  }

  implicit def contravariantfunctorWrite[O] = new ContravariantFunctor[({type λ[I] = Write[I, O]})#λ] {
    def contramap[A, B](wa: Write[A, O], f: B => A): Write[B, O] = Write[B, O]( (b: B) => wa.writes(f(b)) )
  }

   // XXX: Helps the compiler a bit
  import play.api.libs.functional.syntax._
  implicit def fbo[I, O: Monoid](a: Write[I, O]) = toFunctionalBuilderOps[({type λ[I] = Write[I, O]})#λ, I](a)
  implicit def cfo[I, O](a: Write[I, O]) = toContraFunctorOps[({type λ[I] = Write[I, O]})#λ, I](a)

}