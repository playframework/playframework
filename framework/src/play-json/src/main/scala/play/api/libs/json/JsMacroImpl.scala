/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.json

import scala.language.higherKinds
import scala.reflect.macros._
import language.experimental.macros

object JsMacroImpl {

  def formatImpl[A: c.WeakTypeTag](c: Context): c.Expr[Format[A]] =
    macroImpl[A, Format](c, "format", "inmap", reads = true, writes = true)

  def readsImpl[A: c.WeakTypeTag](c: Context): c.Expr[Reads[A]] =
    macroImpl[A, Reads](c, "read", "map", reads = true, writes = false)

  def writesImpl[A: c.WeakTypeTag](c: Context): c.Expr[Writes[A]] =
    macroImpl[A, Writes](c, "write", "contramap", reads = false, writes = true)

  def writesWithGettersImpl[A: c.WeakTypeTag](c: Context): c.Expr[Writes[A]] = {
    import c.universe._

    // Create a few references expressions to where other parts of the JSON lib are
    val jsValueWrapperRef = q"play.api.libs.json.Json.toJsFieldJsValueWrapper"
    val writes = q"play.api.libs.json.Writes"
    val selfLazyRef = q"newWrites.lazyStuff"

    val typeToWrite = c.weakTypeOf[A]
    val getterMethodTerms = for {
      memberSymbol <- typeToWrite.members
      term = memberSymbol.asTerm
      if !term.isPrivate && term.isGetter
    } yield term

    // <-- Check for required implicits
    final case class Implicit(returnType: Type, neededImplicit: Tree, isRecursive: Boolean)

    val inferredImplicits = getterMethodTerms.map { methodTerm =>
      val implType = methodTerm.asMethod.returnType
      val (isRecursive, tpe) = implType match {
        case TypeRef(_, t, args) =>
          val isRec = args.exists(_.typeSymbol == typeToWrite.typeSymbol)
          val tp = if (implType.typeConstructor <:< typeOf[Option[_]].typeConstructor) args.head else implType
          (isRec, tp)
        case TypeRef(_, t, _) =>
          (false, implType)
      }

      // builds M implicit from expected type
      val neededImplicitType = appliedType(c.weakTypeOf[Writes[A]].typeConstructor, tpe :: Nil)
      // infers implicit
      val neededImplicit = c.inferImplicitValue(neededImplicitType)
      Implicit(implType, neededImplicit, isRecursive)
    }

    // if any implicit is missing, abort
    val missingImplicits = inferredImplicits.collect { case Implicit(t, impl, rec) if (impl == EmptyTree && !rec) => t }
    if (missingImplicits.nonEmpty)
      c.abort(c.enclosingPosition, s"No implicit format for ${missingImplicits.mkString(", ")} available.")
    // Check for required implicits -->

    // Creates proper JsValueWrappers based on the methodTerm
    def jsValueWrapper(methodTerm: c.universe.TermSymbol) = {
      val methodAccess = q"o.$methodTerm"
      val isRecursive = methodTerm.typeSignature.typeConstructor.contains(typeToWrite.typeSymbol)
      val finalWrapper = if (isRecursive) {
        val wrapWithOutWriter = q"$jsValueWrapperRef($methodAccess)"
        val methodReturnType = methodTerm.asMethod.returnType
        if (methodReturnType <:< typeOf[List[_]]) { q"$wrapWithOutWriter($writes.list($selfLazyRef))" }
        else if (methodReturnType <:< typeOf[Option[_]]) { q"$wrapWithOutWriter($writes.OptionWrites[$typeToWrite]($selfLazyRef))" }
        else if (methodReturnType <:< typeOf[Set[_]]) { q"$wrapWithOutWriter($writes.set($selfLazyRef))" }
        else if (methodReturnType <:< typeOf[Seq[_]]) { q"$wrapWithOutWriter($writes.seq($selfLazyRef))" }
        else if (methodReturnType <:< typeOf[Map[_, _]]) { q"$wrapWithOutWriter($writes.map($selfLazyRef))" }
        else { q"$wrapWithOutWriter($selfLazyRef)" }
      } else { methodAccess }
      finalWrapper
    }

    // Create the string -> JsValueWrapper pairs to feed to Json.obj
    val jsonObjDoubles = for {
      term <- getterMethodTerms
    } yield {
      val termName = term.asTerm.name.toString
      val wrapper = jsValueWrapper(term)
      q"""(${termName}, $wrapper)"""
    }

    val tree = q"""new play.api.libs.json.util.LazyHelper[Writes, ${tq"$typeToWrite"}] { newWrites =>
      override lazy val lazyStuff: Writes[${tq"$typeToWrite"}] = new Writes[${tq"$typeToWrite"}] {
        def writes(o: ${tq"$typeToWrite"}): JsValue = Json.obj(..$jsonObjDoubles)
      }
    }.lazyStuff"""

    // println(s"Tree: $tree")
    c.Expr[Writes[A]](tree)
  }

