/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.docs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

import play.core.BuildDocHandler;
import play.doc.FileRepository;
import play.doc.FilesystemRepository;
import play.doc.JarRepository;
import scala.Option;

/**
 * Provides a way for build code to create BuildDocHandler objects.
 *
 * <p>This class is used by the Play build plugin run command (to serve
 * documentation from a JAR) and by the Play documentation project (to
 * serve documentation from the filesystem).
 *
 * <p>This class is written in Java and uses only Java types so that
 * communication can work even when the build code and the play-docs project
 * are built with different versions of Scala.
 */
public class BuildDocHandlerFactory {

    /**
     * Create a BuildDocHandler that serves documentation from the given files, which could either be directories
     * or jar files.  The baseDir array must be the same length as the files array, and the corresponding entry in there
     * for jar files is used as a base directory to use resources from in the jar.
     *
     * @param files The directories or jar files to serve documentation from.
     * @param baseDirs The base directories for the jar files.  Entries may be null.
     * @return a BuildDocHandler.
     */
    public static BuildDocHandler fromResources(File[] files, String[] baseDirs) throws IOException {
        assert(files.length == baseDirs.length);

        FileRepository[] repositories = new FileRepository[files.length];
        List<JarFile> jarFiles = new ArrayList<>();

        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            String baseDir = baseDirs[i];


            if (file.isDirectory()) {
                repositories[i] = new FilesystemRepository(file);
            } else {
                // Assume it's a jar file
                JarFile jarFile = new JarFile(file);
                jarFiles.add(jarFile);
                repositories[i] = new JarRepository(jarFile, Option.apply(baseDir));
            }
        }

        return new DocumentationHandler(new AggregateFileRepository(repositories), () -> {
            for (JarFile jarFile: jarFiles) {
                jarFile.close();
            }
        });
    }

    /**
     * Create an BuildDocHandler that serves documentation from a given directory by
     * wrapping a FilesystemRepository.
     *
     * @param directory The directory to serve the documentation from.
     */
    public static BuildDocHandler fromDirectory(File directory) {
        FileRepository repo = new FilesystemRepository(directory);
        return new DocumentationHandler(repo);
    }

    /**
     * Create an BuildDocHandler that serves the manual from a given directory by
     * wrapping a FilesystemRepository, and the API docs from a given JAR file by
     * wrapping a JarRepository
     *
     * @param directory The directory to serve the documentation from.
     * @param jarFile The JAR file to server the documentation from.
     * @param base    The directory within the JAR file to serve the documentation from, or null if the
     *                documentation should be served from the root of the JAR.
     */
    public static BuildDocHandler fromDirectoryAndJar(File directory, JarFile jarFile, String base) {
        return fromDirectoryAndJar(directory, jarFile, base, false);
    }

    /**
     * Create an BuildDocHandler that serves the manual from a given directory by
     * wrapping a FilesystemRepository, and the API docs from a given JAR file by
     * wrapping a JarRepository.
     *
     * @param directory The directory to serve the documentation from.
     * @param jarFile The JAR file to server the documentation from.
     * @param base    The directory within the JAR file to serve the documentation from, or null if the
     *                documentation should be served from the root of the JAR.
     * @param fallbackToJar Whether the doc handler should fall back to the jar repo for docs.
     */
    public static BuildDocHandler fromDirectoryAndJar(File directory, JarFile jarFile, String base, boolean fallbackToJar) {
        FileRepository fileRepo = new FilesystemRepository(directory);
        FileRepository jarRepo = new JarRepository(jarFile, Option.apply(base));
        FileRepository manualRepo;
        if (fallbackToJar) {
            manualRepo = new AggregateFileRepository(new FileRepository[] { fileRepo, jarRepo });
        } else {
            manualRepo = fileRepo;
        }

        return new DocumentationHandler(manualRepo, jarRepo);
    }

    /**
     * Create an BuildDocHandler that serves documentation from a given JAR file by
     * wrapping a JarRepository.
     *
     * @param jarFile The JAR file to server the documentation from.
     * @param base    The directory within the JAR file to serve the documentation from, or null if the
     *                documentation should be served from the root of the JAR.
     */
    public static BuildDocHandler fromJar(JarFile jarFile, String base) {
        FileRepository repo = new JarRepository(jarFile, Option.apply(base));
        return new DocumentationHandler(repo);
    }

    /**
     * Create a BuildDocHandler that doesn't do anything.
     * Used when the documentation jar file is not available.
     */
    public static BuildDocHandler empty() {
        return request -> Option.apply(null);
    }

}
