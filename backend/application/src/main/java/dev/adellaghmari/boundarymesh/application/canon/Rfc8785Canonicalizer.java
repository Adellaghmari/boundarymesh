package dev.adellaghmari.boundarymesh.application.canon;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * RFC 8785 JSON Canonicalization Scheme for a constrained JSON model: objects, arrays, strings,
 * integers, booleans, and null. Floating-point numbers are rejected so integer formatting stays
 * unambiguous. Property order and insignificant whitespace do not affect the canonical bytes.
 */
public final class Rfc8785Canonicalizer {

    private Rfc8785Canonicalizer() {}

    public static byte[] canonicalize(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out);
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static String canonicalizeToString(Object value) {
        return new String(canonicalize(value), StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private static void write(Object value, StringBuilder out) {
        if (value == null) {
            out.append("null");
            return;
        }
        if (value instanceof String text) {
            writeString(text, out);
            return;
        }
        if (value instanceof Boolean flag) {
            out.append(flag ? "true" : "false");
            return;
        }
        if (value instanceof Integer number) {
            out.append(number.longValue());
            return;
        }
        if (value instanceof Long number) {
            out.append(number);
            return;
        }
        if (value instanceof Map<?, ?> map) {
            writeObject((Map<String, Object>) map, out);
            return;
        }
        if (value instanceof List<?> list) {
            writeArray(list, out);
            return;
        }
        throw new IllegalArgumentException(
                "unsupported canonical JSON type: " + value.getClass().getName());
    }

    private static void writeObject(Map<String, Object> map, StringBuilder out) {
        TreeMap<String, Object> sorted = new TreeMap<>(Comparator.naturalOrder());
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getKey() == null) {
                throw new IllegalArgumentException("object member name must not be null");
            }
            sorted.put(entry.getKey(), entry.getValue());
        }
        out.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> entry : sorted.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            writeString(entry.getKey(), out);
            out.append(':');
            write(entry.getValue(), out);
        }
        out.append('}');
    }

    private static void writeArray(List<?> list, StringBuilder out) {
        out.append('[');
        boolean first = true;
        for (Object item : list) {
            if (!first) {
                out.append(',');
            }
            first = false;
            write(item, out);
        }
        out.append(']');
    }

    private static void writeString(String text, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        out.append(String.format("\\u%04x", (int) ch));
                    } else {
                        out.append(ch);
                    }
                }
            }
        }
        out.append('"');
    }

    public static Map<String, Object> object() {
        return new TreeMap<>();
    }

    public static List<Object> array(Object... values) {
        List<Object> list = new ArrayList<>();
        for (Object value : values) {
            list.add(value);
        }
        return list;
    }
}
