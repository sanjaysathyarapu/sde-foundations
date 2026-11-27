package foundations.e008_validation;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Locale;

/** Entry 008: parse raw input into types that can only hold valid values. */
public final class Inputs {

    private Inputs() {}

    /** A checked email address. The compact constructor is the only way in, so holding an Email is proof it passed. */
    public record Email(String value) {
        public Email {
            // 254 = RFC 5321's 256-octet path limit minus the two angle brackets.
            if (value == null || value.isEmpty() || value.length() > 254) {
                throw new IllegalArgumentException("email: required, at most 254 characters");
            }
            int at = value.lastIndexOf('@');
            boolean shapeOk = at > 0 && at <= 64 // the local part is at most 64 octets
                    && value.indexOf('.', at) > at + 1 && !value.endsWith(".")
                    && value.chars().noneMatch(Character::isWhitespace);
            if (!shapeOk) throw new IllegalArgumentException("email: expected local@domain.tld");
        }

        /** Canonicalize, then validate. Checking first and stripping later would validate a different string. */
        public static Email parse(String raw) {
            return new Email(raw == null ? null : raw.strip());
        }

        /** The comparison key for uniqueness. Locale.ROOT, because in a Turkish locale "I" lowercases to dotless "ı". */
        public String key() {
            return value.toLowerCase(Locale.ROOT);
        }
    }

    /** A cross-field rule belongs to the type that owns both fields; neither field can check it alone. */
    public record DateRange(LocalDate start, LocalDate end) {
        public DateRange {
            if (start == null || end == null) throw new IllegalArgumentException("start and end are required");
            if (end.isBefore(start)) throw new IllegalArgumentException("end must not be before start");
        }
    }

    /** Resolves a client-supplied relative path inside base, or refuses. */
    public static Path resolveInside(Path base, String userPath) {
        Path root = base.toAbsolutePath().normalize();
        // Normalize before checking: "docs/../../etc/passwd" has no leading "..", but it normalizes to one.
        Path target = root.resolve(userPath).normalize();
        // Path.startsWith compares whole name elements, so /srv/data2 does not start with /srv/data.
        if (!target.startsWith(root)) throw new IllegalArgumentException("path escapes the base directory");
        return target;
    }
}
