package foundations.e008_validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import foundations.e008_validation.Inputs.DateRange;
import foundations.e008_validation.Inputs.Email;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class InputsTest {

    private static final Path BASE = Path.of("/srv/uploads");

    @Test
    void parseStripsBeforeValidating() {
        assertEquals("ada@example.com", Email.parse("  ada@example.com\n").value());
        // The constructor itself refuses untrimmed input, so no unstripped Email can exist.
        assertThrows(IllegalArgumentException.class, () -> new Email(" ada@example.com"));
    }

    @Test
    void emailRejectsMalformedAndOverlongInput() {
        for (String bad : new String[] {"", "ada", "@example.com", "ada@", "ada@example", "ada@.com", "ada@example.", "a da@example.com"}) {
            assertThrows(IllegalArgumentException.class, () -> Email.parse(bad), bad);
        }
        assertThrows(IllegalArgumentException.class, () -> Email.parse(null));
        assertThrows(IllegalArgumentException.class, () -> Email.parse("a".repeat(65) + "@example.com"));
        assertThrows(IllegalArgumentException.class, () -> Email.parse("a@" + "b".repeat(250) + ".com"));
    }

    @Test
    void keyIsCaseInsensitiveInEveryLocale() {
        Locale saved = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            // A default-locale toLowerCase would turn "I" into dotless "ı" here, and the keys would differ.
            assertFalse("I".toLowerCase().equals("i"));
            assertEquals(Email.parse("ivy@example.com").key(), Email.parse("IVY@Example.COM").key());
        } finally {
            Locale.setDefault(saved);
        }
    }

    @Test
    void dateRangeChecksTheRelationBetweenFields() {
        LocalDate d = LocalDate.of(2026, 11, 27);
        assertEquals(d, new DateRange(d, d).end()); // a one-day range is valid
        assertThrows(IllegalArgumentException.class, () -> new DateRange(d, d.minusDays(1)));
        assertThrows(IllegalArgumentException.class, () -> new DateRange(null, d));
    }

    @Test
    void resolveInsideAcceptsPathsThatStayInside() {
        assertEquals(Path.of("/srv/uploads/docs/a.txt"), Inputs.resolveInside(BASE, "docs/a.txt"));
        assertEquals(Path.of("/srv/uploads/a.txt"), Inputs.resolveInside(BASE, "docs/../a.txt"));
    }

    @Test
    void resolveInsideRejectsEveryEscape() {
        for (String bad : new String[] {"../etc/passwd", "docs/../../etc/passwd", "/etc/passwd", "../uploads2/x"}) {
            assertThrows(IllegalArgumentException.class, () -> Inputs.resolveInside(BASE, bad), bad);
        }
    }

    @Test
    void validatingBeforeDecodingLetsTraversalThrough() {
        String raw = "%2e%2e/etc/passwd";
        // A naive check on the encoded form sees no "..", so it passes.
        assertFalse(raw.contains(".."));
        // Decoding afterwards produces exactly what the check was meant to stop.
        String decoded = URLDecoder.decode(raw, StandardCharsets.UTF_8);
        assertTrue(decoded.startsWith("../"));
        // Checking the canonical form catches it.
        assertThrows(IllegalArgumentException.class, () -> Inputs.resolveInside(BASE, decoded));
    }
}
