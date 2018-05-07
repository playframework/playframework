/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.Optional;

import akka.util.ByteString;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.databind.JsonNode;
import play.http.HttpEntity;
import play.twirl.api.Content;

import static play.mvc.Http.HeaderNames.LOCATION;
import static play.mvc.Http.Status.*;

/**
 * Common results.
 */
public class Results {

    private static final String UTF8 = "utf-8";

    // -- Status

    /**
     * Generates a simple result.
     *
     * @param status the HTTP status for this result e.g. 200 (OK), 404 (NOT_FOUND)
     * @return the header-only result
     */
    public static StatusHeader status(int status) {
        return new StatusHeader(status);
    }

    /**
     * Generates a simple result.
     *
     * @param status the HTTP status for this result e.g. 200 (OK), 404 (NOT_FOUND)
     * @param content the result's body content
     * @return the result
     */
    public static Result status(int status, Content content) {
        return status(status, content, UTF8);
    }

    /**
     * Generates a simple result.
     * @param status the HTTP status for this result e.g. 200 (OK), 404 (NOT_FOUND)
     * @param content the result's body content
     * @param charset the charset to encode the content with (e.g. "UTF-8")
     * @return the result
     */
    public static Result status(int status, Content content, String charset) {
        if (content == null) {
            throw new NullPointerException("Null content");
        }
        return new Result(status, HttpEntity.fromContent(content, charset));
    }

    /**
     * Generates a simple result.
     *
     * @param status the HTTP status for this result e.g. 200 (OK), 404 (NOT_FOUND)
     * @param content the result's body content. It will be encoded as a UTF-8 string.
     * @return the result
     */
    public static Result status(int status, String content) {
        return status(status, content, UTF8);
    }

    /**
     * Generates a simple result.
     *
     * @param status the HTTP status for this result e.g. 200 (OK), 404 (NOT_FOUND)
     * @param content the result's body content.
     * @param charset the charset in which to encode the content (e.g. "UTF-8")
     * @return the result
     */
    public static Result status(int status, String content, String charset) {
        if (content == null) {
            throw new NullPointerException("Null content");
        }
        return new Result(status, HttpEntity.fromString(content, charset));
    }

    /**
     * Generates a simple result with json content and UTF8 encoding.
     *
     * @param status the HTTP status for this result e.g. 200 (OK), 404 (NOT_FOUND)
     * @param content the result's body content as a play-json object
     * @return the result
     *
     */
    public static Result status(int status, JsonNode content) {
        return status(status, content, JsonEncoding.UTF8);
    }

    /**
     * Generates a simple result with json content.
     *
     * @param status the HTTP status for this result e.g. 200 (OK), 404 (NOT_FOUND)
     * @param content the result's body content, as a play-json object
     * @param encoding the encoding into which the json should be encoded
     *
     * @return the result
     *
     */
    public static Result status(int status, JsonNode content, JsonEncoding encoding) {
        if (content == null) {
            throw new NullPointerException("Null content");
        }
        return status(status).sendJson(content, encoding);
    }

    /**
     * Generates a simple result with byte-array content.
     *
     * @param status the HTTP status for this result e.g. 200 (OK), 404 (NOT_FOUND)
     * @param content the result's body content, as a byte array
     * @return the result
     */
    public static Result status(int status, byte[] content) {
        if (content == null) {
            throw new NullPointerException("Null content");
        }
        return new Result(status, new HttpEntity.Strict(ByteString.fromArray(content), Optional.empty()));
    }

    /**
     * Generates a simple result.
     *
     * @param status the HTTP status for this result e.g. 200 (OK), 404 (NOT_FOUND)
     * @param content the result's body content
     * @return the result
     */
    public static Result status(int status, ByteString content) {
        if (content == null) {
            throw new NullPointerException("Null content");
        }
        return new Result(status, new HttpEntity.Strict(content, Optional.empty()));
    }

    /**
     * Generates a chunked result.
     *
     * @param status the HTTP status for this result e.g. 200 (OK), 404 (NOT_FOUND)
     * @param content the input stream containing data to chunk over
     * @return the result
     */
    public static Result status(int status, InputStream content) {
        return status(status).sendInputStream(content);
    }

    /**
     * Generates a chunked result.
     *
     * @param status the HTTP status for this result e.g. 200 (OK), 404 (NOT_FOUND)
     * @param content the input stream containing data to chunk over
     * @param contentLength the length of the provided content in bytes.
     * @return the result
     */
    public static Result status(int status, InputStream content, long contentLength) {
        return status(status).sendInputStream(content, contentLength);
    }

    /**
     * Generates a result with file contents.
     *
     * @param status the HTTP status for this result e.g. 200 (OK), 404 (NOT_FOUND)
     * @param content the file to send
     * @return the result
     */
    public static Result status(int status, File content) {
        return status(status, content, true);
    }

