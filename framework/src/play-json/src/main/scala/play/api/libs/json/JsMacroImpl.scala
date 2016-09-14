/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.json

import scala.language.experimental.macros
import scala.language.higherKinds
import scala.reflect.macros.blackbox

/**
 * Implementation for the JSON macro.
 */
object JsMacroImpl {
  /** Only for internal purposes */
  final class Placeholder {}

  /** Only for internal purposes */
  object Placeholder {
    implicit object Format extends OFormat[Placeholder] {
      val success = JsSuccess(new Placeholder())
      def reads(json: JsValue): JsResult[Placeholder] = success
      def writes(pl: Placeholder) = Json.obj()
    }
  }

  def formatImpl[A: c.WeakTypeTag](c: blackbox.Context): c.Expr[OFormat[A]] =
    macroImpl[A, OFormat, Format](
      c, "format", "inmap", reads = true, writes = true)

  def readsImpl[A: c.WeakTypeTag](c: blackbox.Context): c.Expr[Reads[A]] =
    macroImpl[A, Reads, Reads](c, "read", "map", reads = true, writes = false)

  def writesImpl[A: c.WeakTypeTag](c: blackbox.Context): c.Expr[OWrites[A]] =
    macroImpl[A, OWrites, Writes](
      c, "write", "contramap", reads = false, writes = true)

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
   * only one field in the case class.
   * @param reads Whether we should generate a reads.
   * @param writes Whether we should generate a writes
   * @param atag The class of the type we're generating a reads/writes/format for.
   * @param matag The class of the reads/writes/format.
   * @param natag The class of the reads/writes/format.
   */
  private def macroImpl[A, M[_], N[_]](c: blackbox.Context, methodName: String, mapLikeMethod: String, reads: Boolean, writes: Boolean)(implicit atag: c.WeakTypeTag[A], matag: c.WeakTypeTag[M[A]], natag: c.WeakTypeTag[N[A]]): c.Expr[M[A]] = {
    // Helper function to create parameter lists for function invocations based on whether this is a reads,
    // writes or both.
    def conditionalList[T](ifReads: T, ifWrites: T): List[T] =
      (if (reads) List(ifReads) else Nil) :::
    (if (writes) List(ifWrites) else Nil)

    import c.universe._

    val TypeRef(_, _, tpeArgs) = atag.tpe

    // The call is the term name, either "read", "write" or "format",
    // that gets invoked on JsPath (e.g. `(__ \ "foo").read`)
    val call = TermName(methodName)

    // callNullable is the equivalent of call for options
    // e.g. `(__ \ "foo").readNullable`
    val callNullable = TermName(s"${methodName}Nullable")

    // All these can be sort of thought as imports that can then be used later in quasi quote interpolation
    val libs = q"_root_.play.api.libs"
    val json = q"$libs.json"
    val syntax = q"$libs.functional.syntax"
    val utilPkg = q"$json.util"
    val JsPath = q"$json.JsPath"
    val Reads = q"$json.Reads"
    val Writes = q"$json.Writes"
    val unlift = q"$syntax.unlift"

    val companioned = weakTypeOf[A].typeSymbol
    val companionObject = companioned.companion
    val companionType = companionObject.typeSignature

    // Utility about apply/unapply
    object ApplyUnapply {
      private val unapply = companionType.decl(TermName("unapply"))
      private val unapplySeq = companionType.decl(TermName("unapplySeq"))

      val hasVarArgs = unapplySeq != NoSymbol

      // Returns the unapply symbol
      private val effectiveUnapply: MethodSymbol =
        Seq(unapply, unapplySeq).find(_ != NoSymbol) match {
          case None => c.abort(c.enclosingPosition, s"No unapply or unapplySeq function found for $companioned: $unapply / $unapplySeq")
          case Some(s) => s.asMethod
        }

      val unapplyReturnTypes: Option[List[Type]] =
        effectiveUnapply.returnType match {
          case TypeRef(_, _, Nil) => {
            c.abort(c.enclosingPosition, s"Unapply of $companionObject has no parameters. Are you using an empty case class?")
            None
          }

          case TypeRef(_, _, args) => args.head match {
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

      /* Deep check for type compatibility */
      @annotation.tailrec
      private def conforms(types: Seq[(Type, Type)]): Boolean =
        types.headOption match {
          case Some((TypeRef(NoPrefix, a, _),
            TypeRef(NoPrefix, b, _))) => { // for generic parameter
            if (a.fullName != b.fullName) {
              c.warning(c.enclosingPosition,
                s"Type symbols are not compatible: $a != $b")

              false
            }
            else conforms(types.tail)
          }

          case Some((a, b)) if (a.typeArgs.size != b.typeArgs.size) => {
            c.warning(c.enclosingPosition,
              s"Type parameters are not matching: $a != $b")

            false
          }

          case Some((a, b)) if a.typeArgs.isEmpty =>
            if (a =:= b) conforms(types.tail) else {
              c.warning(c.enclosingPosition,
                s"Types are not compatible: $a != $b")

              false
            }

          case Some((a, b)) if (a.baseClasses != b.baseClasses) => {
            c.warning(c.enclosingPosition,
              s"Generic types are not compatible: $a != $b")

            false
          }

          case Some((a, b)) =>
            conforms((a.typeArgs, b.typeArgs).zipped ++: types.tail)

          case _ => true
        }

      // The apply methods for the object
      private val applies = companionType.decl(TermName("apply")) match {
        case NoSymbol => c.abort(c.enclosingPosition, "No apply function found")

        case s => s.asTerm.alternatives.filter { apply =>
          val ps = apply.asMethod.paramLists

          if (ps.size == 1) true else ps.drop(1).forall {
            case p :: _ => p.isImplicit
            case _ => false
          }
        }
      }

      /* Find an apply method that matches the unapply */
      val maybeApply: Option[MethodSymbol] = applies.collectFirst {
        case (apply: MethodSymbol) if hasVarArgs && {
          // Option[List[c.universe.Type]]
          val someApplyTypes = apply.paramLists.headOption.
            map(_.map(_.asTerm.typeSignature))

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

        case (apply: MethodSymbol) if {
          val applyParams = apply.paramLists.headOption.
            toList.flatten.map(_.typeSignature)
          val unapplyParams = unapplyReturnTypes.toList.flatten

          (applyParams.size == unapplyParams.size &&
            conforms((applyParams, unapplyParams).zipped.toSeq))

        } => apply
      }

      // Parameters symbols -> Function tree
      lazy val applyFunction: Option[(Tree, List[TypeSymbol], List[Symbol])] =
        maybeApply.flatMap { app =>
          app.paramLists.headOption.map { params =>
            val tree = if (hasVarArgs) {
              val applyParams = params.foldLeft(List.empty[Tree]) { (l, e) =>
                l :+ Ident(TermName(e.name.encodedName.toString))
              }
              val vals = params.foldLeft(List.empty[Tree])((l, e) =>
                // Let type inference infer the type by using the empty type
                l :+ q"val ${TermName(e.name.encodedName.toString)}: ${TypeTree()}"
              )

              q"(..$vals) => $companionObject.apply(..${applyParams.init}, ${applyParams.last}: _*)"
            } else if (tpeArgs.isEmpty) {
              q"$companionObject.apply _"
            } else q"$companionObject.apply[..$tpeArgs] _"

            (tree, app.typeParams.map(_.asType), params)
          }
        }

      lazy val unapplyFunction: Tree = if (tpeArgs.isEmpty) {
        q"$unlift($companionObject.$effectiveUnapply)"
      } else q"$unlift($companionObject.$effectiveUnapply[..$tpeArgs])"
    }

    val (applyFunction, tparams, params) = ApplyUnapply.applyFunction match {
      case Some(info) => info

      case _ => c.abort(c.enclosingPosition,
        s"No apply function found matching unapply parameters")
    }

    // Utility for implicit resolution - can hardly be moved outside
    // (due to dependent types)
    object ImplicitResolver {
      // Per each symbol of the type parameters, which type is bound to
      private val boundTypes: Map[String, Type] = tparams.zip(tpeArgs).map {
        case (sym, ty) => sym.fullName -> ty
      }.toMap

      // The placeholder type
      private val PlaceholderType: Type = typeOf[Placeholder]

      /* Refactor the input types, by replacing any type matching the `filter`,
       * by the given `replacement`.
       */
      @annotation.tailrec
      private def refactor(in: List[Type], base: TypeSymbol, out: List[Type], tail: List[(List[Type], TypeSymbol, List[Type])], filter: Type => Boolean, replacement: Type, altered: Boolean): (Type, Boolean) = in match {
        case tpe :: ts =>
          boundTypes.getOrElse(tpe.typeSymbol.fullName, tpe) match {
            case t if (filter(t)) =>
              refactor(ts, base, (replacement :: out), tail,
                filter, replacement, true)

            case TypeRef(_, sym, as) if as.nonEmpty =>
              refactor(as, sym.asType, List.empty, (ts, base, out) :: tail,
                filter, replacement, altered)

            case t => refactor(ts, base, (t :: out), tail,
              filter, replacement, altered)
          }

        case _ => {
          val tpe = appliedType(base, out.reverse)

          tail match {
            case (x, y, more) :: ts =>
              refactor(x, y, (tpe :: more), ts, filter, replacement, altered)

            case _ => tpe -> altered
          }
        }
      }

      /**
       * Replaces any reference to the type itself by the Placeholder type. 
       * @return the normalized type + whether any self reference has been found
       */
      private def normalized(tpe: Type): (Type, Boolean) =
        boundTypes.getOrElse(tpe.typeSymbol.fullName, tpe) match {
          case t if (t =:= atag.tpe) => PlaceholderType -> true

          case TypeRef(_, sym, args) if args.nonEmpty =>
            refactor(args, sym.asType, List.empty, List.empty,
              _ =:= atag.tpe, PlaceholderType, false)

          case t => t -> false
        }

      /* Restores reference to the type itself when Placeholder is found. */
      private def denormalized(ptype: Type): Type = ptype match {
        case PlaceholderType => atag.tpe

        case TypeRef(_, sym, args) if args.nonEmpty =>
          refactor(args, sym.asType, List.empty, List.empty,
            _ == PlaceholderType, atag.tpe, false)._1

        case _ => ptype
      }

      val forwardName = TermName(c.freshName("forward"))

      private object ImplicitTransformer extends Transformer {
        override def transform(tree: Tree): Tree = tree match {
          case tt: TypeTree =>
            super.transform(TypeTree(denormalized(tt.tpe)))

          case Select(Select(This(TypeName("JsMacroImpl")), t), sym) if (
            t.toString == "Placeholder" && sym.toString == "Format"
          ) => super.transform(q"$forwardName")

          case _ => super.transform(tree)
        }
      }

      private def createImplicit(name: Name, ptype: Type): Implicit = {
        val tpe = ptype match {
          case TypeRef(_, _, targ :: _) =>
            // Option[_] needs special treatment because we need to use XXXOpt
            if (ptype.typeConstructor <:< typeOf[Option[_]].typeConstructor) {
              targ
            } else ptype

          case SingleType(_, _) | TypeRef(_, _, _) => ptype
        }

        val (ntpe, selfRef) = normalized(tpe)
        val ptpe = boundTypes.get(ntpe.typeSymbol.fullName).getOrElse(ntpe)

        // infers implicit
        val neededImplicitType = appliedType(natag.tpe.typeConstructor, ptpe)
        val neededImplicit = if (!selfRef) {
          c.inferImplicitValue(neededImplicitType)
        } else c.untypecheck(
          // Reset the type attributes on the refactored tree for the implicit
          ImplicitTransformer.transform(
            c.inferImplicitValue(neededImplicitType))
        )

        Implicit(name, ptype, neededImplicit, tpe, selfRef)
      }

      // To print the implicit types in the compiler messages
      private def prettyType(t: Type): String =
        boundTypes.getOrElse(t.typeSymbol.fullName, t) match {
          case TypeRef(_, base, args) if args.nonEmpty => s"""${base.asType.fullName}[${args.map(prettyType(_)).mkString(", ")}]"""

          case t => t.typeSymbol.fullName
        }

      def apply(params: List[Symbol]): List[Implicit] = {
        val resolvedImplicits = params.map { param =>
          createImplicit(param.name, param.typeSignature)
        }
        val effectiveImplicits = if (ApplyUnapply.hasVarArgs) {
          val varArgsImplicit = createImplicit(
            resolvedImplicits.last.paramName,
            ApplyUnapply.unapplyReturnTypes.get.last)

          resolvedImplicits.init :+ varArgsImplicit
        } else resolvedImplicits

        // if any implicit is missing, abort
        val missingImplicits = effectiveImplicits.collect {
          case Implicit(_, t, EmptyTree/* ~= not found */, _, _) => t
        }

        if (missingImplicits.nonEmpty) {
          c.abort(c.enclosingPosition,
            s"No instance of ${natag.tpe.typeSymbol.fullName} is available for ${missingImplicits.map(prettyType(_)).mkString(", ")} in the implicit scope (Hint: if declared in the same file, make sure it's declared before)")
        }

        effectiveImplicits
      }

      // ---

      // Now we find all the implicits that we need
      final case class Implicit(
        paramName: Name,
        paramType: Type,
        neededImplicit: Tree,
        tpe: Type,
        selfRef: Boolean
      )
    }

    // combines all reads into CanBuildX
    val cfgName = TermName(c.freshName("config"))
    val resolvedImplicits = ImplicitResolver(params)
    val canBuild = resolvedImplicits.map {
      case ImplicitResolver.Implicit(name, pt, impl, _, _) =>
        // Equivalent to __ \ "name", but uses a naming scheme
        // of (String) => (String) to find the correct "name"
        val cn = c.Expr[String](
          q"$cfgName.naming(${name.decodedName.toString})")

        val jspathTree = q"""$JsPath \ $cn"""

        // If we're not recursive, simple, just invoke read/write/format
        // If we're an option, invoke the nullable version
        if (pt.typeConstructor <:< typeOf[Option[_]].typeConstructor) {
          q"$jspathTree.$callNullable($impl)"
        } else {
          q"$jspathTree.$call($impl)"
        }
    }.reduceLeft[Tree] { (acc, r) =>
      q"$acc.and($r)"
    }

    val multiParam = params.length > 1
    // if case class has one single field, needs to use map/contramap/inmap on the Reads/Writes/Format instead of
    // canbuild.apply
    val applyOrMap = TermName(if (multiParam) "apply" else mapLikeMethod)

    val syntaxImport = if (!multiParam && !writes) q"" else q"import $syntax._"
    val canBuildCall = q"$canBuild.$applyOrMap(..${conditionalList(applyFunction, ApplyUnapply.unapplyFunction)})"

    val finalTree =
      if (!resolvedImplicits.exists(_.selfRef)) {
        q"""
        $syntaxImport

        val $cfgName = implicitly[$json.JsonConfiguration]

        $canBuildCall
        """
      } else {
        // Has nested reference to the same type

        val forward: Tree = methodName match {
          case "read" =>
            q"$json.Reads[${atag.tpe}](instance.reads(_))"

          case "write" =>
            q"$json.OWrites[${atag.tpe}](instance.writes(_))"

          case _ =>
            q"$json.OFormat[${atag.tpe}](instance.reads(_), instance.writes(_))"
        }
        val forwardCall =
          q"private val ${ImplicitResolver.forwardName} = $forward"

        val generated = TypeName(c.freshName("Generated"))

        q"""
        final class $generated()(implicit $cfgName: $json.JsonConfiguration) {
          // wrap there for self reference

          $syntaxImport

          $forwardCall

          def instance: ${matag.tpe.typeSymbol}[${atag.tpe}] = $canBuildCall
        }

        new $generated().instance
        """
      }

    if (debugEnabled) {
      c.info(c.enclosingPosition, showCode(finalTree), force = true)
    }

    c.Expr[M[A]](finalTree)
  }

  private lazy val debugEnabled =
    Option(System.getProperty("play.json.macro.debug")).
      filterNot(_.isEmpty).map(_.toLowerCase).exists { v =>
        "true".equals(v) || v.substring(0, 1) == "y"
      }
}
