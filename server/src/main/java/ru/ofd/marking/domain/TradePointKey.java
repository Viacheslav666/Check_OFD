package ru.ofd.marking.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

public final class TradePointKey {

    private TradePointKey() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public static String displayName(String address, String place) {
        String a = address == null || address.isBlank() ? "Без адреса" : address.trim();
        String p = place == null || place.isBlank() ? "" : place.trim();
        return p.isEmpty() ? a : a + " — " + p;
    }

    public static String hash(String address, String place) {
        String raw = normalize(address) + "|" + normalize(place);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
