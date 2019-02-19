/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs

/**
 * Provides implicit type classes when you import the package.
 */
package object ws extends WSBodyReadables with WSBodyWritables
