package play.core.server.netty;

import org.jboss.netty.channel.*;

import java.net.SocketAddress;
import java.util.concurrent.Callable;

/**
 * Delegates operations to the underlying channel, but only when the channel is allowed to do those operations.
 *
 * Some operations are not supported.
 */
class HttpPipeliningChannel implements Channel {

    private final Channel delegate;
    private final ChannelFuture future;
    private Object attachment;
    private boolean allowReading = true;
    private boolean allowWriting = true;

    HttpPipeliningChannel(Channel delegate) {
        this(delegate, false);
    }

    HttpPipeliningChannel(Channel delegate, boolean proceed) {
        this.delegate = delegate;
        if (proceed) {
            this.future = new SucceededChannelFuture(this);
        } else {
            this.future = new DefaultChannelFuture(this, false);
        }
    }

    /**
     * The channel must stop reading, since there is another request in the pipeline.
     *
     * After this is called, any call to register interest or not in reading will fail.
     */
    public void stopReading() {
        allowReading = false;
    }

    /**
     * The channel must stop writing, since it's the next request in the pipelines turn to write.
     *
     * After this is called, all write operations will fail.
     */
    public void stopWriting() {
        allowWriting = false;
    }

    /**
     * The chanel is allowed to start writing.  Any queued up futures will be made successful.
     */
    public void startWriting() {
        future.setSuccess();
    }

    /**
     * The channel has been cancelled.
     */
    public void cancel() {
        future.cancel();
    }

    /**
     * The channel has failed.  This will be called if the previous pipelined response has failed.
     */
    public void fail(Throwable cause) {
        future.setFailure(cause);
    }

    @Override
    public Integer getId() {
        return delegate.getId();
    }

    @Override
    public ChannelFactory getFactory() {
        return delegate.getFactory();
    }

    @Override
    public Channel getParent() {
        return delegate.getParent();
    }

    @Override
    public ChannelConfig getConfig() {
        return delegate.getConfig();
    }

    @Override
    public ChannelPipeline getPipeline() {
        return delegate.getPipeline();
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }

    @Override
    public boolean isBound() {
        return delegate.isBound();
    }

    @Override
    public boolean isConnected() {
        return delegate.isConnected();
    }

    @Override
    public SocketAddress getLocalAddress() {
        return delegate.getLocalAddress();
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return delegate.getRemoteAddress();
    }

    @Override
    public ChannelFuture write(final Object message) {
        if (allowWriting) {
            // Small optimisation
            if (future.isSuccess()) {
                return delegate.write(message);
            } else {
                return chainFutureTo(new Callable<ChannelFuture>() {
                    @Override
                    public ChannelFuture call() throws Exception {
                        return delegate.write(message);
                    }
                });
            }
        } else {
            return new FailedChannelFuture(this, new IllegalStateException("Not allowed to write to pipelined channel after response is finished."));
        }
    }

    @Override
    public ChannelFuture write(final Object message, final SocketAddress remoteAddress) {
        return new FailedChannelFuture(this, new UnsupportedOperationException("Cannot write to an address on a pipelined channel."));
    }

    @Override
    public ChannelFuture bind(final SocketAddress localAddress) {
        return new FailedChannelFuture(this, new UnsupportedOperationException("Cannot bind a pipelined channel."));
    }

    @Override
    public ChannelFuture connect(final SocketAddress remoteAddress) {
        return new FailedChannelFuture(this, new UnsupportedOperationException("Cannot connect a pipelined channel."));
    }

    @Override
    public ChannelFuture disconnect() {
        return chainFutureTo(new Callable<ChannelFuture>() {
            @Override
            public ChannelFuture call() throws Exception {
                return delegate.disconnect();
            }
        });
    }

    @Override
    public ChannelFuture unbind() {
        return new FailedChannelFuture(this, new UnsupportedOperationException("Cannot unbind a pipelined channel"));
    }

    @Override
    public ChannelFuture close() {
        if (future.isSuccess()) {
            return delegate.close();
        } else {
            return chainFutureTo(new Callable<ChannelFuture>() {
                @Override
                public ChannelFuture call() throws Exception {
                    return delegate.close();
                }
            });
        }
    }

    @Override
    public ChannelFuture getCloseFuture() {
        if (future.isSuccess()) {
            return delegate.getCloseFuture();
        } else {
            return chainFutureTo(new Callable<ChannelFuture>() {
                @Override
                public ChannelFuture call() throws Exception {
                    return delegate.getCloseFuture();
                }
            });
        }
    }

    @Override
    public int getInterestOps() {
        return delegate.getInterestOps();
    }

    @Override
    public boolean isReadable() {
        return allowReading && delegate.isReadable();
    }

    @Override
    public boolean isWritable() {
        return allowWriting && future.isDone() && delegate.isWritable();
    }

    @Override
    public ChannelFuture setInterestOps(final int interestOps) {
        if (allowReading) {
            return delegate.setInterestOps(interestOps);
        } else {
            return new FailedChannelFuture(this, new IllegalStateException("Cannot set interested ops on a pipelined channel once the request is finished"));
        }
    }

    @Override
    public ChannelFuture setReadable(final boolean readable) {
        if (allowReading) {
            return delegate.setReadable(readable);
        } else {
            return new FailedChannelFuture(this, new IllegalStateException("Cannot read from a pipelined channel once the current request has finished"));
        }
    }

    @Override
    public Object getAttachment() {
        return attachment;
    }

    @Override
    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }

    @Override
    public int compareTo(Channel channel) {
        return delegate.compareTo(channel);
    }

    /**
     * Basically makes the ChannelFuture a monad of sorts.
     */
    private ChannelFuture chainFutureTo(final Callable<ChannelFuture> op) {
        final ChannelFuture chained = new DefaultChannelFuture(this, true);
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isCancelled()) {
                    chained.cancel();
                } else if (future.isSuccess()) {
                    op.call().addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (future.isSuccess()) {
                                chained.setSuccess();
                            } else if (future.isCancelled()) {
                                chained.cancel();
                            } else {
                                chained.setFailure(future.getCause());
                            }
                        }
                    });
                } else {
                    chained.setFailure(future.getCause());
                }
            }
        });
        return chained;
    }
}
