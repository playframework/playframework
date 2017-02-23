package play.libs.ws;

import java.util.concurrent.CompletionStage;

public interface WSRequestExecutor {
    CompletionStage<WSResponse> apply(WSRequest request);
}
