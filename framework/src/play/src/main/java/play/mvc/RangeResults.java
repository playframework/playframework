/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.mvc;

import akka.stream.javadsl.Source;
import akka.util.ByteString;
import play.api.libs.MimeTypes;
import play.core.j.JavaRangeResult;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

import scala.Option;
import scala.compat.java8.OptionConverters;

/**
 * Java API for Range results.
 *
 * For reference, see <a href="http://tools.ietf.org/html/rfc7233">RFC 7233</a>.
 */
public class RangeResults {

    private static Optional<String> rangeHeader() {
        Http.Request request = Http.Context.current().request();
        return Optional.ofNullable(request.getHeader(Http.HeaderNames.RANGE));
    }

    private static Optional<String> mimeTypeFor(String fileName) {
        Option<String> option = MimeTypes.forFileName(fileName);
        return OptionConverters.toJava(option);
    }

    /**
     * Returns the path as a result considering "Range" header. If the header is present and
     * it is satisfiable, then a Result containing just the requested part will be returned.
     * If the header is not present or is unsatisfiable, then a regular Result will be returned.
     *
     * @param path the content path
     * @return range result if "Range" header is present and regular result if not
     */
    public static Result ofPath(Path path) {
        return JavaRangeResult.ofPath(path, rangeHeader(), mimeTypeFor(path.toFile().getName()));
    }

    /**
     * Returns the path as a result considering "Range" header. If the header is present and
     * it is satisfiable, then a Result containing just the requested part will be returned.
     * If the header is not present or is unsatisfiable, then a regular Result will be returned.
     *
     * @param path the content path
     * @param fileName filename used at the Content-Disposition header.
     * @return range result if "Range" header is present and regular result if not
     */
    public static Result ofPath(Path path, String fileName) {
        return JavaRangeResult.ofPath(path, rangeHeader(), fileName, mimeTypeFor(fileName));
    }

    /**
     * Returns the file as a result considering "Range" header. If the header is present and
     * it is satisfiable, then a Result containing just the requested part will be returned.
     * If the header is not present or is unsatisfiable, then a regular Result will be returned.
     *
     * @param file the content file
     * @return range result if "Range" header is present and regular result if not
     */
    public static Result ofFile(File file) {
        return JavaRangeResult.ofFile(file, rangeHeader(), mimeTypeFor(file.getName()));
    }

    /**
     * Returns the file as a result considering "Range" header. If the header is present and
     * it is satisfiable, then a Result containing just the requested part will be returned.
     * If the header is not present or is unsatisfiable, then a regular Result will be returned.
     *
     * @param file the content file
     * @param fileName filename used at the Content-Disposition header
     * @return range result if "Range" header is present and regular result if not
     */
    public static Result ofFile(File file, String fileName) {
        return JavaRangeResult.ofFile(file, rangeHeader(), fileName, mimeTypeFor(fileName));
    }

    /**
     * Returns the stream as a result considering "Range" header. If the header is present and
     * it is satisfiable, then a Result containing just the requested part will be returned.
     * If the header is not present or is unsatisfiable, then a regular Result will be returned.
     *
     * @param entityLength the entityLength
     * @param source source of the entity
     * @param fileName filename used at the Content-Disposition header
     * @param contentType the content type for this stream
     * @return range result if "Range" header is present and regular result if not
     */
    public static Result ofSource(Long entityLength, Source<ByteString, ?> source, String fileName, String contentType) {
        return JavaRangeResult.ofSource(entityLength, source, rangeHeader(), Optional.ofNullable(fileName), Optional.ofNullable(contentType));
    }
}