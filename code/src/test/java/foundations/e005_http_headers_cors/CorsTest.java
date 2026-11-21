package foundations.e005_http_headers_cors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CorsTest {

    private static final String APP = "https://app.example.com";

    // Header names in the policy are lowercase, as browsers send them in Access-Control-Request-Headers.
    private static final Cors.Policy POLICY = new Cors.Policy(
            Set.of(APP), Set.of("GET", "POST", "PUT"), Set.of("authorization", "content-type"), true, 600);

    @Test
    void requestsAFormCouldSendAreNotPreflighted() {
        assertFalse(Cors.needsPreflight("GET", Map.of()));
        assertFalse(Cors.needsPreflight("POST", Map.of("Content-Type", "text/plain;charset=UTF-8")));
        assertFalse(Cors.needsPreflight("POST", Map.of("content-type", "application/x-www-form-urlencoded")));
        assertFalse(Cors.needsPreflight("GET", Map.of("Accept", "application/json")));
    }

    @Test
    void newMethodsAndNewHeadersArePreflighted() {
        assertTrue(Cors.needsPreflight("PUT", Map.of()));
        assertTrue(Cors.needsPreflight("DELETE", Map.of()));
        assertTrue(Cors.needsPreflight("POST", Map.of("Content-Type", "application/json")));
        assertTrue(Cors.needsPreflight("GET", Map.of("Authorization", "Bearer abc")));
        assertTrue(Cors.needsPreflight("get", Map.of()), "method names are case-sensitive");
    }

    @Test
    void anAllowedPreflightIsApproved() {
        var headers = POLICY.preflight(APP, "PUT", Set.of("content-type", "authorization"));
        assertEquals(APP, headers.get("Access-Control-Allow-Origin"));
        assertEquals("true", headers.get("Access-Control-Allow-Credentials"));
        assertEquals("600", headers.get("Access-Control-Max-Age"));
        assertEquals(Set.of("GET", "POST", "PUT"), Set.of(headers.get("Access-Control-Allow-Methods").split(", ")));
    }

    @Test
    void preflightsOutsideThePolicyGetNoCorsHeaders() {
        assertTrue(POLICY.preflight("https://evil.example", "PUT", Set.of()).isEmpty());
        assertTrue(POLICY.preflight(APP, "DELETE", Set.of()).isEmpty());
        assertTrue(POLICY.preflight(APP, "PUT", Set.of("x-debug")).isEmpty());
    }

    @Test
    void theActualResponseEchoesOnlyAllowlistedOriginsAndAlwaysVaries() {
        assertEquals(APP, POLICY.actual(APP).get("Access-Control-Allow-Origin"));
        var other = POLICY.actual("https://evil.example");
        assertFalse(other.containsKey("Access-Control-Allow-Origin"));
        // Without Vary, a cache could serve the evil-origin answer (no ACAO) to the real app, or the reverse.
        assertEquals("Origin", other.get("Vary"));
    }

    @Test
    void anOriginIncludesSchemeAndPort() {
        assertNull(POLICY.actual("http://app.example.com").get("Access-Control-Allow-Origin"));
        assertNull(POLICY.actual("https://app.example.com:8443").get("Access-Control-Allow-Origin"));
    }
}
