package dev.adellaghmari.boundarymesh.gcp.firestore;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Firestore REST value codec for emulator HTTP. Used only inside backend/gcp. */
final class FirestoreRestValues {

    private FirestoreRestValues() {}

    static JsonObject fieldsOf(Map<String, Object> fields) {
        JsonObject encoded = new JsonObject();
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            if (entry.getValue() != null) {
                encoded.add(entry.getKey(), valueOf(entry.getValue()));
            }
        }
        return encoded;
    }

    static Map<String, Object> fromFields(JsonObject fields) {
        Map<String, Object> decoded = new LinkedHashMap<>();
        if (fields == null) {
            return decoded;
        }
        for (Map.Entry<String, JsonElement> entry : fields.entrySet()) {
            decoded.put(entry.getKey(), fromValue(entry.getValue().getAsJsonObject()));
        }
        return decoded;
    }

    static JsonObject valueOf(Object value) {
        JsonObject encoded = new JsonObject();
        switch (value) {
            case String text -> encoded.addProperty("stringValue", text);
            case Boolean flag -> encoded.addProperty("booleanValue", flag);
            case Integer number -> encoded.addProperty("integerValue", number.toString());
            case Long number -> encoded.addProperty("integerValue", number.toString());
            case Short number -> encoded.addProperty("integerValue", Integer.toString(number.intValue()));
            case Double number -> encoded.addProperty("doubleValue", number);
            case Float number -> encoded.addProperty("doubleValue", number.doubleValue());
            case Map<?, ?> map -> {
                JsonObject nested = new JsonObject();
                nested.add("fields", fieldsOf(castMap(map)));
                encoded.add("mapValue", nested);
            }
            case List<?> list -> {
                JsonObject array = new JsonObject();
                JsonArray values = new JsonArray();
                for (Object item : list) {
                    values.add(valueOf(item));
                }
                array.add("values", values);
                encoded.add("arrayValue", array);
            }
            default -> encoded.addProperty("stringValue", String.valueOf(value));
        }
        return encoded;
    }

    private static Object fromValue(JsonObject value) {
        if (value.has("stringValue")) {
            return value.get("stringValue").getAsString();
        }
        if (value.has("booleanValue")) {
            return value.get("booleanValue").getAsBoolean();
        }
        if (value.has("integerValue")) {
            return Long.parseLong(value.get("integerValue").getAsString());
        }
        if (value.has("doubleValue")) {
            return value.get("doubleValue").getAsDouble();
        }
        if (value.has("nullValue")) {
            return null;
        }
        if (value.has("mapValue")) {
            JsonObject map = value.getAsJsonObject("mapValue");
            return fromFields(map.has("fields") ? map.getAsJsonObject("fields") : new JsonObject());
        }
        if (value.has("arrayValue")) {
            List<Object> items = new ArrayList<>();
            JsonObject array = value.getAsJsonObject("arrayValue");
            if (array.has("values")) {
                for (JsonElement item : array.getAsJsonArray("values")) {
                    items.add(fromValue(item.getAsJsonObject()));
                }
            }
            return items;
        }
        throw new IllegalArgumentException("unsupported Firestore REST value " + value);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }
}
