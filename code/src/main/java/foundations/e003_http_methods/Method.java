package foundations.e003_http_methods;

import java.util.Optional;

/**
 * HTTP methods with the two properties RFC 9110 §9.2 defines for them, as data.
 *
 * <p>Clients, proxies and crawlers act on these flags without seeing the server's code. That only
 * works if every server implements each method with the meaning the flags promise.
 */
public enum Method {
    // safe, idempotent: RFC 9110 §9.1 and the method registry (§18.2), PATCH from RFC 5789,
    // QUERY from RFC 10008.
    GET(true, true),
    HEAD(true, true),
    OPTIONS(true, true),
    TRACE(true, true),
    QUERY(true, true),
    PUT(false, true),
    DELETE(false, true),
    POST(false, false),
    PATCH(false, false),
    CONNECT(false, false);

    public final boolean safe;
    public final boolean idempotent;

    Method(boolean safe, boolean idempotent) {
        this.safe = safe;
        this.idempotent = idempotent;
    }

    /**
     * Method names are case-sensitive (§9.1): "get" is not GET. Empty means the server doesn't
     * recognize the method, which it answers with 501 Not Implemented.
     */
    public static Optional<Method> parse(String token) {
        for (Method m : values()) {
            if (m.name().equals(token)) return Optional.of(m);
        }
        return Optional.empty();
    }

    /**
     * §9.2.2: after a connection failure the client can't tell whether the server applied the
     * request. Resending is harmless only if two copies have the same intended effect as one.
     */
    public boolean mayRetryAutomatically() {
        return idempotent;
    }
}
