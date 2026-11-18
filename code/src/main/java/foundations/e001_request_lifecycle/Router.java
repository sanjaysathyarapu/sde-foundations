package foundations.e001_request_lifecycle;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * Maps (method, path) to a handler. This is the step where a generic HTTP server becomes
 * <em>your</em> application: everything before it is the same for every backend.
 *
 * <p>Exact-match only. Path parameters and versioning are covered in the routing entry.
 */
public final class Router implements Function<HttpRequest, HttpResponse> {

    // path -> (method -> handler). Keyed by path first so we can tell "no such resource" (404)
    // apart from "resource exists, wrong method" (405).
    private final Map<String, Map<String, Function<HttpRequest, HttpResponse>>> routes =
            new LinkedHashMap<>();

    public Router on(String method, String path, Function<HttpRequest, HttpResponse> handler) {
        routes.computeIfAbsent(path, p -> new LinkedHashMap<>()).put(method, handler);
        return this;
    }

    @Override
    public HttpResponse apply(HttpRequest request) {
        Map<String, Function<HttpRequest, HttpResponse>> byMethod = routes.get(request.path());
        if (byMethod == null) {
            return HttpResponse.text(404, "Not Found", "no route for " + request.path() + "\n");
        }
        Function<HttpRequest, HttpResponse> handler = byMethod.get(request.method());
        if (handler == null) {
            // RFC 9110 §15.5.6: a 405 response MUST carry an Allow header listing what is supported,
            // so the client can correct itself instead of guessing.
            String allow = String.join(", ", new TreeSet<>(byMethod.keySet()));
            return HttpResponse.text(405, "Method Not Allowed", "use one of: " + allow + "\n")
                    .withHeader("Allow", allow);
        }
        return handler.apply(request);
    }
}
