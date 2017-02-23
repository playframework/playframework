package play.libs.ws;

public interface WSRequestFilter {
    WSRequestExecutor apply(WSRequestExecutor executor);
}
