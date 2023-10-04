/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package ws;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import jakarta.websocket.*;

// Stupid WebSocket client, but good enough for tests
@ClientEndpoint
public class WebSocketClient {

    private Session session;
    private WsHandler wsHandler;
    private final URI endpointURI;

    public WebSocketClient(String endpoint) {
        try {
            this.endpointURI = new URI(endpoint);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public WebSocketClient(URI endpointURI) {
        this.endpointURI = endpointURI;
    }

    public WebSocketClient addHandler(WsHandler wsHandler) {
        this.wsHandler = wsHandler;
        return this;
    }

    public void connect() {
        try {
            ContainerProvider.getWebSocketContainer().connectToServer(this, endpointURI);
        } catch (DeploymentException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        this.session = session;
        this.wsHandler.onOpen();
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        this.wsHandler.onClose(reason);
        this.session = null;
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        if (this.wsHandler != null) {
            this.wsHandler.handleStringMessage(message);
        }
    }

   @OnMessage
   public void onMessage(Session session, ByteBuffer bytes) {
       if (this.wsHandler != null) {
           this.wsHandler.handleBinaryMessage(bytes);
       }
    }

    @OnMessage
    public void onMessage(Session session, PongMessage pong) {
        if (this.wsHandler != null) {
            this.wsHandler.handlePongMessage(pong);
        }
    }

    @OnError
    public void onError(Session session, Throwable t) {
        if (this.wsHandler != null) {
            this.wsHandler.onError(t);
        } else {
            t.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        try {
            this.session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean close(int closeCode) {
        return this.close(closeCode, null);
    }

    public boolean close(int closeCode, String reason) {
        try {
            this.session.close(new CloseReason(CloseReason.CloseCodes.getCloseCode(closeCode), reason));
            return true;
        } catch (final IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static interface WsHandler {
        default void onOpen() {}
        default void onClose(CloseReason reason) {}
        default void onError(Throwable t) {}
        default void handleStringMessage(String message) {}
        default void handleBinaryMessage(ByteBuffer bytes) {}
        default void handlePongMessage(PongMessage pong) {}
    }
}