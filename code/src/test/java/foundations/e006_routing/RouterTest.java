package foundations.e006_routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import foundations.e006_routing.Router.Found;
import foundations.e006_routing.Router.MethodNotAllowed;
import foundations.e006_routing.Router.NotFound;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RouterTest {

    // Registered with the param route first on purpose: precedence must not depend on registration order.
    private final Router router = new Router()
            .add("GET", "/users/{id}", "getUser")
            .add("GET", "/users/me", "getMe")
            .add("DELETE", "/users/{id}", "deleteUser")
            .add("GET", "/users/{id}/orders", "listOrders")
            .add("GET", "/files/{name}", "getFile")
            .add("GET", "/", "home");

    @Test
    void extractsPathParameters() {
        assertEquals(new Found("getUser", Map.of("id", "42")), router.match("GET", "/users/42"));
        assertEquals(new Found("listOrders", Map.of("id", "42")), router.match("GET", "/users/42/orders"));
        assertEquals(new Found("home", Map.of()), router.match("GET", "/"));
    }

    @Test
    void aStaticSegmentBeatsAParameterRegardlessOfOrder() {
        assertEquals(new Found("getMe", Map.of()), router.match("GET", "/users/me"));
    }

    @Test
    void backtracksWhenTheStaticBranchDeadEnds() {
        // "me" matches the static child, but /users/me has no /orders child, so the router retries "me" as {id}.
        assertEquals(new Found("listOrders", Map.of("id", "me")), router.match("GET", "/users/me/orders"));
    }

    @Test
    void aFailedBranchLeavesNoStaleParameter() {
        var r = new Router().add("GET", "/a/{x}/z", "deep").add("GET", "/{y}/{v}", "pair");
        // "/a/b" captures x=b in the static "a" branch, dead-ends, then matches {y}/{v}; x must not leak.
        assertEquals(new Found("pair", Map.of("y", "a", "v", "b")), r.match("GET", "/a/b"));
    }

    @Test
    void aKnownPathWithTheWrongMethodIs405WithAllow() {
        assertEquals(new MethodNotAllowed(Set.of("GET", "DELETE")), router.match("PUT", "/users/42"));
    }

    @Test
    void unknownPathsAndTrailingSlashesAre404() {
        assertEquals(new NotFound(), router.match("GET", "/orders"));
        assertEquals(new NotFound(), router.match("GET", "/users/42/orders/7"));
        // Strict on purpose: the application decides whether to redirect "/users/42/" to "/users/42".
        assertEquals(new NotFound(), router.match("GET", "/users/42/"));
    }

    @Test
    void decodesEachSegmentAfterSplitting() {
        // An encoded slash is data inside one segment, not a path separator.
        assertEquals(new Found("getFile", Map.of("name", "a/b.txt")), router.match("GET", "/files/a%2Fb.txt"));
        assertEquals(new Found("getFile", Map.of("name", "my report.pdf")), router.match("GET", "/files/my%20report.pdf"));
        assertEquals(new Found("getFile", Map.of("name", "c++.md")), router.match("GET", "/files/c++.md"));
    }

    @Test
    void conflictingRoutesFailAtRegistration() {
        assertThrows(IllegalArgumentException.class, () -> router.add("GET", "/users/{id}", "again"));
        assertThrows(IllegalArgumentException.class, () -> router.add("GET", "/users/{userId}/posts", "posts"));
    }
}
