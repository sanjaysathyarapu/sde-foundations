package foundations.e006_routing;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** A segment-trie router: static segments beat {params}, and a known path with the wrong method is 405, not 404. */
public final class Router {

    public sealed interface Match {}
    public record Found(String handler, Map<String, String> params) implements Match {}
    public record MethodNotAllowed(Set<String> allow) implements Match {}
    public record NotFound() implements Match {}

    private static final class Node {
        final Map<String, Node> statics = new HashMap<>();
        Node param;                // at most one {name} child per position, so "/users/{id}" vs "/users/{name}" can't be ambiguous
        String paramName;
        final Map<String, String> handlers = new TreeMap<>(); // method -> handler, sorted for a stable Allow header
    }

    private final Node root = new Node();

    public Router add(String method, String pattern, String handler) {
        Node n = root;
        for (String seg : segments(pattern)) {
            if (seg.startsWith("{") && seg.endsWith("}")) {
                String name = seg.substring(1, seg.length() - 1);
                if (n.param == null) { n.param = new Node(); n.paramName = name; }
                else if (!n.paramName.equals(name)) throw new IllegalArgumentException("conflicting parameter names: " + pattern);
                n = n.param;
            } else {
                n = n.statics.computeIfAbsent(seg, k -> new Node());
            }
        }
        // Fail at startup, not at request time: two handlers for one route is a configuration bug.
        if (n.handlers.putIfAbsent(method, handler) != null) throw new IllegalArgumentException("duplicate route: " + method + " " + pattern);
        return this;
    }

    public Match match(String method, String path) {
        var params = new LinkedHashMap<String, String>();
        Node n = find(root, segments(path), 0, params);
        if (n == null) return new NotFound();                                   // no route has this path: 404
        String handler = n.handlers.get(method);
        if (handler == null) return new MethodNotAllowed(Set.copyOf(n.handlers.keySet())); // path exists: 405 + Allow
        return new Found(handler, Map.copyOf(params));
    }

    // Depth-first: try the static child first (more specific), and fall back to the param child if that branch dead-ends.
    private static Node find(Node n, List<String> segs, int i, Map<String, String> params) {
        if (i == segs.size()) return n.handlers.isEmpty() ? null : n;
        Node s = n.statics.get(segs.get(i));
        if (s != null) { Node hit = find(s, segs, i + 1, params); if (hit != null) return hit; }
        if (n.param == null) return null;
        params.put(n.paramName, segs.get(i));
        Node hit = find(n.param, segs, i + 1, params);
        if (hit == null) params.remove(n.paramName); // undo the capture so a failed branch leaves no stale value
        return hit;
    }

    private static List<String> segments(String path) {
        if (!path.startsWith("/")) throw new IllegalArgumentException("path must start with /: " + path);
        if (path.equals("/")) return List.of();
        // limit -1 keeps the trailing empty segment, so "/users/" stays distinct from "/users".
        // Split first, decode second: "%2F" inside a segment must not become a new segment.
        // "+" means space only in form-encoded queries, not in paths, so protect it before URLDecoder sees it.
        return Arrays.stream(path.substring(1).split("/", -1))
                .map(s -> URLDecoder.decode(s.replace("+", "%2B"), StandardCharsets.UTF_8))
                .toList();
    }
}
