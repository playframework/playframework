/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs;

import scala.collection.JavaConverters;
import scala.util.Try;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Contains TemporaryFile and TemporaryFileCreator operations.
 */
public final class Files {

    /**
     * This creates temporary files when Play needs to keep overflow data on the filesystem.
     */
    public interface TemporaryFileCreator {
        TemporaryFile create(String prefix, String suffix);

        TemporaryFile create(Path path);

        boolean delete(TemporaryFile temporaryFile);

        // Needed for RawBuffer compatibility
        play.api.libs.Files.TemporaryFileCreator asScala();
    }

    /**
     * A temporary file created by a TemporaryFileCreator.
     */
    public interface TemporaryFile {

        /** @return the path to the temporary file. */
        Path path();

        /**
         * @return the temporaryFile as a java.io.File.
         * @deprecated Use path() over file().
         */
        @Deprecated
        File file();

        TemporaryFileCreator temporaryFileCreator();
    }

    /**
     * A temporary file creator that delegates to a Scala TemporaryFileCreator.
     */
    public static class DelegateTemporaryFileCreator implements TemporaryFileCreator {
        private final play.api.libs.Files.TemporaryFileCreator temporaryFileCreator;

        @Inject
        public DelegateTemporaryFileCreator(play.api.libs.Files.TemporaryFileCreator temporaryFileCreator) {
            this.temporaryFileCreator = temporaryFileCreator;
        }

        @Override
        public TemporaryFile create(String prefix, String suffix) {
            return new DelegateTemporaryFile(temporaryFileCreator.create(prefix, suffix));
        }

        @Override
        public TemporaryFile create(Path path) {
            return new DelegateTemporaryFile(temporaryFileCreator.create(path));
        }

        @Override
        public boolean delete(TemporaryFile temporaryFile) {
            play.api.libs.Files.TemporaryFile scalaFile = asScala().create(temporaryFile.path());
            Try<Object> tryValue = asScala().delete(scalaFile);
            return (Boolean) tryValue.get();
        }

        @Override
        public play.api.libs.Files.TemporaryFileCreator asScala() {
            return this.temporaryFileCreator;
        }
    }

    /**
     * Delegates to the Scala implementation.
     */
    public static class DelegateTemporaryFile implements TemporaryFile {

        private final play.api.libs.Files.TemporaryFile temporaryFile;
        private final TemporaryFileCreator temporaryFileCreator;

        DelegateTemporaryFile(play.api.libs.Files.TemporaryFile temporaryFile) {
            this.temporaryFile = temporaryFile;
            this.temporaryFileCreator = new DelegateTemporaryFileCreator(temporaryFile.temporaryFileCreator());
        }

        @Override
        public Path path() {
            return temporaryFile.path();
        }

        @Override
        public File file() {
            return temporaryFile.path().toFile();
        }

        @Override
        public TemporaryFileCreator temporaryFileCreator() {
            return temporaryFileCreator;
        }
    }

    /**
     * A temporary file creator that uses the Scala play.api.libs.Files.SingletonTemporaryFileCreator
     * class behind the scenes.
     */
    public static class SingletonTemporaryFileCreator implements TemporaryFileCreator {
        private play.api.libs.Files.SingletonTemporaryFileCreator$ instance = play.api.libs.Files.SingletonTemporaryFileCreator$.MODULE$;

        @Override
        public TemporaryFile create(String prefix, String suffix) {
            return new DelegateTemporaryFile(instance.create(prefix, suffix));
        }

        @Override
        public TemporaryFile create(Path path) {
            return new DelegateTemporaryFile(instance.create(path));
        }

        @Override
        public boolean delete(TemporaryFile temporaryFile) {
            play.api.libs.Files.TemporaryFile scalaFile = asScala().create(temporaryFile.path());
            Try<Object> tryValue = asScala().delete(scalaFile);
            return (Boolean) tryValue.get();
        }

        @Override
        public play.api.libs.Files.TemporaryFileCreator asScala() {
            return instance;
        }
    }

    private static final TemporaryFileCreator instance = new Files.SingletonTemporaryFileCreator();

    /**
     * @return the singleton instance of SingletonTemporaryFileCreator.
     */
    public static TemporaryFileCreator singletonTemporaryFileCreator() {
        return instance;
    }

}
