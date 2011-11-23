package play.core.server.websocket;

public enum DecodingState {
        FRAME_START,
        PARSING_LENGTH,
        MASKING_KEY,
        PARSING_LENGTH_2,
        PARSING_LENGTH_3,
        PAYLOAD
}