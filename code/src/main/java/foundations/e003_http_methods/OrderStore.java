package foundations.e003_http_methods;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A toy origin server for {@code /orders}: just enough to show what "the same intended effect"
 * means for POST, PUT and DELETE. Each method returns the status code it would send.
 */
public final class OrderStore {

    private final Map<Integer, String> orders = new LinkedHashMap<>();
    private int nextId = 1;

    /** POST /orders: the server chooses the new URI, so every request creates another order. */
    public int post(String order) {
        while (orders.containsKey(nextId)) nextId++; // skip ids already taken by a PUT
        orders.put(nextId, order);
        return nextId++;
    }

    /** PUT /orders/{id}: the client names the resource and sends its whole new state. */
    public int put(int id, String order) {
        return orders.put(id, order) == null ? 201 : 200; // 201 Created, or 200 OK if replaced
    }

    /** DELETE /orders/{id}: a repeat answers 404, but leaves the state exactly as the first did. */
    public int delete(int id) {
        return orders.remove(id) != null ? 204 : 404;
    }

    public Map<Integer, String> snapshot() {
        return Map.copyOf(orders);
    }
}
