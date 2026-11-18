package foundations.e001_request_lifecycle;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An HTTP/1.1 response before it is turned back into bytes.
 *
 * @param status the three-digit status code
 * @param reason the reason phrase. Clients should ignore it (RFC 9112 §4), so it is for humans only.
 * @param headers extra header fields set by the handler
 * @param body the response body
 */
public record HttpResponse(int status, String reason, Map<String, String> headers, byte[] body) {

    public HttpResponse {
        headers = Map.copyOf(headers);
        body = body.clone();
    }

    /** A plain-text response, the shape most examples in this entry need. */
    public static HttpResponse text(int status, String reason, String text) {
        return new HttpResponse(
                status,
                reason,
                Map.of("Content-Type", "text/plain; charset=utf-8"),
                text.getBytes(StandardCharsets.UTF_8));
    }

    /** Returns a copy with one more header field. */
    public HttpResponse withHeader(String name, String value) {
        Map<String, String> copy = new LinkedHashMap<>(headers);
        copy.put(name, value);
        return new HttpResponse(status, reason, copy, body);
    }

    @Override
    public byte[] body() {
        return body.clone();
    }

    /**
     * Serializes the response to the HTTP/1.1 wire format (RFC 9112 §2.1): a status line, header
     * lines, an empty line, then the body. Every line ends in CRLF.
     */
    public byte[] toBytes() {
        StringBuilder head = new StringBuilder();
        head.append("HTTP/1.1 ").append(status).append(' ').append(reason).append("\r\n");
        headers.forEach((name, value) -> head.append(name).append(": ").append(value).append("\r\n"));
        // Content-Length is how the client knows where this body ends. Without it (and without
        // chunked encoding) the only remaining signal is the connection closing.
        head.append("Content-Length: ").append(body.length).append("\r\n");
        // This teaching server handles one request per connection, so it says so explicitly.
        // HTTP/1.1 connections are persistent by default (RFC 9112 §9.3); "close" opts out.
        head.append("Connection: close\r\n");
        head.append("\r\n");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(head.toString().getBytes(StandardCharsets.US_ASCII));
        out.writeBytes(body);
        return out.toByteArray();
    }
}
