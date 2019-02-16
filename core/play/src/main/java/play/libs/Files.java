/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs;

import scala.util.Try;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.CopyOption;
import java.nio.file.Path;

/** Contains TemporaryFile and TemporaryFileCreator operations. */
public final class Files {

  /** This creates temporary files when Play needs to keep overflow data on the filesystem. */
  public interface TemporaryFileCreator {
    TemporaryFile create(String prefix, String suffix);

    TemporaryFile create(Path path);

    boolean delete(TemporaryFile temporaryFile);

    // Needed for RawBuffer compatibility
    play.api.libs.Files.TemporaryFileCreator asScala();
  }

  /** A temporary file created by a TemporaryFileCreator. */
  public interface TemporaryFile {

    /** @return the path to the temporary file. */
    Path path();

    TemporaryFileCreator temporaryFileCreator();

    /**
     * Copy the temporary file to the specified destination.
     *
     * @param destination the file destination.
     * @see #copyTo(Path, boolean)
     */
    default Path copyTo(File destination) {
      return copyTo(destination, false);
    }

    /**
     * Copy the file to the specified destination and, if the destination exists, decide if replace
     * it based on the {@code replace} parameter.
     *
     * @param destination the file destination.
     * @param replace if it should replace an existing file.
     * @see #copyTo(Path, boolean)
     */
    default Path copyTo(File destination, boolean replace) {
      return copyTo(destination.toPath(), replace);
    }

    /**
     * Copy the file to the specified path destination.
     *
     * @param destination the path destination.
     * @see #copyTo(Path, boolean)
     */
    default Path copyTo(Path destination) {
      return copyTo(destination, false);
    }

    /**
     * Copy the file to the specified path destination and, if the destination exists, decide if
     * replace it based on the {@code replace} parameter.
     *
     * @param destination the path destination.
     * @param replace if it should replace an existing file.
     */
    Path copyTo(Path destination, boolean replace);

    /**
     * Move the file using a {@link java.io.File}.
     *
     * @param destination the path to the destination file
     * @see #moveFileTo(Path, boolean)
     */
    default Path moveFileTo(File destination) {
      return moveFileTo(destination, false);
    }

    /**
     * Move the file to the specified destination {@link java.io.File}. In some cases, the source
     * and destination file may point to the same {@code inode}. See the documentation for {@link
     * java.nio.file.Files#move(Path, Path, CopyOption...)} to see more details.
     *
     * @param destination the path to the destination file
     * @param replace true if an existing file should be replaced, false otherwise.
     */
    Path moveFileTo(File destination, boolean replace);

    /**
     * Move the file using a {@link java.nio.file.Path}.
     *
     * @param to the path to the destination file.
     * @see #moveFileTo(Path, boolean)
     */
    default Path moveFileTo(Path to) {
      return moveFileTo(to, false);
    }

    /**
     * Move the file using a {@link java.nio.file.Path}.
     *
     * @param to the path to the destination file
     * @param replace true if an existing file should be replaced, false otherwise.
     * @see #moveFileTo(Path, boolean)
     */
    default Path moveFileTo(Path to, boolean replace) {
      return moveFileTo(to.toFile(), replace);
    }

    /**
     * Move the file using a {@link java.io.File}.
     *
     * @param destination the path to the destination file
     * @see #moveTo(Path, boolean)
     * @deprecated Deprecated as of 2.7.0. Use {@link #moveFileTo(File)} instead.
     */
    @Deprecated
    default TemporaryFile moveTo(File destination) {
      return moveTo(destination, false);
    }

    /**
     * Move the file to the specified destination {@link java.io.File}. In some cases, the source
     * and destination file may point to the same {@code inode}. See the documentation for {@link
     * java.nio.file.Files#move(Path, Path, CopyOption...)} to see more details.
     *
     * @param destination the path to the destination file
     * @param replace true if an existing file should be replaced, false otherwise.
     * @deprecated Deprecated as of 2.7.0. Use {@link #moveFileTo(File, boolean)} instead.
     */
    @Deprecated
    TemporaryFile moveTo(File destination, boolean replace);

    /**
     * Move the file using a {@link java.nio.file.Path}.
     *
     * @param to the path to the destination file.
     * @see #moveTo(Path, boolean)
     * @deprecated Deprecated as of 2.7.0. Use {@link #moveFileTo(Path)} instead.
     */
    @Deprecated
    default TemporaryFile moveTo(Path to) {
      return moveTo(to, false);
    }

    /**
     * Move the file using a {@link java.nio.file.Path}.
     *
     * @param to the path to the destination file
     * @param replace true if an existing file should be replaced, false otherwise.
     * @see #moveTo(Path, boolean)
     * @deprecated Deprecated as of 2.7.0. Use {@link #moveFileTo(Path, boolean)} instead.
     */
    @Deprecated
    default TemporaryFile moveTo(Path to, boolean replace) {
      return moveTo(to.toFile(), replace);
    }

