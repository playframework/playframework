package play.libs;

import javax.inject.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Creates the Jackson ObjectMapper. Can be used with play.objectMapper.provider to customize ObjectMapper creation.
 */
public interface ObjectMapperProvider extends Provider<ObjectMapper> {
    @Override
    default ObjectMapper get() {
        return Json.newDefaultMapper();
    }
}
