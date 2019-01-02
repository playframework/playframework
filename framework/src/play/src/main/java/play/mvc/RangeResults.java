/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import akka.stream.javadsl.Source;
import akka.util.ByteString;
import play.core.j.JavaRangeResult;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Java API for Range results.
 *
 * For reference, see <a href="https://tools.ietf.org/html/rfc7233">RFC 7233</a>.
 */
public class RangeResults {

    private static Optional<String> rangeHeader(Http.Request request) {
        return request.header(Http.HeaderNames.RANGE);
    }

    /**
     * Returns the stream as a result considering "Range" header. If the header is present and
     * it is satisfiable, then a Result containing just the requested part will be returned.
     * If the header is not present or is unsatisfiable, then a regular Result will be returned.
     *
     * @param stream the content stream
     * @return range result if "Range" header is present and regular result if not
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link #ofStream(Http.Request, InputStream)} instead.
     */
    @Deprecated
    public static Result ofStream(InputStream stream) {
        return ofStream(Http.Context.current().request(), stream);
    }

    /**
     * Returns the stream as a result considering "Range" header. If the header is present and
     * it is satisfiable, then a Result containing just the requested part will be returned.
     * If the header is not present or is unsatisfiable, then a regular Result will be returned.
     *
     * @param request the request from which to retrieve the range header.
     * @param stream the content stream
     * @return range result if "Range" header is present and regular result if not
     */
    public static Result ofStream(Http.Request request, InputStream stream) {
        return JavaRangeResult.ofStream(stream, rangeHeader(request), null, Optional.empty());
    }

    /**
     * Returns the stream as a result considering "Range" header. If the header is present and
     * it is satisfiable, then a Result containing just the requested part will be returned.
     * If the header is not present or is unsatisfiable, then a regular Result will be returned.
     *
     * @param stream the content stream
     * @param contentLength the entity length
     * @return range result if "Range" header is present and regular result if not
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link #ofStream(Http.Request, InputStream, long)} instead.
     */
    @Deprecated
    public static Result ofStream(InputStream stream, long contentLength) {
        return ofStream(Http.Context.current().request(), stream, contentLength);
    }

    /**
     * Returns the stream as a result considering "Range" header. If the header is present and
     * it is satisfiable, then a Result containing just the requested part will be returned.
     * If the header is not present or is unsatisfiable, then a regular Result will be returned.
     *
     * @param request the request from which to retrieve the range header.
     * @param stream the content stream
     * @param contentLength the entity length
     * @return range result if "Range" header is present and regular result if not
     */
    public static Result ofStream(Http.Request request, InputStream stream, long contentLength) {
        return JavaRangeResult.ofStream(contentLength, stream, rangeHeader(request), null, Optional.empty());
    }

    /**
     * Returns the stream as a result considering "Range" header. If the header is present and
     * it is satisfiable, then a Result containing just the requested part will be returned.
     * If the header is not present or is unsatisfiable, then a regular Result will be returned.
     *
     * @param stream the content stream
     * @param contentLength the entity length
     * @param filename filename used at the Content-Disposition header
     * @return range result if "Range" header is present and regular result if not
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link #ofStream(Http.Request, InputStream, long, String)} instead.
     */
    @Deprecated
    public static Result ofStream(InputStream stream, long contentLength, String filename) {
        return ofStream(Http.Context.current().request(), stream, contentLength, filename);
    }

    /**
     * Returns the stream as a result considering "Range" header. If the header is present and
     * it is satisfiable, then a Result containing just the requested part will be returned.
     * If the header is not present or is unsatisfiable, then a regular Result will be returned.
     *
     * @param request the request from which to retrieve the range header.
     * @param stream the content stream
     * @param contentLength the entity length
     * @param filename filename used at the Content-Disposition header
     * @return range result if "Range" header is present and regular result if not
     */
    public static Result ofStream(Http.Request request, InputStream stream, long contentLength, String filename) {
        return JavaRangeResult.ofStream(contentLength, stream, rangeHeader(request), filename, Optional.empty());
    }

    /**
     * Returns the stream as a result considering "Range" header. If the header is present and
     * it is satisfiable, then a Result containing just the requested part will be returned.
     * If the header is not present or is unsatisfiable, then a regular Result will be returned.
     *
     * @param stream the content stream
     * @param contentLength the entity length
     * @param filename filename used at the Content-Disposition header
     * @param contentType the content type for this stream
     * @return range result if "Range" header is present and regular result if not
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link #ofStream(Http.Request, InputStream, long, String, String)} instead.
     */
    @Deprecated
    public static Result ofStream(InputStream stream, long contentLength, String filename, String contentType) {
        return ofStream(Http.Context.current().request(), stream, contentLength, filename, contentType);
    }

