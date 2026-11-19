package foundations.e003_http_methods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class HttpMethodsTest {

    @Test
    void everySafeMethodIsAlsoIdempotent() {
        // Doing nothing to the server's state twice is the same as doing it once (RFC 9110 §9.2.2).
        for (Method m : Method.values()) {
            if (m.safe) assertTrue(m.idempotent, m + " is safe, so it must be idempotent");
        }
    }

    @Test
    void methodNamesAreCaseSensitive() {
        assertEquals(Optional.of(Method.GET), Method.parse("GET"));
        assertTrue(Method.parse("get").isEmpty(), "lowercase is a different, unknown method");
        assertTrue(Method.parse("BREW").isEmpty());
    }

    @Test
    void onlyIdempotentMethodsAreRetriedAutomatically() {
        assertTrue(Method.GET.mayRetryAutomatically());
        assertTrue(Method.PUT.mayRetryAutomatically());
        assertTrue(Method.DELETE.mayRetryAutomatically());
        assertFalse(Method.POST.mayRetryAutomatically());
        assertFalse(Method.PATCH.mayRetryAutomatically());
    }

    @Test
    void retryingAPostWhoseResponseWasLostCreatesADuplicate() {
        OrderStore server = new OrderStore();

        server.post("2 x coffee"); // applied, but the response is lost on the way back
        server.post("2 x coffee"); // the client can't tell, so it resends

        assertEquals(2, server.snapshot().size(), "the customer now has two orders");
    }

    @Test
    void retryingAPutLeavesTheSameState() {
        OrderStore server = new OrderStore();

        int first = server.put(7, "2 x coffee");
        Map<Integer, String> afterOne = server.snapshot();
        int second = server.put(7, "2 x coffee");

        assertEquals(201, first);
        assertEquals(200, second, "the response may differ");
        assertEquals(afterOne, server.snapshot(), "the state must not");
    }

    @Test
    void deleteIsIdempotentEvenThoughTheStatusCodeDiffers() {
        OrderStore server = new OrderStore();
        int id = server.post("1 x tea");

        assertEquals(204, server.delete(id));
        Map<Integer, String> afterOne = server.snapshot();
        assertEquals(404, server.delete(id));

        assertEquals(afterOne, server.snapshot());
    }

    @Test
    void postSkipsIdsAlreadyCreatedByPut() {
        OrderStore server = new OrderStore();
        server.put(1, "made with PUT");

        int id = server.post("made with POST");

        assertEquals(2, id);
        assertEquals("made with PUT", server.snapshot().get(1));
    }
}
