/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javadoc.websocketmigration;

import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import play.mvc.WebSocket;

public class WebSocketCloseMigration {
  public static class In {
    public int count;
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
