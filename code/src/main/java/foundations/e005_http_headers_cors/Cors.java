package foundations.e005_http_headers_cors;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The two halves of CORS: the browser's rule for when to preflight, and a server's answer.
 *
 * <p>Simplified from the WHATWG Fetch Standard: it ignores the length limits on safelisted header
 * values and the other request features that can also force a preflight.
 */
public final class Cors {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "POST");
    private static final Set<String> SAFE_HEADERS = Set.of("accept", "accept-language", "content-language");
    // An HTML form could already send these three, so a cross-origin request using them adds no new power.
    private static final Set<String> FORM_TYPES =
            Set.of("application/x-www-form-urlencoded", "multipart/form-data", "text/plain");

    private Cors() {}

    /** Browser side: must this cross-origin request be approved by an OPTIONS preflight first? */
    public static boolean needsPreflight(String method, Map<String, String> headers) {
        if (!SAFE_METHODS.contains(method)) return true; // method names are case-sensitive
        for (var h : headers.entrySet()) {
            String name = h.getKey().toLowerCase(Locale.ROOT); // field names are not
            if (SAFE_HEADERS.contains(name)) continue;
            if (name.equals("content-type")) {
                String essence = h.getValue().split(";")[0].trim().toLowerCase(Locale.ROOT);
                if (FORM_TYPES.contains(essence)) continue; // ";charset=utf-8" doesn't matter
            }
            return true; // Authorization, X-Request-Id, Content-Type: application/json, ...
        }
        return false;
    }

    /** Server side: a policy with an explicit origin allowlist (never a reflected Origin). */
    public record Policy(Set<String> origins, Set<String> methods, Set<String> headers,
                         boolean credentials, int maxAgeSeconds) {

        /** Headers for the preflight response. Empty means "not approved": the browser blocks the request. */
        public Map<String, String> preflight(String origin, String method, Set<String> requestHeaders) {
            if (!origins.contains(origin) || !methods.contains(method)
                    || !headers.containsAll(requestHeaders)) return Map.of();
            var out = new LinkedHashMap<>(actual(origin));
            out.put("Access-Control-Allow-Methods", String.join(", ", methods));
            if (!headers.isEmpty()) out.put("Access-Control-Allow-Headers", String.join(", ", headers));
            out.put("Access-Control-Max-Age", Integer.toString(maxAgeSeconds));
            return out;
        }

        /** Headers for the actual response. They let the page's script read it; they don't stop it running. */
        public Map<String, String> actual(String origin) {
            var out = new LinkedHashMap<String, String>();
            out.put("Vary", "Origin"); // the answer depends on Origin, so shared caches must key on it
            if (!origins.contains(origin)) return out;
            out.put("Access-Control-Allow-Origin", origin); // one origin, echoed only from the allowlist
            if (credentials) out.put("Access-Control-Allow-Credentials", "true");
            return out;
        }
    }
}
