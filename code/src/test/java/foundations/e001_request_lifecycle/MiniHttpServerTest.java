package foundations.e001_request_lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** End-to-end: real TCP connections against the server on an OS-assigned port. */
class MiniHttpServerTest {

    private MiniHttpServer server;
    // Force HTTP/1.1. The JDK client would otherwise try to upgrade cleartext connections to HTTP/2,
    // which this server does not speak (it would just answer in HTTP/1.1, but the test should be explicit).
    private final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

    @BeforeEach
    void startServer() throws IOException {
        Router routes = LifecycleDemo.routes()
                .on("GET", "/boom", req -> { throw new IllegalStateException("bug in handler"); });
        server = MiniHttpServer.start(0, routes); // port 0: no clashes between parallel test runs
    }

    @AfterEach
    void stopServer() throws IOException {
        server.close();
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + server.port() + path);
    }

    @Test
    void getReturnsHandlerResponse() throws Exception {
        var response = client.send(
                java.net.http.HttpRequest.newBuilder(uri("/hello")).GET().build(), BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("hello\n", response.body());
        assertEquals("6", response.headers().firstValue("content-length").orElseThrow());
    }

    @Test
    void postBodyRoundTrips() throws Exception {
        var response = client.send(
                java.net.http.HttpRequest.newBuilder(uri("/echo")).POST(BodyPublishers.ofString("ping")).build(),
                BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("ping", response.body());
    }

    @Test
    void unknownPathIs404() throws Exception {
        var response = client.send(
                java.net.http.HttpRequest.newBuilder(uri("/nope")).GET().build(), BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    @Test
    void wrongMethodIs405WithAllowHeader() throws Exception {
        var response = client.send(
                java.net.http.HttpRequest.newBuilder(uri("/hello")).DELETE().build(), BodyHandlers.ofString());

        assertEquals(405, response.statusCode());
        assertEquals("GET", response.headers().firstValue("allow").orElseThrow());
    }

    @Test
    void handlerExceptionBecomes500WithoutLeakingDetails() throws Exception {
        var response = client.send(
                java.net.http.HttpRequest.newBuilder(uri("/boom")).GET().build(), BodyHandlers.ofString());

        assertEquals(500, response.statusCode());
        assertTrue(!response.body().contains("bug in handler"), "exception message must not reach the client");
    }

    @Test
    void rawBytesWithoutHostGet400() throws Exception {
        // The JDK client always sends Host, so write the bytes by hand to see the server's check.
        String raw = rawExchange("GET /hello HTTP/1.1\r\n\r\n");

        assertTrue(raw.startsWith("HTTP/1.1 400 Bad Request\r\n"), raw);
    }

    @Test
    void responseWireFormatHasStatusLineHeadersBlankLineBody() throws Exception {
        String raw = rawExchange("GET /hello HTTP/1.1\r\nHost: localhost\r\n\r\n");

        assertTrue(raw.startsWith("HTTP/1.1 200 OK\r\n"), raw);
        assertTrue(raw.contains("\r\nContent-Length: 6\r\n"), raw);
        assertTrue(raw.endsWith("\r\n\r\nhello\n"), raw);
    }

    /** Opens a TCP connection, writes the bytes, and reads until the server closes it. */
    private String rawExchange(String request) throws IOException {
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), server.port())) {
            OutputStream out = socket.getOutputStream();
            out.write(request.getBytes(StandardCharsets.US_ASCII));
            out.flush();
            InputStream in = socket.getInputStream();
            // The server sends "Connection: close" and closes, so end of stream marks the end.
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
