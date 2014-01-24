package play.api.data.mapping

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

    def lookup[T: WeakTypeTag] = {
      val companioned = weakTypeOf[T].typeSymbol
      val companionSymbol = companioned.companionSymbol
      val companionType = companionSymbol.typeSignature

      companionType match {
        case NoSymbol =>
          context.abort(context.enclosingPosition, s"No companion object found for $companioned")
        case _ =>
          val unapply = getMethod(companionType, "unapply")
            .getOrElse(context.abort(context.enclosingPosition, s"No unapply method found for $companionSymbol"))

          val rts = getReturnTypes(unapply)
          val app = getMethod(companionType, "apply")
            .getOrElse(context.abort(context.enclosingPosition, s"No apply method found"))
          val apply = findAltMethod(app, rts)
            .getOrElse(context.abort(context.enclosingPosition, s"No apply method matching the unapply method found"))

          (apply, unapply)
      }
    }

  }

  def write[I: c.WeakTypeTag, O: c.WeakTypeTag](c: Context): c.Expr[Write[I, O]] = {
    import c.universe._
    import c.universe.Flag._

    val helper = new { val context: c.type = c } with Helper
    import helper._

    val (apply, unapply) = lookup[I]

    val writes = for (
      g <- apply.paramss.headOption.toList;
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
        val typeApply = ts.foldLeft(q"$w1 ~ $w2") { (t1, t2) => q"$t1 ~ $t2" }
        q"($typeApply).apply(play.api.libs.functional.syntax.unlift($unapply(_)): $t)"

      case w1 :: Nil =>
        q"$w1.contramap(play.api.libs.functional.syntax.unlift($unapply(_)): $t)"
    }

    // XXX: recursive values need the user to use explcitly typed implicit val
    c.Expr[Write[I, O]](q"""To[${typeO}] { __ => $body }""")
  }

  def rule[I: c.WeakTypeTag, O: c.WeakTypeTag](c: Context): c.Expr[Rule[I, O]] = {
    import c.universe._
    import c.universe.Flag._

    val helper = new { val context: c.type = c } with Helper
    import helper._

    val (apply, unapply) = lookup[O]

    val writes = for (
      g <- apply.paramss.headOption.toList;
      p <- g
    ) yield {
      val term = p.asTerm
      q"""(__ \ ${c.literal(term.name.toString)}).read[${term.typeSignature}]"""
    }

    val typeI = weakTypeOf[I].normalize
    val typeO = weakTypeOf[O].normalize

    val xs: List[Ident] = apply.paramss.head.map(s => Ident(s.name))
    val vs: List[ValDef] = apply.paramss.head.map(s => ValDef(Modifiers(PARAM), newTermName(s.name.encoded), TypeTree(), EmptyTree))
    val companionSymbol = weakTypeOf[O].typeSymbol.companionSymbol
    val applyƒ = q"{ (..$vs) => ${companionSymbol.typeSignature}.apply(..$xs) }"

    val body = writes match {
      case w1 :: w2 :: ts =>
        val typeApply = ts.foldLeft(q"$w1 ~ $w2") { (t1, t2) => q"$t1 ~ $t2" }
        q"($typeApply).apply($applyƒ)"

      case w1 :: Nil =>
        q"$w1.fmap($applyƒ)"
    }

    // XXX: recursive values need the user to use explcitly typed implicit val
    c.Expr[Rule[I, O]](q"""From[${typeI}] { __ => $body }""")
  }

  def format[IR: c.WeakTypeTag, IW: c.WeakTypeTag, O: c.WeakTypeTag](c: Context): c.Expr[Format[IR, IW, O]] = {
    import c.universe._
    import c.universe.Flag._

    val r = rule[IR, O](c)
    val w = write[O, IW](c)
    c.Expr[Format[IR, IW, O]](q"""Format($r, $w)""")
  }
}