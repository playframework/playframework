package play.api.libs.json

import scala.language.reflectiveCalls
import scala.reflect.macros.Context
import language.experimental.macros

object JsMacroImpl {
  def readsImpl[A: c.WeakTypeTag](c: Context): c.Expr[Reads[A]] = {
    import c.universe._
    import c.universe.Flag._

    val companioned = weakTypeOf[A].typeSymbol
    val companionSymbol = companioned.companionSymbol
    val companionType = companionSymbol.typeSignature

    val libsPkg = Select(Select(Ident(newTermName("play")), "api"), "libs")
    val jsonPkg = Select(libsPkg, "json")
    val functionalSyntaxPkg = Select(Select(libsPkg, "functional"), "syntax")
    val utilPkg = Select(jsonPkg, "util")

    val jsPathSelect = Select(jsonPkg, "JsPath")
    val readsSelect = Select(jsonPkg, "Reads")
    val unliftIdent = Select(functionalSyntaxPkg, "unlift")
    val lazyHelperSelect = Select(utilPkg, newTypeName("LazyHelper"))

    companionType.declaration(stringToTermName("unapply")) match {
      case NoSymbol => c.abort(c.enclosingPosition, "No unapply function found")
      case s =>
        val unapply = s.asMethod
        val unapplyReturnTypes = unapply.returnType match {
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

        //println("Unapply return type:" + unapply.returnType)

        companionType.declaration(stringToTermName("apply")) match {
          case NoSymbol => c.abort(c.enclosingPosition, "No apply function found")
          case s =>
            // searches apply method corresponding to unapply
            val applies = s.asMethod.alternatives
            val apply = applies.collectFirst {
              case (apply: MethodSymbol) if (apply.paramss.headOption.map(_.map(_.asTerm.typeSignature)) == unapplyReturnTypes) => apply
            }
            apply match {
              case Some(apply) =>
                //println("apply found:" + apply)    
                val params = apply.paramss.head //verify there is a single parameter group

                val inferedImplicits = params.map(_.typeSignature).map { implType =>

                  val (isRecursive, tpe) = implType match {
                    case TypeRef(_, t, args) =>
                      // Option[_] needs special treatment because we need to use XXXOpt
                      if (implType.typeConstructor <:< typeOf[Option[_]].typeConstructor)
                        (args.exists { a => a.typeSymbol == companioned }, args.head)
                      else (args.exists { a => a.typeSymbol == companioned }, implType)
                    case TypeRef(_, t, _) =>
                      (false, implType)
                  }

                  // builds reads implicit from expected type
                  val neededImplicitType = appliedType(weakTypeOf[Reads[_]].typeConstructor, tpe :: Nil)
                  // infers implicit
                  val neededImplicit = c.inferImplicitValue(neededImplicitType)
                  (implType, neededImplicit, isRecursive, tpe)
                }

                // if any implicit is missing, abort
                // else goes on
                inferedImplicits.collect { case (t, impl, rec, _) if (impl == EmptyTree && !rec) => t } match {
                  case List() =>
                    val namedImplicits = params.map(_.name).zip(inferedImplicits)
                    //println("Found implicits:"+namedImplicits)

                    val helperMember = Select(This(tpnme.EMPTY), "lazyStuff")

                    var hasRec = false

                    // combines all reads into CanBuildX
                    val canBuild = namedImplicits.map {
                      case (name, (t, impl, rec, tpe)) =>
                        // inception of (__ \ name).read(impl)
                        val jspathTree = Apply(
                          Select(jsPathSelect, scala.reflect.NameTransformer.encode("\\")),
                          List(Literal(Constant(name.decoded)))
                        )

                        if (!rec) {
                          val readTree =
                            if (t.typeConstructor <:< typeOf[Option[_]].typeConstructor)
                              Apply(
                                Select(jspathTree, "readNullable"),
                                List(impl)
                              )
                            else Apply(
                              Select(jspathTree, "read"),
                              List(impl)
                            )

                          readTree
                        } else {
                          hasRec = true
                          val readTree =
                            if (t.typeConstructor <:< typeOf[Option[_]].typeConstructor)
                              Apply(
                                Select(jspathTree, "readNullable"),
                                List(
                                  Apply(
                                    Select(Apply(jsPathSelect, List()), "lazyRead"),
                                    List(helperMember)
                                  )
                                )
                              )

                            else {
                              Apply(
                                Select(jspathTree, "lazyRead"),
                                if (tpe.typeConstructor <:< typeOf[List[_]].typeConstructor)
                                  List(
                                  Apply(
                                    Select(readsSelect, "list"),
                                    List(helperMember)
                                  )
                                )
                                else if (tpe.typeConstructor <:< typeOf[Set[_]].typeConstructor)
                                  List(
                                  Apply(
                                    Select(readsSelect, "set"),
                                    List(helperMember)
                                  )
                                )
                                else if (tpe.typeConstructor <:< typeOf[Seq[_]].typeConstructor)
                                  List(
                                  Apply(
                                    Select(readsSelect, "seq"),
                                    List(helperMember)
                                  )
                                )
                                else if (tpe.typeConstructor <:< typeOf[Map[_, _]].typeConstructor)
                                  List(
                                  Apply(
                                    Select(readsSelect, "map"),
                                    List(helperMember)
                                  )
                                )
                                else List(helperMember)
                              )
                            }

                          readTree
                        }
                    }.reduceLeft { (acc, r) =>
                      Apply(
                        Select(acc, "and"),
                        List(r)
                      )
                    }

                    // builds the final Reads using apply method
                    val applyMethod =
                      Function(
                        params.foldLeft(List[ValDef]())((l, e) =>
                          l :+ ValDef(Modifiers(PARAM), newTermName(e.name.encoded), TypeTree(), EmptyTree)
                        ),
                        Apply(
                          Select(Ident(companionSymbol.name), newTermName("apply")),
                          params.foldLeft(List[Tree]())((l, e) =>
                            l :+ Ident(newTermName(e.name.encoded))
                          )
                        )
                      )

                    val unapplyMethod = Apply(
                      unliftIdent,
                      List(
                        Select(Ident(companionSymbol.name), unapply.name)
                      )
                    )

                    // if case class has one single field, needs to use inmap instead of canbuild.apply
                    val finalTree = if (params.length > 1) {
                      Apply(
                        Select(canBuild, "apply"),
                        List(applyMethod)
                      )
                    } else {
                      Apply(
                        Select(canBuild, "map"),
                        List(applyMethod)
                      )
                    }
                    //println("finalTree: "+finalTree)

                    if (!hasRec) {
                      val block = Block(
                        Import(functionalSyntaxPkg, List(ImportSelector(nme.WILDCARD, -1, null, -1))),
                        finalTree
                      )

                      //println("block:"+block)

                      /*val reif = reify(
                        /*new play.api.libs.json.util.LazyHelper[Format, A] {
                          override lazy val lazyStuff: Format[A] = null
                        }*/
                      )
                      println("RAW:"+showRaw(reif.tree, printKinds = true))*/

                      c.Expr[Reads[A]](block)
                    } else {
                      val helper = newTermName("helper")
                      val helperVal = ValDef(
                        Modifiers(),
                        helper,
                        TypeTree(weakTypeOf[play.api.libs.json.util.LazyHelper[Reads, A]]),
                        Apply(lazyHelperSelect, List(finalTree))
                      )

                      val block = Select(
                        Block(
                          Import(functionalSyntaxPkg, List(ImportSelector(nme.WILDCARD, -1, null, -1))),
                          ClassDef(
                            Modifiers(Flag.FINAL),
                            newTypeName("$anon"),
                            List(),
                            Template(
                              List(
                                AppliedTypeTree(
                                  lazyHelperSelect,
                                  List(
                                    Ident(weakTypeOf[Reads[A]].typeSymbol),
                                    Ident(weakTypeOf[A].typeSymbol)
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
                                  AppliedTypeTree(Ident(weakTypeOf[Reads[A]].typeSymbol), List(TypeTree(weakTypeOf[A]))),
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

                      c.Expr[Reads[A]](block)
                    }
                  case l => c.abort(c.enclosingPosition, s"No implicit Reads for ${l.mkString(", ")} available.")
                }

              case None => c.abort(c.enclosingPosition, "No apply function found matching unapply return types")
            }

        }
    }
  }

  def writesImpl[A: c.WeakTypeTag](c: Context): c.Expr[Writes[A]] = {
    import c.universe._
    import c.universe.Flag._

    val companioned = weakTypeOf[A].typeSymbol
    val companionSymbol = companioned.companionSymbol
    val companionType = companionSymbol.typeSignature

    val libsPkg = Select(Select(Ident(newTermName("play")), "api"), "libs")
    val jsonPkg = Select(libsPkg, "json")
    val functionalSyntaxPkg = Select(Select(libsPkg, "functional"), "syntax")
    val utilPkg = Select(jsonPkg, "util")

    val jsPathSelect = Select(jsonPkg, "JsPath")
    val writesSelect = Select(jsonPkg, "Writes")
    val unliftIdent = Select(functionalSyntaxPkg, "unlift")
    val lazyHelperSelect = Select(utilPkg, newTypeName("LazyHelper"))

    companionType.declaration(stringToTermName("unapply")) match {
      case NoSymbol => c.abort(c.enclosingPosition, "No unapply function found")
      case s =>
        val unapply = s.asMethod
        val unapplyReturnTypes = unapply.returnType match {
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

        companionType.declaration(stringToTermName("apply")) match {
          case NoSymbol => c.abort(c.enclosingPosition, "No apply function found")
          case s =>
            // searches apply method corresponding to unapply
            val applies = s.asMethod.alternatives
            val apply = applies.collectFirst {
              case (apply: MethodSymbol) if (apply.paramss.headOption.map(_.map(_.asTerm.typeSignature)) == unapplyReturnTypes) => apply
            }
            apply match {
              case Some(apply) =>
                //println("apply found:" + apply)    
                val params = apply.paramss.head //verify there is a single parameter group

                val inferedImplicits = params.map(_.typeSignature).map { implType =>

                  val (isRecursive, tpe) = implType match {
                    case TypeRef(_, t, args) =>
                      // Option[_] needs special treatment because we need to use XXXOpt
                      if (implType.typeConstructor <:< typeOf[Option[_]].typeConstructor)
                        (args.exists { a => a.typeSymbol == companioned }, args.head)
                      else (args.exists { a => a.typeSymbol == companioned }, implType)
                    case TypeRef(_, t, _) =>
                      (false, implType)
                  }

                  // builds reads implicit from expected type
                  val neededImplicitType = appliedType(weakTypeOf[Writes[_]].typeConstructor, tpe :: Nil)
                  // infers implicit
                  val neededImplicit = c.inferImplicitValue(neededImplicitType)
                  (implType, neededImplicit, isRecursive, tpe)
                }

                // if any implicit is missing, abort
                // else goes on
                inferedImplicits.collect { case (t, impl, rec, _) if (impl == EmptyTree && !rec) => t } match {
                  case List() =>
                    val namedImplicits = params.map(_.name).zip(inferedImplicits)
                    //println("Found implicits:"+namedImplicits)

                    val helperMember = Select(This(tpnme.EMPTY), "lazyStuff")

                    var hasRec = false

                    // combines all reads into CanBuildX
                    val canBuild = namedImplicits.map {
                      case (name, (t, impl, rec, tpe)) =>
                        // inception of (__ \ name).read(impl)
                        val jspathTree = Apply(
                          Select(jsPathSelect, scala.reflect.NameTransformer.encode("\\")),
                          List(Literal(Constant(name.decoded)))
                        )

                        if (!rec) {
                          val writesTree =
                            if (t.typeConstructor <:< typeOf[Option[_]].typeConstructor)
                              Apply(
                                Select(jspathTree, "writeNullable"),
                                List(impl)
                              )
                            else Apply(
                              Select(jspathTree, "write"),
                              List(impl)
                            )

                          writesTree
                        } else {
                          hasRec = true
                          val writesTree =
                            if (t.typeConstructor <:< typeOf[Option[_]].typeConstructor)
                              Apply(
                                Select(jspathTree, "writeNullable"),
                                List(
                                  Apply(
                                    Select(Apply(jsPathSelect, List()), "lazyWrite"),
                                    List(helperMember)
                                  )
                                )
                              )

                            else {
                              Apply(
                                Select(jspathTree, "lazyWrite"),
                                if (tpe.typeConstructor <:< typeOf[List[_]].typeConstructor)
                                  List(
                                  Apply(
                                    Select(writesSelect, "list"),
                                    List(helperMember)
                                  )
                                )
                                else if (tpe.typeConstructor <:< typeOf[Set[_]].typeConstructor)
                                  List(
                                  Apply(
                                    Select(writesSelect, "set"),
                                    List(helperMember)
                                  )
                                )
                                else if (tpe.typeConstructor <:< typeOf[Seq[_]].typeConstructor)
                                  List(
                                  Apply(
                                    Select(writesSelect, "seq"),
                                    List(helperMember)
                                  )
                                )
                                else if (tpe.typeConstructor <:< typeOf[Map[_, _]].typeConstructor)
                                  List(
                                  Apply(
                                    Select(writesSelect, "map"),
                                    List(helperMember)
                                  )
                                )
                                else List(helperMember)
                              )
                            }

                          writesTree
                        }
                    }.reduceLeft { (acc, r) =>
                      Apply(
                        Select(acc, "and"),
                        List(r)
                      )
                    }

                    // builds the final Reads using apply method
                    //val applyMethod = Ident( companionSymbol.name )
                    val applyMethod =
                      Function(
                        params.foldLeft(List[ValDef]())((l, e) =>
                          l :+ ValDef(Modifiers(PARAM), newTermName(e.name.encoded), TypeTree(), EmptyTree)
                        ),
                        Apply(
                          Select(Ident(companionSymbol.name), newTermName("apply")),
                          params.foldLeft(List[Tree]())((l, e) =>
                            l :+ Ident(newTermName(e.name.encoded))
                          )
                        )
                      )

                    val unapplyMethod = Apply(
                      unliftIdent,
                      List(
                        Select(Ident(companionSymbol.name), unapply.name)
                      )
                    )

                    // if case class has one single field, needs to use inmap instead of canbuild.apply
                    val finalTree = if (params.length > 1) {
                      Apply(
                        Select(canBuild, "apply"),
                        List(unapplyMethod)
                      )
                    } else {
                      Apply(
                        Select(canBuild, "contramap"),
                        List(unapplyMethod)
                      )
                    }
                    //println("finalTree: "+finalTree)

                    if (!hasRec) {
                      val block = Block(
                        Import(functionalSyntaxPkg, List(ImportSelector(nme.WILDCARD, -1, null, -1))),
                        finalTree
                      )
                      //println("block:"+block)
                      c.Expr[Writes[A]](block)
                    } else {
                      val helper = newTermName("helper")
                      val helperVal = ValDef(
                        Modifiers(),
                        helper,
                        TypeTree(weakTypeOf[play.api.libs.json.util.LazyHelper[Writes, A]]),
                        Apply(lazyHelperSelect, List(finalTree))
                      )

                      val block = Select(
                        Block(
                          Import(functionalSyntaxPkg, List(ImportSelector(nme.WILDCARD, -1, null, -1))),
                          ClassDef(
                            Modifiers(Flag.FINAL),
                            newTypeName("$anon"),
                            List(),
                            Template(
                              List(
                                AppliedTypeTree(
                                  lazyHelperSelect,
                                  List(
                                    Ident(weakTypeOf[Writes[A]].typeSymbol),
                                    Ident(weakTypeOf[A].typeSymbol)
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
                                  AppliedTypeTree(Ident(weakTypeOf[Writes[A]].typeSymbol), List(TypeTree(weakTypeOf[A]))),
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

                      /*val reif = reify(
                        new play.api.libs.json.util.LazyHelper[Format, A] {
                          override lazy val lazyStuff: Format[A] = null
                        }
                      )
                      //println("RAW:"+showRaw(reif.tree, printKinds = true))*/
                      c.Expr[Writes[A]](block)
                    }
                  case l => c.abort(c.enclosingPosition, s"No implicit Writes for ${l.mkString(", ")} available.")
                }

              case None => c.abort(c.enclosingPosition, "No apply function found matching unapply parameters")
            }

        }
    }
  }

  def formatImpl[A: c.WeakTypeTag](c: Context): c.Expr[Format[A]] = {
    import c.universe._
    import c.universe.Flag._

    val companioned = weakTypeOf[A].typeSymbol
    val companionSymbol = companioned.companionSymbol
    val companionType = companionSymbol.typeSignature

    val libsPkg = Select(Select(Ident(newTermName("play")), "api"), "libs")
    val jsonPkg = Select(libsPkg, "json")
    val functionalSyntaxPkg = Select(Select(libsPkg, "functional"), "syntax")
    val utilPkg = Select(jsonPkg, "util")

    val jsPathSelect = Select(jsonPkg, "JsPath")
    val readsSelect = Select(jsonPkg, "Reads")
    val writesSelect = Select(jsonPkg, "Writes")
    val unliftIdent = Select(functionalSyntaxPkg, "unlift")
    val lazyHelperSelect = Select(utilPkg, newTypeName("LazyHelper"))

    companionType.declaration(stringToTermName("unapply")) match {
      case NoSymbol => c.abort(c.enclosingPosition, "No unapply function found")
      case s =>
        val unapply = s.asMethod
        val unapplyReturnTypes = unapply.returnType match {
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

        companionType.declaration(stringToTermName("apply")) match {
          case NoSymbol => c.abort(c.enclosingPosition, "No apply function found")
          case s =>
            // searches apply method corresponding to unapply
            val applies = s.asMethod.alternatives
            val apply = applies.collectFirst {
              case (apply: MethodSymbol) if (apply.paramss.headOption.map(_.map(_.asTerm.typeSignature)) == unapplyReturnTypes) => apply
            }
            apply match {
              case Some(apply) =>
                //println("apply found:" + apply)    
                val params = apply.paramss.head //verify there is a single parameter group

                val inferedImplicits = params.map(_.typeSignature).map { implType =>

                  val (isRecursive, tpe) = implType match {
                    case TypeRef(_, t, args) =>
                      // Option[_] needs special treatment because we need to use XXXOpt
                      if (implType.typeConstructor <:< typeOf[Option[_]].typeConstructor)
                        (args.exists { a => a.typeSymbol == companioned }, args.head)
                      else (args.exists { a => a.typeSymbol == companioned }, implType)
                    case TypeRef(_, t, _) =>
                      (false, implType)
                  }

                  // builds reads implicit from expected type
                  val neededImplicitType = appliedType(weakTypeOf[Format[_]].typeConstructor, tpe :: Nil)
                  // infers implicit
                  val neededImplicit = c.inferImplicitValue(neededImplicitType)
                  (implType, neededImplicit, isRecursive, tpe)
                }

                // if any implicit is missing, abort
                // else goes on
                inferedImplicits.collect { case (t, impl, rec, _) if (impl == EmptyTree && !rec) => t } match {
                  case List() =>
                    val namedImplicits = params.map(_.name).zip(inferedImplicits)
                    //println("Found implicits:"+namedImplicits)

                    val helperMember = Select(This(tpnme.EMPTY), "lazyStuff")

                    var hasRec = false

                    // combines all reads into CanBuildX
                    val canBuild = namedImplicits.map {
                      case (name, (t, impl, rec, tpe)) =>
                        // inception of (__ \ name).read(impl)
                        val jspathTree = Apply(
                          Select(jsPathSelect, scala.reflect.NameTransformer.encode("\\")),
                          List(Literal(Constant(name.decoded)))
                        )

                        if (!rec) {
                          val formatTree =
                            if (t.typeConstructor <:< typeOf[Option[_]].typeConstructor)
                              Apply(
                                Select(jspathTree, "formatNullable"),
                                List(impl)
                              )
                            else Apply(
                              Select(jspathTree, "format"),
                              List(impl)
                            )

                          formatTree
                        } else {
                          hasRec = true
                          val formatTree =
                            if (t.typeConstructor <:< typeOf[Option[_]].typeConstructor)
                              Apply(
                                Select(jspathTree, "formatNullable"),
                                List(
                                  Apply(
                                    Select(Apply(jsPathSelect, List()), "lazyFormat"),
                                    List(helperMember)
                                  )
                                )
                              )

                            else {
                              Apply(
                                Select(jspathTree, "lazyFormat"),
                                if (tpe.typeConstructor <:< typeOf[List[_]].typeConstructor)
                                  List(
                                  Apply(
                                    Select(readsSelect, "list"),
                                    List(helperMember)
                                  ),
                                  Apply(
                                    Select(writesSelect, "list"),
                                    List(helperMember)
                                  )
                                )
                                else if (tpe.typeConstructor <:< typeOf[Set[_]].typeConstructor)
                                  List(
                                  Apply(
                                    Select(readsSelect, "set"),
                                    List(helperMember)
                                  ),
                                  Apply(
                                    Select(writesSelect, "set"),
                                    List(helperMember)
                                  )
                                )
                                else if (tpe.typeConstructor <:< typeOf[Seq[_]].typeConstructor)
                                  List(
                                  Apply(
                                    Select(readsSelect, "seq"),
                                    List(helperMember)
                                  ),
                                  Apply(
                                    Select(writesSelect, "seq"),
                                    List(helperMember)
                                  )
                                )
                                else if (tpe.typeConstructor <:< typeOf[Map[_, _]].typeConstructor)
                                  List(
                                  Apply(
                                    Select(readsSelect, "map"),
                                    List(helperMember)
                                  ),
                                  Apply(
                                    Select(writesSelect, "map"),
                                    List(helperMember)
                                  )
                                )
                                else List(helperMember)
                              )
                            }

                          formatTree
                        }
                    }.reduceLeft { (acc, r) =>
                      Apply(
                        Select(acc, "and"),
                        List(r)
                      )
                    }

                    // builds the final Reads using apply method
                    //val applyMethod = Ident( companionSymbol.name )
                    val applyMethod =
                      Function(
                        params.foldLeft(List[ValDef]())((l, e) =>
                          l :+ ValDef(Modifiers(PARAM), newTermName(e.name.encoded), TypeTree(), EmptyTree)
                        ),
                        Apply(
                          Select(Ident(companionSymbol.name), newTermName("apply")),
                          params.foldLeft(List[Tree]())((l, e) =>
                            l :+ Ident(newTermName(e.name.encoded))
                          )
                        )
                      )

                    val unapplyMethod = Apply(
                      unliftIdent,
                      List(
                        Select(Ident(companionSymbol.name), unapply.name)
                      )
                    )

                    // if case class has one single field, needs to use inmap instead of canbuild.apply
                    val finalTree = if (params.length > 1) {
                      Apply(
                        Select(canBuild, "apply"),
                        List(applyMethod, unapplyMethod)
                      )
                    } else {
                      Apply(
                        Select(canBuild, "inmap"),
                        List(applyMethod, unapplyMethod)
                      )
                    }
                    //println("finalTree: "+finalTree)

                    if (!hasRec) {
                      val block = Block(
                        Import(functionalSyntaxPkg, List(ImportSelector(nme.WILDCARD, -1, null, -1))),
                        finalTree
                      )
                      //println("block:"+block)
                      c.Expr[Format[A]](block)
                    } else {
                      val helper = newTermName("helper")
                      val helperVal = ValDef(
                        Modifiers(),
                        helper,
                        Ident(weakTypeOf[play.api.libs.json.util.LazyHelper[Format, A]].typeSymbol),
                        Apply(Ident(newTermName("LazyHelper")), List(finalTree))
                      )

                      val block = Select(
                        Block(
                          Import(functionalSyntaxPkg, List(ImportSelector(nme.WILDCARD, -1, null, -1))),
                          ClassDef(
                            Modifiers(Flag.FINAL),
                            newTypeName("$anon"),
                            List(),
                            Template(
                              List(
                                AppliedTypeTree(
                                  lazyHelperSelect,
                                  List(
                                    Ident(weakTypeOf[Format[A]].typeSymbol),
                                    Ident(weakTypeOf[A].typeSymbol)
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
                                  AppliedTypeTree(Ident(weakTypeOf[Format[A]].typeSymbol), List(TypeTree(weakTypeOf[A]))),
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

                      /*val reif = reify(
                        new play.api.libs.json.util.LazyHelper[Format, A] {
                          override lazy val lazyStuff: Format[A] = null
                        }
                      )
                      //println("RAW:"+showRaw(reif.tree, printKinds = true))*/
                      c.Expr[Format[A]](block)
                    }
                  case l => c.abort(c.enclosingPosition, s"No implicit format for ${l.mkString(", ")} available.")
                }

              case None => c.abort(c.enclosingPosition, "No apply function found matching unapply parameters")
            }

        }
    }
  }

}