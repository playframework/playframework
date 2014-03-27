/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.mvc;

/**
 * This exists only for backwards compatibility.
 *
 * @deprecated Use play.api.mvc.Result instead
 */
@Deprecated
public interface SimpleResult {
    play.api.mvc.Result toScala();
}
