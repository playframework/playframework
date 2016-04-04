/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.mvc;

import play.core.j.JavaRangeResult;
import play.api.mvc.Result;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;

public class RangeResult {

    public static Result ofPath(Path path, String range) {
        return JavaRangeResult.ofPath(path, range);
    }

    public static Result ofPath(Path path, String range, String fileName) {
        return JavaRangeResult.ofPath(path, range, fileName);
    }

    public static Result ofFile(File file, String range) {
        return JavaRangeResult.ofFile(file, range);
    }

    public static Result ofFile(File file, String range, String fileName) {
        return JavaRangeResult.ofFile(file, range, fileName);
    }

    public static Result ofStream(InputStream stream, String range) {
        return JavaRangeResult.ofStream(stream, range);
    }

    public static Result ofStream(InputStream stream, String range, String fileName) {
        return JavaRangeResult.ofStream(stream, range, fileName);
    }

}