  def macroImpl[A, M[_]](c: Context, methodName: String, mapLikeMethod: String, reads: Boolean, writes: Boolean)(implicit atag: c.WeakTypeTag[A], matag: c.WeakTypeTag[M[A]]): c.Expr[M[A]] = {

    val nullableMethodName = s"${methodName}Nullable"
    val lazyMethodName = s"lazy${methodName.capitalize}"

    def conditionalList[T](ifReads: T, ifWrites: T): List[T] =
      (if (reads) List(ifReads) else Nil) :::
        (if (writes) List(ifWrites) else Nil)

    import c.universe._
    import c.universe.Flag._

    val companioned = weakTypeOf[A].typeSymbol
    val companionSymbol = companioned.companionSymbol
    val companionType = companionSymbol.typeSignature

    val libsPkg = Select(Select(Ident(newTermName("play")), newTermName("api")), newTermName("libs"))
    val jsonPkg = Select(libsPkg, newTermName("json"))
    val functionalSyntaxPkg = Select(Select(libsPkg, newTermName("functional")), newTermName("syntax"))
    val utilPkg = Select(jsonPkg, newTermName("util"))

    val jsPathSelect = Select(jsonPkg, newTermName("JsPath"))
    val readsSelect = Select(jsonPkg, newTermName("Reads"))
    val writesSelect = Select(jsonPkg, newTermName("Writes"))
    val unliftIdent = Select(functionalSyntaxPkg, newTermName("unlift"))
    val lazyHelperSelect = Select(utilPkg, newTypeName("LazyHelper"))

    val unapply =
      companionType.declaration(stringToTermName("unapply")) match {
        case NoSymbol => c.abort(c.enclosingPosition, "No unapply function found")
        case s => s.asMethod
      }

    val unapplyReturnTypes = unapply.returnType match {
      case TypeRef(_, _, Nil) =>
        c.abort(c.enclosingPosition, s"Unapply of ${companionSymbol} has no parameters. Are you using an empty case class?")
      case TypeRef(_, _, args) =>
        args.head match {
          case t @ TypeRef(_, _, Nil) => Some(List(t))
          case t @ TypeRef(_, _, args) =>
            if (t <:< typeOf[Option[_]]) Some(List(t))
            else if (t <:< typeOf[Seq[_]]) Some(List(t))
            else if (t <:< typeOf[Set[_]]) Some(List(t))
            else if (t <:< typeOf[Map[_, _]]) Some(List(t))
            else if (t <:< typeOf[Product]) Some(args)
          case _ => None
        }
      case _ => None
    }

    //println("Unapply return type:" + unapplyReturnTypes)

    val applies =
      companionType.declaration(stringToTermName("apply")) match {
        case NoSymbol => c.abort(c.enclosingPosition, "No apply function found")
        case s => s.asMethod.alternatives
      }

    // searches apply method corresponding to unapply
    val apply = applies.collectFirst {
      case (apply: MethodSymbol) if (apply.paramss.headOption.map(_.map(_.asTerm.typeSignature)) == unapplyReturnTypes) => apply
    }

    val params = apply match {
      case Some(apply) => apply.paramss.head //verify there is a single parameter group
      case None => c.abort(c.enclosingPosition, "No apply function found matching unapply parameters")
    }

    //println("apply found:" + apply)

    final case class Implicit(paramName: Name, paramType: Type, neededImplicit: Tree, isRecursive: Boolean, tpe: Type)

    val inferredImplicits = params.map { param =>

      val implType = param.typeSignature
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
      val neededImplicitType = appliedType(matag.tpe.typeConstructor, tpe :: Nil)
      // infers implicit
      val neededImplicit = c.inferImplicitValue(neededImplicitType)
      Implicit(param.name, implType, neededImplicit, isRecursive, tpe)
    }

    //println("Found implicits:"+inferredImplicits)

    // if any implicit is missing, abort
    val missingImplicits = inferredImplicits.collect { case Implicit(_, t, impl, rec, _) if (impl == EmptyTree && !rec) => t }
    if (missingImplicits.nonEmpty)
      c.abort(c.enclosingPosition, s"No implicit format for ${missingImplicits.mkString(", ")} available.")

    val helperMember = Select(This(tpnme.EMPTY), newTermName("lazyStuff"))
    def callHelper(target: Tree, methodName: String): Tree =
      Apply(Select(target, newTermName(methodName)), List(helperMember))
    def readsWritesHelper(methodName: String): List[Tree] =
      conditionalList(readsSelect, writesSelect).map(s => callHelper(s, methodName))

    var hasRec = false

    // combines all reads into CanBuildX
    val canBuild = inferredImplicits.map {
      case Implicit(name, t, impl, rec, tpe) =>
        // inception of (__ \ name).read(impl)
        val jspathTree = Apply(
          Select(jsPathSelect, newTermName(scala.reflect.NameTransformer.encode("\\"))),
          List(Literal(Constant(name.decoded)))
        )

        if (!rec) {
          val callMethod = if (t.typeConstructor <:< typeOf[Option[_]].typeConstructor) nullableMethodName else methodName
          Apply(
            Select(jspathTree, newTermName(callMethod)),
            List(impl)
          )
        } else {
          hasRec = true
          if (t.typeConstructor <:< typeOf[Option[_]].typeConstructor)
            Apply(
              Select(jspathTree, newTermName(nullableMethodName)),
              callHelper(Apply(jsPathSelect, Nil), lazyMethodName) :: Nil
            )
          else {
            Apply(
              Select(jspathTree, newTermName(lazyMethodName)),
              if (tpe.typeConstructor <:< typeOf[List[_]].typeConstructor)
                readsWritesHelper("list")
              else if (tpe.typeConstructor <:< typeOf[Set[_]].typeConstructor)
                readsWritesHelper("set")
              else if (tpe.typeConstructor <:< typeOf[Seq[_]].typeConstructor)
                readsWritesHelper("seq")
              else if (tpe.typeConstructor <:< typeOf[Map[_, _]].typeConstructor)
                readsWritesHelper("map")
              else List(helperMember)
            )
          }
        }
    }.reduceLeft { (acc, r) =>
      Apply(
        Select(acc, newTermName("and")),
        List(r)
      )
    }

    // builds the final M[A] using apply method
    //val applyMethod = Ident( companionSymbol )
    val applyMethod =
      Function(
        params.foldLeft(List[ValDef]())((l, e) =>
          l :+ ValDef(Modifiers(PARAM), newTermName(e.name.encoded), TypeTree(), EmptyTree)
        ),
        Apply(
          Select(Ident(companionSymbol), newTermName("apply")),
          params.foldLeft(List[Tree]())((l, e) =>
            l :+ Ident(newTermName(e.name.encoded))
          )
        )
      )

    val unapplyMethod = Apply(
      unliftIdent,
      List(
        Select(Ident(companionSymbol), unapply.name)
      )
    )

    // if case class has one single field, needs to use inmap instead of canbuild.apply
    val method = if (params.length > 1) "apply" else mapLikeMethod
    val finalTree = Apply(
      Select(canBuild, newTermName(method)),
      conditionalList(applyMethod, unapplyMethod)
    )
    //println("finalTree: "+finalTree)

    val importFunctionalSyntax = Import(functionalSyntaxPkg, List(ImportSelector(nme.WILDCARD, -1, null, -1)))
    if (!hasRec) {
      val block = Block(
        importFunctionalSyntax,
        finalTree
      )
      //println("block:"+block)
      c.Expr[M[A]](block)
    } else {
      val helper = newTermName("helper")
      val helperVal = ValDef(
        Modifiers(),
        helper,
        Ident(weakTypeOf[play.api.libs.json.util.LazyHelper[M, A]].typeSymbol),
        Apply(Ident(newTermName("LazyHelper")), List(finalTree))
      )

      val block = Select(
        Block(
          importFunctionalSyntax,
          ClassDef(
            Modifiers(Flag.FINAL),
            newTypeName("$anon"),
            List(),
            Template(
              List(
                AppliedTypeTree(
                  lazyHelperSelect,
                  List(
                    Ident(matag.tpe.typeSymbol),
                    Ident(atag.tpe.typeSymbol)
                  )
                )
              ),
              emptyValDef,
              List(
                DefDef(
                  Modifiers(),
                  nme.CONSTRUCTOR,
                  List(),
                  List(List()),
                  TypeTree(),
                  Block(
                    Apply(
                      Select(Super(This(tpnme.EMPTY), tpnme.EMPTY), nme.CONSTRUCTOR),
                      List()
                    )
                  )
                ),
                ValDef(
                  Modifiers(Flag.OVERRIDE | Flag.LAZY),
                  newTermName("lazyStuff"),
                  AppliedTypeTree(Ident(matag.tpe.typeSymbol), List(TypeTree(atag.tpe))),
                  finalTree
                )
              )
            )
          ),
          Apply(Select(New(Ident(newTypeName("$anon"))), nme.CONSTRUCTOR), List())
        ),
        newTermName("lazyStuff")
      )

      //println("block:"+block)

      c.Expr[M[A]](block)
    }
  }
}
