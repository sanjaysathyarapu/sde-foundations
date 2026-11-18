package foundations.e001_request_lifecycle;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * A deliberately small HTTP/1.1 server built directly on TCP sockets, so every stage of the
 * request lifecycle is visible in one file: accept, read, parse, route, handle, serialize, write.
 *
 * <p>Not for production: no TLS, no keep-alive, no chunked bodies. Its job is to show what a
 * framework such as Spring Boot (on Tomcat or Netty) does for you before your controller runs.
 */
public final class MiniHttpServer implements AutoCloseable {

    /** How long a connection may sit idle before we give up on it. */
    private static final int READ_TIMEOUT_MILLIS = 5_000;

    private final ServerSocket serverSocket;
    private final Function<HttpRequest, HttpResponse> handler;
    // One virtual thread per connection (Java 21). A blocking read parks the virtual thread
    // instead of an OS thread, so thread-per-connection code stays simple without the old cost.
    private final ExecutorService connections = Executors.newVirtualThreadPerTaskExecutor();
    private final Thread acceptLoop;

    private MiniHttpServer(ServerSocket serverSocket, Function<HttpRequest, HttpResponse> handler) {
        this.serverSocket = serverSocket;
        this.handler = handler;
        this.acceptLoop = Thread.ofPlatform().name("accept-loop").daemon().start(this::acceptForever);
    }

    /**
     * Binds to {@code port} on the loopback interface and starts accepting connections.
     * Pass 0 to let the operating system choose a free port, which tests rely on.
     */
    public static MiniHttpServer start(int port, Function<HttpRequest, HttpResponse> handler)
            throws IOException {
        // bind() + listen(): from here the kernel completes TCP handshakes on our behalf and queues
        // finished connections (the backlog, 50 here) until accept() takes them.
        ServerSocket socket = new ServerSocket(port, 50, InetAddress.getLoopbackAddress());
        return new MiniHttpServer(socket, handler);
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    private void acceptForever() {
        while (!serverSocket.isClosed()) {
            try {
                // accept() returns an already-established TCP connection. The three-way handshake
                // happened in the kernel; application code never sees SYN or ACK.
                Socket client = serverSocket.accept();
                connections.submit(() -> handle(client));
            } catch (SocketException closed) {
                return; // close() was called: the listening socket is gone, stop accepting
            } catch (IOException e) {
                // One failed accept must not take the whole server down.
                System.err.println("accept failed: " + e.getMessage());
            }
        }
    }

    private void handle(Socket client) {
        try (client) {
            // Without a timeout, a client that connects and never sends a byte would hold this
            // connection open forever. That is the idea behind slow-client (Slowloris) attacks.
            client.setSoTimeout(READ_TIMEOUT_MILLIS);
            // Buffering matters: the parser reads one byte at a time, and each unbuffered read
            // would be a separate system call.
            InputStream in = new BufferedInputStream(client.getInputStream());
            OutputStream out = client.getOutputStream();

            HttpResponse response;
            try {
                HttpRequest request = RequestParser.parse(in);
                response = handler.apply(request);
            } catch (RequestParser.BadRequestException e) {
                // A malformed request never reaches application code.
                response = HttpResponse.text(e.status(), e.reason(), e.getMessage() + "\n");
            } catch (RuntimeException e) {
                // A bug in a handler becomes a 500 for this request only. The detail goes to the
                // server log, not to the client, because stack traces leak internals.
                System.err.println("handler failed: " + e);
                response = HttpResponse.text(500, "Internal Server Error", "internal error\n");
            }

            out.write(response.toBytes());
            out.flush();
        } catch (SocketTimeoutException e) {
            // The client went quiet. Closing the socket (try-with-resources) frees the connection.
        } catch (IOException e) {
            // The client disconnected mid-exchange. Nothing useful can be sent back.
        }
    }

    @Override
    public void close() throws IOException {
        serverSocket.close(); // unblocks accept() with a SocketException, ending the loop
        connections.close(); // waits for in-flight connections to finish (Java 19+)
        try {
            acceptLoop.join(1_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