    /**
     * Generates a result with file content.
     *
     * @param status the HTTP status for this result e.g. 200 (OK), 404 (NOT_FOUND)
     * @param content the file to send
     * @param inline <code>true</code> to have it sent with inline Content-Disposition.
     * @return the result
     *
     */
    public static Result status(int status, File content, boolean inline) {
        return status(status).sendFile(content, inline);
    }

    /**
     * Generates a result.
     *
     * @param status the HTTP status for this result e.g. 200 (OK), 404 (NOT_FOUND)
     * @param content the file to send
     * @param fileName the name that the client should receive this file as
     * @return the result
     */
    public static Result status(int status, File content, String fileName) {
        return status(status).sendFile(content, fileName);
    }

    /**
     * Generates a 204 No Content result.
     *
     * @return the result
     */
    public static StatusHeader noContent() {
        return new StatusHeader(NO_CONTENT);
    }

    //////////////////////////////////////////////////////
    // EVERYTHING BELOW HERE IS GENERATED
    //
    // See https://github.com/jroper/play-source-generator
    //////////////////////////////////////////////////////


    /**
     * Generates a 200 OK result.
     *
     * @return the result
     */
    public static StatusHeader ok() {
        return new StatusHeader(OK);
    }

    /**
     * Generates a 200 OK result.
     *
     * @param content the HTTP response body
     * @return the result
     */
    public static Result ok(Content content) {
        return status(OK, content);
    }

    /**
     * Generates a 200 OK result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result ok(Content content, String charset) {
        return status(OK, content, charset);
    }

    /**
     * Generates a 200 OK result.
     *
     * @param content HTTP response body, encoded as a UTF-8 string
     * @return the result
     */
    public static Result ok(String content) {
        return status(OK, content);
    }

    /**
     * Generates a 200 OK result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result ok(String content, String charset) {
        return status(OK, content, charset);
    }

    /**
     * Generates a 200 OK result.
     *
     * @param content the result's body content as a play-json object. It will be encoded
     *        as a UTF-8 string.
     * @return the result
     */
    public static Result ok(JsonNode content) {
        return status(OK, content);
    }

    /**
     * Generates a 200 OK result.
     *
     * @param content the result's body content as a play-json object
     * @param encoding the encoding into which the json should be encoded
     * @return the result
     */
    public static Result ok(JsonNode content, JsonEncoding encoding) {
        return status(OK, content, encoding);
    }

    /**
     * Generates a 200 OK result.
     *
     * @param content the result's body content
     * @return the result
     */
    public static Result ok(byte[] content) {
        return status(OK, content);
    }

    /**
     * Generates a 200 OK result.
     *
     * @param content the input stream containing data to chunk over
     * @return the result
     */
    public static Result ok(InputStream content) {
        return status(OK, content);
    }

    /**
     * Generates a 200 OK result.
     *
     * @param content the input stream containing data to chunk over
     * @param contentLength the length of the provided content in bytes.
     * @return the result
     */
    public static Result ok(InputStream content, long contentLength) {
        return status(OK, content, contentLength);
    }

    /**
     * Generates a 200 OK result.
     *
     * @param content The file to send.
     * @return the result
     */
    public static Result ok(File content) {
        return status(OK, content);
    }

    /**
     * Generates a 200 OK result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     * @return the result
     */
    public static Result ok(File content, boolean inline) {
        return status(OK, content, inline);
    }

    /**
     * Generates a 200 OK result.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     * @return the result
     */
    public static Result ok(File content, String filename) {
        return status(OK, content, filename);
    }


    /**
     * Generates a 201 Created result.
     *
     * @return the result
     */
    public static StatusHeader created() {
        return new StatusHeader(CREATED);
    }

    /**
     * Generates a 201 Created result.
     *
     * @param content the HTTP response body
     * @return the result
     */
    public static Result created(Content content) {
        return status(CREATED, content);
    }

    /**
     * Generates a 201 Created result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result created(Content content, String charset) {
        return status(CREATED, content, charset);
    }

    /**
     * Generates a 201 Created result.
     *
     * @param content HTTP response body, encoded as a UTF-8 string
     * @return the result
     */
    public static Result created(String content) {
        return status(CREATED, content);
    }

    /**
     * Generates a 201 Created result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result created(String content, String charset) {
        return status(CREATED, content, charset);
    }

    /**
     * Generates a 201 Created result.
     *
     * @param content the result's body content as a play-json object. It will be encoded
     *        as a UTF-8 string.
     * @return the result
     */
    public static Result created(JsonNode content) {
        return status(CREATED, content);
    }

    /**
     * Generates a 201 Created result.
     *
     * @param content the result's body content as a play-json object
     * @param encoding the encoding into which the json should be encoded
     * @return the result
     */
    public static Result created(JsonNode content, JsonEncoding encoding) {
        return status(CREATED, content, encoding);
    }

