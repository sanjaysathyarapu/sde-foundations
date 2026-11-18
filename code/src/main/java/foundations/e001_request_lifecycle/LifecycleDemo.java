package foundations.e001_request_lifecycle;

/**
 * Wires a router into the server. Run it, then try:
 *
 * <pre>
 * curl -v http://localhost:8080/hello
 * curl -v -X POST --data 'ping' http://localhost:8080/echo
 * curl -v -X DELETE http://localhost:8080/hello     # 405 with an Allow header
 * </pre>
 *
 * <p>{@code curl -v} prints the request and response heads, which is the quickest way to see the
 * wire format described in the entry.
 */
public final class LifecycleDemo {

    private LifecycleDemo() {}

    /** The application: the only part of this example that is specific to "our" backend. */
    public static Router routes() {
        return new Router()
                .on("GET", "/hello", req -> HttpResponse.text(200, "OK", "hello\n"))
                .on("POST", "/echo", req -> HttpResponse.text(200, "OK", req.bodyText()));
    }

    public static void main(String[] args) throws Exception {
        MiniHttpServer server = MiniHttpServer.start(8080, routes());
        System.out.println("listening on http://localhost:" + server.port());
        Thread.currentThread().join(); // keep the JVM alive; stop with Ctrl+C
    }
}
