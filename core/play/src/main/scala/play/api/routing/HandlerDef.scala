/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.routing

/**
 * Information about a `Handler`, especially useful for loading the handler
 * with reflection.
 */
case class HandlerDef(
    classLoader: ClassLoader,
    routerPackage: String,
    controller: String,
    method: String,
    parameterTypes: Seq[Class[?]],
    verb: String,
    path: String,
    comments: String = "",
    modifiers: Seq[String] = Seq.empty
) extends play.routing.HandlerDef
