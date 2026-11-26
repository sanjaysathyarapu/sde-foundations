package foundations.e007_serialization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import foundations.e007_serialization.Json.UserView;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JsonTest {

    @Test
    void quoteEscapesQuotesBackslashesAndControlCharacters() {
        assertEquals("\"a\\\"b\\\\c\\nd\\u0001\"", Json.quote("a\"b\\c\nd\u0001"));
        // Non-ASCII text needs no escaping: the whole document is UTF-8.
        assertEquals("\"café ✓\"", Json.quote("café ✓"));
    }

    @Test
    void aHostileNameStaysOneStringValue() {
        String name = "x\",\"role\":\"admin";
        String naive = "{\"name\":\"" + name + "\"}";
        // String concatenation lets the input close the string and add a member of its own.
        assertTrue(naive.contains("\"role\":\"admin\""));
        String safe = new UserView(1, name).toJson();
        assertFalse(safe.contains("\"role\":\"admin\""));
        assertEquals("{\"id\":\"1\",\"name\":\"x\\\",\\\"role\\\":\\\"admin\"}", safe);
    }

    @Test
    void aDoubleCannotHoldEveryIntegerAboveTwoToThe53() {
        long a = Json.MAX_SAFE + 1; // 2^53
        long b = Json.MAX_SAFE + 2; // 2^53 + 1
        assertNotEquals(a, b);
        // This is what a binary64 parser (JavaScript's JSON.parse, for one) does to the number b.
        assertEquals((double) a, (double) b);
        assertEquals(a, (long) (double) b);
    }

    @Test
    void integerRefusesValuesOutsideTheSafeRange() {
        assertEquals("9007199254740991", Json.integer(Json.MAX_SAFE));
        assertEquals("-9007199254740991", Json.integer(-Json.MAX_SAFE));
        assertThrows(IllegalArgumentException.class, () -> Json.integer(Json.MAX_SAFE + 1));
        assertThrows(IllegalArgumentException.class, () -> Json.integer(Long.MIN_VALUE));
    }

    @Test
    void largeIdsTravelAsStringsWithoutLosingDigits() {
        assertEquals("{\"id\":\"9007199254740993\",\"name\":\"n\"}", new UserView(Json.MAX_SAFE + 2, "n").toJson());
    }

    @Test
    void requireOnlyRejectsFieldsTheEndpointDoesNotAccept() {
        Set<String> allowed = Set.of("name", "email");
        assertDoesNotThrow(() -> Json.requireOnly(Map.of("name", "Ada"), allowed));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Json.requireOnly(Map.of("name", "Ada", "role", "admin", "id", 7), allowed));
        assertEquals("unknown fields: [id, role]", e.getMessage());
    }
}