    /**
     * Generates a 201 Created result.
     *
     * @param content the result's body content
     * @return the result
     */
    public static Result created(byte[] content) {
        return status(CREATED, content);
    }

    /**
     * Generates a 201 Created result.
     *
     * @param content the input stream containing data to chunk over
     * @return the result
     */
    public static Result created(InputStream content) {
        return status(CREATED, content);
    }

    /**
     * Generates a 201 Created result.
     *
     * @param content the input stream containing data to chunk over
     * @param contentLength the length of the provided content in bytes.
     * @return the result
     */
    public static Result created(InputStream content, long contentLength) {
        return status(CREATED, content, contentLength);
    }

    /**
     * Generates a 201 Created result.
     *
     * @param content The file to send.
     * @return the result
     */
    public static Result created(File content) {
        return status(CREATED, content);
    }

    /**
     * Generates a 201 Created result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     * @return the result
     */
    public static Result created(File content, boolean inline) {
        return status(CREATED, content, inline);
    }

    /**
     * Generates a 201 Created result.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     * @return the result
     */
    public static Result created(File content, String filename) {
        return status(CREATED, content, filename);
    }

    /**
     * Generates a 400 Bad Request result.
     *
     * @return the result
     */
    public static StatusHeader badRequest() {
        return new StatusHeader(BAD_REQUEST);
    }

    /**
     * Generates a 400 Bad Request result.
     *
     * @param content the HTTP response body
     * @return the result
     */
    public static Result badRequest(Content content) {
        return status(BAD_REQUEST, content);
    }

    /**
     * Generates a 400 Bad Request result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result badRequest(Content content, String charset) {
        return status(BAD_REQUEST, content, charset);
    }

    /**
     * Generates a 400 Bad Request result.
     *
     * @param content HTTP response body, encoded as a UTF-8 string
     * @return the result
     */
    public static Result badRequest(String content) {
        return status(BAD_REQUEST, content);
    }

    /**
     * Generates a 400 Bad Request result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result badRequest(String content, String charset) {
        return status(BAD_REQUEST, content, charset);
    }

    /**
     * Generates a 400 Bad Request result.
     *
     * @param content the result's body content as a play-json object. It will be encoded
     *        as a UTF-8 string.
     * @return the result
     */
    public static Result badRequest(JsonNode content) {
        return status(BAD_REQUEST, content);
    }

    /**
     * Generates a 400 Bad Request result.
     *
     * @param content the result's body content as a play-json object
     * @param encoding the encoding into which the json should be encoded
     * @return the result
     */
    public static Result badRequest(JsonNode content, JsonEncoding encoding) {
        return status(BAD_REQUEST, content, encoding);
    }

    /**
     * Generates a 400 Bad Request result.
     *
     * @param content the result's body content
     * @return the result
     */
    public static Result badRequest(byte[] content) {
        return status(BAD_REQUEST, content);
    }

    /**
     * Generates a 400 Bad Request result.
     *
     * @param content the input stream containing data to chunk over
     * @return the result
     */
    public static Result badRequest(InputStream content) {
        return status(BAD_REQUEST, content);
    }

    /**
     * Generates a 400 Bad Request result.
     *
     * @param content the input stream containing data to chunk over
     * @param contentLength the length of the provided content in bytes.
     * @return the result
     */
    public static Result badRequest(InputStream content, long contentLength) {
        return status(BAD_REQUEST, content, contentLength);
    }

    /**
     * Generates a 400 Bad Request result.
     *
     * @param content The file to send.
     * @return the result
     */
    public static Result badRequest(File content) {
        return status(BAD_REQUEST, content);
    }

    /**
     * Generates a 400 Bad Request result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     * @return the result
     */
    public static Result badRequest(File content, boolean inline) {
        return status(BAD_REQUEST, content, inline);
    }

    /**
     * Generates a 400 Bad Request result.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     * @return the result
     */
    public static Result badRequest(File content, String filename) {
        return status(BAD_REQUEST, content, filename);
    }

    /**
     * Generates a 401 Unauthorized result.
     *
     * @return the result
     */
    public static StatusHeader unauthorized() {
        return new StatusHeader(UNAUTHORIZED);
    }

    /**
     * Generates a 401 Unauthorized result.
     *
     * @param content the HTTP response body
     * @return the result
     */
    public static Result unauthorized(Content content) {
        return status(UNAUTHORIZED, content);
    }

    /**
     * Generates a 401 Unauthorized result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result unauthorized(Content content, String charset) {
        return status(UNAUTHORIZED, content, charset);
    }

    /**
     * Generates a 401 Unauthorized result.
     *
     * @param content HTTP response body, encoded as a UTF-8 string
     * @return the result
     */
    public static Result unauthorized(String content) {
        return status(UNAUTHORIZED, content);
    }

