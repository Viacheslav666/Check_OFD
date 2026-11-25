package ru.ofd.agent.security;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/** Белый список адресов: отдельные IP и подсети CIDR (IPv4/IPv6). */
public final class IpAllowlist {

    private record Rule(byte[] network, int prefix) {}

    private final List<Rule> rules = new ArrayList<>();

    public IpAllowlist(List<String> entries) {
        if (entries != null) {
            for (String raw : entries) {
                if (raw == null) continue;
                for (String e : raw.split("[,;\\s]+")) {
                    if (!e.isBlank()) rules.add(parse(e.trim()));
                }
            }
        }
    }

    public boolean isEmpty() {
        return rules.isEmpty();
    }

    public boolean allows(String address) {
        if (address == null || address.isBlank()) return false;
        byte[] ip;
        try {
            ip = toBytes(address.trim());
        } catch (IllegalArgumentException e) {
            return false;
        }
        for (Rule r : rules) {
            if (matches(r, ip)) return true;
        }
        return false;
    }

    private static boolean matches(Rule r, byte[] ip) {
        byte[] net = r.network();
        if (net.length != ip.length) {
            // IPv4-mapped IPv6 (::ffff:a.b.c.d) сравниваем как IPv4
            if (ip.length == 16 && net.length == 4 && isV4Mapped(ip)) {
                byte[] v4 = new byte[4];
                System.arraycopy(ip, 12, v4, 0, 4);
                return matches(r, v4);
            }
            return false;
        }
        int bits = r.prefix();
        for (int i = 0; i < net.length && bits > 0; i++) {
            int mask = bits >= 8 ? 0xFF : (0xFF << (8 - bits)) & 0xFF;
            if ((net[i] & mask) != (ip[i] & mask)) return false;
            bits -= 8;
        }
        return true;
    }

    private static boolean isV4Mapped(byte[] ip) {
        for (int i = 0; i < 10; i++) if (ip[i] != 0) return false;
        return ip[10] == (byte) 0xFF && ip[11] == (byte) 0xFF;
    }

    private static Rule parse(String entry) {
        String addr = entry;
        Integer prefix = null;
        int slash = entry.indexOf('/');
        if (slash > 0) {
            addr = entry.substring(0, slash);
            prefix = Integer.parseInt(entry.substring(slash + 1));
        }
        byte[] b = toBytes(addr);
        int max = b.length * 8;
        int p = prefix == null ? max : prefix;
        if (p < 0 || p > max) throw new IllegalArgumentException("Неверная маска в белом списке: " + entry);
        return new Rule(b, p);
    }

    /** Только литералы IP, без DNS-запросов. */
    static byte[] toBytes(String addr) {
        String a = addr;
        if (a.startsWith("[") && a.endsWith("]")) a = a.substring(1, a.length() - 1);
        int pct = a.indexOf('%');
        if (pct > 0) a = a.substring(0, pct);
        if (a.matches("^[0-9.]+$")) {
            // Строгий разбор IPv4 (без DNS): ровно 4 октета 0..255
            String[] parts = a.split("\\.", -1);
            if (parts.length != 4) throw new IllegalArgumentException("Неверный IP-адрес: " + addr);
            byte[] b = new byte[4];
            for (int i = 0; i < 4; i++) {
                if (parts[i].isEmpty() || parts[i].length() > 3) throw new IllegalArgumentException("Неверный IP-адрес: " + addr);
                int v = Integer.parseInt(parts[i]);
                if (v > 255) throw new IllegalArgumentException("Неверный IP-адрес: " + addr);
                b[i] = (byte) v;
            }
            return b;
        }
        if (!a.contains(":") || !a.matches("^[0-9a-fA-F:.]+$")) {
            throw new IllegalArgumentException("Ожидался IP-адрес, а не имя хоста: " + addr);
        }
        try {
            return InetAddress.getByName(a).getAddress();
        } catch (Exception e) {
            throw new IllegalArgumentException("Неверный IP-адрес: " + addr, e);
        }
    }
}
