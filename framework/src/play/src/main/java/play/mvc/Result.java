package play.mvc;

/**
 * Any action result.
 */
public interface Result {
    
    /**
     * Retrieves the real (Scala-based) result.
     */
    play.api.mvc.Result getWrappedResult();
    
}