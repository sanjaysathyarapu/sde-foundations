package foundations.e001_request_lifecycle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Turns the bytes of one HTTP/1.1 request into an {@link HttpRequest}.
 *
 * <p>TCP delivers an ordered byte stream with no message boundaries. HTTP/1.1 puts the boundaries
 * back: the head ends at the first empty line, and the body length comes from Content-Length
 * (RFC 9112 §6). This class implements just enough of RFC 9112 to show that, and rejects what it
 * does not support instead of guessing, because a parser that guesses about framing is how request
 * smuggling bugs happen.
 */
public final class RequestParser {

    /** Longest accepted request line or header line. Bounds memory per connection. */
    static final int MAX_LINE_BYTES = 8 * 1024;

    /** Most header lines accepted. Without a cap a client can stream headers forever. */
    static final int MAX_HEADERS = 100;

    /** Largest accepted body. Real servers make this configurable. */
    static final int MAX_BODY_BYTES = 1024 * 1024;

    private RequestParser() {}

    /** Thrown when the bytes are not a request this server will accept. Carries the status to send. */
    public static final class BadRequestException extends Exception {
        private static final long serialVersionUID = 1L;

        private final int status;
        private final String reason;

        BadRequestException(int status, String reason, String message) {
            super(message);
            this.status = status;
            this.reason = reason;
        }

        public int status() {
            return status;
        }

        public String reason() {
            return reason;
        }
    }

    public static HttpRequest parse(InputStream in) throws IOException, BadRequestException {
        // 1. Request line: method SP request-target SP HTTP-version (RFC 9112 §3).
        // An overlong request line almost always means an overlong target, hence 414 (RFC 9112 §3).
        String requestLine = readLine(in, 414, "URI Too Long");
        if (requestLine == null) {
            throw bad("connection closed before a request line arrived");
        }
        String[] parts = requestLine.split(" ", -1);
        if (parts.length != 3 || parts[0].isEmpty() || parts[1].isEmpty()) {
            throw bad("malformed request line: " + requestLine);
        }
        String method = parts[0];
        String target = parts[1];
        String version = parts[2];
        if (!version.equals("HTTP/1.1") && !version.equals("HTTP/1.0")) {
            throw new BadRequestException(505, "HTTP Version Not Supported", "version " + version);
        }

        // 2. Header fields, one per line, until an empty line ends the head.
        Map<String, String> headers = new LinkedHashMap<>();
        for (int count = 0; ; count++) {
            String line = readLine(in, 431, "Request Header Fields Too Large");
            if (line == null) {
                throw bad("connection closed inside the header section");
            }
            if (line.isEmpty()) {
                break; // the empty line: the head is complete
            }
            if (count >= MAX_HEADERS) {
                throw new BadRequestException(431, "Request Header Fields Too Large", "too many headers");
            }
            int colon = line.indexOf(':');
            if (colon <= 0) {
                throw bad("header line without a field name: " + line);
            }
            String name = line.substring(0, colon);
            // RFC 9112 §5.1: whitespace between the field name and the colon MUST be rejected,
            // because different parsers have disagreed about what it means.
            if (name.endsWith(" ") || name.endsWith("\t")) {
                throw bad("whitespace before colon in header: " + name);
            }
            String key = name.toLowerCase(Locale.ROOT);
            String value = line.substring(colon + 1).strip();
            if (key.equals("content-length") && headers.containsKey(key)) {
                // Two framing values are ambiguous; refuse rather than pick one.
                throw bad("duplicate Content-Length");
            }
            // Repeated fields may be combined into one comma-separated value (RFC 9110 §5.3).
            headers.merge(key, value, (a, b) -> a + ", " + b);
        }

        // 3. HTTP/1.1 requires Host so one IP address can serve many sites (RFC 9112 §3.2).
        if (version.equals("HTTP/1.1") && !headers.containsKey("host")) {
            throw bad("HTTP/1.1 request without a Host header");
        }

        // 4. Body framing. This server supports Content-Length only.
        if (headers.containsKey("transfer-encoding")) {
            // RFC 9112 §6.1: a server that does not understand a transfer coding SHOULD answer 501.
            throw new BadRequestException(501, "Not Implemented", "Transfer-Encoding is not supported");
        }
        byte[] body = readBody(in, headers.get("content-length"));

        return new HttpRequest(method, target, version, headers, body);
    }

    private static byte[] readBody(InputStream in, String contentLength)
            throws IOException, BadRequestException {
        if (contentLength == null) {
            return new byte[0]; // no Content-Length and no Transfer-Encoding: the body is empty
        }
        if (!contentLength.matches("[0-9]{1,10}")) {
            throw bad("invalid Content-Length: " + contentLength);
        }
        long length = Long.parseLong(contentLength);
        if (length > MAX_BODY_BYTES) {
            throw new BadRequestException(413, "Content Too Large", "body of " + length + " bytes");
        }
        // readNBytes loops until it has the full count or hits end of stream. A single read()
        // may return fewer bytes, because TCP segments do not line up with HTTP messages.
        byte[] body = in.readNBytes((int) length);
        if (body.length != length) {
            throw bad("connection closed after " + body.length + " of " + length + " body bytes");
        }
        return body;
    }

    /**
     * Reads one line terminated by CRLF and returns it without the terminator, or {@code null} at
     * end of stream before any byte. A bare LF is also accepted, which RFC 9112 §2.2 permits.
     * A line longer than {@link #MAX_LINE_BYTES} fails with the given status.
     */
    static String readLine(InputStream in, int tooLongStatus, String tooLongReason)
            throws IOException, BadRequestException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') {
                byte[] bytes = line.toByteArray();
                int end = bytes.length > 0 && bytes[bytes.length - 1] == '\r' ? bytes.length - 1 : bytes.length;
                // The head is ASCII-compatible; ISO-8859-1 maps each byte to one char losslessly.
                return new String(bytes, 0, end, StandardCharsets.ISO_8859_1);
            }
            if (line.size() >= MAX_LINE_BYTES) {
                throw new BadRequestException(tooLongStatus, tooLongReason, "line too long");
            }
            line.write(b);
        }
        if (line.size() == 0) {
            return null;
        }
        throw bad("connection closed in the middle of a line");
    }

    private static BadRequestException bad(String message) {
        return new BadRequestException(400, "Bad Request", message);
    }
}