    /**
     * Generates a 401 Unauthorized result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result unauthorized(String content, String charset) {
        return status(UNAUTHORIZED, content, charset);
    }

    /**
     * Generates a 401 Unauthorized result.
     *
     * @param content the result's body content as a play-json object. It will be encoded
     *        as a UTF-8 string.
     * @return the result
     */
    public static Result unauthorized(JsonNode content) {
        return status(UNAUTHORIZED, content);
    }

    /**
     * Generates a 401 Unauthorized result.
     *
     * @param content the result's body content as a play-json object
     * @param encoding the encoding into which the json should be encoded
     * @return the result
     */
    public static Result unauthorized(JsonNode content, JsonEncoding encoding) {
        return status(UNAUTHORIZED, content, encoding);
    }

    /**
     * Generates a 401 Unauthorized result.
     *
     * @param content the result's body content
     * @return the result
     */
    public static Result unauthorized(byte[] content) {
        return status(UNAUTHORIZED, content);
    }

    /**
     * Generates a 401 Unauthorized result.
     *
     * @param content the input stream containing data to chunk over
     * @return the result
     */
    public static Result unauthorized(InputStream content) {
        return status(UNAUTHORIZED, content);
    }

    /**
     * Generates a 401 Unauthorized result.
     *
     * @param content the input stream containing data to chunk over
     * @param contentLength the length of the provided content in bytes.
     * @return the result
     */
    public static Result unauthorized(InputStream content, long contentLength) {
        return status(UNAUTHORIZED, content, contentLength);
    }

    /**
     * Generates a 401 Unauthorized result.
     *
     * @param content The file to send.
     * @return the result
     */
    public static Result unauthorized(File content) {
        return status(UNAUTHORIZED, content);
    }

    /**
     * Generates a 401 Unauthorized result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     * @return the result
     */
    public static Result unauthorized(File content, boolean inline) {
        return status(UNAUTHORIZED, content, inline);
    }

    /**
     * Generates a 401 Unauthorized result.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     * @return the result
     */
    public static Result unauthorized(File content, String filename) {
        return status(UNAUTHORIZED, content, filename);
    }

    /**
     * Generates a 402 Payment Required result.
     *
     * @return the result
     */
    public static StatusHeader paymentRequired() {
        return new StatusHeader(PAYMENT_REQUIRED);
    }

    /**
     * Generates a 402 Payment Required result.
     *
     * @param content the HTTP response body
     * @return the result
     */
    public static Result paymentRequired(Content content) {
        return status(PAYMENT_REQUIRED, content);
    }

    /**
     * Generates a 402 Payment Required result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result paymentRequired(Content content, String charset) {
        return status(PAYMENT_REQUIRED, content, charset);
    }

    /**
     * Generates a 402 Payment Required result.
     *
     * @param content HTTP response body, encoded as a UTF-8 string
     * @return the result
     */
    public static Result paymentRequired(String content) {
        return status(PAYMENT_REQUIRED, content);
    }

    /**
     * Generates a 402 Payment Required result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result paymentRequired(String content, String charset) {
        return status(PAYMENT_REQUIRED, content, charset);
    }

    /**
     * Generates a 402 Payment Required result.
     *
     * @param content the result's body content as a play-json object. It will be encoded
     *        as a UTF-8 string.
     * @return the result
     */
    public static Result paymentRequired(JsonNode content) {
        return status(PAYMENT_REQUIRED, content);
    }

    /**
     * Generates a 402 Payment Required result.
     *
     * @param content the result's body content as a play-json object
     * @param encoding the encoding into which the json should be encoded
     * @return the result
     */
    public static Result paymentRequired(JsonNode content, JsonEncoding encoding) {
        return status(PAYMENT_REQUIRED, content, encoding);
    }

    /**
     * Generates a 402 Payment Required result.
     *
     * @param content the result's body content
     * @return the result
     */
    public static Result paymentRequired(byte[] content) {
        return status(PAYMENT_REQUIRED, content);
    }

    /**
     * Generates a 402 Payment Required result.
     *
     * @param content the input stream containing data to chunk over
     * @return the result
     */
    public static Result paymentRequired(InputStream content) {
        return status(PAYMENT_REQUIRED, content);
    }

    /**
     * Generates a 402 Payment Required result.
     *
     * @param content the input stream containing data to chunk over
     * @param contentLength the length of the provided content in bytes.
     * @return the result
     */
    public static Result paymentRequired(InputStream content, long contentLength) {
        return status(PAYMENT_REQUIRED, content, contentLength);
    }

    /**
     * Generates a 402 Payment Required result.
     *
     * @param content The file to send.
     * @return the result
     */
    public static Result paymentRequired(File content) {
        return status(PAYMENT_REQUIRED, content);
    }