    /**
     * Returns the stream as a result considering "Range" header. If the header is present and
     * it is satisfiable, then a Result containing just the requested part will be returned.
     * If the header is not present or is unsatisfiable, then a regular Result will be returned.
     *
     * @param request the request from which to retrieve the range header.
     * @param stream the content stream
     * @param contentLength the entity length
     * @param filename filename used at the Content-Disposition header
     * @param contentType the content type for this stream
     * @return range result if "Range" header is present and regular result if not
     */
    public static Result ofStream(Http.Request request, InputStream stream, long contentLength, String filename, String contentType) {
        return JavaRangeResult.ofStream(contentLength, stream, rangeHeader(request), filename, Optional.ofNullable(contentType));
    }

    /**
     * Returns the path as a result considering "Range" header. If the header is present and
     * it is satisfiable, then a Result containing just the requested part will be returned.
     * If the header is not present or is unsatisfiable, then a regular Result will be returned.
     *
     * @param path the content path
     * @return range result if "Range" header is present and regular result if not
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link #ofPath(Http.Request, Path)} instead.
     */
    @Deprecated
    public static Result ofPath(Path path) {
        return ofPath(Http.Context.current().request(), path);
    }

    /**
     * Returns the path as a result considering "Range" header. If the header is present and
     * it is satisfiable, then a Result containing just the requested part will be returned.
     * If the header is not present or is unsatisfiable, then a regular Result will be returned.
     *
     * @param request the request from which to retrieve the range header.
     * @param path the content path
     * @return range result if "Range" header is present and regular result if not
     */
    public static Result ofPath(Http.Request request, Path path) {
        return ofPath(request, path, StaticFileMimeTypes.fileMimeTypes());
    }

    /**
     * Returns the path as a result considering "Range" header. If the header is present and
     * it is satisfiable, then a Result containing just the requested part will be returned.
     * If the header is not present or is unsatisfiable, then a regular Result will be returned.
     *
     * @param request the request from which to retrieve the range header.
     * @param path the content path
     * @param fileMimeTypes Used for file type mapping.
     * @return range result if "Range" header is present and regular result if not
     */
    public static Result ofPath(Http.Request request, Path path, FileMimeTypes fileMimeTypes) {
        return JavaRangeResult.ofPath(path, rangeHeader(request), fileMimeTypes.forFileName(path.toFile().getName()));
    }

    /**
     * Returns the path as a result considering "Range" header. If the header is present and
     * it is satisfiable, then a Result containing just the requested part will be returned.
     * If the header is not present or is unsatisfiable, then a regular Result will be returned.
     *
     * @param path the content path
     * @param fileName filename used at the Content-Disposition header.
     * @return range result if "Range" header is present and regular result if not
     *
     * @deprecated Deprecated as of 2.7.0. Use {link {@link #ofPath(Http.Request, Path, String)} instead.
     */
    @Deprecated
    public static Result ofPath(Path path, String fileName) {
        return ofPath(Http.Context.current().request(), path, fileName);
    }

    /**
     * Returns the path as a result considering "Range" header. If the header is present and
     * it is satisfiable, then a Result containing just the requested part will be returned.
     * If the header is not present or is unsatisfiable, then a regular Result will be returned.
     *
     * @param request the request from which to retrieve the range header.
     * @param path the content path
     * @param fileName filename used at the Content-Disposition header.
     * @return range result if "Range" header is present and regular result if not
     */
    public static Result ofPath(Http.Request request, Path path, String fileName) {
        return ofPath(request, path, fileName, StaticFileMimeTypes.fileMimeTypes());
    }

    /**
     * Returns the path as a result considering "Range" header. If the header is present and
     * it is satisfiable, then a Result containing just the requested part will be returned.
     * If the header is not present or is unsatisfiable, then a regular Result will be returned.
     *
     * @param request the request from which to retrieve the range header.
     * @param path the content path
     * @param fileName filename used at the Content-Disposition header.
     * @param fileMimeTypes Used for file type mapping.
     * @return range result if "Range" header is present and regular result if not
     */
    public static Result ofPath(Http.Request request, Path path, String fileName, FileMimeTypes fileMimeTypes) {
        return JavaRangeResult.ofPath(path, rangeHeader(request), fileName, fileMimeTypes.forFileName(fileName));
    }

