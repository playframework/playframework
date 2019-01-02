/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import scala.compat.java8.OptionConverters;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

@Singleton
public class FileMimeTypes {

    private final play.api.http.FileMimeTypes fileMimeTypes;

    @Inject
    public FileMimeTypes(play.api.http.FileMimeTypes fileMimeTypes) {
        this.fileMimeTypes = fileMimeTypes;
    }

    public Optional<String> forFileName(String name) {
        return OptionConverters.toJava(fileMimeTypes.forFileName(name));
    }

    public play.api.http.FileMimeTypes asScala() {
        return fileMimeTypes;
    }
}
