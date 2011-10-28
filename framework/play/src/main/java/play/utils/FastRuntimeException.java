package play.utils;

/**
 * Fast Exception - skips creating stackTrace.
 *
 * More info here: http://www.javaspecialists.eu/archive/Issue129.html
 */
public class FastRuntimeException extends RuntimeException {

    public FastRuntimeException(){
        super();
    }

    public FastRuntimeException( String desc){
        super(desc);
    }

    public FastRuntimeException(String desc, Throwable cause){
        super(desc, cause);
    }

    public FastRuntimeException(Throwable cause){
        super(cause);
    }

    /**
     * Since we override this method, no stacktrace is generated - much faster
     * @return always null
     */
    public Throwable fillInStackTrace() {
        return null;
    }
}
