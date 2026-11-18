package foundations.e001_request_lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import foundations.e001_request_lifecycle.MiniHttpServer.Response;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** End to end over real TCP: raw request bytes in, raw response bytes out, so the wire format is visible. */
class MiniHttpServerTest {

    private MiniHttpServer server;

    @BeforeEach
    void start() throws IOException {
        server = new MiniHttpServer(0, Map.of( // port 0: the OS picks a free port, so parallel runs don't clash
                "/hello", Map.of("GET", req -> new Response(200, "hello\n", null)),
                "/echo", Map.of("POST", req -> new Response(200, req.body(), null)),
                "/boom", Map.of("GET", req -> { throw new IllegalStateException("secret detail"); })));
    }

    @AfterEach
    void stop() throws IOException {
        server.close();
    }

    @Test
    void getReturnsStatusLineHeadersBlankLineBody() throws IOException {
        String raw = exchange("GET /hello?x=1 HTTP/1.1\r\nHost: localhost\r\n\r\n");

        assertTrue(raw.startsWith("HTTP/1.1 200 \r\n"), raw);
        assertTrue(raw.contains("\r\nContent-Length: 6\r\n"), raw);
        assertTrue(raw.endsWith("\r\n\r\nhello\n"), raw);
    }

    @Test
    void postBodyIsReadExactlyByContentLength() throws IOException {
        // Header names in mixed case still work: field names are case-insensitive.
        String raw = exchange("POST /echo HTTP/1.1\r\nHOST: localhost\r\ncontent-LENGTH: 4\r\n\r\nping");

        assertTrue(raw.startsWith("HTTP/1.1 200 "), raw);
        assertTrue(raw.endsWith("\r\n\r\nping"), raw);
    }

    @Test
    void routingDistinguishes404From405WithAllow() throws IOException {
        assertTrue(exchange("GET /nope HTTP/1.1\r\nHost: x\r\n\r\n").startsWith("HTTP/1.1 404 "));

        String raw = exchange("DELETE /hello HTTP/1.1\r\nHost: x\r\n\r\n");
        assertTrue(raw.startsWith("HTTP/1.1 405 "), raw);
        assertTrue(raw.contains("\r\nAllow: GET\r\n"), raw);
    }

    @Test
    void malformedRequestsAreRejectedBeforeAnyHandler() throws IOException {
        assertTrue(exchange("GET /hello HTTP/1.1\r\n\r\n").startsWith("HTTP/1.1 400 "), "missing Host");
        assertTrue(exchange("GET /hello\r\n\r\n").startsWith("HTTP/1.1 400 "), "bad request line");
        assertTrue(exchange("GET /hello HTTP/2.0\r\nHost: x\r\n\r\n").startsWith("HTTP/1.1 505 "), "version");
        assertTrue(exchange("POST /echo HTTP/1.1\r\nHost: x\r\nContent-Length: -1\r\n\r\n").startsWith("HTTP/1.1 400 "));
        assertTrue(exchange("POST /echo HTTP/1.1\r\nHost: x\r\nTransfer-Encoding: chunked\r\n\r\n")
                .startsWith("HTTP/1.1 501 "));
    }

    @Test
    void bodyLargerThanTheLimitIs413WithoutReadingIt() throws IOException {
        // The declared length alone is enough to refuse; the server never allocates or waits for 2 MB.
        String raw = exchange("POST /echo HTTP/1.1\r\nHost: x\r\nContent-Length: "
                + (MiniHttpServer.MAX_BODY_BYTES + 1) + "\r\n\r\n");

        assertTrue(raw.startsWith("HTTP/1.1 413 "), raw);
    }

    @Test
    void handlerExceptionBecomes500WithoutLeakingDetails() throws IOException {
        String raw = exchange("GET /boom HTTP/1.1\r\nHost: x\r\n\r\n");

        assertTrue(raw.startsWith("HTTP/1.1 500 "), raw);
        assertFalse(raw.contains("secret detail"), "exception message must not reach the client");
    }

    @Test
    void silentClientIsDisconnectedAfterTheReadTimeout() throws IOException {
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), server.port())) {
            socket.setSoTimeout(3 * MiniHttpServer.READ_TIMEOUT_MILLIS); // never hang the build
            long start = System.nanoTime();
            // Send nothing. The server's read times out and it closes the socket, so we see end of stream.
            assertEquals(-1, socket.getInputStream().read());
            long waitedMillis = (System.nanoTime() - start) / 1_000_000;
            assertTrue(waitedMillis >= MiniHttpServer.READ_TIMEOUT_MILLIS - 500, "closed after " + waitedMillis + " ms");
        }
    }

    /** Opens a TCP connection, writes the bytes, and reads until the server closes it ("Connection: close"). */
    private String exchange(String request) throws IOException {
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), server.port())) {
            OutputStream out = socket.getOutputStream();
            out.write(request.getBytes(StandardCharsets.ISO_8859_1));
            out.flush();
            return new String(socket.getInputStream().readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }
}
