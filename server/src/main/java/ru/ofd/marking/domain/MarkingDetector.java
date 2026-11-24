package ru.ofd.marking.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * ФФД 1.2: чек/позиция маркированы, если есть код в теге 1163 (ProductCode)
 * и/или SubjectType (тег 1212) равен 31 или 33.
 */
public final class MarkingDetector {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MarkingDetector() {
    }

    public static boolean isMarked(Integer subjectType, String productCodeJson) {
        if (subjectType != null && (subjectType == 31 || subjectType == 33)) {
            return true;
        }
        return hasMarkingCode(productCodeJson);
    }

    public static boolean hasMarkingCode(String productCodeJson) {
        if (productCodeJson == null || productCodeJson.isBlank()) {
            return false;
        }
        try {
            JsonNode node = MAPPER.readTree(productCodeJson);
            if (node == null || !node.isObject() || node.isEmpty()) {
                return false;
            }
            var fields = node.fields();
            while (fields.hasNext()) {
                var e = fields.next();
                JsonNode v = e.getValue();
                if (v != null && v.isTextual() && !v.asText().isBlank()) {
                    return true;
                }
                if (v != null && !v.isNull() && !v.isTextual() && !v.isEmpty()) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