    /**
     * Attempts to move source to target atomically and falls back to a non-atomic move if it fails.
     *
     * <p>This always tries to replace existent files. Since it is platform dependent if atomic
     * moves replaces existent files or not, considering that it will always replaces, makes the API
     * more predictable.
     *
     * @param to the path to the destination file
     */
    Path atomicMoveFileWithFallback(File to);

    /**
     * Attempts to move source to target atomically and falls back to a non-atomic move if it fails.
     *
     * <p>This always tries to replace existent files. Since it is platform dependent if atomic
     * moves replaces existent files or not, considering that it will always replaces, makes the API
     * more predictable.
     *
     * @param to the path to the destination file
     */
    default Path atomicMoveFileWithFallback(Path to) {
      return atomicMoveFileWithFallback(to.toFile());
    }

    /**
     * Attempts to move source to target atomically and falls back to a non-atomic move if it fails.
     *
     * <p>This always tries to replace existent files. Since it is platform dependent if atomic
     * moves replaces existent files or not, considering that it will always replaces, makes the API
     * more predictable.
     *
     * @param to the path to the destination file
     * @deprecated Deprecated as of 2.7.0. Use {@link #atomicMoveFileWithFallback(File)} instead.
     */
    @Deprecated
    TemporaryFile atomicMoveWithFallback(File to);

    /**
     * Attempts to move source to target atomically and falls back to a non-atomic move if it fails.
     *
     * <p>This always tries to replace existent files. Since it is platform dependent if atomic
     * moves replaces existent files or not, considering that it will always replaces, makes the API
     * more predictable.
     *
     * @param to the path to the destination file
     * @deprecated Deprecated as of 2.7.0. Use {@link #atomicMoveFileWithFallback(Path)} instead.
     */
    @Deprecated
    default TemporaryFile atomicMoveWithFallback(Path to) {
      return atomicMoveWithFallback(to.toFile());
    }
  }

  /** A temporary file creator that delegates to a Scala TemporaryFileCreator. */
  public static class DelegateTemporaryFileCreator implements TemporaryFileCreator {
    private final play.api.libs.Files.TemporaryFileCreator temporaryFileCreator;

    @Inject
    public DelegateTemporaryFileCreator(
        play.api.libs.Files.TemporaryFileCreator temporaryFileCreator) {
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

  /** Delegates to the Scala implementation. */
  public static class DelegateTemporaryFile implements TemporaryFile {

    private final play.api.libs.Files.TemporaryFile temporaryFile;
    private final TemporaryFileCreator temporaryFileCreator;

    public DelegateTemporaryFile(play.api.libs.Files.TemporaryFile temporaryFile) {
      this.temporaryFile = temporaryFile;
      this.temporaryFileCreator =
          new DelegateTemporaryFileCreator(temporaryFile.temporaryFileCreator());
    }

    private DelegateTemporaryFile(
        play.api.libs.Files.TemporaryFile temporaryFile,
        TemporaryFileCreator temporaryFileCreator) {
      this.temporaryFile = temporaryFile;
      this.temporaryFileCreator = temporaryFileCreator;
    }

    @Override
    public Path path() {
      return temporaryFile.path();
    }

    @Override
    public TemporaryFileCreator temporaryFileCreator() {
      return temporaryFileCreator;
    }

    @Override
    public Path moveFileTo(File to, boolean replace) {
      return temporaryFile.moveFileTo(to, replace);
    }

    @Override
    @Deprecated
    public TemporaryFile moveTo(File to, boolean replace) {
      return new DelegateTemporaryFile(
          temporaryFile.moveTo(to, replace), this.temporaryFileCreator);
    }

    @Override
    public Path copyTo(Path destination, boolean replace) {
      return temporaryFile.copyTo(destination, replace);
    }

    @Override
    public Path atomicMoveFileWithFallback(File to) {
      return temporaryFile.atomicMoveFileWithFallback(to.toPath());
    }

    @Override
    @Deprecated
    public TemporaryFile atomicMoveWithFallback(File to) {
      return new DelegateTemporaryFile(
          temporaryFile.atomicMoveWithFallback(to.toPath()), this.temporaryFileCreator);
    }
  }

  /**
   * A temporary file creator that uses the Scala play.api.libs.Files.SingletonTemporaryFileCreator
   * class behind the scenes.
   */
  public static class SingletonTemporaryFileCreator implements TemporaryFileCreator {
    private play.api.libs.Files.SingletonTemporaryFileCreator$ instance =
        play.api.libs.Files.SingletonTemporaryFileCreator$.MODULE$;

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

  /** @return the singleton instance of SingletonTemporaryFileCreator. */
  public static TemporaryFileCreator singletonTemporaryFileCreator() {
    return instance;
  }
}
