/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.lang.reflect.*;
import java.util.*;

public final class ObjectMapperConfigUtil {

    private ObjectMapperConfigUtil() {}

    public static ObjectNode toConfigJson(ObjectMapper mapper) {
        ObjectNode root = mapper.createObjectNode();

        // Basic
        root.put("class", mapper.getClass().getName());
        root.put("jsonFactory", mapper.getFactory().getClass().getName());
        root.put("typeFactory", mapper.getTypeFactory().getClass().getName());

        // Modules
        ArrayNode modules = root.putArray("registeredModules");
        for (Object id : mapper.getRegisteredModuleIds()) {
            modules.add(String.valueOf(id));
        }

        // Serialization config
        ObjectNode ser = root.putObject("serializationConfig");
        SerializationConfig serCfg = mapper.getSerializationConfig();
        ser.put("serializationInclusion", String.valueOf(serCfg.getSerializationInclusion()));
        ser.put("defaultPrettyPrinter", classOrNull(serCfg.getDefaultPrettyPrinter()));
        ser.put("timeZone", serCfg.getTimeZone() == null ? null : serCfg.getTimeZone().getID());
        ser.put("dateFormat", classOrNull(serCfg.getDateFormat()));
        ser.put("propertyNamingStrategy", classOrNull(mapper.getPropertyNamingStrategy()));
        ser.set("features", enumFlagsToObject(mapper, SerializationFeature.values(), serCfg::isEnabled));

        // Deserialization config
        ObjectNode deser = root.putObject("deserializationConfig");
        DeserializationConfig deserCfg = mapper.getDeserializationConfig();
        deser.put("timeZone", deserCfg.getTimeZone() == null ? null : deserCfg.getTimeZone().getID());
        deser.put("dateFormat", classOrNull(deserCfg.getDateFormat()));
        deser.set("features", enumFlagsToObject(mapper, DeserializationFeature.values(), deserCfg::isEnabled));

        // Mapper features (apply to both ser+deser)
        root.set("mapperFeatures", enumFlagsToObject(mapper, MapperFeature.values(), mapper::isEnabled));

        // Core factory + features
        ObjectNode core = root.putObject("coreFactory");
        core.set("jsonParserFeatures", enumFlagsToObject(mapper, JsonParser.Feature.values(),
                f -> mapper.getFactory().isEnabled(f)));
        core.set("jsonGeneratorFeatures", enumFlagsToObject(mapper, JsonGenerator.Feature.values(),
                f -> mapper.getFactory().isEnabled(f)));
        core.set("jsonReadFeatures", enumFlagsToObject(mapper, JsonReadFeature.values(),
                f -> mapper.getFactory().isEnabled(f.mappedFeature())));
        core.set("jsonWriteFeatures", enumFlagsToObject(mapper, JsonWriteFeature.values(),
                f -> mapper.getFactory().isEnabled(f.mappedFeature())));

        // Annotation introspectors (names only)
        ObjectNode ai = root.putObject("annotationIntrospectors");
        ai.put("serialization", classOrNull(serCfg.getAnnotationIntrospector()));
        ai.put("deserialization", classOrNull(deserCfg.getAnnotationIntrospector()));

        // Mix-in counts
        root.put("mixInCountSerialization", serCfg.mixInCount());
        root.put("mixInCountDeserialization", deserCfg.mixInCount());

        // Subtype / polymorphicTypeValidator
        root.put("subtypeResolver", classOrNull(mapper.getSubtypeResolver()));
        root.put("polymorphicTypeValidator", classOrNull(mapper.getPolymorphicTypeValidator()));

        return root;
    }

    private static <E extends Enum<E>> ObjectNode enumFlagsToObject(
            ObjectMapper mapper, E[] values, java.util.function.Function<E, Boolean> enabledFn
    ) {
        ObjectNode o = mapper.createObjectNode();
        for (E f : values) {
            o.put(f.name(), Boolean.TRUE.equals(enabledFn.apply(f)));
        }
        return o;
    }

    private static String classOrNull(Object o) {
        return o == null ? null : o.getClass().getName();
    }
}
