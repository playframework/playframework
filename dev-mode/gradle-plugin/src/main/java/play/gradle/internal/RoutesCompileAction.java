/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.gradle.internal;

import static java.util.Optional.ofNullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Stream;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.work.ChangeType;
import org.gradle.workers.WorkAction;
import play.routes.compiler.Language;
import play.routes.compiler.RoutesCompiler$;
import play.routes.compiler.RoutesCompiler.GeneratedSource;
import play.routes.compiler.RoutesCompiler.GeneratedSource$;
import scala.Option;

/** Gradle work action that compile or delete one Routes file. */
public abstract class RoutesCompileAction implements WorkAction<RoutesCompileParams> {

  private static final Logger LOGGER = Logging.getLogger(RoutesCompileAction.class);

  @Override
  public void execute() {
    if (getParameters().getChangeType().get() == ChangeType.REMOVED) {
      delete();
    } else {
      compile();
    }
  }

  private boolean isGeneratedBy(File file, File origin) {
    Option<GeneratedSource> generatedSource = GeneratedSource$.MODULE$.unapply(file);
    if (generatedSource.isEmpty() || generatedSource.get().source().isEmpty()) return false;
    return origin.equals(generatedSource.get().source().get());
  }

  private void delete() {
    File routes = getParameters().getRoutesFile().getAsFile().get();
    File destinationDirectory = getParameters().getDestinationDirectory().getAsFile().get();
    try (Stream<Path> paths = Files.walk(destinationDirectory.toPath())) {
      paths
          .filter(Files::isRegularFile)
          .filter(file -> isGeneratedBy(file.toFile(), routes))
          .forEach(
              file -> {
                try {
                  Files.delete(file);
                } catch (IOException e) {
                  LOGGER.warn("Deletion {} failed", file, e);
                }
              });
    } catch (IOException e) {
      LOGGER.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  private void compile() {
    try {
      File routes = getParameters().getRoutesFile().getAsFile().get();
      File destinationDirectory = getParameters().getDestinationDirectory().getAsFile().get();
      boolean generateForwardsRouter = getParameters().getGenerateForwardsRouter().get();
      boolean generateReverseRouter = getParameters().getGenerateReverseRouter().get();
      boolean generateJsReverseRouter = getParameters().getGenerateJsReverseRouter().get();
      boolean namespaceReverseRouter = getParameters().getNamespaceReverseRouter().get();
      @SuppressWarnings("SwitchStatementWithTooFewBranches")
      Language lang =
          switch (getParameters().getLang().get()) {
            case JAVA -> Language.JAVA;
            default -> Language.SCALA;
          };
      Collection<String> imports = getParameters().getImports().get();
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info(
            "Compile Routes file {} into {}",
            routes.getCanonicalPath(),
            destinationDirectory.getCanonicalPath());
      }
      var compileResult =
          RoutesCompiler$.MODULE$.compile(
              routes,
              imports,
              generateForwardsRouter,
              generateReverseRouter,
              generateJsReverseRouter,
              namespaceReverseRouter,
              lang,
              destinationDirectory);
      if (compileResult.isLeft()) {
        compileResult
            .left()
            .get()
            .forEach(
                error ->
                    LOGGER.error(
                        "{}{}: {}",
                        error.source().getAbsolutePath(),
                        ofNullable(error.line().getOrElse(null)).map(line -> ":" + line).orElse(""),
                        error.message()));
        throw new RuntimeException("Routes compilation failed");
      }
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }
}