    /**
     * Generates a 402 Payment Required result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     * @return the result
     */
    public static Result paymentRequired(File content, boolean inline) {
        return status(PAYMENT_REQUIRED, content, inline);
    }

    /**
     * Generates a 402 Payment Required result.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     * @return the result
     */
    public static Result paymentRequired(File content, String filename) {
        return status(PAYMENT_REQUIRED, content, filename);
    }

    /**
     * Generates a 403 Forbidden result.
     *
     * @return the result
     */
    public static StatusHeader forbidden() {
        return new StatusHeader(FORBIDDEN);
    }

    /**
     * Generates a 403 Forbidden result.
     *
     * @param content the HTTP response body
     * @return the result
     */
    public static Result forbidden(Content content) {
        return status(FORBIDDEN, content);
    }

    /**
     * Generates a 403 Forbidden result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result forbidden(Content content, String charset) {
        return status(FORBIDDEN, content, charset);
    }

    /**
     * Generates a 403 Forbidden result.
     *
     * @param content HTTP response body, encoded as a UTF-8 string
     * @return the result
     */
    public static Result forbidden(String content) {
        return status(FORBIDDEN, content);
    }

    /**
     * Generates a 403 Forbidden result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result forbidden(String content, String charset) {
        return status(FORBIDDEN, content, charset);
    }

    /**
     * Generates a 403 Forbidden result.
     *
     * @param content the result's body content as a play-json object. It will be encoded
     *        as a UTF-8 string.
     * @return the result
     */
    public static Result forbidden(JsonNode content) {
        return status(FORBIDDEN, content);
    }

    /**
     * Generates a 403 Forbidden result.
     *
     * @param content the result's body content as a play-json object
     * @param encoding the encoding into which the json should be encoded
     * @return the result
     */
    public static Result forbidden(JsonNode content, JsonEncoding encoding) {
        return status(FORBIDDEN, content, encoding);
    }

    /**
     * Generates a 403 Forbidden result.
     *
     * @param content the result's body content
     * @return the result
     */
    public static Result forbidden(byte[] content) {
        return status(FORBIDDEN, content);
    }

    /**
     * Generates a 403 Forbidden result.
     *
     * @param content the input stream containing data to chunk over
     * @return the result
     */
    public static Result forbidden(InputStream content) {
        return status(FORBIDDEN, content);
    }

    /**
     * Generates a 403 Forbidden result.
     *
     * @param content the input stream containing data to chunk over
     * @param contentLength the length of the provided content in bytes.
     * @return the result
     */
    public static Result forbidden(InputStream content, long contentLength) {
        return status(FORBIDDEN, content, contentLength);
    }

    /**
     * Generates a 403 Forbidden result.
     *
     * @param content The file to send.
     * @return the result
     */
    public static Result forbidden(File content) {
        return status(FORBIDDEN, content);
    }

    /**
     * Generates a 403 Forbidden result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     * @return the result
     */
    public static Result forbidden(File content, boolean inline) {
        return status(FORBIDDEN, content, inline);
    }

    /**
     * Generates a 403 Forbidden result.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     * @return the result
     */
    public static Result forbidden(File content, String filename) {
        return status(FORBIDDEN, content, filename);
    }

    /**
     * Generates a 404 Not Found result.
     *
     * @return the result
     */
    public static StatusHeader notFound() {
        return new StatusHeader(NOT_FOUND);
    }

    /**
     * Generates a 404 Not Found result.
     *
     * @param content the HTTP response body
     * @return the result
     */
    public static Result notFound(Content content) {
        return status(NOT_FOUND, content);
    }

    /**
     * Generates a 404 Not Found result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result notFound(Content content, String charset) {
        return status(NOT_FOUND, content, charset);
    }

    /**
     * Generates a 404 Not Found result.
     *
     * @param content HTTP response body, encoded as a UTF-8 string
     * @return the result
     */
    public static Result notFound(String content) {
        return status(NOT_FOUND, content);
    }

    /**
     * Generates a 404 Not Found result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result notFound(String content, String charset) {
        return status(NOT_FOUND, content, charset);
    }

    /**
     * Generates a 404 Not Found result.
     *
     * @param content the result's body content as a play-json object. It will be encoded
     *        as a UTF-8 string.
     * @return the result
     */
    public static Result notFound(JsonNode content) {
        return status(NOT_FOUND, content);
    }

    /**
     * Generates a 404 Not Found result.
     *
     * @param content the result's body content as a play-json object
     * @param encoding the encoding into which the json should be encoded
     * @return the result
     */
    public static Result notFound(JsonNode content, JsonEncoding encoding) {
        return status(NOT_FOUND, content, encoding);
    }

    /**
     * Generates a 404 Not Found result.
     *
     * @param content the result's body content
     * @return the result
     */
    public static Result notFound(byte[] content) {
        return status(NOT_FOUND, content);
    }

