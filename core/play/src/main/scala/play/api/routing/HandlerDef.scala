/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
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
    parameterTypes: Seq[Class[_]]
) extends play.routing.HandlerDef
