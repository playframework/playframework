package play.mvc;

/**
 * Any action result.
 */
public interface Result {
    
    /**
     * Retrieve the real (scala based) result.
     */
    play.api.mvc.Result getWrappedResult();
    
}