    /**
     * Generates a 404 Not Found result.
     *
     * @param content the input stream containing data to chunk over
     * @return the result
     */
    public static Result notFound(InputStream content) {
        return status(NOT_FOUND, content);
    }

    /**
     * Generates a 404 Not Found result.
     *
     * @param content the input stream containing data to chunk over
     * @param contentLength the length of the provided content in bytes.
     * @return the result
     */
    public static Result notFound(InputStream content, long contentLength) {
        return status(NOT_FOUND, content, contentLength);
    }

    /**
     * Generates a 404 Not Found result.
     *
     * @param content The file to send.
     * @return the result
     */
    public static Result notFound(File content) {
        return status(NOT_FOUND, content);
    }

    /**
     * Generates a 404 Not Found result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     * @return the result
     */
    public static Result notFound(File content, boolean inline) {
        return status(NOT_FOUND, content, inline);
    }

    /**
     * Generates a 404 Not Found result.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     * @return the result
     */
    public static Result notFound(File content, String filename) {
        return status(NOT_FOUND, content, filename);
    }

    /**
     * Generates a 406 Not Acceptable result.
     *
     * @return the result
     */
    public static StatusHeader notAcceptable() {
        return new StatusHeader(NOT_ACCEPTABLE);
    }

    /**
     * Generates a 406 Not Acceptable result.
     *
     * @param content the HTTP response body
     * @return the result
     */
    public static Result notAcceptable(Content content) {
        return status(NOT_ACCEPTABLE, content);
    }

    /**
     * Generates a 406 Not Acceptable result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result notAcceptable(Content content, String charset) {
        return status(NOT_ACCEPTABLE, content, charset);
    }

    /**
     * Generates a 406 Not Acceptable result.
     *
     * @param content HTTP response body, encoded as a UTF-8 string
     * @return the result
     */
    public static Result notAcceptable(String content) {
        return status(NOT_ACCEPTABLE, content);
    }

    /**
     * Generates a 406 Not Acceptable result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result notAcceptable(String content, String charset) {
        return status(NOT_ACCEPTABLE, content, charset);
    }

    /**
     * Generates a 406 Not Acceptable result.
     *
     * @param content the result's body content as a play-json object. It will be encoded
     *        as a UTF-8 string.
     * @return the result
     */
    public static Result notAcceptable(JsonNode content) {
        return status(NOT_ACCEPTABLE, content);
    }

    /**
     * Generates a 406 Not Acceptable result.
     *
     * @param content the result's body content as a play-json object
     * @param encoding the encoding into which the json should be encoded
     * @return the result
     */
    public static Result notAcceptable(JsonNode content, JsonEncoding encoding) {
        return status(NOT_ACCEPTABLE, content, encoding);
    }

    /**
     * Generates a 406 Not Acceptable result.
     *
     * @param content the result's body content
     * @return the result
     */
    public static Result notAcceptable(byte[] content) {
        return status(NOT_ACCEPTABLE, content);
    }

    /**
     * Generates a 406 Not Acceptable result.
     *
     * @param content the input stream containing data to chunk over
     * @return the result
     */
    public static Result notAcceptable(InputStream content) {
        return status(NOT_ACCEPTABLE, content);
    }

    /**
     * Generates a 406 Not Acceptable result.
     *
     * @param content the input stream containing data to chunk over
     * @param contentLength the length of the provided content in bytes.
     * @return the result
     */
    public static Result notAcceptable(InputStream content, long contentLength) {
        return status(NOT_ACCEPTABLE, content, contentLength);
    }

    /**
     * Generates a 406 Not Acceptable result.
     *
     * @param content The file to send.
     * @return the result
     */
    public static Result notAcceptable(File content) {
        return status(NOT_ACCEPTABLE, content);
    }

    /**
     * Generates a 406 Not Acceptable result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     * @return the result
     */
    public static Result notAcceptable(File content, boolean inline) {
        return status(NOT_ACCEPTABLE, content, inline);
    }

    /**
     * Generates a 406 Not Acceptable result.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     * @return the result
     */
    public static Result notAcceptable(File content, String filename) {
        return status(NOT_ACCEPTABLE, content, filename);
    }


    /**
     * Generates a 415 Unsupported Media Type result.
     *
     * @return the result
     */
    public static StatusHeader unsupportedMediaType() {
        return new StatusHeader(UNSUPPORTED_MEDIA_TYPE);
    }

    /**
     * Generates a 415 Unsupported Media Type result.
     *
     * @param content the HTTP response body
     * @return the result
     */
    public static Result unsupportedMediaType(Content content) {
        return status(UNSUPPORTED_MEDIA_TYPE, content);
    }

