/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.mvc;

/**
 * Any action result.
 */
public interface Result extends SimpleResult {
    
    /**
     * Retrieves the real (Scala-based) result.
     */
    play.api.mvc.Result toScala();
}