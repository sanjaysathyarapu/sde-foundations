package foundations.e001_request_lifecycle;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class RequestParserTest {

    private static InputStream wire(String raw) {
        return new ByteArrayInputStream(raw.getBytes(StandardCharsets.ISO_8859_1));
    }

    private static int statusFor(String raw) {
        return assertThrows(RequestParser.BadRequestException.class, () -> RequestParser.parse(wire(raw)))
                .status();
    }

    @Test
    void parsesRequestLineAndHeaders() throws Exception {
        HttpRequest req = RequestParser.parse(wire(
                "GET /orders?id=7 HTTP/1.1\r\nHost: example.com\r\nAccept: */*\r\n\r\n"));

        assertEquals("GET", req.method());
        assertEquals("/orders?id=7", req.target());
        assertEquals("/orders", req.path());
        assertEquals("HTTP/1.1", req.version());
        assertEquals("example.com", req.header("host"));
        assertEquals(0, req.body().length);
    }

    @Test
    void headerLookupIgnoresCase() throws Exception {
        HttpRequest req = RequestParser.parse(wire("GET / HTTP/1.1\r\nHOST: a\r\nX-Trace-Id: t1\r\n\r\n"));

        assertEquals("a", req.header("Host"));
        assertEquals("t1", req.header("x-trace-id"));
    }

    @Test
    void readsExactlyContentLengthBytesOfBody() throws Exception {
        // Trailing bytes after the body belong to the next message, not to this one.
        HttpRequest req = RequestParser.parse(wire(
                "POST /echo HTTP/1.1\r\nHost: a\r\nContent-Length: 5\r\n\r\nhelloEXTRA"));

        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), req.body());
    }

    @Test
    void acceptsBareLineFeeds() throws Exception {
        HttpRequest req = RequestParser.parse(wire("GET / HTTP/1.1\nHost: a\n\n"));

        assertEquals("a", req.header("host"));
    }

    @Test
    void combinesRepeatedHeaderFields() throws Exception {
        HttpRequest req = RequestParser.parse(wire(
                "GET / HTTP/1.1\r\nHost: a\r\nAccept: text/html\r\nAccept: application/json\r\n\r\n"));

        assertEquals("text/html, application/json", req.header("accept"));
    }

    @Test
    void http11WithoutHostIsRejected() {
        assertEquals(400, statusFor("GET / HTTP/1.1\r\n\r\n"));
    }

    @Test
    void http10WithoutHostIsAccepted() throws Exception {
        assertEquals("HTTP/1.0", RequestParser.parse(wire("GET / HTTP/1.0\r\n\r\n")).version());
    }

    @Test
    void malformedRequestLineIsRejected() {
        assertEquals(400, statusFor("GET /\r\nHost: a\r\n\r\n"));
        assertEquals(400, statusFor("GET  / HTTP/1.1\r\nHost: a\r\n\r\n"));
    }

    @Test
    void unknownVersionIsRejectedWith505() {
        assertEquals(505, statusFor("GET / HTTP/9.9\r\nHost: a\r\n\r\n"));
    }

    @Test
    void whitespaceBeforeColonIsRejected() {
        assertEquals(400, statusFor("GET / HTTP/1.1\r\nHost : a\r\n\r\n"));
    }

    @Test
    void conflictingFramingIsRejected() {
        assertEquals(400, statusFor(
                "POST / HTTP/1.1\r\nHost: a\r\nContent-Length: 3\r\nContent-Length: 4\r\n\r\nabcd"));
        assertEquals(400, statusFor("POST / HTTP/1.1\r\nHost: a\r\nContent-Length: -1\r\n\r\n"));
    }

    @Test
    void transferEncodingIsNotImplemented() {
        assertEquals(501, statusFor(
                "POST / HTTP/1.1\r\nHost: a\r\nTransfer-Encoding: chunked\r\n\r\n0\r\n\r\n"));
    }

    @Test
    void truncatedBodyIsRejected() {
        assertEquals(400, statusFor("POST / HTTP/1.1\r\nHost: a\r\nContent-Length: 10\r\n\r\nshort"));
    }

    @Test
    void oversizedBodyIsRejectedBeforeReadingIt() {
        assertEquals(413, statusFor(
                "POST / HTTP/1.1\r\nHost: a\r\nContent-Length: " + (RequestParser.MAX_BODY_BYTES + 1) + "\r\n\r\n"));
    }

    @Test
    void overlongLinesAreRejected() {
        String longTarget = "/" + "a".repeat(RequestParser.MAX_LINE_BYTES);
        assertEquals(414, statusFor("GET " + longTarget + " HTTP/1.1\r\nHost: a\r\n\r\n"));

        String longHeader = "X-Big: " + "b".repeat(RequestParser.MAX_LINE_BYTES);
        assertEquals(431, statusFor("GET / HTTP/1.1\r\nHost: a\r\n" + longHeader + "\r\n\r\n"));
    }
}
