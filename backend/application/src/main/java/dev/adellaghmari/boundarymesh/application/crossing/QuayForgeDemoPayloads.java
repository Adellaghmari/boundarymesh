package dev.adellaghmari.boundarymesh.application.crossing;

/**
 * Obviously fictional QuayForge payloads for local labs. Values are synthetic demo data, not
 * evidence. Runtime evidence still uses JSON Pointers, never these strings.
 */
public final class QuayForgeDemoPayloads {

    private QuayForgeDemoPayloads() {}

    public static String validV1(String eventId) {
        return "{\"event_id\":\"" + eventId
                + "\",\"event_type\":\"order.updated\",\"order_id\":\"ord-1001\",\"customer_id\":\"cust-9\"}";
    }

    public static String emailOnlyV1(String eventId) {
        return "{\"event_id\":\"" + eventId
                + "\",\"event_type\":\"order.updated\",\"order_id\":\"ord-1001\",\"customer_id\":\"cust-9\","
                + "\"email\":\"ada.lovelace@example.test\"}";
    }

    public static String transformV1(String eventId) {
        return "{\"event_id\":\"" + eventId
                + "\",\"event_type\":\"order.updated\",\"order_id\":\"ord-1001\",\"customer_id\":\"cust-9\","
                + "\"email\":\"ada.lovelace@example.test\",\"full_name\":\"Ada Lovelace\","
                + "\"shipping_address\":\"1 Synthetic Wharf\"}";
    }

    public static String denyV1(String eventId) {
        return "{\"event_id\":\"" + eventId
                + "\",\"event_type\":\"order.updated\",\"order_id\":\"ord-1001\",\"customer_id\":\"cust-9\","
                + "\"internal_risk_score\":99}";
    }

    public static String nestedV1(String eventId) {
        return "{\"event_id\":\"" + eventId
                + "\",\"event_type\":\"order.updated\",\"order_id\":\"ord-1001\",\"customer_id\":\"cust-9\","
                + "\"customer\":{\"email\":\"ada.lovelace@example.test\"}}";
    }

    public static String implicitV1(String eventId) {
        return "{\"event_id\":\"" + eventId
                + "\",\"event_type\":\"order.updated\",\"order_id\":\"ord-1001\",\"customer_id\":\"cust-9\","
                + "\"legacy_region\":\"EU-SYNTHETIC\"}";
    }

    public static String minimizationV1(String eventId) {
        return "{\"event_id\":\"" + eventId
                + "\",\"event_type\":\"order.updated\",\"order_id\":\"ord-1001\",\"customer_id\":\"cust-9\","
                + "\"email\":\"ada.lovelace@example.test\",\"full_name\":\"Ada Lovelace\","
                + "\"shipping_address\":\"1 Synthetic Wharf\",\"support_notes\":\"do-not-forward\","
                + "\"api_debug_metadata\":\"trace-synth\"}";
    }

    public static String missingRequired(String eventId) {
        return "{\"event_id\":\"" + eventId + "\",\"event_type\":\"order.updated\"}";
    }

    public static String wrongType(String eventId) {
        return "{\"event_id\":\"" + eventId
                + "\",\"event_type\":\"order.updated\",\"order_id\":\"ord-1001\",\"amount\":\"nineteen\"}";
    }

    public static String v2Shape(String eventId) {
        return "{\"event_id\":\"" + eventId
                + "\",\"event_type\":\"order.updated\",\"order_id\":\"ord-1001\",\"order_status\":\"created\","
                + "\"amount\":{\"cents\":1999,\"currency\":\"EUR\"}}";
    }

    public static String manyViolations(String eventId) {
        StringBuilder extra = new StringBuilder();
        extra.append("{\"event_id\":\"")
                .append(eventId)
                .append("\",\"event_type\":\"order.updated\",\"order_id\":\"ord-1001\"");
        for (int i = 1; i <= 25; i++) {
            extra.append(",\"extra_").append(i).append("\":").append(i);
        }
        extra.append('}');
        return extra.toString();
    }
}
