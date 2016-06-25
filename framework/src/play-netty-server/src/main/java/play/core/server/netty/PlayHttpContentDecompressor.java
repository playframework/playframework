/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import java.util.List;

public class PlayHttpContentDecompressor extends HttpContentDecompressor {
    private boolean contentLength = false;

    @Override
    protected void decode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out) throws Exception {
        // This will fix https://github.com/playframework/playframework/issues/6152
        // And could be removed if https://github.com/netty/netty/issues/5428 is fixed
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest)msg;
            // This will check if there is a Content-Length header and if it is greater than zero
            contentLength = HttpHeaders.getContentLength(request, 0) != 0;
        }

        // delegate to the original decode method
        super.decode(ctx, msg, out);

        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest)msg;
            // if the request was gzipped and the Content-Length header was set, we check if it is still there
            // since netty-reactive-stream needs to know if this message has a body which is determined
            // by the Content-Length or Transfer-Encoding Header
            if (contentLength && !HttpHeaders.isContentLengthSet(request)) {
                HttpHeaders.setTransferEncodingChunked(request);
            }
        }
    }

}
