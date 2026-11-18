package foundations.e001_request_lifecycle;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

/**
 * A parsed HTTP/1.1 request: the request line, the header fields and the body.
 *
 * <p>This is the first point in the lifecycle where the server has <em>meaning</em> instead of
 * bytes. Everything before it (DNS, TCP, TLS) only moved octets; everything after it (routing,
 * handlers, business logic) works on this structure.
 *
 * @param method the request method token, for example {@code GET}. Methods are case-sensitive
 *     (RFC 9110 §9.1), so it is stored exactly as received.
 * @param target the request-target, usually origin-form such as {@code /orders?id=7}
 * @param version the protocol version, for example {@code HTTP/1.1}
 * @param headers header fields keyed by <em>lower-cased</em> name, because field names are
 *     case-insensitive (RFC 9110 §5.1) and a lookup must not depend on how the client spelled them
 * @param body the message body, empty when the request has none
 */
public record HttpRequest(
        String method, String target, String version, Map<String, String> headers, byte[] body) {

    public HttpRequest {
        // Defensive copy: handlers further down the pipeline must not be able to mutate what the
        // parser produced, so every stage sees the same request.
        headers = Map.copyOf(headers);
        body = body.clone();
    }

    /** Looks up a header by name, ignoring case. Returns {@code null} when it is absent. */
    public String header(String name) {
        return headers.get(name.toLowerCase(Locale.ROOT));
    }

    /** The path part of the target, without the query string. Routing matches on this. */
    public String path() {
        int q = target.indexOf('?');
        return q < 0 ? target : target.substring(0, q);
    }

    /** The body decoded as UTF-8. Real servers pick the charset from Content-Type. */
    public String bodyText() {
        return new String(body, StandardCharsets.UTF_8);
    }

    @Override
    public byte[] body() {
        return body.clone();
    }
}
