/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.mvc;

/**
 * Any action result.
 */
public interface Result {
    
    /**
     * Retrieves the real (Scala-based) result.
     */
    scala.concurrent.Future<play.api.mvc.SimpleResult> getWrappedResult();
    
}