package foundations.e007_serialization;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Three serialization rules from entry 007: escape strings, keep integers exact, and bind only known fields. */
public final class Json {

    private Json() {}

    /** Largest integer every IEEE 754 binary64 parser holds exactly; RFC 8259 §6 calls ±(2^53 − 1) interoperable. */
    public static final long MAX_SAFE = (1L << 53) - 1;

    /** Encodes a Java string as a JSON string literal (RFC 8259 §7). */
    public static String quote(String s) {
        StringBuilder out = new StringBuilder(s.length() + 2).append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");  // an unescaped quote ends the string early: the injection vector
                case '\\' -> out.append("\\\\"); // an unescaped backslash starts an escape sequence
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    // U+0000–U+001F must be escaped; every other character may appear literally.
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.append('"').toString();
    }

    /** Writes an integer as a JSON number, refusing values a binary64 parser would silently round. */
    public static String integer(long n) {
        // Compare both bounds explicitly: Math.abs(Long.MIN_VALUE) is negative and would slip through.
        if (n < -MAX_SAFE || n > MAX_SAFE) {
            throw new IllegalArgumentException(n + " is outside ±(2^53 − 1); send it as a string");
        }
        return Long.toString(n);
    }

    /** Rejects fields the endpoint doesn't accept, so a client can't set what it was never meant to set. */
    public static void requireOnly(Map<String, ?> body, Set<String> allowed) {
        Set<String> unknown = new TreeSet<>(body.keySet()); // sorted, so the error message is deterministic
        unknown.removeAll(allowed);
        if (!unknown.isEmpty()) throw new IllegalArgumentException("unknown fields: " + unknown);
    }

    /** The public shape of a user: an explicit DTO, so a new entity field never leaks into responses by default. */
    public record UserView(long id, String name) {
        public String toJson() {
            // IDs are opaque identifiers, not quantities, so they travel as strings at any size.
            return "{\"id\":" + quote(Long.toString(id)) + ",\"name\":" + quote(name) + "}";
        }
    }
}
