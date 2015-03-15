/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.mvc;

import play.api.libs.iteratee.Concurrent;
import play.api.libs.iteratee.Enumerator;
import play.api.mvc.*;

import play.core.j.JavaResults;
import play.libs.*;
import play.libs.F.*;

import play.twirl.api.Content;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;


/**
 * Common results.
 */
public class Results {

    static Codec utf8 = Codec.javaSupported("utf-8");
    static int defaultChunkSize = 1024 * 8;

    // -- Constructor methods

    /**
     * Generates a 501 NOT_IMPLEMENTED simple result.
     */
    public static Result TODO = new Todo();

    // -- Status

    /**
     * Generates a simple result.
     */
    public static StatusHeader status(int status) {
        return new StatusHeader(play.core.j.JavaResults.Status(status));
    }

    /**
     * Generates a simple result.
     */
    public static Status status(int status, Content content) {
        return new Status(play.core.j.JavaResults.Status(status), content, utf8);
    }

    /**
     * Generates a simple result.
     */
    public static Status status(int status, Content content, String charset) {
        return new Status(play.core.j.JavaResults.Status(status), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a simple result.
     */
    public static Status status(int status, String content) {
        return new Status(play.core.j.JavaResults.Status(status), content, utf8);
    }

    /**
     * Generates a simple result.
     */
    public static Status status(int status, String content, String charset) {
        return new Status(play.core.j.JavaResults.Status(status), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a simple result.
     */
    public static Status status(int status, JsonNode content) {
        return new Status(play.core.j.JavaResults.Status(status), content, utf8);
    }

    /**
     * Generates a simple result.
     */
    public static Status status(int status, JsonNode content, String charset) {
        return new Status(play.core.j.JavaResults.Status(status), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a simple result.
     */
    public static Status status(int status, byte[] content) {
        return new Status(play.core.j.JavaResults.Status(status), content);
    }

    /**
     * Generates a chunked result.
     */
    public static Status status(int status, InputStream content) {
        return status(status, content, defaultChunkSize);
    }

    /**
     * Generates a chunked result.
     */
    public static Status status(int status, InputStream content, int chunkSize) {
        return new Status(play.core.j.JavaResults.Status(status), content, chunkSize);
    }

    /**
     * Generates a chunked result.
     */
    public static Status status(int status, File content) {
        return new Status(play.core.j.JavaResults.Status(status), content);
    }

    /**
     * Generates a simple result.
     */
    public static Status status(int status, File content, int chunkSize) {
        return new Status(play.core.j.JavaResults.Status(status), content, chunkSize);
    }

    /**
     * Generates a chunked result.
     */
    public static Status status(int status, Chunks<?> chunks) {
        return new Status(play.core.j.JavaResults.Status(status), chunks);
    }

    // -- OK

    /**
     * Generates a 200 OK simple result.
     */
    public static StatusHeader ok() {
        return new StatusHeader(play.core.j.JavaResults.Ok());
    }

    /**
     * Generates a 200 OK simple result.
     */
    public static Status ok(Content content) {
        return new Status(play.core.j.JavaResults.Ok(), content, utf8);
    }

    /**
     * Generates a 200 OK simple result.
     */
    public static Status ok(Content content, String charset) {
        return new Status(play.core.j.JavaResults.Ok(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 200 OK simple result.
     */
    public static Status ok(String content) {
        return new Status(play.core.j.JavaResults.Ok(), content, utf8);
    }

    /**
     * Generates a 200 OK simple result.
     */
    public static Status ok(String content, String charset) {
        return new Status(play.core.j.JavaResults.Ok(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 200 OK simple result.
     */
    public static Status ok(JsonNode content) {
        return new Status(play.core.j.JavaResults.Ok(), content, utf8);
    }

    /**
     * Generates a 200 OK simple result.
     */
    public static Status ok(JsonNode content, String charset) {
        return new Status(play.core.j.JavaResults.Ok(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 200 OK simple result.
     */
    public static Status ok(byte[] content) {
        return new Status(play.core.j.JavaResults.Ok(), content);
    }

    /**
     * Generates a 200 OK chunked result.
     */
    public static Status ok(InputStream content) {
        return ok(content, defaultChunkSize);
    }

    /**
     * Generates a 200 OK chunked result.
     */
    public static Status ok(InputStream content, int chunkSize) {
        return new Status(play.core.j.JavaResults.Ok(), content, chunkSize);
    }

    /**
     * Generates a 200 OK file result as an attachment.
     *
     * @param content The file to send.
     */
    public static Status ok(File content) {
        return new Status(play.core.j.JavaResults.Ok(), content);
    }

    /**
     * Generates a 200 OK file result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     */
    public static Status ok(File content, boolean inline) {
        return new Status(JavaResults.Ok(), content, inline);
    }

    /**
     * Generates a 200 OK file result as an attachment.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     */
    public static Status ok(File content, String filename) {
        return new Status(JavaResults.Ok(), content, true, filename);
    }

    /**
     * Generates a 200 OK chunked result.
     */
    public static Status ok(Chunks<?> chunks) {
        return new Status(play.core.j.JavaResults.Ok(), chunks);
    }

    // -- CREATED

    /**
     * Generates a 201 CREATED simple result.
     */
    public static StatusHeader created() {
        return new StatusHeader(play.core.j.JavaResults.Created());
    }

    /**
     * Generates a 201 CREATED simple result.
     */
    public static Status created(Content content) {
        return new Status(play.core.j.JavaResults.Created(), content, utf8);
    }

    /**
     * Generates a 201 CREATED simple result.
     */
    public static Status created(Content content, String charset) {
        return new Status(play.core.j.JavaResults.Created(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 201 CREATED simple result.
     */
    public static Status created(String content) {
        return new Status(play.core.j.JavaResults.Created(), content, utf8);
    }

    /**
     * Generates a 201 CREATED simple result.
     */
    public static Status created(String content, String charset) {
        return new Status(play.core.j.JavaResults.Created(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 201 CREATED simple result.
     */
    public static Status created(JsonNode content) {
        return new Status(play.core.j.JavaResults.Created(), content, utf8);
    }

    /**
     * Generates a 201 CREATED simple result.
     */
    public static Status created(JsonNode content, String charset) {
        return new Status(play.core.j.JavaResults.Created(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 201 CREATED simple result.
     */
    public static Status created(byte[] content) {
        return new Status(play.core.j.JavaResults.Created(), content);
    }

    /**
     * Generates a 201 CREATED chunked result.
     */
    public static Status created(InputStream content) {
        return created(content, defaultChunkSize);
    }

    /**
     * Generates a 201 CREATED chunked result.
     */
    public static Status created(InputStream content, int chunkSize) {
        return new Status(play.core.j.JavaResults.Created(), content, chunkSize);
    }

    /**
     * Generates a 201 CREATED file result as an attachment.
     *
     * @param content The file to send.
     */
    public static Status created(File content) {
        return new Status(play.core.j.JavaResults.Created(), content);
    }

    /**
     * Generates a 201 CREATED file result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     */
    public static Status created(File content, boolean inline) {
        return new Status(JavaResults.Created(), content, inline);
    }

    /**
     * Generates a 201 CREATED file result as an attachment.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     */
    public static Status created(File content, String filename) {
        return new Status(JavaResults.Created(), content, true, filename);
    }

    /**
     * Generates a 201 CREATED chunked result.
     */
    public static Status created(Chunks<?> chunks) {
        return new Status(play.core.j.JavaResults.Created(), chunks);
    }

    // -- NO_CONTENT

    /**
     * Generates a 204 NO_CONTENT simple result.
     */
    public static Status noContent() {
        return new Status(play.core.j.JavaResults.NoContent());
    }

    // -- INTERNAL_SERVER_ERROR

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR simple result.
     */
    public static StatusHeader internalServerError() {
        return new StatusHeader(play.core.j.JavaResults.InternalServerError());
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR simple result.
     */
    public static Status internalServerError(Content content) {
        return new Status(play.core.j.JavaResults.InternalServerError(), content, utf8);
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR simple result.
     */
    public static Status internalServerError(Content content, String charset) {
        return new Status(play.core.j.JavaResults.InternalServerError(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR simple result.
     */
    public static Status internalServerError(String content) {
        return new Status(play.core.j.JavaResults.InternalServerError(), content, utf8);
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR simple result.
     */
    public static Status internalServerError(String content, String charset) {
        return new Status(play.core.j.JavaResults.InternalServerError(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR simple result.
     */
    public static Status internalServerError(JsonNode content) {
        return new Status(play.core.j.JavaResults.InternalServerError(), content, utf8);
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR simple result.
     */
    public static Status internalServerError(JsonNode content, String charset) {
        return new Status(play.core.j.JavaResults.InternalServerError(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR simple result.
     */
    public static Status internalServerError(byte[] content) {
        return new Status(play.core.j.JavaResults.InternalServerError(), content);
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR chunked result.
     */
    public static Status internalServerError(InputStream content) {
        return internalServerError(content, defaultChunkSize);
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR chunked result.
     */
    public static Status internalServerError(InputStream content, int chunkSize) {
        return new Status(play.core.j.JavaResults.InternalServerError(), content, chunkSize);
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR file result as an attachment.
     *
     * @param content The file to send.
     */
    public static Status internalServerError(File content) {
        return new Status(play.core.j.JavaResults.InternalServerError(), content);
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR file result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     */
    public static Status internalServerError(File content, boolean inline) {
        return new Status(JavaResults.InternalServerError(), content, inline);
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR file result as an attachment.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     */
    public static Status internalServerError(File content, String filename) {
        return new Status(JavaResults.InternalServerError(), content, true, filename);
    }

    /**
     * Generates a 500 INTERNAL_SERVER_ERROR chunked result.
     */
    public static Status internalServerError(Chunks<?> chunks) {
        return new Status(play.core.j.JavaResults.InternalServerError(), chunks);
    }

    // -- NOT_FOUND

    /**
     * Generates a 404 NOT_FOUND simple result.
     */
    public static StatusHeader notFound() {
        return new StatusHeader(play.core.j.JavaResults.NotFound());
    }

    /**
     * Generates a 404 NOT_FOUND simple result.
     */
    public static Status notFound(Content content) {
        return new Status(play.core.j.JavaResults.NotFound(), content, utf8);
    }

    /**
     * Generates a 404 NOT_FOUND simple result.
     */
    public static Status notFound(Content content, String charset) {
        return new Status(play.core.j.JavaResults.NotFound(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 404 NOT_FOUND simple result.
     */
    public static Status notFound(String content) {
        return new Status(play.core.j.JavaResults.NotFound(), content, utf8);
    }

    /**
     * Generates a 404 NOT_FOUND simple result.
     */
    public static Status notFound(String content, String charset) {
        return new Status(play.core.j.JavaResults.NotFound(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 404 NOT_FOUND simple result.
     */
    public static Status notFound(JsonNode content) {
        return new Status(play.core.j.JavaResults.NotFound(), content, utf8);
    }

    /**
     * Generates a 404 NOT_FOUND simple result.
     */
    public static Status notFound(JsonNode content, String charset) {
        return new Status(play.core.j.JavaResults.NotFound(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 404 NOT_FOUND simple result.
     */
    public static Status notFound(byte[] content) {
        return new Status(play.core.j.JavaResults.NotFound(), content);
    }

    /**
     * Generates a 404 NOT_FOUND chunked result.
     */
    public static Status notFound(InputStream content) {
        return notFound(content, defaultChunkSize);
    }

    /**
     * Generates a 404 NOT_FOUND chunked result.
     */
    public static Status notFound(InputStream content, int chunkSize) {
        return new Status(play.core.j.JavaResults.NotFound(), content, chunkSize);
    }

    /**
     * Generates a 404 NOT_FOUND file result as an attachment.
     *
     * @param content The file to send.
     */
    public static Status notFound(File content) {
        return new Status(play.core.j.JavaResults.NotFound(), content);
    }

    /**
     * Generates a 404 NOT_FOUND file result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     */
    public static Status notFound(File content, boolean inline) {
        return new Status(JavaResults.NotFound(), content, inline);
    }

    /**
     * Generates a 404 NOT_FOUND file result as an attachment.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     */
    public static Status notFound(File content, String filename) {
        return new Status(JavaResults.NotFound(), content, true, filename);
    }

    /**
     * Generates a 404 NOT_FOUND chunked result.
     */
    public static Status notFound(Chunks<?> chunks) {
        return new Status(play.core.j.JavaResults.NotFound(), chunks);
    }

    // -- FORBIDDEN

    /**
     * Generates a 403 FORBIDDEN simple result.
     */
    public static StatusHeader forbidden() {
        return new StatusHeader(play.core.j.JavaResults.Forbidden());
    }

    /**
     * Generates a 403 FORBIDDEN simple result.
     */
    public static Status forbidden(Content content) {
        return new Status(play.core.j.JavaResults.Forbidden(), content, utf8);
    }

    /**
     * Generates a 403 FORBIDDEN simple result.
     */
    public static Status forbidden(Content content, String charset) {
        return new Status(play.core.j.JavaResults.Forbidden(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 403 FORBIDDEN simple result.
     */
    public static Status forbidden(String content) {
        return new Status(play.core.j.JavaResults.Forbidden(), content, utf8);
    }

    /**
     * Generates a 403 FORBIDDEN simple result.
     */
    public static Status forbidden(String content, String charset) {
        return new Status(play.core.j.JavaResults.Forbidden(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 403 FORBIDDEN simple result.
     */
    public static Status forbidden(JsonNode content) {
        return new Status(play.core.j.JavaResults.Forbidden(), content, utf8);
    }

    /**
     * Generates a 403 FORBIDDEN simple result.
     */
    public static Status forbidden(JsonNode content, String charset) {
        return new Status(play.core.j.JavaResults.Forbidden(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 403 FORBIDDEN simple result.
     */
    public static Status forbidden(byte[] content) {
        return new Status(play.core.j.JavaResults.Forbidden(), content);
    }

    /**
     * Generates a 403 FORBIDDEN chunked result.
     */
    public static Status forbidden(InputStream content) {
        return forbidden(content, defaultChunkSize);
    }

    /**
     * Generates a 403 FORBIDDEN chunked result.
     */
    public static Status forbidden(InputStream content, int chunkSize) {
        return new Status(play.core.j.JavaResults.Forbidden(), content, chunkSize);
    }

    /**
     * Generates a 403 FORBIDDEN file result as an attachment.
     *
     * @param content The file to send.
     */
    public static Status forbidden(File content) {
        return new Status(play.core.j.JavaResults.Forbidden(), content);
    }

    /**
     * Generates a 403 FORBIDDEN file result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     */
    public static Status forbidden(File content, boolean inline) {
        return new Status(JavaResults.Forbidden(), content, inline);
    }

    /**
     * Generates a 403 FORBIDDEN file result as an attachment.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     */
    public static Status forbidden(File content, String filename) {
        return new Status(JavaResults.Forbidden(), content, true, filename);
    }

    /**
     * Generates a 403 FORBIDDEN chunked result.
     */
    public static Status forbidden(Chunks<?> chunks) {
        return new Status(play.core.j.JavaResults.Forbidden(), chunks);
    }

    // -- UNAUTHORIZED

    /**
     * Generates a 401 UNAUTHORIZED simple result.
     */
    public static StatusHeader unauthorized() {
        return new StatusHeader(play.core.j.JavaResults.Unauthorized());
    }

    /**
     * Generates a 401 UNAUTHORIZED simple result.
     */
    public static Status unauthorized(Content content) {
        return new Status(play.core.j.JavaResults.Unauthorized(), content, utf8);
    }

    /**
     * Generates a 401 UNAUTHORIZED simple result.
     */
    public static Status unauthorized(Content content, String charset) {
        return new Status(play.core.j.JavaResults.Unauthorized(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 401 UNAUTHORIZED simple result.
     */
    public static Status unauthorized(String content) {
        return new Status(play.core.j.JavaResults.Unauthorized(), content, utf8);
    }

    /**
     * Generates a 401 UNAUTHORIZED simple result.
     */
    public static Status unauthorized(String content, String charset) {
        return new Status(play.core.j.JavaResults.Unauthorized(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 401 UNAUTHORIZED simple result.
     */
    public static Status unauthorized(JsonNode content) {
        return new Status(play.core.j.JavaResults.Unauthorized(), content, utf8);
    }

    /**
     * Generates a 401 UNAUTHORIZED simple result.
     */
    public static Status unauthorized(JsonNode content, String charset) {
        return new Status(play.core.j.JavaResults.Unauthorized(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 401 UNAUTHORIZED simple result.
     */
    public static Status unauthorized(byte[] content) {
        return new Status(play.core.j.JavaResults.Unauthorized(), content);
    }

    /**
     * Generates a 401 UNAUTHORIZED chunked result.
     */
    public static Status unauthorized(InputStream content) {
        return unauthorized(content, defaultChunkSize);
    }

    /**
     * Generates a 401 UNAUTHORIZED chunked result.
     */
    public static Status unauthorized(InputStream content, int chunkSize) {
        return new Status(play.core.j.JavaResults.Unauthorized(), content, chunkSize);
    }

    /**
     * Generates a 401 UNAUTHORIZED file result as an attachment.
     *
     * @param content The file to send.
     */
    public static Status unauthorized(File content) {
        return new Status(play.core.j.JavaResults.Unauthorized(), content);
    }

    /**
     * Generates a 401 UNAUTHORIZED file result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     */
    public static Status unauthorized(File content, boolean inline) {
        return new Status(JavaResults.Unauthorized(), content, inline);
    }

    /**
     * Generates a 401 UNAUTHORIZED file result as an attachment.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     */
    public static Status unauthorized(File content, String filename) {
        return new Status(JavaResults.Unauthorized(), content, true, filename);
    }

    /**
     * Generates a 401 UNAUTHORIZED chunked result.
     */
    public static Status unauthorized(Chunks<?> chunks) {
        return new Status(play.core.j.JavaResults.Unauthorized(), chunks);
    }

    // -- BAD_REQUEST

    /**
     * Generates a 400 BAD_REQUEST simple result.
     */
    public static StatusHeader badRequest() {
        return new StatusHeader(play.core.j.JavaResults.BadRequest());
    }

    /**
     * Generates a 400 BAD_REQUEST simple result.
     */
    public static Status badRequest(Content content) {
        return new Status(play.core.j.JavaResults.BadRequest(), content, utf8);
    }

    /**
     * Generates a 400 BAD_REQUEST simple result.
     */
    public static Status badRequest(Content content, String charset) {
        return new Status(play.core.j.JavaResults.BadRequest(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 400 BAD_REQUEST simple result.
     */
    public static Status badRequest(String content) {
        return new Status(play.core.j.JavaResults.BadRequest(), content, utf8);
    }

    /**
     * Generates a 400 BAD_REQUEST simple result.
     */
    public static Status badRequest(String content, String charset) {
        return new Status(play.core.j.JavaResults.BadRequest(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 400 BAD_REQUEST simple result.
     */
    public static Status badRequest(JsonNode content) {
        return new Status(play.core.j.JavaResults.BadRequest(), content, utf8);
    }

    /**
     * Generates a 400 BAD_REQUEST simple result.
     */
    public static Status badRequest(JsonNode content, String charset) {
        return new Status(play.core.j.JavaResults.BadRequest(), content, Codec.javaSupported(charset));
    }

    /**
     * Generates a 400 BAD_REQUEST simple result.
     */
    public static Status badRequest(byte[] content) {
        return new Status(play.core.j.JavaResults.BadRequest(), content);
    }

    /**
     * Generates a 400 BAD_REQUEST chunked result.
     */
    public static Status badRequest(InputStream content) {
        return badRequest(content, defaultChunkSize);
    }

    /**
     * Generates a 400 BAD_REQUEST chunked result.
     */
    public static Status badRequest(InputStream content, int chunkSize) {
        return new Status(play.core.j.JavaResults.BadRequest(), content, chunkSize);
    }

    /**
     * Generates a 400 BAD_REQUEST file result as an attachment.
     *
     * @param content The file to send.
     */
    public static Status badRequest(File content) {
        return new Status(play.core.j.JavaResults.BadRequest(), content);
    }

    /**
     * Generates a 400 BAD_REQUEST file result.
     *
     * @param content The file to send.
     * @param inline Whether the file should be sent inline, or as an attachment.
     */
    public static Status badRequest(File content, boolean inline) {
        return new Status(JavaResults.BadRequest(), content, inline);
    }

    /**
     * Generates a 400 BAD_REQUEST file result as an attachment.
     *
     * @param content The file to send.
     * @param filename The name to send the file as.
     */
    public static Status badRequest(File content, String filename) {
        return new Status(JavaResults.BadRequest(), content, true, filename);
    }

    /**
     * Generates a 400 BAD_REQUEST chunked result.
     */
    public static Status badRequest(Chunks<?> chunks) {
        return new Status(play.core.j.JavaResults.BadRequest(), chunks);
    }

    // -- Redirect

    /**
     * Generates a 303 SEE_OTHER simple result.
     *
     * @param url The url to redirect.
     */
    public static Result redirect(String url) {
        return new Redirect(303, url);
    }

    /**
     * Generates a 303 SEE_OTHER simple result.
     *
     * @param call Call defining the url to redirect (typically comes from reverse router).
     */
    public static Result redirect(Call call) {
        return new Redirect(303, call.url());
    }

    // -- Found

    /**
     * Generates a 302 FOUND simple result.
     *
     * @param url The url to redirect.
     */
    public static Result found(String url) {
        return new Redirect(302, url);
    }

    /**
     * Generates a 302 FOUND simple result.
     *
     * @param call Call defining the url to redirect (typically comes from reverse router).
     */
    public static Result found(Call call) {
        return new Redirect(302, call.url());
    }

    // -- Moved Permanently

    /**
     * Generates a 301 MOVED_PERMANENTLY simple result.
     *
     * @param url The url to redirect.
     */
    public static Result movedPermanently(String url) {
        return new Redirect(301, url);
    }

    /**
     * Generates a 301 MOVED_PERMANENTLY simple result.
     *
     * @param call Call defining the url to redirect (typically comes from reverse router).
     */
    public static Result movedPermanently(Call call) {
        return new Redirect(301, call.url());
    }

    // -- See Other

    /**
     * Generates a 303 SEE_OTHER simple result.
     *
     * @param url The url to redirect.
     */
    public static Result seeOther(String url) {
        return new Redirect(303, url);
    }

    /**
     * Generates a 303 SEE_OTHER simple result.
     *
     * @param call Call defining the url to redirect (typically comes from reverse router).
     */
    public static Result seeOther(Call call) {
        return new Redirect(303, call.url());
    }

    // -- Temporary Redirect

    /**
     * Generates a 307 TEMPORARY_REDIRECT simple result.
     *
     * @param url The url to redirect.
     */
    public static Result temporaryRedirect(String url) {
        return new Redirect(307, url);
    }

    /**
     * Generates a 307 TEMPORARY_REDIRECT simple result.
     *
     * @param call Call defining the url to redirect (typically comes from reverse router).
     */
    public static Result temporaryRedirect(Call call) {
        return new Redirect(307, call.url());
    }

    // -- Definitions

    /**
     * A Chunked result.
     */
    public abstract static class Chunks<A> {

        final Enumerator<A> enumerator;
        final play.api.http.Writeable<A> writable;

        public Chunks(play.api.http.Writeable<A> writable) {
            final Chunks<A> self = this;
            this.writable = writable;
            final RedeemablePromise<Object> disconnected = RedeemablePromise.<Object>empty();
            this.enumerator = play.core.j.JavaResults.chunked(new Callback<Concurrent.Channel<A>>() {
                @Override
                public void invoke(Concurrent.Channel<A> channel) {
                    Chunks.Out<A> chunked = new Chunks.Out<A>(channel, disconnected);
                    self.onReady(chunked);
                }
            }, new Callback0() {
                @Override
                public void invoke() throws Throwable {
                    disconnected.success(null);
                }
            });
        }

        /**
         * Called when the Chunked stream is ready.
         *
         * @param out The out stream.
         */
        public abstract void onReady(Chunks.Out<A> out);

        /**
         * A Chunked stream.
         */
        public static class Out<A> {

            /** A Promise that will be redeemed to null when the channel is disconnected. */
            final RedeemablePromise<Object> disconnected;
            final play.api.libs.iteratee.Concurrent.Channel<A> channel;

            public Out(play.api.libs.iteratee.Concurrent.Channel<A> channel, RedeemablePromise<Object> disconnected) {
                this.channel = channel;
                this.disconnected = disconnected;
            }

            public Out(play.api.libs.iteratee.Concurrent.Channel<A> channel, List<Callback0> disconnectedCallbacks) {
                this.channel = channel;
                this.disconnected = RedeemablePromise.<Object>empty();
                for(Callback0 callback: disconnectedCallbacks) {
                    onDisconnected(callback);
                }
            }

            /**
             * Write a Chunk.
             */
            public void write(A chunk) {
                channel.push(chunk);
            }

            /**
             * Attach a callback to be called when the socket is disconnected.
             */
            public void onDisconnected(final Callback0 callback) {
                disconnected.onRedeem(new Callback<Object>() {
                    public void invoke(Object ignored) throws Throwable {
                        callback.invoke();
                    }
                });
            }

            /**
             * Closes the stream.
             */
            public void close() {
                channel.eofAndEnd();
            }

        }

    }

    /**
     * Chunked result based on String chunks.
     */
    public abstract static class StringChunks extends Chunks<String> {

        private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(StringChunks.class);

        public StringChunks() {
            this(utf8);
        }

        public StringChunks(String codec) {
            this(Codec.javaSupported(codec));
        }

        public StringChunks(Codec codec) {
            super(play.core.j.JavaResults.writeString(codec));
        }

        /**
         * Creates a StringChunks. The abstract {@code onReady} method is
         * implemented using the specified {@code Callback<Chunks.Out<String>>}.
         *
         * Uses UTF-8 by default.
         *
         * @param callback the callback used to implement onReady
         * @return a new StringChunks
         * @throws NullPointerException if the specified callback is null
         */
        public static StringChunks whenReady(Callback<Chunks.Out<String>> callback) {
            return whenReady(utf8, callback);
        }

        /**
         * Creates a StringChunks. The abstract {@code onReady} method is
         * implemented using the specified {@code Callback<Chunks.Out<String>>}.
         *
         * @param codec the Codec charset used
         * @param callback the callback used to implement onReady
         * @return a new StringChunks
         * @throws NullPointerException if the specified callback is null
         */
        public static StringChunks whenReady(String codec, Callback<Chunks.Out<String>> callback) {
            return whenReady(Codec.javaSupported(codec), callback);
        }

        /**
         * Creates a StringChunks. The abstract {@code onReady} method is
         * implemented using the specified {@code Callback<Chunks.Out<String>>}.
         *
         * @param codec the Codec used
         * @param callback the callback used to implement onReady
         * @return a new StringChunks
         * @throws NullPointerException if the specified callback is null
         */
        public static StringChunks whenReady(Codec codec, Callback<Chunks.Out<String>> callback) {
            return new WhenReadyStringChunks(codec, callback);
        }

        /**
         * An extension of StringChunks that obtains its onReady from
         * the specified {@code Callback<Chunks.Out<String>>}.
         */
        static final class WhenReadyStringChunks extends StringChunks {

            private final Callback<Chunks.Out<String>> callback;

            WhenReadyStringChunks(Codec codec, Callback<Chunks.Out<String>> callback) {
                super(codec);
                if (callback == null) throw new NullPointerException("StringChunks onReady callback cannot be null");
                this.callback = callback;
            }

            @Override
            public void onReady(Chunks.Out<String> out) {
                try {
                    callback.invoke(out);
                } catch (Throwable e) {
                    logger.error("Exception in StringChunks.onReady", e);
                }
            }
        }

    }

    /**
     * Chunked result based on byte[] chunks.
     */
    public abstract static class ByteChunks extends Chunks<byte[]> {

        public ByteChunks() {
            super(play.core.j.JavaResults.writeBytes());
        }

        /**
         * Creates a ByteChunks. The abstract {@code onReady} method is
         * implemented using the specified {@code Callback<Chunks.Out<byte[]>>}.
         *
         * @param callback the callback used to implement onReady
         * @return a new ByteChunks
         * @throws NullPointerException if the specified callback is null
         */
        public static ByteChunks whenReady(Callback<Chunks.Out<byte[]>> callback) {
            return new WhenReadyByteChunks(callback);
        }

        /**
         * An extension of ByteChunks that obtains its onReady from
         * the specified {@code Callback<Chunks.Out<byte[]>>}.
         */
        static final class WhenReadyByteChunks extends ByteChunks {

            private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WhenReadyByteChunks.class);

            private final Callback<Chunks.Out<byte[]>> callback;

            WhenReadyByteChunks(Callback<Chunks.Out<byte[]>> callback) {
                super();
                if (callback == null) throw new NullPointerException("ByteChunks onReady callback cannot be null");
                this.callback = callback;
            }

            @Override
            public void onReady(Chunks.Out<byte[]> out) {
                try {
                    callback.invoke(out);
                } catch (Throwable e) {
                    logger.error("Exception in ByteChunks.onReady", e);
                }
            }
        }

    }

    /**
     * A 501 NOT_IMPLEMENTED simple result.
     */
    public static class Todo implements Result {

        final private play.api.mvc.Result wrappedResult;

        public Todo() {
            wrappedResult = play.core.j.JavaResults.NotImplemented().apply(
                    views.html.defaultpages.todo.render(),
                    play.core.j.JavaResults.writeContent("text/html", utf8)
                    );
        }

        public play.api.mvc.Result toScala() {
            return this.wrappedResult;
        }
    }

    /**
     * A status with no body
     */
    public static class StatusHeader implements Result {

        private final play.api.mvc.Results.Status wrappedStatus;

        public StatusHeader(play.api.mvc.Results.Status wrappedStatus) {
            this.wrappedStatus = wrappedStatus;
        }

        /**
         * Send the given resource.
         *
         * The resource will be loaded from the same classloader that this class comes from.
         *
         * @param resourceName The path of the resource to load.
         */
        public Status sendResource(String resourceName) {
            return sendResource(resourceName, true);
        }

        /**
         * Send the given resource from the given classloader.
         *
         * @param resourceName The path of the resource to load.
         * @param classLoader The classloader to load it from.
         */
        public Status sendResource(String resourceName, ClassLoader classLoader) {
            return sendResource(resourceName, classLoader, true);
        }

        /**
         * Send the given resource.
         *
         * The resource will be loaded from the same classloader that this class comes from.
         *
         * @param resourceName The path of the resource to load.
         * @param inline Whether it should be served as an inline file, or as an attachment.
         */
        public Status sendResource(String resourceName, boolean inline) {
            return sendResource(resourceName, this.getClass().getClassLoader(), inline);
        }

        /**
         * Send the given resource from the given classloader.
         *
         * @param resourceName The path of the resource to load.
         * @param classLoader The classloader to load it from.
         * @param inline Whether it should be served as an inline file, or as an attachment.
         */
        public Status sendResource(String resourceName, ClassLoader classLoader, boolean inline) {
            return new Status(wrappedStatus.sendResource(resourceName, classLoader, inline));
        }

        public play.api.mvc.Result toScala() {
            return wrappedStatus;
        }
    }

    /**
     * A simple result.
     */
    public static class Status implements Result {

        private play.api.mvc.Result wrappedResult;

        public Status(play.api.mvc.Result wrappedResult) {
            this.wrappedResult = wrappedResult;
        }

        public Status(play.api.mvc.Results.Status status, String content, Codec codec) {
            if(content == null) {
                throw new NullPointerException("null content");
            }
            wrappedResult = status.apply(
                    content,
                    play.core.j.JavaResults.writeString(codec)
                    );
        }

        public Status(play.api.mvc.Results.Status status, JsonNode content, Codec codec) {
            if(content == null) {
                throw new NullPointerException("null content");
            }
            wrappedResult = status.apply(
                    content,
                    play.core.j.JavaResults.writeJson(codec)
                    );
        }

        public Status(play.api.mvc.Results.Status status, Content content, Codec codec) {
            if(content == null) {
                throw new NullPointerException("null content");
            }
            wrappedResult = status.apply(
                    content,
                    play.core.j.JavaResults.writeContent(content.contentType(), codec)
                    );
        }

        public <A> Status(play.api.mvc.Results.Status status, Chunks<A> chunks) {
            if(chunks == null) {
                throw new NullPointerException("null content");
            }
            wrappedResult = status.chunked(chunks.enumerator, chunks.writable);
        }

        public Status(play.api.mvc.Results.Status status, byte[] content) {
            if(content == null) {
                throw new NullPointerException("null content");
            }
            wrappedResult = status.apply(
                    content,
                    play.core.j.JavaResults.writeBytes()
                    );
        }

        public Status(play.api.mvc.Results.Status status, File content) {
            this(status, content, false);
        }

        public Status(play.api.mvc.Results.Status status, File content, boolean inline) {
            this(status, content, inline, content.getName());
        }

        public Status(play.api.mvc.Results.Status status, File content, boolean inline, String filename) {
            if(content == null) {
                throw new NullPointerException("null content");
            }
            wrappedResult = play.core.j.JavaResults.sendFile(status, content, inline, filename);
        }

        public Status(play.api.mvc.Results.Status status, File content, int chunkSize) {
            if(content == null) {
                throw new NullPointerException("null content");
            }
            wrappedResult = status.chunked(
                    play.core.j.JavaResults.chunked(content, chunkSize),
                    play.core.j.JavaResults.writeBytes(Scala.orNull(play.api.libs.MimeTypes.forFileName(content.getName())))
                    );
        }

        public Status(play.api.mvc.Results.Status status, InputStream content, int chunkSize) {
            if(content == null) {
                throw new NullPointerException("null content");
            }
            wrappedResult = status.stream(
                    play.core.j.JavaResults.chunked(content, chunkSize),
                    play.core.j.JavaResults.writeBytes()
                    );
        }

        public play.api.mvc.Result toScala() {
            return wrappedResult;
        }

        /**
         * Change the Content-Type header for this result.
         */
        public Status as(String contentType) {
            wrappedResult = wrappedResult.as(contentType);
            return this;
        }

        public String toString() {
            return wrappedResult.toString();
        }

    }

    /**
     * A redirect result.
     */
    public static class Redirect implements Result {

        final private play.api.mvc.Result wrappedResult;

        public Redirect(int status, String url) {
            wrappedResult = play.core.j.JavaResults.Redirect(url, status);
        }

        public play.api.mvc.Result toScala() {
            return this.wrappedResult;
        }

    }
}