    /**
     * Returns the file as a result considering "Range" header. If the header is present and
     * it is satisfiable, then a Result containing just the requested part will be returned.
     * If the header is not present or is unsatisfiable, then a regular Result will be returned.
     *
     * @param file the content file
     * @return range result if "Range" header is present and regular result if not
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link #ofFile(Http.Request, File)} instead.
     */
    @Deprecated
    public static Result ofFile(File file) {
        return ofFile(Http.Context.current().request(), file);
    }

    /**
     * Returns the file as a result considering "Range" header. If the header is present and
     * it is satisfiable, then a Result containing just the requested part will be returned.
     * If the header is not present or is unsatisfiable, then a regular Result will be returned.
     *
     * @param request the request from which to retrieve the range header.
     * @param file the content file
     * @return range result if "Range" header is present and regular result if not
     */
    public static Result ofFile(Http.Request request, File file) {
        return ofFile(request, file, StaticFileMimeTypes.fileMimeTypes());
    }

    /**
     * Returns the file as a result considering "Range" header. If the header is present and
     * it is satisfiable, then a Result containing just the requested part will be returned.
     * If the header is not present or is unsatisfiable, then a regular Result will be returned.
     *
     * @param request the request from which to retrieve the range header.
     * @param file the content file
     * @param fileMimeTypes Used for file type mapping.
     * @return range result if "Range" header is present and regular result if not
     */
    public static Result ofFile(Http.Request request, File file, FileMimeTypes fileMimeTypes) {
        return JavaRangeResult.ofFile(file, rangeHeader(request), fileMimeTypes.forFileName(file.getName()));
    }

    /**
     * Returns the file as a result considering "Range" header. If the header is present and
     * it is satisfiable, then a Result containing just the requested part will be returned.
     * If the header is not present or is unsatisfiable, then a regular Result will be returned.
     *
     * @param file the content file
     * @param fileName filename used at the Content-Disposition header
     * @return range result if "Range" header is present and regular result if not
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link #ofFile(Http.Request, File, String)} instead.
     */
    @Deprecated
    public static Result ofFile(File file, String fileName) {
        return ofFile(Http.Context.current().request(), file, fileName);
    }

    /**
     * Returns the file as a result considering "Range" header. If the header is present and
     * it is satisfiable, then a Result containing just the requested part will be returned.
     * If the header is not present or is unsatisfiable, then a regular Result will be returned.
     *
     * @param request the request from which to retrieve the range header.
     * @param file the content file
     * @param fileName filename used at the Content-Disposition header
     * @return range result if "Range" header is present and regular result if not
     */
    public static Result ofFile(Http.Request request, File file, String fileName) {
        return ofFile(request, file, fileName, StaticFileMimeTypes.fileMimeTypes());
    }

    /**
     * Returns the file as a result considering "Range" header. If the header is present and
     * it is satisfiable, then a Result containing just the requested part will be returned.
     * If the header is not present or is unsatisfiable, then a regular Result will be returned.
     *
     * @param request the request from which to retrieve the range header.
     * @param file the content file
     * @param fileName filename used at the Content-Disposition header
     * @param fileMimeTypes Used for file type mapping.
     * @return range result if "Range" header is present and regular result if not
     */
    public static Result ofFile(Http.Request request, File file, String fileName, FileMimeTypes fileMimeTypes) {
        return JavaRangeResult.ofFile(file, rangeHeader(request), fileName, fileMimeTypes.forFileName(fileName));
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
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link #ofSource(Http.Request, Long, Source, String, String)} instead.
     */
    @Deprecated
    public static Result ofSource(Long entityLength, Source<ByteString, ?> source, String fileName, String contentType) {
        return ofSource(Http.Context.current().request(), entityLength, source, fileName, contentType);
    }

    /**
     * Returns the stream as a result considering "Range" header. If the header is present and
     * it is satisfiable, then a Result containing just the requested part will be returned.
     * If the header is not present or is unsatisfiable, then a regular Result will be returned.
     *
     * @param request the request from which to retrieve the range header.
     * @param entityLength the entityLength
     * @param source source of the entity
     * @param fileName filename used at the Content-Disposition header
     * @param contentType the content type for this stream
     * @return range result if "Range" header is present and regular result if not
     */
    public static Result ofSource(Http.Request request, Long entityLength, Source<ByteString, ?> source, String fileName, String contentType) {
        return JavaRangeResult.ofSource(entityLength, source, rangeHeader(request), Optional.ofNullable(fileName), Optional.ofNullable(contentType));
    }
}