    /**
     * Generates a 415 Unsupported Media Type result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result unsupportedMediaType(Content content, String charset) {
        return status(UNSUPPORTED_MEDIA_TYPE, content, charset);
    }

    /**
     * Generates a 415 Unsupported Media Type result.
     *
     * @param content HTTP response body, encoded as a UTF-8 string
     * @return the result
     */
    public static Result unsupportedMediaType(String content) {
        return status(UNSUPPORTED_MEDIA_TYPE, content);
    }

    /**
     * Generates a 415 Unsupported Media Type result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result unsupportedMediaType(String content, String charset) {
        return status(UNSUPPORTED_MEDIA_TYPE, content, charset);
    }

    /**
     * Generates a 415 Unsupported Media Type result.
     *
     * @param content the result's body content as a play-json object. It will be encoded
     *        as a UTF-8 string.
     * @return the result
     */
    public static Result unsupportedMediaType(JsonNode content) {
        return status(UNSUPPORTED_MEDIA_TYPE, content);
    }

    /**
     * Generates a 415 Unsupported Media Type result.
     *
     * @param content the result's body content as a play-json object
     * @param encoding the encoding into which the json should be encoded
     * @return the result
     */
    public static Result unsupportedMediaType(JsonNode content, JsonEncoding encoding) {
        return status(UNSUPPORTED_MEDIA_TYPE, content, encoding);
    }

    /**
     * Generates a 415 Unsupported Media Type result.
     *
     * @param content the result's body content
     * @return the result
     */
    public static Result unsupportedMediaType(byte[] content) {
        return status(UNSUPPORTED_MEDIA_TYPE, content);
    }

    /**
     * Generates a 415 Unsupported Media Type result.
     *
     * @param content the input stream containing data to chunk over
     * @return the result
     */
    public static Result unsupportedMediaType(InputStream content) {
        return status(UNSUPPORTED_MEDIA_TYPE, content);
    }

    /**
     * Generates a 415 Unsupported Media Type result.
     *
     * @param content the input stream containing data to chunk over
     * @param contentLength the length of the provided content in bytes.
     * @return the result
     */
    public static Result unsupportedMediaType(InputStream content, long contentLength) {
        return status(UNSUPPORTED_MEDIA_TYPE, content, contentLength);
    }

    /**
     * Generates a 415 Unsupported Media Type result.
     *
     * @param content The file to send.
     * @return the result
     */
    public static Result unsupportedMediaType(File content) {
        return status(UNSUPPORTED_MEDIA_TYPE, content);
    }

    /**
     * Generates a 415 Unsupported Media Type result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     * @return the result
     */
    public static Result unsupportedMediaType(File content, boolean inline) {
        return status(UNSUPPORTED_MEDIA_TYPE, content, inline);
    }

    /**
     * Generates a 415 Unsupported Media Type result.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     * @return the result
     */
    public static Result unsupportedMediaType(File content, String filename) {
        return status(UNSUPPORTED_MEDIA_TYPE, content, filename);
    }

    /**
     * Generates a 500 Internal Server Error result.
     *
     * @return the result
     */
    public static StatusHeader internalServerError() {
        return new StatusHeader(INTERNAL_SERVER_ERROR);
    }

    /**
     * Generates a 500 Internal Server Error result.
     *
     * @param content the HTTP response body
     * @return the result
     */
    public static Result internalServerError(Content content) {
        return status(INTERNAL_SERVER_ERROR, content);
    }

    /**
     * Generates a 500 Internal Server Error result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result internalServerError(Content content, String charset) {
        return status(INTERNAL_SERVER_ERROR, content, charset);
    }

    /**
     * Generates a 500 Internal Server Error result.
     *
     * @param content HTTP response body, encoded as a UTF-8 string
     * @return the result
     */
    public static Result internalServerError(String content) {
        return status(INTERNAL_SERVER_ERROR, content);
    }

    /**
     * Generates a 500 Internal Server Error result.
     *
     * @param content the HTTP response body
     * @param charset the charset into which the content should be encoded (e.g. "UTF-8")
     * @return the result
     */
    public static Result internalServerError(String content, String charset) {
        return status(INTERNAL_SERVER_ERROR, content, charset);
    }

    /**
     * Generates a 500 Internal Server Error result.
     *
     * @param content the result's body content as a play-json object. It will be encoded
     *        as a UTF-8 string.
     * @return the result
     */
    public static Result internalServerError(JsonNode content) {
        return status(INTERNAL_SERVER_ERROR, content);
    }

    /**
     * Generates a 500 Internal Server Error result.
     *
     * @param content the result's body content as a play-json object
     * @param encoding the encoding into which the json should be encoded
     * @return the result
     */
    public static Result internalServerError(JsonNode content, JsonEncoding encoding) {
        return status(INTERNAL_SERVER_ERROR, content, encoding);
    }

