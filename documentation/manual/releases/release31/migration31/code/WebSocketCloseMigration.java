/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javadoc.websocketmigration;

import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import play.api.http.websocket.CloseCodes;
import play.http.websocket.Message;
import play.mvc.WebSocket;

public class WebSocketCloseMigration {
  public static class In {
    public int count;
  }

  private void recordAbnormalClosure() {}

  public WebSocket abnormalClosureWebSocket() {
    // #abnormal-closure
    // Before: an abrupt transport close could complete the stream without a close message.
    // After: raw Message handlers receive Message.Close with code 1006.
    return WebSocket.Message.accept(
        request ->
            Flow.fromSinkAndSource(
                Sink.foreach(
                    message -> {
                      if (message instanceof Message.Close close
                          && close.code().orElse(0) == CloseCodes.ConnectionAbort()) {
                        recordAbnormalClosure();
                      }
                    }),
                Source.maybe()));
    // #abnormal-closure
  }

  public WebSocket typedJsonWebSocket() {
    // #typed-json-decoding
    // Before: {"count":"not-a-number"} closed with status 1003, but used the
    // underlying Jackson exception message as the close reason.
    // After: Play still closes with status 1003, but uses
    // "Unable to parse JSON message" as a bounded generic reason.
    return WebSocket.json(In.class)
        .accept(request -> Flow.fromSinkAndSource(Sink.ignore(), Source.maybe()));
    // #typed-json-decoding
  }
}
