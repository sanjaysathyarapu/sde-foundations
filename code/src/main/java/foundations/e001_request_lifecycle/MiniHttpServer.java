package foundations.e001_request_lifecycle;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

/** HTTP/1.1 on raw sockets: accept, parse, route, handle, write. No TLS, keep-alive or chunked bodies. */
public final class MiniHttpServer implements AutoCloseable {
    static final int READ_TIMEOUT_MILLIS = 5_000, MAX_BODY_BYTES = 1 << 20;
    public record Request(String method, String path, Map<String, String> headers, String body) {}
    public record Response(int status, String body, String allow) {}
    static final class HttpError extends RuntimeException { // a 4xx/5xx decided before any handler runs
        final int status;
        HttpError(int status, String why) { super(why); this.status = status; }
    }

    // path -> method -> handler. Path first, so "no such path" (404) differs from "wrong method" (405).
    private final Map<String, Map<String, Function<Request, Response>>> routes;
    private final ExecutorService connections = Executors.newVirtualThreadPerTaskExecutor(); // cheap blocking threads
    private final ServerSocket socket;

    public MiniHttpServer(int port, Map<String, Map<String, Function<Request, Response>>> routes) throws IOException {
        this.routes = Map.copyOf(routes);
        socket = new ServerSocket(port, 50, InetAddress.getLoopbackAddress()); // bind + listen: kernel now accepts TCP
        Thread.ofPlatform().daemon().start(() -> { // accept() returns connections the kernel already established
            while (!socket.isClosed()) try { Socket c = socket.accept(); connections.submit(() -> handle(c)); } catch (IOException e) {}
        });
    }

    public int port() { return socket.getLocalPort(); }

    private void handle(Socket client) {
        try (client) {
            client.setSoTimeout(READ_TIMEOUT_MILLIS); // otherwise a client that sends nothing holds this socket forever
            Response res;
            try { // ISO-8859-1 maps each byte to one char, so char counts below equal byte counts
                res = route(parse(new BufferedReader(new InputStreamReader(client.getInputStream(), ISO_8859_1))));
            } catch (HttpError e) { // malformed input never reaches a handler
                res = new Response(e.status, e.getMessage() + "\n", null);
            } catch (RuntimeException bug) { // a handler bug: log the details, never send them to the client
                System.err.println("handler failed: " + bug);
                res = new Response(500, "internal error\n", null);
            }
            // Content-Length tells the client where the body ends; "close": one request per connection.
            String head = "HTTP/1.1 " + res.status() + " \r\n" + (res.allow() == null ? "" : "Allow: " + res.allow() + "\r\n")
                    + "Content-Length: " + res.body().length() + "\r\nConnection: close\r\n\r\n";
            client.getOutputStream().write((head + res.body()).getBytes(ISO_8859_1));
        } catch (IOException timeoutOrDisconnect) {} // nothing useful can be sent back
    }

    static Request parse(BufferedReader in) throws IOException {
        String[] line = String.valueOf(in.readLine()).split(" ", -1); // method SP target SP version
        if (line.length != 3) throw new HttpError(400, "bad request line");
        if (!line[2].equals("HTTP/1.1")) throw new HttpError(505, "only HTTP/1.1 is supported");
        Map<String, String> headers = new HashMap<>();
        for (String h; (h = in.readLine()) != null && !h.isEmpty(); ) { // an empty line ends the head
            int colon = h.indexOf(':');
            if (colon <= 0) throw new HttpError(400, "bad header line");
            headers.put(h.substring(0, colon).toLowerCase(Locale.ROOT), h.substring(colon + 1).strip()); // names ignore case
        }
        if (!headers.containsKey("host")) throw new HttpError(400, "missing Host");
        if (headers.containsKey("transfer-encoding")) throw new HttpError(501, "chunked bodies not supported");
        String declared = headers.getOrDefault("content-length", "0"); // no length header: no body
        if (!declared.matches("\\d{1,9}")) throw new HttpError(400, "bad Content-Length");
        if (Integer.parseInt(declared) > MAX_BODY_BYTES) throw new HttpError(413, "body too large"); // before allocating
        char[] body = new char[Integer.parseInt(declared)];
        // TCP has no message boundaries: one read() may return part of the body, so loop until it is complete.
        for (int n = 0, r; n < body.length; n += r)
            if ((r = in.read(body, n, body.length - n)) < 0) throw new HttpError(400, "body cut short");
        return new Request(line[0], line[1].split("\\?", 2)[0], headers, new String(body));
    }

    private Response route(Request req) {
        var byMethod = routes.getOrDefault(req.path(), Map.of());
        if (byMethod.isEmpty()) return new Response(404, "no route\n", null);
        if (!byMethod.containsKey(req.method())) // a 405 MUST list the allowed methods in Allow (RFC 9110 §15.5.6)
            return new Response(405, "method not allowed\n", String.join(", ", new TreeSet<>(byMethod.keySet())));
        return byMethod.get(req.method()).apply(req);
    }

    @Override public void close() throws IOException { socket.close(); connections.close(); }
}
