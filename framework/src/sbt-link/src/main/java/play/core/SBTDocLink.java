package play.core;

/**
 * Interface used by the Play SBT code to communicate with an embedded DocumentationApplication.
 * This occurs happens when SBT runs a Play server in development mode and from within the
 * Play documentation project.
 *
 * <p>SBTLink objects are created by calling the static methods on SBTDocLinkFactory.
 *
 * <p>This interface is written in Java and uses only Java types so that
 * communication can work even when the calling code and the play-docs project
 * are built with different versions of Scala.
 */
public interface SBTDocLink {

  /**
   * Given a request, either handle it and return some result, or don't, and return none.
   *
   * @param request A request of type {@link play.api.mvc.RequestHeader}.
   * @return A value of type {@link Option<play.api.mvc.SimpleResult>}, Some if the result was
   *        handled, None otherwise.
   */
  public Object maybeHandleDocRequest(Object request);

}