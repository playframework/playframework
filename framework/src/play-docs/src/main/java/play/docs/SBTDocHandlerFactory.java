package play.docs;

import java.io.File;
import java.util.jar.JarFile;

import play.api.mvc.RequestHeader;
import play.core.SBTDocHandler;
import play.doc.FileRepository;
import play.doc.FilesystemRepository;
import play.doc.JarRepository;
import scala.Option;

/**
 * Provides a way for SBT code to create SBTDocHandler objects.
 * <p/>
 * <p>This class is used by the Play SBT plugin run command (to serve
 * documentation from a JAR) and by the Play documentation project (to
 * serve documentation from the filesystem).
 * <p/>
 * <p>This class is written in Java and uses only Java types so that
 * communication can work even when the SBT code and the play-docs project
 * are built with different versions of Scala.
 */
public class SBTDocHandlerFactory {

    /**
     * Create an SBTDocHandler that serves documentation from a given directory by
     * wrapping a FilesystemRepository.
     *
     * @param directory The directory to serve the documentation from.
     */
    public static SBTDocHandler fromDirectory(File directory) {
        FileRepository repo = new FilesystemRepository(directory);
        return new DocumentationHandler(repo);
    }

    /**
     * Create an SBTDocHandler that serves the manual from a given directory by
     * wrapping a FilesystemRepository, and the API docs from a given JAR file by
     * wrapping a JarRepository
     *
     * @param directory The directory to serve the documentation from.
     * @param jarFile The JAR file to server the documentation from.
     * @param base    The directory within the JAR file to serve the documentation from, or null if the
     *                documentation should be served from the root of the JAR.
     */
    public static SBTDocHandler fromDirectoryAndJar(File directory, JarFile jarFile, String base) {
        FileRepository repo = new FilesystemRepository(directory);
        FileRepository apiRepo = new JarRepository(jarFile, Option.apply(base));
        return new DocumentationHandler(repo, apiRepo);
    }

    /**
     * Create an SBTDocHandler that serves documentation from a given JAR file by
     * wrapping a JarRepository.
     *
     * @param jarFile The JAR file to server the documentation from.
     * @param base    The directory within the JAR file to serve the documentation from, or null if the
     *                documentation should be served from the root of the JAR.
     */
    public static SBTDocHandler fromJar(JarFile jarFile, String base) {
        FileRepository repo = new JarRepository(jarFile, Option.apply(base));
        return new DocumentationHandler(repo);
    }

}