    /**
     * Generates a 500 Internal Server Error result.
     *
     * @param content the result's body content
     * @return the result
     */
    public static Result internalServerError(byte[] content) {
        return status(INTERNAL_SERVER_ERROR, content);
    }

    /**
     * Generates a 500 Internal Server Error result.
     *
     * @param content the input stream containing data to chunk over
     * @return the result
     */
    public static Result internalServerError(InputStream content) {
        return status(INTERNAL_SERVER_ERROR, content);
    }

    /**
     * Generates a 500 Internal Server Error result.
     *
     * @param content the input stream containing data to chunk over
     * @param contentLength the length of the provided content in bytes.
     * @return the result
     */
    public static Result internalServerError(InputStream content, long contentLength) {
        return status(INTERNAL_SERVER_ERROR, content, contentLength);
    }

    /**
     * Generates a 500 Internal Server Error result.
     *
     * @param content The file to send.
     * @return the result
     */
    public static Result internalServerError(File content) {
        return status(INTERNAL_SERVER_ERROR, content);
    }

    /**
     * Generates a 500 Internal Server Error result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     * @return the result
     */
    public static Result internalServerError(File content, boolean inline) {
        return status(INTERNAL_SERVER_ERROR, content, inline);
    }

    /**
     * Generates a 500 Internal Server Error result.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     * @return the result
     */
    public static Result internalServerError(File content, String filename) {
        return status(INTERNAL_SERVER_ERROR, content, filename);
    }

    /**
     * Generates a 301 Moved Permanently result.
     *
     * @param url The url to redirect.
     * @return the result
     */
    public static Result movedPermanently(String url) {
        return new Result(MOVED_PERMANENTLY, Collections.singletonMap(LOCATION, url));
    }

    /**
     * Generates a 301 Moved Permanently result.
     *
     * @param call Call defining the url to redirect (typically comes from reverse router).
     * @return the result
     */
    public static Result movedPermanently(Call call) {
        return new Result(MOVED_PERMANENTLY, Collections.singletonMap(LOCATION, call.path()));
    }

    /**
     * Generates a 302 Found result.
     *
     * @param url The url to redirect.
     * @return the result
     */
    public static Result found(String url) {
        return new Result(FOUND, Collections.singletonMap(LOCATION, url));
    }

    /**
     * Generates a 302 Found result.
     *
     * @param call Call defining the url to redirect (typically comes from reverse router).
     * @return the result
     */
    public static Result found(Call call) {
        return new Result(FOUND, Collections.singletonMap(LOCATION, call.path()));
    }

    /**
     * Generates a 303 See Other result.
     *
     * @param url The url to redirect.
     * @return the result
     */
    public static Result seeOther(String url) {
        return new Result(SEE_OTHER, Collections.singletonMap(LOCATION, url));
    }

    /**
     * Generates a 303 See Other result.
     *
     * @param call Call defining the url to redirect (typically comes from reverse router).
     * @return the result
     */
    public static Result seeOther(Call call) {
        return new Result(SEE_OTHER, Collections.singletonMap(LOCATION, call.path()));
    }

    /**
     * Generates a 303 See Other result.
     *
     * @param url The url to redirect.
     * @return the result
     */
    public static Result redirect(String url) {
        return new Result(SEE_OTHER, Collections.singletonMap(LOCATION, url));
    }

    /**
     * Generates a 303 See Other result.
     *
     * @param call Call defining the url to redirect (typically comes from reverse router).
     * @return the result
     */
    public static Result redirect(Call call) {
        return new Result(SEE_OTHER, Collections.singletonMap(LOCATION, call.path()));
    }

    /**
     * Generates a 307 Temporary Redirect result.
     *
     * @param url The url to redirect.
     * @return the result
     */
    public static Result temporaryRedirect(String url) {
        return new Result(TEMPORARY_REDIRECT, Collections.singletonMap(LOCATION, url));
    }

    /**
     * Generates a 307 Temporary Redirect result.
     *
     * @param call Call defining the url to redirect (typically comes from reverse router).
     * @return the result
     */
    public static Result temporaryRedirect(Call call) {
        return new Result(TEMPORARY_REDIRECT, Collections.singletonMap(LOCATION, call.path()));
    }

    /**
     * Generates a 308 Permanent Redirect result.
     *
     * @param url The url to redirect.
     * @return the result
     */
    public static Result permanentRedirect(String url) {
        return new Result(PERMANENT_REDIRECT, Collections.singletonMap(LOCATION, url));
    }

    /**
     * Generates a 308 Permanent Redirect result.
     *
     * @param call Call defining the url to redirect (typically comes from reverse router).
     * @return the result
     */
    public static Result permanentRedirect(Call call) {
        return new Result(PERMANENT_REDIRECT, Collections.singletonMap(LOCATION, call.path()));
    }

}
