/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.ws;

import java.util.concurrent.CompletionStage;

import akka.stream.javadsl.Source;
import akka.util.ByteString;
import play.libs.ws.util.CollectionUtil;
import scala.compat.java8.FutureConverters;
import scala.concurrent.Future;

/**
 * A streamed response containing a response header and a streamable body. 
 */
public class StreamedResponse {

	private final WSResponseHeaders headers;
	private final Source<ByteString, ?> body;

	private StreamedResponse(WSResponseHeaders headers, Source<ByteString, ?> body) {
		this.headers = headers;
		this.body = body;
	}

	public WSResponseHeaders getHeaders() {
		return headers;
	}

	public Source<ByteString, ?> getBody() {
		return body;
	}
	
	public static CompletionStage<StreamedResponse> from(Future<play.api.libs.ws.StreamedResponse> from) {
		CompletionStage<play.api.libs.ws.StreamedResponse> res = FutureConverters.toJava(from);
		java.util.function.Function<play.api.libs.ws.StreamedResponse, StreamedResponse> mapper = response -> {
			WSResponseHeaders headers = toJavaHeaders(response.headers());
			Source<ByteString, ?> source = response.body().asJava();
			return new StreamedResponse(headers, source);
	    };
	    return res.thenApply(mapper);	
	}

	private static WSResponseHeaders toJavaHeaders(play.api.libs.ws.WSResponseHeaders from) {
		return new DefaultWSResponseHeaders(from.status(), CollectionUtil.convert(from.headers()));
	}
}
