/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamWriteConstraints;
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
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker.Std;

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

        // Stream constraints (read)
        StreamReadConstraints rc = mapper.getFactory().streamReadConstraints();
        ObjectNode readC = core.putObject("streamReadConstraints");
        readC.put("maxStringLength", rc.getMaxStringLength());
        readC.put("maxNumberLength", rc.getMaxNumberLength());
        readC.put("maxNestingDepth", rc.getMaxNestingDepth());
        readC.put("maxNameLength", rc.getMaxNameLength());
        readC.put("maxDocumentLength", rc.getMaxDocumentLength());
        readC.put("maxTokenCount", rc.getMaxTokenCount());

        // Stream constraints (write)
        StreamWriteConstraints wc = mapper.getFactory().streamWriteConstraints();
        ObjectNode writeC = core.putObject("streamWriteConstraints");
        writeC.put("maxNestingDepth", wc.getMaxNestingDepth());

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

        root.set("visibilityDefaults", buildVisibilityDefaults(mapper));

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

    private static ObjectNode buildVisibilityDefaults(ObjectMapper mapper) {
        final ObjectNode out = mapper.createObjectNode();
        out.set("serialization", visibilityToJson(mapper, mapper.getSerializationConfig().getDefaultVisibilityChecker()));
        out.set("deserialization", visibilityToJson(mapper, mapper.getDeserializationConfig().getDefaultVisibilityChecker()));
        return out;
    }

    private static ObjectNode visibilityToJson(ObjectMapper mapper, VisibilityChecker<?> vc) {
        ObjectNode node = mapper.createObjectNode();
        node.put("FIELD",       detectVisibilityLevel(vc, "FIELD"));
        node.put("GETTER",      detectVisibilityLevel(vc, "GETTER"));
        node.put("IS_GETTER",   detectVisibilityLevel(vc, "IS_GETTER"));
        node.put("SETTER",      detectVisibilityLevel(vc, "SETTER"));
        node.put("CREATOR",     detectVisibilityLevel(vc, "CREATOR"));
        node.put("_implClass",  vc.getClass().getName()); // helpful breadcrumb
        return node;
    }

    /** Read the enum for a given accessor from VisibilityChecker via reflection. */
    private static String detectVisibilityLevel(VisibilityChecker<?> vc, String kind) {
        if (vc instanceof Std) {
            String candidate = switch (kind) {
                case "FIELD"     -> "_fieldMinLevel";
                case "GETTER"    -> "_getterMinLevel";
                case "IS_GETTER" -> "_isGetterMinLevel";
                case "SETTER"    -> "_setterMinLevel";
                case "CREATOR"   -> "_creatorMinLevel";
                default -> null;
            };
            JsonAutoDetect.Visibility v = (JsonAutoDetect.Visibility) tryReadFieldEnum(vc, candidate, JsonAutoDetect.Visibility.class);
            if (v != null) return v.name();
        }
        return "UNKNOWN";
    }

    private static Object tryReadFieldEnum(Object target, String fieldName, Class<?> enumType) {
        try {
            var fld = target.getClass().getDeclaredField(fieldName);
            fld.setAccessible(true);
            Object val = fld.get(target);
            if (val != null && enumType.isInstance(val)) return val;
        } catch (NoSuchFieldException ignored) {
        } catch (Throwable t) {
            // Any other reflection issue: keep trying other names
        }
        return null;
    }
}
