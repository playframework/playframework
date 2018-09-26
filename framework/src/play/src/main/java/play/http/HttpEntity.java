/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.http;

import akka.japi.pf.PFBuilder;
import akka.stream.Materializer;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import play.api.http.HttpChunk;
import play.twirl.api.Content;
import play.twirl.api.Xml;
import scala.compat.java8.OptionConverters;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * An HTTP entity
 */
public abstract class HttpEntity {

    // sealed
    private HttpEntity() {}

    /**
     * @return The content type, if defined
     */
    public abstract Optional<String> contentType();

    /**
     * @return  Whether the entity is known to be empty or not.
     */
    public abstract boolean isKnownEmpty();

    /**
     * @return The content length, if known
     */
    public abstract Optional<Long> contentLength();

    /**
     * @return The stream of data.
     */
    public abstract Source<ByteString, ?> dataStream();

    /**
     * @param contentType    the content type to use, i.e. "text/html".
     * @return Return the entity as the given content type.
     */
    public abstract HttpEntity as(String contentType);

    /**
     * Consumes the data.
     *
     * This method should be used carefully, since if the source represents an ephemeral stream, then the entity may
     * not be usable after this method is invoked.
     * @param mat    the application's materializer.
     * @return a CompletionStage holding the data
     */
    public CompletionStage<ByteString> consumeData(Materializer mat) {
        return dataStream().runFold(ByteString.empty(), ByteString::concat, mat);
    }

    public abstract play.api.http.HttpEntity asScala();

    /**
     * No entity.
     */
    public static final HttpEntity NO_ENTITY = new Strict(ByteString.empty(), Optional.empty());

    /**
     * Create an entity from the given content.
     *
     * @param content The content.
     * @param charset The charset.
     *
     * @return the HTTP entity.
     */
    public static final HttpEntity fromContent(Content content, String charset) {
        String body;
        if (content instanceof Xml) {
            // See https://github.com/playframework/playframework/issues/2770
            body = content.body().trim();
        } else {
            body = content.body();
        }
        return new Strict(ByteString.fromString(body, charset), Optional.of(content.contentType() + "; charset=" + charset));
    }

    /**
     * Create an entity from the given String.
     *
     * @param content The content.
     * @param charset The charset.
     * @return the HTTP entity.
     */
    public static final HttpEntity fromString(String content, String charset) {
        return new Strict(ByteString.fromString(content, charset), Optional.of("text/plain; charset=" + charset));
    }

    /**
     * Convert the given source of ByteStrings to a chunked entity.
     *
     * @param data The source.
     * @param contentType The optional content type.
     * @return The ByteStrings.
     */
    public static final HttpEntity chunked(Source<ByteString, ?> data, Optional<String> contentType) {
        return new Chunked(data.map(HttpChunk.Chunk::new), contentType);
    }

    /**
     * A strict entity, where all the data for it is in memory.
     */
    public final static class Strict extends HttpEntity {
        private final ByteString data;
        private final Optional<String> contentType;

        public Strict(ByteString data, Optional<String> contentType) {
            this.data = data;
            this.contentType = contentType;
        }

        public ByteString data() {
            return data;
        }

        @Override
        public Optional<String> contentType() {
            return contentType;
        }

        @Override
        public boolean isKnownEmpty() {
            return data.isEmpty();
        }

        @Override
        public Optional<Long> contentLength() {
            return Optional.of((long) data.length());
        }

        @Override
        public HttpEntity as(String contentType) {
            return new Strict(data, Optional.of(contentType));
        }

        @Override
        public Source<ByteString, ?> dataStream() {
            return Source.<ByteString>single(data);
        }

        @Override
        public play.api.http.HttpEntity asScala() {
            return new play.api.http.HttpEntity.Strict(data, OptionConverters.toScala(contentType));
        }
    }

    /**
     * A streamed entity, backed by a source.
     */
    public final static class Streamed extends HttpEntity {
        private final Source<ByteString, ?> data;
        private final Optional<Long> contentLength;
        private final Optional<String> contentType;

        public Streamed(Source<ByteString, ?> data, Optional<Long> contentLength, Optional<String> contentType) {
            this.data = data;
            this.contentType = contentType;
            this.contentLength = contentLength;
        }

        public Source<ByteString, ?> data() {
            return data;
        }

        @Override
        public Optional<String> contentType() {
            return contentType;
        }

        @Override
        public boolean isKnownEmpty() {
            return false;
        }

        @Override
        public Optional<Long> contentLength() {
            return contentLength;
        }

        @Override
        public HttpEntity as(String contentType) {
            return new Streamed(data, contentLength, Optional.of(contentType));
        }

        @Override
        public Source<ByteString, ?> dataStream() {
            return data;
        }

        @Override
        @SuppressWarnings("unchecked")
        public play.api.http.HttpEntity asScala() {
            return new play.api.http.HttpEntity.Streamed(data.asScala(),
                    /* scala Option[Long] produces a Java generic signature of Option<Object>, so we need to do an
                       unchecked cast here to get it to typecheck */
                    (scala.Option) OptionConverters.toScala(contentLength),
                    OptionConverters.toScala(contentType));
        }
    }

    /**
     * A chunked entity, backed by a source of chunks.
     */
    public final static class Chunked extends HttpEntity {
        private final Source<HttpChunk, ?> chunks;
        private final Optional<String> contentType;

        public Chunked(Source<HttpChunk, ?> chunks, Optional<String> contentType) {
            this.chunks = chunks;
            this.contentType = contentType;
        }

        public Source<HttpChunk, ?> chunks() {
            return chunks;
        }

        @Override
        public Optional<String> contentType() {
            return contentType;
        }

        @Override
        public boolean isKnownEmpty() {
            return false;
        }

        @Override
        public Optional<Long> contentLength() {
            return Optional.empty();
        }

        @Override
        public HttpEntity as(String contentType) {
            return new Chunked(chunks, Optional.of(contentType));
        }

        @Override
        public Source<ByteString, ?> dataStream() {
            return chunks.<ByteString>collect(new PFBuilder<HttpChunk, ByteString>()
                    .match(HttpChunk.Chunk.class, HttpChunk.Chunk::data)
                    .build()
            );
        }

        @Override
        public play.api.http.HttpEntity asScala() {
            return new play.api.http.HttpEntity.Chunked(chunks.asScala(), OptionConverters.toScala(contentType));
        }
    }
}
