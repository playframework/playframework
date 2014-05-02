/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package anorm

/**
 * Marker trait to indicate that even if a type T accept null as value,
 * it must be refused in some Anorm context.
 */
trait NotNullGuard