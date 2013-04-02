package play.core.server.netty;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.*;

import java.util.LinkedList;

/**
 * Implements HTTP pipelining ordering, ensuring that responses are completely served in the same order as their
 * corresponding requests.
 *
 * @author James Roper
 */
public class HttpPipeliningHandler implements ChannelUpstreamHandler, ChannelDownstreamHandler {

    // The queue of pipelined channels
    private final LinkedList<HttpPipeliningChannel> channelQueue = new LinkedList<HttpPipeliningChannel>();

    // These two variables are used to store data about the response currently being sent.
    // Remaining content is -1 if there was no Content-Length header
    private long remainingContentLength = -1;
    private boolean isChunked;

    @Override
    public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        if (e instanceof MessageEvent) {
            MessageEvent msgEvent = (MessageEvent) e;
            Object message = msgEvent.getMessage();
            if (message instanceof HttpResponse) {
                // It's an http response, setup the
                HttpResponse response = (HttpResponse) message;
                if (response.getStatus() == HttpResponseStatus.NO_CONTENT || response.getStatus() == HttpResponseStatus.NOT_MODIFIED) {
                    remainingContentLength = 0;
                } else {
                    remainingContentLength = HttpHeaders.getContentLength(response, -1);
                }
                // If a content length was specified, the Netty HTTP encoder ignores is chunked
                isChunked = remainingContentLength == -1 && response.isChunked();
                if (!isChunked) {
                    observeContent(response.getContent(), e.getFuture());
                }
            } else if (message instanceof HttpChunk) {
                HttpChunk chunk = (HttpChunk) message;
                if (isChunked) {
                    // If the response is actually chunked, then we just need to pop the channel queue on the last chunk
                    if (chunk.isLast()) {
                        popChannelQueue(e.getFuture());
                    }
                } else {
                    // Otherwise just look at the content
                    observeContent(chunk.getContent(), e.getFuture());
                }
            } else if (message instanceof ChannelBuffer) {
                observeContent((ChannelBuffer) message, e.getFuture());
            }
        }
        ctx.sendDownstream(e);
    }

    /**
     * Decrements remainingContentLength by the content length in the buffer, and when 0 is reached, pops the channel
     * off the queue
     */
    private void observeContent(ChannelBuffer buffer, ChannelFuture future) {
        if (remainingContentLength >= 0) {
            remainingContentLength -= buffer.readableBytes();
            if (remainingContentLength <= 0) {
                popChannelQueue(future);
                remainingContentLength = -1;
            }
        }
    }

    /**
     * Pop the channel off the queue, once the current operation is complete.  If there is another channel on the queue,
     * tell it to proceed.
     */
    private void popChannelQueue(ChannelFuture future) {
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                HttpPipeliningChannel next;
                synchronized (channelQueue) {
                    channelQueue.pop().stopWriting();
                    next = channelQueue.peek();
                }
                if (future.isCancelled()) {
                    next.cancel();
                } else if (future.isSuccess()) {
                    next.startWriting();
                } else {
                    next.fail(new RuntimeException("Previous request in pipeline failed", future.getCause()));
                }
            }
        });
    }

    @Override
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        // A message has come from upstream (ie, part of a request)
        if (e instanceof MessageEvent) {
            MessageEvent msgEvent = (MessageEvent) e;
            Object msg = msgEvent.getMessage();
            if (msg instanceof HttpRequest) {
                synchronized (channelQueue) {
                    if (channelQueue.size() > 0) {
                        // This request is pipelined behind another.
                        channelQueue.getLast().stopReading();
                        channelQueue.add(new HttpPipeliningChannel(e.getChannel()));
                    } else {
                        // No pipelining, so tell the channel to proceed immediately
                        channelQueue.add(new HttpPipeliningChannel(e.getChannel(), true));
                    }
                }
            }
            ctx.sendUpstream(new UpstreamMessageEvent(channelQueue.getLast(), msg, msgEvent.getRemoteAddress()));
        } else if (e instanceof ChannelStateEvent) {
            ChannelStateEvent event = (ChannelStateEvent) e;
            if (event.getState() == ChannelState.OPEN && event.getValue() == Boolean.FALSE) {
                // The channel is closed.  Send upstream, and for each pipelining channel in the queue we need to tell
                // them to proceed so they can fail if necessary.
                // But pop the first one cos it should already have proceeded.
                if (!channelQueue.isEmpty()) {
                    channelQueue.pop();
                }
                for (HttpPipeliningChannel channel : channelQueue) {
                    channel.startWriting();
                }
            }
            ctx.sendUpstream(e);
        } else {
            ctx.sendUpstream(e);
        }
    }
}
