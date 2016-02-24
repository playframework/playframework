/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.json

import scala.language.higherKinds
import scala.reflect.macros.blackbox.Context
import language.experimental.macros

/**
 * Implementation for the JSON macro.
 */
object JsMacroImpl {

  def formatImpl[A: c.WeakTypeTag](c: Context): c.Expr[OFormat[A]] =
    macroImpl[A, OFormat, Format](c, "format", "inmap", reads = true, writes = true)

  def readsImpl[A: c.WeakTypeTag](c: Context): c.Expr[Reads[A]] =
    macroImpl[A, Reads, Reads](c, "read", "map", reads = true, writes = false)

  def writesImpl[A: c.WeakTypeTag](c: Context): c.Expr[OWrites[A]] =
    macroImpl[A, OWrites, Writes](c, "write", "contramap", reads = false, writes = true)

  /**
   * Generic implementation of the macro
   *
   * The reads/writes flags are used to say whether a reads/writes is being generated (in the case format,
   * these are both true).  This is also used to determine what arguments should be passed to various
   * functions, for example, if the apply, unapply, or both should be passed to the functional builder apply
   * method.
   *
   * @param c The context
   * @param methodName The name of the method on JsPath that gets called, ie, read/write/format
   * @param mapLikeMethod The method that's used to map the type of thing being built, used in case there is
   *                      only one field in the case class.
   * @param reads Whether we should generate a reads.
   * @param writes Whether we should generate a writes
   * @param atag The class of the type we're generating a reads/writes/format for.
   * @param matag The class of the reads/writes/format.
   * @param natag The class of the reads/writes/format.
   */
  private def macroImpl[A, M[_], N[_]](c: Context, methodName: String, mapLikeMethod: String, reads: Boolean, writes: Boolean)(implicit atag: c.WeakTypeTag[A], matag: c.WeakTypeTag[M[A]], natag: c.WeakTypeTag[N[A]]): c.Expr[M[A]] = {

    // Helper function to create parameter lists for function invocations based on whether this is a reads,
    // writes or both.
    def conditionalList[T](ifReads: T, ifWrites: T): List[T] =
      (if (reads) List(ifReads) else Nil) :::
        (if (writes) List(ifWrites) else Nil)

    import c.universe._

    // The call is the term name, either "read", "write" or "format", that gets invoked on JsPath,
    // eg (__ \ "foo").read
    val call = TermName(methodName)
    // callNullable is the equivalent of call for options
    // eg (__ \ "foo").readNullable
    val callNullable = TermName(s"${methodName}Nullable")
    // Used for doing lazy reads (when recursive)
    val lazyCall = TermName(s"lazy${methodName.capitalize}")

    val companioned = weakTypeOf[A].typeSymbol
    val companionObject = companioned.companion
    val companionType = companionObject.typeSignature

    // All these can be sort of thought as imports that can then be used later in quasi quote interpolation
    val libs = q"_root_.play.api.libs"
    val json = q"$libs.json"
    val syntax = q"$libs.functional.syntax"
    val utilPkg = q"$json.util"
    val JsPath = q"$json.JsPath"
    val Reads = q"$json.Reads"
    val Writes = q"$json.Writes"
    val unlift = q"$syntax.unlift"
    val LazyHelper = tq"$utilPkg.LazyHelper"

    // First find the unapply for the object
    val unapply = companionType.decl(TermName("unapply"))
    val unapplySeq = companionType.decl(TermName("unapplySeq"))
    val hasVarArgs = unapplySeq != NoSymbol

    val effectiveUnapply = Seq(unapply, unapplySeq).find(_ != NoSymbol) match {
      case None => c.abort(c.enclosingPosition, "No unapply or unapplySeq function found")
      case Some(s) => s.asMethod
    }

    val unapplyReturnTypes: Option[List[Type]] = effectiveUnapply.returnType match {
      case TypeRef(_, _, Nil) =>
        c.abort(c.enclosingPosition, s"Unapply of $companionObject has no parameters. Are you using an empty case class?")
        None

      case TypeRef(_, _, args) =>
        args.head match {
          case t @ TypeRef(_, _, Nil) => Some(List(t))
          case t @ TypeRef(_, _, args) =>
            import c.universe.definitions.TupleClass
            if (!TupleClass.seq.exists(tupleSym => t.baseType(tupleSym) ne NoType)) Some(List(t))
            else if (t <:< typeOf[Product]) Some(args)
            else None
          case _ => None
        }
      case _ => None
    }

    // Now the apply methods for the object
    val applies =
      companionType.decl(TermName("apply")) match {
        case NoSymbol => c.abort(c.enclosingPosition, "No apply function found")
        case s => s.asTerm.alternatives
      }

    // Find an apply method that matches the unapply
    val maybeApply = applies.collectFirst {
      case (apply: MethodSymbol) if hasVarArgs && {
        val someApplyTypes = apply.paramLists.headOption.map(_.map(_.asTerm.typeSignature))
        val someInitApply = someApplyTypes.map(_.init)
        val someApplyLast = someApplyTypes.map(_.last)
        val someInitUnapply = unapplyReturnTypes.map(_.init)
        val someUnapplyLast = unapplyReturnTypes.map(_.last)
        val initsMatch = someInitApply == someInitUnapply
        val lastMatch = (for {
          lastApply <- someApplyLast
          lastUnapply <- someUnapplyLast
        } yield lastApply <:< lastUnapply).getOrElse(false)
        initsMatch && lastMatch
      } => apply
      case (apply: MethodSymbol) if apply.paramLists.headOption.map(_.map(_.asTerm.typeSignature)) == unapplyReturnTypes => apply
    }

    val params = maybeApply match {
      case Some(apply) => apply.paramLists.head //verify there is a single parameter group
      case None => c.abort(c.enclosingPosition, "No apply function found matching unapply parameters")
    }

    // Now we find all the implicits that we need
    final case class Implicit(paramName: Name, paramType: Type, neededImplicit: Tree, isRecursive: Boolean, tpe: Type)

    val createImplicit = { (name: Name, implType: c.universe.type#Type) =>
      val (isRecursive, tpe) = implType match {
        case TypeRef(_, t, args) =>
          val isRec = args.exists(_.typeSymbol == companioned)
          // Option[_] needs special treatment because we need to use XXXOpt
          val tp = if (implType.typeConstructor <:< typeOf[Option[_]].typeConstructor) args.head else implType
          (isRec, tp)
        case TypeRef(_, t, _) =>
          (false, implType)
      }

      // builds M implicit from expected type
      val neededImplicitType = appliedType(natag.tpe.typeConstructor, tpe :: Nil)
      // infers implicit
      val neededImplicit = c.inferImplicitValue(neededImplicitType)
      Implicit(name, implType, neededImplicit, isRecursive, tpe)
    }

    val applyParamImplicits = params.map { param => createImplicit(param.name, param.typeSignature) }
    val effectiveInferredImplicits = if (hasVarArgs) {
      val varArgsImplicit = createImplicit(applyParamImplicits.last.paramName, unapplyReturnTypes.get.last)
      applyParamImplicits.init :+ varArgsImplicit
    } else applyParamImplicits

    // if any implicit is missing, abort
    val missingImplicits = effectiveInferredImplicits.collect { case Implicit(_, t, impl, rec, _) if impl == EmptyTree && !rec => t }
    if (missingImplicits.nonEmpty)
      c.abort(c.enclosingPosition, s"No implicit format for ${missingImplicits.mkString(", ")} available.")

    var hasRec = false

    // combines all reads into CanBuildX
    val canBuild = effectiveInferredImplicits.map {
      case Implicit(name, t, impl, rec, tpe) =>
        // Equivalent to __ \ "name"
        val jspathTree = q"""$JsPath \ ${name.decodedName.toString}"""

        // If we're not recursive, simple, just invoke read/write/format
        if (!rec) {
          // If we're an option, invoke the nullable version
          if (t.typeConstructor <:< typeOf[Option[_]].typeConstructor) {
            q"$jspathTree.$callNullable($impl)"
          } else {
            q"$jspathTree.$call($impl)"
          }
        } else {
          // Otherwise we have to invoke the lazy version
          hasRec = true
          if (t.typeConstructor <:< typeOf[Option[_]].typeConstructor) {
            q"$jspathTree.$callNullable($JsPath.$lazyCall(this.lazyStuff))"
          } else {
            // If this is a list/set/seq/map, then we need to wrap the reads into that.
            def readsWritesHelper(methodName: String): List[Tree] =
              conditionalList(Reads, Writes).map(s => q"$s.${TermName(methodName)}(this.lazyStuff)")

            val arg = if (tpe.typeConstructor <:< typeOf[List[_]].typeConstructor)
              readsWritesHelper("list")
            else if (tpe.typeConstructor <:< typeOf[Set[_]].typeConstructor)
              readsWritesHelper("set")
            else if (tpe.typeConstructor <:< typeOf[Seq[_]].typeConstructor)
              readsWritesHelper("seq")
            else if (tpe.typeConstructor <:< typeOf[Map[_, _]].typeConstructor)
              readsWritesHelper("map")
            else List(q"this.lazyStuff")

            q"$jspathTree.$lazyCall(..$arg)"
          }
        }
    }.reduceLeft[Tree] { (acc, r) =>
      q"$acc.and($r)"
    }

    val applyFunction = {
      if (hasVarArgs) {

        val applyParams = params.foldLeft(List[Tree]())((l, e) =>
          l :+ Ident(TermName(e.name.encodedName.toString))
        )
        val vals = params.foldLeft(List[Tree]())((l, e) =>
          // Let type inference infer the type by using the empty type, TypeTree()
          l :+ q"val ${TermName(e.name.encodedName.toString)}: ${TypeTree()}"
        )

        q"(..$vals) => $companionObject.apply(..${applyParams.init}, ${applyParams.last}: _*)"
      } else {
        q"$companionObject.apply _"
      }
    }

    val unapplyFunction = q"$unlift($companionObject.$effectiveUnapply)"

    // if case class has one single field, needs to use map/contramap/inmap on the Reads/Writes/Format instead of
    // canbuild.apply
    val applyOrMap = TermName(if (params.length > 1) "apply" else mapLikeMethod)
    val finalTree = q"""
      import $syntax._

      $canBuild.$applyOrMap(..${conditionalList(applyFunction, unapplyFunction)})
    """

    val lazyFinalTree = if (!hasRec) {
      finalTree
    } else {
      // If we're recursive, we need to wrap the whole thing in a class that breaks the recursion using a
      // lazy val
      q"""
        new $LazyHelper[${matag.tpe.typeSymbol}, ${atag.tpe.typeSymbol}] {
          override lazy val lazyStuff: ${matag.tpe.typeSymbol}[${atag.tpe}] = $finalTree
        }.lazyStuff
       """
    }
    c.Expr[M[A]](lazyFinalTree)
  }

}
