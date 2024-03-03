/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.runsupport;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

import java.io.File;
import java.util.List;
import java.util.Map;
import play.api.PlayException;

public interface CompileResult {

  class CompileSuccess implements CompileResult {
    private final Map<String, Source> sources;
    private final List<File> classpath;

    public CompileSuccess(Map<String, Source> sources, List<File> classpath) {
      this.sources = requireNonNullElse(sources, Map.of());
      this.classpath = requireNonNullElse(classpath, List.of());
    }

    public Map<String, Source> getSources() {
      return sources;
    }

    public List<File> getClasspath() {
      return classpath;
    }
  }

  class CompileFailure implements CompileResult {
    private final PlayException exception;

    public CompileFailure(PlayException exception) {
      this.exception = requireNonNull(exception);
    }

    public PlayException getException() {
      return exception;
    }
  }
}
