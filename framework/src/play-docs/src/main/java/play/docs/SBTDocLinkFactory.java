package play.docs;

import java.io.File;
import java.util.jar.JarFile;

import play.api.mvc.RequestHeader;
import play.core.SBTDocLink;
import play.doc.FileRepository;
import play.doc.FilesystemRepository;
import play.doc.JarRepository;
import scala.Option;

/**
 * Provides a way for SBT code to create SBTDocLink objects.
 *
 * <p>This class is used by the Play SBT plugin run command (to serve
 * documentation from a JAR) and by the Play documentation project (to
 * server documentation from the filesystem).
 *
 * <p>This class is written in Java and uses only Java types so that
 * communication can work even when the SBT code and the play-docs project
 * are built with different versions of Scala.
 */
public class SBTDocLinkFactory {

  /**
   * Create an SBTDocLink that serves documentation from a given directory by
   * wrapping a FilesystemRepository.
   *
   * @param directory The directory to serve the documentation from.
   */
  public static SBTDocLink fromDirectory(File directory) {
    FileRepository repo = new FilesystemRepository(directory);
    final DocumentationHandler handler = new DocumentationHandler(repo);
    return new SBTDocLink() {
      public Object maybeHandleDocRequest(Object request) {
        return handler.maybeHandleRequest((RequestHeader) request);
      }
    };
  }

  /**
   * Create an SBTDocLink that serves documentation from a given JAR file by
   * wrapping a JarRepository.
   *
   * @param jarFile The JAR file to server the documentation from.
   * @param base The directory within the JAR file to serve the documentation from, or null if the
   *    documentation should be served from the root of the JAR.
   */
  public static SBTDocLink fromJar(final JarFile jarFile, String base) {
    FileRepository repo = new JarRepository(jarFile, Option.apply(base));
    final DocumentationHandler handler = new DocumentationHandler(repo);
    return new SBTDocLink() {
      public Object maybeHandleDocRequest(Object request) {
        return handler.maybeHandleRequest((RequestHeader) request);
      }
    };
  }

}